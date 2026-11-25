package com.personaltracker.domain.model

/**
 * Configurable pure Kotlin policy governing the non-blocking daily active Urgent task soft cap (AD-1, AD-4).
 */
data class UrgencyScarcityPolicy(
    val maxActiveUrgent: Int = DEFAULT_MAX_ACTIVE_URGENT
) {
    init {
        require(maxActiveUrgent in MIN_MAX_ACTIVE_URGENT..MAX_MAX_ACTIVE_URGENT) {
            "maxActiveUrgent must be between $MIN_MAX_ACTIVE_URGENT and $MAX_MAX_ACTIVE_URGENT"
        }
    }

    companion object {
        const val DEFAULT_MAX_ACTIVE_URGENT = 2
        const val MIN_MAX_ACTIVE_URGENT = 1
        const val MAX_MAX_ACTIVE_URGENT = 5
    }
}
