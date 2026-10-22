package com.personaltracker.ui.screens.habits

import androidx.compose.ui.geometry.Offset

/**
 * Sealed interface representing all user interactions on the Habits screen (AD-2).
 */
sealed interface HabitsUiIntent {
    data class ToggleHabit(val habitId: String, val startOffset: Offset = Offset.Zero) : HabitsUiIntent
    data class CreateHabit(val name: String, val cadence: String = "DAILY") : HabitsUiIntent
    object Refresh : HabitsUiIntent
}
