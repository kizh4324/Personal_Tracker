package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.usecase.CaptureQuickThoughtUseCase
import com.personaltracker.domain.validation.CommandValidationPipeline
import com.personaltracker.domain.validation.RelationalIntegrityValidator
import com.personaltracker.domain.validation.TaskDeduplicationValidator
import com.personaltracker.domain.validation.TemporalSafetyValidator
import com.personaltracker.platform.voice.QuickCaptureVoiceRecorder
import com.personaltracker.platform.voice.SpeechRecognitionError
import com.personaltracker.platform.voice.SpeechRecognitionManager
import com.personaltracker.platform.voice.SpeechRecognitionState
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiEffect
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiIntent
import com.personaltracker.ui.screens.quickcapture.QuickCaptureViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTaskRepo: TestTaskRepository
    private lateinit var fakeInboxRepo: TestUnfiledInboxRepository
    private lateinit var fakeHabitRepo: TestHabitRepository
    private lateinit var fakeRoutineRepo: TestRoutineRepository
    private lateinit var validationPipeline: CommandValidationPipeline
    private lateinit var parser: DeterministicNlpCommandParser
    private lateinit var captureUseCase: CaptureQuickThoughtUseCase
    private lateinit var fakeVoiceRecorder: FakeVoiceRecorder
    private lateinit var fakeSpeechManager: FakeSpeechRecognitionManager

    private class FakeVoiceRecorder : QuickCaptureVoiceRecorder(null) {
        var permissionGranted = true
        var recordingStarted = false
        var recordingStopped = false

        override fun hasRecordPermission(): Boolean = permissionGranted

        override fun startRecording(onAudioChunk: ((ShortArray, Int) -> Unit)?): Flow<Float> {
            recordingStarted = true
            return flowOf(0.25f, 0.55f, 0.85f)
        }

        override fun stopRecording() {
            recordingStopped = true
        }
    }

    private class FakeSpeechRecognitionManager : SpeechRecognitionManager(null) {
        var permissionGranted = true
        var recognitionAvailable = true
        var listeningStarted = false
        var listeningStopped = false
        var listeningCancelled = false
        var destroyed = false
        var emitStates: List<SpeechRecognitionState> = listOf(
            SpeechRecognitionState.Listening(0.25f),
            SpeechRecognitionState.Listening(0.55f),
            SpeechRecognitionState.Listening(0.85f)
        )

        override fun hasRecordPermission(): Boolean = permissionGranted
        override fun isRecognitionAvailable(): Boolean = recognitionAvailable

        override fun startListening(preferOffline: Boolean): Flow<SpeechRecognitionState> = flow {
            listeningStarted = true
            for (state in emitStates) {
                emit(state)
            }
        }

        override fun stopListening() {
            listeningStopped = true
        }

        override fun cancelListening() {
            listeningCancelled = true
        }

        override fun destroy() {
            destroyed = true
        }
    }

    @Before
    fun setUp() {
        fakeTaskRepo = TestTaskRepository()
        fakeInboxRepo = TestUnfiledInboxRepository()
        fakeHabitRepo = TestHabitRepository()
        fakeRoutineRepo = TestRoutineRepository()
        validationPipeline = CommandValidationPipeline(
            TemporalSafetyValidator(),
            RelationalIntegrityValidator(fakeHabitRepo, fakeRoutineRepo),
            TaskDeduplicationValidator(fakeTaskRepo)
        )
        parser = DeterministicNlpCommandParser()
        captureUseCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo, validationPipeline)
        fakeVoiceRecorder = FakeVoiceRecorder()
        fakeSpeechManager = FakeSpeechRecognitionManager()
    }

    private fun createViewModel(): QuickCaptureViewModel {
        return QuickCaptureViewModel(
            voiceRecorder = fakeVoiceRecorder,
            speechRecognitionManager = fakeSpeechManager,
            captureQuickThoughtUseCase = captureUseCase,
            taskRepository = fakeTaskRepo,
            unfiledInboxRepository = fakeInboxRepo,
            validationPipeline = validationPipeline
        )
    }

    @Test
    fun testHighConfidenceRoutingCommitsTaskAndEmitsUndoToast() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // High confidence task command (> 0.85)
            viewModel.onIntent(QuickCaptureUiIntent.SubmitText("Study algorithms for 45 minutes at 4pm tomorrow urgent"))

            val effect1 = awaitItem()
            assertTrue(effect1 is QuickCaptureUiEffect.ShowUndoToast)
            val toast = effect1 as QuickCaptureUiEffect.ShowUndoToast
            assertTrue(toast.taskTitle.contains("Study algorithms", ignoreCase = true))

            val effect2 = awaitItem()
            assertTrue(effect2 is QuickCaptureUiEffect.CloseSheet)

            // Verify task was committed to repository
            val tasks = fakeTaskRepo.tasksFlow.value
            assertEquals(1, tasks.size)
            assertTrue(tasks.first().title.contains("Study algorithms", ignoreCase = true))
        }

        val state = viewModel.uiState.value
        assertFalse(state.isVisible)
        assertNotNull(state.highConfidenceCommittedTask)
    }

    @Test
    fun testHighConfidenceUndoDeletesCommittedTask() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.SubmitText("Study algorithms for 45 minutes at 4pm tomorrow urgent"))
            awaitItem() // ShowUndoToast
            awaitItem() // CloseSheet

            assertEquals(1, fakeTaskRepo.tasksFlow.value.size)

            viewModel.onIntent(QuickCaptureUiIntent.UndoHighConfidence)

            val effect = awaitItem()
            assertTrue(effect is QuickCaptureUiEffect.ShowSnackbar)
            assertEquals("Capture undone", (effect as QuickCaptureUiEffect.ShowSnackbar).message)

            // Task must be removed from repository without penalties (AD-8)
            assertEquals(0, fakeTaskRepo.tasksFlow.value.size)
            assertNull(viewModel.uiState.value.highConfidenceCommittedTask)
        }
    }

    @Test
    fun testMediumConfidencePresentsConfirmationChipsWithoutDbMutation() = runTest {
        val viewModel = createViewModel()

        // "Read book tomorrow" has confidence in [0.50..0.85] (ConfidenceTier.MEDIUM)
        viewModel.onIntent(QuickCaptureUiIntent.SubmitText("Read book tomorrow"))

        val state = viewModel.uiState.value
        assertNotNull(state.mediumProposal)
        assertEquals("Read book", state.mediumProposal?.title)

        // No autonomous database mutation occurs until confirmation
        assertEquals(0, fakeTaskRepo.tasksFlow.value.size)

        // Now user confirms proposal
        val proposal = state.mediumProposal!!
        viewModel.onIntent(QuickCaptureUiIntent.ConfirmProposal(proposal))

        // After user confirms, task is committed
        assertEquals(1, fakeTaskRepo.tasksFlow.value.size)
        assertEquals("Read book", fakeTaskRepo.tasksFlow.value.first().title)
        assertNull(viewModel.uiState.value.mediumProposal)
    }

    @Test
    fun testLowConfidenceRoutesToUnfiledInbox() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // Gibberish command with confidence < 0.50
            viewModel.onIntent(QuickCaptureUiIntent.SubmitText("asdkjfhakjsdhfkjasd fnasdkfj"))

            val effect1 = awaitItem()
            assertTrue(effect1 is QuickCaptureUiEffect.ShowSnackbar)
            assertEquals("Saved to unfiled inbox", (effect1 as QuickCaptureUiEffect.ShowSnackbar).message)

            val effect2 = awaitItem()
            assertTrue(effect2 is QuickCaptureUiEffect.CloseSheet)

            // Saved to inbox repository
            val inboxItems = fakeInboxRepo.inboxItemsFlow.value
            assertEquals(1, inboxItems.size)
            assertEquals("asdkjfhakjsdhfkjasd fnasdkfj", inboxItems.first().rawTranscriptText)

            // Zero mutation to tasks table
            assertEquals(0, fakeTaskRepo.tasksFlow.value.size)
        }

        val state = viewModel.uiState.value
        assertNotNull(state.lowConfidenceNotice)
    }

    @Test
    fun testVoiceRecordingToggleStreamsRmsAmplitudes() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        assertTrue(viewModel.uiState.value.isRecording)
        assertTrue(fakeSpeechManager.listeningStarted)
        assertEquals(0.85f, viewModel.uiState.value.recordingAmplitude, 0.001f)

        // Stop recording
        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)
        assertFalse(viewModel.uiState.value.isRecording)
        assertTrue(fakeSpeechManager.listeningStopped)
        assertEquals(0f, viewModel.uiState.value.recordingAmplitude, 0.001f)
    }

    @Test
    fun testOpenSheetAutoStartsRecordingWhenPermissionGranted() = runTest {
        fakeSpeechManager.permissionGranted = true
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)

        val state = viewModel.uiState.value
        assertTrue(state.isVisible)
        assertTrue(state.isRecording)
        assertTrue(fakeSpeechManager.listeningStarted)
        assertEquals(0.85f, state.recordingAmplitude, 0.001f)
    }

    @Test
    fun testOpenSheetShowsPermissionSnackbarWhenDenied() = runTest {
        fakeSpeechManager.permissionGranted = false
        fakeVoiceRecorder.permissionGranted = false
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)

            val effect = awaitItem()
            assertTrue(effect is QuickCaptureUiEffect.ShowSnackbar)
            assertTrue((effect as QuickCaptureUiEffect.ShowSnackbar).message.contains("permission", ignoreCase = true))

            val state = viewModel.uiState.value
            assertTrue(state.isVisible)
            assertFalse(state.isRecording)
            assertFalse(fakeSpeechManager.listeningStarted)
            assertEquals(0f, state.recordingAmplitude, 0.001f)
        }
    }

    @Test
    fun testDismissSheetStopsRecordingAndResetsAmplitude() = runTest {
        fakeSpeechManager.permissionGranted = true
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)
        assertTrue(viewModel.uiState.value.isRecording)

        viewModel.onIntent(QuickCaptureUiIntent.DismissSheet)
        val state = viewModel.uiState.value
        assertFalse(state.isVisible)
        assertFalse(state.isRecording)
        assertTrue(fakeSpeechManager.listeningCancelled)
        assertEquals(0f, state.recordingAmplitude, 0.001f)
    }

    @Test
    fun testSubmitTextStopsActiveRecording() = runTest {
        fakeSpeechManager.permissionGranted = true
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)
        assertTrue(viewModel.uiState.value.isRecording)

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.SubmitText("Study algorithms for 45 minutes at 4pm tomorrow urgent"))
            awaitItem() // ShowUndoToast
            awaitItem() // CloseSheet

            assertFalse(viewModel.uiState.value.isRecording)
            assertTrue(fakeSpeechManager.listeningStopped)
            assertEquals(0f, viewModel.uiState.value.recordingAmplitude, 0.001f)
        }
    }

    @Test
    fun testSpeechTranscriptionHighConfidenceAutoCommit() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.3f),
            SpeechRecognitionState.Processing,
            SpeechRecognitionState.Result("Study algorithms for 45 minutes at 4pm tomorrow urgent", isFinal = true)
        )
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

            val effect1 = awaitItem()
            assertTrue(effect1 is QuickCaptureUiEffect.ShowUndoToast)
            val effect2 = awaitItem()
            assertTrue(effect2 is QuickCaptureUiEffect.CloseSheet)

            assertEquals(1, fakeTaskRepo.tasksFlow.value.size)
            assertTrue(fakeTaskRepo.tasksFlow.value.first().title.contains("Study algorithms", ignoreCase = true))
        }

        assertNotNull(viewModel.uiState.value.highConfidenceCommittedTask)
        assertFalse(viewModel.uiState.value.isRecording)
    }

    @Test
    fun testSpeechTranscriptionMediumConfidenceProposal() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.4f),
            SpeechRecognitionState.Processing,
            SpeechRecognitionState.Result("Buy groceries tomorrow", isFinal = true)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertNotNull(state.mediumProposal)
        assertEquals("Buy groceries", state.mediumProposal?.title)
        assertEquals(0, fakeTaskRepo.tasksFlow.value.size) // No database mutation before confirmation

        viewModel.onIntent(QuickCaptureUiIntent.ConfirmProposal(state.mediumProposal!!))

        assertEquals(1, fakeTaskRepo.tasksFlow.value.size)
        assertEquals("Buy groceries", fakeTaskRepo.tasksFlow.value.first().title)
    }

    @Test
    fun testSpeechTranscriptionLowConfidenceUnfiledInbox() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.2f),
            SpeechRecognitionState.Result("some random noise gibberish words xyz", isFinal = true)
        )
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

            val effect1 = awaitItem()
            assertTrue(effect1 is QuickCaptureUiEffect.ShowSnackbar)
            val effect2 = awaitItem()
            assertTrue(effect2 is QuickCaptureUiEffect.CloseSheet)

            assertEquals(1, fakeInboxRepo.inboxItemsFlow.value.size)
            assertEquals("some random noise gibberish words xyz", fakeInboxRepo.inboxItemsFlow.value.first().rawTranscriptText)
            assertEquals("VOICE_STT", fakeInboxRepo.inboxItemsFlow.value.first().captureSource)
            assertEquals(0, fakeTaskRepo.tasksFlow.value.size)
        }

        assertNotNull(viewModel.uiState.value.lowConfidenceNotice)
    }

    @Test
    fun testSpeechRecognitionProcessingUpdatesTranscriptionStatus() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.3f),
            SpeechRecognitionState.Processing
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertFalse(state.isListening)
        assertEquals(0f, state.recordingAmplitude, 0.001f)
        assertEquals("Processing...", state.transcriptionStatus)
    }

    @Test
    fun testBlankFinalSpeechResultSetsNoSpeechDetectedError() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.2f),
            SpeechRecognitionState.Result("   ", isFinal = true)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertFalse(state.isListening)
        assertEquals(0f, state.recordingAmplitude, 0.001f)
        assertEquals("No speech detected. Tap mic to retry.", state.errorMessage)
        assertEquals(0, fakeTaskRepo.tasksFlow.value.size)
        assertEquals(0, fakeInboxRepo.inboxItemsFlow.value.size)
    }

    @Test
    fun testSpeechRecognitionTimeoutShowsRetryNotice() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.1f),
            SpeechRecognitionState.Error(SpeechRecognitionError.SPEECH_TIMEOUT, "No speech detected. Tap mic to retry.")
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertFalse(state.isListening)
        assertEquals(0f, state.recordingAmplitude, 0.001f)
        assertEquals("No speech detected. Tap mic to retry.", state.errorMessage)
    }

    @Test
    fun testSpeechRecognitionNetworkErrorFailover() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.2f),
            SpeechRecognitionState.Error(SpeechRecognitionError.NETWORK, "Network connection lost. Switched to offline mode.")
        )
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

            val snackbar = awaitItem()
            assertTrue(snackbar is QuickCaptureUiEffect.ShowSnackbar)
            assertTrue((snackbar as QuickCaptureUiEffect.ShowSnackbar).message.contains("offline", ignoreCase = true))
        }

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testSpeechRecognitionPartialResultsUpdateUiState() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.3f),
            SpeechRecognitionState.Result("Buy groc", isFinal = false)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertTrue(state.isRecording)
        assertEquals("Buy groc", state.partialTranscript)
        assertEquals("Listening...", state.transcriptionStatus)
    }

    @Test
    fun testAudioInterruptionPreservesPartialDraft() = runTest {
        fakeSpeechManager.emitStates = listOf(
            SpeechRecognitionState.Listening(0.3f),
            SpeechRecognitionState.Result("Call doctor tom", isFinal = false),
            SpeechRecognitionState.Error(SpeechRecognitionError.AUDIO, "Audio recording error. Please check microphone.")
        )
        val viewModel = createViewModel()

        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertEquals("Call doctor tom", state.inputText)
        assertTrue(state.errorMessage?.contains("interrupted", ignoreCase = true) == true)
    }

    @Test
    fun testConfirmProposalWithPastTimeIsRejectedAndBlocksPersistence() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)

        val invalidProposal = ParsedCommand(
            rawText = "Meeting at 10:00 today",
            intent = CommandIntent.CREATE_TASK,
            title = "Meeting",
            date = java.time.LocalDate.now().toString(),
            time = "00:01", // Past time (assuming test does not run at midnight 00:00)
            deliveryIntensity = "ROUTINE",
            confidence = 0.70f,
            tier = ConfidenceTier.MEDIUM
        )

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.ConfirmProposal(invalidProposal))

            val snackbar = awaitItem()
            assertTrue(snackbar is QuickCaptureUiEffect.ShowSnackbar)
            val msg = (snackbar as QuickCaptureUiEffect.ShowSnackbar).message
            assertTrue(msg.contains("past", ignoreCase = true))

            // Zero mutation to tasks table
            assertEquals(0, fakeTaskRepo.tasksFlow.value.size)

            val state = viewModel.uiState.value
            assertTrue(state.isVisible)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage?.contains("past", ignoreCase = true) == true)
        }
    }

    @Test
    fun testConfirmProposalWithDuplicateActiveTaskIsRejectedAndBlocksPersistence() = runTest {
        val today = java.time.LocalDate.now().toString()
        val existingTask = com.personaltracker.domain.model.Task(
            id = "task-existing",
            title = "Gym Workout",
            scheduledDate = today,
            state = "PENDING"
        )
        fakeTaskRepo.saveTask(existingTask)
        assertEquals(1, fakeTaskRepo.tasksFlow.value.size)

        val viewModel = createViewModel()
        viewModel.onIntent(QuickCaptureUiIntent.OpenSheet)

        val duplicateProposal = ParsedCommand(
            rawText = "Gym workout tomorrow",
            intent = CommandIntent.CREATE_TASK,
            title = "Gym Workout",
            date = today,
            confidence = 0.70f,
            tier = ConfidenceTier.MEDIUM
        )

        viewModel.uiEffect.test {
            viewModel.onIntent(QuickCaptureUiIntent.ConfirmProposal(duplicateProposal))

            val snackbar = awaitItem()
            assertTrue(snackbar is QuickCaptureUiEffect.ShowSnackbar)
            val msg = (snackbar as QuickCaptureUiEffect.ShowSnackbar).message
            assertTrue(msg.contains("already active", ignoreCase = true))

            // Still only the original 1 task in repository
            assertEquals(1, fakeTaskRepo.tasksFlow.value.size)

            val state = viewModel.uiState.value
            assertTrue(state.isVisible)
            assertNotNull(state.errorMessage)
        }
    }

    @Test
    fun testHighConfidenceCaptureFailingValidationDowngradesToMediumConfidenceInViewModel() = runTest {
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // High confidence syntax with "urgent" and duration, but date is yesterday!
            viewModel.onIntent(QuickCaptureUiIntent.SubmitText("Study algorithms for 45 minutes $yesterday urgent"))

            // Since it fails validation, it should NOT emit ShowUndoToast or CloseSheet!
            expectNoEvents()

            // Database must have ZERO tasks committed
            assertEquals(0, fakeTaskRepo.tasksFlow.value.size)

            // UI state must hold mediumProposal with warning banner
            val state = viewModel.uiState.value
            assertNotNull(state.mediumProposal)
            assertEquals(ConfidenceTier.MEDIUM, state.mediumProposal?.tier)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage?.contains("past", ignoreCase = true) == true)
            assertNull(state.highConfidenceCommittedTask)
        }
    }
}
