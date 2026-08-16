package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personaltracker.data.local.db.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tasks.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface TaskDao {

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY createdTimestamp ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledDate < :currentDate AND state = 'PENDING'")
    suspend fun getOverduePendingTasks(currentDate: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE state = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveInProgressTask(): TaskEntity?

    @Query("SELECT * FROM tasks WHERE state = 'INTERRUPTED' ORDER BY createdTimestamp DESC LIMIT 1")
    suspend fun getLatestInterruptedTask(): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
