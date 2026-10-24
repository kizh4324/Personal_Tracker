package com.personaltracker.ui.components.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personaltracker.domain.model.Task
import com.personaltracker.ui.components.cards.formatCarryForwardText
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateless dialog enabling 1-tap carry-forward rescheduling or guilt-free dismissal of overdue tasks.
 *
 * Sourced 1:1 from AD-4, AD-7, EXP-008, EXP-011, and EXP-016.
 * Enforces 48dp minimum touch target floor for all interactive actions and contains zero guilt copy.
 */
@Composable
fun CarryForwardReviewDialog(
    isOpen: Boolean,
    overdueTasks: List<Task>,
    onCarryForwardAll: () -> Unit,
    onDismissAllWithoutGuilt: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val typography = PersonalTrackerTheme.typography
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Max height for list of overdue tasks using design tokens (64*3 + 48 = 240dp)
    val listMaxHeight = spacing.spacing16 + spacing.spacing16 + spacing.spacing16 + spacing.spacing12

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.spacing4)
                .semantics { contentDescription = "Carry-forward review dialog" },
            shape = shapes.lg,
            color = colors.surfaceRaised,
            tonalElevation = elevation.medium,
            shadowElevation = elevation.medium,
            border = BorderStroke(spacing.spacing0_5, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Text(
                    text = "Carry-Forward Review",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = formatCarryForwardText(overdueTasks.size),
                    style = typography.caption,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Overdue items list or supportive empty message
                if (overdueTasks.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        items(
                            items = overdueTasks,
                            key = { it.id }
                        ) { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = spacing.minTouchTarget)
                                    .clip(shapes.md),
                                shape = shapes.md,
                                color = colors.surfaceBase,
                                border = BorderStroke(spacing.spacing0_5, colors.borderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = spacing.spacing3,
                                            vertical = spacing.spacing2
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = typography.label,
                                            color = colors.inkPrimary
                                        )
                                        val scheduledClause = "Scheduled ${task.scheduledDate}"
                                        val carryClause = if (task.carryForwardCount > 0) {
                                            " • Carried ${task.carryForwardCount}x"
                                        } else ""
                                        Text(
                                            text = "$scheduledClause$carryClause",
                                            style = typography.caption,
                                            color = colors.inkSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.spacing4))
                } else {
                    Text(
                        text = "All caught up! No overdue tasks to carry forward.",
                        style = typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing4))
                }

                // 1-Tap Carry-Forward to Today Button
                Button(
                    onClick = onCarryForwardAll,
                    enabled = overdueTasks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentWarm,
                        contentColor = colors.surfaceBase,
                        disabledContainerColor = colors.surfaceBase,
                        disabledContentColor = colors.inkTertiary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = "Carry Forward to Today",
                        style = typography.label
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                // Guilt-Free Dismiss Button
                OutlinedButton(
                    onClick = onDismissAllWithoutGuilt,
                    enabled = overdueTasks.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = "Dismiss Without Guilt",
                        style = typography.label,
                        color = if (overdueTasks.isNotEmpty()) colors.inkPrimary else colors.inkTertiary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                // Keep for Later / Dismiss Dialog Button
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.End)
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = "Keep for Later",
                        style = typography.label,
                        color = colors.inkSecondary
                    )
                }
            }
        }
    }
}
