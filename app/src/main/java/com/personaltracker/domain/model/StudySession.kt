package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a completed or interrupted study/focus block (FR-2.5, FR-4.1, AD-1).
 */
data class StudySession(
    val id: String,
    val subjectTag: String,
    val sessionDate: String, // "YYYY-MM-DD"
    val startTime: String, // "HH:mm"
    val durationMinutes: Int,
    val coinsEarned: Int = 0,
    val linkedTaskId: String? = null,
    val isInterrupted: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val targetDurationMinutes: Int = durationMinutes,
    val actualElapsedMinutes: Int = durationMinutes,
    val intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK
) {
    val isBreak: Boolean
        get() = intervalType.isBreak
}
