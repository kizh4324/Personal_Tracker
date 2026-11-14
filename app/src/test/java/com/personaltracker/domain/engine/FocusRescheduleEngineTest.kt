package com.personaltracker.domain.engine

import com.personaltracker.domain.engine.FocusRescheduleEngine.InterruptionReason
import com.personaltracker.domain.model.FocusIntervalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusRescheduleEngineTest {

    private lateinit var engine: FocusRescheduleEngine

    @Before
    fun setUp() {
        engine = FocusRescheduleEngine()
    }

    // ==========================================
    // 1. Break Overrun Tests
    // ==========================================

    @Test
    fun testCalculateBreakOverrunShiftsEndTimeCalmly() {
        // Scenario from Spec:
        // Planned 5m break, overrun to 15m in 25m session (10m elapsed)
        // Original scheduled end time was 14:30
        val adjustment = engine.calculateBreakOverrun(
            plannedSessionDuration = 25,
            elapsedFocusMinutes = 10,
            plannedBreakMinutes = 5,
            actualBreakMinutes = 15,
            scheduledEndTime = "14:30"
        )

        assertEquals(5, adjustment.plannedBreakMinutes)
        assertEquals(15, adjustment.actualBreakMinutes)
        assertEquals(10, adjustment.overrunMinutes)
        assertEquals(15, adjustment.remainingFocusMinutes)
        assertEquals("14:30", adjustment.originalEndTime)
        assertEquals("14:40", adjustment.adjustedEndTime)

        // Verify non-punitive calm copy
        assertTrue(adjustment.advisoryMessage.contains("adjusted by 10 min"))
    }

    @Test
    fun testCalculateBreakWithoutOverrunKeepsEndTime() {
        val adjustment = engine.calculateBreakOverrun(
            plannedSessionDuration = 50,
            elapsedFocusMinutes = 25,
            plannedBreakMinutes = 10,
            actualBreakMinutes = 10,
            scheduledEndTime = "15:00"
        )

        assertEquals(0, adjustment.overrunMinutes)
        assertEquals(25, adjustment.remainingFocusMinutes)
        assertEquals("15:00", adjustment.adjustedEndTime)
    }

    // ==========================================
    // 2. Pause & Interruption Tests
    // ==========================================

    @Test
    fun testCalculatePauseAdjustmentShiftsEndTime() {
        val pauseAdjustment = engine.calculatePauseAdjustment(
            remainingMinutes = 20,
            pauseDurationMinutes = 15,
            currentScheduledEndTime = "16:00"
        )

        assertEquals(20, pauseAdjustment.remainingMinutes)
        assertEquals(15, pauseAdjustment.pauseDurationMinutes)
        assertEquals("16:00", pauseAdjustment.originalEndTime)
        assertEquals("16:15", pauseAdjustment.adjustedEndTime)
        assertTrue(pauseAdjustment.advisoryMessage.contains("16:15"))
    }

    @Test
    fun testCalculateInterruptionPreservesRemainingMinutesNonPunitively() {
        val summary = engine.calculateInterruption(
            plannedDurationMinutes = 60,
            elapsedMinutes = 35
        )

        assertEquals(60, summary.plannedDurationMinutes)
        assertEquals(35, summary.elapsedMinutes)
        assertEquals(25, summary.remainingMinutes)
        assertTrue(summary.advisoryMessage.contains("25 min remaining"))
        assertTrue(summary.advisoryMessage.contains("resume anytime"))
    }

    @Test
    fun testCalculateInterruptionSummaryWithDifferentReasons() {
        // PHONE_CALL
        val phoneSummary = engine.calculateInterruptionSummary(
            plannedDurationMinutes = 25,
            elapsedSeconds = 10 * 60L,
            reason = InterruptionReason.PHONE_CALL
        )
        assertEquals(10, phoneSummary.elapsedMinutes)
        assertEquals(15, phoneSummary.remainingMinutes)
        assertTrue(phoneSummary.advisoryMessage.contains("Call handled"))
        assertTrue(phoneSummary.advisoryMessage.contains("10 min banked"))

        // DISTRACTION_OVERLAY_BREAK
        val breakSummary = engine.calculateInterruptionSummary(
            plannedDurationMinutes = 45,
            elapsedSeconds = 20 * 60L,
            reason = InterruptionReason.DISTRACTION_OVERLAY_BREAK
        )
        assertEquals(20, breakSummary.elapsedMinutes)
        assertEquals(25, breakSummary.remainingMinutes)
        assertTrue(breakSummary.advisoryMessage.contains("Resting for 5 min"))
        assertTrue(breakSummary.advisoryMessage.contains("20 min banked"))

        // USER_END_EARLY
        val endEarlySummary = engine.calculateInterruptionSummary(
            plannedDurationMinutes = 30,
            elapsedSeconds = 14 * 60L,
            reason = InterruptionReason.USER_END_EARLY
        )
        assertEquals(14, endEarlySummary.elapsedMinutes)
        assertEquals(16, endEarlySummary.remainingMinutes)
        assertTrue(endEarlySummary.advisoryMessage.contains("Session ended calmly"))
        assertTrue(endEarlySummary.advisoryMessage.contains("14 min banked"))

        // USER_PAUSE with elapsed > 0
        val pauseSummary = engine.calculateInterruptionSummary(
            plannedDurationMinutes = 25,
            elapsedSeconds = 5 * 60L,
            reason = InterruptionReason.USER_PAUSE
        )
        assertEquals(5, pauseSummary.elapsedMinutes)
        assertEquals(20, pauseSummary.remainingMinutes)
        assertTrue(pauseSummary.advisoryMessage.contains("Focus block paused with 20 min remaining"))
        assertTrue(pauseSummary.advisoryMessage.contains("5 min banked!"))

        // USER_PAUSE with elapsed = 0
        val zeroElapsedSummary = engine.calculateInterruptionSummary(
            plannedDurationMinutes = 25,
            elapsedSeconds = 0L,
            reason = InterruptionReason.USER_PAUSE
        )
        assertEquals(0, zeroElapsedSummary.elapsedMinutes)
        assertEquals(25, zeroElapsedSummary.remainingMinutes)
        assertTrue(zeroElapsedSummary.advisoryMessage.contains("Ready to jump back in whenever you are."))
    }

    @Test
    fun testFormatResumptionPrompt() {
        val promptWithBanked = engine.formatResumptionPrompt(bankedMinutes = 18, remainingMinutes = 7)
        assertEquals("Pick back up where you left off — 18 min banked!", promptWithBanked)

        val promptWithoutBanked = engine.formatResumptionPrompt(bankedMinutes = 0, remainingMinutes = 25)
        assertEquals("Ready to jump back in whenever you are.", promptWithoutBanked)
    }

    // ==========================================
    // 3. Midnight Overflow Clamping Tests
    // ==========================================

    @Test
    fun testCalculateBreakOverrunClampsAtMidnightBoundary() {
        val adjustment = engine.calculateBreakOverrun(
            plannedSessionDuration = 50,
            elapsedFocusMinutes = 20,
            plannedBreakMinutes = 5,
            actualBreakMinutes = 45, // 40m overrun
            scheduledEndTime = "23:30" // 23:30 + 40m would be 00:10 next day
        )

        assertEquals("23:30", adjustment.originalEndTime)
        assertEquals("23:59", adjustment.adjustedEndTime)
        assertTrue(adjustment.advisoryMessage.contains("capped at 23:59 day boundary"))
    }

    @Test
    fun testCalculatePauseAdjustmentClampsAtMidnightBoundary() {
        val pauseAdjustment = engine.calculatePauseAdjustment(
            remainingMinutes = 20,
            pauseDurationMinutes = 60, // 23:15 + 60m = 00:15 next day
            currentScheduledEndTime = "23:15"
        )

        assertEquals("23:15", pauseAdjustment.originalEndTime)
        assertEquals("23:59", pauseAdjustment.adjustedEndTime)
        assertTrue(pauseAdjustment.advisoryMessage.contains("capped at 23:59 day boundary"))
    }

    // ==========================================
    // 4. Pomodoro Interval Progression & Cycle Tests (FR-4.1)
    // ==========================================

    @Test
    fun testPomodoroCycle1TransitionRecommendsShortBreak() {
        val transition = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.POMODORO_WORK,
            completedWorkCycles = 0
        )

        assertEquals(FocusIntervalType.SHORT_BREAK, transition.nextIntervalType)
        assertEquals(5, transition.suggestedDurationMinutes)
        assertEquals(1, transition.completedWorkCycles)
        assertTrue(transition.advisoryMessage.contains("1/4"))
        assertTrue(transition.advisoryMessage.contains("5-minute short break"))
    }

    @Test
    fun testPomodoroCycle4TransitionRecommendsLongBreak() {
        val transition = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.POMODORO_WORK,
            completedWorkCycles = 3
        )

        assertEquals(FocusIntervalType.LONG_BREAK, transition.nextIntervalType)
        assertEquals(15, transition.suggestedDurationMinutes)
        assertEquals(4, transition.completedWorkCycles)
        assertTrue(transition.advisoryMessage.contains("4 focus cycles completed"))
        assertTrue(transition.advisoryMessage.contains("15-minute long break"))
    }

    @Test
    fun testShortBreakCompletionTransitionsToPomodoroWorkPreservingCycles() {
        val transition = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.SHORT_BREAK,
            completedWorkCycles = 2
        )

        assertEquals(FocusIntervalType.POMODORO_WORK, transition.nextIntervalType)
        assertEquals(25, transition.suggestedDurationMinutes)
        assertEquals(2, transition.completedWorkCycles)
        assertTrue(transition.advisoryMessage.contains("Break complete"))
    }

    @Test
    fun testLongBreakCompletionResetsCyclesToZero() {
        val transition = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.LONG_BREAK,
            completedWorkCycles = 4
        )

        assertEquals(FocusIntervalType.POMODORO_WORK, transition.nextIntervalType)
        assertEquals(25, transition.suggestedDurationMinutes)
        assertEquals(0, transition.completedWorkCycles)
        assertTrue(transition.advisoryMessage.contains("Long break complete"))
    }

    @Test
    fun testDeepWorkAndReviewCompletionSuggestsShortBreak() {
        val transitionDeep = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.DEEP_WORK
        )
        assertEquals(FocusIntervalType.SHORT_BREAK, transitionDeep.nextIntervalType)
        assertEquals(5, transitionDeep.suggestedDurationMinutes)

        val transitionReview = engine.calculateIntervalTransition(
            currentIntervalType = FocusIntervalType.REVIEW
        )
        assertEquals(FocusIntervalType.SHORT_BREAK, transitionReview.nextIntervalType)
        assertEquals(5, transitionReview.suggestedDurationMinutes)
    }

    @Test
    fun testCycleTrackingAndDurationHelpers() {
        assertEquals(
            FocusIntervalType.SHORT_BREAK,
            engine.getNextIntervalType(FocusIntervalType.POMODORO_WORK, 0)
        )
        assertEquals(
            FocusIntervalType.LONG_BREAK,
            engine.getNextIntervalType(FocusIntervalType.POMODORO_WORK, 3)
        )
        assertEquals(
            FocusIntervalType.POMODORO_WORK,
            engine.getNextIntervalType(FocusIntervalType.SHORT_BREAK, 2)
        )

        assertEquals(25, engine.getSuggestedDuration(FocusIntervalType.POMODORO_WORK))
        assertEquals(5, engine.getSuggestedDuration(FocusIntervalType.SHORT_BREAK))
        assertEquals(15, engine.getSuggestedDuration(FocusIntervalType.LONG_BREAK))
        assertEquals(45, engine.getSuggestedDuration(FocusIntervalType.DEEP_WORK))
    }
}
