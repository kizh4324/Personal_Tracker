package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.HeroActiveSession
import com.personaltracker.domain.model.HeroPriorityState
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import javax.inject.Inject

/**
 * Pure Kotlin use case implementing the 6-state Hero Card priority resolution machine (FR-1, AD-1, AD-3).
 *
 * Evaluates the strict priority hierarchy:
 * 1. ActiveFocus: Live study session timer is running
 * 2. InterruptedResumable: Task in INTERRUPTED state or paused timer
 * 3. MorningBanner: Morning template swap banner not yet dismissed
 * 4. UpcomingItem: Tasks pending for today or active routine step; filters out snoozed items; clamps duration to >= 0
 * 5. CarryForwardReview: Overdue uncompleted tasks from prior days
 * 6. IntentionalIdle: Fallback when all items are caught up or snoozed
 */
class ResolveHeroStateUseCase @Inject constructor() {

    fun resolve(
        activeSession: HeroActiveSession? = null,
        interruptedTask: Task? = null,
        isMorningBannerDismissed: Boolean = false,
        activeDayTypeName: String = "Standard Routine",
        activeDayTypeId: String? = null,
        pendingTasks: List<Task> = emptyList(),
        overdueTasks: List<Task> = emptyList(),
        snoozedTaskIds: Set<String> = emptySet(),
        activeRoutineStep: RoutineStep? = null,
        isRoutineStepCompleted: Boolean = false
    ): HeroPriorityState {
        // Priority 1: ActiveFocus
        if (activeSession != null && activeSession.isRunning && !activeSession.isCompleted) {
            return HeroPriorityState.ActiveFocus(activeSession)
        }

        // Priority 2: InterruptedResumable
        if (interruptedTask != null && interruptedTask.state.equals("INTERRUPTED", ignoreCase = true)) {
            return HeroPriorityState.InterruptedResumable(
                taskId = interruptedTask.id,
                title = interruptedTask.title,
                elapsedSinceFormatted = "Paused"
            )
        } else if (activeSession != null && activeSession.isPaused && !activeSession.isCompleted) {
            return HeroPriorityState.InterruptedResumable(
                taskId = null,
                title = "${activeSession.subjectTag} Session",
                elapsedSinceFormatted = "Paused"
            )
        }

        // Priority 3: MorningBanner
        if (!isMorningBannerDismissed) {
            return HeroPriorityState.MorningBanner(
                dayTypeName = activeDayTypeName,
                dayTypeId = activeDayTypeId
            )
        }

        // Priority 4: UpcomingItem (Task or Routine Step)
        val nextUpcomingTask = pendingTasks.firstOrNull { task ->
            !task.state.equals("COMPLETED", ignoreCase = true) &&
            !task.state.equals("CANCELLED", ignoreCase = true) &&
            task.id !in snoozedTaskIds
        }
        if (nextUpcomingTask != null) {
            return HeroPriorityState.UpcomingItem(
                task = nextUpcomingTask,
                startsInMinutes = maxOf(0, nextUpcomingTask.estimatedDurationMinutes)
            )
        } else if (activeRoutineStep != null && !isRoutineStepCompleted && activeRoutineStep.id !in snoozedTaskIds) {
            return HeroPriorityState.UpcomingItem(
                activeRoutineStep = activeRoutineStep,
                startsInMinutes = maxOf(0, activeRoutineStep.durationMinutes)
            )
        }

        // Priority 5: CarryForwardReview
        val pendingOverdue = overdueTasks.filter { task ->
            !task.state.equals("COMPLETED", ignoreCase = true) &&
            !task.state.equals("CANCELLED", ignoreCase = true)
        }
        if (pendingOverdue.isNotEmpty()) {
            return HeroPriorityState.CarryForwardReview(
                overdueTasks = pendingOverdue
            )
        }

        // Priority 6: IntentionalIdle
        return HeroPriorityState.IntentionalIdle(
            message = "All caught up for today. Rest well."
        )
    }

    operator fun invoke(
        activeSession: HeroActiveSession? = null,
        interruptedTask: Task? = null,
        isMorningBannerDismissed: Boolean = false,
        activeDayTypeName: String = "Standard Routine",
        activeDayTypeId: String? = null,
        pendingTasks: List<Task> = emptyList(),
        overdueTasks: List<Task> = emptyList(),
        snoozedTaskIds: Set<String> = emptySet(),
        activeRoutineStep: RoutineStep? = null,
        isRoutineStepCompleted: Boolean = false
    ): HeroPriorityState = resolve(
        activeSession = activeSession,
        interruptedTask = interruptedTask,
        isMorningBannerDismissed = isMorningBannerDismissed,
        activeDayTypeName = activeDayTypeName,
        activeDayTypeId = activeDayTypeId,
        pendingTasks = pendingTasks,
        overdueTasks = overdueTasks,
        snoozedTaskIds = snoozedTaskIds,
        activeRoutineStep = activeRoutineStep,
        isRoutineStepCompleted = isRoutineStepCompleted
    )
}
