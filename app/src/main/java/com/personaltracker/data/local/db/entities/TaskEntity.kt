package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task domain entity (FR-2.2, FR-5.1, Dimension C).
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("scheduledDate"),
        Index("state"),
        Index("deliveryIntensity")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String, // UUIDv4
    val title: String,
    val description: String?,
    val scheduledDate: String, // "YYYY-MM-DD"
    val scheduledTime: String?, // "HH:mm" (optional fixed-time)
    val estimatedDurationMinutes: Int,
    val deliveryIntensity: String, // "ROUTINE", "IMPORTANT", "URGENT" (FR-5.1)
    val state: String, // "PENDING", "IN_PROGRESS", "INTERRUPTED", "COMPLETED", "CANCELLED"
    val isCarryForward: Boolean, // True if rolled over from a previous date (FR-6.1)
    val carryForwardCount: Int,
    val completionTimestamp: Long?,
    val createdTimestamp: Long
)
