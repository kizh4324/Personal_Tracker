---
title: 'Story 1.10: Core Task Management & Delivery-Intensity Classification'
type: 'feature'
created: '2026-10-24'
status: 'done'
baseline_commit: 'fcfeb6cbaa88a08c38f85dd078ef056950b0fdb8'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users cannot create, edit, or delete discrete tasks directly from the Home surface, delivery intensity classes (`ROUTINE`, `IMPORTANT`, `URGENT`) lack pure domain CRUD interactors and non-blocking urgency soft-scarcity guidance (>= 2 active urgent tasks), and scheduled task reminders are not coordinated cleanly with `ExactAlarmScheduler` without leaking framework dependencies into the domain layer.

**Approach:** Implement pure Kotlin domain interactors (`SaveTaskUseCase`, `DeleteTaskUseCase`, `EvaluateTaskUrgencyScarcityUseCase`), an abstracted `TaskAlarmScheduler` domain interface bound to `ExactAlarmScheduler` in platform, MVI coordination in `HomeViewModel`, and a 100% stateless, token-compliant `TaskEditorSheet` with 48dp touch targets and zero-guilt microcopy.

## Boundaries & Constraints

**Always:**
- Strictly conform to `ARCHITECTURE-SPINE.md` (AD-1 through AD-8) and `_bmad-output/project-context.md`.
- Pure Kotlin domain (`com.personaltracker.domain`): zero `android.*`, `androidx.*`, or `platform/` imports in domain models, interactors, and interfaces (AD-1, EXP-005).
- Delivery intensity classes: `ROUTINE` (5 coins), `IMPORTANT` (7 coins), `URGENT` (10 coins) matching `GamificationLedgerEngine` (FR-2.1, FR-2.2). Prohibit arbitrary numeric P1-P4 scales.
- Non-blocking soft scarcity guidance (AD-4, FR-5.2, FR-5.3): when active `URGENT` tasks for the day reach >= 2, provide supportive non-blocking guidance; never hard-block or silently downgrade user choice.
- Reminder coordination: task creation/updates with `scheduledTime` must schedule exact/inexact alarms; task completion/deletion or cleared time must cancel pending alarms cleanly via `TaskAlarmScheduler`.
- Design token exclusivity (AD-7, EXP-007): resolve all colors, typography, shapes, and spacing from `com.personaltracker.ui.theme`. Minimum interactive touch target is 48dp floor (`spacing.minTouchTarget`, EXP-008).
- Headless-first / Figma-ready UI boundary (EXP-016): maintain 100% stateless `TaskEditorSheet(isOpen, task, isSoftCapReached, onSave, onDelete, onDismiss)` with zero ViewModel dependencies.
- Persistence mutations on `Dispatchers.IO` (AD-3, EXP-004): all repository writes must execute safely on IO coroutine contexts.

**Ask First:**
- Altering the SQLite schema of `tasks` table (`TaskEntity`).
- Adding additional third-party date/time picker libraries.

**Never:**
- Never import Android framework classes or platform service classes into `com.personaltracker.domain`.
- Never penalize user streaks, deduct coins, or use shame/guilt copy when deleting tasks or exceeding urgency soft-caps (AD-4).
- Never write hardcoded `dp`, `sp`, or hex color literals in UI composables.
- Never hard-block saving an urgent task when >= 2 urgent tasks already exist.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create New Task (Routine) | User enters title, selects `ROUTINE`, taps Save | Task saved with generated UUID, scheduled for today, PENDING state; `HomeUiState` updates; snackbar emitted | Validation: title must not be blank |
| Create Urgent Task Under Cap | User selects `URGENT` with 0 or 1 active urgent tasks | Task saved as `URGENT`; no scarcity warning; potential coin payout = 10; alarm registered if time set | Writes on `Dispatchers.IO` |
| Create Urgent Task Over Soft-Cap | User selects `URGENT` when 2 active urgent tasks exist | Non-blocking soft guidance banner displayed ("2 urgent tasks already active today. Keep focus sharp!"); user can still save as `URGENT` | Never blocks save; purely advisory |
| Edit Existing Task | User taps task card; edits title/time/intensity; saves | `SaveTaskUseCase` updates task; if time changed, old alarm cancelled and new alarm scheduled | Emits error snackbar on repository failure |
| Delete Task | User opens existing task and taps Delete | `DeleteTaskUseCase` removes task from DB; cancels pending alarm `TASK_<id>`; closes editor | Safe no-op if task not found |
| Task Completed Clears Alarm | User marks scheduled task as complete from list or Hero | Task completed with coin reward; pending alarm cancelled cleanly via scheduler | Idempotent transaction |
| Blank Title Validation | User attempts to save task with empty or whitespace title | Save prevented; editor displays supportive inline error ("Please enter a task title") | No DB write or side effects |
| Time Cleared on Edit | User clears previously set `scheduledTime` and saves | Task updated with `scheduledTime = null`; previously scheduled alarm is cancelled | Safe cancellation |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/alarm/TaskAlarmScheduler.kt` -- Pure Kotlin domain contract for scheduling and cancelling task reminder alarms.
- `app/src/main/java/com/personaltracker/platform/alarm/TaskAlarmSchedulerImpl.kt` -- Platform implementation delegating to `ExactAlarmScheduler`.
- `app/src/main/java/com/personaltracker/domain/usecase/SaveTaskUseCase.kt` -- Pure domain interactor validating, persisting, and coordinating reminder alarms for task creation/editing.
- `app/src/main/java/com/personaltracker/domain/usecase/DeleteTaskUseCase.kt` -- Pure domain interactor removing tasks and cancelling associated reminder alarms.
- `app/src/main/java/com/personaltracker/domain/usecase/EvaluateTaskUrgencyScarcityUseCase.kt` -- Pure domain interactor evaluating active urgent task count and soft-scarcity advisory guidance.
- `app/src/main/java/com/personaltracker/ui/components/overlays/TaskEditorSheet.kt` -- Stateless Compose editor sheet/dialog with 3-tier intensity chips, soft-cap guidance banner, and 48dp touch targets.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- Extend state with `isTaskEditorOpen`, `editingTask`, `activeUrgentTaskCount`, and `isUrgencySoftCapReached`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- Add intents: `OpenTaskEditor`, `DismissTaskEditor`, `SaveTaskItem`, `DeleteTaskItem`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- MVI coordinator handling task editor intents, soft-cap evaluation, and CRUD persistence.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Stateless `HomeScreenContent` hosting `TaskEditorSheet`, adding "Add Task" action, and wiring card clicks.
- `app/src/main/java/com/personaltracker/di/PlatformModule.kt` -- Hilt binding for `TaskAlarmScheduler`.
- `app/src/test/java/com/personaltracker/domain/TaskManagementUseCasesTest.kt` -- Unit tests verifying task save, delete, validation, alarm triggers, and soft-scarcity evaluation.
- `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Turbine unit tests verifying task editor MVI state transitions, task CRUD, and alarm coordination.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/alarm/TaskAlarmScheduler.kt` -- Define pure Kotlin alarm scheduler domain contract -- AD-1 domain purity.
- [x] `app/src/main/java/com/personaltracker/platform/alarm/TaskAlarmSchedulerImpl.kt` -- Implement platform delegate adapter for `ExactAlarmScheduler` and bind in `PlatformModule.kt` -- AD-1.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/EvaluateTaskUrgencyScarcityUseCase.kt` -- Implement active urgent count evaluator and soft-scarcity advisory logic -- AD-1, AD-4.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/SaveTaskUseCase.kt` -- Implement task validation, repository persistence, and alarm coordination -- AD-1, AD-3.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/DeleteTaskUseCase.kt` -- Implement task removal and alarm cancellation -- AD-1, AD-3.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/TaskEditorSheet.kt` -- Create stateless Compose task editor with delivery intensity chips and 48dp touch targets -- AD-4, AD-7, EXP-016.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- Add editor visibility, editing task, and urgency scarcity fields -- AD-2 MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- Add `OpenTaskEditor`, `DismissTaskEditor`, `SaveTaskItem`, `DeleteTaskItem` intents -- AD-2 MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Integrate CRUD use cases, alarm coordination, and editor state flows -- AD-2.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Host `TaskEditorSheet` in `HomeScreenContent` and wire "Add Task" header button and card clicks -- EXP-016, AD-7.
- [x] `app/src/test/java/com/personaltracker/domain/TaskManagementUseCasesTest.kt` -- Unit test task validation, persistence, deletion, alarm scheduling, and urgency scarcity thresholds -- JVM test suite.
- [x] `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Turbine test task creation, editing, deletion, and soft-cap guidance flows -- JVM test suite.

**Acceptance Criteria:**
- Given a valid task title and intensity selection (`ROUTINE`, `IMPORTANT`, or `URGENT`), when saved, the task is persisted in Room database with appropriate coin valuation (5, 7, 10 coins).
- Given a task with `scheduledTime`, saving registers an alarm via `TaskAlarmScheduler`, and completing or deleting the task cancels the alarm.
- Given 2 active `URGENT` tasks for today, selecting `URGENT` for a 3rd task displays a non-blocking advisory banner without preventing save or altering user selection.
- Given clicking on an existing `TaskIntensityCard`, the editor opens with pre-populated fields and a functional "Delete" option.
- All domain code contains 0 Android framework imports, and UI composables contain 0 hardcoded color/dp literals with 48dp touch targets.

## Verification

**Commands:**
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: all 8 harness components pass
- `.\gradlew test` -- expected: all JVM unit tests pass (including new task management tests)
- `.\gradlew lintDebug` -- expected: 0 errors

## Suggested Review Order

**Domain Architecture & Pure Interactors**

- Pure Kotlin alarm contract isolating OS AlarmManager from business layer
  [`TaskAlarmScheduler.kt:12`](../../app/src/main/java/com/personaltracker/domain/alarm/TaskAlarmScheduler.kt#L12)

- Task validation, normalization, and IO-dispatched reminder alarm coordination
  [`SaveTaskUseCase.kt:30`](../../app/src/main/java/com/personaltracker/domain/usecase/SaveTaskUseCase.kt#L30)

- Task removal with clean cancellation of pending OS alarms
  [`DeleteTaskUseCase.kt:22`](../../app/src/main/java/com/personaltracker/domain/usecase/DeleteTaskUseCase.kt#L22)

- Non-blocking soft-scarcity advisory evaluator excluding the currently edited task
  [`EvaluateTaskUrgencyScarcityUseCase.kt:31`](../../app/src/main/java/com/personaltracker/domain/usecase/EvaluateTaskUrgencyScarcityUseCase.kt#L31)

- Task completion reward execution with idempotent alarm cancellation
  [`CompleteTaskUseCase.kt:53`](../../app/src/main/java/com/personaltracker/domain/usecase/CompleteTaskUseCase.kt#L53)

**Platform Alarm Integration**

- Platform adapter delegating to exact alarm scheduler with normalized IDs
  [`TaskAlarmSchedulerImpl.kt:16`](../../app/src/main/java/com/personaltracker/platform/alarm/TaskAlarmSchedulerImpl.kt#L16)

- Dagger/Hilt singleton provider binding domain scheduler to platform adapter
  [`PlatformModule.kt:28`](../../app/src/main/java/com/personaltracker/di/PlatformModule.kt#L28)

**Presentation & MVI Coordination**

- Editor visibility, editing model, and dynamic scarcity fields in UI state
  [`HomeUiState.kt:22`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt#L22)

- Explicit user intents for opening, dismissing, saving, and deleting tasks
  [`HomeUiIntent.kt:29`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt#L29)

- Atomic editor control state, dynamic scarcity combine, and CRUD handlers
  [`HomeViewModel.kt:321`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt#L321)

- Stateless bottom sheet editor with delivery intensity chips and accessibility
  [`TaskEditorSheet.kt:85`](../../app/src/main/java/com/personaltracker/ui/components/overlays/TaskEditorSheet.kt#L85)

- Home screen wiring for Add Task action, task clicks, and particle origins
  [`HomeScreen.kt:139`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt#L139)

**Verification & Test Suites**

- Platform alarm adapter delegation and prefix normalization unit tests
  [`TaskAlarmSchedulerImplTest.kt:21`](../../app/src/test/java/com/personaltracker/platform/TaskAlarmSchedulerImplTest.kt#L21)

- Comprehensive domain use case tests covering validation, alarms, and scarcity
  [`TaskManagementUseCasesTest.kt:150`](../../app/src/test/java/com/personaltracker/domain/TaskManagementUseCasesTest.kt#L150)

- Turbine MVI state transition and intent handling tests
  [`HomeViewModelTest.kt:558`](../../app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt#L558)

