package com.personaltracker.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DeterministicNlpCommandParserTest {

    private lateinit var parser: DeterministicNlpCommandParser

    // Fixed reference: Monday, October 26, 2026 at 10:00:00
    private val fixedReferenceDate: LocalDate = LocalDate.of(2026, 10, 26)
    private val fixedReferenceTime: LocalTime = LocalTime.of(10, 0)
    private val fixedReferenceDateTime: LocalDateTime = LocalDateTime.of(fixedReferenceDate, fixedReferenceTime)

    @Before
    fun setUp() {
        parser = DeterministicNlpCommandParser()
    }

    // ==========================================
    // 1. High Confidence Parsing (>= 0.85, Tier: HIGH)
    // ==========================================

    @Test
    fun testParseHighConfidenceTaskFullAttributes() {
        // Scenario 1: "Remind me to review PR at 4pm tomorrow urgent" (ref: 2026-10-26 10:00)
        val input = "Remind me to review PR at 4pm tomorrow urgent"

        val startTime = System.currentTimeMillis()
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate, referenceTime = fixedReferenceTime)
        val elapsed = System.currentTimeMillis() - startTime

        // Latency requirement: strictly < 300ms on JVM (NFR-1)
        assertTrue("Execution must be < 300ms, was ${elapsed}ms", elapsed < 300)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Review PR", parsed.title)
        assertEquals("2026-10-27", parsed.date)
        assertEquals("16:00", parsed.time)
        assertEquals("URGENT", parsed.deliveryIntensity)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseRelativeTimeOffsetTask() {
        // Scenario 2: "Study physics in 30 minutes for 45 mins" (ref: 2026-10-26 14:15)
        val ref = LocalDateTime.of(fixedReferenceDate, LocalTime.of(14, 15))
        val input = "Study physics in 30 minutes for 45 mins"

        val parsed = parser.parse(input, referenceDateTime = ref)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Study physics", parsed.title)
        assertEquals("2026-10-26", parsed.date)
        assertEquals("14:45", parsed.time)
        assertEquals(45, parsed.durationMinutes)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseDayOfWeekTask() {
        // Scenario 3: "Team sync next monday at 10am" (ref: Mon 2026-10-26)
        val input = "Team sync next monday at 10am"

        val parsed = parser.parse(input, referenceDate = fixedReferenceDate, referenceTime = fixedReferenceTime)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Team sync", parsed.title)
        assertEquals("2026-11-02", parsed.date) // Resolves via TemporalAdjusters.next
        assertEquals("10:00", parsed.time)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseExistingHighConfidenceAlgorithmsTask() {
        val input = "Study algorithms for 45 minutes at 4pm tomorrow urgent"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Study algorithms", parsed.title)
        assertEquals(45, parsed.durationMinutes)
        assertEquals("16:00", parsed.time)
        assertEquals("2026-10-27", parsed.date)
        assertEquals("URGENT", parsed.deliveryIntensity)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    // ==========================================
    // 2. Medium Confidence Parsing (0.50..0.84, Tier: MEDIUM)
    // ==========================================

    @Test
    fun testParseMediumConfidenceTaskPartialAttributes() {
        // Scenario 4: "Buy groceries tomorrow" (ref: 2026-10-26)
        val input = "Buy groceries tomorrow"

        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Buy groceries", parsed.title)
        assertEquals("2026-10-27", parsed.date)
        assertNull(parsed.time)
        assertNull(parsed.durationMinutes)
        assertEquals("ROUTINE", parsed.deliveryIntensity)
        assertTrue(
            "Confidence must be in 0.50f..0.84f, was ${parsed.confidence}",
            parsed.confidence in 0.50f..0.84f
        )
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    @Test
    fun testParseReadBookTomorrowMediumConfidence() {
        val input = "Read book tomorrow"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Read book", parsed.title)
        assertEquals("2026-10-27", parsed.date)
        assertNull(parsed.time)
        assertNull(parsed.durationMinutes)
        assertEquals("ROUTINE", parsed.deliveryIntensity)
        assertTrue(
            "Confidence must be in 0.50f..0.84f, was ${parsed.confidence}",
            parsed.confidence in 0.50f..0.84f
        )
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    // ==========================================
    // 3. Shift Routine Intent Parsing
    // ==========================================

    @Test
    fun testParseShiftRoutineIntentHighConfidence() {
        // Scenario 5: "Shift morning routine by 15 minutes"
        val input = "Shift morning routine by 15 minutes"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.SHIFT_ROUTINE, parsed.intent)
        assertEquals("morning routine", parsed.title)
        assertEquals("morning", parsed.targetRoutineName)
        assertEquals(15, parsed.shiftMinutes)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseShiftRoutineVariants() {
        val delay = parser.parse("Delay morning routine by 10 minutes", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, delay.intent)
        assertEquals("morning", delay.targetRoutineName)
        assertEquals(10, delay.shiftMinutes)
        assertEquals(ConfidenceTier.HIGH, delay.tier)

        val pushBack = parser.parse("Push back bedtime routine by 20 minutes", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, pushBack.intent)
        assertEquals("bedtime", pushBack.targetRoutineName)
        assertEquals(20, pushBack.shiftMinutes)
        assertEquals(ConfidenceTier.HIGH, pushBack.tier)

        // Routine shift by hour (1 hour = 60 mins)
        val hourShift = parser.parse("Shift morning routine by 1 hour", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, hourShift.intent)
        assertEquals("morning", hourShift.targetRoutineName)
        assertEquals(60, hourShift.shiftMinutes)
        assertEquals(ConfidenceTier.HIGH, hourShift.tier)

        // Multi-word routine name ("wind down")
        val multiWord = parser.parse("Shift wind down routine by 15 minutes", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, multiWord.intent)
        assertEquals("wind down", multiWord.targetRoutineName)
        assertEquals(15, multiWord.shiftMinutes)
        assertEquals(ConfidenceTier.HIGH, multiWord.tier)

        // Shift with "the routine" -> targetRoutineName is null, shiftMinutes = 15
        val theRoutine = parser.parse("Shift the routine by 15 minutes", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, theRoutine.intent)
        assertNull(theRoutine.targetRoutineName)
        assertEquals(15, theRoutine.shiftMinutes)
        assertTrue(theRoutine.confidence in 0.50f..0.84f)
        assertEquals(ConfidenceTier.MEDIUM, theRoutine.tier)

        // Missing routine name -> MEDIUM tier
        val noName = parser.parse("Shift routine by 15 minutes", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, noName.intent)
        assertNull(noName.targetRoutineName)
        assertEquals(15, noName.shiftMinutes)
        assertTrue(noName.confidence in 0.50f..0.84f)
        assertEquals(ConfidenceTier.MEDIUM, noName.tier)

        // Missing minutes -> MEDIUM tier
        val noMinutes = parser.parse("Shift morning routine", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.SHIFT_ROUTINE, noMinutes.intent)
        assertEquals("morning", noMinutes.targetRoutineName)
        assertNull(noMinutes.shiftMinutes)
        assertTrue(noMinutes.confidence in 0.50f..0.84f)
        assertEquals(ConfidenceTier.MEDIUM, noMinutes.tier)
    }

    // ==========================================
    // 4. Log Habit Intent Parsing
    // ==========================================

    @Test
    fun testParseLogHabitIntent() {
        // Scenario 6: "Log habit Drink 2L water"
        val input = "Log habit Drink 2L water"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.LOG_HABIT, parsed.intent)
        assertEquals("Drink 2L water", parsed.title)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseTrackHabitVariants() {
        val parsed = parser.parse("Track habit Morning meditation", referenceDate = fixedReferenceDate)
        assertEquals(CommandIntent.LOG_HABIT, parsed.intent)
        assertEquals("Morning meditation", parsed.title)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    // ==========================================
    // 5. Start Focus Intent Parsing
    // ==========================================

    @Test
    fun testParseStartFocusIntent() {
        // Scenario 7: "Start focus for 25 minutes"
        val input = "Start focus for 25 minutes"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.START_FOCUS, parsed.intent)
        assertEquals("Focus", parsed.title)
        assertEquals(25, parsed.durationMinutes)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseFocusSessionWithSubject() {
        val input = "Focus on math for 30 minutes"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.START_FOCUS, parsed.intent)
        assertEquals(30, parsed.durationMinutes)
        assertTrue(parsed.title.contains("math", ignoreCase = true))
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    @Test
    fun testParseFocusWithoutDurationIsMedium() {
        val input = "Start focus"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.START_FOCUS, parsed.intent)
        assertEquals("Focus", parsed.title)
        assertNull(parsed.durationMinutes)
        assertTrue(parsed.confidence in 0.50f..0.84f)
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    @Test
    fun testParsePrecedingModifierTaskPrefixStripping() {
        // Preceding modifier: "Tomorrow remind me to buy milk"
        val input = "Tomorrow remind me to buy milk"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Buy milk", parsed.title)
        assertEquals("2026-10-27", parsed.date)
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    // ==========================================
    // 6. Low Confidence / Gibberish (< 0.50, Tier: LOW)
    // ==========================================

    @Test
    fun testParseLowConfidenceGibberishRoutesToLowTier() {
        // Scenario 8: "mumble random background chatter"
        val input = "mumble random background chatter"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.UNKNOWN, parsed.intent)
        assertTrue("Confidence must be < 0.50, was ${parsed.confidence}", parsed.confidence < 0.50f)
        assertEquals(ConfidenceTier.LOW, parsed.tier)
    }

    @Test
    fun testParseBlankStringReturnsLowConfidence() {
        // Scenario 9: "    "
        val parsed = parser.parse("   ")
        assertEquals(CommandIntent.UNKNOWN, parsed.intent)
        assertEquals("", parsed.title)
        assertEquals(0.0f, parsed.confidence, 0.001f)
        assertEquals(ConfidenceTier.LOW, parsed.tier)
    }

    // ==========================================
    // 7. Exact Confidence Boundaries & Tier Mapping
    // ==========================================

    @Test
    fun testExactBoundaryScoresAlignWithAd8() {
        // At exactly 0.85 confidence (Tier: HIGH):
        // Habit with title >= 3: 0.50 + 0.10 (keyword) + 0.25 (target) = 0.85f
        val atBoundary85 = parser.parse("Log habit Drink water", referenceDate = fixedReferenceDate)
        assertEquals(0.85f, atBoundary85.confidence, 0.001f)
        assertEquals(ConfidenceTier.HIGH, atBoundary85.tier)

        // Medium tier (0.50..0.84):
        // Task with keyword + title >= 3: 0.50 + 0.10 = 0.60f
        val mediumTask = parser.parse("Study math", referenceDate = fixedReferenceDate)
        assertEquals(0.60f, mediumTask.confidence, 0.001f)
        assertTrue(mediumTask.confidence in 0.50f..0.84f)
        assertEquals(ConfidenceTier.MEDIUM, mediumTask.tier)

        // Below 0.50 confidence (Tier: LOW):
        // Title < 3 chars: 0.20 + 0.10 = 0.30f
        val shortTitle = parser.parse("add a", referenceDate = fixedReferenceDate)
        assertEquals(0.30f, shortTitle.confidence, 0.001f)
        assertTrue(shortTitle.confidence < 0.50f)
        assertEquals(ConfidenceTier.LOW, shortTitle.tier)

        // Unknown intent: 0.25f
        val unknownIntent = parser.parse("random chatter", referenceDate = fixedReferenceDate)
        assertEquals(0.25f, unknownIntent.confidence, 0.001f)
        assertTrue(unknownIntent.confidence < 0.50f)
        assertEquals(ConfidenceTier.LOW, unknownIntent.tier)
    }

    // ==========================================
    // 8. Time & Title Cleanup Verification
    // ==========================================

    @Test
    fun testMidnightAndNoon12HourTimeParsing() {
        val midnight = parser.parse("Meditation at 12:00 am today", referenceDate = fixedReferenceDate)
        assertEquals("00:00", midnight.time)

        val noon = parser.parse("Lunch with team at 12:00 pm today", referenceDate = fixedReferenceDate)
        assertEquals("12:00", noon.time)

        val pastMidnight = parser.parse("Read at 12:45 am today", referenceDate = fixedReferenceDate)
        assertEquals("00:45", pastMidnight.time)

        val afternoon = parser.parse("Walk at 12:30 pm today", referenceDate = fixedReferenceDate)
        assertEquals("12:30", afternoon.time)
    }

    @Test
    fun testTokenStrippingCollapsesWhitespaceAndRemovesTrailingPrepositions() {
        val parsed = parser.parse("Finish report for 30 minutes at 3pm tomorrow urgent", referenceDate = fixedReferenceDate)
        assertEquals("Finish report", parsed.title)
        assertFalse(parsed.title.contains("  "))
    }

    @Test
    fun testLatencyUnderThreeHundredMillisOverMultipleIterations() {
        val inputs = listOf(
            "Remind me to review PR at 4pm tomorrow urgent",
            "Study physics in 30 minutes for 45 mins",
            "Team sync next monday at 10am",
            "Buy groceries tomorrow",
            "Shift morning routine by 15 minutes",
            "Log habit Drink 2L water",
            "Start focus for 25 minutes",
            "mumble random background chatter"
        )

        for (input in inputs) {
            val start = System.nanoTime()
            val result = parser.parse(input, referenceDate = fixedReferenceDate, referenceTime = fixedReferenceTime)
            val durationMs = (System.nanoTime() - start) / 1_000_000.0
            assertTrue("Parsing '$input' took ${durationMs}ms, which must be < 300ms", durationMs < 300.0)
            assertNotNull(result)
        }
    }
}
