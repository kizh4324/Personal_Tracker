package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing an unfiled capture item for manual triage (FR-7.5, AD-1, AD-8).
 */
data class UnfiledInboxItem(
    val id: String,
    val rawTranscriptText: String,
    val draftAttributesJson: String,
    val parserConfidence: Float,
    val captureSource: String, // "OFFLINE_STT", "GEMINI_LIVE", "TEXT_QUICK_ADD"
    val createdTimestamp: Long = System.currentTimeMillis()
)
