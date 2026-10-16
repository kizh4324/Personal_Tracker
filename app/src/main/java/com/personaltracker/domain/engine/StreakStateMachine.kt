package com.personaltracker.domain.engine

import com.personaltracker.domain.model.Habit

/**
 * 4-state flame model representing streak progression (AD-4).
 */
enum class FlameState {
    COLD,
    WARMING,
    BURNING,
    BLAZING
}

/**
 * Pure Kotlin state machine managing non-punitive habit streaks, slack banking,
 * freeze grace rules, and 4-state flame transitions (FR-2.4, FR-8.5, AD-1, AD-4).
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
        val resultingStatus: String, // "COMPLETED", "SLACK_USED", "FROZEN", "MISSED"
        val flameState: FlameState
    )

    /**
     * Determines the flame visualization state from a given streak count.
     *
     * Thresholds:
     * - COLD: 0 days
     * - WARMING: 1..2 days
     * - BURNING: 3..6 days
     * - BLAZING: 7+ days
     *
     * @param streak Non-negative integer streak count
     */
    fun getFlameState(streak: Int): FlameState {
        require(streak >= 0) { "Streak count must be non-negative: $streak" }
        return when (streak) {
            0 -> FlameState.COLD
            in 1..2 -> FlameState.WARMING
            in 3..6 -> FlameState.BURNING
            else -> FlameState.BLAZING
        }
    }

    /**
     * Evaluates a missed habit day using non-punitive slack-banking and freeze preservation.
     *
     * Resolution order:
     * 1. Active freeze day: streak preserved, freeze consumed, status "FROZEN"
     * 2. Available slack bank: streak preserved, slackBankDays decremented by 1, status "SLACK_USED"
     * 3. No protection: streak resets to 0, status "MISSED"
     *
     * In all cases, bestStreak is unconditionally preserved.
     */
    fun evaluateMissedDay(habit: Habit): StreakEvaluationResult {
        require(habit.currentStreak >= 0) { "Habit currentStreak must be non-negative" }
        require(habit.slackBankDays >= 0) { "Habit slackBankDays must be non-negative" }

        val bestStreak = maxOf(habit.bestStreak, habit.currentStreak)

        val (updatedHabit, status) = when {
            habit.isFreezeActive -> {
                // Freeze grace consumed, streak preserved
                habit.copy(
                    isFreezeActive = false,
                    bestStreak = bestStreak
                ) to "FROZEN"
            }
            habit.slackBankDays > 0 -> {
                // Slack bank used, streak preserved
                habit.copy(
                    slackBankDays = habit.slackBankDays - 1,
                    bestStreak = bestStreak
                ) to "SLACK_USED"
            }
            else -> {
                // Unprotected miss resets current streak to 0, preserves bestStreak
                habit.copy(
                    currentStreak = 0,
                    bestStreak = bestStreak
                ) to "MISSED"
            }
        }

        return StreakEvaluationResult(
            updatedHabit = updatedHabit,
            resultingStatus = status,
            flameState = getFlameState(updatedHabit.currentStreak)
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
            resultingStatus = "COMPLETED",
            flameState = getFlameState(newStreak)
        )
    }

    /**
     * Evaluates a habit log event (completed vs. missed).
     * If uncompleted on a non-target rest day, preserves streak as REST_DAY without consuming slack or freeze.
     *
     * @param habit Habit domain model
     * @param isCompleted True if user completed the habit
     * @param logDate Optional ISO date string ("YYYY-MM-DD") to evaluate against targetDaysOfWeek
     */
    fun evaluateDay(
        habit: Habit,
        isCompleted: Boolean,
        logDate: String? = null
    ): StreakEvaluationResult {
        if (isCompleted) {
            return evaluateCompletion(habit)
        }

        // Check if date corresponds to a scheduled target day of week
        if (logDate != null && habit.targetDaysOfWeek.isNotBlank()) {
            val targetDays = habit.targetDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (targetDays.isNotEmpty()) {
                val dayOfWeek = runCatching { java.time.LocalDate.parse(logDate).dayOfWeek.value }.getOrNull()
                if (dayOfWeek != null && !targetDays.contains(dayOfWeek)) {
                    // Today is a rest day according to habit cadence: preserve streak without consuming slack or freeze
                    return StreakEvaluationResult(
                        updatedHabit = habit,
                        resultingStatus = "REST_DAY",
                        flameState = getFlameState(habit.currentStreak)
                    )
                }
            }
        }

        return evaluateMissedDay(habit)
    }
}
