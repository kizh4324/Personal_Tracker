package com.personaltracker.domain.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Pure Kotlin relative date and time parsing engine (FR-7, AD-1, AD-8).
 *
 * Resolves natural language date and time expressions locally without Android framework
 * dependencies or external network calls:
 * - Relative dates ("today", "tomorrow", "day after tomorrow", "yesterday", "in 3 days")
 * - Day of week ("monday", "this friday", "next monday", "on wednesday")
 * - Relative time offsets ("in 30 minutes", "in 2 hours", "in half an hour")
 * - Explicit 12-hour AM/PM ("at 4pm", "9:30 am", "12:00 am", "12:00 pm")
 * - Explicit 24-hour time ("14:15", "at 16:00")
 * - Named periods ("noon", "midnight", "tonight", "this morning", "in the afternoon")
 */
class RelativeDateTimeParser {

    private val timeFormat24 = DateTimeFormatter.ofPattern("HH:mm")

    data class Result(
        val date: String? = null, // "YYYY-MM-DD"
        val time: String? = null, // "HH:mm"
        val dateToken: String? = null,
        val timeToken: String? = null,
        val isInferredDate: Boolean = false
    )

    // Date regexes (ordered by specificity)
    private val isoDateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
    private val dayAfterTomorrowRegex = Regex("""\bday\s+after\s+tomorrow\b""", RegexOption.IGNORE_CASE)
    private val tomorrowRegex = Regex("""\btomorrow\b""", RegexOption.IGNORE_CASE)
    private val todayRegex = Regex("""\btoday\b""", RegexOption.IGNORE_CASE)
    private val tonightDateRegex = Regex("""\btonight\b""", RegexOption.IGNORE_CASE)
    private val yesterdayRegex = Regex("""\byesterday\b""", RegexOption.IGNORE_CASE)
    private val inDaysRegex = Regex("""\bin\s+(\d+)\s*(?:days|day)\b""", RegexOption.IGNORE_CASE)
    private val dayOfWeekRegex = Regex(
        """\b(?:(next|this|on)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\b""",
        RegexOption.IGNORE_CASE
    )

    // Time regexes
    private val relativeTimeOffsetRegex = Regex(
        """\bin\s+(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)\b""",
        RegexOption.IGNORE_CASE
    )
    private val relativeHourPhraseRegex = Regex(
        """\bin\s+(?:an?|one)\s+(?:hour|hr|h)\b""",
        RegexOption.IGNORE_CASE
    )
    private val relativeHalfHourPhraseRegex = Regex(
        """\bin\s+half\s+an?\s+(?:hour|hr|h)\b""",
        RegexOption.IGNORE_CASE
    )

    private val timeAmPmRegex = Regex(
        """\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""",
        RegexOption.IGNORE_CASE
    )
    private val time24Regex = Regex(
        """\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""",
        RegexOption.IGNORE_CASE
    )

    // Named time periods
    private val noonRegex = Regex("""\b(?:at\s+)?noon\b""", RegexOption.IGNORE_CASE)
    private val midnightRegex = Regex("""\b(?:at\s+)?midnight\b""", RegexOption.IGNORE_CASE)
    private val tonightTimeRegex = Regex("""\b(?:at\s+)?tonight\b|\bat\s+night\b""", RegexOption.IGNORE_CASE)
    private val morningPeriodRegex = Regex("""\b(?:in\s+the|this)\s+morning\b""", RegexOption.IGNORE_CASE)
    private val afternoonPeriodRegex = Regex("""\b(?:in\s+the|this)\s+afternoon\b""", RegexOption.IGNORE_CASE)
    private val eveningPeriodRegex = Regex("""\b(?:in\s+the|this)\s+evening\b""", RegexOption.IGNORE_CASE)

    /**
     * Resolves natural language date and time tokens into ISO-8601 strings.
     */
    fun parse(
        text: String,
        referenceDateTime: LocalDateTime = LocalDateTime.now()
    ): Result {
        val referenceDate = referenceDateTime.toLocalDate()
        var resolvedDate: String? = null
        var dateToken: String? = null

        // 1. Resolve explicit or relative date
        val isoMatch = isoDateRegex.find(text)
        val validIsoDate = isoMatch?.let { match ->
            runCatching { LocalDate.parse(match.value) }.getOrNull()
        }
        val dayAfterTomorrowMatch = dayAfterTomorrowRegex.find(text)
        val tomorrowMatch = tomorrowRegex.find(text)
        val todayMatch = todayRegex.find(text)
        val tonightDateMatch = tonightDateRegex.find(text)
        val yesterdayMatch = yesterdayRegex.find(text)
        val inDaysMatch = inDaysRegex.find(text)
        val dayOfWeekMatch = dayOfWeekRegex.find(text)

        when {
            validIsoDate != null -> {
                resolvedDate = validIsoDate.toString()
                dateToken = isoMatch!!.value
            }
            dayAfterTomorrowMatch != null -> {
                resolvedDate = runCatching { referenceDate.plusDays(2).toString() }.getOrNull()
                dateToken = dayAfterTomorrowMatch.value
            }
            tomorrowMatch != null -> {
                resolvedDate = runCatching { referenceDate.plusDays(1).toString() }.getOrNull()
                dateToken = tomorrowMatch.value
            }
            tonightDateMatch != null -> {
                resolvedDate = referenceDate.toString()
                dateToken = tonightDateMatch.value
            }
            todayMatch != null -> {
                resolvedDate = referenceDate.toString()
                dateToken = todayMatch.value
            }
            yesterdayMatch != null -> {
                resolvedDate = runCatching { referenceDate.minusDays(1).toString() }.getOrNull()
                dateToken = yesterdayMatch.value
            }
            inDaysMatch != null -> {
                val days = inDaysMatch.groupValues[1].toLongOrNull() ?: 0L
                resolvedDate = runCatching { referenceDate.plusDays(days).toString() }.getOrNull()
                dateToken = inDaysMatch.value
            }
            dayOfWeekMatch != null -> {
                val modifier = dayOfWeekMatch.groupValues[1].lowercase(Locale.ROOT)
                val dayStr = dayOfWeekMatch.groupValues[2]
                val dayOfWeek = mapDayOfWeek(dayStr)
                if (dayOfWeek != null) {
                    val targetDate = runCatching {
                        if (modifier == "this") {
                            referenceDate.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                        } else {
                            // "next monday", "on monday", or bare "monday" resolve via TemporalAdjusters.next
                            referenceDate.with(TemporalAdjusters.next(dayOfWeek))
                        }
                    }.getOrNull()
                    resolvedDate = targetDate?.toString()
                    dateToken = dayOfWeekMatch.value
                }
            }
        }

        // 2. Resolve Time
        var resolvedTime: String? = null
        var timeToken: String? = null
        var isInferredDate = false

        // Check relative time offsets first ("in 30 minutes", "in 2 hours", "in half an hour")
        val relOffsetMatch = relativeTimeOffsetRegex.find(text)
        val relHourMatch = relativeHourPhraseRegex.find(text)
        val relHalfHourMatch = relativeHalfHourPhraseRegex.find(text)

        if (relOffsetMatch != null) {
            val amount = relOffsetMatch.groupValues[1].toLongOrNull() ?: 0L
            val unit = relOffsetMatch.groupValues[2].lowercase(Locale.ROOT)
            val offsetMinutes = if (unit.startsWith("h")) (amount * 60L).coerceAtMost(Long.MAX_VALUE / 2) else amount
            val targetDateTime = runCatching { referenceDateTime.plusMinutes(offsetMinutes) }.getOrNull()
            if (targetDateTime != null) {
                resolvedTime = targetDateTime.toLocalTime().format(timeFormat24)
                timeToken = relOffsetMatch.value
                if (resolvedDate == null) {
                    resolvedDate = targetDateTime.toLocalDate().toString()
                    isInferredDate = true
                }
            }
        } else if (relHourMatch != null) {
            val targetDateTime = runCatching { referenceDateTime.plusHours(1L) }.getOrNull()
            if (targetDateTime != null) {
                resolvedTime = targetDateTime.toLocalTime().format(timeFormat24)
                timeToken = relHourMatch.value
                if (resolvedDate == null) {
                    resolvedDate = targetDateTime.toLocalDate().toString()
                    isInferredDate = true
                }
            }
        } else if (relHalfHourMatch != null) {
            val targetDateTime = runCatching { referenceDateTime.plusMinutes(30L) }.getOrNull()
            if (targetDateTime != null) {
                resolvedTime = targetDateTime.toLocalTime().format(timeFormat24)
                timeToken = relHalfHourMatch.value
                if (resolvedDate == null) {
                    resolvedDate = targetDateTime.toLocalDate().toString()
                    isInferredDate = true
                }
            }
        } else {
            // Check absolute 12-hour AM/PM
            val amPmMatch = timeAmPmRegex.find(text)
            val time24Match = time24Regex.find(text)

            if (amPmMatch != null) {
                val hour = amPmMatch.groupValues[1].toIntOrNull() ?: 0
                val minute = amPmMatch.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
                val period = amPmMatch.groupValues[3].lowercase(Locale.ROOT)
                val normalizedHour = when {
                    period == "pm" && hour < 12 -> hour + 12
                    period == "am" && hour == 12 -> 0
                    else -> hour
                }
                resolvedTime = String.format(Locale.ROOT, "%02d:%02d", normalizedHour, minute)
                timeToken = amPmMatch.value
            } else if (time24Match != null) {
                val hour = time24Match.groupValues[1].toIntOrNull() ?: 0
                val minute = time24Match.groupValues[2].toIntOrNull() ?: 0
                resolvedTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                timeToken = time24Match.value
            } else {
                // Check named periods
                val noonMatch = noonRegex.find(text)
                val midnightMatch = midnightRegex.find(text)
                val tonightTimeMatch = tonightTimeRegex.find(text)
                val morningMatch = morningPeriodRegex.find(text)
                val afternoonMatch = afternoonPeriodRegex.find(text)
                val eveningMatch = eveningPeriodRegex.find(text)

                when {
                    noonMatch != null -> {
                        resolvedTime = "12:00"
                        timeToken = noonMatch.value
                    }
                    midnightMatch != null -> {
                        resolvedTime = "00:00"
                        timeToken = midnightMatch.value
                    }
                    tonightTimeMatch != null -> {
                        resolvedTime = "20:00"
                        timeToken = tonightTimeMatch.value
                    }
                    morningMatch != null -> {
                        resolvedTime = "09:00"
                        timeToken = morningMatch.value
                        if (morningMatch.value.contains("this", ignoreCase = true) && resolvedDate == null) {
                            resolvedDate = referenceDate.toString()
                            isInferredDate = true
                        }
                    }
                    afternoonMatch != null -> {
                        resolvedTime = "14:00"
                        timeToken = afternoonMatch.value
                        if (afternoonMatch.value.contains("this", ignoreCase = true) && resolvedDate == null) {
                            resolvedDate = referenceDate.toString()
                            isInferredDate = true
                        }
                    }
                    eveningMatch != null -> {
                        resolvedTime = "19:00"
                        timeToken = eveningMatch.value
                        if (eveningMatch.value.contains("this", ignoreCase = true) && resolvedDate == null) {
                            resolvedDate = referenceDate.toString()
                            isInferredDate = true
                        }
                    }
                }
            }
        }

        // If "tonight" was used as date and no time was set, default time to 20:00
        if (tonightDateMatch != null && resolvedTime == null) {
            resolvedTime = "20:00"
        }

        return Result(
            date = resolvedDate,
            time = resolvedTime,
            dateToken = dateToken,
            timeToken = timeToken,
            isInferredDate = isInferredDate
        )
    }

    fun parse(
        text: String,
        referenceDate: LocalDate,
        referenceTime: LocalTime = LocalTime.now()
    ): Result = parse(text, LocalDateTime.of(referenceDate, referenceTime))

    fun parseDate(
        text: String,
        referenceDate: LocalDate = LocalDate.now()
    ): String? = parse(text, LocalDateTime.of(referenceDate, LocalTime.MIDNIGHT)).date

    fun parseTime(
        text: String,
        referenceDateTime: LocalDateTime = LocalDateTime.now()
    ): String? = parse(text, referenceDateTime).time

    fun parseTime(
        text: String,
        referenceTime: LocalTime,
        referenceDate: LocalDate = LocalDate.now()
    ): String? = parse(text, LocalDateTime.of(referenceDate, referenceTime)).time

    private fun mapDayOfWeek(name: String): DayOfWeek? {
        return when (name.lowercase(Locale.ROOT)) {
            "monday", "mon" -> DayOfWeek.MONDAY
            "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY
            "wednesday", "wed" -> DayOfWeek.WEDNESDAY
            "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY
            "friday", "fri" -> DayOfWeek.FRIDAY
            "saturday", "sat" -> DayOfWeek.SATURDAY
            "sunday", "sun" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}
