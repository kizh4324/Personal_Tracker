package com.personaltracker.ui.screens.study

import com.personaltracker.domain.model.StudySession
import com.personaltracker.platform.timer.StudySessionTimerState

/**
 * Single immutable UI state representing the Study focus session surface (AD-2).
 */
data class StudyUiState(
    val timerState: StudySessionTimerState = StudySessionTimerState(),
    val selectedSubjectTag: String = "Focus",
    val selectedDurationMinutes: Int = 25,
    val todayTotalStudyMinutes: Int = 0,
    val recentSessions: List<StudySession> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
