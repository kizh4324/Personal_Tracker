package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Result data class for task urgency soft-scarcity evaluation (AD-4, FR-5.2, FR-5.3).
 */
data class TaskUrgencyScarcityResult(
    val activeUrgentCount: Int,
    val isSoftCapReached: Boolean,
    val advisoryMessage: String? = null
)

/**
 * Pure Kotlin domain use case evaluating active urgent task count and soft-scarcity advisory guidance (AD-1, AD-4).
 *
 * Implements non-blocking soft-scarcity guidance:
 * When active URGENT tasks for the day reach >= 2, provides supportive non-blocking guidance;
 * never hard-blocks or silently downgrades the user's intensity selection.
 */
class EvaluateTaskUrgencyScarcityUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    /**
     * Evaluates soft-scarcity for a provided list of tasks, optionally excluding [excludingTaskId] (e.g. when editing).
     */
    operator fun invoke(tasks: List<Task>, excludingTaskId: String? = null): TaskUrgencyScarcityResult {
        val activeUrgentCount = tasks.count { task ->
            task.id != excludingTaskId &&
                task.deliveryIntensity.equals(URGENT_INTENSITY, ignoreCase = true) &&
                !task.state.equals("COMPLETED", ignoreCase = true) &&
                !task.state.equals("CANCELLED", ignoreCase = true)
        }

        val isSoftCapReached = activeUrgentCount >= SOFT_CAP_THRESHOLD
        val advisoryMessage = if (isSoftCapReached) {
            "$activeUrgentCount urgent tasks already active today. Keep focus sharp!"
        } else {
            null
        }

        return TaskUrgencyScarcityResult(
            activeUrgentCount = activeUrgentCount,
            isSoftCapReached = isSoftCapReached,
            advisoryMessage = advisoryMessage
        )
    }

    /**
     * Evaluates soft-scarcity by querying repository tasks for [date].
     */
    suspend operator fun invoke(date: String, excludingTaskId: String? = null): TaskUrgencyScarcityResult {
        val tasks = taskRepository.getTasksForDate(date).first()
        return invoke(tasks, excludingTaskId)
    }

    companion object {
        const val URGENT_INTENSITY = "URGENT"
        const val SOFT_CAP_THRESHOLD = 2
    }
}
