package com.personaltracker.ui.screens.quickcapture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import com.personaltracker.domain.usecase.CaptureQuickThoughtUseCase
import com.personaltracker.domain.usecase.QuickCaptureResult
import com.personaltracker.platform.voice.QuickCaptureVoiceRecorder
import com.personaltracker.platform.voice.SpeechRecognitionError
import com.personaltracker.platform.voice.SpeechRecognitionManager
import com.personaltracker.platform.voice.SpeechRecognitionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Production ViewModel coordinating Quick Capture dual input (voice/text)
 * with on-device/online speech failover and enforcing deterministic 3-tier
 * confidence routing (FR-7, AD-1, AD-8).
 */
@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val voiceRecorder: QuickCaptureVoiceRecorder,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val captureQuickThoughtUseCase: CaptureQuickThoughtUseCase,
    private val taskRepository: TaskRepository,
    private val unfiledInboxRepository: UnfiledInboxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<QuickCaptureUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<QuickCaptureUiEffect> = _uiEffect.receiveAsFlow()

    private var recordingJob: Job? = null

    fun onIntent(intent: QuickCaptureUiIntent) {
        when (intent) {
            is QuickCaptureUiIntent.OpenSheet -> {
                _uiState.update { it.copy(isVisible = true, errorMessage = null) }
                if (hasRecordPermission()) {
                    startRecordingInternal()
                } else {
                    viewModelScope.launch {
                        _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Microphone permission required for voice capture"))
                    }
                }
            }
            is QuickCaptureUiIntent.DismissSheet -> {
                stopRecordingInternal()
                speechRecognitionManager.cancelListening()
                _uiState.update {
                    it.copy(
                        isVisible = false,
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null,
                        errorMessage = null
                    )
                }
            }
            is QuickCaptureUiIntent.InputTextChanged -> {
                _uiState.update { it.copy(inputText = intent.text) }
            }
            is QuickCaptureUiIntent.SubmitText -> {
                processCapturedText(intent.text, captureSource = "MANUAL_TEXT")
            }
            is QuickCaptureUiIntent.ToggleRecording -> {
                handleToggleRecording()
            }
            is QuickCaptureUiIntent.ConfirmProposal -> {
                handleConfirmProposal(intent)
            }
            is QuickCaptureUiIntent.DiscardProposal -> {
                _uiState.update { it.copy(mediumProposal = null) }
            }
            is QuickCaptureUiIntent.ProposalChipClicked -> {
                // Allows user to edit specific field
            }
            is QuickCaptureUiIntent.UndoHighConfidence -> {
                handleUndo()
            }
            is QuickCaptureUiIntent.DismissUndoBanner -> {
                _uiState.update { it.copy(highConfidenceCommittedTask = null, highConfidenceCommittedTitle = null) }
            }
        }
    }

    private fun hasRecordPermission(): Boolean {
        return speechRecognitionManager.hasRecordPermission()
    }

    private fun handleToggleRecording() {
        if (_uiState.value.isRecording || _uiState.value.isListening) {
            speechRecognitionManager.stopListening()
            voiceRecorder.stopRecording()
            _uiState.update {
                it.copy(
                    isRecording = false,
                    isListening = false,
                    recordingAmplitude = 0f,
                    transcriptionStatus = "Processing..."
                )
            }
        } else {
            startRecordingInternal()
        }
    }

    private fun startRecordingInternal() {
        if (!hasRecordPermission()) {
            viewModelScope.launch {
                _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Microphone permission required for voice capture"))
            }
            return
        }

        _uiState.update {
            it.copy(
                isRecording = true,
                isListening = true,
                transcriptionStatus = "Listening...",
                partialTranscript = null,
                errorMessage = null
            )
        }
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            speechRecognitionManager.startListening(preferOffline = true)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isListening = false,
                            recordingAmplitude = 0f,
                            transcriptionStatus = null,
                            partialTranscript = null,
                            errorMessage = error.message ?: "Speech recognition encountered an issue"
                        )
                    }
                }
                .collect { speechState ->
                    handleSpeechState(speechState)
                }
        }
    }

    private fun handleSpeechState(speechState: SpeechRecognitionState) {
        when (speechState) {
            is SpeechRecognitionState.Idle -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null
                    )
                }
            }
            is SpeechRecognitionState.Listening -> {
                _uiState.update {
                    it.copy(
                        isRecording = true,
                        isListening = true,
                        recordingAmplitude = speechState.rms,
                        transcriptionStatus = "Listening..."
                    )
                }
            }
            is SpeechRecognitionState.Processing -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = "Processing..."
                    )
                }
            }
            is SpeechRecognitionState.Result -> {
                if (speechState.isFinal) {
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            isListening = false,
                            recordingAmplitude = 0f,
                            transcriptionStatus = null,
                            partialTranscript = null
                        )
                    }
                    stopRecordingInternal()
                    if (speechState.text.isNotBlank()) {
                        processCapturedText(speechState.text, captureSource = "VOICE_STT")
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = "No speech detected. Tap mic to retry."
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            partialTranscript = speechState.text,
                            transcriptionStatus = "Listening..."
                        )
                    }
                }
            }
            is SpeechRecognitionState.Error -> {
                handleSpeechError(speechState.error, speechState.message)
            }
        }
    }

    private fun handleSpeechError(error: SpeechRecognitionError, message: String) {
        stopRecordingInternal()
        when {
            error.isNetworkError -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null,
                        errorMessage = message
                    )
                }
                viewModelScope.launch {
                    _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar(message))
                }
            }
            error.isTimeoutOrNoMatch -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null,
                        errorMessage = "No speech detected. Tap mic to retry."
                    )
                }
            }
            error == SpeechRecognitionError.AUDIO -> {
                val draft = _uiState.value.partialTranscript
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null,
                        inputText = if (it.inputText.isBlank() && !draft.isNullOrBlank()) draft else it.inputText,
                        errorMessage = "Audio interrupted. Text preserved."
                    )
                }
            }
            error == SpeechRecognitionError.INSUFFICIENT_PERMISSIONS -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null
                    )
                }
                viewModelScope.launch {
                    _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Microphone permission required for voice capture"))
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isListening = false,
                        recordingAmplitude = 0f,
                        transcriptionStatus = null,
                        partialTranscript = null,
                        errorMessage = message
                    )
                }
            }
        }
    }

    private fun processCapturedText(text: String, captureSource: String) {
        if (text.isBlank()) return

        stopRecordingInternal()
        _uiState.update {
            it.copy(
                isProcessing = true,
                isRecording = false,
                isListening = false,
                recordingAmplitude = 0f,
                errorMessage = null,
                transcriptionStatus = null,
                partialTranscript = null
            )
        }

        viewModelScope.launch {
            runCatching {
                captureQuickThoughtUseCase(rawText = text, captureSource = captureSource)
            }.onSuccess { result ->
                when (result) {
                    is QuickCaptureResult.HighConfidence -> {
                        _uiState.update {
                            it.copy(
                                isVisible = false,
                                inputText = "",
                                isProcessing = false,
                                highConfidenceCommittedTask = result.committedTask,
                                highConfidenceCommittedTitle = result.committedTask.title,
                                mediumProposal = null,
                                lowConfidenceNotice = null
                            )
                        }
                        _uiEffect.send(QuickCaptureUiEffect.ShowUndoToast(result.committedTask.title))
                        _uiEffect.send(QuickCaptureUiEffect.CloseSheet)
                    }
                    is QuickCaptureResult.MediumConfidence -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                mediumProposal = result.draftCommand,
                                highConfidenceCommittedTask = null,
                                highConfidenceCommittedTitle = null,
                                lowConfidenceNotice = null
                            )
                        }
                    }
                    is QuickCaptureResult.LowConfidence -> {
                        _uiState.update {
                            it.copy(
                                isVisible = false,
                                inputText = "",
                                isProcessing = false,
                                lowConfidenceNotice = "Saved to unfiled inbox • Triage whenever you're ready",
                                mediumProposal = null,
                                highConfidenceCommittedTask = null,
                                highConfidenceCommittedTitle = null
                            )
                        }
                        _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Saved to unfiled inbox"))
                        _uiEffect.send(QuickCaptureUiEffect.CloseSheet)
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Could not parse command. Saved to drafts."
                    )
                }
            }
        }
    }

    private fun stopRecordingInternal() {
        speechRecognitionManager.stopListening()
        voiceRecorder.stopRecording()
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun handleConfirmProposal(intent: QuickCaptureUiIntent.ConfirmProposal) {
        val proposal = intent.proposal
        viewModelScope.launch {
            runCatching {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = proposal.title,
                    scheduledDate = proposal.date ?: LocalDate.now().toString(),
                    scheduledTime = proposal.time,
                    estimatedDurationMinutes = proposal.durationMinutes ?: 0,
                    deliveryIntensity = proposal.deliveryIntensity,
                    state = "PENDING"
                )
                taskRepository.saveTask(task)

                _uiState.update {
                    it.copy(
                        mediumProposal = null,
                        isVisible = false,
                        highConfidenceCommittedTask = task,
                        highConfidenceCommittedTitle = task.title
                    )
                }
                _uiEffect.send(QuickCaptureUiEffect.ShowUndoToast(task.title))
                _uiEffect.send(QuickCaptureUiEffect.CloseSheet)
            }.onFailure {
                _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Could not confirm item. Please try again."))
            }
        }
    }

    private fun handleUndo() {
        val taskToDelete = _uiState.value.highConfidenceCommittedTask
        if (taskToDelete != null) {
            viewModelScope.launch {
                runCatching {
                    taskRepository.deleteTask(taskToDelete)
                }
            }
        }
        _uiState.update {
            it.copy(highConfidenceCommittedTask = null, highConfidenceCommittedTitle = null)
        }
        viewModelScope.launch {
            _uiEffect.send(QuickCaptureUiEffect.ShowSnackbar("Capture undone"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.destroy()
        voiceRecorder.stopRecording()
        recordingJob?.cancel()
        recordingJob = null
    }
}
