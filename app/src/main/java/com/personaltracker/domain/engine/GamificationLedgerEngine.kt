package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.Task

/**
 * Valid companion pet mood states (AD-4, EXPERIENCE.md §4.9).
 * Strictly non-punishing: zero negative, sad, or punitive states.
 */
enum class CompanionMood(val ordinalValue: Int) {
    IDLE_CONTENT(0),
    ENCOURAGING(1),
    CELEBRATING(2),
    CONCERNED(3),
    FOCUSED(4),
    SLEEPING(5),
    EXCITED(6)
}

/**
 * High-level domain triggers driving companion emotional and leveling responses.
 */
sealed interface CompanionTriggerEvent {
    object TaskCompleted : CompanionTriggerEvent
    data class HabitCompleted(val streak: Int) : CompanionTriggerEvent
    object StreakAtRisk : CompanionTriggerEvent
    object FocusSessionStarted : CompanionTriggerEvent
    object FocusSessionCompleted : CompanionTriggerEvent
    object EveningRest : CompanionTriggerEvent
    object MilestoneReached : CompanionTriggerEvent
    object FreezeUsed : CompanionTriggerEvent
    object ReturnAfterAbsence : CompanionTriggerEvent
    object Idle : CompanionTriggerEvent
}

/**
 * Pure Kotlin behavioral engine managing coin reward math, deterministic idempotency keys,
 * companion XP progression, and positive mood state transitions (FR-8.1, FR-8.2, AD-1, AD-4, AD-6).
 *
 * Contains zero Android framework dependencies.
 */
class GamificationLedgerEngine {

    companion object {
        const val ENERGY_PER_LEVEL = 100
        const val MIN_STUDY_MINUTES_FOR_COINS = 10

        const val REWARD_TASK_URGENT = 10
        const val REWARD_TASK_IMPORTANT = 7
        const val REWARD_TASK_ROUTINE = 5

        const val REWARD_HABIT_BASE = 5
        const val REWARD_ROUTINE_BASE = 15
    }

    /**
     * Calculates the coin payout for completing a task based on delivery intensity.
     */
    fun calculateTaskReward(task: Task): Int {
        return when (task.deliveryIntensity.uppercase()) {
            "URGENT" -> REWARD_TASK_URGENT
            "IMPORTANT" -> REWARD_TASK_IMPORTANT
            else -> REWARD_TASK_ROUTINE
        }
    }

    /**
     * Calculates the coin payout for completing a habit.
     */
    fun calculateHabitReward(habit: Habit): Int {
        return REWARD_HABIT_BASE
    }

    /**
     * Calculates the coin payout for a study session.
     * Sessions shorter than 10 minutes receive 0 coins (EXPERIENCE.md §4.5).
     * Sessions >= 10 minutes earn 10 coins per 60 minutes (minimum 5 coins).
     */
    fun calculateStudySessionReward(durationMinutes: Int): Int {
        if (durationMinutes < MIN_STUDY_MINUTES_FOR_COINS) return 0
        return (durationMinutes / 6).coerceAtLeast(5)
    }

    /**
     * Calculates the coin payout for completing a sequential routine.
     */
    fun calculateRoutineReward(routine: Routine): Int {
        return REWARD_ROUTINE_BASE
    }

    /**
     * Generates a deterministic compound idempotency key for task completion: TASK_COMP_{id}.
     */
    fun generateTaskIdempotencyKey(taskId: String): String {
        require(taskId.isNotBlank() && !taskId.any(Char::isWhitespace)) {
            "taskId must be non-blank without whitespace: $taskId"
        }
        return "TASK_COMP_$taskId"
    }

    /**
     * Generates a deterministic compound idempotency key for habit logging: HABIT_LOG_{id}_{date}.
     */
    fun generateHabitIdempotencyKey(habitId: String, logDate: String): String {
        require(habitId.isNotBlank() && !habitId.any(Char::isWhitespace)) {
            "habitId must be non-blank without whitespace: $habitId"
        }
        require(logDate.isNotBlank() && !logDate.any(Char::isWhitespace)) {
            "logDate must be non-blank without whitespace: $logDate"
        }
        return "HABIT_LOG_${habitId}_$logDate"
    }

    /**
     * Generates a deterministic compound idempotency key for study sessions: STUDY_SESSION_{id}.
     */
    fun generateStudyIdempotencyKey(sessionId: String): String {
        require(sessionId.isNotBlank() && !sessionId.any(Char::isWhitespace)) {
            "sessionId must be non-blank without whitespace: $sessionId"
        }
        return "STUDY_SESSION_$sessionId"
    }

    /**
     * Generates a deterministic compound idempotency key for routine completion: ROUTINE_COMP_{id}_{date}.
     */
    fun generateRoutineIdempotencyKey(routineId: String, date: String): String {
        require(routineId.isNotBlank() && !routineId.any(Char::isWhitespace)) {
            "routineId must be non-blank without whitespace: $routineId"
        }
        require(date.isNotBlank() && !date.any(Char::isWhitespace)) {
            "date must be non-blank without whitespace: $date"
        }
        return "ROUTINE_COMP_${routineId}_$date"
    }

    /**
     * Advances companion XP and level without negative energy or HP degradation (AD-4).
     *
     * @param profile Existing companion profile
     * @param energyDelta Positive energy units earned
     * @return Updated companion profile
     */
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

        val newMood = if (levelsGained > 0) CompanionMood.EXCITED.ordinalValue else profile.currentExpressionOrdinal

        return profile.copy(
            level = newLevel,
            currentEnergyUnits = remainderEnergy,
            currentExpressionOrdinal = newMood
        )
    }

    /**
     * Resolves the non-punitive emotional expression of the companion.
     */
    fun resolveMoodForEvent(event: CompanionTriggerEvent): CompanionMood {
        return when (event) {
            is CompanionTriggerEvent.TaskCompleted -> CompanionMood.CELEBRATING
            is CompanionTriggerEvent.HabitCompleted -> {
                if (event.streak >= 7 && event.streak % 7 == 0) {
                    CompanionMood.EXCITED
                } else {
                    CompanionMood.CELEBRATING
                }
            }
            is CompanionTriggerEvent.StreakAtRisk -> CompanionMood.CONCERNED
            is CompanionTriggerEvent.FocusSessionStarted -> CompanionMood.FOCUSED
            is CompanionTriggerEvent.FocusSessionCompleted -> CompanionMood.CELEBRATING
            is CompanionTriggerEvent.EveningRest -> CompanionMood.SLEEPING
            is CompanionTriggerEvent.MilestoneReached -> CompanionMood.EXCITED
            is CompanionTriggerEvent.FreezeUsed -> CompanionMood.ENCOURAGING
            is CompanionTriggerEvent.ReturnAfterAbsence -> CompanionMood.ENCOURAGING
            is CompanionTriggerEvent.Idle -> CompanionMood.IDLE_CONTENT
        }
    }

    /**
     * Applies a mood transition to a companion profile.
     */
    fun applyMood(profile: CompanionProfile, mood: CompanionMood): CompanionProfile {
        return profile.copy(currentExpressionOrdinal = mood.ordinalValue)
    }
}
