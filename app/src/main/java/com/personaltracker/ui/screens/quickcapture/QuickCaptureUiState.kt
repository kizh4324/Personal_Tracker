package com.personaltracker.ui.screens.quickcapture

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.parser.ParsedCommand

/**
 * Immutable UI state for the ubiquitous Quick Capture sheet (AD-2, AD-8).
 */
data class QuickCaptureUiState(
    val isVisible: Boolean = false,
    val inputText: String = "",
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val recordingAmplitude: Float = 0f,
    val transcriptionStatus: String? = null,
    val partialTranscript: String? = null,
    val mediumProposal: ParsedCommand? = null,
    val highConfidenceCommittedTask: Task? = null,
    val highConfidenceCommittedTitle: String? = null,
    val lowConfidenceNotice: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
