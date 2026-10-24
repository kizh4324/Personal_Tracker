package com.personaltracker.ui.screens.home

import androidx.compose.ui.geometry.Offset
import com.personaltracker.domain.model.Task

/**
 * Sealed interface representing all explicit user interactions on the Home screen (AD-2).
 */
sealed interface HomeUiIntent {
    data class ToggleTask(val taskId: String, val startOffset: Offset = Offset.Zero) : HomeUiIntent
    data class ToggleHabit(val habitId: String, val startOffset: Offset = Offset.Zero) : HomeUiIntent
    data class AdvanceRoutineStep(val stepId: String) : HomeUiIntent
    object PauseActiveFocus : HomeUiIntent
    object ResumeActiveFocus : HomeUiIntent
    object EndActiveFocus : HomeUiIntent
    data class ResumeInterruptedTask(val taskId: String) : HomeUiIntent
    object ReviewCarryForward : HomeUiIntent
    object DismissMorningBanner : HomeUiIntent
    object SwapDayType : HomeUiIntent
    object DayTypeClicked : HomeUiIntent
    object OpenDayTypeSelector : HomeUiIntent
    object DismissDayTypeSelector : HomeUiIntent
    data class SelectDayType(val dayTypeId: String) : HomeUiIntent
    object ResetDayTypeOverride : HomeUiIntent
    data class SnoozeHeroItem(val taskId: String) : HomeUiIntent
    data class SkipHeroItem(val taskId: String) : HomeUiIntent
    data class StartHeroItem(val taskId: String, val startOffset: Offset = Offset.Zero) : HomeUiIntent
    object OpenCarryForwardReview : HomeUiIntent
    object DismissCarryForwardDialog : HomeUiIntent
    object CarryForwardAllOverdue : HomeUiIntent
    object DismissAllOverdue : HomeUiIntent
    data class OpenTaskEditor(val task: Task? = null) : HomeUiIntent
    object DismissTaskEditor : HomeUiIntent
    data class SaveTaskItem(val task: Task) : HomeUiIntent
    data class DeleteTaskItem(val taskId: String) : HomeUiIntent
    object Refresh : HomeUiIntent
}
