package com.personaltracker.ui.components.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.personaltracker.R
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.components.atoms.WaveformVisualizer
import com.personaltracker.ui.theme.PersonalTrackerTheme
import kotlinx.coroutines.delay

/**
 * Chip metadata representing a single editable proposal field.
 */
data class ProposalChipData(
    val chipType: String,
    val label: String,
    val value: String
)

/**
 * Pure function resolving the 5 medium-confidence proposal chips.
 */
fun resolveProposalChips(proposal: ParsedCommand): List<ProposalChipData> {
    return listOf(
        ProposalChipData(chipType = "Title", label = "Title", value = proposal.title),
        ProposalChipData(chipType = "Date", label = "Date", value = proposal.date ?: "Today"),
        ProposalChipData(chipType = "Time", label = "Time", value = proposal.time ?: "Flexible"),
        ProposalChipData(chipType = "Type", label = "Type", value = proposal.intent.name),
        ProposalChipData(chipType = "Urgency", label = "Urgency", value = proposal.deliveryIntensity)
    )
}

/**
 * Universal Quick Capture Floating Drawer / Bottom Sheet.
 *
 * Sourced 1:1 from `EXPERIENCE.md` §3/§4 and `AD-8`:
 * - Dual voice/text input with real-time waveform visualizer
 * - 3-tier confidence routing UI:
 *   1) High Confidence (>= 0.85): 5-second bottom undo toast banner
 *   2) Medium Confidence (0.50..0.85): 1-tap confirmation chips (Title, Date, Time, Type, Urgency)
 *   3) Low Confidence (< 0.50): Saved to unfiled inbox notice
 * - Strictly enforces 48dp minimum touch target floor and token consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    inputText: String = "",
    onInputTextChanged: (String) -> Unit = {},
    onSubmitText: (String) -> Unit = {},
    isRecording: Boolean = false,
    recordingAmplitude: Float = 0f,
    onToggleRecording: () -> Unit = {},
    mediumConfidenceProposal: ParsedCommand? = null,
    onConfirmProposal: (ParsedCommand) -> Unit = {},
    onDiscardProposal: () -> Unit = {},
    onProposalChipClick: (chipType: String) -> Unit = {},
    highConfidenceCommittedTitle: String? = null,
    onUndoHighConfidence: () -> Unit = {},
    onDismissUndoBanner: () -> Unit = {},
    lowConfidenceNotice: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isVisible && highConfidenceCommittedTitle == null) return

    val colors = PersonalTrackerTheme.colors
    val shapes = PersonalTrackerTheme.shapes

    // 5-second automatic countdown for high-confidence undo toast banner
    LaunchedEffect(highConfidenceCommittedTitle) {
        if (highConfidenceCommittedTitle != null) {
            delay(5000L)
            onDismissUndoBanner()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = colors.surfaceBase,
            shape = shapes.xxxl,
            modifier = modifier
        ) {
            QuickCaptureSheetContent(
                inputText = inputText,
                onInputTextChanged = onInputTextChanged,
                onSubmitText = onSubmitText,
                isRecording = isRecording,
                recordingAmplitude = recordingAmplitude,
                onToggleRecording = onToggleRecording,
                mediumConfidenceProposal = mediumConfidenceProposal,
                onConfirmProposal = onConfirmProposal,
                onDiscardProposal = onDiscardProposal,
                onProposalChipClick = onProposalChipClick,
                lowConfidenceNotice = lowConfidenceNotice,
                onClose = onDismissRequest
            )
        }
    }

    // High Confidence 5s Reversible Undo Toast Banner (rendered at root overlay level)
    AnimatedVisibility(
        visible = highConfidenceCommittedTitle != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (highConfidenceCommittedTitle != null) {
            HighConfidenceUndoToast(
                title = highConfidenceCommittedTitle,
                onUndo = onUndoHighConfidence,
                onDismiss = onDismissUndoBanner
            )
        }
    }
}

/**
 * Main content body for [QuickCaptureSheet].
 */
@Composable
fun QuickCaptureSheetContent(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSubmitText: (String) -> Unit,
    isRecording: Boolean,
    recordingAmplitude: Float,
    onToggleRecording: () -> Unit,
    mediumConfidenceProposal: ParsedCommand?,
    onConfirmProposal: (ParsedCommand) -> Unit,
    onDiscardProposal: () -> Unit,
    lowConfidenceNotice: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onProposalChipClick: (chipType: String) -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.spacing4)
            .padding(bottom = spacing.spacing6)
            .navigationBarsPadding()
    ) {
        // Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Quick Capture",
                style = typography.greetingTitle,
                color = colors.inkPrimary
            )

            Surface(
                shape = shapes.full,
                color = colors.surfaceSunken,
                modifier = Modifier.size(spacing.spacing10)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Quick Capture",
                        tint = colors.inkPrimary
                    )
                }
            }
        }

        // Low Confidence Notice (< 0.50)
        if (!lowConfidenceNotice.isNullOrBlank()) {
            Surface(
                shape = shapes.md,
                color = colors.infoSubtle,
                border = BorderStroke(spacing.spacing0_5, colors.info),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = lowConfidenceNotice,
                    style = typography.bodySecondary,
                    color = colors.inkPrimary,
                    modifier = Modifier.padding(spacing.spacing3)
                )
            }
        }

        // Medium Confidence 1-Tap Confirmation Proposal (0.50..0.85)
        if (mediumConfidenceProposal != null) {
            MediumConfidenceProposalCard(
                proposal = mediumConfidenceProposal,
                onConfirm = { onConfirmProposal(mediumConfidenceProposal) },
                onDiscard = onDiscardProposal,
                onChipClick = onProposalChipClick
            )
        }

        // Voice Recording Visualizer feedback area
        if (isRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.spacing2)
            ) {
                WaveformVisualizer(
                    amplitude = recordingAmplitude,
                    isRecording = true,
                    useBarsLayout = true
                )
            }
        }

        // Dual Voice / Text Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = if (isRecording) "Listening..." else "Capture a task, habit, or note...",
                        style = typography.body,
                        color = colors.inkTertiary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmitText(inputText) }),
                shape = shapes.full,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.surfaceRaised,
                    unfocusedContainerColor = colors.surfaceRaised,
                    cursorColor = colors.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            )

            // Voice Capture Toggle Button (fixed 48dp circular button to avoid distortion)
            Surface(
                shape = shapes.full,
                color = if (isRecording) colors.danger else colors.primaryDark,
                modifier = Modifier
                    .size(spacing.minTouchTarget)
                    .clickable(
                        role = Role.Button,
                        onClick = onToggleRecording
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_waveform),
                        contentDescription = if (isRecording) "Stop voice recording" else "Start voice recording",
                        tint = colors.surfaceRaised,
                        modifier = Modifier.size(spacing.spacing6)
                    )
                }
            }

            // Submit Text Button (48dp floor)
            if (inputText.isNotBlank() && !isRecording) {
                Button(
                    onClick = { onSubmitText(inputText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceRaised
                    ),
                    shape = shapes.full,
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Submit quick capture",
                        tint = colors.surfaceRaised,
                        modifier = Modifier.size(spacing.spacing4)
                    )
                }
            }
        }
    }
}

/**
 * 1-Tap Confirmation Chips Row for Medium-Confidence Captures (0.50..0.85).
 */
@Composable
fun MediumConfidenceProposalCard(
    proposal: ParsedCommand,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    onChipClick: (chipType: String) -> Unit = {}
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val chips = resolveProposalChips(proposal)

    Surface(
        shape = shapes.xxl,
        color = colors.surfaceRaised,
        border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
            modifier = Modifier.padding(spacing.spacing4)
        ) {
            Text(
                text = "Does this look right?",
                style = typography.title,
                color = colors.inkPrimary
            )

            // Horizontal scrolling chip row for Title, Date, Time, Type, Urgency
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                chips.forEach { chip ->
                    ConfirmationChip(
                        label = chip.label,
                        value = chip.value,
                        onClick = { onChipClick(chip.chipType) }
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceRaised
                    ),
                    shape = shapes.full,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Confirm", style = typography.ctaText)
                }

                OutlinedButton(
                    onClick = onDiscard,
                    shape = shapes.full,
                    border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.inkPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Discard", style = typography.label)
                }
            }
        }
    }
}

/**
 * Individual 1-tap confirmation chip with 48dp touch target for inline editing.
 */
@Composable
fun ConfirmationChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(
                minWidth = spacing.minTouchTarget,
                minHeight = spacing.minTouchTarget
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = "Edit $label: $value"
            }
    ) {
        Surface(
            shape = shapes.full,
            color = colors.surfaceRaised,
            border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
            modifier = Modifier.defaultMinSize(minHeight = spacing.chipHeight)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
                modifier = Modifier.padding(
                    horizontal = spacing.spacing3,
                    vertical = spacing.spacing1
                )
            ) {
                Text(
                    text = "$label:",
                    style = typography.caption,
                    color = colors.inkTertiary
                )
                Text(
                    text = value,
                    style = typography.data,
                    color = colors.inkPrimary
                )
            }
        }
    }
}

/**
 * 5-second Reversible Undo Toast Pill for High Confidence Captures (>= 0.85).
 * Anchored above the bottom navigation bar and navigation bar insets.
 */
@Composable
fun HighConfidenceUndoToast(
    title: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = spacing.navBarHeight + spacing.spacing2)
    ) {
        Surface(
            shape = shapes.full,
            color = colors.inkPrimary,
            shadowElevation = elevation.medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenPaddingHorizontal)
                .semantics {
                    contentDescription = "Added: $title. 5-second undo window available."
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(
                    horizontal = spacing.spacing4,
                    vertical = spacing.spacing1
                )
            ) {
                Text(
                    text = "Added: $title",
                    style = typography.bodyMedium,
                    color = colors.surfaceRaised,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                        onUndo()
                        onDismiss()
                    },
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Text(
                        text = "Undo",
                        style = typography.label,
                        color = colors.bannerPeach
                    )
                }
            }
        }
    }
}
