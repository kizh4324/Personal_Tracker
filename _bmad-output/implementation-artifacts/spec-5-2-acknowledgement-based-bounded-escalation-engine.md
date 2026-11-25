---
title: 'Story 5.2: Acknowledgement-Based Bounded Escalation Engine'
type: 'feature'
created: '2026-11-25'
status: 'in-progress'
baseline_commit: '13e92b8f3a9ddb95f26b729a58f822323cf31874'
review_loop_iteration: 1
context:
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-1-tiered-notification-channels-exact-alarm-scheduling-engine.md'
  - 'ARCHITECTURE-SPINE.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 5.1 delivers single-shot tiered alarms, so unacknowledged `URGENT` alerts stay silent indefinitely once triggered, and notification actions or swipes do not halt escalation or coordinate active Urgent quota release.

**Approach:** Add pure Kotlin `EscalationPolicy`, `BoundedEscalationEngine`, and `EscalationStateRepository` to schedule bounded `URGENT` follow-ups (default 5-minute interval, max 3 re-fires) until acknowledged, completed, or dismissed. Synchronize OS notification/action receivers with per-coordinator `Mutex`, bounded `goAsync()` execution on `Dispatchers.IO`, in-place notification updates, and configurable cadence chips in `SettingsScreen`.

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** `EscalationPolicy`, `EscalationDecision`, `BoundedEscalationEngine`, and `EscalationStateRepository` in `domain/` must contain zero `android.*` or `androidx.*` imports.
- **AD-4 Non-Punishing Copy & Zero Restricted Word Rule:** Follow-up alerts use calm informational copy (`"Reminder: %1$s"` and `"Follow-up %1$d of %2$d • Ready when you are"`). Neither notification copy, identifiers, KDoc comments, logs, nor test assertions may contain the prohibited 3-letter word (`n-a-g` or its derivatives, FR-5.4).
- **Bounded Escalation Guarantee:** Only `URGENT` items escalate (`ROUTINE` and `IMPORTANT` never re-fire). Initial trigger is attempt `0`; re-fires are strictly bounded by `currentAttemptNumber < policy.maxReFires` (`0..maxReFires`).
- **Immediate Halt, Cleanup & Quota Release (FR-5.4, FR-5.5):** Completing (`CompleteTaskUseCase`), deleting (`DeleteTaskUseCase`), dismissing (`DismissOverdueTasksUseCase`), notification swipe-dismiss (`deleteIntent`), or tapping `"Got it"` halts escalation immediately. In-app task completion/deletion/dismissal also dismisses posted OS notifications and prunes acknowledgement keys; `EvaluateTaskUrgencyScarcityUseCase` dynamically releases Urgent slots whenever tasks transition to `COMPLETED`/`CANCELLED`.
- **Serialized OS Concurrency Safety:** `EscalationAlarmCoordinator` must guard trigger checks and action mutations with a `Mutex`, verify `POST_NOTIFICATIONS` before `notify()`, and run `BroadcastReceiver` work inside `goAsync()` + `withTimeout(8_000L)` + `try/finally { pendingResult.finish() }` on `Dispatchers.IO`.
- **AD-7 Token Conformance:** Settings `FilterChip` rows resolve 100% from `PersonalTrackerTheme` tokens (0 raw hex/dp/sp) and enforce `defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)` (48dp floor in both dimensions).
- **Strict No Code During Design:** No application Kotlin/XML edits until Design Checkpoint approval.

**Ask First:**
- Any schema migration bumping `AppDatabase` beyond v3 (avoided by storing `EscalationPolicy` and transient `ack_<taskId>` keys in `SharedPreferences`).

**Never:**
- Never escalate `ROUTINE` or `IMPORTANT` notifications, or schedule attempt `> policy.maxReFires`.
- Never clear acknowledgement inside `ExactAlarmScheduler.scheduleForTask(task)` (which `BootRecoveryCoordinator` invokes on reboot); clear only in `SaveTaskUseCase`, `CarryForwardTasksUseCase`, `CompleteTaskUseCase`, `DeleteTaskUseCase`, and `DismissOverdueTasksUseCase`.
- Never delete or cancel a `Task` on `"Got it"` / `deleteIntent`; leave `Task` `PENDING` while halting escalation.
- Never omit `EXTRA_ESCALATION_ATTEMPT` from `SystemAlarmManagerDelegate.scheduleInexactFallback` (prevents infinite attempt-`0` loop on permission fallback).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| **Initial Urgent Fire (Attempt 0)** | `URGENT`, `PENDING`, `ack=false`, `attempt=0`, `maxReFires=3` | Posts notification ID `taskId.hashCode()` with Story 5.1 title/description + `"Mark Done"`, `"Got it"`, `deleteIntent`; schedules `"ESCALATION_<taskId>"` attempt `1` at `now + 5m` | Fallback routes to `setAndAllowWhileIdle` with `EXTRA_ESCALATION_ATTEMPT=1` |
| **Follow-up Cap Reached** | `attempt=3`, `maxReFires=3` | Posts `"Reminder: <title>"` / `"Follow-up 3 of 3 • Ready when you are"`; `BoundedEscalationEngine` returns `shouldScheduleNext=false` | No attempt `4` scheduled |
| **Policy Lowered Mid-Sequence** | `attempt=2`, user lowered `maxReFires` to `1` | Coordinator sees `attempt > policy.maxReFires`, cancels `"ESCALATION_<taskId>"`, returns `Suppress` | Safe halt without `"2 of 1"` |
| **Tap "Got it" or Swipe Dismiss** | `ACTION_ACKNOWLEDGE_TASK` on `NotificationActionReceiver` | Marks `ack=true` (synchronized `commit()`), cancels `"ESCALATION_<taskId>"`, cancels OS notification; task stays `PENDING` | Idempotent if already cancelled |
| **Tap "Mark Done"** | `ACTION_COMPLETE_TASK` on `NotificationActionReceiver` | Runs `CompleteTaskUseCase(taskId)`, clears `ack`, cancels `"TASK_<id>"` & `"ESCALATION_<id>"`, cancels notification, releases Urgent slot | `runCatching` suppresses missing/completed errors & still dismisses notification |
| **Stale / Non-Existent Task Fire** | Task `COMPLETED`/`CANCELLED`/deleted or `ack=true` | Coordinator cancels `"TASK_<id>"` & `"ESCALATION_<id>"`, skips `notify()`, returns `Suppress` | Safe no-op without crash |
| **Task Edited or Carried Forward** | `SaveTaskUseCase` / `CarryForwardTasksUseCase` runs | Calls `clearAcknowledgement(taskId)`, cancels old `"TASK_*"` + `"ESCALATION_*"` alarms, reschedules if future | Past times rejected by `ExactAlarmScheduler` |
| **Standalone / `taskId == null` Fire** | Direct test broadcast with `taskId == null` | Coordinator returns direct `PostNotification` (`id = alarmId.hashCode()`) without Room lookup or escalation | Safe fallback |
| **Settings Cadence Updated** | Interval `[3,5,10,15]`, Re-fires `[0,1,2,3,5]` | Updates `EscalationStateRepository`, emits `ShowSnackbar`, future triggers adopt new policy | Out-of-bounds rejected via `require(...)` |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/EscalationPolicy.kt` -- `[Create]` `data class EscalationPolicy(val retryIntervalMinutes: Int = 5, val maxReFires: Int = 3)` with `init { require(retryIntervalMinutes in 1..60); require(maxReFires in 0..5) }`.
- `app/src/main/java/com/personaltracker/domain/engine/BoundedEscalationEngine.kt` -- `[Create]` Pure Kotlin engine: `EscalationDecision(val shouldScheduleNext: Boolean, val nextAttemptNumber: Int = 0, val nextTriggerEpochMillis: Long = 0L, val haltReason: String? = null)` and `fun evaluateNextEscalation(taskId: String, deliveryIntensity: String, taskState: String, currentAttemptNumber: Int, isAcknowledged: Boolean, currentEpochMillis: Long, policy: EscalationPolicy = EscalationPolicy()): EscalationDecision`.
- `app/src/main/java/com/personaltracker/domain/repository/EscalationStateRepository.kt` -- `[Create]` Interface with `fun observePolicy(): Flow<EscalationPolicy>`, `fun getPolicy(): EscalationPolicy`, `fun updatePolicy(policy: EscalationPolicy)`, `fun isAcknowledged(taskId: String): Boolean`, `fun markAcknowledged(taskId: String)`, `fun clearAcknowledgement(taskId: String)`.
- `app/src/main/java/com/personaltracker/platform/notification/SharedPrefsEscalationStateRepository.kt` -- `[Create]` `@Singleton` synchronized `SharedPreferences` (`commit()` writes) + `MutableStateFlow<EscalationPolicy>` implementation.
- `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` & `PlatformModule.kt` -- `[Modify]` Provide singleton `BoundedEscalationEngine` in `DomainEngineModule`, and bind `EscalationStateRepository` + provide `TaskAlarmSchedulerImpl(exactAlarmScheduler, { NotificationManagerCompat.from(context).cancel(it) })` in `PlatformModule`.
- `app/src/main/java/com/personaltracker/platform/alarm/ExactAlarmScheduler.kt` & `TaskAlarmSchedulerImpl.kt` -- `[Modify]` Add `val escalationAttempt: Int = 0` to `ScheduledAlarm` and `EXTRA_ESCALATION_ATTEMPT = "extra_escalation_attempt"`; propagate `EXTRA_ESCALATION_ATTEMPT` in **both** `scheduleExact` and `scheduleInexactFallback`. Keep `ExactAlarmScheduler(delegate)` constructor unchanged. Add `scheduleEscalation(task: Task, attemptNumber: Int, triggerEpochMillis: Long): Boolean` (`"ESCALATION_${task.id}"`) and `cancelEscalationAlarm(taskId: String)`. In `TaskAlarmSchedulerImpl(exactAlarmScheduler, notificationCanceller: (Int) -> Unit = {})`, cancel `"TASK_$rawId"`, `"ESCALATION_$rawId"`, and call `notificationCanceller(rawId.hashCode())`.
- `app/src/main/java/com/personaltracker/domain/usecase/SaveTaskUseCase.kt`, `CompleteTaskUseCase.kt`, `DeleteTaskUseCase.kt`, `DismissOverdueTasksUseCase.kt`, `CarryForwardTasksUseCase.kt` -- `[Modify]` Append optional `taskAlarmScheduler: TaskAlarmScheduler? = null` (where missing) and `escalationStateRepository: EscalationStateRepository? = null` to end of constructors (preserving all existing test calls) to cancel alarms and call `clearAcknowledgement(taskId)` on lifecycle changes.
- `app/src/main/java/com/personaltracker/platform/alarm/EscalationAlarmCoordinator.kt` -- `[Create]` `@Singleton` JVM-testable coordinator using `kotlinx.coroutines.sync.Mutex` (`mutex.withLock`) with `onAlarmTriggered(...) -> AlarmTriggerOutcome` and `onNotificationAction(action, taskId) -> NotificationActionOutcome`.
- `app/src/main/java/com/personaltracker/platform/alarm/AlarmNotificationReceiver.kt` -- `[Modify]` Execute `goAsync()` + `withTimeout(8_000L)` + `Dispatchers.IO` calling `EscalationAlarmCoordinator` via `@EntryPoint`; post `(taskId ?: alarmId).hashCode()` notification (`setOnlyAlertOnce(false)`); for `isUrgent && taskId != null`, attach distinct `PendingIntent`s (`requestCode = (taskId.hashCode() * 31) + {1..4}` + distinct `personaltracker://notification/$taskId/{open,complete,acknowledge,swipe_dismiss}` URIs). Note: tapping notification body (`setAutoCancel(true)`) opens `MainActivity` without halting escalation until `"Got it"`, swipe-dismiss, or completion.
- `app/src/main/java/com/personaltracker/platform/alarm/NotificationActionReceiver.kt` & `AndroidManifest.xml` -- `[Create/Modify]` Internal `BroadcastReceiver` (`exported="false"`) defining `ACTION_ACKNOWLEDGE_TASK = "com.personaltracker.action.ACKNOWLEDGE_TASK"`, `ACTION_COMPLETE_TASK = "com.personaltracker.action.COMPLETE_TASK"`, calling `coordinator.onNotificationAction(...)` inside `goAsync()` + `withTimeout(8_000L)` and invoking `NotificationManagerCompat.from(context).cancel(notificationId)`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt` -- `[Modify]` Add `val escalationPolicy: EscalationPolicy = EscalationPolicy()` to `SettingsUiState`, `data class UpdateEscalationPolicy(val retryIntervalMinutes: Int, val maxReFires: Int) : SettingsUiIntent`, optional trailing constructor param `escalationStateRepository: EscalationStateRepository? = null` in `SettingsViewModel`, and Card 4 `FilterChip` selectors (Intervals `[3, 5, 10, 15]`, Re-fires `[0, 1, 2, 3, 5]`) with 48dp width/height floor and `stringResource(...)`.
- `app/src/main/res/values/strings.xml` -- `[Modify]` Add `notification_escalation_title`, `notification_escalation_message`, `notification_action_acknowledge` (`"Got it"`), `notification_action_complete` (`"Mark Done"`), and Settings cadence labels.
- `app/src/test/java/com/personaltracker/domain/BoundedEscalationEngineTest.kt`, `platform/alarm/EscalationAlarmCoordinatorTest.kt`, `domain/CarryForwardUseCasesTest.kt`, `platform/TaskAlarmSchedulerImplTest.kt`, `platform/ExactAlarmSchedulerTest.kt`, `domain/TaskManagementUseCasesTest.kt`, `ui/SettingsViewModelTest.kt` -- `[Create/Modify]` Cover all I/O matrix cases, update `TaskAlarmSchedulerImplTest` assertions to `listOf("TASK_...", "ESCALATION_...")`, and test `"Mark Done"` Urgent slot release with `EvaluateTaskUrgencyScarcityUseCase`.

## Tasks & Acceptance

**Execution:**
- [ ] `domain/model/EscalationPolicy.kt`, `domain/engine/BoundedEscalationEngine.kt`, `domain/repository/EscalationStateRepository.kt`, `di/DomainEngineModule.kt` -- Create pure Kotlin policy, bounded engine, and repository contract -- Enforce AD-1/AD-3.
- [ ] `platform/notification/SharedPrefsEscalationStateRepository.kt`, `di/PlatformModule.kt` -- Implement `SharedPreferences` `commit()` store with `MutableStateFlow` and bind DI -- Ensure durable storage without Room migration.
- [ ] `platform/alarm/ExactAlarmScheduler.kt`, `TaskAlarmSchedulerImpl.kt`, use cases (`Save`, `Complete`, `Delete`, `DismissOverdue`, `CarryForward`) -- Propagate `EXTRA_ESCALATION_ATTEMPT` in both exact & fallback delegates, add dual alarm+notification cancellation, and clear stale acks in task lifecycle use cases -- Eliminate reboot/reschedule/fallback bugs.
- [ ] `platform/alarm/EscalationAlarmCoordinator.kt`, `AlarmNotificationReceiver.kt`, `NotificationActionReceiver.kt`, `AndroidManifest.xml`, `strings.xml` -- Build `Mutex`-serialized coordinator and `goAsync()` + `withTimeout(8_000L)` receivers with isolated `PendingIntent`s (`requestCode` + `Uri`) and AD-4 templates -- Prevent concurrency races and action collisions.
- [ ] `ui/screens/settings/SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt` -- Wire configurable cadence `FilterChip` controls (`[3,5,10,15]` min, `[0,1,2,3,5]` re-fires) with 48dp width/height floor -- Ensure AD-7 compliance and test compatibility.
- [ ] Unit test suites (`BoundedEscalationEngineTest`, `EscalationAlarmCoordinatorTest`, `TaskAlarmSchedulerImplTest`, `ExactAlarmSchedulerTest`, `CarryForwardUseCasesTest`, `TaskManagementUseCasesTest`, `SettingsViewModelTest`) -- Verify bounded escalation, concurrency safety, fallback propagation, quota release, and MVI settings updates -- Prove 100% pass rate on JVM.

**Acceptance Criteria:**
- Given an uncompleted, unacknowledged `URGENT` task, when triggers fire, then bounded escalation fires at `policy.retryIntervalMinutes` up to `policy.maxReFires` and halts cleanly afterward.
- Given `"Got it"`, swipe-dismiss, or in-app/background task completion/deletion/dismissal, when handled, then pending alarms halt immediately, notifications dismiss cleanly, and completion/cancellation frees the Urgent slot in `EvaluateTaskUrgencyScarcityUseCase`.
- Given zero occurrences of the restricted 3-letter word (`n-a-g`) across code, strings, comments, logs, and tests, `domain_purity_check.py`, `lint_spine.py`, `.\gradlew test`, and `.\gradlew lintDebug` all pass cleanly.

## Spec Change Log

- **Iteration 1 (2026-11-25):** Resolved Goldfish Swarm findings (`PASS_WITH_NOTES` / `FAIL` / `NO-GO`): (1) serialized `EscalationAlarmCoordinator` with `Mutex` and `withTimeout(8_000L)` around `goAsync()`; (2) removed ack-clearing from `ExactAlarmScheduler.scheduleForTask` to protect reboot state, assigning ack clearing exclusively to task lifecycle use cases (`Save`, `Complete`, `Delete`, `Dismiss`, `CarryForward`); (3) added `EXTRA_ESCALATION_ATTEMPT` requirement to `scheduleInexactFallback` to prevent unbounded fallback loops; (4) mandated distinct `PendingIntent` request codes (`+1..+4`) and `Uri` data + explicit `NotificationManager.cancel`; (5) standardized synchronous `commit()` persistence in `SharedPrefsEscalationStateRepository`, mid-sequence `attempt > policy.maxReFires` suppression, 48dp width/height chip floor, exact Kotlin/Intent contracts, and updated `TaskAlarmSchedulerImplTest` assertions. KEEP: `AD-1` domain purity, non-punitive `AD-4` copy, and zero Room migration strategy.

## Design Notes

- **In-Place Notification Updates & Separate Alarm Request Codes:** Primary alarms use `"TASK_<taskId>"` and follow-ups use `"ESCALATION_<taskId>"` (leaving reboot primary scheduling in `BootRecoveryCoordinator` intact), while both share OS notification ID `taskId.hashCode()` for single-slot updates.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All 8 H0 components valid`
- `.\gradlew test` -- expected: All JVM unit tests passing clean
- `.\gradlew lintDebug` -- expected: 0 errors
- `adb devices -l` -- expected: Gate 6 Artemis verification if `10BD9D24YF000BE` is connected
