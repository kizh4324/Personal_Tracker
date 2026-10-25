package com.personaltracker.ui.components.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

data class VerticalBarMetric(
    val title: String,
    val percentage: Float,
    val color: Color
)

/**
 * 4 Vertical Capsule Progress Bars + Summary Insights Card conforming to
 * Figma Frame 3 (03_Progress_And_Insights) and Framer project QaEW7GnAiltJMNVpWjTs-dKIrE.
 */
@Composable
fun VerticalCapsuleBars(
    modifier: Modifier = Modifier,
    onShareProgress: () -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    val metrics = listOf(
        VerticalBarMetric("Walking", 0.48f, colors.barDeepCocoa),
        VerticalBarMetric("Running", 0.33f, colors.barWarmRust),
        VerticalBarMetric("Meditation", 0.27f, colors.barMatchaOlive),
        VerticalBarMetric("Drink", 0.40f, colors.barSoftOrchid)
    )

    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLoaded = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.spacing6)
    ) {
        // 4 Vertical Capsule Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.barHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            metrics.forEachIndexed { _, metric ->
                val animatedProgress by animateFloatAsState(
                    targetValue = if (isLoaded) metric.percentage else 0f,
                    animationSpec = AnimationTokens.staggeredBarSpringSpec(),
                    label = "VerticalBarFill_${metric.title}"
                )

                Box(
                    modifier = Modifier
                        .width(spacing.barWidth)
                        .height(spacing.barHeight)
                        .clip(shapes.capsule)
                        .background(colors.surfaceSunken),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Top Percentage Label
                    Text(
                        text = "${(metric.percentage * 100).toInt()}%",
                        style = typography.title,
                        color = colors.inkPrimary,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = spacing.spacing4)
                    )

                    // Animated Rounded Fill Capsule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedProgress)
                            .clip(shapes.capsule)
                            .background(metric.color)
                    )

                    // Bottom Activity Label
                    Text(
                        text = metric.title,
                        style = typography.caption,
                        color = colors.surfaceRaised,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = spacing.spacing4)
                    )
                }
            }
        }

        // Bottom Summary Card
        Surface(
            shape = shapes.xxxl,
            color = colors.surfaceRaised,
            shadowElevation = elevation.low,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing6)
            ) {
                Text(
                    text = "Points Earned / For this week",
                    style = typography.label,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "842 Points",
                    style = typography.heroScore,
                    color = colors.barWarmRust
                )
                Spacer(modifier = Modifier.height(spacing.spacing5))

                // 3 Metrics Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "440 lb", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Weight", style = typography.caption, color = colors.inkSecondary)
                    }
                    Column {
                        Text(text = "200 ft", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Elevation", style = typography.caption, color = colors.inkSecondary)
                    }
                    Column {
                        Text(text = "7h 30m", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Total time", style = typography.caption, color = colors.inkSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(spacing.spacing5))

                // Full-Pill Primary Share Progress Button
                Button(
                    onClick = onShareProgress,
                    shape = shapes.full,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceRaised
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.buttonHeight)
                ) {
                    Text(
                        text = "Share Progress",
                        style = typography.bodyMedium
                    )
                }
            }
        }
    }
}
