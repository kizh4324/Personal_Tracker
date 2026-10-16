package com.personaltracker.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.R
import com.personaltracker.domain.engine.FlameState
import com.personaltracker.ui.theme.PersonalTrackerColors
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Resolves the 4-state flame model from streak count with clamping to non-negative (AD-4).
 */
fun resolveFlameState(streakCount: Int): FlameState {
    val effectiveStreak = maxOf(0, streakCount)
    return when (effectiveStreak) {
        0 -> FlameState.COLD
        in 1..2 -> FlameState.WARMING
        in 3..6 -> FlameState.BURNING
        else -> FlameState.BLAZING
    }
}

/**
 * Maps FlameState to the matching vector drawable resource.
 */
fun getFlameDrawableRes(flameState: FlameState): Int {
    return when (flameState) {
        FlameState.COLD -> R.drawable.ic_flame_cold
        FlameState.WARMING -> R.drawable.ic_flame_warming
        FlameState.BURNING -> R.drawable.ic_flame_burning
        FlameState.BLAZING -> R.drawable.ic_flame_blazing
    }
}

/**
 * Resolves flame accent color from theme tokens.
 */
fun getFlameColor(flameState: FlameState, colors: PersonalTrackerColors): Color {
    return when (flameState) {
        FlameState.COLD -> colors.streakCool
        FlameState.WARMING -> colors.streakFlameTip
        FlameState.BURNING -> colors.streakFlameCore
        FlameState.BLAZING -> colors.streakFlameCore
    }
}

/**
 * Atomic Streak Flame Badge component.
 *
 * Displays the 4-state flame icon (`ic_flame_cold`, `ic_flame_warming`, `ic_flame_burning`,
 * `ic_flame_blazing`) alongside the formatted streak number using data typography.
 * Strictly adheres to AD-7 with zero hardcoded literals and enforces 48dp touch targets when clickable.
 */
@Composable
fun StreakFlameBadge(
    streakCount: Int,
    modifier: Modifier = Modifier,
    flameState: FlameState? = null,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val effectiveStreak = maxOf(0, streakCount)
    val state = flameState ?: resolveFlameState(effectiveStreak)
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val flameAccentColor = getFlameColor(state, colors)
    val drawableRes = getFlameDrawableRes(state)

    val touchTargetModifier = if (onClick != null) {
        Modifier
            .defaultMinSize(
                minWidth = spacing.minTouchTarget,
                minHeight = spacing.minTouchTarget
            )
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        shape = shapes.full,
        color = colors.surfaceRaised,
        tonalElevation = PersonalTrackerTheme.elevation.flat,
        modifier = modifier
            .then(touchTargetModifier)
            .semantics {
                contentDescription = "Habit streak: $effectiveStreak days, flame status: ${state.name}"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
            modifier = Modifier.padding(
                horizontal = spacing.spacing2,
                vertical = spacing.spacing1
            )
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                tint = if (state == FlameState.COLD) colors.streakCool else Color.Unspecified,
                modifier = Modifier.size(spacing.coinIconSize)
            )

            if (showLabel) {
                Text(
                    text = "$effectiveStreak",
                    style = typography.data,
                    color = flameAccentColor
                )
            }
        }
    }
}
