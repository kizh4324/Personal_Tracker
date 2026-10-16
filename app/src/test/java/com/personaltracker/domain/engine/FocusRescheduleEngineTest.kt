package com.personaltracker.domain.engine

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
}
