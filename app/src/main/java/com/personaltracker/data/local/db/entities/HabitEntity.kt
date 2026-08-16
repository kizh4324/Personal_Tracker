package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Habit domain entity supporting streak slack and freeze days (FR-2.4).
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String, // UUIDv4
    val name: String,
    val description: String?,
    val targetDaysOfWeek: String, // e.g. "1,2,3,4,5,6,7"
    val currentStreak: Int,
    val bestStreak: Int,
    val slackBankDays: Int,
    val isFreezeActive: Boolean,
    val createdTimestamp: Long
)

/**
 * Daily habit completion log entity (FR-2.4).
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index(value = ["habitId", "logDate"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey val id: String, // UUIDv4
    val habitId: String,
    val logDate: String, // "YYYY-MM-DD"
    val status: String, // "COMPLETED", "MISSED", "SLACK_USED", "FROZEN"
    val loggedTimestamp: Long
)
