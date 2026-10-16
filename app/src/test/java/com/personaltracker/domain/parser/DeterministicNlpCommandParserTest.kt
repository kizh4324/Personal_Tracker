package com.personaltracker.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DeterministicNlpCommandParserTest {

    private lateinit var parser: DeterministicNlpCommandParser
    private val fixedReferenceDate: LocalDate = LocalDate.of(2026, 10, 16)

    @Before
    fun setUp() {
        parser = DeterministicNlpCommandParser()
    }

    // ==========================================
    // 1. High Confidence Parsing (> 0.85)
    // ==========================================

    @Test
    fun testParseHighConfidenceTaskFullAttributes() {
        val input = "Study algorithms for 45 minutes at 4pm tomorrow urgent"

        val startTime = System.currentTimeMillis()
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)
        val elapsed = System.currentTimeMillis() - startTime

        // Verification of latency (< 300ms)
        assertTrue("Execution must be < 300ms, was ${elapsed}ms", elapsed < 300)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Study algorithms", parsed.title)
        assertEquals(45, parsed.durationMinutes)
        assertEquals("16:00", parsed.time)
        assertEquals("2026-10-17", parsed.date) // tomorrow
        assertEquals("URGENT", parsed.deliveryIntensity)
        assertTrue("Confidence must be >= 0.85, was ${parsed.confidence}", parsed.confidence >= 0.85f)
        assertEquals(ConfidenceTier.HIGH, parsed.tier)
    }

    // ==========================================
    // 2. Medium Confidence Parsing (0.50..0.85)
    // ==========================================

    @Test
    fun testParseMediumConfidenceTaskPartialAttributes() {
        val input = "Read book tomorrow"

        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.CREATE_TASK, parsed.intent)
        assertEquals("Read book", parsed.title)
        assertEquals("2026-10-17", parsed.date)
        assertNull(parsed.time)
        assertNull(parsed.durationMinutes)
        assertEquals("ROUTINE", parsed.deliveryIntensity)
        assertTrue(
            "Confidence must be in 0.50..0.85, was ${parsed.confidence}",
            parsed.confidence in 0.50f..0.85f
        )
        assertEquals(ConfidenceTier.MEDIUM, parsed.tier)
    }

    // ==========================================
    // 3. Low Confidence / Gibberish Parsing (< 0.50)
    // ==========================================

    @Test
    fun testParseLowConfidenceGibberishRoutesToLowTier() {
        val input = "mumble gibberish random noises"

        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.UNKNOWN, parsed.intent)
        assertTrue(
            "Confidence must be < 0.50, was ${parsed.confidence}",
            parsed.confidence < 0.50f
        )
        assertEquals(ConfidenceTier.LOW, parsed.tier)
    }

    @Test
    fun testParseBlankStringReturnsLowConfidence() {
        val parsed = parser.parse("   ")
        assertEquals(CommandIntent.UNKNOWN, parsed.intent)
        assertEquals(0.0f, parsed.confidence, 0.001f)
        assertEquals(ConfidenceTier.LOW, parsed.tier)
    }

    // ==========================================
    // 4. Intent Extraction Variants
    // ==========================================

    @Test
    fun testParseHabitIntent() {
        val input = "Log habit Drink 2L Water"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.LOG_HABIT, parsed.intent)
        assertTrue(parsed.title.contains("Drink 2L Water"))
    }

    @Test
    fun testParseFocusIntent() {
        val input = "Start focus for 25 minutes"
        val parsed = parser.parse(input, referenceDate = fixedReferenceDate)

        assertEquals(CommandIntent.START_FOCUS, parsed.intent)
        assertEquals(25, parsed.durationMinutes)
    }

    @Test
    fun testParseTimeFormats() {
        val parsedAm = parser.parse("Review pull request at 9:30 am today", referenceDate = fixedReferenceDate)
        assertEquals("09:30", parsedAm.time)

        val parsed24 = parser.parse("Submit report at 14:15 today", referenceDate = fixedReferenceDate)
        assertEquals("14:15", parsed24.time)
    }
}
