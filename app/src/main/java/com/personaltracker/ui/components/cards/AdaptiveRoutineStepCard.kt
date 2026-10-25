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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ScheduledRoutineStep
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Progression status of an individual routine step.
 */
enum class RoutineStepStatus {
    COMPLETED,
    ACTIVE,
    UPCOMING
}

/**
 * Stateless Routine Step Progression Card component.
 *
 * Sourced 1:1 from `EXPERIENCE.md` §4.4 and `DESIGN.md` routine-card specifications.
 * Sequential execution model: current step highlighted, completed steps subtle,
 * upcoming steps dimmed, with a 48dp minimum touch target step advancement and skip actions.
 */
@Composable
fun AdaptiveRoutineStepCard(
    stepOrder: Int,
    title: String,
    durationMinutes: Int,
    status: RoutineStepStatus,
    onAdvanceStep: () -> Unit,
    modifier: Modifier = Modifier,
    timeSlot: String? = null,
    onSkip: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Standard 3dp accent stripe width token math: spacing0_5 (2dp) + spacing0_5 / 2 (1dp) = 3dp
    val accentStripeWidth = spacing.spacing0_5 + (spacing.spacing0_5 / 2)

    val cardAlpha = when (status) {
        RoutineStepStatus.ACTIVE -> 1.0f
        RoutineStepStatus.COMPLETED -> 0.7f
        RoutineStepStatus.UPCOMING -> 0.5f
    }

    val cardBorder = when (status) {
        RoutineStepStatus.ACTIVE -> BorderStroke(spacing.spacing0_5, colors.companion)
        else -> BorderStroke(spacing.spacing0_5, colors.borderSubtle)
    }

    val durationText = if (timeSlot != null) {
        "$durationMinutes min • $timeSlot"
    } else {
        "$durationMinutes min"
    }

    Surface(
        shape = shapes.xl,
        color = if (status == RoutineStepStatus.COMPLETED) colors.surfaceSunken else colors.surfaceRaised,
        shadowElevation = elevation.flat,
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Routine step $stepOrder: $title, ${durationMinutes} minutes${if (timeSlot != null) ", $timeSlot" else ""}, status: ${status.name}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left companion accent stripe
            Box(
                modifier = Modifier
                    .width(accentStripeWidth)
                    .fillMaxHeight()
                    .background(if (status == RoutineStepStatus.COMPLETED) colors.primary else colors.companion)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                // Step sequence indicator and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                    modifier = Modifier.weight(1f)
                ) {
                    // Timeline Check / Sequence Node
                    Surface(
                        shape = shapes.full,
                        color = when (status) {
                            RoutineStepStatus.ACTIVE -> colors.companion
                            RoutineStepStatus.COMPLETED -> colors.primary
                            RoutineStepStatus.UPCOMING -> colors.surfaceBase
                        },
                        border = if (status == RoutineStepStatus.UPCOMING) BorderStroke(spacing.spacing0_5, colors.borderSubtle) else null,
                        modifier = Modifier.size(spacing.timelineCircleSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (status == RoutineStepStatus.COMPLETED) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.surfaceRaised,
                                    modifier = Modifier.size(spacing.spacing3)
                                )
                            } else {
                                Text(
                                    text = "$stepOrder",
                                    style = typography.caption,
                                    color = if (status == RoutineStepStatus.ACTIVE) colors.surfaceRaised else colors.inkSecondary
                                )
                            }
                        }
                    }

                    // Pastel Routine Icon Box (Figma Spec)
                    val iconBg = when (stepOrder % 3) {
                        1 -> colors.iconSoftPeach
                        2 -> colors.iconSoftMatcha
                        else -> colors.iconSoftLavender
                    }
                    val emoji = when (stepOrder % 4) {
                        1 -> "💧"
                        2 -> "🧘"
                        3 -> "🤸"
                        else -> "👟"
                    }
                    Surface(
                        shape = shapes.md,
                        color = iconBg,
                        modifier = Modifier.size(spacing.iconBoxSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = emoji,
                                style = typography.title
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.spacing0_5)
                    ) {
                        Text(
                            text = title,
                            style = typography.title,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (status == RoutineStepStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = durationText,
                            style = typography.data,
                            color = colors.inkTertiary
                        )
                    }
                }

                // Step advancement and skip buttons (enforcing 48dp touch target)
                if (status == RoutineStepStatus.ACTIVE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onSkip != null) {
                            TextButton(
                                onClick = onSkip,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colors.inkSecondary
                                ),
                                modifier = Modifier.defaultMinSize(
                                    minWidth = spacing.minTouchTarget,
                                    minHeight = spacing.minTouchTarget
                                )
                            ) {
                                Text(text = "Skip", style = typography.label)
                            }
                        }

                        Button(
                            onClick = onAdvanceStep,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.companion,
                                contentColor = colors.surfaceRaised
                            ),
                            modifier = Modifier.defaultMinSize(
                                minWidth = spacing.minTouchTarget,
                                minHeight = spacing.minTouchTarget
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
                            ) {
                                Text(text = "Complete", style = typography.label)
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
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
 * Domain model overload for [AdaptiveRoutineStepCard].
 */
@Composable
fun AdaptiveRoutineStepCard(
    step: RoutineStep,
    status: RoutineStepStatus,
    onAdvanceStep: () -> Unit,
    modifier: Modifier = Modifier,
    timeSlot: String? = null,
    onSkip: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    AdaptiveRoutineStepCard(
        stepOrder = step.orderIndex,
        title = step.title,
        durationMinutes = step.durationMinutes,
        status = status,
        onAdvanceStep = onAdvanceStep,
        modifier = modifier,
        timeSlot = timeSlot,
        onSkip = onSkip,
        onClick = onClick
    )
}

/**
 * Scheduled routine step overload for [AdaptiveRoutineStepCard].
 */
@Composable
fun AdaptiveRoutineStepCard(
    scheduledStep: ScheduledRoutineStep,
    status: RoutineStepStatus,
    onAdvanceStep: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    AdaptiveRoutineStepCard(
        stepOrder = scheduledStep.step.orderIndex,
        title = scheduledStep.step.title,
        durationMinutes = scheduledStep.step.durationMinutes,
        status = status,
        onAdvanceStep = onAdvanceStep,
        modifier = modifier,
        timeSlot = "${scheduledStep.startTime} - ${scheduledStep.endTime}",
        onSkip = onSkip,
        onClick = onClick
    )
}
