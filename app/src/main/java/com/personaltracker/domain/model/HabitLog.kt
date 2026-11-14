package com.personaltracker.domain.model

/**
 * Canonical statuses for daily habit logs (FR-2.4, AD-1, AD-4).
 */
object HabitLogStatus {
    const val COMPLETED = "COMPLETED"
    const val MISSED = "MISSED"
    const val SLACK_USED = "SLACK_USED"
    const val FROZEN = "FROZEN"
    const val GRACE_SHIELD_ABSORBED = "GRACE_SHIELD_ABSORBED"
    const val REST_DAY = "REST_DAY"
    const val DAY_TYPE_IMMUNITY = "DAY_TYPE_IMMUNITY"
}

/**
 * Pure Kotlin domain model representing a single-day habit execution log (FR-2.4, AD-1).
 */
data class HabitLog(
    val id: String,
    val habitId: String,
    val logDate: String, // "YYYY-MM-DD"
    val status: String, // See [HabitLogStatus]
    val loggedTimestamp: Long = System.currentTimeMillis()
)
