package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UrgencyScarcityPolicy
import com.personaltracker.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tiered urgency soft-cap availability state for a given day (AD-1, AD-4).
 */
enum class UrgencyScarcityTier {
    AVAILABLE,
    APPROACHING,
    AT_OR_OVER_CAP
}

/**
 * Result data class for task urgency soft-scarcity evaluation (AD-4, FR-5.2, FR-5.3).
 */
data class TaskUrgencyScarcityResult(
    val activeUrgentCount: Int,
    val isSoftCapReached: Boolean,
    val advisoryMessage: String? = null,
    val maxActiveUrgent: Int = EvaluateTaskUrgencyScarcityUseCase.SOFT_CAP_THRESHOLD,
    val remainingSlots: Int = (maxActiveUrgent - activeUrgentCount).coerceAtLeast(0),
    val tier: UrgencyScarcityTier = when {
        isSoftCapReached || activeUrgentCount >= maxActiveUrgent -> UrgencyScarcityTier.AT_OR_OVER_CAP
        activeUrgentCount > 0 && activeUrgentCount >= maxActiveUrgent - 1 -> UrgencyScarcityTier.APPROACHING
        else -> UrgencyScarcityTier.AVAILABLE
    }
)

/**
 * Pure Kotlin domain use case evaluating active urgent task count and soft-scarcity advisory guidance (AD-1, AD-4).
 *
 * Implements non-blocking soft-scarcity guidance:
 * When active URGENT tasks for the day reach the configurable policy ceiling (default 2), provides supportive
 * non-blocking guidance; never hard-blocks or silently downgrades the user's intensity selection.
 */
class EvaluateTaskUrgencyScarcityUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    /**
     * Evaluates soft-scarcity for a provided list of tasks using the default [UrgencyScarcityPolicy],
     * optionally excluding [excludingTaskId] (e.g. when editing).
     */
    operator fun invoke(tasks: List<Task>, excludingTaskId: String? = null): TaskUrgencyScarcityResult =
        invoke(tasks, excludingTaskId, UrgencyScarcityPolicy())

    /**
     * Evaluates soft-scarcity for a provided list of tasks against an explicit [policy],
     * optionally excluding [excludingTaskId].
     */
    operator fun invoke(
        tasks: List<Task>,
        excludingTaskId: String?,
        policy: UrgencyScarcityPolicy
    ): TaskUrgencyScarcityResult {
        val activeUrgentCount = tasks.count { task ->
            task.id != excludingTaskId &&
                task.deliveryIntensity.equals(URGENT_INTENSITY, ignoreCase = true) &&
                !task.state.equals("COMPLETED", ignoreCase = true) &&
                !task.state.equals("CANCELLED", ignoreCase = true)
        }

        val maxActiveUrgent = policy.maxActiveUrgent
        val isSoftCapReached = activeUrgentCount >= maxActiveUrgent
        val remainingSlots = (maxActiveUrgent - activeUrgentCount).coerceAtLeast(0)
        val tier = when {
            activeUrgentCount >= maxActiveUrgent -> UrgencyScarcityTier.AT_OR_OVER_CAP
            activeUrgentCount > 0 && activeUrgentCount >= maxActiveUrgent - 1 -> UrgencyScarcityTier.APPROACHING
            else -> UrgencyScarcityTier.AVAILABLE
        }
        val advisoryMessage = if (isSoftCapReached) {
            if (activeUrgentCount == 1) {
                "1 urgent task already active today. Keep focus sharp!"
            } else {
                "$activeUrgentCount urgent tasks already active today. Keep focus sharp!"
            }
        } else {
            null
        }

        return TaskUrgencyScarcityResult(
            activeUrgentCount = activeUrgentCount,
            isSoftCapReached = isSoftCapReached,
            advisoryMessage = advisoryMessage,
            maxActiveUrgent = maxActiveUrgent,
            remainingSlots = remainingSlots,
            tier = tier
        )
    }

    /**
     * Evaluates soft-scarcity by querying repository tasks for [date] using the default [UrgencyScarcityPolicy].
     */
    suspend operator fun invoke(date: String, excludingTaskId: String? = null): TaskUrgencyScarcityResult =
        invoke(date, excludingTaskId, UrgencyScarcityPolicy())

    /**
     * Evaluates soft-scarcity by querying repository tasks for [date] against an explicit [policy].
     */
    suspend operator fun invoke(
        date: String,
        excludingTaskId: String?,
        policy: UrgencyScarcityPolicy
    ): TaskUrgencyScarcityResult {
        val tasks = taskRepository.getTasksForDate(date).first()
        return invoke(tasks, excludingTaskId, policy)
    }

    companion object {
        const val URGENT_INTENSITY = "URGENT"
        const val SOFT_CAP_THRESHOLD = UrgencyScarcityPolicy.DEFAULT_MAX_ACTIVE_URGENT
    }
}
