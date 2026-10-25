package com.personaltracker.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.ui.components.atoms.DayCapsuleStrip
import com.personaltracker.ui.components.cards.AdaptiveRoutineStepCard
import com.personaltracker.ui.components.cards.HabitStreakCard
import com.personaltracker.ui.components.cards.HeroActionCard
import com.personaltracker.ui.components.cards.RoutineStepStatus
import com.personaltracker.ui.components.cards.TaskIntensityCard
import com.personaltracker.ui.components.overlays.CarryForwardReviewDialog
import com.personaltracker.ui.components.overlays.DayTypeSelectorDialog
import com.personaltracker.ui.components.overlays.TaskEditorSheet
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Home (AD-2 Figma-Ready Split).
 * Collects immutable [HomeUiState] from [HomeViewModel] and routes transient effects.
 */
@Composable
fun HomeRoute(
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeUiEffect.NavigateTo -> onNavigate?.invoke(effect.destination)
                is HomeUiEffect.ShowSnackbar -> {
                    // Handled at Scaffold level snackbar host or local snackbar
                }
                is HomeUiEffect.ShowCoinReward -> {
                    rewardStage.dispatchCoinReward(effect.startOffset, effect.coinCount)
                }
                is HomeUiEffect.ShowLottieReward -> {
                    rewardStage.dispatchLottieReward(effect.type)
                }
                is HomeUiEffect.DayTypeChanged -> {
                    // Handled reactively through uiState stream
                }
            }
        }
    }

    HomeScreenContent(
        state = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * Stateless Home Screen content composable.
 * Sourced from Story 1.5 & Story 1.7 specifications:
 * 1. Hero Action Card priority hierarchy (6-state dynamic priority)
 * 2. Today's Delivery Intensity Tasks (Urgent/Important/Routine)
 * 3. Daily Habit Streaks (with 4-stage flame badge)
 * 4. Adaptive Routine Steps with mid-day recalculation and non-punishing progression
 */
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    val taskOffsets = remember { mutableStateMapOf<String, Offset>() }
    val habitOffsets = remember { mutableStateMapOf<String, Offset>() }
    val routineOffsets = remember { mutableStateMapOf<String, Offset>() }
    var defaultTaskOffset by remember { mutableStateOf(Offset.Zero) }

    DayTypeSelectorDialog(
        isOpen = state.isDayTypeSelectorOpen,
        availableDayTypes = state.availableDayTypes,
        activeDayTypeId = state.activeDayTypeId,
        isOverrideActive = state.isDayOverrideActive,
        onSelectDayType = { dayTypeId ->
            onIntent(HomeUiIntent.SelectDayType(dayTypeId))
        },
        onResetOverride = {
            onIntent(HomeUiIntent.ResetDayTypeOverride)
        },
        onDismiss = {
            onIntent(HomeUiIntent.DismissDayTypeSelector)
        }
    )

    CarryForwardReviewDialog(
        isOpen = state.isCarryForwardDialogOpen,
        overdueTasks = state.overdueTasks,
        onCarryForwardAll = {
            onIntent(HomeUiIntent.CarryForwardAllOverdue)
        },
        onDismissAllWithoutGuilt = {
            onIntent(HomeUiIntent.DismissAllOverdue)
        },
        onDismissRequest = {
            onIntent(HomeUiIntent.DismissCarryForwardDialog)
        }
    )

    TaskEditorSheet(
        isOpen = state.isTaskEditorOpen,
        task = state.editingTask,
        isSoftCapReached = state.isUrgencySoftCapReached,
        activeUrgentTaskCount = state.activeUrgentTaskCount,
        onSave = { task ->
            onIntent(HomeUiIntent.SaveTaskItem(task))
        },
        onDelete = { taskId ->
            onIntent(HomeUiIntent.DeleteTaskItem(taskId))
        },
        onDismiss = {
            onIntent(HomeUiIntent.DismissTaskEditor)
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = spacing.screenPaddingHorizontal,
                    vertical = spacing.screenPaddingVertical
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Greeting Row (Figma Frame 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Morning, Budi",
                        style = typography.greetingTitle,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing0_5))
                    Text(
                        text = "Thursday, 10 March, 2025",
                        style = typography.bodySecondary,
                        color = colors.inkSecondary
                    )
                }

                Surface(
                    shape = shapes.full,
                    color = colors.bannerPeach,
                    modifier = Modifier.size(spacing.minTouchTarget)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🦊",
                            style = typography.title
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing5))

            // 7-Day Horizontal Capsule Strip (Framer stiffness 380, damping 28)
            DayCapsuleStrip(
                selectedDate = 10,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.spacing5))

            // Hero Action Card (Priority Machine & Reminder Banner)
            HeroActionCard(
                state = state.heroCardState,
                modifier = Modifier.fillMaxWidth()
            )

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Section: Today's High-Intensity Tasks
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Focus Tasks",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing1))
                    Text(
                        text = "Tap checkmark to complete and earn coins",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                }

                TextButton(
                    onClick = { onIntent(HomeUiIntent.OpenTaskEditor(null)) },
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new task",
                        tint = colors.primary,
                        modifier = Modifier.size(spacing.spacing4)
                    )
                    Spacer(modifier = Modifier.width(spacing.spacing1))
                    Text(
                        text = "Add",
                        style = typography.label,
                        color = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.spacing3))

            if (state.focusTasks.isEmpty()) {
                // Default demonstration task if database has no tasks for today
                TaskIntensityCard(
                    title = "Prepare System Design Presentation",
                    deliveryIntensity = "URGENT",
                    scheduledTime = "3:00 PM",
                    isCompleted = false,
                    onToggleCompletion = { /* Guarded: demo item has no backing database entity */ },
                    onClick = { onIntent(HomeUiIntent.OpenTaskEditor(null)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            defaultTaskOffset = coordinates.positionInWindow()
                        }
                )
            } else {
                state.focusTasks.forEach { task ->
                    val isDone = task.state.equals("COMPLETED", ignoreCase = true)
                    TaskIntensityCard(
                        title = task.title,
                        deliveryIntensity = task.deliveryIntensity,
                        scheduledTime = task.scheduledTime ?: "Today",
                        isCompleted = isDone,
                        onToggleCompletion = {
                            val offset = taskOffsets[task.id] ?: Offset.Zero
                            onIntent(HomeUiIntent.ToggleTask(task.id, offset))
                        },
                        onClick = {
                            onIntent(HomeUiIntent.OpenTaskEditor(task))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                            .onGloballyPositioned { coordinates ->
                                taskOffsets[task.id] = coordinates.positionInWindow()
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Section: Active Habit Streaks
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Daily Habits",
                style = typography.title,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = "4-stage flame visualizer with non-punishing freeze protection",
                style = typography.caption,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spacing3))

            if (state.habits.isEmpty()) {
                // Default demonstration habit if no habits logged (guarded no-op)
                HabitStreakCard(
                    name = "Morning Meditation & Journaling",
                    currentStreak = 14,
                    cadence = "DAILY",
                    isFreezeActive = false,
                    isCompleted = false,
                    onToggleCompletion = { /* Guarded: demo item has no backing database entity */ },
                    onClick = { /* Guarded: demo item has no backing database entity */ },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                state.habits.forEach { habitState ->
                    HabitStreakCard(
                        name = habitState.habit.name,
                        currentStreak = habitState.habit.currentStreak,
                        cadence = habitState.habit.targetDaysOfWeek,
                        isFreezeActive = habitState.habit.isFreezeActive,
                        isCompleted = habitState.isCompletedToday,
                        onToggleCompletion = {
                            val offset = habitOffsets[habitState.habit.id] ?: Offset.Zero
                            onIntent(HomeUiIntent.ToggleHabit(habitState.habit.id, offset))
                        },
                        onClick = {
                            val offset = habitOffsets[habitState.habit.id] ?: Offset.Zero
                            onIntent(HomeUiIntent.ToggleHabit(habitState.habit.id, offset))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                            .onGloballyPositioned { coordinates ->
                                habitOffsets[habitState.habit.id] = coordinates.positionInWindow()
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Section: Adaptive Routine Steps & Mid-Day Recalculation
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.activeRoutine?.title ?: "Active Routine",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing1))
                    if (state.routineProgressSummary.isNotBlank()) {
                        Surface(
                            shape = shapes.full,
                            color = colors.surfaceRaised,
                            border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                            modifier = Modifier.padding(top = spacing.spacing0_5)
                        ) {
                            Text(
                                text = state.routineProgressSummary,
                                style = typography.caption,
                                color = colors.inkSecondary,
                                modifier = Modifier.padding(horizontal = spacing.spacing2, vertical = spacing.spacing0_5)
                            )
                        }
                    } else {
                        Text(
                            text = "Step-by-step guidance dynamically adapted to today's schedule",
                            style = typography.caption,
                            color = colors.inkSecondary
                        )
                    }
                }

                TextButton(
                    onClick = { onIntent(HomeUiIntent.ShiftRoutineToNow) },
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Shift routine to current time",
                        tint = colors.primary,
                        modifier = Modifier.size(spacing.spacing4)
                    )
                    Spacer(modifier = Modifier.width(spacing.spacing1))
                    Text(
                        text = "Shift to Now",
                        style = typography.label,
                        color = colors.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.spacing3))

            if (state.isRoutineCompletedToday) {
                // Celebratory completion banner with companion styling
                Surface(
                    shape = shapes.lg,
                    color = colors.companionSubtle,
                    border = BorderStroke(spacing.spacing0_5, colors.companion),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.spacing4),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Routine completed for today! 15 coins earned.",
                                style = typography.body,
                                color = colors.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(spacing.spacing0_5))
                            Text(
                                text = "Great momentum. Companion celebrated your consistency.",
                                style = typography.caption,
                                color = colors.inkSecondary
                            )
                        }

                        TextButton(
                            onClick = { onIntent(HomeUiIntent.ResetRoutineProgress) },
                            modifier = Modifier.defaultMinSize(
                                minWidth = spacing.minTouchTarget,
                                minHeight = spacing.minTouchTarget
                            )
                        ) {
                            Text(
                                text = "Reset",
                                style = typography.label,
                                color = colors.companion
                            )
                        }
                    }
                }
            } else if (state.routineTimelineSteps.isNotEmpty()) {
                state.routineTimelineSteps.forEach { stepState ->
                    AdaptiveRoutineStepCard(
                        stepOrder = stepState.step.orderIndex,
                        title = stepState.step.title,
                        durationMinutes = stepState.step.durationMinutes,
                        status = stepState.status,
                        timeSlot = stepState.timeSlot,
                        onAdvanceStep = {
                            val offset = routineOffsets[stepState.step.id] ?: Offset.Zero
                            onIntent(HomeUiIntent.AdvanceRoutineStep(stepState.step.id, offset))
                        },
                        onSkip = {
                            onIntent(HomeUiIntent.SkipRoutineStep(stepState.step.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                            .onGloballyPositioned { coordinates ->
                                routineOffsets[stepState.step.id] = coordinates.positionInWindow()
                            }
                    )
                }
            } else {
                val routineStep = state.activeRoutineStep
                if (routineStep != null) {
                    AdaptiveRoutineStepCard(
                        stepOrder = routineStep.step.orderIndex,
                        title = routineStep.step.title,
                        durationMinutes = routineStep.step.durationMinutes,
                        status = routineStep.status,
                        onAdvanceStep = {
                            onIntent(HomeUiIntent.AdvanceRoutineStep(routineStep.step.id))
                        },
                        onSkip = {
                            onIntent(HomeUiIntent.SkipRoutineStep(routineStep.step.id))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        shape = shapes.lg,
                        color = colors.surfaceSunken,
                        border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No routine scheduled for this template.",
                            style = typography.caption,
                            color = colors.inkTertiary,
                            modifier = Modifier.padding(spacing.spacing4)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }

    // Floating Quick-Capture `+` FAB (Deep Espresso #59280E)
    Surface(
        shape = shapes.full,
        color = colors.primaryDark,
        shadowElevation = elevation.high,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = spacing.spacing4, bottom = spacing.spacing4)
            .size(spacing.fabSize)
            .clickable(
                role = Role.Button,
                onClick = { onIntent(HomeUiIntent.OpenTaskEditor(null)) }
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick capture new task or habit",
                tint = colors.surfaceRaised,
                modifier = Modifier.size(spacing.spacing6)
            )
        }
    }
}
}
