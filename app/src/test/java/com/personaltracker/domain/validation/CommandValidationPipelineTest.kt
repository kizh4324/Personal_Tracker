package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class CommandValidationPipelineTest {

    private val refDateTime = LocalDateTime.of(2026, 10, 26, 14, 0)

    private class MockValidator(
        val resultToReturn: ValidationResult,
        var wasCalled: Boolean = false
    ) : CommandValidator {
        override suspend fun validate(
            command: ParsedCommand,
            referenceDateTime: LocalDateTime
        ): ValidationResult {
            wasCalled = true
            return resultToReturn
        }
    }

    @Test
    fun testAllValidatorsPassReturnsValid() = runTest {
        val v1 = MockValidator(ValidationResult.Valid)
        val v2 = MockValidator(ValidationResult.Valid)
        val v3 = MockValidator(ValidationResult.Valid)

        val pipeline = CommandValidationPipeline(listOf(v1, v2, v3))
        val command = ParsedCommand(
            rawText = "Study algorithms",
            intent = CommandIntent.CREATE_TASK,
            title = "Study algorithms",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = pipeline.validate(command, refDateTime)

        assertEquals(ValidationResult.Valid, result)
        assertTrue(v1.wasCalled)
        assertTrue(v2.wasCalled)
        assertTrue(v3.wasCalled)
    }

    @Test
    fun testFirstValidatorFailsShortCircuitsSubsequentValidators() = runTest {
        val failure = ValidationResult.Invalid(
            reason = ValidationFailureReason.TEMPORAL_PAST_TIME,
            message = "Scheduled time is in the past — pick a future time",
            invalidField = InvalidField.TIME
        )
        val v1 = MockValidator(failure)
        val v2 = MockValidator(ValidationResult.Valid)
        val v3 = MockValidator(ValidationResult.Valid)

        val pipeline = CommandValidationPipeline(listOf(v1, v2, v3))
        val command = ParsedCommand(
            rawText = "Meeting at 10:00 today",
            intent = CommandIntent.CREATE_TASK,
            title = "Meeting",
            date = "2026-10-26",
            time = "10:00",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = pipeline.validate(command, refDateTime)

        assertEquals(failure, result)
        assertTrue(v1.wasCalled)
        assertTrue(!v2.wasCalled)
        assertTrue(!v3.wasCalled)
    }

    @Test
    fun testSecondValidatorFailsShortCircuitsThirdValidator() = runTest {
        val v1 = MockValidator(ValidationResult.Valid)
        val failure = ValidationResult.Invalid(
            reason = ValidationFailureReason.RELATIONAL_HABIT_NOT_FOUND,
            message = "Habit not found",
            invalidField = InvalidField.HABIT
        )
        val v2 = MockValidator(failure)
        val v3 = MockValidator(ValidationResult.Valid)

        val pipeline = CommandValidationPipeline(listOf(v1, v2, v3))
        val command = ParsedCommand(
            rawText = "Log habit skydiving",
            intent = CommandIntent.LOG_HABIT,
            title = "skydiving",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = pipeline.validate(command, refDateTime)

        assertEquals(failure, result)
        assertTrue(v1.wasCalled)
        assertTrue(v2.wasCalled)
        assertTrue(!v3.wasCalled)
    }
}
