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
 * upcoming steps dimmed, with a 48dp minimum touch target step advancement action.
 */
@Composable
fun AdaptiveRoutineStepCard(
    stepOrder: Int,
    title: String,
    durationMinutes: Int,
    status: RoutineStepStatus,
    onAdvanceStep: () -> Unit,
    modifier: Modifier = Modifier,
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

    Surface(
        shape = shapes.lg,
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
                contentDescription = "Routine step $stepOrder: $title, ${durationMinutes} minutes, status: ${status.name}"
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
                    .background(colors.companion)
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
                    // Step number badge
                    Surface(
                        shape = shapes.full,
                        color = when (status) {
                            RoutineStepStatus.ACTIVE -> colors.companion
                            RoutineStepStatus.COMPLETED -> colors.companionSubtle
                            RoutineStepStatus.UPCOMING -> colors.surfaceBase
                        },
                        modifier = Modifier.size(spacing.spacing8)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (status == RoutineStepStatus.COMPLETED) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.companion,
                                    modifier = Modifier.size(spacing.spacing4)
                                )
                            } else {
                                Text(
                                    text = "$stepOrder",
                                    style = typography.data,
                                    color = if (status == RoutineStepStatus.ACTIVE) colors.surfaceRaised else colors.inkSecondary
                                )
                            }
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
                            text = "$durationMinutes min",
                            style = typography.data,
                            color = colors.inkTertiary
                        )
                    }
                }

                // Step advancement button (enforcing 48dp touch target)
                if (status == RoutineStepStatus.ACTIVE) {
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

/**
 * Domain model overload for [AdaptiveRoutineStepCard].
 */
@Composable
fun AdaptiveRoutineStepCard(
    step: RoutineStep,
    status: RoutineStepStatus,
    onAdvanceStep: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    AdaptiveRoutineStepCard(
        stepOrder = step.orderIndex,
        title = step.title,
        durationMinutes = step.durationMinutes,
        status = status,
        onAdvanceStep = onAdvanceStep,
        modifier = modifier,
        onClick = onClick
    )
}
