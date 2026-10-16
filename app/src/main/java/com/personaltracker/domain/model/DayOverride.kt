package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a specific single-date schedule override (FR-3.1, AD-1).
 */
data class DayOverride(
    val date: String, // ISO-8601: "YYYY-MM-DD"
    val dayTypeId: String,
    val isUserModified: Boolean,
    val createdTimestamp: Long
)
