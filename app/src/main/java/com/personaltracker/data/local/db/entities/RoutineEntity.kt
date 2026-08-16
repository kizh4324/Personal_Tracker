package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Routine template entity bound to a DayType (FR-2.3).
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
    val name: String,
    val targetStartTime: String, // "HH:mm"
    val isSequential: Boolean,
    val iconIdentifier: String,
    val displayOrder: Int
)

/**
 * Sequential step entity within a routine (FR-2.3).
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
    indices = [Index("routineId")]
)
data class RoutineStepEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val title: String,
    val durationMinutes: Int,
    val orderIndex: Int,
    val isCompleted: Boolean
)
