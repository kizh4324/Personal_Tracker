package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.HabitLogStatus
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Result data class for habit logging.
 */
data class LogHabitResult(
    val updatedHabit: Habit,
    val resultingStatus: String, // See [HabitLogStatus]
    val flameState: FlameState,
    val coinsEarned: Int,
    val idempotencyKey: String?,
    val updatedCompanionProfile: CompanionProfile?
)

/**
 * Pure Kotlin use case executing habit logging, evaluating non-punitive streaks,
 * upgrading prior rest/missed logs to completed, awarding coins idempotently,
 * and updating companion mood (FR-2.4, FR-8.1, FR-8.2, FR-8.5, ARCH-6, AD-1, AD-4).
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
        require(logDate.isNotBlank()) { "logDate must not be blank" }
        require(runCatching { LocalDate.parse(logDate) }.isSuccess) {
            "logDate must be a valid ISO-8601 date (YYYY-MM-DD): $logDate"
        }

        val habit = habitRepository.getHabitById(habitId)
            ?: throw IllegalArgumentException("Habit with id '$habitId' does not exist.")

        val existingLog = habitRepository.getLogForDate(habitId, logDate)

        // If already completed, idempotent return with 0 coins
        if (existingLog != null && existingLog.status.equals(HabitLogStatus.COMPLETED, ignoreCase = true)) {
            return LogHabitResult(
                updatedHabit = habit,
                resultingStatus = existingLog.status,
                flameState = streakStateMachine.getFlameState(habit.currentStreak, habit.isFreezeActive),
                coinsEarned = 0,
                idempotencyKey = null,
                updatedCompanionProfile = economyRepository.getCompanionProfile()
            )
        }

        // If uncompleting when already logged with non-completed status, return existing
        if (!isCompleted && existingLog != null) {
            return LogHabitResult(
                updatedHabit = habit,
                resultingStatus = existingLog.status,
                flameState = streakStateMachine.getFlameState(habit.currentStreak, habit.isFreezeActive),
                coinsEarned = 0,
                idempotencyKey = null,
                updatedCompanionProfile = economyRepository.getCompanionProfile()
            )
        }

        return if (isCompleted) {
            // Restore slack or freeze if previously absorbed today during an earlier miss
            val habitToEvaluate = when (existingLog?.status) {
                HabitLogStatus.SLACK_USED -> habit.copy(
                    slackBankDays = minOf(StreakStateMachine.MAX_SLACK_BANK_DAYS, habit.slackBankDays + 1)
                )
                HabitLogStatus.GRACE_SHIELD_ABSORBED -> habit.copy(isFreezeActive = true)
                else -> habit
            }

            val evaluation = streakStateMachine.evaluateCompletion(habitToEvaluate)
            val coinReward = ledgerEngine.calculateHabitReward(habit)
            val idempotencyKey = ledgerEngine.generateHabitIdempotencyKey(habitId, logDate)

            // Commit log with reward transaction and handle duplicate tap conflicts
            val log = HabitLog(
                id = existingLog?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                logDate = logDate,
                status = HabitLogStatus.COMPLETED
            )

            try {
                // Update habit state
                habitRepository.updateHabit(evaluation.updatedHabit)

                habitRepository.logHabitWithReward(
                    log = log,
                    coinReward = coinReward,
                    idempotencyKey = idempotencyKey
                )
            } catch (_: Exception) {
                // Duplicate completion / rapid tap: suppress error and return latest state
                val currentHabit = habitRepository.getHabitById(habitId) ?: evaluation.updatedHabit
                val currentLog = habitRepository.getLogForDate(habitId, logDate)
                return LogHabitResult(
                    updatedHabit = currentHabit,
                    resultingStatus = currentLog?.status ?: HabitLogStatus.COMPLETED,
                    flameState = streakStateMachine.getFlameState(currentHabit.currentStreak, currentHabit.isFreezeActive),
                    coinsEarned = 0,
                    idempotencyKey = null,
                    updatedCompanionProfile = economyRepository.getCompanionProfile()
                )
            }

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
                resultingStatus = HabitLogStatus.COMPLETED,
                flameState = evaluation.flameState,
                coinsEarned = coinReward,
                idempotencyKey = idempotencyKey,
                updatedCompanionProfile = updatedProfile
            )
        } else {
            val evaluation = streakStateMachine.evaluateDay(habit, isCompleted = false, logDate = logDate)

            // Update habit state with preserved or reset streak
            habitRepository.updateHabit(evaluation.updatedHabit)

            val log = HabitLog(
                id = existingLog?.id ?: UUID.randomUUID().toString(),
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
