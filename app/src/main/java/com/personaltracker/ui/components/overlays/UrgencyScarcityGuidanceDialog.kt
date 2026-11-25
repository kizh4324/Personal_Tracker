package com.personaltracker.ui.components.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personaltracker.R
import com.personaltracker.ui.screens.home.UrgencyScarcityPromptState
import com.personaltracker.ui.theme.PersonalTrackerTheme

internal data class UrgencyDialogMessageSpec(
    val messageResId: Int,
    val activeCount: Int,
    val formattedDate: String?
)

internal fun resolveUrgencyDialogMessageSpec(
    promptState: UrgencyScarcityPromptState
): UrgencyDialogMessageSpec {
    val messageResId = when {
        promptState.isTargetDateToday && promptState.activeUrgentCount == 1 ->
            R.string.urgency_scarcity_dialog_message_today_one
        promptState.isTargetDateToday ->
            R.string.urgency_scarcity_dialog_message_today_many
        promptState.activeUrgentCount == 1 ->
            R.string.urgency_scarcity_dialog_message_date_one
        else ->
            R.string.urgency_scarcity_dialog_message_date_many
    }
    val formattedDate = if (promptState.isTargetDateToday) {
        null
    } else {
        promptState.pendingTask.scheduledDate
    }
    return UrgencyDialogMessageSpec(
        messageResId = messageResId,
        activeCount = promptState.activeUrgentCount,
        formattedDate = formattedDate
    )
}

/**
 * Stateless non-blocking dialog presenting calm choice guidance when saving a task as URGENT
 * at or above the soft cap (AD-2, AD-4, AD-7).
 *
 * Offers both "Change to Important", autonomous "Keep Urgent", and accessible "Keep Editing" dismiss
 * with 48dp minimum touch targets.
 */
@Composable
fun UrgencyScarcityGuidanceDialog(
    promptState: UrgencyScarcityPromptState?,
    onKeepUrgent: () -> Unit,
    onChangeToImportant: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (promptState == null) return

    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val typography = PersonalTrackerTheme.typography
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    val dialogCd = stringResource(R.string.urgency_scarcity_dialog_cd)
    val messageSpec = resolveUrgencyDialogMessageSpec(promptState)
    val messageText = if (messageSpec.formattedDate != null) {
        stringResource(messageSpec.messageResId, messageSpec.activeCount, messageSpec.formattedDate)
    } else {
        stringResource(messageSpec.messageResId, messageSpec.activeCount)
    }

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
                .semantics { contentDescription = dialogCd },
            shape = shapes.lg,
            color = colors.surfaceRaised,
            tonalElevation = elevation.medium,
            shadowElevation = elevation.medium,
            border = BorderStroke(spacing.spacing0_5, colors.warning)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡",
                        style = typography.title,
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                    Spacer(modifier = Modifier.width(spacing.spacing2))
                    Text(
                        text = stringResource(R.string.urgency_scarcity_dialog_title),
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                Text(
                    text = messageText,
                    style = typography.bodyMedium,
                    color = colors.inkPrimary
                )

                Spacer(modifier = Modifier.height(spacing.spacing1))

                Text(
                    text = stringResource(R.string.urgency_scarcity_dialog_subtitle),
                    style = typography.caption,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(spacing.spacing4))

                Button(
                    onClick = onChangeToImportant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceRaised
                    ),
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.urgency_scarcity_action_change_important),
                        style = typography.label
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                OutlinedButton(
                    onClick = onKeepUrgent,
                    border = BorderStroke(spacing.spacing0_5, colors.warning),
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.urgency_scarcity_action_keep_urgent),
                        style = typography.label,
                        color = colors.inkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing1))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.End)
                        .defaultMinSize(
                            minWidth = spacing.minTouchTarget,
                            minHeight = spacing.minTouchTarget
                        )
                ) {
                    Text(
                        text = stringResource(R.string.urgency_scarcity_action_dismiss),
                        style = typography.label,
                        color = colors.inkSecondary
                    )
                }
            }
        }
    }
}
