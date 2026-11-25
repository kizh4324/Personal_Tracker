package com.personaltracker.domain.usecase

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.EscalationStateRepository
import com.personaltracker.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Result data class for task completion (AD-4, AD-6).
 */
data class CompleteTaskResult(
    val taskId: String,
    val coinReward: Int,
    val idempotencyKey: String,
    val updatedCompanionProfile: CompanionProfile?,
    val isCapReached: Boolean = false
)

/**
 * Pure Kotlin use case executing task completion, calculating daily capped coin rewards,
 * generating deterministic coin transactions, coordinating alarm cancellation,
 * and rewarding the companion pet (FR-2.2, FR-5, FR-8.1, FR-8.2, AD-1, AD-3, AD-4, AD-6).
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine,
    private val taskAlarmScheduler: TaskAlarmScheduler? = null,
    private val escalationStateRepository: EscalationStateRepository? = null
) {
    constructor(
        taskRepository: TaskRepository,
        economyRepository: EconomyRepository,
        ledgerEngine: GamificationLedgerEngine
    ) : this(taskRepository, economyRepository, ledgerEngine, null, null)

    constructor(
        taskRepository: TaskRepository,
        economyRepository: EconomyRepository,
        ledgerEngine: GamificationLedgerEngine,
        taskAlarmScheduler: TaskAlarmScheduler?
    ) : this(taskRepository, economyRepository, ledgerEngine, taskAlarmScheduler, null)
    suspend operator fun invoke(
        taskId: String,
        completionTimestamp: Long = System.currentTimeMillis()
    ): CompleteTaskResult {
        require(taskId.isNotBlank()) { "taskId must not be blank" }
        require(completionTimestamp > 0) { "completionTimestamp must be positive: $completionTimestamp" }

        val task = taskRepository.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task with id '$taskId' does not exist.")

        val idempotencyKey = ledgerEngine.generateTaskIdempotencyKey(taskId)

        if (task.state.equals("COMPLETED", ignoreCase = true)) {
            taskAlarmScheduler?.cancelTaskAlarm(taskId)
            escalationStateRepository?.clearAcknowledgement(taskId)
            return CompleteTaskResult(
                taskId = taskId,
                coinReward = 0,
                idempotencyKey = idempotencyKey,
                updatedCompanionProfile = economyRepository.getCompanionProfile(),
                isCapReached = false
            )
        }

        // 1. Calculate capped coin award based on today's category and global farmable limits
        val startOfDayTimestamp = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val taskEarned = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION)
        val habitEarned = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.HABIT_COMPLETION)
        val routineEarned = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.ROUTINE_COMPLETION)
        val globalFarmableEarned = taskEarned + habitEarned + routineEarned

        val requestedReward = ledgerEngine.calculateTaskReward(task)
        val cappedCoins = ledgerEngine.calculateCappedReward(
            requestedAmount = requestedReward,
            categoryEarned = taskEarned,
            categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
            globalFarmableEarned = globalFarmableEarned,
            globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
            isFarmableCategory = true
        )

        val isCapReached = (taskEarned + cappedCoins >= GamificationLedgerEngine.DAILY_TASK_CAP) ||
            (globalFarmableEarned + cappedCoins >= GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP) ||
            (requestedReward > 0 && cappedCoins < requestedReward)

        // 2. Atomically complete task and commit coin ledger entry
        try {
            taskRepository.completeTaskWithReward(
                taskId = taskId,
                completionTimestamp = completionTimestamp,
                coinReward = cappedCoins,
                idempotencyKey = idempotencyKey
            )
        } catch (e: Exception) {
            // Handle duplicate submission or already completed task gracefully without UI errors
            taskAlarmScheduler?.cancelTaskAlarm(taskId)
            escalationStateRepository?.clearAcknowledgement(taskId)
            return CompleteTaskResult(
                taskId = taskId,
                coinReward = 0,
                idempotencyKey = idempotencyKey,
                updatedCompanionProfile = economyRepository.getCompanionProfile(),
                isCapReached = isCapReached
            )
        }

        // Cleanly cancel pending reminder alarm if scheduled and prune acknowledgement state
        taskAlarmScheduler?.cancelTaskAlarm(taskId)
        escalationStateRepository?.clearAcknowledgement(taskId)

        // 3. Award companion XP and celebrate
        val currentProfile = economyRepository.getCompanionProfile()
        val updatedProfile = if (currentProfile != null && cappedCoins > 0) {
            val withEnergy = ledgerEngine.addEnergy(currentProfile, cappedCoins)
            val mood = if (withEnergy.level > currentProfile.level) {
                CompanionMood.EXCITED
            } else {
                CompanionMood.CELEBRATING
            }
            val withMood = ledgerEngine.applyMood(withEnergy, mood)
            economyRepository.updateCompanionProfile(withMood)
            withMood
        } else currentProfile

        return CompleteTaskResult(
            taskId = taskId,
            coinReward = cappedCoins,
            idempotencyKey = idempotencyKey,
            updatedCompanionProfile = updatedProfile,
            isCapReached = isCapReached
        )
    }
}
