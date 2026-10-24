package com.personaltracker.domain.model

/**
 * Domain DTO for an active study or focus session (AD-1).
 */
data class HeroActiveSession(
    val subjectTag: String,
    val remainingSeconds: Long = 0L,
    val formattedRemainingTime: String = "00:00",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false
)

/**
 * Pure Kotlin sealed interface representing the 6 Hero priority states (AD-1, AD-3).
 *
 * Priority order (1 highest to 6 lowest):
 * 1. ActiveFocus
 * 2. InterruptedResumable
 * 3. MorningBanner
 * 4. UpcomingItem
 * 5. CarryForwardReview
 * 6. IntentionalIdle
 */
sealed interface HeroPriorityState {
    val priority: Int

    data class ActiveFocus(
        val session: HeroActiveSession
    ) : HeroPriorityState {
        override val priority: Int = 1
    }

    data class InterruptedResumable(
        val taskId: String? = null,
        val title: String,
        val elapsedSinceFormatted: String = "Paused"
    ) : HeroPriorityState {
        override val priority: Int = 2
    }

    data class MorningBanner(
        val dayTypeName: String,
        val dayTypeId: String? = null
    ) : HeroPriorityState {
        override val priority: Int = 3
    }

    data class UpcomingItem(
        val task: Task? = null,
        val activeRoutineStep: RoutineStep? = null,
        val startsInMinutes: Int = task?.let { maxOf(0, it.estimatedDurationMinutes) } ?: activeRoutineStep?.let { maxOf(0, it.durationMinutes) } ?: 0,
        val taskId: String? = task?.id ?: activeRoutineStep?.id,
        val title: String = task?.title ?: activeRoutineStep?.title.orEmpty()
    ) : HeroPriorityState {
        override val priority: Int = 4
    }

    data class CarryForwardReview(
        val overdueTasks: List<Task>
    ) : HeroPriorityState {
        override val priority: Int = 5
        val itemCount: Int get() = overdueTasks.size
    }

    data class IntentionalIdle(
        val message: String = "All caught up for today. Rest well."
    ) : HeroPriorityState {
        override val priority: Int = 6
    }
}
