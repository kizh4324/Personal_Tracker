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
     * When current streak is 0, no active streak exists to protect, so freeze and slack days are preserved.
     *
     * Resolution order:
     * 1. Current streak is 0: streak remains 0, protections preserved, status "MISSED"
     * 2. Active freeze day: streak preserved, freeze consumed, status "FROZEN"
     * 3. Available slack bank: streak preserved, slackBankDays decremented by 1, status "SLACK_USED"
     * 4. No protection: streak resets to 0, status "MISSED"
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
                flameState = getFlameState(habit.currentStreak)
            )
        }

        val bestStreak = maxOf(habit.bestStreak, habit.currentStreak)

        if (habit.currentStreak == 0) {
            return StreakEvaluationResult(
                updatedHabit = habit.copy(bestStreak = bestStreak),
                resultingStatus = "MISSED",
                flameState = FlameState.COLD
            )
        }

        var remainingMissed = missedDays
        var freezeActive = habit.isFreezeActive
        var slackBank = habit.slackBankDays
        var streak = habit.currentStreak
        var status = "MISSED"

        // 1. Consume freeze if available
        if (remainingMissed > 0 && freezeActive) {
            freezeActive = false
            remainingMissed--
            status = "FROZEN"
        }

        // 2. Consume slack days if available
        if (remainingMissed > 0 && slackBank > 0) {
            val slackUsed = minOf(slackBank, remainingMissed)
            slackBank -= slackUsed
            remainingMissed -= slackUsed
            status = "SLACK_USED"
        }

        // 3. If missed days still remain, protections exhausted -> reset streak
        if (remainingMissed > 0) {
            streak = 0
            status = "MISSED"
        }

        val updatedHabit = habit.copy(
            currentStreak = streak,
            bestStreak = bestStreak,
            slackBankDays = slackBank,
            isFreezeActive = freezeActive
        )

        return StreakEvaluationResult(
            updatedHabit = updatedHabit,
            resultingStatus = status,
            flameState = getFlameState(streak)
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
