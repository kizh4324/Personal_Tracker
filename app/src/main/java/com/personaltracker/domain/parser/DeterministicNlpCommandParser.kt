package com.personaltracker.domain.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Pure Kotlin deterministic regex and token offline NLP command parser (FR-7, AD-1, AD-8).
 *
 * Extracts intent, title, date, time, duration, routine shifts, and urgency with calibrated
 * 3-tier confidence scoring:
 * - HIGH (>= 0.85): Instant commit with reversible undo toast
 * - MEDIUM (0.50..0.84): 1-tap confirmation chips
 * - LOW (< 0.50): Saved to unfiled inbox triage
 *
 * Guarantees execution in < 300ms without Android framework dependencies or cloud connectivity.
 */
class DeterministicNlpCommandParser(
    private val dateTimeParser: RelativeDateTimeParser = RelativeDateTimeParser()
) {

    // Action verbs identifying CREATE_TASK intent
    private val taskActionVerbs = listOf(
        "add", "task", "study", "read", "write", "finish", "complete", "buy", "review",
        "prepare", "work", "email", "call", "send", "practice", "do", "clean", "organize", "remind",
        "sync", "meet", "meeting", "exercise", "walk", "run", "pay", "submit", "check", "attend",
        "chat", "talk", "schedule", "plan", "cook", "watch", "listen"
    )

    // Prefixes identifying task intent
    private val taskPrefixRegex = Regex(
        """^(?:please\s+remind\s+me\s+to|remind\s+me\s+to|remind\s+me|i\s+need\s+to|need\s+to|add\s+(?:a\s+)?task\s+to|add\s+(?:a\s+)?task|create\s+(?:a\s+)?task|schedule\s+(?:a\s+)?task\s+to|schedule\s+(?:a\s+)?task|schedule|task\s+to|task|add)\s+""",
        RegexOption.IGNORE_CASE
    )

    // Keywords identifying HABIT intent
    private val habitKeywords = listOf("habit", "log habit", "track habit", "drank water")
    private val habitPrefixRegex = Regex(
        """^(?:log\s+habit|track\s+habit|log\s+a\s+habit|track\s+a\s+habit|habit|log)\s*""",
        RegexOption.IGNORE_CASE
    )

    // Keywords identifying FOCUS intent
    private val focusKeywords = listOf("start focus", "focus session", "focus", "pomodoro")
    private val focusPrefixRegex = Regex(
        """^(?:start\s+(?:a\s+)?focus\s+session(?:\s+(?:on|for))?|start\s+focus(?:\s+(?:on|for))?|focus(?:\s+session)?(?:\s+(?:on|for))?|pomodoro(?:\s+for)?)\s*""",
        RegexOption.IGNORE_CASE
    )

    // Regex patterns for routine shifts supporting multi-word names and hour durations
    private val routineShiftBareWithAmountRegex = Regex(
        """\b(?:shift|delay|push\s+back|move|adjust|reschedule)\s+(?:the\s+)?routine(?:\s+by)?\s+(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val routineShiftNamedBeforeWithAmountRegex = Regex(
        """\b(?:shift|delay|push\s+back|move|adjust|reschedule)\s+(?:the\s+)?([a-zA-Z0-9_ -]+?)\s+routine(?:\s+by)?\s+(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val routineShiftNamedAfterWithAmountRegex = Regex(
        """\b(?:shift|delay|push\s+back|move|adjust|reschedule)\s+(?:the\s+)?routine\s+(?!by\b)([a-zA-Z0-9_ -]+?)(?:\s+by)?\s+(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val routineShiftNamedNoAmountRegex = Regex(
        """\b(?:shift|delay|push\s+back|move|adjust|reschedule)\s+(?:the\s+)?([a-zA-Z0-9_ -]+?)\s+routine\b""",
        RegexOption.IGNORE_CASE
    )
    private val routineShiftBareNoAmountRegex = Regex(
        """\b(?:shift|delay|push\s+back|move|adjust|reschedule)\s+(?:the\s+)?routine\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex patterns for attributes
    private val intensityRegex = Regex("""\b(urgent|asap|critical|important|priority|routine)\b""", RegexOption.IGNORE_CASE)
    private val explicitForDurationRegex = Regex("""\bfor\s+(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)\b""", RegexOption.IGNORE_CASE)
    private val fallbackDurationRegex = Regex("""\b(\d+)\s*(minutes|minute|mins|min|m|hours|hour|hrs|hr|h)\b""", RegexOption.IGNORE_CASE)

    /**
     * Parses a raw text command into a structured ParsedCommand with calibrated confidence.
     *
     * @param rawText Natural language speech transcript or typed text
     * @param referenceDate Base date for relative date resolution (defaults to today)
     * @param referenceTime Base time for relative time offset resolution (defaults to now)
     */
    fun parse(
        rawText: String,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now()
    ): ParsedCommand = parseInternal(rawText, LocalDateTime.of(referenceDate, referenceTime))

    /**
     * Overload accepting an explicit reference DateTime for deterministic evaluation.
     */
    fun parse(
        rawText: String,
        referenceDateTime: LocalDateTime
    ): ParsedCommand = parseInternal(rawText, referenceDateTime)

    private fun parseInternal(
        rawText: String,
        referenceDateTime: LocalDateTime
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

        // =========================================================================
        // 1. Check for SHIFT_ROUTINE Intent
        // =========================================================================
        val bareWithAmount = routineShiftBareWithAmountRegex.find(trimmed)
        val namedBeforeWithAmount = if (bareWithAmount == null) routineShiftNamedBeforeWithAmountRegex.find(trimmed) else null
        val namedAfterWithAmount = if (bareWithAmount == null && namedBeforeWithAmount == null) routineShiftNamedAfterWithAmountRegex.find(trimmed) else null
        val namedNoAmount = if (bareWithAmount == null && namedBeforeWithAmount == null && namedAfterWithAmount == null) routineShiftNamedNoAmountRegex.find(trimmed) else null
        val bareNoAmount = if (bareWithAmount == null && namedBeforeWithAmount == null && namedAfterWithAmount == null && namedNoAmount == null) routineShiftBareNoAmountRegex.find(trimmed) else null

        if (bareWithAmount != null || namedBeforeWithAmount != null || namedAfterWithAmount != null || namedNoAmount != null || bareNoAmount != null) {
            val targetRoutineName: String?
            val shiftMinutes: Int?

            when {
                bareWithAmount != null -> {
                    targetRoutineName = null
                    shiftMinutes = parseShiftMinutes(bareWithAmount.groupValues[1], bareWithAmount.groupValues.getOrNull(2))
                }
                namedBeforeWithAmount != null -> {
                    val rawName = namedBeforeWithAmount.groupValues[1].trim()
                    targetRoutineName = rawName.takeUnless { it.equals("the", ignoreCase = true) || it.isBlank() }?.lowercase(Locale.ROOT)
                    shiftMinutes = parseShiftMinutes(namedBeforeWithAmount.groupValues[2], namedBeforeWithAmount.groupValues.getOrNull(3))
                }
                namedAfterWithAmount != null -> {
                    val rawName = namedAfterWithAmount.groupValues[1].trim()
                    targetRoutineName = rawName.takeUnless { it.equals("the", ignoreCase = true) || it.isBlank() }?.lowercase(Locale.ROOT)
                    shiftMinutes = parseShiftMinutes(namedAfterWithAmount.groupValues[2], namedAfterWithAmount.groupValues.getOrNull(3))
                }
                namedNoAmount != null -> {
                    val rawName = namedNoAmount.groupValues[1].trim()
                    targetRoutineName = rawName.takeUnless { it.equals("the", ignoreCase = true) || it.isBlank() }?.lowercase(Locale.ROOT)
                    shiftMinutes = null
                }
                else -> {
                    targetRoutineName = null
                    shiftMinutes = null
                }
            }

            val routineTitle = if (!targetRoutineName.isNullOrBlank()) {
                "$targetRoutineName routine"
            } else {
                "routine"
            }

            var routineConfidence = 0.50f // base intent recognized
            routineConfidence += 0.10f // explicit shift/delay action keyword
            if (!targetRoutineName.isNullOrBlank()) routineConfidence += 0.15f
            if (shiftMinutes != null) routineConfidence += 0.15f
            routineConfidence = routineConfidence.coerceIn(0.0f, 1.0f)

            val tier = when {
                routineConfidence >= 0.85f -> ConfidenceTier.HIGH
                routineConfidence >= 0.50f -> ConfidenceTier.MEDIUM
                else -> ConfidenceTier.LOW
            }

            return ParsedCommand(
                rawText = rawText,
                intent = CommandIntent.SHIFT_ROUTINE,
                title = routineTitle,
                confidence = routineConfidence,
                tier = tier,
                shiftMinutes = shiftMinutes,
                targetRoutineName = targetRoutineName
            )
        }

        // =========================================================================
        // 2. Extract Intensity
        // =========================================================================
        val intensityMatch = intensityRegex.find(lower)
        val deliveryIntensity = when (intensityMatch?.value?.lowercase(Locale.ROOT)) {
            "urgent", "asap", "critical" -> "URGENT"
            "important", "priority" -> "IMPORTANT"
            else -> "ROUTINE"
        }
        val hasExplicitIntensity = intensityMatch != null

        // =========================================================================
        // 3. Extract Date and Time via RelativeDateTimeParser
        // =========================================================================
        val dtResult = dateTimeParser.parse(trimmed, referenceDateTime)
        val dateStr = dtResult.date
        val timeStr = dtResult.time
        val dateToken = dtResult.dateToken
        val timeToken = dtResult.timeToken

        // =========================================================================
        // 4. Extract Duration (minutes)
        // Ensure duration does not conflict with matched time tokens
        // =========================================================================
        var durationMinutes: Int? = null
        var durationToken: String? = null

        val textWithoutTimeToken = if (timeToken != null) {
            trimmed.replace(timeToken, " ".repeat(timeToken.length))
        } else {
            trimmed
        }

        val forDurationMatch = explicitForDurationRegex.find(textWithoutTimeToken)
        val fallbackDurationMatch = if (forDurationMatch == null) {
            fallbackDurationRegex.find(textWithoutTimeToken)
        } else {
            null
        }
        val durationMatch = forDurationMatch ?: fallbackDurationMatch

        if (durationMatch != null) {
            val rawAmount = durationMatch.groupValues[1].toLongOrNull() ?: 0L
            val unit = durationMatch.groupValues[2].lowercase(Locale.ROOT)
            durationMinutes = if (unit.startsWith("h")) {
                (rawAmount * 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            } else {
                rawAmount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
            durationToken = durationMatch.value
        }

        // =========================================================================
        // 5. Detect Intent
        // =========================================================================
        val isHabit = habitKeywords.any { lower.contains(it) }
        val isFocus = focusKeywords.any { lower.contains(it) }
        val hasTaskActionVerb = taskActionVerbs.any { verb ->
            lower.startsWith(verb) || Regex("""\b$verb\b""").containsMatchIn(lower)
        }
        val hasTaskPrefix = taskPrefixRegex.containsMatchIn(trimmed)
        val hasExplicitTaskKeyword = hasTaskActionVerb || hasTaskPrefix

        val intent = when {
            isHabit -> CommandIntent.LOG_HABIT
            isFocus -> CommandIntent.START_FOCUS
            hasExplicitTaskKeyword || dateStr != null || timeStr != null || durationMinutes != null || hasExplicitIntensity -> {
                CommandIntent.CREATE_TASK
            }
            else -> CommandIntent.UNKNOWN
        }

        // =========================================================================
        // 6. Clean and Extract Title
        // =========================================================================
        var cleaned = trimmed

        if (intensityMatch != null) {
            cleaned = cleaned.replace(Regex("""\b${Regex.escape(intensityMatch.value)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (durationToken != null) {
            cleaned = cleaned.replace(Regex("""\b(?:for\s+)?${Regex.escape(durationToken)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (timeToken != null) {
            cleaned = cleaned.replace(Regex("""\b(?:at\s+)?${Regex.escape(timeToken)}\b""", RegexOption.IGNORE_CASE), "")
        }
        if (dateToken != null && !dtResult.isInferredDate) {
            cleaned = cleaned.replace(Regex("""\b${Regex.escape(dateToken)}\b""", RegexOption.IGNORE_CASE), "")
        }

        // Trim leading spaces so leading tokens (like "Tomorrow" or "URGENT") removed above
        // do not leave whitespace that prevents ^ from matching
        cleaned = cleaned.trimStart()

        // Remove intent-specific trigger prefixes
        when (intent) {
            CommandIntent.LOG_HABIT -> {
                cleaned = cleaned.replace(habitPrefixRegex, "")
            }
            CommandIntent.START_FOCUS -> {
                cleaned = cleaned.replace(focusPrefixRegex, "")
            }
            CommandIntent.CREATE_TASK -> {
                cleaned = cleaned.replace(taskPrefixRegex, "")
            }
            else -> { /* no prefix stripping */ }
        }

        // Remove leftover trailing prepositions and punctuation
        cleaned = cleaned.replace(Regex("""\s+(?:for|at|on|by|in|to)\s*$""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""\s+"""), " ")
        cleaned = cleaned.trim().trim(',', '.', '!', '?').trim()

        // Capitalize first character if lowercase
        if (cleaned.isNotBlank()) {
            cleaned = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        val title = when {
            cleaned.isNotBlank() -> cleaned
            intent == CommandIntent.START_FOCUS -> "Focus"
            else -> trimmed
        }

        // =========================================================================
        // 7. Calculate Calibrated Confidence Score
        // =========================================================================
        var confidence = 0.0f

        when (intent) {
            CommandIntent.UNKNOWN -> {
                confidence = 0.25f
            }
            CommandIntent.LOG_HABIT -> {
                if (title.length >= 3) {
                    confidence += 0.50f
                    confidence += 0.10f // explicit keyword
                    confidence += 0.25f // habit target specified
                } else {
                    confidence += 0.20f
                }
            }
            CommandIntent.START_FOCUS -> {
                confidence += 0.50f
                confidence += 0.10f // explicit focus keyword
                if (durationMinutes != null) {
                    confidence += 0.25f // required duration specified
                }
            }
            CommandIntent.CREATE_TASK -> {
                if (title.length >= 3) {
                    confidence += 0.50f
                } else {
                    confidence += 0.20f
                }
                if (hasExplicitTaskKeyword) confidence += 0.10f
                if (dateStr != null) confidence += 0.15f
                if (timeStr != null) confidence += 0.15f
                if (durationMinutes != null) confidence += 0.10f
                if (hasExplicitIntensity) confidence += 0.05f
            }
            CommandIntent.SHIFT_ROUTINE -> {
                // Handled earlier
            }
        }

        confidence = confidence.coerceIn(0.0f, 1.0f)

        // AD-8 exact boundary tiers: High (>= 0.85), Medium (0.50..0.84), Low (< 0.50)
        val tier = when {
            confidence >= 0.85f -> ConfidenceTier.HIGH
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

    private fun parseShiftMinutes(amountStr: String, unitStr: String?): Int {
        val rawAmount = amountStr.toLongOrNull() ?: 0L
        val unit = unitStr?.lowercase(Locale.ROOT) ?: ""
        return if (unit.startsWith("h")) {
            (rawAmount * 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else {
            rawAmount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
