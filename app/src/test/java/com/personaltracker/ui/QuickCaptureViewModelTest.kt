package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.usecase.CaptureQuickThoughtUseCase
import com.personaltracker.platform.voice.QuickCaptureVoiceRecorder
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiEffect
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiIntent
import com.personaltracker.ui.screens.quickcapture.QuickCaptureViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTaskRepo: TestTaskRepository
    private lateinit var fakeInboxRepo: TestUnfiledInboxRepository
    private lateinit var parser: DeterministicNlpCommandParser
    private lateinit var captureUseCase: CaptureQuickThoughtUseCase
    private lateinit var fakeVoiceRecorder: FakeVoiceRecorder

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

    @Before
    fun setUp() {
        fakeTaskRepo = TestTaskRepository()
        fakeInboxRepo = TestUnfiledInboxRepository()
        parser = DeterministicNlpCommandParser()
        captureUseCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo)
        fakeVoiceRecorder = FakeVoiceRecorder()
    }

    private fun createViewModel(): QuickCaptureViewModel {
        return QuickCaptureViewModel(
            voiceRecorder = fakeVoiceRecorder,
            captureQuickThoughtUseCase = captureUseCase,
            taskRepository = fakeTaskRepo,
            unfiledInboxRepository = fakeInboxRepo
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
        assertTrue(fakeVoiceRecorder.recordingStarted)
        assertEquals(0.85f, viewModel.uiState.value.recordingAmplitude, 0.001f)

        // Stop recording
        viewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)
        assertFalse(viewModel.uiState.value.isRecording)
        assertTrue(fakeVoiceRecorder.recordingStopped)
        assertEquals(0f, viewModel.uiState.value.recordingAmplitude, 0.001f)
    }
}
