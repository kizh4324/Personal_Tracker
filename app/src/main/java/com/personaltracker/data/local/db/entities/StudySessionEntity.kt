package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Study and focus session log entity (FR-2.5, FR-4.1).
 */
@Entity(
    tableName = "study_sessions",
    indices = [
        Index("subjectTag"),
        Index("sessionDate")
    ]
)
data class StudySessionEntity(
    @PrimaryKey val id: String, // UUIDv4
    val subjectTag: String,
    val sessionDate: String, // "YYYY-MM-DD"
    val startTime: String, // "HH:mm"
    val durationMinutes: Int,
    val coinsEarned: Int,
    val linkedTaskId: String?,
    val isInterrupted: Boolean,
    val createdTimestamp: Long
)
