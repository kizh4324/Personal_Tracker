package com.personaltracker.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.cards.AdaptiveRoutineStepCard
import com.personaltracker.ui.components.cards.HabitStreakCard
import com.personaltracker.ui.components.cards.HeroActionCard
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus
import com.personaltracker.ui.components.cards.TaskIntensityCard
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Home Screen.
 *
 * Integrates the atomic component library and interactive animation pipeline:
 * 1. State-Aware Hero Card (`HeroActionCard`) managing 6 priority states.
 * 2. Task Intensity Card with delivery intensity stripes (Urgent/Important/Routine)
 *    triggering hardware VSYNC-synced Bézier [CoinArcAnimation] and Lottie micro-bursts upon completion.
 * 3. Habit Streak Card with 4-state [StreakFlameBadge] and freeze protection.
 * 4. Adaptive Routine Step Card with active step progression.
 *
 * Conforms 100% to AD-7 (design tokens only) and AD-4 (non-punishing zero guilt copy).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val rewardMotionStage = LocalRewardMotionStage.current

    // State for interactive Hero Card demonstration
    var heroState by remember {
        mutableStateOf<HeroCardState>(
            HeroCardState.ActiveFocus(
                title = "Deep Work: Architectural Review",
                countdownFormatted = "23:40",
                onPause = {},
                onEndSession = {}
            )
        )
    }

    // Task completion states and coordinate tracking for Bézier coin arcs
    var taskCompleted by remember { mutableStateOf(false) }
    var taskCardOffset by remember { mutableStateOf(Offset.Zero) }

    // Habit completion state
    var habitCompleted by remember { mutableStateOf(false) }
    var habitStreakCount by remember { mutableIntStateOf(14) }

    // Routine step completion
    var routineStepStatus by remember { mutableStateOf(RoutineStepStatus.ACTIVE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPaddingHorizontal, vertical = spacing.screenPaddingVertical),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Action Card (Priority Machine)
        HeroActionCard(
            state = heroState,
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

            val toggleTask = {
                taskCompleted = !taskCompleted
                if (taskCompleted) {
                    // Dispatch Bézier arc to HUD and Lottie micro-burst
                    rewardMotionStage.dispatchCoinReward(
                        startOffset = taskCardOffset,
                        coinCount = 5
                    )
                    rewardMotionStage.dispatchLottieReward(RewardAnimationType.CoinEarnMicro)
                }
            }

            TaskIntensityCard(
                title = "Prepare System Design Presentation",
                deliveryIntensity = "URGENT",
                scheduledTime = "3:00 PM",
                isCompleted = taskCompleted,
                onToggleCompletion = toggleTask,
                onClick = toggleTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        taskCardOffset = coordinates.positionInWindow()
                    }
            )
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

            val toggleHabit = {
                habitCompleted = !habitCompleted
                if (habitCompleted) {
                    habitStreakCount += 1
                    rewardMotionStage.dispatchLottieReward(RewardAnimationType.CoinEarnMicro)
                } else {
                    habitStreakCount = maxOf(0, habitStreakCount - 1)
                }
            }

            HabitStreakCard(
                name = "Morning Meditation & Journaling",
                currentStreak = habitStreakCount,
                cadence = "DAILY",
                isFreezeActive = false,
                isCompleted = habitCompleted,
                onToggleCompletion = toggleHabit,
                onClick = toggleHabit,
                modifier = Modifier.fillMaxWidth()
            )
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

            AdaptiveRoutineStepCard(
                stepOrder = 2,
                title = "Review Critical Pull Requests",
                durationMinutes = 15,
                status = routineStepStatus,
                onAdvanceStep = {
                    routineStepStatus = if (routineStepStatus == RoutineStepStatus.ACTIVE) {
                        RoutineStepStatus.COMPLETED
                    } else {
                        RoutineStepStatus.ACTIVE
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
