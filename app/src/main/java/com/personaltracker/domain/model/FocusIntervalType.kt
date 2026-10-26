package com.personaltracker.domain.model

/**
 * Pure Kotlin enum classifying study and focus intervals (FR-4.1, AD-1).
 * Supports Deep Work blocks, Pomodoro cycles, and structured breaks.
 */
enum class FocusIntervalType(
    val displayName: String,
    val isBreak: Boolean,
    val defaultDurationMinutes: Int
) {
    DEEP_WORK("Deep Work", isBreak = false, defaultDurationMinutes = 45),
    POMODORO_WORK("Pomodoro", isBreak = false, defaultDurationMinutes = 25),
    SHORT_BREAK("Short Break", isBreak = true, defaultDurationMinutes = 5),
    LONG_BREAK("Long Break", isBreak = true, defaultDurationMinutes = 15),
    REVIEW("Review", isBreak = false, defaultDurationMinutes = 20)
}
