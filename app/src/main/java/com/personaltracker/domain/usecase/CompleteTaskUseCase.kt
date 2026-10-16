package com.personaltracker.domain.usecase

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
 * and rewarding the companion pet (FR-2.2, FR-8.1, FR-8.2, AD-1, AD-4, AD-6).
 */
class CompleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine
) {
    suspend operator fun invoke(
        taskId: String,
        completionTimestamp: Long = System.currentTimeMillis()
    ): CompleteTaskResult {
        val task = taskRepository.getTaskById(taskId)
            ?: throw IllegalArgumentException("Task with id '$taskId' does not exist.")

        if (task.state.equals("COMPLETED", ignoreCase = true)) {
            return CompleteTaskResult(
                taskId = taskId,
                coinReward = 0,
                idempotencyKey = "",
                updatedCompanionProfile = null
            )
        }

        val coinReward = ledgerEngine.calculateTaskReward(task)
        val idempotencyKey = ledgerEngine.generateTaskIdempotencyKey(taskId)

        // Atomically complete task and commit coin ledger entry
        taskRepository.completeTaskWithReward(
            taskId = taskId,
            completionTimestamp = completionTimestamp,
            coinReward = coinReward,
            idempotencyKey = idempotencyKey
        )

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
