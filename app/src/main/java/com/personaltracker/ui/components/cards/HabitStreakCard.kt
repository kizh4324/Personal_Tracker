package com.personaltracker.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.personaltracker.domain.model.Habit
import com.personaltracker.ui.components.atoms.StreakFlameBadge
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Formats the targetDaysOfWeek string to clean user-facing cadence copy.
 */
fun formatCadence(targetDaysOfWeek: String): String {
    val trimmed = targetDaysOfWeek.replace(" ", "")
    val days = trimmed.split(",").filter { it.isNotBlank() }.toSet()
    return when {
        days == setOf("1", "2", "3", "4", "5", "6", "7") -> "DAILY"
        days == setOf("1", "2", "3", "4", "5") -> "WEEKDAYS"
        targetDaysOfWeek.equals("DAILY", ignoreCase = true) -> "DAILY"
        targetDaysOfWeek.equals("WEEKDAYS", ignoreCase = true) -> "WEEKDAYS"
        else -> "CUSTOM"
    }
}

/**
 * Stateless Habit Streak Card component.
 *
 * Displays habit metadata, streak flame progression, freeze status, and a 48dp completion toggle.
 * Sourced 1:1 from `DESIGN.md` habit-card and `EXPERIENCE.md` §4.3 specifications.
 */
@Composable
fun HabitStreakCard(
    name: String,
    currentStreak: Int,
    isCompleted: Boolean,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    isFreezeActive: Boolean = false,
    cadence: String = "DAILY",
    onClick: () -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation
    val haptics = LocalHapticFeedback.current

    // Standard 3dp accent stripe width token math: spacing0_5 (2dp) + spacing0_5 / 2 (1dp) = 3dp
    val accentStripeWidth = spacing.spacing0_5 + (spacing.spacing0_5 / 2)

    Surface(
        shape = shapes.lg,
        color = if (isCompleted) colors.surfaceSunken else colors.surfaceRaised,
        shadowElevation = elevation.flat,
        border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Habit: $name, streak: $currentStreak days, ${if (isCompleted) "completed" else "pending"}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left secondary accent stripe
            Box(
                modifier = Modifier
                    .width(accentStripeWidth)
                    .fillMaxHeight()
                    .background(colors.secondary)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                // Habit details
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        Text(
                            text = name,
                            style = typography.title,
                            color = if (isCompleted) colors.inkSecondary else colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        StreakFlameBadge(streakCount = currentStreak)
                    }

                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = typography.bodySecondary,
                            color = colors.inkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                        modifier = Modifier.padding(top = spacing.spacing0_5)
                    ) {
                        Text(
                            text = formatCadence(cadence),
                            style = typography.caption,
                            color = colors.inkTertiary
                        )

                        if (isFreezeActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.spacing0_5)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.info,
                                    modifier = Modifier.size(spacing.spacing3)
                                )
                                Text(
                                    text = "Freeze active today",
                                    style = typography.caption,
                                    color = colors.info
                                )
                            }
                        }
                    }
                }

                // 48dp minimum touch target completion toggle
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleCompletion()
                    },
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = spacing.minTouchTarget,
                            minHeight = spacing.minTouchTarget
                        )
                        .semantics {
                            contentDescription = if (isCompleted) "Mark habit incomplete" else "Mark habit completed"
                        }
                ) {
                    Surface(
                        shape = shapes.sm,
                        color = if (isCompleted) colors.companion else colors.surfaceBase,
                        border = BorderStroke(
                            width = spacing.spacing0_5,
                            color = if (isCompleted) colors.companion else colors.borderDefault
                        ),
                        modifier = Modifier.size(spacing.spacing6)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.surfaceRaised,
                                    modifier = Modifier.size(spacing.spacing4)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Domain model overload for [HabitStreakCard].
 */
@Composable
fun HabitStreakCard(
    habit: Habit,
    isCompleted: Boolean,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    HabitStreakCard(
        name = habit.name,
        currentStreak = habit.currentStreak,
        isCompleted = isCompleted,
        onToggleCompletion = onToggleCompletion,
        modifier = modifier,
        description = habit.description,
        isFreezeActive = habit.isFreezeActive,
        cadence = habit.targetDaysOfWeek,
        onClick = onClick
    )
}
