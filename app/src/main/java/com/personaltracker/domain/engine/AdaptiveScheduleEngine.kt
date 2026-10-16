package com.personaltracker.domain.engine

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pure Kotlin behavioral engine managing DayType hierarchy resolution, routine duration scaling,
 * and mid-day sequential timeline recalculation (FR-3, AD-1, AD-3).
 *
 * Contains zero Android framework dependencies.
 */
class AdaptiveScheduleEngine {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Resolves the active DayType template according to the strict priority:
     * 1. Date-specific user override (if present and valid in availableDayTypes)
     * 2. Day-of-week default matching the target date
     * 3. Default template (isDefault == true)
     * 4. First available DayType as fallback
     *
     * @param date ISO-8601 calendar string "YYYY-MM-DD"
     * @param override Date-specific override if set by user
     * @param availableDayTypes Full list of available DayType templates
     * @return The resolved DayType
     */
    fun resolveActiveDayType(
        date: String,
        override: DayOverride?,
        availableDayTypes: List<DayType>
    ): DayType {
        require(availableDayTypes.isNotEmpty()) { "availableDayTypes list must not be empty" }

        // 1. User override for this specific date
        if (override != null) {
            val matchedOverride = availableDayTypes.find { it.id == override.dayTypeId }
            if (matchedOverride != null) {
                return matchedOverride
            }
        }

        // 2. Day-of-week default template
        val localDate = runCatching { LocalDate.parse(date) }.getOrNull()
        if (localDate != null) {
            val dayOfWeekValue = localDate.dayOfWeek.value.toString() // "1" for Monday to "7" for Sunday
            val dayName = localDate.dayOfWeek.name // "MONDAY", "TUESDAY", etc.

            val matchedDayOfWeek = availableDayTypes.find { dt ->
                val days = dt.defaultDaysOfWeek.split(",").map { it.trim() }
                days.contains(dayOfWeekValue) || days.any { it.equals(dayName, ignoreCase = true) }
            }
            if (matchedDayOfWeek != null) {
                return matchedDayOfWeek
            }
        }

        // 3. Fallback to default template (isDefault == true)
        val defaultTemplate = availableDayTypes.find { it.isDefault }
        if (defaultTemplate != null) {
            return defaultTemplate
        }

        // 4. Fallback to first available DayType
        return availableDayTypes.first()
    }

    /**
     * Represents a scheduled step in a sequential routine execution.
     */
    data class ScheduledStep(
        val stepId: String,
        val orderIndex: Int,
        val title: String,
        val durationMinutes: Int,
        val startTime: String, // "HH:mm"
        val endTime: String,   // "HH:mm"
        val isCompleted: Boolean = false
    )

    /**
     * Recalculates routine steps sequentially.
     * When delayed, completed steps remain unchanged at their original time,
     * while uncompleted steps are shifted sequentially forward starting from the delayed time.
     *
     * @param steps Ordered list of routine steps
     * @param targetStartTime Base target start time formatted as "HH:mm"
     * @param delayMinutes Number of minutes to shift uncompleted steps forward
     * @param completedStepIds Set of step IDs that are already completed
     * @return Ordered list of recalculated ScheduledStep items
     */
    fun recalculateSequentialRoutine(
        steps: List<RoutineStep>,
        targetStartTime: String,
        delayMinutes: Int = 0,
        completedStepIds: Set<String> = emptySet()
    ): List<ScheduledStep> {
        if (steps.isEmpty()) return emptyList()

        val sortedSteps = steps.sortedBy { it.orderIndex }
        val baseStart = runCatching {
            LocalTime.parse(targetStartTime, timeFormatter)
        }.getOrDefault(LocalTime.of(8, 0))

        val result = mutableListOf<ScheduledStep>()
        var currentUncompletedTime: LocalTime? = null
        var originalTimelineCursor = baseStart

        for (step in sortedSteps) {
            val isCompleted = completedStepIds.contains(step.id)
            val duration = step.durationMinutes.coerceAtLeast(1)

            if (isCompleted) {
                // Completed step keeps original scheduled timeline, unless an uncompleted delay has already moved past it
                var stepStart = originalTimelineCursor
                if (currentUncompletedTime != null && stepStart.isBefore(currentUncompletedTime)) {
                    stepStart = currentUncompletedTime
                }
                val stepEnd = stepStart.plusMinutes(duration.toLong())
                result.add(
                    ScheduledStep(
                        stepId = step.id,
                        orderIndex = step.orderIndex,
                        title = step.title,
                        durationMinutes = step.durationMinutes,
                        startTime = stepStart.format(timeFormatter),
                        endTime = stepEnd.format(timeFormatter),
                        isCompleted = true
                    )
                )
                originalTimelineCursor = stepEnd
                if (currentUncompletedTime != null && currentUncompletedTime.isBefore(stepEnd)) {
                    currentUncompletedTime = stepEnd
                }
            } else {
                // Initialize uncompleted timeline cursor if not yet initialized
                if (currentUncompletedTime == null) {
                    currentUncompletedTime = originalTimelineCursor.plusMinutes(delayMinutes.toLong().coerceAtLeast(0L))
                }
                val stepStart = currentUncompletedTime
                val stepEnd = stepStart.plusMinutes(duration.toLong())
                result.add(
                    ScheduledStep(
                        stepId = step.id,
                        orderIndex = step.orderIndex,
                        title = step.title,
                        durationMinutes = step.durationMinutes,
                        startTime = stepStart.format(timeFormatter),
                        endTime = stepEnd.format(timeFormatter),
                        isCompleted = false
                    )
                )
                currentUncompletedTime = stepEnd
                originalTimelineCursor = originalTimelineCursor.plusMinutes(duration.toLong())
            }
        }

        return result
    }

    /**
     * Dynamically scales routine step durations proportionally to match a new target total duration.
     *
     * @param routine Routine containing template steps
     * @param targetTotalMinutes Desired total duration in minutes
     * @return New Routine with scaled step durations and updated totalEstimatedMinutes
     */
    fun scaleRoutineDurations(
        routine: Routine,
        targetTotalMinutes: Int
    ): Routine {
        require(targetTotalMinutes > 0) { "targetTotalMinutes must be positive" }
        if (routine.steps.isEmpty() || routine.totalEstimatedMinutes <= 0) {
            return routine.copy(totalEstimatedMinutes = targetTotalMinutes)
        }

        val safeTarget = targetTotalMinutes.coerceAtLeast(routine.steps.size)
        val scaleFactor = safeTarget.toDouble() / routine.totalEstimatedMinutes.toDouble()
        var accumulatedMinutes = 0
        val scaledSteps = mutableListOf<RoutineStep>()

        for (i in routine.steps.indices) {
            val step = routine.steps[i]
            val remainingSteps = routine.steps.size - 1 - i
            val scaledDuration = if (i == routine.steps.lastIndex) {
                // Final step takes remaining balance so sum is exactly safeTarget
                safeTarget - accumulatedMinutes
            } else {
                val maxAllowedForStep = safeTarget - accumulatedMinutes - remainingSteps
                val calculated = Math.round(step.durationMinutes * scaleFactor).toInt().coerceIn(1, maxAllowedForStep)
                accumulatedMinutes += calculated
                calculated
            }
            scaledSteps.add(step.copy(durationMinutes = scaledDuration))
        }

        return routine.copy(
            totalEstimatedMinutes = safeTarget,
            steps = scaledSteps
        )
    }
}
