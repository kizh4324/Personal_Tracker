package com.personaltracker.ui.screens.home

import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus

/**
 * Single immutable UI state representing the entire Home screen surface (AD-2).
 */
data class HomeUiState(
    val activeDayTypeName: String = "Weekday Routine",
    val activeDayTypeId: String? = null,
    val streakCount: Int = 0,
    val flameState: FlameState = FlameState.COLD,
    val coinBalance: Int = 0,
    val heroCardState: HeroCardState = HeroCardState.IntentionalIdle(),
    val focusTasks: List<Task> = emptyList(),
    val habits: List<HomeHabitItemState> = emptyList(),
    val activeRoutineStep: HomeRoutineStepState? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Habit item state representing streak and today's completion status.
 */
data class HomeHabitItemState(
    val habit: Habit,
    val isCompletedToday: Boolean = false,
    val flameState: FlameState = FlameState.COLD
)

/**
 * Routine step item state representing sequential step progress.
 */
data class HomeRoutineStepState(
    val step: RoutineStep,
    val status: RoutineStepStatus = RoutineStepStatus.ACTIVE
)
