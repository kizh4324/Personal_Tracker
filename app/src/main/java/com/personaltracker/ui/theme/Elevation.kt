package com.personaltracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation & Depth Tokens for Personal-Tracker.
 * Sourced 1:1 from `design-system.md` Elevation & Depth section.
 * Rule: Resting cards are flat; elevation appears only during interaction.
 */
@Immutable
data class PersonalTrackerElevation(
    val flat: Dp = 0.dp,
    val low: Dp = 1.dp,
    val medium: Dp = 4.dp,
    val high: Dp = 8.dp
)

val LocalPersonalTrackerElevation = staticCompositionLocalOf { PersonalTrackerElevation() }
