package com.personaltracker.domain.parser

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure Kotlin deterministic regex and token offline NLP command parser (FR-7, AD-1, AD-8).
 *
 * Extracts intent, title, date, time, duration, and urgency with calibrated 3-tier confidence scoring:
 * - HIGH (>= 0.85): Instant commit with reversible undo toast
 * - MEDIUM (0.50..0.85): 1-tap confirmation chips
 * - LOW (< 0.50): Saved to unfiled inbox triage
 *
 * Guarantees execution in < 300ms without Android framework dependencies or cloud connectivity.
 */
class DeterministicNlpCommandParser {

    private val timeFormat24 = DateTimeFormatter.ofPattern("HH:mm")

    // Action verbs identifying CREATE_TASK intent
    private val taskActionVerbs = listOf(
        "add", "task", "study", "read", "write", "finish", "complete", "buy", "review",
        "prepare", "work", "email", "call", "send", "practice", "do", "clean", "organize", "remind"
    )

    // Keywords identifying HABIT intent
    private val habitKeywords = listOf("habit", "log habit", "track habit", "drank water")

    // Keywords identifying FOCUS intent
    private val focusKeywords = listOf("start focus", "focus session", "focus", "pomodoro")

    // Regex patterns
    private val intensityRegex = Regex("""\b(urgent|asap|critical|important|priority|routine)\b""", RegexOption.IGNORE_CASE)
    private val durationRegex = Regex("""\b(?:for\s+)?(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)\b""", RegexOption.IGNORE_CASE)
    private val timeAmPmRegex = Regex("""\b(?:at\s+)?(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val time24Regex = Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)
    private val dateRelativeRegex = Regex("""\b(today|tomorrow|tonight|day after tomorrow)\b""", RegexOption.IGNORE_CASE)

    /**
     * Parses a raw text command into a structured ParsedCommand with calibrated confidence.
     *
     * @param rawText Natural language speech transcript or typed text
     * @param referenceDate Base date for relative date resolution (defaults to today)
     */
    fun parse(
        rawText: String,
        referenceDate: LocalDate = LocalDate.now()
    ): ParsedCommand {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return ParsedCommand(
                rawText = rawText,
                intent = CommandIntent.UNKNOWN,
                title = "",
                confidence = 0.0f,
                tier = ConfidenceTier.LOW
            )
        }

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Extract Intensity
        val intensityMatch = intensityRegex.find(lower)
        val deliveryIntensity = when (intensityMatch?.value?.lowercase(Locale.ROOT)) {
            "urgent", "asap", "critical" -> "URGENT"
            "important", "priority" -> "IMPORTANT"
            else -> "ROUTINE"
        }
        val hasExplicitIntensity = intensityMatch != null

        // 2. Extract Duration in minutes
        var durationMinutes: Int? = null
        val durationMatch = durationRegex.find(lower)
        if (durationMatch != null) {
            val amount = durationMatch.groupValues[1].toIntOrNull() ?: 0
            val unit = durationMatch.groupValues[2].lowercase(Locale.ROOT)
            durationMinutes = if (unit.startsWith("h")) {
                amount * 60
            } else {
                amount
            }
        }

        // 3. Extract Time ("HH:mm")
        var timeStr: String? = null
        val timeAmPmMatch = timeAmPmRegex.find(lower)
        val time24Match = if (timeAmPmMatch == null) time24Regex.find(lower) else null
        if (timeAmPmMatch != null) {
            val hour = timeAmPmMatch.groupValues[1].toIntOrNull() ?: 0
            val minute = timeAmPmMatch.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            val period = timeAmPmMatch.groupValues[3].lowercase(Locale.ROOT)
            val normalizedHour = when {
                period == "pm" && hour < 12 -> hour + 12
                period == "am" && hour == 12 -> 0
                else -> hour
            }
            timeStr = String.format(Locale.ROOT, "%02d:%02d", normalizedHour, minute)
        } else if (time24Match != null) {
            val hour = time24Match.groupValues[1].toIntOrNull() ?: 0
            val minute = time24Match.groupValues[2].toIntOrNull() ?: 0
            timeStr = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
        }

        // 4. Extract Date ("YYYY-MM-DD")
        var dateStr: String? = null
        val dateMatch = dateRelativeRegex.find(lower)
        if (dateMatch != null) {
            dateStr = when (dateMatch.value.lowercase(Locale.ROOT)) {
                "tomorrow" -> referenceDate.plusDays(1).toString()
                "day after tomorrow" -> referenceDate.plusDays(2).toString()
                "tonight", "today" -> referenceDate.toString()
                else -> referenceDate.toString()
            }
            if (dateMatch.value.equals("tonight", ignoreCase = true) && timeStr == null) {
                timeStr = "20:00"
            }
        }

        // 5. Detect Intent
        val intent = when {
            habitKeywords.any { lower.contains(it) } -> CommandIntent.LOG_HABIT
            focusKeywords.any { lower.contains(it) } -> CommandIntent.START_FOCUS
            taskActionVerbs.any { verb ->
                lower.startsWith(verb) || Regex("""\b$verb\b""").containsMatchIn(lower)
            } -> CommandIntent.CREATE_TASK
            else -> CommandIntent.UNKNOWN
        }

        // 6. Clean and Extract Title
        var cleaned = trimmed
        if (intensityMatch != null) {
            cleaned = cleaned.replace(Regex("""\b${Regex.escape(intensityMatch.value)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (durationMatch != null) {
            cleaned = cleaned.replace(Regex("""\b(?:for\s+)?${Regex.escape(durationMatch.value)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (timeAmPmMatch != null) {
            cleaned = cleaned.replace(Regex("""\b(?:at\s+)?${Regex.escape(timeAmPmMatch.value)}\b""", RegexOption.IGNORE_CASE), "")
        } else if (time24Match != null) {
            cleaned = cleaned.replace(Regex("""\b(?:at\s+)?${Regex.escape(time24Match.value)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (dateMatch != null) {
            cleaned = cleaned.replace(Regex("""\b${Regex.escape(dateMatch.value)}\b""", RegexOption.IGNORE_CASE), "")
        }

        // Remove prefix tokens like "add", "remind me to", "schedule", "task"
        cleaned = cleaned.replace(Regex("""^(?:remind me to|add\s+task|schedule|add|task)\s+""", RegexOption.IGNORE_CASE), "")
        // Remove habit prefixes if any
        cleaned = cleaned.replace(Regex("""^(?:log\s+habit|track\s+habit|habit)\s+""", RegexOption.IGNORE_CASE), "")
        // Clean leftover trailing prepositions
        cleaned = cleaned.replace(Regex("""\s+(?:for|at|on|by|in)\s*$""", RegexOption.IGNORE_CASE), "")
        // Collapse multiple whitespace gaps into a single space
        cleaned = cleaned.replace(Regex("""\s+"""), " ")
        cleaned = cleaned.trim().trim(',', '.', '!', '?').trim()

        val title = if (cleaned.isNotBlank()) cleaned else trimmed

        // 7. Calculate Calibrated Confidence Score
        var confidence = 0.0f

        if (intent == CommandIntent.UNKNOWN) {
            confidence = 0.25f
        } else {
            // Recognized intent with a title of reasonable length
            if (title.length >= 3) {
                confidence += 0.50f
            } else {
                confidence += 0.20f
            }

            if (dateStr != null) confidence += 0.15f
            if (timeStr != null) confidence += 0.15f
            if (durationMinutes != null) confidence += 0.10f
            if (hasExplicitIntensity) confidence += 0.05f
        }

        confidence = confidence.coerceIn(0.0f, 1.0f)

        // AD-8 exact boundary tiers: High (>0.85), Medium (0.50..0.85), Low (<0.50)
        val tier = when {
            confidence > 0.85f -> ConfidenceTier.HIGH
            confidence >= 0.50f -> ConfidenceTier.MEDIUM
            else -> ConfidenceTier.LOW
        }

        return ParsedCommand(
            rawText = rawText,
            intent = intent,
            title = title,
            date = dateStr,
            time = timeStr,
            durationMinutes = durationMinutes,
            deliveryIntensity = deliveryIntensity,
            confidence = confidence,
            tier = tier
        )
    }
}
