package com.personaltracker.ui.screens.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.personaltracker.ui.components.companion.CompanionState
import com.personaltracker.ui.components.companion.CompanionView
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Companion screen (AD-2 Figma-Ready Split).
 * Collects immutable [CompanionUiState] and dispatches hoisted animations to [LocalRewardMotionStage].
 */
@Composable
fun CompanionRoute(
    modifier: Modifier = Modifier,
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    viewModel: CompanionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CompanionUiEffect.PlayLottieReward -> {
                    rewardMotionStage.dispatchLottieReward(effect.type)
                }
                is CompanionUiEffect.ShowSnackbar -> {
                    // Handled via scaffold or toast
                }
            }
        }
    }

    CompanionScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Companion showcase and interaction controls.
 * Conforms to AD-4 (non-punishing mechanics) and AD-7 (design tokens).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreenContent(
    state: CompanionUiState,
    onIntent: (CompanionUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

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
        // Companion Stage Showcase Card
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
                    text = "${state.speciesName} • Level ${state.level}",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Always supportive • Never punishing • Zero guilt",
                    style = typography.caption,
                    color = colors.companion
                )
                Spacer(modifier = Modifier.height(spacing.spacing3))

                // Energy Progress Bar
                LinearProgressIndicator(
                    progress = { state.energyProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(spacing.spacing2)
                        .clip(shapes.full),
                    color = colors.companion,
                    trackColor = colors.companionSubtle,
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "${state.currentEnergyUnits} / ${state.maxEnergyUnits} Energy to Level ${state.level + 1}",
                    style = typography.caption,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Rive Companion View
                Box(
                    modifier = Modifier
                        .size(spacing.spacing16 * 2 + spacing.spacing8) // 160dp token equivalent
                        .clip(shapes.full)
                        .background(colors.companionSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    CompanionView(
                        state = state.mood,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))
                Text(
                    text = "Mood: ${state.mood.stateName} (Rive State: ${state.mood.riveValue})",
                    style = typography.label,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Rive State Machine Controller
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
                    text = "Emotional Registers (`companion_sm`)",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "The 7 non-punitive emotional registers:",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing3))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompanionState.entries.forEach { companionState ->
                        val isSelected = state.mood == companionState
                        FilterChip(
                            selected = isSelected,
                            onClick = { onIntent(CompanionUiIntent.SelectMood(companionState)) },
                            label = {
                                Text(
                                    text = companionState.stateName,
                                    style = typography.caption
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.companion,
                                selectedLabelColor = colors.surfaceBase,
                                containerColor = colors.surfaceSunken,
                                labelColor = colors.inkPrimary
                            ),
                            modifier = Modifier.defaultMinSize(
                                minHeight = spacing.minTouchTarget
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Lottie Reward Animation Trigger Section
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
                    text = "Lottie Reward Runtimes",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "One-shot reward reveals hoisted to root viewport layer:",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing4))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
                ) {
                    Button(
                        onClick = { onIntent(CompanionUiIntent.TriggerMilestoneCelebration) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentWarm,
                            contentColor = colors.surfaceBase
                        ),
                        shape = shapes.md,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(
                            text = "Milestone Reveal",
                            style = typography.label
                        )
                    }

                    Button(
                        onClick = { onIntent(CompanionUiIntent.TriggerCoinMicroBurst) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.surfaceBase
                        ),
                        shape = shapes.md,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(
                            text = "Coin Micro Burst",
                            style = typography.label
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Behavioral & Design Invariants Card
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
                    text = "Core Behavioral Contract (AD-4)",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "• 0 HP loss, zero hunger decay, zero death mechanics.\n" +
                            "• Zero nag or shame copy in all UI & notification text.\n" +
                            "• Companion celebrates progress and rests peacefully when all caught up.",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
