package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.ParsedCommand
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin composite pipeline orchestrating command validators (ARCH-3, FR-7.6, AD-1).
 *
 * Runs validators in order:
 * 1. TemporalSafetyValidator
 * 2. RelationalIntegrityValidator
 * 3. TaskDeduplicationValidator
 *
 * Short-circuits on the first Invalid result or returns Valid if all pass.
 */
@Singleton
class CommandValidationPipeline(
    private val validators: List<CommandValidator>
) : CommandValidator {

    @Inject
    constructor(
        temporalSafetyValidator: TemporalSafetyValidator,
        relationalIntegrityValidator: RelationalIntegrityValidator,
        taskDeduplicationValidator: TaskDeduplicationValidator
    ) : this(
        listOf(
            temporalSafetyValidator,
            relationalIntegrityValidator,
            taskDeduplicationValidator
        )
    )

    override suspend fun validate(
        command: ParsedCommand,
        referenceDateTime: LocalDateTime
    ): ValidationResult {
        for (validator in validators) {
            val result = validator.validate(command, referenceDateTime)
            if (result is ValidationResult.Invalid) {
                return result
            }
        }
        return ValidationResult.Valid
    }
}
