package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.ParsedCommand
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TemporalSafetyValidatorTest {

    private lateinit var validator: TemporalSafetyValidator
    private val refDateTime = LocalDateTime.of(2026, 10, 26, 14, 0) // 2026-10-26 14:00

    @Before
    fun setUp() {
        validator = TemporalSafetyValidator()
    }

    @Test
    fun testPastScheduledTimeOnTodayFailsValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Meeting at 10:00 today",
            intent = CommandIntent.CREATE_TASK,
            title = "Meeting",
            date = "2026-10-26",
            time = "10:00",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.TEMPORAL_PAST_TIME, invalid.reason)
        assertEquals(InvalidField.TIME, invalid.invalidField)
        assertEquals("Scheduled time is in the past — pick a future time", invalid.message)
    }

    @Test
    fun testPastScheduledDateFailsValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Buy groceries yesterday",
            intent = CommandIntent.CREATE_TASK,
            title = "Buy groceries",
            date = "2026-10-25",
            time = null,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)

        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.TEMPORAL_PAST_TIME, invalid.reason)
        assertEquals(InvalidField.DATE, invalid.invalidField)
        assertEquals("Scheduled date is in the past — pick today or a future date", invalid.message)
    }

    @Test
    fun testFutureTimeOnTodayPassesValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Team sync at 16:00 today",
            intent = CommandIntent.CREATE_TASK,
            title = "Team sync",
            date = "2026-10-26",
            time = "16:00",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testExactCurrentTimeOnTodayPassesValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Focus at 14:00",
            intent = CommandIntent.CREATE_TASK,
            title = "Focus",
            date = "2026-10-26",
            time = "14:00",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testFutureDateWithEarlierTimePassesValidation() = runTest {
        // Scheduled tomorrow at 10:00 (10:00 is earlier in day than ref 14:00, but date is in future)
        val command = ParsedCommand(
            rawText = "Dentist appointment tomorrow at 10:00",
            intent = CommandIntent.CREATE_TASK,
            title = "Dentist appointment",
            date = "2026-10-27",
            time = "10:00",
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testTodayWithNoTimeSpecifiedPassesValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Clean desk today",
            intent = CommandIntent.CREATE_TASK,
            title = "Clean desk",
            date = "2026-10-26",
            time = null,
            confidence = 0.65f,
            tier = ConfidenceTier.MEDIUM
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testFlexibleTaskWithNoDateOrTimePassesValidation() = runTest {
        val command = ParsedCommand(
            rawText = "Read a chapter",
            intent = CommandIntent.CREATE_TASK,
            title = "Read a chapter",
            date = null,
            time = null,
            confidence = 0.50f,
            tier = ConfidenceTier.MEDIUM
        )

        val result = validator.validate(command, refDateTime)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testLeapYearDateBoundaryPassesWhenInFuture() = runTest {
        val leapRef = LocalDateTime.of(2028, 2, 28, 12, 0)
        val command = ParsedCommand(
            rawText = "Leap day reflection",
            intent = CommandIntent.CREATE_TASK,
            title = "Leap day reflection",
            date = "2028-02-29",
            time = null,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, leapRef)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testLeapYearDateBoundaryFailsWhenInPast() = runTest {
        val afterLeapRef = LocalDateTime.of(2028, 3, 1, 12, 0)
        val command = ParsedCommand(
            rawText = "Leap day reflection",
            intent = CommandIntent.CREATE_TASK,
            title = "Leap day reflection",
            date = "2028-02-29",
            time = null,
            confidence = 0.85f,
            tier = ConfidenceTier.HIGH
        )

        val result = validator.validate(command, afterLeapRef)
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(ValidationFailureReason.TEMPORAL_PAST_TIME, invalid.reason)
        assertEquals(InvalidField.DATE, invalid.invalidField)
    }
}
