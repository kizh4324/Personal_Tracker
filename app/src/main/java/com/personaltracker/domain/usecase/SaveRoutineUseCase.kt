package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.repository.RoutineRepository
import javax.inject.Inject

/**
 * Pure Kotlin use case validating and persisting routine templates and sequential steps (FR-2.3, AD-1, AD-3).
 *
 * Contains zero Android framework dependencies.
 */
class SaveRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {

    suspend operator fun invoke(
        routine: Routine,
        steps: List<RoutineStep> = routine.steps
    ) {
        require(routine.title.isNotBlank()) { "Routine title must not be blank" }
        require(routine.dayTypeId.isNotBlank()) { "dayTypeId must not be blank" }

        for (step in steps) {
            require(step.title.isNotBlank()) { "Routine step title must not be blank" }
            require(step.durationMinutes >= 1) {
                "Routine step duration must be at least 1 minute, but was ${step.durationMinutes} for '${step.title}'."
            }
        }

        val totalMinutes = if (steps.isNotEmpty()) {
            steps.sumOf { it.durationMinutes }
        } else {
            routine.totalEstimatedMinutes
        }

        val updatedRoutine = routine.copy(
            totalEstimatedMinutes = totalMinutes,
            steps = steps
        )

        routineRepository.saveRoutineWithSteps(updatedRoutine, steps)
    }
}
