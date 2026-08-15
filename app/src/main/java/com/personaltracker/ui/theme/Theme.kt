package com.personaltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Main Theme Composable for Personal-Tracker.
 * Wraps children with all custom design tokens: Colors, Typography, Shapes, Spacing, Elevation, and Animation.
 */
@Composable
fun PersonalTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPersonalTrackerColors else LightPersonalTrackerColors
    val typography = PersonalTrackerTypography()
    val shapes = PersonalTrackerShapes()
    val spacing = PersonalTrackerSpacing()
    val elevation = PersonalTrackerElevation()

    CompositionLocalProvider(
        LocalPersonalTrackerColors provides colors,
        LocalPersonalTrackerTypography provides typography,
        LocalPersonalTrackerShapes provides shapes,
        LocalPersonalTrackerSpacing provides spacing,
        LocalPersonalTrackerElevation provides elevation,
        LocalPersonalTrackerAnimationTokens provides AnimationTokens,
        content = content
    )
}

/**
 * Access object for PersonalTracker design tokens.
 */
object PersonalTrackerTheme {
    val colors: PersonalTrackerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerColors.current

    val typography: PersonalTrackerTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerTypography.current

    val shapes: PersonalTrackerShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerShapes.current

    val spacing: PersonalTrackerSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerSpacing.current

    val elevation: PersonalTrackerElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerElevation.current

    val animation: AnimationTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalPersonalTrackerAnimationTokens.current
}
