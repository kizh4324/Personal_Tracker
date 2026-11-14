package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionState
import com.personaltracker.domain.engine.CompanionStateEngine
import com.personaltracker.domain.engine.CompanionTriggerEvent
import com.personaltracker.domain.repository.DayTypeRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pure Kotlin use case evaluating current user context (active timer, DayType, streak milestone)
 * into a deterministic non-punishing CompanionState (AD-1, AD-4).
 */
class ResolveCompanionMoodUseCase @Inject constructor(
    private val companionStateEngine: CompanionStateEngine,
    private val dayTypeRepository: DayTypeRepository
) {
    suspend operator fun invoke(
        isStudySessionActive: Boolean = false,
        isStreakAtRisk: Boolean = false,
        recentTrigger: CompanionTriggerEvent = CompanionTriggerEvent.Idle,
        date: String = LocalDate.now().toString()
    ): CompanionState {
        val override = dayTypeRepository.getOverrideForDate(date)
        val dayTypeId = override?.dayTypeId
        return companionStateEngine.resolveMood(
            dayTypeId = dayTypeId,
            isStudySessionActive = isStudySessionActive,
            isStreakAtRisk = isStreakAtRisk,
            recentTrigger = recentTrigger
        )
    }

    operator fun invoke(
        dayTypeId: String?,
        isStudySessionActive: Boolean,
        isStreakAtRisk: Boolean = false,
        recentTrigger: CompanionTriggerEvent = CompanionTriggerEvent.Idle
    ): CompanionState {
        return companionStateEngine.resolveMood(
            dayTypeId = dayTypeId,
            isStudySessionActive = isStudySessionActive,
            isStreakAtRisk = isStreakAtRisk,
            recentTrigger = recentTrigger
        )
    }
}
