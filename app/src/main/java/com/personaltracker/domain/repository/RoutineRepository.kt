package com.personaltracker.domain.repository

import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for Routine templates and sequential steps (FR-2.3, AD-1).
 */
interface RoutineRepository {
    fun getRoutinesForDayType(dayTypeId: String): Flow<List<Routine>>
    fun getStepsForRoutine(routineId: String): Flow<List<RoutineStep>>
    suspend fun saveRoutine(routine: Routine)
    suspend fun saveSteps(steps: List<RoutineStep>)
    suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>)
    suspend fun deleteRoutine(routineId: String)
}
