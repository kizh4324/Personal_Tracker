package com.personaltracker.ui.screens.study

import com.personaltracker.domain.model.FocusIntervalType

/**
 * Sealed interface representing all user actions on the Study screen (AD-2).
 */
sealed interface StudyUiIntent {
    data class StartSession(
        val subjectTag: String = "Focus",
        val durationMinutes: Int = 25,
        val intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK
    ) : StudyUiIntent
    object PauseSession : StudyUiIntent
    object ResumeSession : StudyUiIntent
    object StopSession : StudyUiIntent
    data class SelectSubjectTag(val tag: String) : StudyUiIntent
    data class SelectDuration(val minutes: Int) : StudyUiIntent
    data class SelectIntervalType(val intervalType: FocusIntervalType) : StudyUiIntent
    data class SelectHeatmapCell(val date: String) : StudyUiIntent
    object Refresh : StudyUiIntent
}
