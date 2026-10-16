package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a habit with non-punitive streak tracking (FR-2.4, AD-1, AD-4).
 */
data class Habit(
    val id: String,
    val name: String,
    val description: String? = null,
    val targetDaysOfWeek: String, // e.g. "1,2,3,4,5,6,7"
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val slackBankDays: Int = 0,
    val isFreezeActive: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
