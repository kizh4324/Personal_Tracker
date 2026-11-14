---
title: 'Story 4.1: Habit Entity Models, Cadence Tracking & Streak Grace Engine'
type: 'feature'
created: '2026-11-14'
status: 'done'
baseline_commit: '1c6d8bc9d1bc3fa5fe5bad6c529ca8b4de6f93b1'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/epic-4-context.md'
  - 'ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Personal productivity habit systems typically enforce rigid daily commitments with unforgiving zero-resets upon a single miss, triggering shame spirals and abandonment (punitive UX violating AD-4). Users cannot configure flexible weekly cadences or rest days without being unfairly penalized as "missed".

**Approach:** Implement pure Kotlin cadence domain models (`Daily`, `SpecificDays`, `WeeklyFrequency`), a non-punitive deterministic streak & grace state machine (`StreakStateMachine`) supporting 5 flame/protection states (`COLD`, `WARMING`, `BURNING`, `BLAZING`, `GRACE_SHIELD`), automatic grace-day absorption, off-cadence day immunity, and `LOW_CAPACITY`/`DayType.REST` immunity. Wire idempotent logging (`HABIT_LOG_{habitId}_{date}`), non-destructive Room v2->v3 migration preserving legacy `targetDaysOfWeek`, and MVI presentation across `HabitStreakCard` and dynamic `VerticalCapsuleBars`.

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** Zero `android.*` or `androidx.*` dependencies in `domain/` (`Habit.kt`, `HabitCadence.kt`, `HabitLog.kt`, `StreakStateMachine.kt`, `EvaluateHabitStreaksUseCase.kt`).
- **AD-3 Offline Encrypted Persistence:** Non-destructive Room migration `MIGRATION_2_3` adding cadence columns to `habits` table and migrating existing custom cadences to `SPECIFIC_DAYS`.
- **AD-4 Non-Punishing UX:** Zero guilt/shame copy in UI strings or notifications. Best streak is unconditionally preserved; missed days on off-cadence, rest, or low-capacity days never consume grace shields or decrement streaks.
- **AD-7 Design Token Conformance:** All composable styling strictly resolves from `PersonalTrackerTheme` tokens (0 raw hex/dp/sp literals, 48dp minimum touch target floor).
- **Compound Idempotency:** Habit coin rewards require deterministic idempotency key format `HABIT_LOG_{habitId}_{date}`.

**Ask First:**
- Altering the maximum banked grace days (`MAX_SLACK_BANK_DAYS = 3`) or slack accrual interval (`SLACK_EARN_CADENCE_DAYS = 7`).
- Changing database schema tables other than `habits`.

**Never:**
- Never allow Coins to purchase streak freezes in v1 (FR-8.5).
- Never penalize a streak on unscheduled weekdays or when active `DayType` is `LOW_CAPACITY` or `REST`.
- Never execute production code modifications during the design phase (EGM Elephant rule).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **Scheduled Day Completion** | Habit with streak 6, completed on scheduled day | Streak increments to 7 (`BLAZING`), slack banked (+1, cap 3), coins awarded (`+5`), key `HABIT_LOG_{habitId}_{date}` | Log already present with status `COMPLETED` -> No-op idempotent return |
| **Off-Cadence Unscheduled Day** | Habit scheduled for Mon/Wed/Fri evaluated on Tuesday (uncompleted) | Streak unchanged, 0 slack/freeze consumed, status `REST_DAY`, flame state preserved | N/A |
| **DayType Immunity** | Uncompleted habit on date with active `DayType.LOW_CAPACITY` or `DayType.REST` | Streak preserved, 0 slack consumed, status `DAY_TYPE_IMMUNITY` | If DayType missing, fallback to standard schedule |
| **Active Freeze Absorption** | Scheduled day missed, `isFreezeActive = true`, streak 5 | `isFreezeActive` consumed (`false`), streak preserved at 5, status `GRACE_SHIELD_ABSORBED`, flame `GRACE_SHIELD` | N/A |
| **Slack Bank Absorption** | Scheduled day missed, `isFreezeActive = false`, `slackBankDays = 2`, streak 8 | `slackBankDays` decremented to 1, streak preserved at 8, status `SLACK_USED`, flame `GRACE_SHIELD` | N/A |
| **Protections Exhausted** | Scheduled day missed, `isFreezeActive = false`, `slackBankDays = 0`, streak 10, best 12 | `currentStreak` resets to 0 (`COLD`), `bestStreak` preserved at 12, status `MISSED` | N/A |
| **WeeklyFrequency Mid-Week** | Habit with `WeeklyFrequency(3)` uncompleted on Wednesday rollover | Streak unchanged, 0 slack consumed, status `REST_DAY` (flexible mid-week evaluation) | N/A |
| **WeeklyFrequency Sunday Check** | `WeeklyFrequency(3)` evaluated on Sunday rollover with 3 completions logged | Streak preserved/incremented at week boundary, status `COMPLETED` | If completions < 3, absorbs grace/slack or marks `MISSED` |
| **Upgrade Prior Missed/Rest Log** | Day previously logged as `REST_DAY` or `MISSED`, user completes later today | Upgrades log status to `COMPLETED`, updates streak (+1), awards coins (+5) | N/A |
| **Duplicate Completion / Rapid Tap** | Concurrent or repeated completion toggled on same date | Detects existing log or duplicate idempotency key, returns existing state with 0 duplicate coins | Catches conflict gracefully, suppresses UI error |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/HabitCadence.kt` -- Pure Kotlin cadence hierarchy: `CadenceType` enum (`DAILY`, `SPECIFIC_DAYS`, `WEEKLY_FREQUENCY`), sealed `HabitCadence` (`Daily`, `SpecificDays`, `WeeklyFrequency`).
- `app/src/main/java/com/personaltracker/domain/model/Habit.kt` -- Expanded domain model with backward-compatible defaults: `cadenceType: CadenceType = CadenceType.DAILY`, `weeklyTargetFrequency: Int = 7`, `reminderTime: String? = null`, `isReminderEnabled: Boolean = false`, `goalTargetCount: Int = 1`, `category: String? = null`, plus helper `val cadence: HabitCadence`.
- `app/src/main/java/com/personaltracker/domain/model/HabitLog.kt` -- Log domain model and canonical `HabitLogStatus` constants (`COMPLETED`, `MISSED`, `SLACK_USED`, `FROZEN`, `GRACE_SHIELD_ABSORBED`, `REST_DAY`, `DAY_TYPE_IMMUNITY`).
- `app/src/main/java/com/personaltracker/domain/engine/StreakStateMachine.kt` -- Pure Kotlin state machine: 5 flame states (`COLD`, `WARMING`, `BURNING`, `BLAZING`, `GRACE_SHIELD`), `getFlameState(streak: Int, isGraceShieldActive: Boolean = false)`, and `evaluateDay(habit, isCompleted, logDate, activeDayType: DayType?)`.
- `app/src/main/java/com/personaltracker/domain/usecase/EvaluateHabitStreaksUseCase.kt` -- Domain use case orchestrating batch/rollover streak evaluation for any target date, resolving target date DayType and multi-day chronological gaps.
- `app/src/main/java/com/personaltracker/domain/usecase/LogHabitUseCase.kt` -- Habit logging use case with log upgrade support and idempotent reward transactions.
- `app/src/main/java/com/personaltracker/data/local/db/entities/HabitEntity.kt` -- Room entity updated with 6 new columns with default values.
- `app/src/main/java/com/personaltracker/data/local/db/daos/HabitDao.kt` -- DAO with `getAllHabitsList(): List<HabitEntity>` and batch operations.
- `app/src/main/java/com/personaltracker/data/local/db/AppDatabase.kt` -- Database schema version bumped to 3 with `MIGRATION_2_3`.
- `app/src/main/java/com/personaltracker/di/DatabaseModule.kt` -- Room database provider registering `MIGRATION_1_2` and `MIGRATION_2_3`.
- `app/src/main/java/com/personaltracker/data/repository/DataMappers.kt` -- Bidirectional entity-domain mappers with string-to-enum parsing and legacy cadence inference.
- `app/src/main/res/drawable/ic_flame_shield.xml` -- Vector drawable for the grace shield indicator.
- `app/src/main/java/com/personaltracker/ui/components/atoms/StreakFlameBadge.kt` -- Atomic component supporting `FlameState.GRACE_SHIELD` mapped to `ic_flame_shield` and `colors.info`.
- `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- Card accepting `flameState: FlameState? = null`, 7-day mini cadence indicator, grace badge, and 48dp toggle.
- `app/src/main/java/com/personaltracker/ui/components/cards/VerticalCapsuleBars.kt` -- Dynamic 4-capsule progress bar parametrized by `List<VerticalBarMetric>` and habit consistency metrics.
- `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsViewModel.kt` -- ViewModel exposing `capsuleMetrics`, active streaks, and live `flameState` propagation.
- `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsScreen.kt` -- Screen passing `item.flameState` to cards and `state.capsuleMetrics` to `VerticalCapsuleBars`.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/HabitCadence.kt` -- Create pure Kotlin cadence model (`CadenceType`, `HabitCadence` hierarchy with `Daily`, `SpecificDays(daysOfWeek: Set<Int>)`, `WeeklyFrequency(targetTimesPerWeek: Int)`) -- Foundation for flexible cadences (AD-1).
- [x] `app/src/main/java/com/personaltracker/domain/model/HabitLog.kt` -- Define canonical `HabitLogStatus` constants (`COMPLETED`, `MISSED`, `SLACK_USED`, `FROZEN`, `GRACE_SHIELD_ABSORBED`, `REST_DAY`, `DAY_TYPE_IMMUNITY`) -- Canonical status model.
- [x] `app/src/main/java/com/personaltracker/domain/model/Habit.kt` -- Expand `Habit` with backward-compatible defaults: `cadenceType: CadenceType = CadenceType.DAILY`, `weeklyTargetFrequency: Int = 7`, `reminderTime: String? = null`, `isReminderEnabled: Boolean = false`, `goalTargetCount: Int = 1`, `category: String? = null`, and helper `val cadence: HabitCadence` -- Domain model alignment.
- [x] `app/src/main/java/com/personaltracker/domain/engine/StreakStateMachine.kt` -- Add `FlameState.GRACE_SHIELD`, update `getFlameState(streak: Int, isGraceShieldActive: Boolean = false): FlameState`, and update `evaluateDay(habit: Habit, isCompleted: Boolean, logDate: String? = null, activeDayType: DayType? = null): StreakEvaluationResult` with DayType immunity and off-cadence day immunity -- Non-punishing streak engine (AD-4).
- [x] `app/src/main/java/com/personaltracker/domain/usecase/EvaluateHabitStreaksUseCase.kt` -- Create batch evaluation use case injecting `HabitRepository`, `DayTypeRepository`, `ResolveDayTypeUseCase`, `StreakStateMachine`, and `TransactionRunner`, handling target date DayType resolution and multi-day chronological gaps -- Automated rollover/miss handling.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/LogHabitUseCase.kt` -- Update to allow upgrading prior `REST_DAY`/`MISSED` logs to `COMPLETED`, ensure `HABIT_LOG_{habitId}_{date}` idempotency key validation, and catch duplicate key conflicts gracefully -- Idempotent economy ledger (ARCH-6).
- [x] `app/src/main/java/com/personaltracker/data/local/db/entities/HabitEntity.kt` -- Add `cadenceType`, `weeklyTargetFrequency`, `reminderTime`, `isReminderEnabled`, `goalTargetCount`, `category` columns with default values -- Room entity persistence.
- [x] `app/src/main/java/com/personaltracker/data/local/db/daos/HabitDao.kt` -- Add `suspend fun getAllHabitsList(): List<HabitEntity>` and batch operations -- Persistence access optimization.
- [x] `app/src/main/java/com/personaltracker/data/local/db/AppDatabase.kt` & `app/src/main/java/com/personaltracker/di/DatabaseModule.kt` -- Bump DB version to 3, implement `MIGRATION_2_3` executing `ALTER TABLE` and conditional `UPDATE habits SET cadenceType = 'SPECIFIC_DAYS'`, and register in Hilt module -- Database schema migration.
- [x] `app/src/main/java/com/personaltracker/data/repository/DataMappers.kt` -- Update `toDomain()` and `toEntity()` mappers for `HabitEntity` and `Habit` with string-to-enum parsing and legacy cadence inference -- Entity-domain mapping consistency.
- [x] `app/src/main/res/drawable/ic_flame_shield.xml` & `app/src/main/java/com/personaltracker/ui/components/atoms/StreakFlameBadge.kt` -- Add shield vector and atomically update `getFlameDrawableRes` and `getFlameColor` with `FlameState.GRACE_SHIELD` mapped to `ic_flame_shield` and `colors.info` token -- Visual grace status (AD-7).
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- Accept `flameState: FlameState? = null`, pass to `StreakFlameBadge`, and render 7-day cadence pills and grace shield indicator -- Warm Organic Tactile card presentation.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/VerticalCapsuleBars.kt` -- Parametrize with `metrics: List<VerticalBarMetric>`, purge fitness dummy text, and display habit productivity insights -- Dynamic visual feedback.
- [x] `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsUiState.kt`, `HabitsViewModel.kt` & `HabitsScreen.kt` -- Add `capsuleMetrics` to `HabitsUiState`, calculate 7-day habit percentages in ViewModel, pass `item.flameState` to cards, and default `slackBankDays = 0` on habit creation -- Unidirectional MVI presentation wiring (AD-2).
- [x] `app/src/test/java/com/personaltracker/domain/engine/StreakStateMachineTest.kt` -- Update existing unit tests for `GRACE_SHIELD_ABSORBED` and add test cases for cadence evaluation, off-cadence day immunity, DayType immunity, and grace shield transitions -- Verification of I/O Matrix.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/EvaluateHabitStreaksUseCaseTest.kt` & `app/src/test/java/com/personaltracker/ui/HabitsAndStudyViewModelTest.kt` -- Unit test streak evaluation use case and ViewModel state updates -- Comprehensive test coverage.

**Acceptance Criteria:**
- Given a habit with `SPECIFIC_DAYS` cadence (e.g. Mon/Wed/Fri), when evaluated on an unscheduled day (Tuesday) without completion, then the streak is not reset, 0 slack is consumed, and status is `REST_DAY`.
- Given a habit with active streak 5 and `isFreezeActive = true`, when missed on a scheduled day, then `isFreezeActive` becomes false, streak remains 5, flame displays `GRACE_SHIELD`, and status is `GRACE_SHIELD_ABSORBED`.
- Given an uncompleted habit on a date where active DayType is `LOW_CAPACITY` or `DayType.REST`, when evaluated, then streak is preserved without consuming slack bank days (status `DAY_TYPE_IMMUNITY`).
- Given a habit completion toggle, when executed, then a unique `HABIT_LOG_{habitId}_{date}` idempotency key is recorded and duplicate toggles do not award duplicate coins.
- Given a habit previously marked as `REST_DAY` or `MISSED` today, when the user completes it later in the day, then the log is upgraded to `COMPLETED`, streak increments, and coins are awarded.
- Given database version 2 with existing habits where `targetDaysOfWeek = "1,3,5"`, when upgrading to version 3, then `MIGRATION_2_3` runs successfully and preserves `cadenceType = SPECIFIC_DAYS` without data loss.

## Spec Change Log

- **2026-11-14 (EGM Pre-Code Gate Swarm Review):**
  - Clarified `WeeklyFrequency` evaluation rules: flexible non-penalization mid-week (`REST_DAY`), evaluated at Sunday midnight rollover against `targetTimesPerWeek`.
  - Added conditional SQL update to `MIGRATION_2_3` to preserve custom `targetDaysOfWeek` habits as `SPECIFIC_DAYS`.
  - Fixed `FlameState.GRACE_SHIELD` pipeline: overloaded `getFlameState(streak, isGraceShieldActive)` and wired `flameState` through ViewModel, Screen, Card, and Badge.
  - Added `activeDayType: DayType? = null` parameter to `StreakStateMachine.evaluateDay` and resolved target evaluation date in `EvaluateHabitStreaksUseCase`.
  - Fully specified `EvaluateHabitStreaksUseCase` dependencies and multi-day chronological gap processing.
  - Grouped `StreakFlameBadge.kt` exhaustive `when` update with `ic_flame_shield.xml` to prevent compilation errors.
  - Updated `LogHabitUseCase` to support upgrading prior non-completed logs to `COMPLETED` and handling duplicate key conflicts.
  - Parameterized `VerticalCapsuleBars` with `List<VerticalBarMetric>`, added `capsuleMetrics` to `HabitsUiState`, and purged hardcoded fitness metrics.
  - Established canonical `HabitLogStatus` constants in `HabitLog.kt`.
  - Added `HabitDao.kt` to Code Map and Tasks with non-Flow list accessor.
  - Set default `slackBankDays = 0` on new habit creation in `HabitsViewModel`.
  - Corrected test file targets to `HabitsAndStudyViewModelTest.kt` and `EvaluateHabitStreaksUseCaseTest.kt`.

## Design Notes

- **Cadence Bitmask / Days Set:** ISO-8601 day-of-week numbering (1 = Monday .. 7 = Sunday) matches `java.time.DayOfWeek.value`. In `HabitEntity`, `targetDaysOfWeek` stores comma-separated numbers (e.g. `"1,3,5"`) ensuring 100% backward compatibility with existing rows.
- **Flame State Hierarchy:**
  - `COLD`: 0 days
  - `WARMING`: 1..2 days
  - `BURNING`: 3..6 days
  - `BLAZING`: 7+ days
  - `GRACE_SHIELD`: Active freeze or day protected by grace shield/slack
- **Database Migration:**
  ```sql
  ALTER TABLE habits ADD COLUMN cadenceType TEXT NOT NULL DEFAULT 'DAILY';
  ALTER TABLE habits ADD COLUMN weeklyTargetFrequency INTEGER NOT NULL DEFAULT 7;
  ALTER TABLE habits ADD COLUMN reminderTime TEXT;
  ALTER TABLE habits ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0;
  ALTER TABLE habits ADD COLUMN goalTargetCount INTEGER NOT NULL DEFAULT 1;
  ALTER TABLE habits ADD COLUMN category TEXT;
  UPDATE habits SET cadenceType = 'SPECIFIC_DAYS' WHERE targetDaysOfWeek != '1,2,3,4,5,6,7' AND targetDaysOfWeek != '';
  ```

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED` (0 `android.*` in domain).
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `total_findings: 0`.
- `.\gradlew test` -- expected: All unit tests pass with 0 errors.
- `.\gradlew lintDebug` -- expected: Android lint completes with 0 errors.
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid.
