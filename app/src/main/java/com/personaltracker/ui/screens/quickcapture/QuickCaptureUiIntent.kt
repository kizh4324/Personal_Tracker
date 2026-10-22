package com.personaltracker.ui.screens.quickcapture

import com.personaltracker.domain.parser.ParsedCommand

/**
 * Sealed interface representing all user actions on the Quick Capture sheet (AD-2, AD-8).
 */
sealed interface QuickCaptureUiIntent {
    object OpenSheet : QuickCaptureUiIntent
    object DismissSheet : QuickCaptureUiIntent
    data class InputTextChanged(val text: String) : QuickCaptureUiIntent
    data class SubmitText(val text: String) : QuickCaptureUiIntent
    object ToggleRecording : QuickCaptureUiIntent
    data class ConfirmProposal(val proposal: ParsedCommand) : QuickCaptureUiIntent
    object DiscardProposal : QuickCaptureUiIntent
    data class ProposalChipClicked(val chipType: String) : QuickCaptureUiIntent
    object UndoHighConfidence : QuickCaptureUiIntent
    object DismissUndoBanner : QuickCaptureUiIntent
}
