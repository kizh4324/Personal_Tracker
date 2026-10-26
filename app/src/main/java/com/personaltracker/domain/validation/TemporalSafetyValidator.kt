package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.ParsedCommand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin validator enforcing temporal safety rules (ARCH-3, FR-7.6, AD-1, AD-4):
 * - Rejects scheduled dates in the past relative to referenceDateTime.
 * - Rejects scheduled times on today that have already elapsed relative to referenceDateTime.
 */
@Singleton
class TemporalSafetyValidator @Inject constructor() : CommandValidator {

    override suspend fun validate(
        command: ParsedCommand,
        referenceDateTime: LocalDateTime
    ): ValidationResult {
        val referenceDate = referenceDateTime.toLocalDate()
        val referenceTime = referenceDateTime.toLocalTime()

        // 1. Check scheduled date if present
        val commandDate = command.date?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

        if (commandDate != null) {
            if (commandDate.isBefore(referenceDate)) {
                return ValidationResult.Invalid(
                    reason = ValidationFailureReason.TEMPORAL_PAST_TIME,
                    message = "Scheduled date is in the past — pick today or a future date",
                    invalidField = InvalidField.DATE
                )
            }
        }

        // 2. Check scheduled time if present
        val commandTime = command.time?.let {
            runCatching { LocalTime.parse(it) }.getOrNull()
        }

        if (commandTime != null) {
            val effectiveDate = commandDate ?: referenceDate

            if (effectiveDate == referenceDate && commandTime.isBefore(referenceTime)) {
                return ValidationResult.Invalid(
                    reason = ValidationFailureReason.TEMPORAL_PAST_TIME,
                    message = "Scheduled time is in the past — pick a future time",
                    invalidField = InvalidField.TIME
                )
            }
        }

        return ValidationResult.Valid
    }
}
