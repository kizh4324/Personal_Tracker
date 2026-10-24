package com.personaltracker.domain.usecase

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Result data class for task completion.
 */
data class CompleteTaskResult(
    val taskId: String,
    val coinReward: Int,
    val idempotencyKey: String,
    val updatedCompanionProfile: CompanionProfile?
)

/**
 * Pure Kotlin use case executing task completion, generating deterministic coin transactions,
 * coordinating alarm cancellation, and rewarding the companion pet (FR-2.2, FR-5, FR-8.1, FR-8.2, AD-1, AD-4, AD-6).
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine,
    private val taskAlarmScheduler: TaskAlarmScheduler? = null
) {
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
            return CompleteTaskResult(
                taskId = taskId,
                coinReward = 0,
                idempotencyKey = idempotencyKey,
                updatedCompanionProfile = economyRepository.getCompanionProfile()
            )
        }

        val coinReward = ledgerEngine.calculateTaskReward(task)

        // Atomically complete task and commit coin ledger entry
        taskRepository.completeTaskWithReward(
            taskId = taskId,
            completionTimestamp = completionTimestamp,
            coinReward = coinReward,
            idempotencyKey = idempotencyKey
        )

        // Cleanly cancel pending reminder alarm if scheduled
        taskAlarmScheduler?.cancelTaskAlarm(taskId)

        // Award companion XP and celebrate
        val currentProfile = economyRepository.getCompanionProfile()
        val updatedProfile = if (currentProfile != null) {
            val withEnergy = ledgerEngine.addEnergy(currentProfile, coinReward)
            val mood = if (withEnergy.level > currentProfile.level) {
                CompanionMood.EXCITED
            } else {
                CompanionMood.CELEBRATING
            }
            val withMood = ledgerEngine.applyMood(withEnergy, mood)
            economyRepository.updateCompanionProfile(withMood)
            withMood
        } else null

        return CompleteTaskResult(
            taskId = taskId,
            coinReward = coinReward,
            idempotencyKey = idempotencyKey,
            updatedCompanionProfile = updatedProfile
        )
    }
}
