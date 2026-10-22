package com.personaltracker.ui.screens.quickcapture

/**
 * Sealed interface representing one-shot side effects for Quick Capture (AD-2, AD-8).
 */
sealed interface QuickCaptureUiEffect {
    data class ShowUndoToast(val taskTitle: String) : QuickCaptureUiEffect
    data class ShowSnackbar(val message: String) : QuickCaptureUiEffect
    object CloseSheet : QuickCaptureUiEffect
}
