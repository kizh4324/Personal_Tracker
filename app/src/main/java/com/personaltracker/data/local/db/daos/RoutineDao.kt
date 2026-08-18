package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.personaltracker.data.local.db.entities.RoutineEntity
import com.personaltracker.data.local.db.entities.RoutineStepEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Routines and Routine Steps.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface RoutineDao {

    @Query("SELECT * FROM routines WHERE dayTypeId = :dayTypeId ORDER BY targetStartTime ASC")
    fun getRoutinesForDayType(dayTypeId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getStepsForRoutine(routineId: String): Flow<List<RoutineStepEntity>>

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertSteps(steps: List<RoutineStepEntity>)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteStepsForRoutine(routineId: String)

    @Transaction
    suspend fun saveRoutineWithSteps(routine: RoutineEntity, steps: List<RoutineStepEntity>) {
        upsertRoutine(routine)
        deleteStepsForRoutine(routine.id)
        upsertSteps(steps)
    }

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)
}
