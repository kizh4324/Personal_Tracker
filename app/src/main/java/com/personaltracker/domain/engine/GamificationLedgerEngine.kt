package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DeliveryIntensity
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.StreakTier
import com.personaltracker.domain.model.Task
import java.time.LocalDate
import javax.inject.Inject

/**
 * Valid companion pet mood states (AD-4, EXPERIENCE.md §4.9).
 * Strictly non-punishing: zero negative, sad, or punitive states.
 */
@Deprecated("Use com.personaltracker.domain.engine.CompanionState instead")
enum class CompanionMood(val ordinalValue: Int) {
    IDLE_CONTENT(0),
    CELEBRATING(1),
    ENCOURAGING(2),
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
 * daily earn caps, companion XP progression, and positive mood state transitions
 * (FR-8.1, FR-8.2, AD-1, AD-4, AD-6).
 *
 * Contains zero Android framework dependencies.
 */
class GamificationLedgerEngine @Inject constructor() {

    companion object {
        const val ENERGY_PER_LEVEL = 100
        const val MIN_STUDY_MINUTES_FOR_COINS = 10

        const val REWARD_TASK_URGENT = 10
        const val REWARD_TASK_IMPORTANT = 7
        const val REWARD_TASK_ROUTINE = 5

        const val REWARD_HABIT_BASE = 5
        const val REWARD_ROUTINE_BASE = 15
        const val REWARD_ROUTINE_STEP = 2

        const val DAILY_TASK_CAP = 50
        const val DAILY_HABIT_CAP = 40
        const val DAILY_FOCUS_CAP = 120
        const val DAILY_GLOBAL_FARMABLE_CAP = 100

        val FARMABLE_EVENT_TYPES: Set<String> = setOf(
            EconomyEventTypes.TASK_COMPLETION,
            EconomyEventTypes.HABIT_COMPLETION,
            EconomyEventTypes.ROUTINE_COMPLETION
        )

        /**
         * Calculates coin payout for task completion based on delivery intensity.
         */
        fun calculateTaskReward(intensity: DeliveryIntensity): Int = when (intensity) {
            DeliveryIntensity.ROUTINE -> REWARD_TASK_ROUTINE
            DeliveryIntensity.IMPORTANT -> REWARD_TASK_IMPORTANT
            DeliveryIntensity.URGENT -> REWARD_TASK_URGENT
        }

        fun calculateTaskReward(task: Task): Int =
            calculateTaskReward(DeliveryIntensity.fromString(task.deliveryIntensity))

        fun calculateTaskReward(intensity: String): Int =
            calculateTaskReward(DeliveryIntensity.fromString(intensity))

        /**
         * Calculates coin payout for completing a habit based on streak tier and 7-day milestones.
         */
        fun calculateHabitReward(habit: Habit): Int {
            val base = REWARD_HABIT_BASE
            val streakBonus = when (habit.streakTier) {
                StreakTier.COLD -> 0
                StreakTier.WARMING -> 1
                StreakTier.BURNING -> 2
                StreakTier.BLAZING -> 3
            }
            val milestoneBonus = if (habit.currentStreak > 0 && habit.currentStreak % 7 == 0) 10 else 0
            return base + streakBonus + milestoneBonus
        }

        /**
         * Calculates coin payout for a study session.
         * Sessions < 10 mins: 0 coins.
         * Sessions >= 10 mins: (durationMinutes / 6).coerceIn(5, 15).
         */
        fun calculateStudyReward(durationMinutes: Long): Int {
            if (durationMinutes < MIN_STUDY_MINUTES_FOR_COINS) return 0
            return (durationMinutes / 6).toInt().coerceIn(5, 15)
        }

        fun calculateStudySessionReward(durationMinutes: Int): Int =
            calculateStudyReward(durationMinutes.toLong())

        /**
         * Calculates coin payout for advancing an individual routine step.
         */
        fun calculateRoutineStepReward(): Int = REWARD_ROUTINE_STEP

        /**
         * Calculates coin payout for completing an entire sequential routine.
         */
        fun calculateRoutineReward(routine: Routine): Int = REWARD_ROUTINE_BASE

        /**
         * Two-tier cap clamping logic (AD-4, AD-6).
         * Clamps requested reward against category cap and optional global farmable cap.
         */
        fun calculateCappedReward(
            requestedAmount: Int,
            categoryEarned: Int,
            categoryCap: Int,
            globalFarmableEarned: Int = 0,
            globalFarmableCap: Int = Int.MAX_VALUE,
            isFarmableCategory: Boolean = false
        ): Int {
            if (requestedAmount <= 0) return 0
            val categoryRemaining = maxOf(0, categoryCap - categoryEarned)
            val afterCategory = minOf(requestedAmount, categoryRemaining)
            return if (isFarmableCategory) {
                val globalRemaining = maxOf(0, globalFarmableCap - globalFarmableEarned)
                minOf(afterCategory, globalRemaining)
            } else {
                afterCategory
            }
        }

        // Idempotency Key Generators
        fun generateTaskCompletionKey(taskId: String): String {
            require(taskId.isNotBlank() && !taskId.any(Char::isWhitespace)) {
                "taskId must be non-blank without whitespace: $taskId"
            }
            return "TASK_COMP_$taskId"
        }

        fun generateTaskIdempotencyKey(taskId: String): String =
            generateTaskCompletionKey(taskId)

        fun generateHabitLogKey(habitId: String, date: LocalDate): String =
            generateHabitLogKey(habitId, date.toString())

        fun generateHabitLogKey(habitId: String, date: String): String {
            require(habitId.isNotBlank() && !habitId.any(Char::isWhitespace)) {
                "habitId must be non-blank without whitespace: $habitId"
            }
            require(date.isNotBlank() && !date.any(Char::isWhitespace)) {
                "date must be non-blank without whitespace: $date"
            }
            return "HABIT_LOG_${habitId}_$date"
        }

        fun generateHabitIdempotencyKey(habitId: String, logDate: String): String =
            generateHabitLogKey(habitId, logDate)

        fun generateStudySessionKey(sessionId: String): String {
            require(sessionId.isNotBlank() && !sessionId.any(Char::isWhitespace)) {
                "sessionId must be non-blank without whitespace: $sessionId"
            }
            return "STUDY_SESSION_$sessionId"
        }

        fun generateStudyIdempotencyKey(sessionId: String): String =
            generateStudySessionKey(sessionId)

        fun generateRoutineStepKey(routineId: String, date: LocalDate): String =
            generateRoutineStepKey(routineId, date.toString())

        fun generateRoutineStepKey(routineId: String, date: String): String {
            require(routineId.isNotBlank() && !routineId.any(Char::isWhitespace)) {
                "routineId must be non-blank without whitespace: $routineId"
            }
            require(date.isNotBlank() && !date.any(Char::isWhitespace)) {
                "date must be non-blank without whitespace: $date"
            }
            return "ROUTINE_COMP_${routineId}_$date"
        }

        fun generateRoutineIdempotencyKey(routineId: String, date: String): String =
            generateRoutineStepKey(routineId, date)

        fun generateCaptureUndoKey(inboxId: String): String {
            require(inboxId.isNotBlank() && !inboxId.any(Char::isWhitespace)) {
                "inboxId must be non-blank without whitespace: $inboxId"
            }
            return "CAPTURE_UNDO_$inboxId"
        }
    }

    // Instance delegation for backward compatibility
    fun calculateTaskReward(intensity: DeliveryIntensity): Int = Companion.calculateTaskReward(intensity)
    fun calculateTaskReward(task: Task): Int = Companion.calculateTaskReward(task)
    fun calculateTaskReward(intensity: String): Int = Companion.calculateTaskReward(intensity)
    fun calculateHabitReward(habit: Habit): Int = Companion.calculateHabitReward(habit)
    fun calculateStudyReward(durationMinutes: Long): Int = Companion.calculateStudyReward(durationMinutes)
    fun calculateStudySessionReward(durationMinutes: Int): Int = Companion.calculateStudySessionReward(durationMinutes)
    fun calculateRoutineStepReward(): Int = Companion.calculateRoutineStepReward()
    fun calculateRoutineReward(routine: Routine): Int = Companion.calculateRoutineReward(routine)
    fun calculateCappedReward(
        requestedAmount: Int,
        categoryEarned: Int,
        categoryCap: Int,
        globalFarmableEarned: Int = 0,
        globalFarmableCap: Int = Int.MAX_VALUE,
        isFarmableCategory: Boolean = false
    ): Int = Companion.calculateCappedReward(
        requestedAmount, categoryEarned, categoryCap, globalFarmableEarned, globalFarmableCap, isFarmableCategory
    )

    fun generateTaskCompletionKey(taskId: String): String = Companion.generateTaskCompletionKey(taskId)
    fun generateTaskIdempotencyKey(taskId: String): String = Companion.generateTaskIdempotencyKey(taskId)
    fun generateHabitLogKey(habitId: String, date: LocalDate): String = Companion.generateHabitLogKey(habitId, date)
    fun generateHabitLogKey(habitId: String, date: String): String = Companion.generateHabitLogKey(habitId, date)
    fun generateHabitIdempotencyKey(habitId: String, logDate: String): String = Companion.generateHabitIdempotencyKey(habitId, logDate)
    fun generateStudySessionKey(sessionId: String): String = Companion.generateStudySessionKey(sessionId)
    fun generateStudyIdempotencyKey(sessionId: String): String = Companion.generateStudyIdempotencyKey(sessionId)
    fun generateRoutineStepKey(routineId: String, date: LocalDate): String = Companion.generateRoutineStepKey(routineId, date)
    fun generateRoutineStepKey(routineId: String, date: String): String = Companion.generateRoutineStepKey(routineId, date)
    fun generateRoutineIdempotencyKey(routineId: String, date: String): String = Companion.generateRoutineIdempotencyKey(routineId, date)
    fun generateCaptureUndoKey(inboxId: String): String = Companion.generateCaptureUndoKey(inboxId)

    /**
     * Advances companion XP and level without negative energy or HP degradation (AD-4).
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
