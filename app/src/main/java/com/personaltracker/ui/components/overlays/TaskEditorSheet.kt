package com.personaltracker.ui.components.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.personaltracker.domain.model.Task
import com.personaltracker.ui.theme.PersonalTrackerTheme
import java.time.LocalDate
import java.util.UUID

/**
 * Metadata definition for delivery intensity selector chips.
 */
data class IntensityOption(
    val key: String,
    val label: String,
    val coinRewardText: String
)

private val INTENSITY_OPTIONS = listOf(
    IntensityOption(key = "ROUTINE", label = "Routine", coinRewardText = "+5 coins"),
    IntensityOption(key = "IMPORTANT", label = "Important", coinRewardText = "+7 coins"),
    IntensityOption(key = "URGENT", label = "Urgent", coinRewardText = "+10 coins")
)

private val TIME_FORMAT_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * 100% Stateless Modal Sheet for creating, editing, and deleting discrete tasks (AD-2, AD-4, AD-7, EXP-016).
 *
 * Implements 3-tier delivery intensity selection (ROUTINE 5c, IMPORTANT 7c, URGENT 10c),
 * non-blocking soft-scarcity advisory guidance (>= 2 active urgent tasks), scheduled reminder configuration,
 * and 48dp minimum touch target floor with zero-guilt microcopy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    isOpen: Boolean,
    task: Task?,
    isSoftCapReached: Boolean,
    onSave: (Task) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    activeUrgentTaskCount: Int = 2
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val colors = PersonalTrackerTheme.colors
    val shapes = PersonalTrackerTheme.shapes
    val spacing = PersonalTrackerTheme.spacing
    val typography = PersonalTrackerTheme.typography

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deliveryIntensity by remember { mutableStateOf("ROUTINE") }
    var scheduledTime by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(isOpen, task) {
        if (isOpen) {
            title = task?.title.orEmpty()
            description = task?.description.orEmpty()
            deliveryIntensity = task?.deliveryIntensity?.uppercase()?.ifBlank { "ROUTINE" } ?: "ROUTINE"
            scheduledTime = task?.scheduledTime.orEmpty()
            titleError = null
            timeError = null
            isSubmitting = false
        }
    }

    val isEditing = task != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceRaised,
        shape = shapes.xl,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screenPaddingHorizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing) "Edit Task" else "New Focus Task",
                        style = typography.headline,
                        color = colors.inkPrimary
                    )
                    Spacer(modifier = Modifier.height(spacing.spacing0_5))
                    Text(
                        text = if (isEditing) "Update task details and reminder schedule" else "Set clear intent and pace for today",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close task editor",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(spacing.spacing5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing4))

            // Task Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null && it.isNotBlank()) {
                        titleError = null
                    }
                },
                label = { Text("Task Title", style = typography.bodySecondary) },
                placeholder = { Text("What do you want to accomplish?", style = typography.bodySecondary) },
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(
                            text = titleError ?: "",
                            style = typography.caption,
                            color = colors.danger
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderDefault,
                    errorBorderColor = colors.danger,
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary
                ),
                shape = shapes.md,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.spacing2))

            // Task Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes or Context (optional)", style = typography.bodySecondary) },
                placeholder = { Text("Add helpful details or steps...", style = typography.bodySecondary) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderDefault,
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary
                ),
                shape = shapes.md,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.spacing4))

            // Delivery Intensity Section
            Text(
                text = "Delivery Intensity",
                style = typography.title,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = "Classify effort level to determine completion coin reward",
                style = typography.caption,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spacing2))

            // Intensity Selector Chips with selectableGroup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
            ) {
                INTENSITY_OPTIONS.forEach { option ->
                    val isSelected = deliveryIntensity.equals(option.key, ignoreCase = true)
                    val chipBorderColor = when {
                        isSelected && option.key == "URGENT" -> colors.danger
                        isSelected && option.key == "IMPORTANT" -> colors.warning
                        isSelected -> colors.primary
                        else -> colors.borderSubtle
                    }
                    val chipBackgroundColor = when {
                        isSelected && option.key == "URGENT" -> colors.dangerSubtle
                        isSelected && option.key == "IMPORTANT" -> colors.warningSubtle
                        isSelected -> colors.primarySubtle
                        else -> colors.surfaceSunken
                    }

                    Surface(
                        shape = shapes.md,
                        color = chipBackgroundColor,
                        border = BorderStroke(spacing.spacing0_5, chipBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { deliveryIntensity = option.key }
                            )
                            .semantics {
                                contentDescription = "${option.label} intensity, ${option.coinRewardText}"
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = spacing.spacing2, horizontal = spacing.spacing1),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.label,
                                style = typography.label,
                                color = if (isSelected) colors.inkPrimary else colors.inkSecondary
                            )
                            Spacer(modifier = Modifier.height(spacing.spacing0_5))
                            Text(
                                text = option.coinRewardText,
                                style = typography.caption,
                                color = if (isSelected) colors.primary else colors.inkTertiary
                            )
                        }
                    }
                }
            }

            // Non-blocking Soft-Scarcity Guidance Banner
            AnimatedVisibility(
                visible = deliveryIntensity.equals("URGENT", ignoreCase = true) && isSoftCapReached,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(spacing.spacing3))
                    Surface(
                        shape = shapes.md,
                        color = colors.warningSubtle,
                        border = BorderStroke(spacing.spacing0_5, colors.warning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.spacing3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
                        ) {
                            Text(
                                text = "⚡",
                                style = typography.title
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$activeUrgentTaskCount urgent tasks already active today. Keep focus sharp!",
                                    style = typography.caption,
                                    color = colors.inkPrimary
                                )
                                Spacer(modifier = Modifier.height(spacing.spacing0_5))
                                Text(
                                    text = "You can still save as urgent — soft guidance helps prevent burnout.",
                                    style = typography.caption,
                                    color = colors.inkSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing4))

            // Scheduled Reminder Time
            Text(
                text = "Reminder Time (optional)",
                style = typography.title,
                color = colors.inkPrimary
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            Text(
                text = "Set a 24h reminder time (HH:mm) to trigger an exact alarm",
                style = typography.caption,
                color = colors.inkSecondary
            )
            Spacer(modifier = Modifier.height(spacing.spacing2))

            OutlinedTextField(
                value = scheduledTime,
                onValueChange = {
                    scheduledTime = it
                    if (timeError != null) timeError = null
                },
                label = { Text("Scheduled Time", style = typography.bodySecondary) },
                placeholder = { Text("e.g. 14:30 or 09:00", style = typography.bodySecondary) },
                isError = timeError != null,
                supportingText = {
                    if (timeError != null) {
                        Text(
                            text = timeError ?: "",
                            style = typography.caption,
                            color = colors.danger
                        )
                    }
                },
                singleLine = true,
                trailingIcon = {
                    if (scheduledTime.isNotBlank()) {
                        IconButton(
                            onClick = {
                                scheduledTime = ""
                                timeError = null
                            },
                            modifier = Modifier.defaultMinSize(
                                minWidth = spacing.minTouchTarget,
                                minHeight = spacing.minTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear reminder time",
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(spacing.spacing4)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderDefault,
                    errorBorderColor = colors.danger,
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary
                ),
                shape = shapes.md,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.spacing6))

            // Bottom Actions (Save, Cancel, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = {
                            task?.let { onDelete(it.id) }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.danger
                        ),
                        border = BorderStroke(spacing.spacing0_5, colors.danger),
                        shape = shapes.md,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text("Delete", style = typography.label)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(spacing.spacing0_5, colors.borderDefault),
                        shape = shapes.md,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text("Cancel", style = typography.label, color = colors.inkSecondary)
                    }
                }

                Button(
                    onClick = {
                        if (isSubmitting) return@Button

                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank()) {
                            titleError = "Please enter a task title"
                            return@Button
                        }

                        val trimmedTime = scheduledTime.trim()
                        if (trimmedTime.isNotBlank() && !TIME_FORMAT_REGEX.matches(trimmedTime)) {
                            timeError = "Enter 24h time in HH:mm format (e.g. 14:30)"
                            return@Button
                        }

                        isSubmitting = true

                        val taskToPersist = (task ?: Task(
                            id = UUID.randomUUID().toString(),
                            title = trimmedTitle,
                            scheduledDate = LocalDate.now().toString()
                        )).copy(
                            title = trimmedTitle,
                            description = description.trim().ifBlank { null },
                            deliveryIntensity = deliveryIntensity,
                            scheduledTime = trimmedTime.ifBlank { null }
                        )

                        onSave(taskToPersist)
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceRaised
                    ),
                    shape = shapes.md,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(if (isEditing) "Save Changes" else "Create Task", style = typography.label)
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing4))
        }
    }
}
