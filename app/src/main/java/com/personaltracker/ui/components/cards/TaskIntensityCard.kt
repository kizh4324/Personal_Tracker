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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.personaltracker.domain.model.Task
import com.personaltracker.ui.theme.PersonalTrackerColors
import com.personaltracker.ui.theme.PersonalTrackerSpacing
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Encapsulates the visual styling specification for delivery intensity accent stripes.
 */
data class IntensityStripeSpec(
    val width: Dp,
    val color: Color,
    val isUrgent: Boolean
)

/**
 * Resolves the delivery intensity stripe dimensions and colors based strictly on theme tokens.
 * Sourced 1:1 from `DESIGN.md` task-card and `EXPERIENCE.md` §4.2 specifications:
 * - ROUTINE: 3dp width, colors.info
 * - IMPORTANT: 3dp width, colors.warning
 * - URGENT: 6dp width (progressBarHeight), colors.danger + lightning badge
 * - Invalid string: Fallback to ROUTINE (3dp, colors.info)
 */
fun resolveIntensityStripeSpec(
    intensity: String,
    colors: PersonalTrackerColors,
    spacing: PersonalTrackerSpacing
): IntensityStripeSpec {
    val standardWidth = spacing.spacing0_5 + (spacing.spacing0_5 / 2) // 3dp
    val urgentWidth = spacing.progressBarHeight // 6dp

    return when (intensity.uppercase()) {
        "IMPORTANT" -> IntensityStripeSpec(
            width = standardWidth,
            color = colors.warning,
            isUrgent = false
        )
        "URGENT" -> IntensityStripeSpec(
            width = urgentWidth,
            color = colors.danger,
            isUrgent = true
        )
        else -> IntensityStripeSpec(
            width = standardWidth,
            color = colors.info,
            isUrgent = false
        )
    }
}

/**
 * Stateless Task Intensity Card component.
 *
 * Implements 3dp/6dp delivery intensity accent stripes, completion strikethrough,
 * carry-forward badges, and a 48dp minimum touch target completion toggle.
 */
@Composable
fun TaskIntensityCard(
    title: String,
    deliveryIntensity: String,
    isCompleted: Boolean,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    isCarryForward: Boolean = false,
    carryForwardCount: Int = 0,
    scheduledTime: String? = null,
    onClick: () -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation
    val haptics = LocalHapticFeedback.current

    val stripeSpec = resolveIntensityStripeSpec(deliveryIntensity, colors, spacing)

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
                contentDescription = "Task: $title, intensity: $deliveryIntensity, ${if (isCompleted) "completed" else "pending"}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left delivery intensity accent stripe (3dp or 6dp)
            Box(
                modifier = Modifier
                    .width(stripeSpec.width)
                    .fillMaxHeight()
                    .background(stripeSpec.color)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                // Task content
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        Text(
                            text = title,
                            style = typography.title,
                            color = if (isCompleted) colors.inkSecondary else colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )

                        // Urgent lightning badge
                        if (stripeSpec.isUrgent) {
                            Surface(
                                shape = shapes.full,
                                color = colors.dangerSubtle,
                                modifier = Modifier.defaultMinSize(minHeight = spacing.spacing5)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing0_5),
                                    modifier = Modifier.padding(
                                        horizontal = spacing.spacing2,
                                        vertical = spacing.spacing0_5
                                    )
                                ) {
                                    Text(
                                        text = "⚡ URGENT",
                                        style = typography.caption,
                                        color = colors.danger
                                    )
                                }
                            }
                        }
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

                    // Metadata row: scheduled time and carry-forward badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                        modifier = Modifier.padding(top = spacing.spacing0_5)
                    ) {
                        if (!scheduledTime.isNullOrBlank()) {
                            Text(
                                text = scheduledTime,
                                style = typography.data,
                                color = colors.inkTertiary
                            )
                        }

                        if (isCarryForward) {
                            Surface(
                                shape = shapes.full,
                                color = colors.accentWarmSubtle
                            ) {
                                Text(
                                    text = if (carryForwardCount > 0) "↻ $carryForwardCount" else "↻",
                                    style = typography.caption,
                                    color = colors.accentWarm,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.spacing2,
                                        vertical = spacing.spacing0_5
                                    )
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
                            contentDescription = if (isCompleted) "Mark task pending" else "Mark task completed"
                        }
                ) {
                    Surface(
                        shape = shapes.sm,
                        color = if (isCompleted) colors.primary else colors.surfaceBase,
                        border = BorderStroke(
                            width = spacing.spacing0_5,
                            color = if (isCompleted) colors.primary else colors.borderDefault
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
 * Domain model overload for [TaskIntensityCard].
 */
@Composable
fun TaskIntensityCard(
    task: Task,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    TaskIntensityCard(
        title = task.title,
        deliveryIntensity = task.deliveryIntensity,
        isCompleted = task.state == "COMPLETED",
        onToggleCompletion = onToggleCompletion,
        modifier = modifier,
        description = task.description,
        isCarryForward = task.isCarryForward,
        carryForwardCount = task.carryForwardCount,
        scheduledTime = task.scheduledTime,
        onClick = onClick
    )
}
