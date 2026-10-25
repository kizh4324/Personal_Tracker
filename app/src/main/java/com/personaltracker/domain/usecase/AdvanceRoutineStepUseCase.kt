package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.RoutineRepository
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Action applied to a routine step during sequential progression (FR-2.3, AD-1, AD-4).
 */
enum class RoutineStepAction {
    COMPLETE,
    SKIP
}

/**
 * Result data class for sequential routine step advancement.
 */
data class AdvanceRoutineStepResult(
    val nextStep: RoutineStep?,
    val isRoutineCompleted: Boolean,
    val coinsAwarded: Int,
    val idempotencyKey: String?,
    val completedStepIds: Set<String>,
    val skippedStepIds: Set<String>,
    val message: String
)

/**
 * Pure Kotlin use case executing sequential routine step progression, non-punitive skipping,
 * cursor advancement, and 15-coin routine completion bonus idempotency (FR-2.3, FR-8.1, FR-8.2, AD-1, AD-3, AD-4, AD-6).
 *
 * Contains zero Android framework dependencies.
 */
class AdvanceRoutineStepUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine
) {

    suspend operator fun invoke(
        routineId: String,
        stepId: String,
        action: RoutineStepAction = RoutineStepAction.COMPLETE,
        date: String = LocalDate.now().toString(),
        completedStepIds: Set<String> = emptySet(),
        skippedStepIds: Set<String> = emptySet()
    ): AdvanceRoutineStepResult {
        require(routineId.isNotBlank()) { "routineId must not be blank" }
        require(stepId.isNotBlank()) { "stepId must not be blank" }

        val routine = routineRepository.getRoutineById(routineId)
            ?: Routine.BUILT_IN_ROUTINES.find { it.id == routineId }
            ?: throw IllegalArgumentException("Routine with id '$routineId' does not exist.")

        val steps = (if (routine.steps.isNotEmpty()) routine.steps else routineRepository.getStepsForRoutineList(routineId))
            .sortedBy { it.orderIndex }

        val currentStep = steps.find { it.id == stepId }
            ?: throw IllegalArgumentException("Step with id '$stepId' does not exist in routine '$routineId'.")

        val updatedCompleted = when (action) {
            RoutineStepAction.COMPLETE -> completedStepIds + currentStep.id
            RoutineStepAction.SKIP -> completedStepIds - currentStep.id
        }

        val updatedSkipped = when (action) {
            RoutineStepAction.COMPLETE -> skippedStepIds - currentStep.id
            RoutineStepAction.SKIP -> skippedStepIds + currentStep.id
        }

        val resolvedStepIds = updatedCompleted + updatedSkipped
        val nextStep = steps.firstOrNull { it.id !in resolvedStepIds }
        val isRoutineCompleted = nextStep == null

        var coinsAwarded = 0
        var idempotencyKey: String? = null
        var message: String

        if (isRoutineCompleted) {
            val key = ledgerEngine.generateRoutineIdempotencyKey(routine.id, date)
            idempotencyKey = key
            val existingTx = economyRepository.getTransactionByIdempotencyKey(key)

            if (existingTx == null) {
                val reward = ledgerEngine.calculateRoutineReward(routine)
                coinsAwarded = reward

                val currentBalance = economyRepository.getLatestBalance()
                val newBalance = currentBalance + reward
                val transaction = CoinTransaction(
                    id = UUID.randomUUID().toString(),
                    idempotencyKey = key,
                    deltaAmount = reward,
                    eventType = "ROUTINE_COMPLETION",
                    sourceEntityId = routine.id,
                    balanceAfter = newBalance,
                    timestamp = System.currentTimeMillis()
                )
                economyRepository.recordTransaction(transaction)

                val profile = economyRepository.getCompanionProfile()
                if (profile != null) {
                    val withEnergy = ledgerEngine.addEnergy(profile, reward)
                    val withMood = ledgerEngine.applyMood(withEnergy, CompanionMood.CELEBRATING)
                    economyRepository.updateCompanionProfile(withMood)
                }

                message = "Routine completed! Earned $reward coins."
            } else {
                coinsAwarded = 0
                message = "Routine completed for today."
            }
        } else {
            message = when (action) {
                RoutineStepAction.COMPLETE -> "Step '${currentStep.title}' completed. Next: ${nextStep.title}"
                RoutineStepAction.SKIP -> "Step '${currentStep.title}' skipped. Next: ${nextStep.title}"
            }
        }

        return AdvanceRoutineStepResult(
            nextStep = nextStep,
            isRoutineCompleted = isRoutineCompleted,
            coinsAwarded = coinsAwarded,
            idempotencyKey = idempotencyKey,
            completedStepIds = updatedCompleted,
            skippedStepIds = updatedSkipped,
            message = message
        )
    }
}
