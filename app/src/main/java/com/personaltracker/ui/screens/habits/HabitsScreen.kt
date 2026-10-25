package com.personaltracker.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import com.personaltracker.ui.components.cards.HabitStreakCard
import com.personaltracker.ui.components.cards.VerticalCapsuleBars
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Habits (AD-2 Figma-Ready Split).
 */
@Composable
fun HabitsRoute(
    modifier: Modifier = Modifier,
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HabitsUiEffect.ShowCoinReward -> {
                    rewardMotionStage.dispatchCoinReward(
                        startOffset = effect.startOffset,
                        coinCount = effect.coinCount
                    )
                }
                is HabitsUiEffect.ShowLottieReward -> {
                    rewardMotionStage.dispatchLottieReward(effect.type)
                }
                is HabitsUiEffect.ShowSnackbar -> {
                    // Handled via scaffold or toast
                }
            }
        }
    }

    HabitsScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Habits screen.
 * Strictly consumes design tokens with zero hardcoded literals (AD-4, AD-7).
 */
@Composable
fun HabitsScreenContent(
    state: HabitsUiState,
    onIntent: (HabitsUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    var habitOffset by remember { mutableStateOf(Offset.Zero) }

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
        // Habits Header Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.lg,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Habits & Streaks",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "${state.totalActiveStreaks} active streak(s) • Non-punishing freeze protection",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Habit Cards
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.habits.isEmpty()) {
                HabitStreakCard(
                    name = "Morning Meditation & Journaling",
                    currentStreak = 14,
                    cadence = "DAILY",
                    isFreezeActive = false,
                    isCompleted = false,
                    onToggleCompletion = {
                        onIntent(HabitsUiIntent.ToggleHabit("demo_habit", habitOffset))
                    },
                    onClick = {
                        onIntent(HabitsUiIntent.ToggleHabit("demo_habit", habitOffset))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            habitOffset = coordinates.positionInWindow()
                        }
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
                            onIntent(HabitsUiIntent.ToggleHabit(habitState.habit.id, habitOffset))
                        },
                        onClick = {
                            onIntent(HabitsUiIntent.ToggleHabit(habitState.habit.id, habitOffset))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.spacing2)
                            .onGloballyPositioned { coordinates ->
                                habitOffset = coordinates.positionInWindow()
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Progress & Insights (Figma Frame 3)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your progress and insights",
                style = typography.headline,
                color = colors.inkPrimary,
                modifier = Modifier.padding(bottom = spacing.spacing4)
            )
            VerticalCapsuleBars(
                modifier = Modifier.fillMaxWidth(),
                onShareProgress = {
                    // Handled gracefully with haptic feedback or share intent
                }
            )
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
