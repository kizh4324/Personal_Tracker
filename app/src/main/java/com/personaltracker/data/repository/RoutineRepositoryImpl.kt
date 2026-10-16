package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.RoutineDao
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.repository.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RoutineRepository {

    override fun getRoutinesForDayType(dayTypeId: String): Flow<List<Routine>> =
        routineDao.getRoutinesForDayType(dayTypeId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getStepsForRoutine(routineId: String): Flow<List<RoutineStep>> =
        routineDao.getStepsForRoutine(routineId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun saveRoutine(routine: Routine) =
        withContext(ioDispatcher) {
            routineDao.upsertRoutine(routine.toEntity())
        }

    override suspend fun saveSteps(steps: List<RoutineStep>) =
        withContext(ioDispatcher) {
            routineDao.upsertSteps(steps.map { it.toEntity() })
        }

    override suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) =
        withContext(ioDispatcher) {
            routineDao.saveRoutineWithSteps(
                routine.toEntity(),
                steps.map { it.copy(routineId = routine.id).toEntity() }
            )
        }

    override suspend fun deleteRoutine(routineId: String) =
        withContext(ioDispatcher) {
            routineDao.deleteRoutine(routineId)
        }
}
