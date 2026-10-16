package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a focus distraction intervention rule (FR-4.3, FR-10.1, AD-1).
 */
data class InterventionRule(
    val packageName: String,
    val appDisplayName: String,
    val isStrictBlocked: Boolean = true,
    val dailyLimitMinutes: Int? = null
)
