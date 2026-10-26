package com.personaltracker.domain.validation

/**
 * Categorized failure reasons for command validation (ARCH-3, FR-7.6, AD-1, AD-4).
 */
enum class ValidationFailureReason {
    TEMPORAL_PAST_TIME,
    TEMPORAL_PAST_DATE,
    RELATIONAL_HABIT_NOT_FOUND,
    RELATIONAL_ROUTINE_NOT_FOUND,
    DUPLICATE_ACTIVE_TASK
}

/**
 * Field indicators for pinpointing which proposal chip or attribute is invalid.
 */
enum class InvalidField {
    DATE,
    TIME,
    TITLE,
    HABIT,
    ROUTINE
}

/**
 * Sealed interface representing the outcome of command validation (ARCH-3, FR-7.6, AD-1).
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(
        val reason: ValidationFailureReason,
        val message: String,
        val invalidField: InvalidField? = null
    ) : ValidationResult
}
