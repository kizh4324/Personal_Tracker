package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing an ordered step within a routine (FR-2.3, AD-1).
 */
data class RoutineStep(
    val id: String,
    val routineId: String,
    val orderIndex: Int,
    val title: String,
    val durationMinutes: Int
)
