package com.personaltracker.ui.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

data class DayCapsuleData(
    val dayLabel: String,
    val dateNumber: Int,
    val initial: String
)

val DEFAULT_WEEK_DAYS = listOf(
    DayCapsuleData("Mon", 7, "M"),
    DayCapsuleData("Tue", 8, "T"),
    DayCapsuleData("Wed", 9, "W"),
    DayCapsuleData("Thu", 10, "T"),
    DayCapsuleData("Fri", 11, "F"),
    DayCapsuleData("Sat", 12, "S"),
    DayCapsuleData("Sun", 13, "S")
)

/**
 * Interactive 7-Day Capsule Strip conforming to Figma Frame 1 (01_Home_Daily_Routine)
 * and Framer motion spring physics (stiffness 380, damping 28).
 */
@Composable
fun DayCapsuleStrip(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    selectedDate: Int = 10,
    onDateSelected: (Int) -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val pillWidth = spacing.capsuleWidth
    val pillHeight = if (compact) spacing.capsuleWidth else spacing.capsuleHeight
    val pillShape = shapes.xxl

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DEFAULT_WEEK_DAYS.forEach { item ->
            val isSelected = item.dateNumber == selectedDate

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) colors.inkPrimary else colors.surfaceRaised,
                animationSpec = AnimationTokens.dayCapsuleSpringSpec(),
                label = "CapsuleBgColor"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.surfaceRaised else colors.inkPrimary,
                animationSpec = AnimationTokens.dayCapsuleSpringSpec(),
                label = "CapsuleTextColor"
            )

            val subTextColor by animateColorAsState(
                targetValue = if (isSelected) colors.surfaceRaised else colors.inkSecondary,
                animationSpec = AnimationTokens.dayCapsuleSpringSpec(),
                label = "CapsuleSubColor"
            )

            Surface(
                shape = pillShape,
                color = containerColor,
                tonalElevation = spacing.spacing0_5,
                modifier = Modifier
                    .width(pillWidth)
                    .height(pillHeight)
                    .clickable(
                        role = Role.Button,
                        onClick = { onDateSelected(item.dateNumber) }
                    )
                    .semantics {
                        contentDescription = "${item.dayLabel} ${item.dateNumber}${if (isSelected) ", selected" else ""}"
                    }
            ) {
                if (!compact) {
                    Column(
                        modifier = Modifier.size(pillWidth, pillHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.dayLabel,
                            style = typography.caption,
                            color = subTextColor
                        )
                        Spacer(modifier = Modifier.height(spacing.spacing1))
                        Text(
                            text = "${item.dateNumber}",
                            style = typography.title,
                            color = contentColor
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(pillWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.initial,
                            style = typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
