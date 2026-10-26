---
title: 'Story 2.2: Deterministic Kotlin Regex & Rule NLP Parser'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: 'a08450e52781a0050290df6bd46d66a8baf1bf01'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Fast multimodal thought capture requires parsing raw speech transcripts and typed strings into structured commands (intent, title, date, time, duration, delivery intensity, routine shifts) deterministically in < 300ms on-device without cloud network calls, while guaranteeing strict privacy (AD-8) and providing calibrated 3-tier confidence routing (High $\ge 0.85$, Medium $0.50..0.84$, Low $< 0.50$).

**Approach:** Build a pure Kotlin deterministic NLP parser and relative date-time resolution engine (`DeterministicNlpCommandParser` and `RelativeDateTimeParser`) using regex and rule heuristics to extract commands (`CREATE_TASK`, `LOG_HABIT`, `START_FOCUS`, `SHIFT_ROUTINE`), resolve relative dates ("tomorrow", "next monday", "in 3 days") and relative times ("in 30 minutes", "at 4pm", "tonight"), and compute confidence scores 100% locally on-device.

## Boundaries & Constraints

**Always:**
- Keep all parser code pure Kotlin in `com.personaltracker.domain.parser` with zero Android framework imports (`android.*`, `androidx.*`) (AD-1).
- Execute parsing locally on-device with zero network or cloud API calls (AD-8).
- Ensure execution latency remains strictly < 300ms on `Dispatchers.Default` / standard JVM (NFR-1).
- Classify confidence into exact 3-tier thresholds: High ($\ge 0.85$), Medium ($0.50..0.84$), Low ($< 0.50$) (AD-8).
- Resolve dates to ISO-8601 calendar strings (`YYYY-MM-DD`) and times to 24-hour strings (`HH:mm`).
- Support deterministic testing by accepting explicit `referenceDate` (`LocalDate`) and `referenceTime` (`LocalTime`) or `referenceDateTime` (`LocalDateTime`) with system defaults.

**Ask First:**
- Modifying `ParsedCommand` fields in a way that breaks existing UI or use case contracts (`CaptureQuickThoughtUseCase`).
- Altering the confidence score tier threshold values ($\ge 0.85$, $0.50..0.84$, $< 0.50$).

**Never:**
- Make external network, telemetry, or remote LLM API calls during parsing (AD-8).
- Mutate database entities directly from within the parser layer (AD-1, ARCH-3).
- Import Android context, bundle, or OS-specific date formatters (AD-1).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| High Confidence Task with Date & Time | `"Remind me to review PR at 4pm tomorrow urgent"` (ref: 2026-10-26 10:00) | `intent = CREATE_TASK`, `title = "Review PR"`, `date = "2026-10-27"`, `time = "16:00"`, `deliveryIntensity = "URGENT"`, `confidence >= 0.85`, `tier = HIGH` | Clean title stripping |
| Relative Time Offset | `"Study physics in 30 minutes for 45 mins"` (ref: 2026-10-26 14:15) | `intent = CREATE_TASK`, `title = "Study physics"`, `date = "2026-10-26"`, `time = "14:45"`, `durationMinutes = 45`, `confidence >= 0.85`, `tier = HIGH` | Computes time via `LocalTime.plusMinutes` |
| Day of Week | `"Team sync next monday at 10am"` (ref: Mon 2026-10-26) | `intent = CREATE_TASK`, `title = "Team sync"`, `date = "2026-11-02"`, `time = "10:00"`, `confidence >= 0.85`, `tier = HIGH` | Resolves via `TemporalAdjusters.next` |
| Medium Confidence Partial Task | `"Buy groceries tomorrow"` (ref: 2026-10-26) | `intent = CREATE_TASK`, `title = "Buy groceries"`, `date = "2026-10-27"`, `time = null`, `confidence in 0.50f..0.84f`, `tier = MEDIUM` | Emits draft for 1-tap chip confirmation |
| Shift Routine Intent | `"Shift morning routine by 15 minutes"` | `intent = SHIFT_ROUTINE`, `title = "morning routine"`, `targetRoutineName = "morning"`, `shiftMinutes = 15`, `confidence >= 0.85`, `tier = HIGH` | Extracts routine name and shift minutes |
| Log Habit Intent | `"Log habit Drink 2L water"` | `intent = LOG_HABIT`, `title = "Drink 2L water"`, `confidence >= 0.85`, `tier = HIGH` | Strips habit keywords from title |
| Start Focus Intent | `"Start focus for 25 minutes"` | `intent = START_FOCUS`, `durationMinutes = 25`, `confidence >= 0.85`, `tier = HIGH` | Strips focus trigger tokens |
| Low Confidence Gibberish | `"mumble random background chatter"` | `intent = UNKNOWN`, `confidence < 0.50`, `tier = LOW` | Routes to unfiled inbox item triage |
| Blank Input | `"    "` | `intent = UNKNOWN`, `title = ""`, `confidence = 0.0f`, `tier = LOW` | Immediate empty low tier result |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt` -- Extends `CommandIntent` with `SHIFT_ROUTINE`; adds optional `shiftMinutes: Int?`, `targetRoutineName: String?`.
- `app/src/main/java/com/personaltracker/domain/parser/RelativeDateTimeParser.kt` -- Pure Kotlin utility resolving relative dates ("today", "tomorrow", "next monday", "in 3 days") and relative times ("in 30 minutes", "at 4pm", "tonight", "noon") using `java.time.*`.
- `app/src/main/java/com/personaltracker/domain/parser/DeterministicNlpCommandParser.kt` -- Offline regex & rule parser extracting intent, title, parameters, and calculating calibrated confidence score & tier (< 300ms).
- `app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt` -- Verified for compatibility with extended `ParsedCommand` and confidence tiers.
- `app/src/test/java/com/personaltracker/domain/parser/RelativeDateTimeParserTest.kt` -- Unit tests validating relative date/time edge cases, day-of-week roll-forwards, and time offsets.
- `app/src/test/java/com/personaltracker/domain/parser/DeterministicNlpCommandParserTest.kt` -- JVM unit tests asserting exact confidence boundaries, latency (< 300ms), and phrasings.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt` -- Add `SHIFT_ROUTINE` to `CommandIntent`; add `shiftMinutes: Int? = null` and `targetRoutineName: String? = null` to `ParsedCommand`.
- [x] `app/src/main/java/com/personaltracker/domain/parser/RelativeDateTimeParser.kt` -- Implement pure Kotlin relative date and time extraction ("today", "tomorrow", "day after tomorrow", "yesterday", days of week, "in X days", "in X mins/hrs", 12h/24h, named periods).
- [x] `app/src/main/java/com/personaltracker/domain/parser/DeterministicNlpCommandParser.kt` -- Integrate `RelativeDateTimeParser`, support `SHIFT_ROUTINE`, clean titles, and calibrate scoring to High ($\ge 0.85$), Medium ($0.50..0.84$), Low ($< 0.50$).
- [x] `app/src/test/java/com/personaltracker/domain/parser/RelativeDateTimeParserTest.kt` -- Create unit test suite verifying relative date and time parsing patterns against fixed reference clocks.
- [x] `app/src/test/java/com/personaltracker/domain/parser/DeterministicNlpCommandParserTest.kt` -- Expand unit tests to assert latency (< 300ms), exact tier boundaries (0.85, 0.50), `SHIFT_ROUTINE`, `LOG_HABIT`, `START_FOCUS`, relative time offsets, and gibberish fallback.

**Acceptance Criteria:**
- Given raw natural language transcripts, when `DeterministicNlpCommandParser.parse()` executes, then parsing completes in < 300ms on `Dispatchers.Default` with pure Kotlin and zero network calls.
- Given a command with exact intent and required parameters (e.g. `"Study algorithms for 45 minutes at 4pm tomorrow urgent"`), then confidence is $\ge 0.85$ and tier is `ConfidenceTier.HIGH`.
- Given a command with recognized intent but missing/ambiguous parameters (e.g. `"Buy groceries tomorrow"`), then confidence is in $0.50..0.84$ and tier is `ConfidenceTier.MEDIUM`.
- Given an unrecognized command or gibberish (e.g. `"random mumble chatter"`), then confidence is $< 0.50$ and tier is `ConfidenceTier.LOW`.
- Given relative date phrasings (`"next monday"`, `"in 3 days"`, `"tomorrow"`) and relative time phrasings (`"in 30 minutes"`, `"at 5pm"`), then dates and times resolve to exact ISO-8601 strings (`YYYY-MM-DD`) and 24-hour strings (`HH:mm`).
- Given routine shift phrases (e.g. `"Shift morning routine by 15 minutes"`), then intent is `CommandIntent.SHIFT_ROUTINE` with `targetRoutineName = "morning"` and `shiftMinutes = 15`.

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Design Notes

- **Exact Confidence Calibration:** Base intent + title $\ge 3$ chars: `+0.50`; explicit action keyword: `+0.10`; date: `+0.15`; time: `+0.15`; duration: `+0.10`; explicit intensity: `+0.05`. Clamped to `[0.0, 1.0]`. Tier mapping: `confidence >= 0.85f -> HIGH`, `confidence >= 0.50f -> MEDIUM`, `else -> LOW`.
- **Pure Domain Date Arithmetic:** Uses `java.time.LocalDate`, `LocalTime`, `LocalDateTime`, `DayOfWeek`, and `TemporalAdjusters`. Standard JVM compatible, zero Android framework imports.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `0 findings`
- `.\gradlew test --tests "com.personaltracker.domain.parser.*"` -- expected: All parser unit tests pass in < 300ms
- `.\gradlew test` -- expected: Complete JVM test suite passes cleanly
- `.\gradlew lintDebug` -- expected: 0 errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid


## Suggested Review Order

**Intent Classification & Scoring Engine**

- Primary entry point orchestrating rule heuristics and 3-tier confidence scoring
  [`DeterministicNlpCommandParser.kt:98`](../../app/src/main/java/com/personaltracker/domain/parser/DeterministicNlpCommandParser.kt#L98)

- Extended command intent enum and structured DTO fields
  [`ParsedCommand.kt:6`](../../app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt#L6)

**Relative Date & Time Resolution**

- Pure Kotlin temporal resolution using `TemporalAdjusters` and defensive arithmetic
  [`RelativeDateTimeParser.kt:82`](../../app/src/main/java/com/personaltracker/domain/parser/RelativeDateTimeParser.kt#L82)

**Test Verification & Edge Cases**

- Comprehensive intent, confidence boundary, and latency verification tests
  [`DeterministicNlpCommandParserTest.kt:33`](../../app/src/test/java/com/personaltracker/domain/parser/DeterministicNlpCommandParserTest.kt#L33)

- Temporal arithmetic, ISO validation, and weekday shift tests
  [`RelativeDateTimeParserTest.kt:31`](../../app/src/test/java/com/personaltracker/domain/parser/RelativeDateTimeParserTest.kt#L31)
