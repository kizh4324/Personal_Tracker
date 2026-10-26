package com.personaltracker.domain.parser

/**
 * High-level intent classification for offline natural language commands (FR-7, AD-8).
 */
enum class CommandIntent {
    CREATE_TASK,
    LOG_HABIT,
    START_FOCUS,
    SHIFT_ROUTINE,
    UNKNOWN
}

/**
 * 3-tier confidence scoring levels governing AI action commitments (AD-8).
 * - HIGH (>= 0.85): Instant commit with reversible undo toast
 * - MEDIUM (0.50..0.84): 1-tap confirmation chips presented in QuickCaptureSheet
 * - LOW (< 0.50): Saved to UnfiledCaptureInboxEntity for manual triage
 */
enum class ConfidenceTier {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Pure Kotlin DTO representing the outcome of deterministic command parsing (FR-7, AD-8).
 */
data class ParsedCommand(
    val rawText: String,
    val intent: CommandIntent,
    val title: String,
    val date: String? = null, // "YYYY-MM-DD"
    val time: String? = null, // "HH:mm"
    val durationMinutes: Int? = null,
    val deliveryIntensity: String = "ROUTINE", // "ROUTINE", "IMPORTANT", "URGENT"
    val confidence: Float,
    val tier: ConfidenceTier,
    val shiftMinutes: Int? = null,
    val targetRoutineName: String? = null
)
