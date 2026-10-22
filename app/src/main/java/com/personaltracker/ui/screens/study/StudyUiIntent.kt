package com.personaltracker.ui.screens.study

/**
 * Sealed interface representing all user actions on the Study screen (AD-2).
 */
sealed interface StudyUiIntent {
    data class StartSession(val subjectTag: String = "Focus", val durationMinutes: Int = 25) : StudyUiIntent
    object PauseSession : StudyUiIntent
    object ResumeSession : StudyUiIntent
    object StopSession : StudyUiIntent
    data class SelectSubjectTag(val tag: String) : StudyUiIntent
    data class SelectDuration(val minutes: Int) : StudyUiIntent
    object Refresh : StudyUiIntent
}
