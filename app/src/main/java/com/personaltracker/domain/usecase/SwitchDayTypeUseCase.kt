package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject

/**
 * Result data class for DayType template switch actions.
 */
data class DayTypeSwitchResult(
    val activeDayType: DayType,
    val isOverrideActive: Boolean,
    val scaledRoutineStepCount: Int = 0,
    val message: String
)

/**
 * Pure Kotlin use case executing date-specific DayType template swaps and schedule scaling.
 *
 * Persists single-date overrides on Dispatchers.IO, adapts uncompleted routine step durations
 * via AdaptiveScheduleEngine while leaving base templates unmutated, and produces zero-guilt microcopy (AD-1, AD-2, AD-4, AD-6).
 */
class SwitchDayTypeUseCase @Inject constructor(
    private val dayTypeRepository: DayTypeRepository,
    private val routineRepository: RoutineRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val adaptiveScheduleEngine: AdaptiveScheduleEngine
) {

    suspend operator fun invoke(
        targetDayTypeId: String,
        date: String = LocalDate.now().toString(),
        completedStepIds: Set<String> = emptySet()
    ): DayTypeSwitchResult {
        // Resolve target DayType and ensure it exists in repository to satisfy foreign key constraints
        val targetDayType = dayTypeRepository.getDayTypeById(targetDayTypeId)
            ?: run {
                val seeded = dayTypeRepository.ensureDefaultDayTypes()
                seeded.find { it.id == targetDayTypeId }
            }
            ?: DayType.BUILT_IN_DAY_TYPES.find { it.id == targetDayTypeId }
            ?: return DayTypeSwitchResult(
                activeDayType = resolveDayTypeUseCase(date),
                isOverrideActive = false,
                scaledRoutineStepCount = 0,
                message = "Selected schedule template not found."
            )

        // Ensure target template is saved in persistence to prevent foreign key violation
        if (dayTypeRepository.getDayTypeById(targetDayType.id) == null) {
            dayTypeRepository.saveDayType(targetDayType)
        }

        val override = DayOverride(
            date = date,
            dayTypeId = targetDayType.id,
            isUserModified = true,
            createdTimestamp = System.currentTimeMillis()
        )
        dayTypeRepository.setOverrideForDate(override)

        // Calculate scaled routine step count for the active schedule while leaving base templates unmutated
        var scaledCount = 0
        val routines = routineRepository.getRoutinesForDayType(targetDayType.id).firstOrNull() ?: emptyList()
        for (routine in routines) {
            val steps = routineRepository.getStepsForRoutine(routine.id).firstOrNull() ?: emptyList()
            if (steps.isNotEmpty() && targetDayType.targetStudyMinutes > 0) {
                val fullRoutine = routine.copy(steps = steps)
                val scaled = adaptiveScheduleEngine.scaleRoutineDurations(
                    fullRoutine,
                    targetDayType.targetStudyMinutes
                )
                scaledCount += scaled.steps.count { !completedStepIds.contains(it.id) }
            }
        }

        val message = when (targetDayType.id) {
            "dt_rest" -> "Switched to ${targetDayType.name}. Rest well today."
            "dt_low_capacity" -> "Switched to ${targetDayType.name}. Schedule adjusted gently."
            "dt_crunch" -> "Switched to ${targetDayType.name}. Deep focus blocks ready."
            else -> "Schedule adapted to ${targetDayType.name} for today."
        }

        return DayTypeSwitchResult(
            activeDayType = targetDayType,
            isOverrideActive = true,
            scaledRoutineStepCount = scaledCount,
            message = message
        )
    }

    suspend fun resetOverride(date: String = LocalDate.now().toString()): DayTypeSwitchResult {
        dayTypeRepository.deleteOverrideForDate(date)
        val activeDayType = resolveDayTypeUseCase(date)
        return DayTypeSwitchResult(
            activeDayType = activeDayType,
            isOverrideActive = false,
            scaledRoutineStepCount = 0,
            message = "Schedule reset to ${activeDayType.name} for today."
        )
    }
}
