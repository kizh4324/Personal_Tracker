package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personaltracker.data.local.db.entities.HabitEntity
import com.personaltracker.data.local.db.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Habits and Habit Daily Logs.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface HabitDao {

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND logDate = :logDate LIMIT 1")
    suspend fun getLogForDate(habitId: String, logDate: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY logDate DESC")
    fun getLogsForHabit(habitId: String): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity)
}
