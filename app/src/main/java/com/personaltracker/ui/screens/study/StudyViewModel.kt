package com.personaltracker.ui.screens.study

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.HeatmapLedgerEngine
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Production ViewModel driving the Study focus timer, interval modes, and contribution heatmap (AD-1, AD-2).
 * Synchronizes with [StudySessionForegroundService.timerState] across background/foreground boundaries.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val heatmapLedgerEngine: HeatmapLedgerEngine = HeatmapLedgerEngine(),
    private val focusRescheduleEngine: FocusRescheduleEngine = FocusRescheduleEngine(),
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState(isLoading = true))
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<StudyUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<StudyUiEffect> = _uiEffect.receiveAsFlow()

    private var previousTimerState: StudySessionTimerState = StudySessionTimerState()
    private var observeTimerJob: Job? = null
    private var observeSessionsJob: Job? = null

    init {
        observeTimerAndSessions()
    }

    private fun observeTimerAndSessions() {
        observeTimerJob?.cancel()
        observeSessionsJob?.cancel()
        val today = LocalDate.now().toString()

        // 1. Observe Foreground Service timer state
        observeTimerJob = StudySessionForegroundService.timerState
            .onEach { timer ->
                // Check if session completed
                if (timer.isCompleted && !previousTimerState.isCompleted) {
                    val earnedCoins = maxOf(1, (timer.totalSeconds / 300).toInt())
                    _uiEffect.send(StudyUiEffect.ShowSessionCompleteCelebration(earnedCoins))
                    _uiEffect.send(StudyUiEffect.ShowLottieReward(RewardAnimationType.MilestoneFullReveal))

                    // Evaluate interval transition (e.g. Pomodoro work -> short break / long break)
                    val currentInterval = _uiState.value.selectedIntervalType
                    val transition = focusRescheduleEngine.calculateIntervalTransition(
                        currentIntervalType = currentInterval,
                        completedWorkCycles = _uiState.value.completedCycles
                    )
                    _uiState.update {
                        it.copy(
                            completedCycles = transition.completedWorkCycles,
                            selectedIntervalType = transition.nextIntervalType,
                            selectedDurationMinutes = transition.suggestedDurationMinutes
                        )
                    }
                }
                previousTimerState = timer

                _uiState.update {
                    it.copy(
                        timerState = timer,
                        isLoading = false
                    )
                }
            }.launchIn(viewModelScope)

        // 2. Observe sessions from repository and aggregate heatmap reactively
        observeSessionsJob = studySessionRepository.getAllSessions()
            .distinctUntilChanged()
            .onEach { sessions ->
                val todayMinutes = sessions
                    .filter { it.sessionDate == today && !it.isInterrupted && !it.isBreak }
                    .sumOf { if (it.actualElapsedMinutes > 0) it.actualElapsedMinutes else it.durationMinutes }

                val heatmap = heatmapLedgerEngine.calculateHeatmap(sessions)

                _uiState.update {
                    it.copy(
                        recentSessions = sessions.take(50),
                        todayTotalStudyMinutes = todayMinutes,
                        heatmapData = heatmap
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            is StudyUiIntent.StartSession -> handleStartSession(
                subjectTag = intent.subjectTag,
                durationMinutes = intent.durationMinutes,
                intervalType = intent.intervalType
            )
            is StudyUiIntent.PauseSession -> handlePauseSession()
            is StudyUiIntent.ResumeSession -> handleResumeSession()
            is StudyUiIntent.StopSession -> handleStopSession()
            is StudyUiIntent.SelectSubjectTag -> _uiState.update { it.copy(selectedSubjectTag = intent.tag) }
            is StudyUiIntent.SelectDuration -> _uiState.update { it.copy(selectedDurationMinutes = intent.minutes) }
            is StudyUiIntent.SelectIntervalType -> handleSelectIntervalType(intent.intervalType)
            is StudyUiIntent.SelectHeatmapCell -> handleSelectHeatmapCell(intent.date)
            is StudyUiIntent.Refresh -> observeTimerAndSessions()
        }
    }

    private fun handleSelectIntervalType(intervalType: FocusIntervalType) {
        _uiState.update {
            it.copy(
                selectedIntervalType = intervalType,
                selectedDurationMinutes = intervalType.defaultDurationMinutes
            )
        }
    }

    private fun handleSelectHeatmapCell(date: String) {
        _uiState.update {
            it.copy(
                selectedCellDate = if (it.selectedCellDate == date) null else date
            )
        }
    }

    private fun handleStartSession(
        subjectTag: String,
        durationMinutes: Int,
        intervalType: FocusIntervalType = _uiState.value.selectedIntervalType
    ) {
        val ctx = context ?: return
        val durationSeconds = durationMinutes * 60L
        val sessionId = UUID.randomUUID().toString()
        StudySessionForegroundService.startService(
            context = ctx,
            sessionId = sessionId,
            subjectTag = subjectTag,
            durationSeconds = durationSeconds
        )
    }

    private fun handlePauseSession() {
        val ctx = context ?: return
        StudySessionForegroundService.pauseService(ctx)
    }

    private fun handleResumeSession() {
        val ctx = context ?: return
        StudySessionForegroundService.resumeService(ctx)
    }

    private fun handleStopSession() {
        val ctx = context ?: return
        StudySessionForegroundService.stopService(ctx)
    }
}
