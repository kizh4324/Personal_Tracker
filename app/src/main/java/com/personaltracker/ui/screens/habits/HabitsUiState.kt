package com.personaltracker.ui.screens.habits

import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.model.Habit
import com.personaltracker.ui.components.cards.VerticalBarMetric

/**
 * Immutable UI state for the Habits screen (AD-2, AD-4).
 */
data class HabitsUiState(
    val habits: List<HabitItemUiState> = emptyList(),
    val totalActiveStreaks: Int = 0,
    val capsuleMetrics: List<VerticalBarMetric> = emptyList(),
    val consistencyPercentage: Int = 0,
    val bestStreak: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Individual habit item state with calculated flame and today's status.
 */
data class HabitItemUiState(
    val habit: Habit,
    val isCompletedToday: Boolean = false,
    val flameState: FlameState = FlameState.COLD
)
