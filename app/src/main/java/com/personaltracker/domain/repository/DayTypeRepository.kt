package com.personaltracker.domain.repository

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for DayType schedule templates and date overrides (FR-3, AD-1).
 */
interface DayTypeRepository {
    fun getAllDayTypes(): Flow<List<DayType>>
    suspend fun getDayTypeById(id: String): DayType?
    suspend fun saveDayType(dayType: DayType)
    suspend fun saveDayTypes(dayTypes: List<DayType>)
    suspend fun ensureDefaultDayTypes(): List<DayType>
    suspend fun getOverrideForDate(date: String): DayOverride?
    fun observeOverrideForDate(date: String): Flow<DayOverride?>
    suspend fun setOverrideForDate(override: DayOverride)
    suspend fun deleteOverrideForDate(date: String)
}
