package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Result data class for guilt-free dismissal operation.
 */
data class DismissOverdueResult(
    val dismissedTasks: List<Task>,
    val dismissedCount: Int
)

/**
 * Pure Kotlin use case executing guilt-free dismissal of overdue tasks (FR-6, AD-1, AD-4).
 *
 * Safely marks overdue tasks as CANCELLED without coin penalties, streak resets, or pet energy loss.
 */
class DismissOverdueTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        targetDate: String = LocalDate.now().toString(),
        tasks: List<Task>? = null
    ): DismissOverdueResult {
        val rawTasks = tasks ?: taskRepository.getOverduePendingTasks(targetDate)
        val tasksToDismiss = rawTasks.filter { it.state.equals("PENDING", ignoreCase = true) }
        if (tasksToDismiss.isEmpty()) {
            return DismissOverdueResult(emptyList(), 0)
        }

        val dismissedTasks = tasksToDismiss.map { task ->
            task.copy(state = "CANCELLED")
        }

        taskRepository.saveTasks(dismissedTasks)
        return DismissOverdueResult(dismissedTasks, dismissedTasks.size)
    }
}
