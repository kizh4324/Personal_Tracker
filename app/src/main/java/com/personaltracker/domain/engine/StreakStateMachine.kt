package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CadenceType
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLogStatus
import java.time.LocalDate

/**
 * 5-state flame model representing streak progression and protection status (AD-4).
 */
enum class FlameState {
    COLD,
    WARMING,
    BURNING,
    BLAZING,
    GRACE_SHIELD
}

/**
 * Pure Kotlin state machine managing non-punitive habit streaks, cadence tracking,
 * slack banking, freeze grace rules, DayType immunity, and 5-state flame transitions
 * (FR-2.4, FR-8.5, AD-1, AD-4).
 *
 * Contains zero Android framework dependencies.
 */
class StreakStateMachine {

    companion object {
        const val MAX_SLACK_BANK_DAYS = 3
        const val SLACK_EARN_CADENCE_DAYS = 7
    }

    /**
     * Encapsulates the output of streak evaluation.
     */
    data class StreakEvaluationResult(
        val updatedHabit: Habit,
        val resultingStatus: String, // See [HabitLogStatus]
        val flameState: FlameState
    )

    /**
     * Determines the flame visualization state from a given streak count and grace shield status.
     *
     * Thresholds:
     * - GRACE_SHIELD: Active freeze day or day absorbed by grace shield/slack
     * - COLD: 0 days
     * - WARMING: 1..2 days
     * - BURNING: 3..6 days
     * - BLAZING: 7+ days
     *
     * @param streak Non-negative integer streak count
     * @param isGraceShieldActive True if active freeze or grace shield is protecting the streak
     */
    fun getFlameState(streak: Int, isGraceShieldActive: Boolean = false): FlameState {
        require(streak >= 0) { "Streak count must be non-negative: $streak" }
        if (isGraceShieldActive) {
            return FlameState.GRACE_SHIELD
        }
        return when (streak) {
            0 -> FlameState.COLD
            in 1..2 -> FlameState.WARMING
            in 3..6 -> FlameState.BURNING
            else -> FlameState.BLAZING
        }
    }

    /**
     * Evaluates a missed habit day using non-punitive slack-banking and freeze preservation.
     * When current streak is 0, no active streak exists to protect, so freeze and slack days are preserved.
     *
     * Resolution order:
     * 1. Current streak is 0: streak remains 0, protections preserved, status "MISSED", flame COLD
     * 2. Active freeze day: streak preserved, freeze consumed, status "GRACE_SHIELD_ABSORBED", flame GRACE_SHIELD
     * 3. Available slack bank: streak preserved, slackBankDays decremented by 1, status "SLACK_USED", flame GRACE_SHIELD
     * 4. No protection: streak resets to 0, status "MISSED", flame COLD
     *
     * In all cases, bestStreak is unconditionally preserved.
     */
    fun evaluateMissedDay(habit: Habit): StreakEvaluationResult {
        return evaluateMissedDays(habit, 1)
    }

    /**
     * Evaluates multiple consecutive missed days (e.g. across multi-day gap or absence).
     *
     * Order of protection consumption:
     * 1. If current streak is 0, streak remains 0 and zero freeze or slack is consumed.
     * 2. If isFreezeActive is true, 1 missed day is absorbed by freeze grace.
     * 3. Remaining missed days consume available slackBankDays 1-for-1.
     * 4. If remaining missed days exceed available slack days, protections are exhausted and streak resets to 0.
     * 5. If all missed days were absorbed, streak is preserved.
     *
     * @param habit Habit to evaluate
     * @param missedDays Total number of missed days in the gap
     * @return StreakEvaluationResult after processing the multi-day gap
     */
    fun evaluateMissedDays(habit: Habit, missedDays: Int): StreakEvaluationResult {
        require(missedDays >= 0) { "missedDays must be non-negative: $missedDays" }
        require(habit.currentStreak >= 0) { "Habit currentStreak must be non-negative" }
        require(habit.slackBankDays >= 0) { "Habit slackBankDays must be non-negative" }

        if (missedDays == 0) {
            return StreakEvaluationResult(
                updatedHabit = habit,
                resultingStatus = "NO_CHANGE",
                flameState = getFlameState(habit.currentStreak, habit.isFreezeActive)
            )
        }

        val bestStreak = maxOf(habit.bestStreak, habit.currentStreak)

        if (habit.currentStreak == 0) {
            return StreakEvaluationResult(
                updatedHabit = habit.copy(bestStreak = bestStreak),
                resultingStatus = HabitLogStatus.MISSED,
                flameState = FlameState.COLD
            )
        }

        var remainingMissed = missedDays
        var freezeActive = habit.isFreezeActive
        var slackBank = habit.slackBankDays
        var streak = habit.currentStreak
        var status = HabitLogStatus.MISSED

        // 1. Consume freeze if available
        if (remainingMissed > 0 && freezeActive) {
            freezeActive = false
            remainingMissed--
            status = HabitLogStatus.GRACE_SHIELD_ABSORBED
        }

        // 2. Consume slack days if available
        if (remainingMissed > 0 && slackBank > 0) {
            val slackUsed = minOf(slackBank, remainingMissed)
            slackBank -= slackUsed
            remainingMissed -= slackUsed
            status = HabitLogStatus.SLACK_USED
        }

        // 3. If missed days still remain, protections exhausted -> reset streak
        if (remainingMissed > 0) {
            streak = 0
            status = HabitLogStatus.MISSED
        }

        val updatedHabit = habit.copy(
            currentStreak = streak,
            bestStreak = bestStreak,
            slackBankDays = slackBank,
            isFreezeActive = freezeActive
        )

        val flame = when (status) {
            HabitLogStatus.GRACE_SHIELD_ABSORBED, HabitLogStatus.SLACK_USED -> FlameState.GRACE_SHIELD
            HabitLogStatus.MISSED -> FlameState.COLD
            else -> getFlameState(streak, freezeActive)
        }

        return StreakEvaluationResult(
            updatedHabit = updatedHabit,
            resultingStatus = status,
            flameState = flame
        )
    }

    /**
     * Evaluates a successful habit completion.
     * Increments currentStreak, updates bestStreak, and awards slack banking at cadence milestones.
     */
    fun evaluateCompletion(habit: Habit): StreakEvaluationResult {
        require(habit.currentStreak >= 0) { "Habit currentStreak must be non-negative" }
        require(habit.slackBankDays >= 0) { "Habit slackBankDays must be non-negative" }

        val newStreak = habit.currentStreak + 1
        val newBestStreak = maxOf(newStreak, habit.bestStreak)

        // Award a slack bank day every SLACK_EARN_CADENCE_DAYS up to MAX_SLACK_BANK_DAYS
        val bonusSlack = if (newStreak % SLACK_EARN_CADENCE_DAYS == 0 && habit.slackBankDays < MAX_SLACK_BANK_DAYS) 1 else 0
        val newSlackBank = (habit.slackBankDays + bonusSlack).coerceAtMost(MAX_SLACK_BANK_DAYS)

        val updatedHabit = habit.copy(
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            slackBankDays = newSlackBank
        )

        return StreakEvaluationResult(
            updatedHabit = updatedHabit,
            resultingStatus = HabitLogStatus.COMPLETED,
            flameState = getFlameState(newStreak, updatedHabit.isFreezeActive)
        )
    }

    /**
     * Evaluates a habit log event (completed vs. missed), incorporating DayType immunity,
     * cadence off-day immunity, flexible weekly frequencies, and non-punitive grace shields.
     *
     * @param habit Habit domain model
     * @param isCompleted True if user completed the habit
     * @param logDate Optional ISO date string ("YYYY-MM-DD") to evaluate against cadence schedules
     * @param activeDayType Optional active DayType for the date (LOW_CAPACITY / REST provide immunity)
     * @param weeklyCompletionsCount Optional completion count for the current week (for WeeklyFrequency evaluation on Sunday)
     */
    fun evaluateDay(
        habit: Habit,
        isCompleted: Boolean,
        logDate: String? = null,
        activeDayType: DayType? = null,
        weeklyCompletionsCount: Int? = null
    ): StreakEvaluationResult {
        if (isCompleted) {
            return evaluateCompletion(habit)
        }

        // 1. DayType Immunity Check: LOW_CAPACITY and REST days unconditionally protect streaks
        if (activeDayType != null) {
            val isImmuneDayType = activeDayType.id == DayType.LOW_CAPACITY.id ||
                activeDayType.id == DayType.REST.id ||
                activeDayType.name.equals(DayType.LOW_CAPACITY.name, ignoreCase = true) ||
                activeDayType.name.equals(DayType.REST.name, ignoreCase = true) ||
                activeDayType.name.contains("Rest", ignoreCase = true) ||
                activeDayType.name.contains("Low Capacity", ignoreCase = true)

            if (isImmuneDayType) {
                return StreakEvaluationResult(
                    updatedHabit = habit,
                    resultingStatus = HabitLogStatus.DAY_TYPE_IMMUNITY,
                    flameState = getFlameState(habit.currentStreak, habit.isFreezeActive)
                )
            }
        }

        // 2. Cadence & Schedule Immunity Check
        if (logDate != null) {
            val dayOfWeek = runCatching { LocalDate.parse(logDate).dayOfWeek.value }.getOrNull()

            if (dayOfWeek != null) {
                // Flexible WeeklyFrequency evaluation
                if (habit.cadenceType == CadenceType.WEEKLY_FREQUENCY) {
                    if (dayOfWeek != 7) {
                        // Mon-Sat: mid-week is flexible; uncompleted days are non-penalizing REST_DAY
                        return StreakEvaluationResult(
                            updatedHabit = habit,
                            resultingStatus = HabitLogStatus.REST_DAY,
                            flameState = getFlameState(habit.currentStreak, habit.isFreezeActive)
                        )
                    } else {
                        // Sunday week boundary check: if user already reached target frequency, week completed!
                        if (weeklyCompletionsCount != null && weeklyCompletionsCount >= habit.weeklyTargetFrequency) {
                            return StreakEvaluationResult(
                                updatedHabit = habit,
                                resultingStatus = HabitLogStatus.COMPLETED,
                                flameState = getFlameState(habit.currentStreak, habit.isFreezeActive)
                            )
                        }
                    }
                }

                // Specific days / custom weekdays cadence check
                val isSpecificDays = habit.cadenceType == CadenceType.SPECIFIC_DAYS ||
                    (habit.cadenceType == CadenceType.DAILY && habit.targetDaysOfWeek.isNotBlank() && habit.targetDaysOfWeek != "1,2,3,4,5,6,7")

                if (isSpecificDays) {
                    val targetDays = habit.targetDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (targetDays.isNotEmpty() && !targetDays.contains(dayOfWeek)) {
                        // Off-cadence rest day: preserve streak without consuming slack or freeze
                        return StreakEvaluationResult(
                            updatedHabit = habit,
                            resultingStatus = HabitLogStatus.REST_DAY,
                            flameState = getFlameState(habit.currentStreak, habit.isFreezeActive)
                        )
                    }
                }
            }
        }

        return evaluateMissedDay(habit)
    }
}
