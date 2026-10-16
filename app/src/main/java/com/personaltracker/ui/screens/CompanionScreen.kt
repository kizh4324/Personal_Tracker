package com.personaltracker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.companion.CompanionState
import com.personaltracker.ui.components.companion.CompanionView
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Companion Screen.
 *
 * Full interactive showcase of the Rive-powered non-punitive companion pet and Lottie
 * reward animations. Sourced 1:1 from `EXPERIENCE.md` §4.9-§4.10 and `DESIGN.md`.
 *
 * - Rive Runtime: Drives the 7-state machine (`companion_sm`) on input `state` (0-6).
 * - Lottie Runtime: Dispatches milestone full-screen celebration and coin micro-rewards.
 * - Enforces zero-guilt copy, AD-7 token conformance, and 48dp touch targets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreen(
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val rewardMotionStage = LocalRewardMotionStage.current

    var currentCompanionState by remember { mutableStateOf(CompanionState.Idle) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenPaddingHorizontal, vertical = spacing.screenPaddingVertical),
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
                    text = "Personal Companion",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Always supportive • Never punishing • Zero guilt",
                    style = typography.caption,
                    color = colors.companion
                )
                Spacer(modifier = Modifier.height(spacing.spacing5))

                // Rive Companion View
                Box(
                    modifier = Modifier
                        .size(spacing.spacing16 * 2 + spacing.spacing8) // 160dp equivalent via tokens
                        .clip(shapes.full)
                        .background(colors.companionSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    CompanionView(
                        state = currentCompanionState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))
                Text(
                    text = "State: ${currentCompanionState.stateName} (Rive State: ${currentCompanionState.riveValue})",
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
                    text = "Rive State Machine (`companion_sm`)",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Drive state integer input across the 7 non-punitive emotional registers:",
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
                        val isSelected = currentCompanionState == companionState
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentCompanionState = companionState },
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
                        onClick = {
                            currentCompanionState = CompanionState.Celebrating
                            rewardMotionStage.dispatchLottieReward(RewardAnimationType.MilestoneFullReveal)
                        },
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
                        onClick = {
                            rewardMotionStage.dispatchLottieReward(RewardAnimationType.CoinEarnMicro)
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
