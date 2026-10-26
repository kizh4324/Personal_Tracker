---
title: 'Story 3.1: Focus & Study Session Models, Timer & Heatmap Tracking'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: '98bff88dc92f2086254ccf31dc9516f0180e86c6'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users need structured focus intervals (Deep Work vs. Pomodoro cycles and breaks) and long-term visual feedback of their study consistency. Currently, focus blocks only support raw minutes without interval classification, and the Study surface lacks historical heatmap aggregation, making it impossible to visualize daily focus momentum (FR-2.5, FR-4.1, FR-4.2).

**Approach:** Expand the pure Kotlin domain models (`StudySession.kt`, `FocusRescheduleEngine.kt`) to support interval classification (`FocusIntervalType`), target vs. actual elapsed duration tracking, and Pomodoro cycle transitions. Implement a pure Kotlin `HeatmapLedgerEngine` to roll up historical sessions into structured 5-tier contribution heatmap grids. Wire these engines into `StudyViewModel` via MVI, syncing with `StudySessionForegroundService`, and render an accessible, token-strict "Warm Organic Tactile" contribution graph and interval mode selector on `StudyScreen` (AD-1, AD-2, AD-7).

## Boundaries & Constraints

**Always:**
- Keep all domain models and behavioral engines 100% pure Kotlin in `com.personaltracker.domain` with zero `android.*` or `androidx.*` dependencies (AD-1).
- Maintain complete backward compatibility in `StudySession` with default parameters (`intervalType = FocusIntervalType.DEEP_WORK`, `targetDurationMinutes = durationMinutes`, `actualElapsedMinutes = durationMinutes`).
- Calculate heatmap contributions into 5 deterministic intensity tiers: Tier 0 (0m), Tier 1 (1–29m), Tier 2 (30–59m), Tier 3 (60–119m), Tier 4 (120m+).
- Enforce non-punishing UX copy (AD-4): zero guilt or shame copy on interrupted sessions or empty heatmap days.
- Adhere strictly to the Warm Organic Tactile design tokens: zero hardcoded hex literals (`#...`), `dp`, or `sp` literals in UI composables; resolve exclusively from `PersonalTrackerTheme.colors`, `typography`, `spacing`, and `shapes` (AD-7).
- Ensure 48dp minimum touch target floor for all interactive controls (AD-7).

**Ask First:**
- Modifying Room database schema or adding new columns to `StudySessionEntity` (use purely domain/in-memory mapping unless persistent migration is explicitly requested).
- Changing the minimum session duration threshold for Coin reward allocation (currently >= 10 mins).

**Never:**
- Import Android framework classes (`Context`, `SharedPreferences`, `Bundle`, `Notification`) into `domain/` (AD-1).
- Penalize the user for breaks or interrupted focus sessions with streak losses or pet energy decay (AD-4).
- Write hardcoded color values or bypass theme tokens in `StudyScreen` or any new UI components (AD-7).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Deep Work Session (Standard) | User starts 45m "Deep Work" session and completes full duration | Session logged with `actualElapsedMinutes = 45`, `targetDurationMinutes = 45`, `isInterrupted = false`; awards coins; heatmap adds 45m | N/A |
| Pomodoro Work to Break Transition | 25m Pomodoro finishes; user transitions to break | `FocusRescheduleEngine.calculateIntervalTransition` suggests 5m `SHORT_BREAK`; cycle count increments | Clamps to max 4 cycles before suggesting `LONG_BREAK` |
| Interrupted Focus Block | Session ends early (e.g. 15m elapsed out of 45m target) | Logged with `actualElapsedMinutes = 15`, `targetDurationMinutes = 45`, `isInterrupted = true`; partial minutes credited to heatmap; zero coin reward if <10m | Non-punishing copy: "Focus block paused with 30 min remaining. You can resume anytime." |
| Empty Heatmap History | New user with 0 completed sessions | `HeatmapLedgerEngine` generates 28-day grid with all Tier 0 cells; total minutes = 0 | Empty state displays calm encouragement |
| Multi-Session Daily Aggregation | User completes 25m Pomodoro, 15m Reading, and 40m Deep Work on same date | Total = 80m; cell for today displays Tier 3 (60..119m); breakdown maps individual subjects | Arithmetic overflow guarded via safe math |
| Session Crossing Midnight | Session starts at 23:45 and ends at 00:15 | Duration clamped at day boundary or allocated to start date according to `safePlusMinutes` | No negative durations or crash |
| Heatmap Day Selection | User clicks on a specific cell in contribution grid | `StudyUiIntent.SelectHeatmapCell` updates state with selected day's breakdown and minutes | Tapping already selected day clears or toggles view |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/StudySession.kt` -- Extended with `FocusIntervalType`, `targetDurationMinutes`, `actualElapsedMinutes`, and helper properties.
- `app/src/main/java/com/personaltracker/domain/model/FocusIntervalType.kt` -- Pure Kotlin enum (`DEEP_WORK`, `POMODORO_WORK`, `SHORT_BREAK`, `LONG_BREAK`, `REVIEW`).
- `app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt` -- Enhanced with interval transition logic and Pomodoro cycle calculations.
- `app/src/main/java/com/personaltracker/domain/engine/HeatmapLedgerEngine.kt` -- Pure Kotlin behavioral engine aggregating sessions into structured 5-tier contribution heatmaps and subject rollups.
- `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Provides Hilt singleton binding for `HeatmapLedgerEngine`.
- `app/src/main/java/com/personaltracker/ui/screens/study/StudyUiState.kt` -- Extended with `heatmapData`, `selectedIntervalType`, `completedCycles`, and selected day breakdown.
- `app/src/main/java/com/personaltracker/ui/screens/study/StudyUiIntent.kt` -- Extended with `SelectIntervalType` and `SelectHeatmapCell`.
- `app/src/main/java/com/personaltracker/ui/screens/study/StudyViewModel.kt` -- Injects `HeatmapLedgerEngine`; observes repository sessions to dynamically compute heatmap; handles interval selections.
- `app/src/main/java/com/personaltracker/ui/screens/study/StudyScreen.kt` -- Updated with tactile 28-day contribution heatmap grid, interval type selector pills, and breakdown card using design tokens.
- `app/src/test/java/com/personaltracker/domain/engine/HeatmapLedgerEngineTest.kt` -- Unit tests verifying 5-tier classification, 28-day padding, subject breakdowns, empty histories, and multi-session aggregations.
- `app/src/test/java/com/personaltracker/domain/engine/FocusRescheduleEngineTest.kt` -- Unit tests verifying Pomodoro cycle transitions and interval adjustments.
- `app/src/test/java/com/personaltracker/ui/StudyViewModelTest.kt` -- Unit tests verifying heatmap computation, interval selection, and timer state synchronization.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/FocusIntervalType.kt` -- Create enum `FocusIntervalType` with display titles and break flags.
- [x] `app/src/main/java/com/personaltracker/domain/model/StudySession.kt` -- Add `targetDurationMinutes`, `actualElapsedMinutes`, `intervalType`, and `isBreak` with backward-compatible defaults.
- [x] `app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt` -- Add `calculateIntervalTransition` and cycle tracking helpers.
- [x] `app/src/main/java/com/personaltracker/domain/engine/HeatmapLedgerEngine.kt` -- Implement heatmap rollup engine computing 28-day cells, 5-tier intensity levels, active days, and subject breakdown.
- [x] `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Provide `@Singleton` `HeatmapLedgerEngine`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/StudyUiState.kt` -- Add `heatmapData: StudyHeatmapData`, `selectedIntervalType: FocusIntervalType`, and `selectedCellDate: String?`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/StudyUiIntent.kt` -- Add `SelectIntervalType` and `SelectHeatmapCell` intents.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/StudyViewModel.kt` -- Inject `HeatmapLedgerEngine`; recalculate heatmap reactively upon session flow emissions; handle new intents.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/StudyScreen.kt` -- Render tactile interval selector and 28-day contribution heatmap grid with Warm Organic Tactile tokens (AD-7).
- [x] `app/src/test/java/com/personaltracker/domain/engine/HeatmapLedgerEngineTest.kt` -- Test 5-tier thresholds, empty list, multi-session day aggregation, and subject breakdowns.
- [x] `app/src/test/java/com/personaltracker/domain/engine/FocusRescheduleEngineTest.kt` -- Test Pomodoro interval progression and break recommendations.
- [x] `app/src/test/java/com/personaltracker/ui/StudyViewModelTest.kt` -- Test ViewModel state mapping, heatmap observation, and interval selection.

**Acceptance Criteria:**
- Given a series of completed study sessions across multiple days, when processed by `HeatmapLedgerEngine`, then a 28-day chronological cell list is produced with accurate intensity tiers (0 to 4) and total focus minutes.
- Given an active Pomodoro work session reaching 0 seconds, when evaluating the next interval, then `FocusRescheduleEngine` recommends a `SHORT_BREAK` (or `LONG_BREAK` after 4 completed cycles).
- Given the `StudyScreen`, when rendered, then the contribution heatmap displays 28 tactile cells using `colors.surfaceSunken`, `colors.iconSoftPeach`, `colors.iconSoftMatcha`, `colors.primaryLight`, and `colors.primary` with zero hardcoded literals (AD-7).
- Given a user clicking a heatmap cell, when clicked, then the selected day's total focus minutes and subject breakdown are displayed.
- Given all domain code in `com.personaltracker.domain.model` and `com.personaltracker.domain.engine`, when inspected, then zero `android.*` or `androidx.*` packages are imported (AD-1).

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Design Notes

- **Heatmap 5-Tier Intensity Mapping (FR-4.2):**
  - `Tier 0`: 0 minutes (`colors.surfaceSunken`, `#EBE5DC`)
  - `Tier 1`: 1–29 minutes (`colors.iconSoftPeach`, `#FCD9B8`)
  - `Tier 2`: 30–59 minutes (`colors.iconSoftMatcha`, `#A3D929`)
  - `Tier 3`: 60–119 minutes (`colors.primaryLight`, `#FFE8D6`)
  - `Tier 4`: 120+ minutes (`colors.primary`, `#F26B1D`)
- **Interval Lifecycle (FR-4.1):**
  - Standard Pomodoro: 25m work + 5m short break.
  - After 4 work cycles: 15m long break.
  - Deep Work: Unstructured block (15m, 25m, 45m, 60m+).

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `0 findings`
- `.\gradlew test` -- expected: Complete JVM unit test suite passes cleanly
- `.\gradlew lintDebug` -- expected: 0 errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid

## Suggested Review Order

**Domain Models & Behavioral Engines (AD-1 Purity)**

- Focus interval classification and interval metadata
  [`FocusIntervalType.kt:7`](../../app/src/main/java/com/personaltracker/domain/model/FocusIntervalType.kt#L7)

- Extended study session model with backward-compatible interval defaults
  [`StudySession.kt:6`](../../app/src/main/java/com/personaltracker/domain/model/StudySession.kt#L6)

- Pomodoro cycle progression and transition logic
  [`FocusRescheduleEngine.kt:156`](../../app/src/main/java/com/personaltracker/domain/engine/FocusRescheduleEngine.kt#L156)

- 5-tier contribution heatmap ledger and subject aggregation engine
  [`HeatmapLedgerEngine.kt:19`](../../app/src/main/java/com/personaltracker/domain/engine/HeatmapLedgerEngine.kt#L19)

**Dependency Injection & MVI ViewModel Integration**

- Singleton provision of heatmap engine in Hilt
  [`DomainEngineModule.kt:98`](../../app/src/main/java/com/personaltracker/di/DomainEngineModule.kt#L98)

- Reactive session collection, heatmap calculation, and Pomodoro auto-progression
  [`StudyViewModel.kt:33`](../../app/src/main/java/com/personaltracker/ui/screens/study/StudyViewModel.kt#L33)

- Expanded MVI state with heatmap data, selected interval, and cell breakdown
  [`StudyUiState.kt:12`](../../app/src/main/java/com/personaltracker/ui/screens/study/StudyUiState.kt#L12)

**Token-Strict UI Presentation (AD-7)**

- Tactile 28-day contribution heatmap grid and interval selector pills
  [`StudyScreen.kt:472`](../../app/src/main/java/com/personaltracker/ui/screens/study/StudyScreen.kt#L472)

**Comprehensive Test Verification**

- Heatmap 5-tier classification and 28-day distribution tests
  [`HeatmapLedgerEngineTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/engine/HeatmapLedgerEngineTest.kt#L1)

- Focus reschedule interval progression and cycle escalation tests
  [`FocusRescheduleEngineTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/engine/FocusRescheduleEngineTest.kt#L1)

- ViewModel MVI state, intent, and reactive observation tests
  [`StudyViewModelTest.kt:1`](../../app/src/test/java/com/personaltracker/ui/StudyViewModelTest.kt#L1)
