package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.ParsedCommand
import java.time.LocalDateTime

/**
 * Pure Kotlin contract for validating parsed commands against domain constraints (ARCH-3, FR-7.6, AD-1).
 */
interface CommandValidator {
    suspend fun validate(
        command: ParsedCommand,
        referenceDateTime: LocalDateTime = LocalDateTime.now()
    ): ValidationResult
}
