package com.personaltracker.domain.repository

import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for Habits and non-punitive daily logs (FR-2.4, AD-1, AD-4).
 */
interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: String): Habit?
    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>>
    suspend fun getLogForDate(habitId: String, logDate: String): HabitLog?
    suspend fun saveHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun logHabit(log: HabitLog)
    suspend fun logHabitWithReward(
        log: HabitLog,
        coinReward: Int = 5,
        idempotencyKey: String
    )
}
