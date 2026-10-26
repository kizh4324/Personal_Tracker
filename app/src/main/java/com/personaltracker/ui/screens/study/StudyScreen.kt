package com.personaltracker.ui.screens.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudyHeatmapData
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Study (AD-2 Figma-Ready Split).
 */
@Composable
fun StudyRoute(
    modifier: Modifier = Modifier,
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is StudyUiEffect.ShowLottieReward -> {
                    rewardMotionStage.dispatchLottieReward(effect.type)
                }
                is StudyUiEffect.ShowSessionCompleteCelebration -> {
                    // Handled with milestone celebration
                }
                is StudyUiEffect.ShowSnackbar -> {
                    // Handled via toast or snackbar
                }
            }
        }
    }

    StudyScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Study session timers, interval selection,
 * and 28-day contribution heatmap history (AD-1, AD-2).
 * Strictly consumes design tokens with zero hardcoded literals and 48dp touch targets (AD-4, AD-7).
 * Aligned 1:1 with Figma Frame 04_Study_Focus_Timer.
 */
@Composable
fun StudyScreenContent(
    state: StudyUiState,
    onIntent: (StudyUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val timer = state.timerState
    val durations = listOf(15, 25, 45, 60)
    val tags = listOf("Deep Work", "Pomodoro", "Flow State", "Reading")
    val intervalTypes = listOf(
        FocusIntervalType.DEEP_WORK,
        FocusIntervalType.POMODORO_WORK,
        FocusIntervalType.SHORT_BREAK,
        FocusIntervalType.LONG_BREAK,
        FocusIntervalType.REVIEW
    )

    val rawProgress = if (timer.totalSeconds > 0) {
        1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = AnimationTokens.timerPauseResumeSpringSpec(),
        label = "studyTimerProgress"
    )

    val dialSize = spacing.spacing16 * 3 + spacing.spacing12 // 240dp equivalent
    val strokeWidth = spacing.spacing2

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
        // Top Header Row matching 04_Study_Focus_Timer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "${state.selectedIntervalType.displayName} Session",
                style = typography.greetingTitle,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = when {
                    timer.isPaused -> "Paused • Take a breath"
                    timer.isRunning -> "In focus • Stay in the zone"
                    state.selectedIntervalType == FocusIntervalType.POMODORO_WORK && state.completedCycles > 0 ->
                        "Pomodoro • Cycle ${state.completedCycles}/4 • ${state.selectedDurationMinutes} min target"
                    else -> "${state.selectedSubjectTag} • ${state.selectedDurationMinutes} min target"
                },
                style = typography.bodySecondary,
                color = colors.inkSecondary
            )
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Hero Focus Timer Stage Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxxl,
            color = colors.surfaceRaised,
            shadowElevation = spacing.spacing0_5
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Focus Timer Ring with Canvas Progress Arc
                Box(
                    modifier = Modifier.size(dialSize),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = colors.surfaceSunken
                    val arcColor = colors.primary

                    Canvas(modifier = Modifier.size(dialSize)) {
                        val strokePx = strokeWidth.toPx()
                        val halfStroke = strokePx / 2f
                        val arcBounds = Size(size.width - strokePx, size.height - strokePx)

                        // 360-degree sand track
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = arcBounds,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // Animated progress arc
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                topLeft = Offset(halfStroke, halfStroke),
                                size = arcBounds,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Centered Counter and Status
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (timer.isRunning || timer.isPaused) {
                                timer.formattedRemainingTime
                            } else {
                                "%02d:00".format(state.selectedDurationMinutes)
                            },
                            style = typography.heroScore,
                            color = colors.inkPrimary
                        )

                        Spacer(modifier = Modifier.height(spacing.spacing1))

                        Text(
                            text = when {
                                timer.isPaused -> "PAUSED"
                                timer.isRunning -> if (state.selectedIntervalType.isBreak) "RESTING..." else "FOCUSING..."
                                else -> "READY"
                            },
                            style = typography.caption,
                            color = colors.inkSecondary
                        )

                        Spacer(modifier = Modifier.height(spacing.spacing2))

                        // Soft Peach Coin Reward Pill
                        Surface(
                            shape = shapes.full,
                            color = colors.iconSoftPeach,
                            modifier = Modifier.padding(horizontal = spacing.spacing2)
                        ) {
                            Text(
                                text = if (state.selectedIntervalType.isBreak) "Rest & Recharge" else "+10 Coins Reward",
                                style = typography.label,
                                color = colors.primary,
                                modifier = Modifier.padding(
                                    horizontal = spacing.spacing3,
                                    vertical = spacing.spacing0_5
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing6))

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!timer.isRunning && !timer.isPaused) {
                        Button(
                            onClick = {
                                onIntent(
                                    StudyUiIntent.StartSession(
                                        subjectTag = state.selectedSubjectTag,
                                        durationMinutes = state.selectedDurationMinutes,
                                        intervalType = state.selectedIntervalType
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.surfaceRaised
                            ),
                            shape = shapes.full,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.fabSize)
                        ) {
                            Text(
                                text = if (state.selectedIntervalType.isBreak) "Start Break" else "Start Focus",
                                style = typography.ctaText
                            )
                        }
                    } else {
                        if (timer.isPaused) {
                            Button(
                                onClick = { onIntent(StudyUiIntent.ResumeSession) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.surfaceRaised
                                ),
                                shape = shapes.full,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.fabSize)
                            ) {
                                Text(text = "Resume", style = typography.ctaText)
                            }
                        } else {
                            Button(
                                onClick = { onIntent(StudyUiIntent.PauseSession) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primaryDark,
                                    contentColor = colors.surfaceRaised
                                ),
                                shape = shapes.full,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.fabSize)
                            ) {
                                Text(text = "Pause Session", style = typography.ctaText)
                            }
                        }

                        OutlinedButton(
                            onClick = { onIntent(StudyUiIntent.StopSession) },
                            shape = shapes.full,
                            border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.inkPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = spacing.fabSize)
                        ) {
                            Text(text = "End Early", style = typography.label)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Interval Type, Duration & Tag Selection (visible when timer is idle)
        if (!timer.isRunning && !timer.isPaused) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.xxl,
                color = colors.surfaceRaised
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.spacing4)
                ) {
                    // 1. Focus Interval Type Selector Pills
                    Text(
                        text = "Focus Interval",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing2))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        intervalTypes.forEach { intervalType ->
                            val isSelected = state.selectedIntervalType == intervalType
                            Surface(
                                shape = shapes.full,
                                color = if (isSelected) colors.inkPrimary else colors.surfaceRaised,
                                border = if (isSelected) null else BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = spacing.spacing16, minHeight = spacing.minTouchTarget)
                                    .clip(shapes.full)
                                    .clickable(role = Role.Button) {
                                        onIntent(StudyUiIntent.SelectIntervalType(intervalType))
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.spacing3,
                                        vertical = spacing.spacing2
                                    )
                                ) {
                                    Text(
                                        text = intervalType.displayName,
                                        style = typography.label,
                                        color = if (isSelected) colors.surfaceRaised else colors.inkPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.spacing4))

                    // 2. Duration Selector Pills
                    Text(
                        text = "Session Duration",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        durations.forEach { minutes ->
                            val isSelected = state.selectedDurationMinutes == minutes
                            Surface(
                                shape = shapes.full,
                                color = if (isSelected) colors.inkPrimary else colors.surfaceRaised,
                                border = if (isSelected) null else BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                                    .clip(shapes.full)
                                    .clickable(role = Role.Button) {
                                        onIntent(StudyUiIntent.SelectDuration(minutes))
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "${minutes}m",
                                        style = typography.label,
                                        color = if (isSelected) colors.surfaceRaised else colors.inkPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.spacing4))

                    // 3. Subject Focus Mode Pills
                    Text(
                        text = "Focus Mode",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        tags.forEach { tag ->
                            val isSelected = state.selectedSubjectTag == tag
                            Surface(
                                shape = shapes.full,
                                color = if (isSelected) colors.inkPrimary else colors.surfaceRaised,
                                border = if (isSelected) null else BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                                    .clip(shapes.full)
                                    .clickable(role = Role.Button) {
                                        onIntent(StudyUiIntent.SelectSubjectTag(tag))
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = spacing.spacing1)
                                ) {
                                    Text(
                                        text = tag,
                                        style = typography.caption,
                                        color = if (isSelected) colors.surfaceRaised else colors.inkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sectionGap))
        }

        // 28-Day Contribution Heatmap Section
        StudyHeatmapSection(
            heatmapData = state.heatmapData,
            selectedCellDate = state.selectedCellDate,
            onSelectCell = { date -> onIntent(StudyUiIntent.SelectHeatmapCell(date)) }
        )

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Daily Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxl,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Today's Focus: ${state.todayTotalStudyMinutes} minutes",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Lock-screen timer keeps countdown active seamlessly in background",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}

/**
 * 28-Day tactile contribution heatmap grid and breakdown card (FR-4.2, AD-7).
 * Strictly consumes design tokens with zero hardcoded literals and 48dp minimum touch targets.
 */
@Composable
private fun StudyHeatmapSection(
    heatmapData: StudyHeatmapData,
    selectedCellDate: String?,
    onSelectCell: (String) -> Unit
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val selectedCell = selectedCellDate?.let { date -> heatmapData.cells.find { it.date == date } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.xxl,
        color = colors.surfaceRaised,
        shadowElevation = spacing.spacing0_5
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.spacing4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Focus Consistency",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Text(
                    text = "${heatmapData.activeDaysCount} active days",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // 28-day grid: 4 rows x 7 columns
            val cellList = heatmapData.cells
            val rows = if (cellList.size >= 28) cellList.chunked(7) else listOf(cellList)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1)
            ) {
                rows.forEach { weekCells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
                    ) {
                        weekCells.forEach { cell ->
                            val isSelected = selectedCellDate == cell.date
                            val cellColor = when (cell.intensityTier) {
                                1 -> colors.iconSoftPeach
                                2 -> colors.iconSoftMatcha
                                3 -> colors.primaryLight
                                4 -> colors.primary
                                else -> colors.surfaceSunken
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                                    .clickable(role = Role.Button) {
                                        onSelectCell(cell.date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(spacing.spacing6)
                                        .clip(shapes.xs)
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    BorderStroke(spacing.spacing0_5, colors.inkPrimary),
                                                    shapes.xs
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = typography.caption,
                    color = colors.inkTertiary
                )
                Spacer(modifier = Modifier.width(spacing.spacing1))
                listOf(
                    colors.surfaceSunken,
                    colors.iconSoftPeach,
                    colors.iconSoftMatcha,
                    colors.primaryLight,
                    colors.primary
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(spacing.spacing2)
                            .clip(shapes.xs)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(spacing.spacing0_5))
                }
                Spacer(modifier = Modifier.width(spacing.spacing0_5))
                Text(
                    text = "More",
                    style = typography.caption,
                    color = colors.inkTertiary
                )
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Day Breakdown or Rolling 28-Day Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.lg,
                color = colors.surfaceBase
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.spacing3)
                ) {
                    if (selectedCell != null) {
                        Text(
                            text = "${selectedCell.date} • ${selectedCell.totalMinutes} min focus",
                            style = typography.label,
                            color = colors.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.spacing1))
                        if (selectedCell.totalMinutes == 0) {
                            Text(
                                text = "No study sessions recorded on this day. Take your time to get started.",
                                style = typography.caption,
                                color = colors.inkSecondary
                            )
                        } else {
                            selectedCell.subjectBreakdown.forEach { (subject, mins) ->
                                Text(
                                    text = "• $subject: $mins min",
                                    style = typography.caption,
                                    color = colors.inkSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Past 4 Weeks: ${heatmapData.totalFocusMinutes} min total focus",
                            style = typography.label,
                            color = colors.inkPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.spacing1))
                        val top = heatmapData.topSubject
                        val summaryText = if (top != null) {
                            "Top subject: $top • Tap any day cell for breakdown"
                        } else {
                            "Tap any day cell to view focus breakdown."
                        }
                        Text(
                            text = summaryText,
                            style = typography.caption,
                            color = colors.inkSecondary
                        )
                    }
                }
            }
        }
    }
}
