package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.DayOverrideDao
import com.personaltracker.data.local.db.daos.DayTypeDao
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.repository.DayTypeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DayTypeRepositoryImpl @Inject constructor(
    private val dayTypeDao: DayTypeDao,
    private val dayOverrideDao: DayOverrideDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DayTypeRepository {

    override fun getAllDayTypes(): Flow<List<DayType>> =
        dayTypeDao.getAllDayTypes()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getDayTypeById(id: String): DayType? =
        withContext(ioDispatcher) {
            dayTypeDao.getDayTypeById(id)?.toDomain()
        }

    override suspend fun saveDayType(dayType: DayType) =
        withContext(ioDispatcher) {
            dayTypeDao.insertDayType(dayType.toEntity())
        }

    override suspend fun saveDayTypes(dayTypes: List<DayType>) =
        withContext(ioDispatcher) {
            dayTypeDao.insertDayTypes(dayTypes.map { it.toEntity() })
        }

    override suspend fun getOverrideForDate(date: String): DayOverride? =
        withContext(ioDispatcher) {
            dayOverrideDao.getOverrideForDate(date)?.toDomain()
        }

    override fun observeOverrideForDate(date: String): Flow<DayOverride?> =
        dayOverrideDao.observeOverrideForDate(date)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun setOverrideForDate(override: DayOverride) =
        withContext(ioDispatcher) {
            dayOverrideDao.insertOrUpdateOverride(override.toEntity())
        }

    override suspend fun deleteOverrideForDate(date: String) =
        withContext(ioDispatcher) {
            dayOverrideDao.deleteOverrideForDate(date)
        }
}
