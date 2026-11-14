package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.HeroActiveSession
import com.personaltracker.domain.model.HeroPriorityState
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import javax.inject.Inject

/**
 * Pure Kotlin use case implementing the 6-state Hero Card priority resolution machine (FR-1, AD-1, AD-3).
 *
 * Evaluates the strict priority hierarchy:
 * 1. ActiveFocus: Live study session timer is running
 * 2. InterruptedResumable: Active paused focus session, interrupted study session from Room, or Task in INTERRUPTED state
 * 3. MorningBanner: Morning template swap banner not yet dismissed
 * 4. UpcomingItem: Tasks pending for today or active routine step; filters out snoozed items; clamps duration to >= 0
 * 5. CarryForwardReview: Overdue uncompleted tasks from prior days
 * 6. IntentionalIdle: Fallback when all items are caught up or snoozed
 */
class ResolveHeroStateUseCase @Inject constructor() {

    fun resolve(
        activeSession: HeroActiveSession? = null,
        interruptedSession: StudySession? = null,
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
        // 2A: Live in-memory paused timer session (State A)
        if (activeSession != null && activeSession.isPaused && !activeSession.isCompleted) {
            val remainingMin = (activeSession.remainingSeconds / 60).toInt()
            return HeroPriorityState.InterruptedResumable(
                sessionId = activeSession.sessionId,
                title = "${activeSession.subjectTag} Session",
                elapsedSinceFormatted = "Paused",
                bankedMinutes = activeSession.bankedMinutes,
                remainingMinutes = remainingMin,
                advisoryPrompt = if (activeSession.bankedMinutes > 0)
                    "Pick back up where you left off — ${activeSession.bankedMinutes} min banked!"
                else
                    "Ready to jump back in whenever you are.",
                isStudySession = true
            )
        }

        // 2B: Interrupted study session from Room (State B)
        if (interruptedSession != null && interruptedSession.isInterrupted &&
            interruptedSession.actualElapsedMinutes < interruptedSession.targetDurationMinutes
        ) {
            val banked = interruptedSession.actualElapsedMinutes
            val remaining = maxOf(1, interruptedSession.targetDurationMinutes - banked)
            return HeroPriorityState.InterruptedResumable(
                sessionId = interruptedSession.id,
                title = "${interruptedSession.subjectTag} Session",
                elapsedSinceFormatted = "$banked min banked",
                bankedMinutes = banked,
                remainingMinutes = remaining,
                advisoryPrompt = "Pick back up where you left off — $banked min banked!",
                isStudySession = true
            )
        }

        // 2C: Interrupted Task
        if (interruptedTask != null && interruptedTask.state.equals("INTERRUPTED", ignoreCase = true)) {
            return HeroPriorityState.InterruptedResumable(
                taskId = interruptedTask.id,
                title = interruptedTask.title,
                elapsedSinceFormatted = "Paused",
                isStudySession = false
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
        interruptedSession: StudySession? = null,
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
        interruptedSession = interruptedSession,
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
