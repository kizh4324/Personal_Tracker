package com.personaltracker.ui.components.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * 6 Priority States for HeroActionCard (FR-1.1, EXPERIENCE.md §4.1).
 * Priority order (1 highest to 6 lowest):
 * 1. ActiveFocus
 * 2. InterruptedResumable
 * 3. MorningBanner
 * 4. UpcomingItem
 * 5. CarryForwardReview
 * 6. IntentionalIdle
 */
sealed interface HeroCardState {
    val priority: Int

    data class ActiveFocus(
        val title: String,
        val countdownFormatted: String,
        val onPause: () -> Unit = {},
        val onEndSession: () -> Unit = {}
    ) : HeroCardState {
        override val priority: Int = 1
    }

    data class InterruptedResumable(
        val title: String,
        val elapsedSinceFormatted: String,
        val onResume: () -> Unit = {},
        val onReview: () -> Unit = {},
        val onMarkComplete: () -> Unit = {}
    ) : HeroCardState {
        override val priority: Int = 2
    }

    data class MorningBanner(
        val dayTypeName: String,
        val onSwapDayType: () -> Unit = {},
        val onDismiss: () -> Unit = {}
    ) : HeroCardState {
        override val priority: Int = 3
    }

    data class UpcomingItem(
        val title: String,
        val startsInMinutes: Int,
        val onStartNow: () -> Unit = {},
        val onSnooze: () -> Unit = {}
    ) : HeroCardState {
        override val priority: Int = 4
    }

    data class CarryForwardReview(
        val itemCount: Int,
        val onReviewItems: () -> Unit = {}
    ) : HeroCardState {
        override val priority: Int = 5
    }

    data class IntentionalIdle(
        val message: String = "All caught up for today. Rest well."
    ) : HeroCardState {
        override val priority: Int = 6
    }
}

/**
 * Deterministically resolves state conflicts by selecting the highest priority state.
 */
fun resolveHeroPriority(candidates: List<HeroCardState>): HeroCardState {
    return candidates.minByOrNull { it.priority } ?: HeroCardState.IntentionalIdle()
}

/**
 * Formats the non-punitive carry-forward copy with proper singular/plural grammar.
 */
fun formatCarryForwardText(itemCount: Int): String {
    val countClause = if (itemCount == 1) "1 item still open" else "$itemCount items still open"
    return "$countClause. What would you like to do?"
}

/**
 * State-aware Hero Card container managing 6 priority states with 300ms FastOutSlowIn cross-fade.
 *
 * Sourced 1:1 from `EXPERIENCE.md` §4.1 and `DESIGN.md` Hero Card specifications.
 * Enforces 48dp minimum touch target floor for all interactive actions.
 */
@Composable
fun HeroActionCard(
    state: HeroCardState,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Min height 120dp expressed purely via design tokens: spacing16 (64dp) + spacing12 (48dp) + spacing2 (8dp) = 120dp
    val stableMinHeight = spacing.spacing16 + spacing.spacing12 + spacing.spacing2

    val isMorningBanner = state is HeroCardState.MorningBanner

    Surface(
        shape = if (isMorningBanner) shapes.xxl else shapes.lg,
        color = if (isMorningBanner) colors.bannerPeach else colors.surfaceRaised,
        shadowElevation = elevation.low,
        border = BorderStroke(spacing.spacing0_5, if (isMorningBanner) colors.bannerPeach else colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = stableMinHeight)
            .semantics {
                contentDescription = "Hero action card: state priority ${state.priority}"
            }
    ) {
        Box(
            modifier = Modifier
                .padding(spacing.spacing4)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it.priority },
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = AnimationTokens.DurationStateShort,
                            easing = AnimationTokens.StandardEasing
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = AnimationTokens.DurationStateShort,
                            easing = AnimationTokens.StandardEasing
                        )
                    )
                },
                label = "HeroCardStateTransition"
            ) { targetState ->
                when (targetState) {
                    is HeroCardState.ActiveFocus -> {
                        ActiveFocusContent(targetState)
                    }
                    is HeroCardState.InterruptedResumable -> {
                        InterruptedResumableContent(targetState)
                    }
                    is HeroCardState.MorningBanner -> {
                        MorningBannerContent(targetState)
                    }
                    is HeroCardState.UpcomingItem -> {
                        UpcomingItemContent(targetState)
                    }
                    is HeroCardState.CarryForwardReview -> {
                        CarryForwardReviewContent(targetState)
                    }
                    is HeroCardState.IntentionalIdle -> {
                        IntentionalIdleContent(targetState)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFocusContent(state: HeroCardState.ActiveFocus) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Active Session",
            style = typography.caption,
            color = colors.primary
        )
        Text(
            text = state.title,
            style = typography.title,
            color = colors.inkPrimary
        )
        Text(
            text = state.countdownFormatted,
            style = typography.dataLarge,
            color = colors.inkPrimary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = state.onPause,
                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Pause", style = typography.label)
            }
            Button(
                onClick = state.onEndSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = colors.surfaceRaised
                ),
                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "End Session", style = typography.label)
            }
        }
    }
}

@Composable
private fun InterruptedResumableContent(state: HeroCardState.InterruptedResumable) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Interrupted Item",
            style = typography.caption,
            color = colors.warning
        )
        Text(
            text = state.title,
            style = typography.title,
            color = colors.inkPrimary
        )
        Text(
            text = "Paused ${state.elapsedSinceFormatted}",
            style = typography.bodySecondary,
            color = colors.inkSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = state.onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surfaceRaised
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Resume", style = typography.label, maxLines = 1)
            }
            OutlinedButton(
                onClick = state.onReview,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Review", style = typography.label, maxLines = 1)
            }
            TextButton(
                onClick = state.onMarkComplete,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Complete", style = typography.label, color = colors.success, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MorningBannerContent(state: HeroCardState.MorningBanner) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Set the reminder",
                style = typography.title,
                color = colors.inkPrimary
            )
            Text(
                text = "Never miss your morning routine! Active: ${state.dayTypeName}",
                style = typography.caption,
                color = colors.primaryDark
            )
            Spacer(modifier = Modifier.height(spacing.spacing2))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = state.onSwapDayType,
                    shape = shapes.full,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryDark,
                        contentColor = colors.surfaceRaised
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Set Now", style = typography.label)
                }
                TextButton(
                    onClick = state.onDismiss,
                    shape = shapes.full,
                    modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Dismiss", style = typography.label, color = colors.inkSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(spacing.spacing3))

        Surface(
            shape = shapes.full,
            color = colors.primary,
            modifier = Modifier.size(spacing.fabSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "🔔",
                    style = typography.title
                )
            }
        }
    }
}

@Composable
private fun UpcomingItemContent(state: HeroCardState.UpcomingItem) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Upcoming Scheduled Item",
            style = typography.caption,
            color = colors.info
        )
        Text(
            text = state.title,
            style = typography.title,
            color = colors.inkPrimary
        )
        val timingText = if (state.startsInMinutes <= 0) {
            "Starts now — Start Focus?"
        } else {
            "Starts in ${state.startsInMinutes} min"
        }
        Text(
            text = timingText,
            style = typography.bodySecondary,
            color = colors.inkSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = state.onStartNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surfaceRaised
                ),
                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Start Now", style = typography.label)
            }
            OutlinedButton(
                onClick = state.onSnooze,
                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(text = "Snooze", style = typography.label)
            }
        }
    }
}

@Composable
private fun CarryForwardReviewContent(state: HeroCardState.CarryForwardReview) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Carry-Forward Review",
            style = typography.caption,
            color = colors.accentWarm
        )
        Text(
            text = formatCarryForwardText(state.itemCount),
            style = typography.title,
            color = colors.inkPrimary
        )
        Button(
            onClick = state.onReviewItems,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentWarm,
                contentColor = colors.surfaceRaised
            ),
            modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
        ) {
            Text(text = "Review Items", style = typography.label)
        }
    }
}

@Composable
private fun IntentionalIdleContent(state: HeroCardState.IntentionalIdle) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = shapes.full,
            color = colors.companionSubtle,
            modifier = Modifier.size(spacing.spacing10)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Companion resting",
                    tint = colors.companion,
                    modifier = Modifier.size(spacing.spacing6)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Today's Schedule",
                style = typography.caption,
                color = colors.companion
            )
            Text(
                text = state.message,
                style = typography.title,
                color = colors.inkPrimary
            )
        }
    }
}
