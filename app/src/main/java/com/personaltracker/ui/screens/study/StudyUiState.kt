package com.personaltracker.ui.screens.study

import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.HeatmapCell
import com.personaltracker.domain.model.StudyHeatmapData
import com.personaltracker.domain.model.StudySession
import com.personaltracker.platform.timer.StudySessionTimerState

/**
 * Single immutable UI state representing the Study focus session surface (AD-2).
 */
data class StudyUiState(
    val timerState: StudySessionTimerState = StudySessionTimerState(),
    val selectedSubjectTag: String = "Focus",
    val selectedDurationMinutes: Int = 25,
    val selectedIntervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK,
    val completedCycles: Int = 0,
    val heatmapData: StudyHeatmapData = StudyHeatmapData(),
    val selectedCellDate: String? = null,
    val todayTotalStudyMinutes: Int = 0,
    val recentSessions: List<StudySession> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCell: HeatmapCell?
        get() = selectedCellDate?.let { date -> heatmapData.cells.find { it.date == date } }
}
