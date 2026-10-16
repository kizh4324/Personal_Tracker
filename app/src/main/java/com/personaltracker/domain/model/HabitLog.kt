package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a single-day habit execution log (FR-2.4, AD-1).
 */
data class HabitLog(
    val id: String,
    val habitId: String,
    val logDate: String, // "YYYY-MM-DD"
    val status: String, // "COMPLETED", "MISSED", "SLACK_USED", "FROZEN"
    val loggedTimestamp: Long = System.currentTimeMillis()
)
