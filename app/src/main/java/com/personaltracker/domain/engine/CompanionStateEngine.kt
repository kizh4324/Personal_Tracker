package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import javax.inject.Inject

/**
 * Pure Kotlin 7-state behavioral engine evaluating non-punishing companion mood transitions
 * from DayType, study timer state, streak events, and XP leveling (AD-1, AD-4).
 *
 * Sourced 1:1 from Story 4.4 specification. Zero Android framework dependencies.
 */
class CompanionStateEngine @Inject constructor() {

    companion object {
        const val ENERGY_PER_LEVEL = 100
    }

    fun resolveMood(
        dayTypeId: String?,
        isStudySessionActive: Boolean,
        isStreakAtRisk: Boolean = false,
        recentTrigger: CompanionTriggerEvent = CompanionTriggerEvent.Idle
    ): CompanionState {
        if (isStudySessionActive) return CompanionState.FOCUSING_STUDY
        if (dayTypeId == "dt_rest" || dayTypeId == "dt_low_capacity" ||
            dayTypeId == "LOW_CAPACITY" || dayTypeId == "REST_RECOVERY"
        ) {
            return CompanionState.RESTING_PEACEFUL
        }
        return when (recentTrigger) {
            is CompanionTriggerEvent.TaskCompleted -> CompanionState.CELEBRATING_REWARD
            is CompanionTriggerEvent.HabitCompleted -> {
                if (recentTrigger.streak >= 7 && recentTrigger.streak % 7 == 0) {
                    CompanionState.HAPPY_BOUNCE
                } else {
                    CompanionState.CELEBRATING_REWARD
                }
            }
            is CompanionTriggerEvent.MilestoneReached -> CompanionState.HAPPY_BOUNCE
            is CompanionTriggerEvent.StreakAtRisk -> CompanionState.CONCERNED_CHECKIN
            is CompanionTriggerEvent.FreezeUsed,
            is CompanionTriggerEvent.ReturnAfterAbsence -> CompanionState.ENCOURAGING_WARM
            is CompanionTriggerEvent.EveningRest -> CompanionState.RESTING_PEACEFUL
            is CompanionTriggerEvent.FocusSessionStarted -> CompanionState.FOCUSING_STUDY
            is CompanionTriggerEvent.FocusSessionCompleted -> CompanionState.CELEBRATING_REWARD
            is CompanionTriggerEvent.Idle -> {
                if (isStreakAtRisk) CompanionState.CONCERNED_CHECKIN else CompanionState.IDLE_COZY
            }
        }
    }

    fun addEnergy(profile: CompanionProfile, energyDelta: Int): CompanionProfile {
        require(energyDelta >= 0) { "Energy earned cannot be negative: $energyDelta" }
        if (energyDelta == 0) return profile

        val safeCurrentEnergy = maxOf(0, profile.currentEnergyUnits)
        val safeLevel = maxOf(1, profile.level)

        val totalEnergy = try {
            Math.addExact(safeCurrentEnergy, energyDelta)
        } catch (_: ArithmeticException) {
            Int.MAX_VALUE
        }

        val levelsGained = totalEnergy / ENERGY_PER_LEVEL
        val newLevel = try {
            Math.addExact(safeLevel, levelsGained)
        } catch (_: ArithmeticException) {
            Int.MAX_VALUE
        }
        val remainderEnergy = totalEnergy % ENERGY_PER_LEVEL

        val newMood = if (levelsGained > 0) {
            CompanionState.HAPPY_BOUNCE.ordinalValue
        } else {
            profile.currentExpressionOrdinal
        }

        return profile.copy(
            level = newLevel,
            currentEnergyUnits = remainderEnergy,
            currentExpressionOrdinal = newMood
        )
    }
}
