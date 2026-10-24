package com.personaltracker.domain.usecase

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Pure Kotlin use case removing tasks and cancelling associated reminder alarms (AD-1, AD-3, FR-2.2, FR-5).
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Removes the task identified by [taskId] from persistence and cancels its reminder alarm.
     *
     * @return true if the task was found and deleted, false if the task did not exist (safe no-op).
     */
    suspend operator fun invoke(taskId: String): Boolean = withContext(ioDispatcher) {
        require(taskId.isNotBlank()) { "Task id must not be blank" }

        val task = taskRepository.getTaskById(taskId) ?: return@withContext false

        taskRepository.deleteTask(task)
        taskAlarmScheduler.cancelTaskAlarm(taskId)
        true
    }
}
