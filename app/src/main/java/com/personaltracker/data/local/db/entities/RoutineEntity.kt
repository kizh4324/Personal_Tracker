package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Routine template entity bound to a DayType (FR-2.3).
 * Sourced 1:1 from architecture.md §2.1.
 */
@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = DayTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayTypeId")]
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    val dayTypeId: String,
    val title: String,
    val targetStartTime: String, // "HH:mm"
    val totalEstimatedMinutes: Int
)

/**
 * Sequential step entity within a routine template (FR-2.3).
 * Sourced 1:1 from architecture.md §2.1.
 */
@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("routineId"),
        Index(value = ["routineId", "orderIndex"], unique = true)
    ]
)
data class RoutineStepEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val orderIndex: Int,
    val title: String,
    val durationMinutes: Int
)
