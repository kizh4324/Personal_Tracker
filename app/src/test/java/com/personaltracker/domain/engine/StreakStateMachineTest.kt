package com.personaltracker.domain.engine

import com.personaltracker.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class StreakStateMachineTest {

    private lateinit var stateMachine: StreakStateMachine

    @Before
    fun setUp() {
        stateMachine = StreakStateMachine()
    }

    // ==========================================
    // 1. 4-State Flame Transition Tests
    // ==========================================

    @Test
    fun testFlameTransitionsAcrossThresholds() {
        // COLD: 0 days
        assertEquals(FlameState.COLD, stateMachine.getFlameState(0))

        // WARMING: 1..2 days
        assertEquals(FlameState.WARMING, stateMachine.getFlameState(1))
        assertEquals(FlameState.WARMING, stateMachine.getFlameState(2))

        // BURNING: 3..6 days
        assertEquals(FlameState.BURNING, stateMachine.getFlameState(3))
        assertEquals(FlameState.BURNING, stateMachine.getFlameState(5))
        assertEquals(FlameState.BURNING, stateMachine.getFlameState(6))

        // BLAZING: 7+ days
        assertEquals(FlameState.BLAZING, stateMachine.getFlameState(7))
        assertEquals(FlameState.BLAZING, stateMachine.getFlameState(14))
        assertEquals(FlameState.BLAZING, stateMachine.getFlameState(100))
    }

    @Test
    fun testFlameTransitionRejectsNegativeStreak() {
        try {
            stateMachine.getFlameState(-1)
            fail("Expected IllegalArgumentException for negative streak")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-negative") == true)
        }
    }

    // ==========================================
    // 2. Non-Punitive Slack Banking Tests
    // ==========================================

    @Test
    fun testStreakMissWithSlackBankPreservesStreakAndDecrementsSlack() {
        val habit = Habit(
            id = "h1",
            name = "Morning Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 10,
            slackBankDays = 2,
            isFreezeActive = false
        )

        val result = stateMachine.evaluateMissedDay(habit)

        // Streak is preserved at 10, slack decremented to 1
        assertEquals(10, result.updatedHabit.currentStreak)
        assertEquals(1, result.updatedHabit.slackBankDays)
        assertEquals(10, result.updatedHabit.bestStreak)
        assertEquals("SLACK_USED", result.resultingStatus)
        assertEquals(FlameState.BLAZING, result.flameState)
    }

    // ==========================================
    // 3. Freeze Grace Rules Tests
    // ==========================================

    @Test
    fun testStreakMissWithFreezeActivePreservesStreakAndConsumesFreeze() {
        val habit = Habit(
            id = "h2",
            name = "Drink 2L Water",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 12,
            slackBankDays = 0,
            isFreezeActive = true
        )

        val result = stateMachine.evaluateMissedDay(habit)

        // Streak is preserved at 10, freeze consumed
        assertEquals(10, result.updatedHabit.currentStreak)
        assertFalse(result.updatedHabit.isFreezeActive)
        assertEquals(12, result.updatedHabit.bestStreak)
        assertEquals("FROZEN", result.resultingStatus)
        assertEquals(FlameState.BLAZING, result.flameState)
    }

    @Test
    fun testFreezeTakesPriorityOverSlackBank() {
        val habit = Habit(
            id = "h3",
            name = "Read 20 Pages",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 5,
            slackBankDays = 2,
            isFreezeActive = true
        )

        val result = stateMachine.evaluateMissedDay(habit)

        // Freeze consumed, slack bank untouched at 2
        assertEquals(5, result.updatedHabit.currentStreak)
        assertFalse(result.updatedHabit.isFreezeActive)
        assertEquals(2, result.updatedHabit.slackBankDays)
        assertEquals("FROZEN", result.resultingStatus)
    }

    // ==========================================
    // 4. Unprotected Miss Tests
    // ==========================================

    @Test
    fun testUnprotectedMissResetsStreakToZeroWhilePreservingBestStreak() {
        val habit = Habit(
            id = "h4",
            name = "Exercise",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 14,
            bestStreak = 20,
            slackBankDays = 0,
            isFreezeActive = false
        )

        val result = stateMachine.evaluateMissedDay(habit)

        // Streak resets to 0, bestStreak preserved at 20
        assertEquals(0, result.updatedHabit.currentStreak)
        assertEquals(20, result.updatedHabit.bestStreak)
        assertEquals("MISSED", result.resultingStatus)
        assertEquals(FlameState.COLD, result.flameState)
    }

    // ==========================================
    // 5. Completion Progression Tests
    // ==========================================

    @Test
    fun testEvaluateCompletionAdvancesStreakAndBestStreak() {
        val habit = Habit(
            id = "h5",
            name = "Coding Practice",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 6,
            bestStreak = 6,
            slackBankDays = 0,
            isFreezeActive = false
        )

        val result = stateMachine.evaluateCompletion(habit)

        // Advances from 6 to 7 days, reaches BLAZING, bestStreak updated to 7
        // At 7 days milestone, awards 1 slack bank day
        assertEquals(7, result.updatedHabit.currentStreak)
        assertEquals(7, result.updatedHabit.bestStreak)
        assertEquals(1, result.updatedHabit.slackBankDays)
        assertEquals("COMPLETED", result.resultingStatus)
        assertEquals(FlameState.BLAZING, result.flameState)
    }

    @Test
    fun testCompletionFromOneToTwoDoesNotAwardBonusSlackDay() {
        val habit = Habit(
            id = "h6",
            name = "Reading",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 1,
            bestStreak = 1,
            slackBankDays = 0,
            isFreezeActive = false
        )

        val result = stateMachine.evaluateCompletion(habit)

        // Streak advances to 2, but NO bonus slack bank day is awarded (only every 7 days)
        assertEquals(2, result.updatedHabit.currentStreak)
        assertEquals(2, result.updatedHabit.bestStreak)
        assertEquals(0, result.updatedHabit.slackBankDays)
        assertEquals("COMPLETED", result.resultingStatus)
        assertEquals(FlameState.WARMING, result.flameState)
    }

    @Test
    fun testEvaluateDayOnNonTargetRestDayPreservesStreakWithoutConsumingSlack() {
        // Weekdays habit (Monday=1 to Friday=5)
        val habit = Habit(
            id = "h7",
            name = "Work Focus",
            targetDaysOfWeek = "1,2,3,4,5",
            currentStreak = 5,
            bestStreak = 5,
            slackBankDays = 2,
            isFreezeActive = false
        )

        // 2026-10-17 is Saturday (day 6, not a target day)
        val result = stateMachine.evaluateDay(habit, isCompleted = false, logDate = "2026-10-17")

        assertEquals(5, result.updatedHabit.currentStreak)
        assertEquals(2, result.updatedHabit.slackBankDays)
        assertEquals("REST_DAY", result.resultingStatus)
        assertEquals(FlameState.BURNING, result.flameState)
    }

    // ==========================================
    // 6. Zero Streak & Multi-Day Gap Tests
    // ==========================================

    @Test
    fun testZeroStreakMissPreservesFreezeAndSlackBankWithoutWastingProtection() {
        val habit = Habit(
            id = "h8",
            name = "Morning Stretch",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 0,
            bestStreak = 10,
            slackBankDays = 2,
            isFreezeActive = true
        )

        val result = stateMachine.evaluateMissedDay(habit)

        // When streak is 0, freeze and slack bank must NOT be burned
        assertEquals(0, result.updatedHabit.currentStreak)
        assertTrue(result.updatedHabit.isFreezeActive)
        assertEquals(2, result.updatedHabit.slackBankDays)
        assertEquals(10, result.updatedHabit.bestStreak)
        assertEquals("MISSED", result.resultingStatus)
        assertEquals(FlameState.COLD, result.flameState)
    }

    @Test
    fun testMultiDayGapAbsorbedByFreezeAndSlackBank() {
        val habit = Habit(
            id = "h9",
            name = "Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 10,
            slackBankDays = 2,
            isFreezeActive = true
        )

        // Miss 3 consecutive days: Day 1 consumes freeze, Days 2 & 3 consume 2 slack days
        val result = stateMachine.evaluateMissedDays(habit, 3)

        assertEquals(10, result.updatedHabit.currentStreak)
        assertFalse(result.updatedHabit.isFreezeActive)
        assertEquals(0, result.updatedHabit.slackBankDays)
        assertEquals(10, result.updatedHabit.bestStreak)
        assertEquals("SLACK_USED", result.resultingStatus)
        assertEquals(FlameState.BLAZING, result.flameState)
    }

    @Test
    fun testMultiDayGapExceedingProtectionResetsStreak() {
        val habit = Habit(
            id = "h10",
            name = "Reading",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 15,
            slackBankDays = 2,
            isFreezeActive = true
        )

        // Miss 4 consecutive days: 1 freeze + 2 slack = 3 protected; 4th day unprotected miss
        val result = stateMachine.evaluateMissedDays(habit, 4)

        assertEquals(0, result.updatedHabit.currentStreak)
        assertFalse(result.updatedHabit.isFreezeActive)
        assertEquals(0, result.updatedHabit.slackBankDays)
        assertEquals(15, result.updatedHabit.bestStreak)
        assertEquals("MISSED", result.resultingStatus)
        assertEquals(FlameState.COLD, result.flameState)
    }

    @Test
    fun testMultiDayGapZeroDaysReturnsNoChange() {
        val habit = Habit(
            id = "h11",
            name = "Water",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 5,
            slackBankDays = 1,
            isFreezeActive = true
        )

        val result = stateMachine.evaluateMissedDays(habit, 0)

        assertEquals(5, result.updatedHabit.currentStreak)
        assertEquals("NO_CHANGE", result.resultingStatus)
    }
}
