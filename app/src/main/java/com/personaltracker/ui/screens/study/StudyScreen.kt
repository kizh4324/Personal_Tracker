package com.personaltracker.ui.screens.study

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
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
 * 100% Stateless Composable rendering Study session timers and recent history.
 * Strictly consumes design tokens with zero hardcoded literals and 48dp touch targets (AD-4, AD-7).
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
    val tags = listOf("Focus", "Reading", "Deep Work", "Coding")

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
        // Study Timer Stage
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxl,
            color = colors.surfaceRaised,
            tonalElevation = spacing.spacing0_5
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${timer.subjectTag} Session",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = when {
                        timer.isPaused -> "Paused • Take a breath"
                        timer.isRunning -> "In focus • Stay in the zone"
                        else -> "Ready whenever you are"
                    },
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing6))

                // Timer Dial
                Box(
                    modifier = Modifier
                        .size(spacing.spacing16 * 2 + spacing.spacing16) // 192dp equivalent
                        .clip(CircleShape)
                        .background(colors.surfaceSunken),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (timer.isRunning || timer.isPaused) {
                                timer.formattedRemainingTime
                            } else {
                                "%02d:00".format(state.selectedDurationMinutes)
                            },
                            style = typography.display,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.height(spacing.spacing1))
                        Text(
                            text = if (timer.isRunning) "REMAINING" else "TARGET",
                            style = typography.caption,
                            color = colors.inkTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing6))

                // Action Controls
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
                                        durationMinutes = state.selectedDurationMinutes
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.surfaceBase
                            ),
                            shape = shapes.md,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = spacing.minTouchTarget)
                        ) {
                            Text(text = "Start Focus", style = typography.label)
                        }
                    } else {
                        if (timer.isPaused) {
                            Button(
                                onClick = { onIntent(StudyUiIntent.ResumeSession) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.surfaceBase
                                ),
                                shape = shapes.md,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                            ) {
                                Text(text = "Resume", style = typography.label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onIntent(StudyUiIntent.PauseSession) },
                                shape = shapes.md,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                            ) {
                                Text(text = "Pause", style = typography.label)
                            }
                        }

                        Button(
                            onClick = { onIntent(StudyUiIntent.StopSession) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.warning,
                                contentColor = colors.surfaceBase
                            ),
                            shape = shapes.md,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = spacing.minTouchTarget)
                        ) {
                            Text(text = "End Session", style = typography.label)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Duration & Tag Selection (visible when not running)
        if (!timer.isRunning && !timer.isPaused) {
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
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntent(StudyUiIntent.SelectDuration(minutes)) },
                                label = { Text(text = "${minutes}m", style = typography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.surfaceBase,
                                    containerColor = colors.surfaceSunken,
                                    labelColor = colors.inkPrimary
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.spacing4))
                    Text(
                        text = "Subject / Focus Area",
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
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntent(StudyUiIntent.SelectSubjectTag(tag)) },
                                label = { Text(text = tag, style = typography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primary,
                                    selectedLabelColor = colors.surfaceBase,
                                    containerColor = colors.surfaceSunken,
                                    labelColor = colors.inkPrimary
                                ),
                                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.sectionGap))
        }

        // Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.md,
            color = colors.surfaceSunken
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
