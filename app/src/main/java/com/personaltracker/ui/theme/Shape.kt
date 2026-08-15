package com.personaltracker.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shape & Corner Radius Design Tokens for Personal-Tracker.
 * Sourced 1:1 from `design-system.md` Shapes section.
 * Rule: Corner radius increases with emotional warmth.
 */
@Immutable
data class PersonalTrackerShapes(
    val xs: CornerBasedShape = RoundedCornerShape(4.dp),
    val sm: CornerBasedShape = RoundedCornerShape(8.dp),
    val md: CornerBasedShape = RoundedCornerShape(12.dp),
    val lg: CornerBasedShape = RoundedCornerShape(16.dp),
    val xl: CornerBasedShape = RoundedCornerShape(20.dp),
    val xxl: CornerBasedShape = RoundedCornerShape(24.dp),
    val full: Shape = CircleShape,
    val default: CornerBasedShape = RoundedCornerShape(12.dp)
)

typealias CornerBasedShape = RoundedCornerShape

val LocalPersonalTrackerShapes = staticCompositionLocalOf { PersonalTrackerShapes() }
