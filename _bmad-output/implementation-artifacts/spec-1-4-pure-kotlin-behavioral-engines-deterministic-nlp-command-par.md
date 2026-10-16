---
title: 'Story 1.4: Pure Kotlin Behavioral Engines & Deterministic NLP Command Parser'
type: 'feature'
created: '2026-10-16'
status: 'done'
baseline_commit: 'c943b7e4947767dab93670da57319b6783eb1581'
review_loop_iteration: 0
context:
  - ARCHITECTURE-SPINE.md
  - _bmad-output/project-context.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app requires complex behavioral, scheduling, streak, gamification, and distraction heuristics along with offline speech command parsing. Without dedicated pure Kotlin engines and use cases, business logic would leak into Android UI or platform components, violating AD-1, AD-3, and AD-4, causing untestable code and risking punitive streak resets or corrupted gamification states.

**Approach:** Implement 5 deterministic, stateless behavioral engines (`AdaptiveScheduleEngine`, `StreakStateMachine`, `GamificationLedgerEngine`, `FocusRescheduleEngine`, `InterventionRuleEngine`) and `DeterministicNlpCommandParser` in `com.personaltracker.domain.engine` and `com.personaltracker.domain.parser` with zero Android imports. Orchestrate them with clean domain use cases wired via Hilt `DomainEngineModule`, verified by 100% JVM unit tests.

## Boundaries & Constraints

**Always:**
- Strictly preserve AD-1 & AD-3: 100% pure Kotlin in `com.personaltracker.domain` (`kotlin.*`, `kotlinx.coroutines.*`, `java.time.*`). Absolutely zero imports from `android.*` or `androidx.*`.
- Strictly preserve AD-4: Non-punitive streak tracking with slack banking and freeze preservation (no zero reset on protected missed days). Companion pet state machine contains zero HP loss, starvation, or death mechanics.
- Strictly preserve AD-4 & AD-6: All gamification coin payouts must generate deterministic compound idempotency keys in `{DOMAIN}_{ACTION}_{ID}` format.
- Strictly preserve AD-8: 3-tier confidence scoring for NLP parsing: High (>0.85) auto-commit with undo, Medium (0.50..0.85) confirmation chips, Low (<0.50) unfiled inbox triage.
- All engine and use case unit tests must execute on standard JVM without Robolectric or emulator dependencies.

**Ask First:**
- Any change to existing Room entities, DAOs, or repository interfaces defined in Story 1.3.
- Any modification to UI theme tokens or navigation contracts.

**Never:**
- Never import Android framework classes (`android.*`, `androidx.*`) into `com.personaltracker.domain`.
- Never allow coin duplicate generation or non-deterministic coin transaction IDs.
- Never write guilt-inducing or punitive copy in engine status messages or companion mood transitions.
- Never use KAPT; use KSP only.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Active DayType Resolution | Date with user override + list of default templates | Returns user override template over day-of-week default | Falls back to default template if override ID missing |
| Sequential Routine Recalculation | Routine with 4 steps, delay start time shifted by 30 min, 1 step completed | Completed step unchanged; remaining 3 steps shifted sequentially forward | Handles empty steps gracefully |
| Streak Miss with Slack Bank | Habit with streak=10, slackBankDays=2, log=MISSED | Streak preserved at 10, slackBankDays decremented to 1, status SLACK_USED | If slack=0 and freeze=false, resets to 0 |
| Streak Miss with Freeze Active | Habit with streak=10, isFreezeActive=true, log=MISSED | Streak preserved at 10, isFreezeActive consumed, status FROZEN | Preserves bestStreak always |
| 4-State Flame Transition | Current streak count (0, 2, 5, 14) | COLD (0), WARMING (1..2), BURNING (3..6), BLAZING (7+) | Non-negative integer validation |
| Coin Reward & Idempotency Key | Task completion (taskId="t123", intensity="URGENT") | Coins=10, idempotencyKey="TASK_COMP_t123", companion XP increased | Deterministic key identical on re-calculation |
| Companion Mood Progression | Habit completed, streak milestone, or active focus | Expression ordinal updated (2=CELEBRATING, 4=FOCUSED, etc.); zero HP loss | Never enters sad/dead states |
| Focus Break Overrun | Planned 5m break, overrun to 15m in 25m session (10m elapsed) | Remaining minutes=15, adjusted end time shifted by 10m overrun | Non-punitive calm adjustment |
| Distraction Intervention Cooldown | Blocked app opened within 5m cooldown window | JITAI prompt suppressed to avoid nagging | Fires prompt once when cooldown expires |
| NLP Parsing - High Confidence | "Study algorithms for 45 minutes at 4pm tomorrow urgent" | Intent=CREATE_TASK, Title="Study algorithms", Duration=45, Time="16:00", Intensity=URGENT, Confidence > 0.85 | Clean token extraction |
| NLP Parsing - Medium Confidence | "Read book tomorrow" | Intent=CREATE_TASK, Title="Read book", Date tomorrow, Time=null, Confidence in 0.50..0.85 | Emits confirmation chips payload |
| NLP Parsing - Low Confidence | "mumble gibberish random noises" | Confidence < 0.50, routed to UnfiledInboxItem payload | Draft json captured without crash |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/engine/AdaptiveScheduleEngine.kt` -- DayType hierarchy resolution, routine duration scaling, and mid-day sequential timeline recalculation.
- `app/src/main/java/com/personaltracker/domain/engine/StreakStateMachine.kt` -- 4-state flame transitions (`COLD`, `WARMING`, `BURNING`, `BLAZING`), slack banking, freeze grace, and non-punitive streak updates.
- `app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt` -- Coin reward calculation, `{DOMAIN}_{ACTION}_{ID}` idempotency keys, companion XP/leveling, and mood state transitions with 0 HP loss.
- `app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt` -- Study session pause/resume recalculation, break overrun adjustments, and non-punitive schedule shifting.
- `app/src/main/java/com/personaltracker/domain/engine/InterventionRuleEngine.kt` -- JITAI distraction evaluation, cooldown tracking, and urgency tier categorization.
- `app/src/main/java/com/personaltracker/domain/parser/DeterministicNlpCommandParser.kt` -- Regex/token NLP parser extracting intent, title, date, time, duration, and urgency with calibrated confidence scoring.
- `app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt` -- Pure Kotlin DTOs for parsed command proposals and confidence tiers (`HIGH`, `MEDIUM`, `LOW`).
- `app/src/main/java/com/personaltracker/domain/usecase/ResolveDayTypeUseCase.kt` -- Interactor resolving today's active DayType using repository and engine.
- `app/src/main/java/com/personaltracker/domain/usecase/CompleteTaskUseCase.kt` -- Interactor completing task, generating coin transaction, and rewarding companion.
- `app/src/main/java/com/personaltracker/domain/usecase/LogHabitUseCase.kt` -- Interactor logging habit execution, evaluating streak state machine, and rewarding coins.
- `app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt` -- Interactor routing voice/text captures based on 3-tier NLP confidence.
- `app/src/main/java/com/personaltracker/domain/usecase/EvaluateInterventionUseCase.kt` -- Interactor evaluating foreground package distraction rules and JITAI nudges.
- `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Hilt DI module providing singleton pure Kotlin behavioral engines.
- `app/src/test/java/com/personaltracker/domain/engine/AdaptiveScheduleEngineTest.kt` -- Unit tests for DayType resolution, scaling, and routine shifting.
- `app/src/test/java/com/personaltracker/domain/engine/StreakStateMachineTest.kt` -- Unit tests for 4-state flame transitions, slack preservation, and freeze rules.
- `app/src/test/java/com/personaltracker/domain/engine/GamificationLedgerEngineTest.kt` -- Unit tests for reward payouts, idempotency keys, and companion leveling.
- `app/src/test/java/com/personaltracker/domain/engine/FocusRescheduleEngineTest.kt` -- Unit tests for pause, interruption, and break overrun recovery.
- `app/src/test/java/com/personaltracker/domain/engine/InterventionRuleEngineTest.kt` -- Unit tests for distraction rule cooldowns and urgency tiers.
- `app/src/test/java/com/personaltracker/domain/parser/DeterministicNlpCommandParserTest.kt` -- Unit tests for high, medium, and low confidence offline NLP command parsing.
- `app/src/test/java/com/personaltracker/domain/usecase/DomainUseCasesTest.kt` -- Unit tests verifying domain use case orchestration with mock/fake repositories.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/engine/AdaptiveScheduleEngine.kt` -- Implement DayType resolution (override > day-of-week > default), routine duration scaling, and mid-day sequential timeline recalculation.
- [x] `app/src/main/java/com/personaltracker/domain/engine/StreakStateMachine.kt` -- Implement 4-state flame model (`COLD`, `WARMING`, `BURNING`, `BLAZING`) and non-punitive slack-banking / freeze-day streak preservation.
- [x] `app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt` -- Implement coin reward math, deterministic `{DOMAIN}_{ACTION}_{ID}` idempotency keys, companion XP/leveling, and non-punitive mood state machine.
- [x] `app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt` -- Implement pause, interruption, and break-overrun schedule recalculation without guilt copy.
- [x] `app/src/main/java/com/personaltracker/domain/engine/InterventionRuleEngine.kt` -- Implement distraction rule evaluation, single-prompt JITAI cooldowns, and urgency tiers.
- [x] `app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt` & `DeterministicNlpCommandParser.kt` -- Implement deterministic offline regex/token NLP parsing with 3-tier confidence scoring (`High >= 0.85`, `Medium 0.50..0.85`, `Low < 0.50`).
- [x] `app/src/main/java/com/personaltracker/domain/usecase/` -- Implement `ResolveDayTypeUseCase`, `CompleteTaskUseCase`, `LogHabitUseCase`, `CaptureQuickThoughtUseCase`, and `EvaluateInterventionUseCase`.
- [x] `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Wire all engines and parser as `@Singleton` bindings in Hilt DI.
- [x] `app/src/test/java/com/personaltracker/domain/` -- Implement comprehensive JVM unit tests for all 5 engines, NLP parser tiers, and use cases.

### Review Findings

- [x] [Review][Patch] Prevent midnight cross-day overflow in sequential routine recalculation by clamping step start/end times at 23:59 day boundary [`AdaptiveScheduleEngine.kt:89`]
- [x] [Review][Patch] Prevent midnight cross-day overflow in study session break overrun and pause recalculation by clamping adjusted end time at 23:59 with calm non-punitive advisory copy [`FocusRescheduleEngine.kt:44`]
- [x] [Review][Patch] Prevent zero-streak miss from wasting active freeze grace or decrementing slack bank days when no active streak exists [`StreakStateMachine.kt:74`]
- [x] [Review][Patch] Implement multi-day gap evaluation across missed days with ordered freeze and slack bank consumption [`StreakStateMachine.kt:84`]
- [x] [Review][Patch] Align NLP confidence tier boundaries with AD-8 invariant: High is strictly >0.85, Medium is 0.50..0.85, Low is <0.50 [`DeterministicNlpCommandParser.kt:180`]
- [x] [Review][Patch] Tighten 12h AM/PM time regex to valid hour range (1..12) and minute range (00..59), collapse multi-space gaps, and strip trailing prepositions [`DeterministicNlpCommandParser.kt:37`]
- [x] [Review][Patch] Return valid deterministic `{DOMAIN}_{ACTION}_{ID}` idempotency key on already completed tasks and validate non-blank taskId and positive completionTimestamp [`CompleteTaskUseCase.kt:33`]
- [x] [Review][Patch] Add integer overflow protection and non-negative sanitization to companion XP progression and leveling [`GamificationLedgerEngine.kt:151`]
- [x] [Review][Patch] Validate logDate ISO-8601 format and non-blank input in habit logging [`LogHabitUseCase.kt:42`]
- [x] [Review][Patch] Protect against NoSuchElementException in flow collection with firstOrNull() fallback [`EvaluateInterventionUseCase.kt:21`, `ResolveDayTypeUseCase.kt:35`]

**Acceptance Criteria:**
- Given a date with a user override, when `AdaptiveScheduleEngine.resolveActiveDayType` is invoked, then the override DayType is selected over the day-of-week template.
- Given a habit with slackBankDays > 0, when a missed day is evaluated, then the streak is preserved, slackBankDays is decremented, and the state does not reset to 0.
- Given a habit streak of 0, 2, 5, and 14 days, when flame states are computed, then the states are COLD, WARMING, BURNING, and BLAZING respectively.
- Given a task completion, when `GamificationLedgerEngine` calculates payout, then a valid coin delta and unique `{DOMAIN}_{ACTION}_{ID}` key are generated without companion HP loss.
- Given natural language commands across high, medium, and low clarity, when `DeterministicNlpCommandParser` parses them, then commands route into High (>0.85), Medium (0.50..0.85), and Low (<0.50) confidence tiers deterministically in < 300ms.
- Given the entire `com.personaltracker.domain` package, then zero `android.*` or `androidx.*` imports exist.
- Given `.\gradlew test`, `.\gradlew lintDebug`, and `lint_spine.py`, then all verification passes with 0 errors.

## Spec Change Log

## Design Notes

- Flame threshold constants: `COLD = 0`, `WARMING = 1..2`, `BURNING = 3..6`, `BLAZING = 7+`.
- Companion moods: `0 = IDLE_CONTENT`, `1 = ENCOURAGING`, `2 = CELEBRATING`, `3 = CONCERNED`, `4 = FOCUSED`, `5 = SLEEPING`, `6 = EXCITED`. Strictly zero negative/punitive states.
- Idempotency key pattern: `{DOMAIN}_{ACTION}_{ID}` (e.g. `TASK_COMP_{id}`, `HABIT_LOG_{id}_{date}`, `STUDY_SESSION_{id}`, `ROUTINE_COMP_{id}_{date}`).

## Verification

**Commands:**
- `.\gradlew test` -- expected: BUILD SUCCESSFUL, all unit tests pass on JVM.
- `.\gradlew lintDebug` -- expected: BUILD SUCCESSFUL with 0 errors.
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: ok: true, 0 findings.

## Suggested Review Order

**Behavioral Engines**

- DayType template hierarchy, sequential timeline delay recalculation, and proportional scaling
  [`AdaptiveScheduleEngine.kt:28`](../../app/src/main/java/com/personaltracker/domain/engine/AdaptiveScheduleEngine.kt#L28)

- 4-state flame model, slack banking, and rest-day preservation with zero guilt
  [`StreakStateMachine.kt:22`](../../app/src/main/java/com/personaltracker/domain/engine/StreakStateMachine.kt#L22)

- Compound idempotency keys, reward payouts, and non-punitive companion pet leveling
  [`GamificationLedgerEngine.kt:29`](../../app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt#L29)

- Study session pause and break overrun recovery with calm advisory copy
  [`FocusRescheduleEngine.kt:22`](../../app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt#L22)

- Distraction rule evaluation, JITAI single-prompt cooldowns, and urgency scarcity guardrails
  [`InterventionRuleEngine.kt:20`](../../app/src/main/java/com/personaltracker/domain/engine/InterventionRuleEngine.kt#L20)

**Deterministic NLP Command Parser**

- Token & regex NLP parser with calibrated 3-tier confidence scoring
  [`DeterministicNlpCommandParser.kt:26`](../../app/src/main/java/com/personaltracker/domain/parser/DeterministicNlpCommandParser.kt#L26)

- Command proposals and confidence tiers DTO contracts
  [`ParsedCommand.kt:11`](../../app/src/main/java/com/personaltracker/domain/parser/ParsedCommand.kt#L11)

**Domain Use Cases & Hilt Wiring**

- Task completion interactor enforcing single-completion idempotency and pet leveling
  [`CompleteTaskUseCase.kt:18`](../../app/src/main/java/com/personaltracker/domain/usecase/CompleteTaskUseCase.kt#L18)

- Habit logging interactor orchestrating streak transitions and coin rewards
  [`LogHabitUseCase.kt:20`](../../app/src/main/java/com/personaltracker/domain/usecase/LogHabitUseCase.kt#L20)

- Quick thought capture interactor with 3-tier confidence routing
  [`CaptureQuickThoughtUseCase.kt:22`](../../app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt#L22)

- Dependency injection module binding singleton stateless behavioral engines
  [`DomainEngineModule.kt:18`](../../app/src/main/java/com/personaltracker/di/DomainEngineModule.kt#L18)

**JVM Verification Suite**

- Comprehensive unit tests covering all 5 engines, NLP parser, and use cases
  [`AdaptiveScheduleEngineTest.kt:18`](../../app/src/test/java/com/personaltracker/domain/engine/AdaptiveScheduleEngineTest.kt#L18)
  [`DeterministicNlpCommandParserTest.kt:15`](../../app/src/test/java/com/personaltracker/domain/parser/DeterministicNlpCommandParserTest.kt#L15)
  [`DomainUseCasesTest.kt:27`](../../app/src/test/java/com/personaltracker/domain/usecase/DomainUseCasesTest.kt#L27)

