package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a routine template (FR-2.3, AD-1).
 */
data class Routine(
    val id: String,
    val dayTypeId: String,
    val title: String,
    val targetStartTime: String, // "HH:mm"
    val totalEstimatedMinutes: Int,
    val steps: List<RoutineStep> = emptyList()
)
