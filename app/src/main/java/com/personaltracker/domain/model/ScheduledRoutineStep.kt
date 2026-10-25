package com.personaltracker.domain.model

/**
 * Pure Kotlin presentation/execution domain model representing a routine step with resolved sequential time slots (AD-1, AD-6).
 */
data class ScheduledRoutineStep(
    val step: RoutineStep,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isCurrentCursor: Boolean = false
)
