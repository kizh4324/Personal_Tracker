package com.personaltracker.platform.timer

import com.personaltracker.domain.model.FocusIntervalType

/**
 * Immutable state model representing a study/focus session timer countdown (FR-4.1, AD-1).
 */
data class StudySessionTimerState(
    val sessionId: String? = null,
    val subjectTag: String = "Focus",
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK,
    val bankedMinutes: Int = ((totalSeconds - remainingSeconds) / 60).toInt().coerceAtLeast(0)
) {
    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}
