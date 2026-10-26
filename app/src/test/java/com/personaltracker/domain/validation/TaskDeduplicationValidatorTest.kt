package com.personaltracker.domain.validation

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.TestTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TaskDeduplicationValidatorTest {

    private lateinit var taskRepo: TestTaskRepository
    private lateinit var validator: TaskDeduplicationValidator
    private val refDateTime = LocalDateTime.of(2026, 10, 26, 14, 0)
    private val today = "2026-10-26"
    private val tomorrow = "2026-10-27"

    @Before
    fun setUp() {
        taskRepo = TestTaskRepository()
        validator = TaskDeduplicationValidator(taskRepo)
    }

    @Test
    fun testActiveTaskWithSameTitleOnSameDateFailsValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-1",
                title = "Gym",
                scheduledDate = today,
                state = "PENDING"
            )
        )

        val command = ParsedCommand(
            rawText = "Gym today",
            intent = CommandIntent.CREATE_TASK,
            title = "Gym",
            date = today,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.DUPLICATE_ACTIVE_TASK, invalid.reason)
        assertEquals(InvalidField.TITLE, invalid.invalidField)
        assertEquals("A task with this title is already active for today", invalid.message)
    }

    @Test
    fun testCaseInsensitiveActiveDuplicateFailsValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-2",
                title = "Buy Groceries",
                scheduledDate = today,
                state = "IN_PROGRESS"
            )
        )

        val command = ParsedCommand(
            rawText = "buy groceries today",
            intent = CommandIntent.CREATE_TASK,
            title = "buy groceries",
            date = today,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.DUPLICATE_ACTIVE_TASK, invalid.reason)
    }

    @Test
    fun testCompletedTaskOnSameDatePassesValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-3",
                title = "Gym",
                scheduledDate = today,
                state = "COMPLETED"
            )
        )

        val command = ParsedCommand(
            rawText = "Gym today",
            intent = CommandIntent.CREATE_TASK,
            title = "Gym",
            date = today,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testCancelledTaskOnSameDatePassesValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-4",
                title = "Gym",
                scheduledDate = today,
                state = "CANCELLED"
            )
        )

        val command = ParsedCommand(
            rawText = "Gym today",
            intent = CommandIntent.CREATE_TASK,
            title = "Gym",
            date = today,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testSameTitleOnDifferentDatePassesValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-5",
                title = "Gym",
                scheduledDate = today,
                state = "PENDING"
            )
        )

        val command = ParsedCommand(
            rawText = "Gym tomorrow",
            intent = CommandIntent.CREATE_TASK,
            title = "Gym",
            date = tomorrow,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testNonTaskIntentPassesValidation() = runTest {
        taskRepo.saveTask(
            Task(
                id = "task-6",
                title = "Water",
                scheduledDate = today,
                state = "PENDING"
            )
        )

        val command = ParsedCommand(
            rawText = "Log habit Water",
            intent = CommandIntent.LOG_HABIT,
            title = "Water",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }
}
