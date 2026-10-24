---
title: 'Story 1.9: Dynamic State-Aware Hero Card Integration with Action List'
type: 'feature'
created: '2026-10-24'
status: 'done'
baseline_commit: '0fb2cf5c89cb8a85bacca98643663e8f0da78f0f'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The 6-state `HeroActionCard` priority resolution currently runs ad-hoc inside `HomeViewModel` presentation logic without pure domain isolation, snooze/skip interactions are unhandled no-ops, and carry-forward reviews lack 1-tap reschedule/guilt-free dismissal synchronization with the daily action list.

**Approach:** Implement pure Kotlin `ResolveHeroStateUseCase` evaluating the 6-state Hero priority hierarchy (`ActiveFocus` > `InterruptedResumable` > `MorningBanner` > `UpcomingItem` > `CarryForwardReview` > `IntentionalIdle`), domain interactors for 1-tap carry-forward/guilt-free dismissal (`CarryForwardTasksUseCase`, `DismissOverdueTasksUseCase`), reactive snooze promotion in `HomeViewModel`, and bidirectional MVI synchronization between `HeroActionCard` and the daily action list with Figma-ready stateless UI boundaries.

## Boundaries & Constraints

**Always:**
- Strictly conform to `ARCHITECTURE-SPINE.md` (AD-1 through AD-8) and `_bmad-output/project-context.md`.
- Pure Kotlin domain (`com.personaltracker.domain`): zero `android.*`, `androidx.*`, or `platform/` imports in domain use cases and models (AD-1, EXP-005).
- Hero Priority Order (Strict 6-State Hierarchy): 1) `ActiveFocus` > 2) `InterruptedResumable` > 3) `MorningBanner` > 4) `UpcomingItem` > 5) `CarryForwardReview` > 6) `IntentionalIdle`.
- Non-punishing psychological safety (AD-4, EXP-011): 1-tap snooze and dismiss operations must NEVER deduct coins, damage companion pet energy, or penalize streak counters; microcopy must remain supportive and calming.
- Design token exclusivity (AD-7, EXP-007): resolve all colors, typography, shapes, and spacing from `com.personaltracker.ui.theme`. Minimum interactive touch target is 48dp floor (`spacing.minTouchTarget`, EXP-008).
- Headless-first / Figma-ready UI boundary (EXP-016): maintain 100% stateless `HomeScreenContent(state, onIntent)` and `CarryForwardReviewDialog`.
- Atomic persistence mutations on `Dispatchers.IO` (AD-3, EXP-004): multi-task carry-forward updates must execute within repository/Room transaction scopes.

**Ask First:**
- Altering the SQLite schema of `TaskEntity` or `RoutineEntity`.
- Changing the 6-state Hero priority ordering.

**Never:**
- Never import Android framework classes or platform service classes into `com.personaltracker.domain`.
- Never penalize user streaks, deduct coins, or display guilt copy when snoozing items or dismissing overdue tasks (AD-4).
- Never allow state divergence between `HeroActionCard` and subordinate task list items.
- Never write hardcoded `dp`, `sp`, or hex color literals in UI composables.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Active Focus Running | Live study session timer is running | Hero Card resolves `ActiveFocus` (priority 1) with formatted countdown and pause/end actions | Resets to IntentionalIdle if timer session completes |
| Interrupted Session | Task in `INTERRUPTED` state or paused timer | Hero Card resolves `InterruptedResumable` (priority 2); shows "Resume", "Review", "Complete" | Emits error snackbar if task record missing |
| Morning Banner Check-in | Morning banner not yet dismissed | Hero Card resolves `MorningBanner` (priority 3); shows active DayType and swap/dismiss actions | Fallback to next candidate on dismiss |
| Upcoming Item Promotion | Tasks pending for today; banner dismissed | Hero Card resolves `UpcomingItem` (priority 4); shows title and start/snooze actions | Clamps duration/startsIn to >= 0 |
| Snooze Current Hero Item | User taps "Snooze" on upcoming item | Item is added to session snoozed set; next pending task or routine step immediately promotes to Hero Card without penalty | If no further items, transitions to CarryForward or IntentionalIdle |
| Carry-Forward Review Available | Overdue uncompleted tasks from prior days; no upcoming items | Hero Card resolves `CarryForwardReview` (priority 5); shows "X items still open. What would you like to do?" | Pluralization handled cleanly (1 item vs N items) |
| 1-Tap Carry-Forward to Today | User reviews carry-forward and taps "Carry Forward to Today" | Overdue tasks updated with `scheduledDate = today`, `isCarryForward = true`, and incremented count; Hero Card updates | Writes on `Dispatchers.IO`; emits error snackbar on DB failure |
| Guilt-Free Dismiss Overdue | User taps "Dismiss Without Guilt" in review dialog | Overdue tasks marked `CANCELLED`; cleared from pending queue with zero guilt copy; Hero transitions to IntentionalIdle | Safe no-op if list is empty |
| All Caught Up | No active timer, interrupted tasks, banner dismissed, no pending items | Hero Card resolves `IntentionalIdle` (priority 6) with calming companion resting message | Always guaranteed safe fallback |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/HeroPriorityState.kt` -- Pure Kotlin sealed domain model for 6 Hero priority states and session DTOs (`HeroActiveSession`).
- `app/src/main/java/com/personaltracker/domain/usecase/ResolveHeroStateUseCase.kt` -- Pure Kotlin priority machine evaluating the 6 states with snooze/skip filtering.
- `app/src/main/java/com/personaltracker/domain/usecase/CarryForwardTasksUseCase.kt` -- Domain interactor updating overdue tasks to today's date with carry-forward metadata.
- `app/src/main/java/com/personaltracker/domain/usecase/DismissOverdueTasksUseCase.kt` -- Domain interactor safely dismissing overdue tasks with zero penalty.
- `app/src/main/java/com/personaltracker/ui/components/cards/HeroActionCard.kt` -- UI presentation card consuming `HeroCardState` with 48dp touch targets and token styling.
- `app/src/main/java/com/personaltracker/ui/components/overlays/CarryForwardReviewDialog.kt` -- Stateless Compose dialog displaying overdue tasks with 1-tap reschedule or guilt-free dismiss.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- State exposing `isCarryForwardDialogOpen`, `overdueTasks`, and active `heroCardState`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- User intents: `SnoozeHeroItem`, `SkipHeroItem`, `StartHeroItem`, `OpenCarryForwardReview`, `DismissCarryForwardDialog`, `CarryForwardAllOverdue`, `DismissAllOverdue`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- MVI coordinator driving `ResolveHeroStateUseCase`, snooze tracking, carry-forward actions, and action list synchronization.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Stateless `HomeScreenContent` hosting `CarryForwardReviewDialog` and wiring Hero actions to `onIntent`.
- `app/src/test/java/com/personaltracker/domain/ResolveHeroStateUseCaseTest.kt` -- Unit tests validating 6-tier priority hierarchy and snooze promotion.
- `app/src/test/java/com/personaltracker/domain/CarryForwardUseCasesTest.kt` -- Unit tests verifying carry-forward reschedule and non-punitive dismissal.
- `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Turbine unit tests verifying bidirectional Hero/action list synchronization and effect emissions.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/HeroPriorityState.kt` -- Create pure Kotlin sealed interface and session DTOs for the 6 Hero priority states -- AD-1 domain purity.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/ResolveHeroStateUseCase.kt` -- Implement priority machine with snooze and overdue evaluation -- AD-1, AD-4, AD-6.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/CarryForwardTasksUseCase.kt` -- Implement 1-tap reschedule interactor for overdue tasks -- AD-3, AD-6.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/DismissOverdueTasksUseCase.kt` -- Implement guilt-free dismissal interactor for overdue tasks -- AD-4.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/CarryForwardReviewDialog.kt` -- Build stateless dialog with 48dp touch targets and zero-guilt copy -- AD-4, AD-7.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- Add `isCarryForwardDialogOpen` and `overdueTasks` fields -- AD-2 MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- Add intents for snooze, skip, start, carry-forward, and dismiss dialog actions -- AD-2 MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Integrate `ResolveHeroStateUseCase`, carry-forward interactors, snooze state flow, and atomic list synchronization -- AD-2, EXP-014.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Wire `CarryForwardReviewDialog` and Hero Card intents into `HomeScreenContent` -- EXP-016, AD-7.
- [x] `app/src/test/java/com/personaltracker/domain/ResolveHeroStateUseCaseTest.kt` -- Unit test all 6 priority tiers, snooze promotion, and empty fallbacks -- JVM test suite.
- [x] `app/src/test/java/com/personaltracker/domain/CarryForwardUseCasesTest.kt` -- Unit test carry-forward rescheduling and guilt-free dismissal -- JVM test suite.
- [x] `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Turbine test bidirectional Hero Card and action list synchronization -- JVM test suite.

**Acceptance Criteria:**
- Given an active focus session or interrupted task, Hero Card displays state 1 or 2 regardless of pending tasks.
- Given an upcoming item on the Hero Card, tapping "Snooze" immediately promotes the next pending task or routine step without deducting coins or penalizing streaks.
- Given completing a task via either the Hero Card button or the daily action list item, both components update atomically and dispatch coin arc / reward animation `UiEffect`s.
- Given overdue tasks from prior days, tapping "Review Items" on the Hero Card opens `CarryForwardReviewDialog`, and selecting "Carry Forward to Today" or "Dismiss Without Guilt" updates persistence and transitions Hero Card to the next state.
- All domain code contains 0 Android framework imports, and UI composables contain 0 hardcoded color/dp literals with 48dp touch targets.

## Verification

**Commands:**
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: all 8 harness components pass
- `.\gradlew test` -- expected: all JVM unit tests pass (including new Hero and CarryForward tests)
- `.\gradlew lintDebug` -- expected: 0 errors

## Suggested Review Order

**Pure Domain Hero Priority & Interactors**

- Strict 6-state Hero priority state machine evaluating live focus, interrupted tasks, banner, upcoming queue, carry-forward, and idle.
  [`ResolveHeroStateUseCase.kt:34`](../../app/src/main/java/com/personaltracker/domain/usecase/ResolveHeroStateUseCase.kt#L34)

- Domain model defining 6 Hero priority states and decoupled session DTOs.
  [`HeroPriorityState.kt:25`](../../app/src/main/java/com/personaltracker/domain/model/HeroPriorityState.kt#L25)

- Atomic 1-tap carry-forward rescheduling for overdue tasks to today with rollover metadata.
  [`CarryForwardTasksUseCase.kt:25`](../../app/src/main/java/com/personaltracker/domain/usecase/CarryForwardTasksUseCase.kt#L25)

- Non-punitive dismissal of overdue tasks without penalty or streak reduction.
  [`DismissOverdueTasksUseCase.kt:24`](../../app/src/main/java/com/personaltracker/domain/usecase/DismissOverdueTasksUseCase.kt#L24)

**MVI Coordination & Bidirectional Synchronization**

- MVI flow aggregation coordinating priority machine, snooze tracking, and atomic action list sync.
  [`HomeViewModel.kt:180`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt#L180)

- User intents covering snooze, skip, start, dialog controls, and active focus timer resumption.
  [`HomeUiIntent.kt:22`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt#L22)

- Immutable state contract exposing carry-forward dialog visibility and overdue task list.
  [`HomeUiState.kt:20`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt#L20)

**Stateless UI Components & Overlays**

- Stateless Compose dialog with 48dp touch targets, zero-guilt copy, and WCAG AA contrast.
  [`CarryForwardReviewDialog.kt:35`](../../app/src/main/java/com/personaltracker/ui/components/overlays/CarryForwardReviewDialog.kt#L35)

- Screen content root wiring CarryForwardReviewDialog and HeroActionCard to onIntent.
  [`HomeScreen.kt:114`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt#L114)

**Verification & Test Suites**

- Comprehensive unit tests covering all 6 priority tiers, snooze promotion, and empty fallbacks.
  [`ResolveHeroStateUseCaseTest.kt:20`](../../app/src/test/java/com/personaltracker/domain/ResolveHeroStateUseCaseTest.kt#L20)

- Unit tests verifying carry-forward date updates, count increments, and cancellation without guilt.
  [`CarryForwardUseCasesTest.kt:20`](../../app/src/test/java/com/personaltracker/domain/CarryForwardUseCasesTest.kt#L20)

- Turbine integration tests asserting bidirectional Hero/list sync and reward emissions.
  [`HomeViewModelTest.kt:270`](../../app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt#L270)

