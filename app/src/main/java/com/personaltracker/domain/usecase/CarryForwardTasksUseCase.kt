package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Result data class for carry-forward operation.
 */
data class CarryForwardResult(
    val updatedTasks: List<Task>,
    val carriedCount: Int
)

/**
 * Pure Kotlin use case executing 1-tap carry-forward rescheduling of overdue tasks (FR-6, AD-1, AD-3, AD-6).
 *
 * Sets scheduledDate to target date (today), sets isCarryForward = true, and increments carryForwardCount.
 * Operates without guilt copy or penalty.
 */
class CarryForwardTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        targetDate: String = LocalDate.now().toString(),
        tasks: List<Task>? = null
    ): CarryForwardResult {
        val rawTasks = tasks ?: taskRepository.getOverduePendingTasks(targetDate)
        val tasksToCarry = rawTasks.filter { it.state.equals("PENDING", ignoreCase = true) }
        if (tasksToCarry.isEmpty()) {
            return CarryForwardResult(emptyList(), 0)
        }

        val updatedTasks = tasksToCarry.map { task ->
            task.copy(
                scheduledDate = targetDate,
                isCarryForward = true,
                carryForwardCount = task.carryForwardCount + 1
            )
        }

        taskRepository.saveTasks(updatedTasks)
        return CarryForwardResult(updatedTasks, updatedTasks.size)
    }
}
