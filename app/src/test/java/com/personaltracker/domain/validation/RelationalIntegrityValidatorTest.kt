package com.personaltracker.domain.validation

import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.TestHabitRepository
import com.personaltracker.ui.TestRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class RelationalIntegrityValidatorTest {

    private lateinit var habitRepo: TestHabitRepository
    private lateinit var routineRepo: TestRoutineRepository
    private lateinit var validator: RelationalIntegrityValidator
    private val refDateTime = LocalDateTime.of(2026, 10, 26, 14, 0)

    @Before
    fun setUp() {
        habitRepo = TestHabitRepository()
        routineRepo = TestRoutineRepository()
        validator = RelationalIntegrityValidator(habitRepo, routineRepo)
    }

    @Test
    fun testNonExistentHabitFailsValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Log habit Skydiving",
            intent = CommandIntent.LOG_HABIT,
            title = "Skydiving",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.RELATIONAL_HABIT_NOT_FOUND, invalid.reason)
        assertEquals(InvalidField.HABIT, invalid.invalidField)
        assertEquals("Habit not found — check name or create habit first", invalid.message)
    }

    @Test
    fun testExistingHabitExactMatchPassesValidation() = runTest {
        habitRepo.saveHabit(
            Habit(
                id = "habit-1",
                name = "Drink Water",
                targetDaysOfWeek = "1,2,3,4,5,6,7"
            )
        )

        val command = ParsedCommand(
            rawText = "Log habit Drink Water",
            intent = CommandIntent.LOG_HABIT,
            title = "Drink Water",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testExistingHabitCaseInsensitiveMatchPassesValidation() = runTest {
        habitRepo.saveHabit(
            Habit(
                id = "habit-2",
                name = "Meditation",
                targetDaysOfWeek = "1,2,3,4,5,6,7"
            )
        )

        val command = ParsedCommand(
            rawText = "Log habit meditation",
            intent = CommandIntent.LOG_HABIT,
            title = "meditation",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testNonExistentRoutineFailsValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Shift Yoga routine by 15 mins",
            intent = CommandIntent.SHIFT_ROUTINE,
            title = "yoga routine",
            targetRoutineName = "yoga",
            shiftMinutes = 15,
            confidence = 0.90f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.RELATIONAL_ROUTINE_NOT_FOUND, invalid.reason)
        assertEquals(InvalidField.ROUTINE, invalid.invalidField)
        assertEquals("Routine not found — check routine name", invalid.message)
    }

    @Test
    fun testExistingRoutineExactMatchPassesValidation() = runTest {
        routineRepo.saveRoutine(
            Routine(
                id = "routine-morning",
                dayTypeId = "dt-1",
                title = "Morning Kickoff Routine",
                targetStartTime = "08:00",
                totalEstimatedMinutes = 25
            )
        )

        val command = ParsedCommand(
            rawText = "Shift Morning Kickoff routine by 10 mins",
            intent = CommandIntent.SHIFT_ROUTINE,
            title = "Morning Kickoff routine",
            targetRoutineName = "Morning Kickoff",
            shiftMinutes = 10,
            confidence = 0.90f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testExistingRoutineCaseInsensitivePartialMatchPassesValidation() = runTest {
        routineRepo.saveRoutine(
            Routine(
                id = "routine-crunch",
                dayTypeId = "dt-2",
                title = "Crunch Focus Launch",
                targetStartTime = "09:00",
                totalEstimatedMinutes = 45
            )
        )

        val command = ParsedCommand(
            rawText = "Shift crunch focus routine by 15 mins",
            intent = CommandIntent.SHIFT_ROUTINE,
            title = "crunch focus routine",
            targetRoutineName = "crunch focus",
            shiftMinutes = 15,
            confidence = 0.90f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testCreateTaskIntentBypassesRelationalValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Complete assignment",
            intent = CommandIntent.CREATE_TASK,
            title = "Complete assignment",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }
}
