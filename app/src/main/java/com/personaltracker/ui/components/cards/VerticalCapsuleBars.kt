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

/**
 * Metric representation for a vertical capsule progress bar.
 */
data class VerticalBarMetric(
    val title: String,
    val percentage: Float,
    val color: Color = Color.Unspecified
)

/**
 * Dynamic 4 Vertical Capsule Progress Bars + Habit Productivity Insights Card
 * conforming to Figma Frame 3 (03_Progress_And_Insights) and Framer project QaEW7GnAiltJMNVpWjTs-dKIrE.
 */
@Composable
fun VerticalCapsuleBars(
    modifier: Modifier = Modifier,
    metrics: List<VerticalBarMetric> = emptyList(),
    weeklyPoints: Int = 842,
    activeHabitsCount: Int = 4,
    consistencyPercentage: Int = 85,
    bestStreakDays: Int = 14,
    onShareProgress: () -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    val defaultPalette = listOf(
        colors.barDeepCocoa,
        colors.barWarmRust,
        colors.barMatchaOlive,
        colors.barSoftOrchid
    )

    val effectiveMetrics = if (metrics.isNotEmpty()) {
        metrics.take(4).mapIndexed { index, m ->
            if (m.color == Color.Unspecified) {
                m.copy(color = defaultPalette[index % defaultPalette.size])
            } else {
                m
            }
        }
    } else {
        listOf(
            VerticalBarMetric("Focus", 0.85f, colors.barDeepCocoa),
            VerticalBarMetric("Reading", 0.70f, colors.barWarmRust),
            VerticalBarMetric("Movement", 0.60f, colors.barMatchaOlive),
            VerticalBarMetric("Routine", 0.90f, colors.barSoftOrchid)
        )
    }

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
            effectiveMetrics.forEach { metric ->
                val animatedProgress by animateFloatAsState(
                    targetValue = if (isLoaded) metric.percentage.coerceIn(0f, 1f) else 0f,
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
                        text = "${(metric.percentage.coerceIn(0f, 1f) * 100).toInt()}%",
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

        // Bottom Habit Productivity Summary Card
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
                    text = "$weeklyPoints Points",
                    style = typography.heroScore,
                    color = colors.barWarmRust
                )
                Spacer(modifier = Modifier.height(spacing.spacing5))

                // 3 Habit Productivity Metrics Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "$activeHabitsCount", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Active Habits", style = typography.caption, color = colors.inkSecondary)
                    }
                    Column {
                        Text(text = "$consistencyPercentage%", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Consistency", style = typography.caption, color = colors.inkSecondary)
                    }
                    Column {
                        Text(text = "${bestStreakDays}d", style = typography.title, color = colors.inkPrimary)
                        Text(text = "Best Streak", style = typography.caption, color = colors.inkSecondary)
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
