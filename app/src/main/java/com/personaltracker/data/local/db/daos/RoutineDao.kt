package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.personaltracker.data.local.db.entities.RoutineEntity
import com.personaltracker.data.local.db.entities.RoutineStepEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Routines and Routine Steps.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface RoutineDao {

    @Query("SELECT * FROM routines WHERE dayTypeId = :dayTypeId ORDER BY displayOrder ASC")
    fun getRoutinesForDayType(dayTypeId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getStepsForRoutine(routineId: String): Flow<List<RoutineStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RoutineStepEntity>)

    @Update
    suspend fun updateStep(step: RoutineStepEntity)

    @Query("UPDATE routine_steps SET isCompleted = 0 WHERE routineId = :routineId")
    suspend fun resetRoutineSteps(routineId: String)
}
