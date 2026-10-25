package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ScheduledRoutineStep
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Result data class for routine timeline recalculation.
 */
data class RecalculateTimelineResult(
    val scheduledSteps: List<ScheduledRoutineStep>,
    val delayMinutes: Int,
    val message: String
)

/**
 * Pure Kotlin use case executing mid-day sequential routine recalculation,
 * non-destructive duration scaling, safe 23:59 clamping, and zero-guilt microcopy (FR-3, AD-1, AD-4, AD-6).
 *
 * Contains zero Android framework dependencies.
 */
class RecalculateRoutineTimelineUseCase @Inject constructor(
    private val adaptiveScheduleEngine: AdaptiveScheduleEngine
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    operator fun invoke(
        steps: List<RoutineStep>,
        baseTargetStartTime: String,
        newStartTime: String,
        completedStepIds: Set<String> = emptySet(),
        skippedStepIds: Set<String> = emptySet(),
        targetDayType: DayType? = null
    ): RecalculateTimelineResult {
        if (steps.isEmpty()) {
            return RecalculateTimelineResult(
                scheduledSteps = emptyList(),
                delayMinutes = 0,
                message = "No steps to schedule."
            )
        }

        val sortedSteps = steps.sortedBy { it.orderIndex }
        val baseTime = runCatching {
            LocalTime.parse(baseTargetStartTime, timeFormatter)
        }.getOrDefault(LocalTime.of(8, 0))

        val targetTime = runCatching {
            LocalTime.parse(newStartTime, timeFormatter)
        }.getOrDefault(baseTime)

        // Find when the first uncompleted step was scheduled to begin
        var originalCursor = baseTime
        for (step in sortedSteps) {
            if (completedStepIds.contains(step.id)) {
                originalCursor = originalCursor.plusMinutes(step.durationMinutes.toLong().coerceAtLeast(1))
            } else {
                break
            }
        }

        val delayMinutes = java.time.Duration.between(originalCursor, targetTime).toMinutes().toInt().coerceAtLeast(0)

        // Optionally scale uncompleted step durations if targetDayType specifies a study target
        val effectiveSteps = if (targetDayType != null && targetDayType.targetStudyMinutes > 0) {
            val totalCurrent = steps.sumOf { it.durationMinutes }
            val tempRoutine = Routine(
                id = "temp_routine",
                dayTypeId = targetDayType.id,
                title = "Temp",
                targetStartTime = baseTargetStartTime,
                totalEstimatedMinutes = totalCurrent,
                steps = steps
            )
            val scaled = adaptiveScheduleEngine.scaleRoutineDurations(tempRoutine, targetDayType.targetStudyMinutes)
            steps.map { original ->
                if (completedStepIds.contains(original.id)) {
                    original
                } else {
                    scaled.steps.find { it.id == original.id } ?: original
                }
            }
        } else {
            steps
        }

        // Recalculate sequential timeline via pure engine
        val rawScheduled = adaptiveScheduleEngine.recalculateSequentialRoutine(
            steps = effectiveSteps,
            targetStartTime = baseTargetStartTime,
            delayMinutes = delayMinutes,
            completedStepIds = completedStepIds
        )

        // Identify current cursor (first step that is neither completed nor skipped)
        val resolvedIds = completedStepIds + skippedStepIds
        val currentCursorId = effectiveSteps.sortedBy { it.orderIndex }.firstOrNull { it.id !in resolvedIds }?.id

        val scheduledSteps = rawScheduled.map { scheduled ->
            val matchingStep = effectiveSteps.find { it.id == scheduled.stepId }
                ?: RoutineStep(
                    id = scheduled.stepId,
                    routineId = "",
                    orderIndex = scheduled.orderIndex,
                    title = scheduled.title,
                    durationMinutes = scheduled.durationMinutes
                )

            val isSkipped = skippedStepIds.contains(scheduled.stepId)
            ScheduledRoutineStep(
                step = matchingStep,
                startTime = scheduled.startTime,
                endTime = scheduled.endTime,
                isCompleted = scheduled.isCompleted,
                isSkipped = isSkipped,
                isCurrentCursor = scheduled.stepId == currentCursorId
            )
        }

        return RecalculateTimelineResult(
            scheduledSteps = scheduledSteps,
            delayMinutes = delayMinutes,
            message = "Routine shifted to now. Moving forward smoothly."
        )
    }
}
