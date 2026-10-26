package com.personaltracker.domain.engine

import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class HeatmapLedgerEngineTest {

    private lateinit var engine: HeatmapLedgerEngine

    @Before
    fun setUp() {
        engine = HeatmapLedgerEngine()
    }

    // ==========================================
    // 1. 5-Tier Intensity Mapping Tests (FR-4.2)
    // ==========================================

    @Test
    fun testResolveTierMapsCorrectlyAcrossAllThresholds() {
        // Tier 0: 0 minutes
        assertEquals(0, engine.resolveTier(0))
        assertEquals(0, engine.resolveTier(-10)) // Negative input clamped safely

        // Tier 1: 1–29 minutes
        assertEquals(1, engine.resolveTier(1))
        assertEquals(1, engine.resolveTier(15))
        assertEquals(1, engine.resolveTier(29))

        // Tier 2: 30–59 minutes
        assertEquals(2, engine.resolveTier(30))
        assertEquals(2, engine.resolveTier(45))
        assertEquals(2, engine.resolveTier(59))

        // Tier 3: 60–119 minutes
        assertEquals(3, engine.resolveTier(60))
        assertEquals(3, engine.resolveTier(90))
        assertEquals(3, engine.resolveTier(119))

        // Tier 4: 120+ minutes
        assertEquals(4, engine.resolveTier(120))
        assertEquals(4, engine.resolveTier(180))
        assertEquals(4, engine.resolveTier(500))
    }

    // ==========================================
    // 2. Empty History Edge-Case Tests
    // ==========================================

    @Test
    fun testEmptySessionHistoryGenerates28Tier0Cells() {
        val today = LocalDate.of(2026, 10, 26)
        val heatmap = engine.calculateHeatmap(
            sessions = emptyList(),
            referenceDate = today,
            days = 28
        )

        assertEquals(28, heatmap.cells.size)
        assertEquals(0, heatmap.totalFocusMinutes)
        assertEquals(0, heatmap.activeDaysCount)
        assertTrue(heatmap.subjectBreakdown.isEmpty())
        assertNull(heatmap.topSubject)

        // Verify chronological order ending at referenceDate
        assertEquals("2026-09-29", heatmap.cells.first().date)
        assertEquals("2026-10-26", heatmap.cells.last().date)

        // All cells must be Tier 0 with 0 minutes
        heatmap.cells.forEach { cell ->
            assertEquals(0, cell.totalMinutes)
            assertEquals(0, cell.intensityTier)
            assertEquals(0, cell.sessionCount)
            assertTrue(cell.subjectBreakdown.isEmpty())
        }
    }

    // ==========================================
    // 3. Multi-Session Daily Aggregation Tests
    // ==========================================

    @Test
    fun testMultiSessionDailyAggregationCalculatesTiersAndBreakdown() {
        val today = LocalDate.of(2026, 10, 26)
        val todayStr = "2026-10-26"

        // Scenario: 25m Pomodoro + 15m Reading + 40m Deep Work on same date -> Total = 80m (Tier 3)
        val s1 = StudySession(
            id = "s1",
            subjectTag = "Pomodoro",
            sessionDate = todayStr,
            startTime = "09:00",
            durationMinutes = 25,
            intervalType = FocusIntervalType.POMODORO_WORK
        )
        val s2 = StudySession(
            id = "s2",
            subjectTag = "Reading",
            sessionDate = todayStr,
            startTime = "11:00",
            durationMinutes = 15,
            intervalType = FocusIntervalType.DEEP_WORK
        )
        val s3 = StudySession(
            id = "s3",
            subjectTag = "Deep Work",
            sessionDate = todayStr,
            startTime = "14:00",
            durationMinutes = 40,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        val heatmap = engine.calculateHeatmap(
            sessions = listOf(s1, s2, s3),
            referenceDate = today,
            days = 28
        )

        assertEquals(28, heatmap.cells.size)
        assertEquals(80, heatmap.totalFocusMinutes)
        assertEquals(1, heatmap.activeDaysCount)

        val todayCell = heatmap.cells.last()
        assertEquals(todayStr, todayCell.date)
        assertEquals(80, todayCell.totalMinutes)
        assertEquals(3, todayCell.intensityTier) // 60..119m -> Tier 3
        assertEquals(3, todayCell.sessionCount)

        assertEquals(25, todayCell.subjectBreakdown["Pomodoro"])
        assertEquals(15, todayCell.subjectBreakdown["Reading"])
        assertEquals(40, todayCell.subjectBreakdown["Deep Work"])

        assertEquals("Deep Work", heatmap.topSubject)
    }

    // ==========================================
    // 4. Break Session & Interruption Filtering Tests
    // ==========================================

    @Test
    fun testBreakSessionsAreExcludedFromFocusHeatmap() {
        val today = LocalDate.of(2026, 10, 26)
        val todayStr = "2026-10-26"

        val workSession = StudySession(
            id = "w1",
            subjectTag = "Deep Work",
            sessionDate = todayStr,
            startTime = "09:00",
            durationMinutes = 45,
            intervalType = FocusIntervalType.DEEP_WORK
        )
        val breakSession = StudySession(
            id = "b1",
            subjectTag = "Break",
            sessionDate = todayStr,
            startTime = "09:45",
            durationMinutes = 15,
            intervalType = FocusIntervalType.SHORT_BREAK
        )

        val heatmap = engine.calculateHeatmap(
            sessions = listOf(workSession, breakSession),
            referenceDate = today,
            days = 28
        )

        val todayCell = heatmap.cells.last()
        assertEquals(45, todayCell.totalMinutes)
        assertEquals(2, todayCell.intensityTier) // 30..59m -> Tier 2
        assertEquals(1, todayCell.sessionCount) // Break session excluded
        assertNull(todayCell.subjectBreakdown["Break"])
    }

    @Test
    fun testInterruptedFocusBlockCreditsActualElapsedMinutes() {
        val today = LocalDate.of(2026, 10, 26)
        val todayStr = "2026-10-26"

        // Scenario: Session planned for 45m ended early at 15m elapsed
        val interrupted = StudySession(
            id = "int1",
            subjectTag = "Math",
            sessionDate = todayStr,
            startTime = "10:00",
            durationMinutes = 45,
            targetDurationMinutes = 45,
            actualElapsedMinutes = 15,
            isInterrupted = true,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        val heatmap = engine.calculateHeatmap(
            sessions = listOf(interrupted),
            referenceDate = today,
            days = 28
        )

        val todayCell = heatmap.cells.last()
        assertEquals(15, todayCell.totalMinutes)
        assertEquals(1, todayCell.intensityTier) // 1..29m -> Tier 1
        assertEquals(15, todayCell.subjectBreakdown["Math"])
    }

    // ==========================================
    // 5. Multi-Day Chronological Distribution Tests
    // ==========================================

    @Test
    fun testMultiDaySessionDistributionAcross28Days() {
        val refDate = LocalDate.of(2026, 10, 26)

        val day1 = StudySession(
            id = "d1",
            subjectTag = "Writing",
            sessionDate = "2026-10-20",
            startTime = "10:00",
            durationMinutes = 130, // Tier 4 (120m+)
            intervalType = FocusIntervalType.DEEP_WORK
        )
        val day2 = StudySession(
            id = "d2",
            subjectTag = "Coding",
            sessionDate = "2026-10-22",
            startTime = "14:00",
            durationMinutes = 50, // Tier 2 (30-59m)
            intervalType = FocusIntervalType.DEEP_WORK
        )
        val day3 = StudySession(
            id = "d3",
            subjectTag = "Coding",
            sessionDate = "2026-10-26",
            startTime = "09:00",
            durationMinutes = 20, // Tier 1 (1-29m)
            intervalType = FocusIntervalType.DEEP_WORK
        )

        val heatmap = engine.calculateHeatmap(
            sessions = listOf(day1, day2, day3),
            referenceDate = refDate,
            days = 28
        )

        assertEquals(28, heatmap.cells.size)
        assertEquals(200, heatmap.totalFocusMinutes)
        assertEquals(3, heatmap.activeDaysCount)

        val cellOct20 = heatmap.cells.find { it.date == "2026-10-20" }!!
        assertEquals(130, cellOct20.totalMinutes)
        assertEquals(4, cellOct20.intensityTier)

        val cellOct22 = heatmap.cells.find { it.date == "2026-10-22" }!!
        assertEquals(50, cellOct22.totalMinutes)
        assertEquals(2, cellOct22.intensityTier)

        val cellOct26 = heatmap.cells.find { it.date == "2026-10-26" }!!
        assertEquals(20, cellOct26.totalMinutes)
        assertEquals(1, cellOct26.intensityTier)

        assertEquals("Writing", heatmap.topSubject) // 130 > 70
        assertEquals(130, heatmap.subjectBreakdown["Writing"])
        assertEquals(70, heatmap.subjectBreakdown["Coding"])
    }
}
