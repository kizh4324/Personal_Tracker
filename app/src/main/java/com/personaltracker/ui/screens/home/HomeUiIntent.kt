package com.personaltracker.ui.screens.home

import androidx.compose.ui.geometry.Offset

/**
 * Sealed interface representing all explicit user interactions on the Home screen (AD-2).
 */
sealed interface HomeUiIntent {
    data class ToggleTask(val taskId: String, val startOffset: Offset = Offset.Zero) : HomeUiIntent
    data class ToggleHabit(val habitId: String, val startOffset: Offset = Offset.Zero) : HomeUiIntent
    data class AdvanceRoutineStep(val stepId: String) : HomeUiIntent
    object PauseActiveFocus : HomeUiIntent
    object EndActiveFocus : HomeUiIntent
    data class ResumeInterruptedTask(val taskId: String) : HomeUiIntent
    object ReviewCarryForward : HomeUiIntent
    object DismissMorningBanner : HomeUiIntent
    object SwapDayType : HomeUiIntent
    object DayTypeClicked : HomeUiIntent
    object Refresh : HomeUiIntent
}
