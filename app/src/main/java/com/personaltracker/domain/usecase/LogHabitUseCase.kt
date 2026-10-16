package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Result data class for habit logging.
 */
data class LogHabitResult(
    val updatedHabit: Habit,
    val resultingStatus: String, // "COMPLETED", "SLACK_USED", "FROZEN", "MISSED"
    val flameState: FlameState,
    val coinsEarned: Int,
    val idempotencyKey: String?,
    val updatedCompanionProfile: CompanionProfile?
)

/**
 * Pure Kotlin use case executing habit logging, evaluating non-punitive streaks,
 * awarding coins, and updating companion mood (FR-2.4, FR-8.1, FR-8.2, FR-8.5, AD-1, AD-4).
 */
class LogHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val economyRepository: EconomyRepository,
    private val streakStateMachine: StreakStateMachine,
    private val ledgerEngine: GamificationLedgerEngine
) {
    suspend operator fun invoke(
        habitId: String,
        logDate: String,
        isCompleted: Boolean = true
    ): LogHabitResult {
        val habit = habitRepository.getHabitById(habitId)
            ?: throw IllegalArgumentException("Habit with id '$habitId' does not exist.")

        val existingLog = habitRepository.getLogForDate(habitId, logDate)
        if (existingLog != null) {
            return LogHabitResult(
                updatedHabit = habit,
                resultingStatus = existingLog.status,
                flameState = streakStateMachine.getFlameState(habit.currentStreak),
                coinsEarned = 0,
                idempotencyKey = null,
                updatedCompanionProfile = economyRepository.getCompanionProfile()
            )
        }

        val evaluation = streakStateMachine.evaluateDay(habit, isCompleted, logDate)

        return if (isCompleted) {
            val coinReward = ledgerEngine.calculateHabitReward(habit)
            val idempotencyKey = ledgerEngine.generateHabitIdempotencyKey(habitId, logDate)

            // Update habit state
            habitRepository.updateHabit(evaluation.updatedHabit)

            // Commit log with reward transaction
            val log = HabitLog(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                logDate = logDate,
                status = evaluation.resultingStatus
            )
            habitRepository.logHabitWithReward(
                log = log,
                coinReward = coinReward,
                idempotencyKey = idempotencyKey
            )

            // Update companion
            val currentProfile = economyRepository.getCompanionProfile()
            val updatedProfile = if (currentProfile != null) {
                val withEnergy = ledgerEngine.addEnergy(currentProfile, coinReward)
                val mood = when {
                    withEnergy.level > currentProfile.level -> CompanionMood.EXCITED
                    evaluation.updatedHabit.currentStreak >= 7 && evaluation.updatedHabit.currentStreak % 7 == 0 -> CompanionMood.EXCITED
                    else -> CompanionMood.CELEBRATING
                }
                val withMood = ledgerEngine.applyMood(withEnergy, mood)
                economyRepository.updateCompanionProfile(withMood)
                withMood
            } else null

            LogHabitResult(
                updatedHabit = evaluation.updatedHabit,
                resultingStatus = evaluation.resultingStatus,
                flameState = evaluation.flameState,
                coinsEarned = coinReward,
                idempotencyKey = idempotencyKey,
                updatedCompanionProfile = updatedProfile
            )
        } else {
            // Update habit state with preserved or reset streak
            habitRepository.updateHabit(evaluation.updatedHabit)

            val log = HabitLog(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                logDate = logDate,
                status = evaluation.resultingStatus
            )
            habitRepository.logHabit(log)

            // Non-punitive mood update: encouraging, never sad or dead
            val currentProfile = economyRepository.getCompanionProfile()
            val updatedProfile = if (currentProfile != null) {
                val mood = CompanionMood.ENCOURAGING
                val withMood = ledgerEngine.applyMood(currentProfile, mood)
                economyRepository.updateCompanionProfile(withMood)
                withMood
            } else null

            LogHabitResult(
                updatedHabit = evaluation.updatedHabit,
                resultingStatus = evaluation.resultingStatus,
                flameState = evaluation.flameState,
                coinsEarned = 0,
                idempotencyKey = null,
                updatedCompanionProfile = updatedProfile
            )
        }
    }
}
