package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.repository.DayTypeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pure Kotlin use case resolving today's active DayType template according to:
 * 1. Date-specific user override
 * 2. Day-of-week default
 * 3. Default template fallback
 *
 * Implements FR-3, AD-1, AD-3.
 */
class ResolveDayTypeUseCase @Inject constructor(
    private val dayTypeRepository: DayTypeRepository,
    private val adaptiveScheduleEngine: AdaptiveScheduleEngine
) {
    companion object {
        val SAFE_DEFAULT_DAY_TYPE = DayType(
            id = "dt_default_fallback",
            name = "Default Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = ""
        )
    }

    suspend operator fun invoke(date: String = LocalDate.now().toString()): DayType {
        val override = dayTypeRepository.getOverrideForDate(date)
        val allDayTypes = dayTypeRepository.getAllDayTypes().firstOrNull() ?: emptyList()
        if (allDayTypes.isEmpty()) {
            return SAFE_DEFAULT_DAY_TYPE
        }
        return adaptiveScheduleEngine.resolveActiveDayType(date, override, allDayTypes)
    }
}
