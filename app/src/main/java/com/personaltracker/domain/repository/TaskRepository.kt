package com.personaltracker.domain.repository

import com.personaltracker.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for Task management and atomic completions (FR-2.2, FR-5.1, AD-1, AD-6).
 */
interface TaskRepository {
    fun getTasksForDate(date: String): Flow<List<Task>>
    suspend fun getTaskById(id: String): Task?
    suspend fun getOverduePendingTasks(currentDate: String): List<Task>
    suspend fun getActiveInProgressTask(): Task?
    suspend fun getLatestInterruptedTask(): Task?
    suspend fun saveTask(task: Task)
    suspend fun saveTasks(tasks: List<Task>)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun completeTaskWithReward(
        taskId: String,
        completionTimestamp: Long = System.currentTimeMillis(),
        coinReward: Int = 10,
        idempotencyKey: String
    )
}
