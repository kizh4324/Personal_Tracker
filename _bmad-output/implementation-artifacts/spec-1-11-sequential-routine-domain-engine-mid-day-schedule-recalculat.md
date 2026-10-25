---
title: 'Story 1.11: Sequential Routine Domain Engine & Mid-Day Schedule Recalculation'
type: 'feature'
created: '2026-10-25'
status: 'done'
baseline_commit: '1c56a4fe83d78ae6a365c4fda70962168c19ab0f'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users need structured sequential multi-step routines bound to their active DayType template, but currently routines only have a placeholder step in `HomeViewModel`, lack pure domain interactors for step progression, skipping, and completion rewards (15-coin bonus with `ROUTINE_COMP_{id}_{date}` idempotency), and cannot adapt dynamically when the user delays their start or taps "Shift to Now" mid-day without corrupting baseline SQLite templates or losing credit for previously completed steps.

**Approach:** Implement pure Kotlin domain interactors (`AdvanceRoutineStepUseCase`, `RecalculateRoutineTimelineUseCase`, `SaveRoutineUseCase`) orchestrating `AdaptiveScheduleEngine`, `GamificationLedgerEngine`, `RoutineRepository`, and `EconomyRepository` on `Dispatchers.IO` (AD-1, AD-2, AD-3, AD-6). Wire active routine timeline state, sequential step progression, and 1-tap "Shift to Now" recalculation actions into `HomeViewModel` MVI streams (`HomeUiState`, `HomeUiIntent`, `HomeUiEffect`), wired to `AdaptiveRoutineStepCard` and `HeroActionCard`. Maintain a 100% stateless, token-compliant Compose UI boundary with 48dp minimum touch targets and zero-guilt microcopy (AD-4, AD-7, EXP-016).

## Boundaries & Constraints

**Always:**
- Strictly conform to `ARCHITECTURE-SPINE.md` (AD-1 through AD-8), `_bmad-output/project-context.md`, and `.agents/rules/harness-subagents.md`.
- Pure Kotlin domain (`com.personaltracker.domain`): zero `android.*`, `androidx.*`, or `platform/` imports in domain models, interactors, and engines (AD-1, EXP-005).
- Non-destructive schedule adaptation (AD-6, EXP-019): base template steps in SQLite (`routine_steps`) must remain completely immutable during duration scaling or mid-day shifts; timeline recalculations and scaling are computed dynamically for the active schedule.
- Preservation of historical completions (AD-4, FR-3.5): previously completed or in-progress steps must retain their original completion timestamps and status when mid-day schedule recalculations or DayType template swaps occur.
- Safe timeline clamping (AD-6): LocalTime calculations must clamp safely at `23:59` to prevent cross-day midnight wrap-around bugs.
- Deterministic compound idempotency keys (AD-3, EXP-004): routine completion coin rewards (15 coins) must be committed to `CoinLedgerEntity` with compound key `ROUTINE_COMP_{routineId}_{date}`. Duplicate executions must be idempotent.
- Design token exclusivity (AD-7, EXP-007): resolve all colors, typography, shapes, and spacing from `com.personaltracker.ui.theme`. Minimum interactive touch target is 48dp floor (`spacing.minTouchTarget`, EXP-008).
- Headless-first / Figma-ready UI boundary (EXP-016): maintain 100% stateless `HomeScreenContent` and `AdaptiveRoutineStepCard` with zero ViewModel dependencies.
- Zero guilt microcopy (AD-4, EXP-011): no shame, guilt, or punitive copy when delaying, shifting to now, or resetting routines.

**Ask First:**
- Modifying Room database schema version (current is v1).
- Adding third-party time calculation or timeline libraries.

**Never:**
- Never import Android framework classes into `com.personaltracker.domain`.
- Never overwrite `RoutineStepEntity` durations in SQLite during schedule scaling or mid-day shifts (EXP-019).
- Never penalize user streaks, deduct coins, or shame the user when a routine is delayed or shifted (AD-4).
- Never write hardcoded `dp`, `sp`, or hex color literals in UI composables (AD-7).
- Never use `OnConflictStrategy.REPLACE` on Room entities with cascading foreign keys (AD-3, EXP-003).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Advance Active Step (Complete) | User taps "Complete" on active `RoutineStep` (order 1 of 3) | Step marked completed; cursor advances to step 2 (now `ACTIVE`); step 1 becomes `COMPLETED` (struck-through, 70% alpha); UI state updates | Safe state transition; no DB corruption |
| Complete Final Step of Routine | User completes final step (order 3 of 3) | Routine status transitions to completed; 15 coins awarded idempotently (`ROUTINE_COMP_{id}_{date}`); companion XP increased by 15 + `CELEBRATING` mood; `CoinArc` and `RewardAnimation` effects emitted | Idempotent transaction; duplicate calls return 0 coins |
| Skip Routine Step | User skips an optional/unfeasible step | Step marked skipped; cursor advances to next step; does not award step coins; preserves routine completion eligibility if policy permits | Safe progression without guilt copy |
| Mid-Day "Shift to Now" | Routine was scheduled for 08:00; user taps "Shift to Now" at 14:15 | Remaining uncompleted steps recalculate sequentially starting at 14:15; completed step timestamps preserved; zero-guilt snackbar emitted ("Routine shifted to now. Moving forward smoothly.") | Clamped safely at 23:59 via `safePlusMinutes` |
| DayType Morning/Mid-day Swap | User swaps DayType (e.g. Standard to Crunch) mid-day | `SwitchDayTypeUseCase` scales uncompleted routine step durations to match new DayType study targets while keeping base templates in SQLite unmutated; completed steps remain intact | Non-destructive dynamic scaling (EXP-019) |
| Routine Running Past 23:59 | Routine steps exceed remaining minutes in the day | End times clamp cleanly at `23:59`; does not wrap around to 00:00 or crash | Clamped via `AdaptiveScheduleEngine.safePlusMinutes` |
| Reset Routine for Day | User resets routine progress for testing/replay | Steps revert to initial state (step 1 `ACTIVE`, others `UPCOMING`); previously awarded coins remain safe in ledger | Zero guilt copy |
| Empty Routine Template | DayType has no routines configured | Section displays calm empty state ("No routine scheduled for this template.") with optional "Add Routine" trigger | Graceful empty list handling |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/Routine.kt` -- Pure domain models for `Routine`, `RoutineStep`, and built-in template definitions (`BUILT_IN_ROUTINES`).
- `app/src/main/java/com/personaltracker/domain/model/ScheduledRoutineStep.kt` -- Pure domain model representing scheduled execution state with time slots (`startTime`, `endTime`, `status`, `isCurrentCursor`).
- `app/src/main/java/com/personaltracker/domain/repository/RoutineRepository.kt` -- Extended pure repository interface with `getRoutineById`, `getAllRoutines`, `getStepsForRoutineList`, and `ensureDefaultRoutines`.
- `app/src/main/java/com/personaltracker/data/local/db/daos/RoutineDao.kt` -- Add query methods (`getRoutineById`, `getAllRoutines`, `getStepsForRoutineList`) preserving schema v1.
- `app/src/main/java/com/personaltracker/data/repository/RoutineRepositoryImpl.kt` -- Implement extended queries on `Dispatchers.IO` and default routine seeding.
- `app/src/main/java/com/personaltracker/domain/usecase/AdvanceRoutineStepUseCase.kt` -- Pure domain interactor executing step progression, cursor advancing, and 15-coin routine bonus idempotency.
- `app/src/main/java/com/personaltracker/domain/usecase/RecalculateRoutineTimelineUseCase.kt` -- Pure domain interactor shifting uncompleted steps to target/current time safely clamped at 23:59 with zero-guilt messaging.
- `app/src/main/java/com/personaltracker/domain/usecase/SaveRoutineUseCase.kt` -- Pure domain interactor validating and saving routine templates and ordered steps.
- `app/src/main/java/com/personaltracker/ui/components/cards/AdaptiveRoutineStepCard.kt` -- Support optional scheduled time slot (`"08:00 - 08:10"`), skip action, and 48dp touch targets.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- Expose active routine, timeline step list, active step cursor, progress summary, and completion state.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- Add `AdvanceRoutineStep(stepId, startOffset)`, `SkipRoutineStep(stepId)`, `ShiftRoutineToNow`, `ResetRoutineProgress`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Orchestrate routine observation, sequential progression, mid-day shift, and Hero card integration.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Render active routine module with "Shift to Now" button, sequential step cards, and completion banner.
- `app/src/test/java/com/personaltracker/domain/RoutineUseCasesTest.kt` -- Unit tests for `AdvanceRoutineStepUseCase`, `RecalculateRoutineTimelineUseCase`, and `SaveRoutineUseCase`.
- `app/src/test/java/com/personaltracker/ui/HomeViewModelRoutineTest.kt` -- Turbine unit tests for routine state flow, step progression, coin rewards, and "Shift to Now" recalculations.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/Routine.kt` -- Add built-in template constants and helper models -- AD-1 domain purity.
- [x] `app/src/main/java/com/personaltracker/domain/model/ScheduledRoutineStep.kt` -- Define pure Kotlin scheduled routine step presentation model -- AD-1.
- [x] `app/src/main/java/com/personaltracker/domain/repository/RoutineRepository.kt` -- Extend repository interface with queries and default seeding contract -- AD-1.
- [x] `app/src/main/java/com/personaltracker/data/local/db/daos/RoutineDao.kt` -- Add Room DAO queries for routine lookup and listing -- AD-3 persistence integrity.
- [x] `app/src/main/java/com/personaltracker/data/repository/RoutineRepositoryImpl.kt` -- Implement queries and built-in routine provisioning on `Dispatchers.IO` -- AD-3.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/AdvanceRoutineStepUseCase.kt` -- Implement step progression, cursor management, and 15-coin routine bonus idempotency -- AD-1, AD-4, AD-6.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/RecalculateRoutineTimelineUseCase.kt` -- Implement mid-day timeline recalculation with 23:59 safe clamping and zero-guilt microcopy -- AD-1, AD-4, AD-6.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/SaveRoutineUseCase.kt` -- Implement routine template validation and persistence -- AD-1, AD-3.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/AdaptiveRoutineStepCard.kt` -- Add time slot support and skip button with 48dp minimum touch targets -- AD-7, EXP-008.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` & `HomeUiIntent.kt` -- Wire routine timeline state, cursor, progress summary, and shift-to-now intents -- AD-2 MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Connect routine use cases, sequential progression, coin animations, and Hero card integration -- AD-2.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Render active routine card list with "Shift to Now" action and celebratory completion state -- EXP-016, AD-7.
- [x] `app/src/test/java/com/personaltracker/domain/RoutineUseCasesTest.kt` -- JVM test suite validating step advancement, routine bonus, safe clamping, and recalculation -- Pure JVM.
- [x] `app/src/test/java/com/personaltracker/ui/HomeViewModelRoutineTest.kt` -- Turbine test suite validating HomeViewModel routine streams and MVI actions -- Pure JVM.

**Acceptance Criteria:**
- Given a routine linked to the active DayType, when starting the routine, steps progress sequentially with the active step highlighted and upcoming steps dimmed.
- Given the final step in a routine is completed, `AdvanceRoutineStepUseCase` awards 15 coins idempotently with key `ROUTINE_COMP_{id}_{date}`, updates companion XP, and emits coin arc and reward animations.
- Given a routine delayed into the afternoon, tapping "Shift to Now" recalculates uncompleted steps to start at current time, clamped safely at 23:59, while preserving completed step timestamps and leaving base SQLite templates unmutated.
- All domain code contains zero Android framework imports (`domain_purity_check.py` passes).
- All UI composables resolve exclusively from `com.personaltracker.ui.theme` with 48dp minimum touch targets and zero hardcoded literals.

## Verification

**Commands:**
- `python .agents/scripts/harness2_bridge.py --check`
- `python .agents/scripts/domain_purity_check.py`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .`
- `.\gradlew test`
- `.\gradlew lintDebug`
