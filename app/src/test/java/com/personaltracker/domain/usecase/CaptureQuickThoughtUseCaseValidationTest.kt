package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.validation.CommandValidationPipeline
import com.personaltracker.domain.validation.InvalidField
import com.personaltracker.domain.validation.RelationalIntegrityValidator
import com.personaltracker.domain.validation.TaskDeduplicationValidator
import com.personaltracker.domain.validation.TemporalSafetyValidator
import com.personaltracker.domain.validation.ValidationFailureReason
import com.personaltracker.domain.validation.ValidationResult
import com.personaltracker.ui.TestHabitRepository
import com.personaltracker.ui.TestRoutineRepository
import com.personaltracker.ui.TestTaskRepository
import com.personaltracker.ui.TestUnfiledInboxRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class CaptureQuickThoughtUseCaseValidationTest {

    private lateinit var taskRepo: TestTaskRepository
    private lateinit var inboxRepo: TestUnfiledInboxRepository
    private lateinit var habitRepo: TestHabitRepository
    private lateinit var routineRepo: TestRoutineRepository
    private lateinit var parser: DeterministicNlpCommandParser
    private lateinit var pipeline: CommandValidationPipeline
    private lateinit var useCase: CaptureQuickThoughtUseCase

    // Reference time: 2026-10-26 14:00 (Monday)
    private val refDateTime = LocalDateTime.of(2026, 10, 26, 14, 0)

    @Before
    fun setUp() {
        taskRepo = TestTaskRepository()
        inboxRepo = TestUnfiledInboxRepository()
        habitRepo = TestHabitRepository()
        routineRepo = TestRoutineRepository()
        parser = DeterministicNlpCommandParser()
        pipeline = CommandValidationPipeline(
            TemporalSafetyValidator(),
            RelationalIntegrityValidator(habitRepo, routineRepo),
            TaskDeduplicationValidator(taskRepo)
        )
        useCase = CaptureQuickThoughtUseCase(parser, taskRepo, inboxRepo, pipeline)
    }

    @Test
    fun testHighConfidenceTaskWithPastTimeIsDowngradedToMediumConfidence() = runTest {
        // High confidence task structure with past time 10:00 (ref is 14:00 today)
        val input = "Study algorithms for 45 minutes at 10:00 today urgent"

        val result = useCase(input, referenceDateTime = refDateTime)

        // Handshake: Must downgrade from High to Medium confidence
        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val medium = result as QuickCaptureResult.MediumConfidence
        assertEquals(ConfidenceTier.MEDIUM, medium.draftCommand.tier)
        assertEquals("Scheduled time is in the past — pick a future time", medium.validationWarning)

        val invalidResult = medium.validationResult as? ValidationResult.Invalid
        assertNotNull(invalidResult)
        assertEquals(ValidationFailureReason.TEMPORAL_PAST_TIME, invalidResult?.reason)
        assertEquals(InvalidField.TIME, invalidResult?.invalidField)

        // Zero Room database mutations
        assertEquals(0, taskRepo.tasksFlow.value.size)
        assertEquals(0, inboxRepo.inboxItemsFlow.value.size)
    }

    @Test
    fun testHighConfidenceTaskWithPastDateIsDowngradedToMediumConfidence() = runTest {
        // "yesterday" resolves to 2026-10-25 relative to refDateTime
        val input = "Buy groceries yesterday for 30 minutes at 4pm urgent"

        val result = useCase(input, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val medium = result as QuickCaptureResult.MediumConfidence
        assertEquals(ConfidenceTier.MEDIUM, medium.draftCommand.tier)
        assertEquals("Scheduled date is in the past — pick today or a future date", medium.validationWarning)

        val invalidResult = medium.validationResult as? ValidationResult.Invalid
        assertNotNull(invalidResult)
        assertEquals(ValidationFailureReason.TEMPORAL_PAST_TIME, invalidResult?.reason)
        assertEquals(InvalidField.DATE, invalidResult?.invalidField)

        // Zero database mutations
        assertEquals(0, taskRepo.tasksFlow.value.size)
    }

    @Test
    fun testHighConfidenceTaskWithDuplicateActiveTitleIsDowngradedToMediumConfidence() = runTest {
        // Pre-populate an active task "Buy Groceries" for today
        taskRepo.saveTask(
            Task(
                id = "task-existing",
                title = "Buy groceries",
                scheduledDate = "2026-10-26",
                state = "PENDING"
            )
        )
        assertEquals(1, taskRepo.tasksFlow.value.size)

        // Attempting to create duplicate active task on today
        val input = "Buy groceries for 30 minutes at 4pm today urgent"

        val result = useCase(input, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val medium = result as QuickCaptureResult.MediumConfidence
        assertEquals(ConfidenceTier.MEDIUM, medium.draftCommand.tier)
        assertEquals("A task with this title is already active for today", medium.validationWarning)

        val invalidResult = medium.validationResult as? ValidationResult.Invalid
        assertNotNull(invalidResult)
        assertEquals(ValidationFailureReason.DUPLICATE_ACTIVE_TASK, invalidResult?.reason)
        assertEquals(InvalidField.TITLE, invalidResult?.invalidField)

        // Database still only has the 1 existing task
        assertEquals(1, taskRepo.tasksFlow.value.size)
    }

    @Test
    fun testDuplicateCompletedTaskAllowsNewTaskCreation() = runTest {
        // Pre-populate a completed task "Buy Groceries" for today
        taskRepo.saveTask(
            Task(
                id = "task-completed",
                title = "Buy groceries",
                scheduledDate = "2026-10-26",
                state = "COMPLETED"
            )
        )

        // High confidence task for future time on today (16:00 when ref is 14:00)
        val input = "Buy groceries for 30 minutes at 4pm today urgent"

        val result = useCase(input, referenceDateTime = refDateTime)

        // Completed task does not block new task; auto-commits!
        assertTrue(result is QuickCaptureResult.HighConfidence)
        val high = result as QuickCaptureResult.HighConfidence
        assertEquals("Buy groceries", high.committedTask.title)

        // Both tasks in repo
        assertEquals(2, taskRepo.tasksFlow.value.size)
    }

    @Test
    fun testUnknownHabitCaptureFailsValidationAndDowngrades() = runTest {
        val input = "Log habit skydiving"

        val result = useCase(input, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val medium = result as QuickCaptureResult.MediumConfidence
        assertEquals("Habit not found — check name or create habit first", medium.validationWarning)

        val invalidResult = medium.validationResult as? ValidationResult.Invalid
        assertEquals(ValidationFailureReason.RELATIONAL_HABIT_NOT_FOUND, invalidResult?.reason)
    }

    @Test
    fun testUnknownRoutineCaptureFailsValidationAndDowngrades() = runTest {
        val input = "Shift Yoga routine by 15 mins"

        val result = useCase(input, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val medium = result as QuickCaptureResult.MediumConfidence
        assertEquals("Routine not found — check routine name", medium.validationWarning)

        val invalidResult = medium.validationResult as? ValidationResult.Invalid
        assertEquals(ValidationFailureReason.RELATIONAL_ROUTINE_NOT_FOUND, invalidResult?.reason)
    }

    @Test
    fun testValidHighConfidenceTaskAutoCommitsToRepository() = runTest {
        // High confidence task tomorrow at 10:00 (ref is today 14:00)
        val input = "Study algorithms for 45 minutes at 10am tomorrow urgent"

        val result = useCase(input, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.HighConfidence)
        val high = result as QuickCaptureResult.HighConfidence
        assertEquals("Study algorithms", high.committedTask.title)
        assertEquals("2026-10-27", high.committedTask.scheduledDate)
        assertEquals("10:00", high.committedTask.scheduledTime)

        // Successfully persisted in TaskRepository
        assertEquals(1, taskRepo.tasksFlow.value.size)
        assertEquals("Study algorithms", taskRepo.tasksFlow.value.first().title)
    }

    @Test
    fun testLowConfidenceCaptureRoutesToUnfiledInboxWithoutTriggeringValidation() = runTest {
        val gibberish = "asdkjfh aksjdfh qwerty"

        val result = useCase(gibberish, referenceDateTime = refDateTime)

        assertTrue(result is QuickCaptureResult.LowConfidence)
        val low = result as QuickCaptureResult.LowConfidence
        assertEquals(gibberish, low.inboxItem.rawTranscriptText)

        // Persisted to unfiled inbox
        assertEquals(1, inboxRepo.inboxItemsFlow.value.size)
        assertEquals(0, taskRepo.tasksFlow.value.size)
    }
}
