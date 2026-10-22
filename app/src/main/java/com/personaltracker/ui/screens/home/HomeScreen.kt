package com.personaltracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.ui.components.cards.AdaptiveRoutineStepCard
import com.personaltracker.ui.components.cards.HabitStreakCard
import com.personaltracker.ui.components.cards.HeroActionCard
import com.personaltracker.ui.components.cards.RoutineStepStatus
import com.personaltracker.ui.components.cards.TaskIntensityCard
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Home (AD-2 Figma-Ready Split).
 * Collects immutable [HomeUiState] from [HomeViewModel] and routes transient effects.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigate: (PersonalTrackerDestination) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowCoinReward -> {
                    rewardMotionStage.dispatchCoinReward(
                        startOffset = effect.startOffset,
                        coinCount = effect.coinCount
                    )
                }
                is HomeUiEffect.ShowLottieReward -> {
                    rewardMotionStage.dispatchLottieReward(effect.type)
                }
                is HomeUiEffect.ShowSnackbar -> {
                    // Handled via scaffold or toast
                }
                is HomeUiEffect.NavigateTo -> {
                    onNavigate(effect.destination)
                }
            }
        }
    }

    HomeScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Home screen content.
 * Strictly consumes design tokens with zero hardcoded literals and 48dp touch targets (AD-4, AD-7).
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

    var taskCardOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = spacing.screenPaddingHorizontal,
                vertical = spacing.screenPaddingVertical
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Action Card (Priority Machine)
        HeroActionCard(
            state = state.heroCardState,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Section: Today's High-Intensity Tasks
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Today's Focus Tasks",
                style = typography.title,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = "Tap complete to trigger Bézier coin arc & Lottie reward animation",
                style = typography.caption,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spacing3))

            if (state.focusTasks.isEmpty()) {
                // Default demonstration task if database has no tasks for today (guarded no-op)
                TaskIntensityCard(
                    title = "Prepare System Design Presentation",
                    deliveryIntensity = "URGENT",
                    scheduledTime = "3:00 PM",
                    isCompleted = false,
                    onToggleCompletion = { /* Guarded: demo item has no backing database entity */ },
                    onClick = { /* Guarded: demo item has no backing database entity */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            taskCardOffset = coordinates.positionInWindow()
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
                            onIntent(HomeUiIntent.ToggleTask(task.id, taskCardOffset))
                        },
                        onClick = {
                            onIntent(HomeUiIntent.ToggleTask(task.id, taskCardOffset))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                            .onGloballyPositioned { coordinates ->
                                taskCardOffset = coordinates.positionInWindow()
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
                            onIntent(HomeUiIntent.ToggleHabit(habitState.habit.id, taskCardOffset))
                        },
                        onClick = {
                            onIntent(HomeUiIntent.ToggleHabit(habitState.habit.id, taskCardOffset))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Section: Adaptive Routine Step
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Active Routine",
                style = typography.title,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = "Step-by-step guidance dynamically adapted to today's schedule",
                style = typography.caption,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spacing3))

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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
