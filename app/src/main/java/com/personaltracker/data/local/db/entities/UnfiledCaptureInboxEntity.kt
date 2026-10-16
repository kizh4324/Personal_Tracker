package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Unfiled capture inbox entity for low-confidence or ambiguous voice/text captures (FR-7.5).
 */
@Entity(tableName = "unfiled_capture_inbox")
data class UnfiledCaptureInboxEntity(
    @PrimaryKey val id: String, // UUIDv4
    val rawTranscriptText: String,
    val draftAttributesJson: String, // Serialized PartialTaskDraft DTO
    val parserConfidence: Float, // 0.00 to 1.00
    val captureSource: String, // "OFFLINE_STT", "GEMINI_LIVE", "TEXT_QUICK_ADD"
    val createdTimestamp: Long
)
