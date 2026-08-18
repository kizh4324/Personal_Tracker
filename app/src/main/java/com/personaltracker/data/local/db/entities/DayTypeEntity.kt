package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Baseline DayType schedule template entity (FR-3, Decision 3).
 */
@Entity(tableName = "day_types")
data class DayTypeEntity(
    @PrimaryKey val id: String, // e.g., "weekday_standard", "heavy_study", "weekend_rest"
    val name: String,
    val colorHex: String,
    val targetStudyMinutes: Int,
    val isDefault: Boolean,
    val defaultDaysOfWeek: String // Comma-separated ISO day numbers: "1,2,3,4,5" (Mon-Fri)
)

/**
 * Specific single-date user schedule override entity (FR-3.1, FR-3.4).
 */
@Entity(
    tableName = "day_overrides",
    foreignKeys = [
        ForeignKey(
            entity = DayTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("dayTypeId")]
)
data class DayOverrideEntity(
    @PrimaryKey val date: String, // ISO-8601 format: "YYYY-MM-DD"
    val dayTypeId: String,
    val isUserModified: Boolean,
    val createdTimestamp: Long
)
