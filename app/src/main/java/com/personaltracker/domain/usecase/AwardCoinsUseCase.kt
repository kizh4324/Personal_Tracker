package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.AwardCoinsResult
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.repository.EconomyRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Pure Kotlin use case executing standalone coin awards with idempotency deduplication,
 * daily category earn caps, and companion XP progression (AD-1, AD-4, AD-6, FR-8.1, FR-8.2).
 */
class AwardCoinsUseCase @Inject constructor(
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine
) {
    suspend operator fun invoke(
        eventType: String,
        sourceEntityId: String,
        requestedAmount: Int,
        idempotencyKey: String,
        date: LocalDate = LocalDate.now()
    ): AwardCoinsResult {
        require(eventType.isNotBlank()) { "eventType must not be blank" }
        require(sourceEntityId.isNotBlank()) { "sourceEntityId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(requestedAmount >= 0) { "requestedAmount must be non-negative: $requestedAmount" }

        val startOfDayTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = economyRepository.awardCoins(
            eventType = eventType,
            sourceEntityId = sourceEntityId,
            requestedAmount = requestedAmount,
            idempotencyKey = idempotencyKey,
            startOfDayTimestamp = startOfDayTimestamp
        )

        // If coins were awarded and not a duplicate, advance companion XP
        if (result.coinsAwarded > 0 && !result.isDuplicate) {
            val currentProfile = economyRepository.getCompanionProfile()
            if (currentProfile != null) {
                val withEnergy = ledgerEngine.addEnergy(currentProfile, result.coinsAwarded)
                val mood = if (withEnergy.level > currentProfile.level) {
                    CompanionMood.EXCITED
                } else {
                    CompanionMood.CELEBRATING
                }
                val withMood = ledgerEngine.applyMood(withEnergy, mood)
                economyRepository.updateCompanionProfile(withMood)
            }
        }

        return result
    }

    /**
     * Convenience method to award coins for completing a study focus session.
     */
    suspend fun awardStudySession(
        sessionId: String,
        durationMinutes: Long,
        date: LocalDate = LocalDate.now()
    ): AwardCoinsResult {
        val requestedCoins = ledgerEngine.calculateStudyReward(durationMinutes)
        val idempotencyKey = ledgerEngine.generateStudySessionKey(sessionId)
        return invoke(
            eventType = EconomyEventTypes.STUDY_SESSION,
            sourceEntityId = sessionId,
            requestedAmount = requestedCoins,
            idempotencyKey = idempotencyKey,
            date = date
        )
    }

    /**
     * Convenience method to award coins for completing a routine step.
     */
    suspend fun awardRoutineStep(
        routineId: String,
        date: LocalDate = LocalDate.now()
    ): AwardCoinsResult {
        val requestedCoins = ledgerEngine.calculateRoutineStepReward()
        val idempotencyKey = ledgerEngine.generateRoutineStepKey(routineId, date)
        return invoke(
            eventType = EconomyEventTypes.ROUTINE_COMPLETION,
            sourceEntityId = routineId,
            requestedAmount = requestedCoins,
            idempotencyKey = idempotencyKey,
            date = date
        )
    }
}
