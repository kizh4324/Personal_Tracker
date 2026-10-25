package com.personaltracker.ui.screens.home

import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus

/**
 * Single immutable UI state representing the entire Home screen surface (AD-2).
 */
data class HomeUiState(
    val activeDayTypeName: String = "Standard Routine",
    val activeDayTypeId: String? = null,
    val activeDayType: DayType? = null,
    val availableDayTypes: List<DayType> = emptyList(),
    val isDayOverrideActive: Boolean = false,
    val isDayTypeSelectorOpen: Boolean = false,
    val isCarryForwardDialogOpen: Boolean = false,
    val isTaskEditorOpen: Boolean = false,
    val editingTask: Task? = null,
    val activeUrgentTaskCount: Int = 0,
    val isUrgencySoftCapReached: Boolean = false,
    val streakCount: Int = 0,
    val flameState: FlameState = FlameState.COLD,
    val coinBalance: Int = 0,
    val heroCardState: HeroCardState = HeroCardState.IntentionalIdle(),
    val focusTasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val habits: List<HomeHabitItemState> = emptyList(),
    val activeRoutine: Routine? = null,
    val activeRoutineStep: HomeRoutineStepState? = null,
    val routineTimelineSteps: List<HomeScheduledStepState> = emptyList(),
    val routineProgressSummary: String = "",
    val isRoutineCompletedToday: Boolean = false,
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

/**
 * Scheduled routine step state representing timeline progression slot (AD-6).
 */
data class HomeScheduledStepState(
    val step: RoutineStep,
    val timeSlot: String,
    val status: RoutineStepStatus,
    val isCurrentCursor: Boolean
)
