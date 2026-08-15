package com.personaltracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing Design Tokens for Personal-Tracker.
 * Built on a 4dp base scale (8dp-dominant).
 * Sourced 1:1 from `design-system.md` Layout & Spacing section.
 */
@Immutable
data class PersonalTrackerSpacing(
    val spacing0_5: Dp = 2.dp,
    val spacing1: Dp = 4.dp,
    val spacing2: Dp = 8.dp,
    val spacing3: Dp = 12.dp,
    val spacing4: Dp = 16.dp,
    val spacing5: Dp = 20.dp,
    val spacing6: Dp = 24.dp,
    val spacing8: Dp = 32.dp,
    val spacing10: Dp = 40.dp,
    val spacing12: Dp = 48.dp,
    val spacing16: Dp = 64.dp,

    // Named Layout Tokens
    val gutter: Dp = 12.dp,
    val marginMobile: Dp = 16.dp,
    val sectionGap: Dp = 24.dp,
    val screenPaddingHorizontal: Dp = 16.dp,
    val screenPaddingVertical: Dp = 16.dp,

    // Component Dimensions
    val minTouchTarget: Dp = 48.dp,
    val navBarHeight: Dp = 64.dp,
    val buttonHeight: Dp = 48.dp,
    val chipHeight: Dp = 32.dp,
    val fabSize: Dp = 56.dp,
    val progressBarHeight: Dp = 6.dp,
    val coinIconSize: Dp = 20.dp
)

val LocalPersonalTrackerSpacing = staticCompositionLocalOf { PersonalTrackerSpacing() }
