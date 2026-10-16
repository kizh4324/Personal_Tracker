package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a baseline schedule template (FR-3, AD-1).
 */
data class DayType(
    val id: String,
    val name: String,
    val colorHex: String,
    val targetStudyMinutes: Int,
    val isDefault: Boolean,
    val defaultDaysOfWeek: String
)
