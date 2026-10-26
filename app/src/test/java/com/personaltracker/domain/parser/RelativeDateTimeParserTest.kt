package com.personaltracker.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RelativeDateTimeParserTest {

    private lateinit var parser: RelativeDateTimeParser
    // Fixed reference: Monday, October 26, 2026 at 14:15:00
    private val fixedReferenceDate = LocalDate.of(2026, 10, 26)
    private val fixedReferenceTime = LocalTime.of(14, 15)
    private val fixedReferenceDateTime = LocalDateTime.of(fixedReferenceDate, fixedReferenceTime)

    @Before
    fun setUp() {
        parser = RelativeDateTimeParser()
    }

    // ==========================================
    // 1. Relative Date Parsing
    // ==========================================

    @Test
    fun testExplicitIsoDateParsing() {
        val result = parser.parse("Project milestone 2026-11-15", fixedReferenceDateTime)
        assertEquals("2026-11-15", result.date)
        assertEquals("2026-11-15", result.dateToken)

        // Invalid calendar date (e.g. Feb 31) must not produce an invalid date string
        val invalidDate = parser.parse("Milestone on 2026-02-31", fixedReferenceDateTime)
        assertNull(invalidDate.date)
    }

    @Test
    fun testRelativeDatesTodayTomorrowAndDayAfter() {
        val today = parser.parse("Review report today", fixedReferenceDateTime)
        assertEquals("2026-10-26", today.date)
        assertEquals("today", today.dateToken)

        val tomorrow = parser.parse("Review PR tomorrow", fixedReferenceDateTime)
        assertEquals("2026-10-27", tomorrow.date)
        assertEquals("tomorrow", tomorrow.dateToken)

        val dayAfter = parser.parse("Submit paper day after tomorrow", fixedReferenceDateTime)
        assertEquals("2026-10-28", dayAfter.date)
        assertEquals("day after tomorrow", dayAfter.dateToken)

        val yesterday = parser.parse("Note from yesterday", fixedReferenceDateTime)
        assertEquals("2026-10-25", yesterday.date)
        assertEquals("yesterday", yesterday.dateToken)
    }

    @Test
    fun testTonightDateAndDefaultTime() {
        val result = parser.parse("Meditation tonight", fixedReferenceDateTime)
        assertEquals("2026-10-26", result.date)
        assertEquals("20:00", result.time)
        assertEquals("tonight", result.dateToken)
    }

    @Test
    fun testInDaysOffset() {
        val in3Days = parser.parse("Conference in 3 days", fixedReferenceDateTime)
        assertEquals("2026-10-29", in3Days.date)
        assertEquals("in 3 days", in3Days.dateToken)

        val in1Day = parser.parse("Call doctor in 1 day", fixedReferenceDateTime)
        assertEquals("2026-10-27", in1Day.date)

        val in7Days = parser.parse("Review sprint in 7 days", fixedReferenceDateTime)
        assertEquals("2026-11-02", in7Days.date)

        // "in 3D" must NOT match as 3 days
        val threeD = parser.parse("Model in 3D environment", fixedReferenceDateTime)
        assertNull(threeD.date)
    }

    @Test
    fun testDayOfWeekResolutionWithTemporalAdjusters() {
        // Monday 2026-10-26 -> "next monday" resolves to 2026-11-02 via TemporalAdjusters.next
        val nextMonday = parser.parse("Team sync next monday at 10am", fixedReferenceDateTime)
        assertEquals("2026-11-02", nextMonday.date)
        assertEquals("next monday", nextMonday.dateToken)
        assertEquals("10:00", nextMonday.time)

        // Monday 2026-10-26 -> "this monday" resolves to 2026-10-26 via TemporalAdjusters.nextOrSame
        val thisMonday = parser.parse("Status update this monday", fixedReferenceDateTime)
        assertEquals("2026-10-26", thisMonday.date)

        // Monday 2026-10-26 -> "this friday" resolves to 2026-10-30
        val thisFriday = parser.parse("Sprint demo this friday", fixedReferenceDateTime)
        assertEquals("2026-10-30", thisFriday.date)

        // Monday 2026-10-26 -> "on wednesday" resolves to 2026-10-28
        val onWednesday = parser.parse("Dentist appointment on wednesday", fixedReferenceDateTime)
        assertEquals("2026-10-28", onWednesday.date)

        // Abbreviated day of week: "fri"
        val friAbbr = parser.parse("Gym session fri", fixedReferenceDateTime)
        assertEquals("2026-10-30", friAbbr.date)
    }

    // ==========================================
    // 2. Relative Time Offset Parsing
    // ==========================================

    @Test
    fun testRelativeTimeOffsetMinutesAndHours() {
        // ref: 14:15 -> "in 30 minutes" = 14:45
        val in30Mins = parser.parse("Study physics in 30 minutes for 45 mins", fixedReferenceDateTime)
        assertEquals("14:45", in30Mins.time)
        assertEquals("in 30 minutes", in30Mins.timeToken)
        assertEquals("2026-10-26", in30Mins.date)
        assertTrue(in30Mins.isInferredDate)

        // ref: 14:15 -> "in 45 mins" = 15:00
        val in45Mins = parser.parse("Call client in 45 mins", fixedReferenceDateTime)
        assertEquals("15:00", in45Mins.time)

        // ref: 14:15 -> "in 2 hours" = 16:15
        val in2Hours = parser.parse("Check oven in 2 hours", fixedReferenceDateTime)
        assertEquals("16:15", in2Hours.time)

        // ref: 14:15 -> "in half an hour" = 14:45
        val halfHour = parser.parse("Take break in half an hour", fixedReferenceDateTime)
        assertEquals("14:45", halfHour.time)

        // ref: 14:15 -> "in an hour" = 15:15
        val oneHour = parser.parse("Resume focus in an hour", fixedReferenceDateTime)
        assertEquals("15:15", oneHour.time)
    }

    @Test
    fun testRelativeTimeOffsetRollsOverMidnight() {
        // ref: 23:45 on 2026-10-26 -> +30 mins = 00:15 on 2026-10-27
        val lateNight = LocalDateTime.of(fixedReferenceDate, LocalTime.of(23, 45))
        val result = parser.parse("Late task in 30 minutes", lateNight)
        assertEquals("00:15", result.time)
        assertEquals("2026-10-27", result.date)
        assertTrue(result.isInferredDate)
    }

    // ==========================================
    // 3. Absolute Time Formats
    // ==========================================

    @Test
    fun test12HourAmPmTimeFormats() {
        val fourPm = parser.parse("Review PR at 4pm tomorrow", fixedReferenceDateTime)
        assertEquals("16:00", fourPm.time)
        assertEquals("at 4pm", fourPm.timeToken)

        val nineThirtyAm = parser.parse("Daily standup at 9:30 am", fixedReferenceDateTime)
        assertEquals("09:30", nineThirtyAm.time)

        val noon = parser.parse("Lunch at 12:00 pm", fixedReferenceDateTime)
        assertEquals("12:00", noon.time)

        val midnight = parser.parse("Sleep at 12:00 am", fixedReferenceDateTime)
        assertEquals("00:00", midnight.time)

        val pastMidnight = parser.parse("Reading at 12:45 am", fixedReferenceDateTime)
        assertEquals("00:45", pastMidnight.time)

        val afternoon = parser.parse("Walk at 12:30 pm", fixedReferenceDateTime)
        assertEquals("12:30", afternoon.time)
    }

    @Test
    fun test24HourTimeFormats() {
        val at1415 = parser.parse("Submit report at 14:15 today", fixedReferenceDateTime)
        assertEquals("14:15", at1415.time)

        val at0900 = parser.parse("Check emails 09:00", fixedReferenceDateTime)
        assertEquals("09:00", at0900.time)

        val at2359 = parser.parse("End of day at 23:59", fixedReferenceDateTime)
        assertEquals("23:59", at2359.time)
    }

    @Test
    fun testNamedPeriods() {
        val noon = parser.parse("Meet for lunch at noon", fixedReferenceDateTime)
        assertEquals("12:00", noon.time)

        val midnight = parser.parse("Release lock at midnight", fixedReferenceDateTime)
        assertEquals("00:00", midnight.time)

        val morning = parser.parse("Jogging this morning", fixedReferenceDateTime)
        assertEquals("09:00", morning.time)
        assertEquals("2026-10-26", morning.date)
        assertTrue(morning.isInferredDate)

        val afternoon = parser.parse("Coffee in the afternoon", fixedReferenceDateTime)
        assertEquals("14:00", afternoon.time)

        val thisAfternoon = parser.parse("Coffee this afternoon", fixedReferenceDateTime)
        assertEquals("14:00", thisAfternoon.time)
        assertEquals("2026-10-26", thisAfternoon.date)
        assertTrue(thisAfternoon.isInferredDate)

        val evening = parser.parse("Read book this evening", fixedReferenceDateTime)
        assertEquals("19:00", evening.time)
        assertEquals("2026-10-26", evening.date)
        assertTrue(evening.isInferredDate)
    }

    // ==========================================
    // 4. Convenience Utility Methods
    // ==========================================

    @Test
    fun testConvenienceMethodsParseDateAndParseTime() {
        val dateOnly = parser.parseDate("Buy flowers tomorrow", fixedReferenceDate)
        assertEquals("2026-10-27", dateOnly)

        val timeOnly = parser.parseTime("Sync at 3:30 pm", fixedReferenceDateTime)
        assertEquals("15:30", timeOnly)

        val noDate = parser.parseDate("Just a general note", fixedReferenceDate)
        assertNull(noDate)

        val noTime = parser.parseTime("General task for tomorrow", fixedReferenceDateTime)
        assertNull(noTime)
    }
}
