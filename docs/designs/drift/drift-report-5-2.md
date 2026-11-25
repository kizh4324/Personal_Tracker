# Architectural Drift Report: Story 5.2 (Acknowledgement-Based Bounded Escalation Engine)

**Target Spec:** `docs/designs/drafts/spec-5-2-acknowledgement-based-bounded-escalation-engine.md` (Iteration 1)  
**Date:** 2026-11-25  
**Audit Scope:** `app/src/main/` Kotlin production sources, `AndroidManifest.xml`, `strings.xml`, and unit tests vs approved specification  
**Drift Verdict:** **ZERO DRIFT (100% SPEC CONFORMANCE)**

---

## 1. Architectural Spine Invariant Audits (AD-1 to AD-8)

| Invariant | Standard | Implementation Verification | Status |
| :--- | :--- | :--- | :--- |
| **AD-1: Domain Purity** | Zero `android.*` or `androidx.*` dependencies in `domain/` layer. Pure Kotlin contracts. | `EscalationPolicy.kt`, `BoundedEscalationEngine.kt`, `EscalationStateRepository.kt`, and all 5 updated lifecycle use cases contain 0 `android.*` or `androidx.*` imports. `domain_purity_check.py` passed cleanly. | **PASSED** |
| **AD-2: Unidirectional Reactive MVI** | Single immutable `UiState`, sealed `UiIntent`, Channel-backed `UiEffect`. Stateless `Content` composables. | `SettingsUiState` exposes `escalationPolicy: EscalationPolicy`, `SettingsUiIntent` exposes `UpdateEscalationPolicy(retryIntervalMinutes, maxReFires)`, and `SettingsViewModel` updates repository + state + emits `ShowSnackbar`. | **PASSED** |
| **AD-3: Concurrency & Storage Integrity** | No uncoordinated shared state or silent data loss across BroadcastReceivers. | `EscalationAlarmCoordinator` guards trigger evaluation and notification action handling under `Mutex.withLock`. `SharedPrefsEscalationStateRepository` uses synchronized `commit()` writes and a `MutableStateFlow<EscalationPolicy>`, avoiding unnecessary Room schema bumps. | **PASSED** |
| **AD-4: Non-Punishing Guilt-Free UX** | Zero guilt, shame, or restricted `n-a-g` words across notification copy, strings, comments, logs, and tests. | Follow-up copy uses `"Reminder: %1$s"` and `"Follow-up %1$d of %2$d • Ready when you are"`. Action labels use `"Got it"` and `"Mark Done"`. Restricted-word scan across all Story 5.2 sources, specs, strings, and tests returned 0 hits. | **PASSED** |
| **AD-7: Design Token Conformance & Touch Targets** | 100% tokens from `PersonalTrackerTheme`. Zero raw hex or `dp`/`sp` literals. 48dp touch floor. | Settings cadence `FilterChip` selectors (`[3, 5, 10, 15]` min and `[0, 1, 2, 3, 5]` re-fires) use `PersonalTrackerTheme` colors/typography/spacing, `stringResource(...)`, and `.defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)`. | **PASSED** |

---

## 2. File & Component Level Drift Analysis

### A. Pure Domain Models, Engine & Contract `[Created]`
- **`EscalationPolicy.kt`:** `data class EscalationPolicy(val retryIntervalMinutes: Int = 5, val maxReFires: Int = 3)` with `require(retryIntervalMinutes in 1..60)` and `require(maxReFires in 0..5)`. Matches 1:1.
- **`BoundedEscalationEngine.kt`:** Pure Kotlin engine returning `EscalationDecision` (`shouldScheduleNext`, `nextAttemptNumber`, `nextTriggerEpochMillis`, `haltReason`). Only `URGENT` + `PENDING` + `!isAcknowledged` + `currentAttemptNumber in 0 until policy.maxReFires` schedules follow-up attempt `currentAttemptNumber + 1` at `currentEpochMillis + policy.retryIntervalMinutes * 60_000L`. Matches 1:1.
- **`EscalationStateRepository.kt`:** Interface exposing `observePolicy()`, `getPolicy()`, `updatePolicy()`, `isAcknowledged()`, `markAcknowledged()`, and `clearAcknowledgement()`. Matches 1:1.
- **`DomainEngineModule.kt` `[Modified]`:** Provides `@Singleton` `BoundedEscalationEngine`. Matches 1:1.

### B. Persistence & DI Binding `[Created/Modified]`
- **`SharedPrefsEscalationStateRepository.kt`:** `@Singleton` synchronized `SharedPreferences` implementation using `commit()` writes for crash-safe BroadcastReceiver consistency and a `MutableStateFlow<EscalationPolicy>` stream. Matches 1:1.
- **`PlatformModule.kt` `[Modified]`:** Binds `EscalationStateRepository` to `SharedPrefsEscalationStateRepository` and wires `TaskAlarmSchedulerImpl` with `NotificationManagerCompat.from(context).cancel(notificationId)`. Matches 1:1.

### C. Alarm Scheduling & Lifecycle Use Cases `[Modified]`
- **`ExactAlarmScheduler.kt`:** Adds `escalationAttempt: Int = 0` to `ScheduledAlarm`, `EXTRA_ESCALATION_ATTEMPT = "extra_escalation_attempt"`, propagates `EXTRA_ESCALATION_ATTEMPT` in both `scheduleExact()` and `scheduleInexactFallback()`, adds `scheduleEscalation()` (`"ESCALATION_${task.id}"`) and `cancelEscalationAlarm()`, and keeps `ExactAlarmScheduler(delegate)` constructor intact without clearing ack on `scheduleForTask()`. Matches 1:1.
- **`TaskAlarmSchedulerImpl.kt`:** Cancels both `"TASK_$rawId"` and `"ESCALATION_$rawId"` plus invokes `notificationCanceller(rawId.hashCode())`. Matches 1:1.
- **Lifecycle Use Cases (`SaveTaskUseCase`, `CompleteTaskUseCase`, `DeleteTaskUseCase`, `DismissOverdueTasksUseCase`, `CarryForwardTasksUseCase`):** All accept optional trailing `escalationStateRepository: EscalationStateRepository? = null` (plus binary-compatible secondary constructors) and call `clearAcknowledgement(...)` alongside alarm cancellation/rescheduling. Matches 1:1.

### D. Coordinator, Receivers, Manifest & Strings `[Created/Modified]`
- **`EscalationAlarmCoordinator.kt`:** `@Singleton` JVM-testable coordinator serializing `onAlarmTriggered(...)` and `onNotificationAction(...)` via `Mutex.withLock`. Enforces task lookup, `PENDING` check, acknowledgement check, mid-sequence `escalationAttempt > policy.maxReFires` suppression, bounded follow-up scheduling, `"Got it"` ack + escalation cancel without mutating `Task`, and `"Mark Done"` atomic completion + ack clear + dual alarm cancel + Urgent quota release. Matches 1:1.
- **`AlarmNotificationReceiver.kt`:** Uses `goAsync()` + `withTimeout(8_000L)` on `Dispatchers.IO`, verifies `areNotificationsEnabled()` & `POST_NOTIFICATIONS`, posts `(taskId ?: alarmId).hashCode()` with `setOnlyAlertOnce(false)`, and attaches isolated `PendingIntent`s (`baseCode + 1..4` and distinct `personaltracker://notification/$taskId/{open,complete,acknowledge,swipe_dismiss}` URIs). Matches 1:1.
- **`NotificationActionReceiver.kt` & `AndroidManifest.xml`:** Internal `BroadcastReceiver` (`exported="false"`) handling `ACTION_ACKNOWLEDGE_TASK` and `ACTION_COMPLETE_TASK` inside `goAsync()` + `withTimeout(8_000L)` and cancelling `outcome.notificationId` via `NotificationManagerCompat`. Matches 1:1.
- **`strings.xml`:** Defines all calm escalation, action, and Settings string resources. Matches 1:1.

### E. Presentation & Tests `[Created/Modified]`
- **`SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`:** Exposes `escalationPolicy`, `UpdateEscalationPolicy`, repository sync + immediate `_uiState` update, and Card 4 `FilterChip` controls with `minTouchTarget` (48dp) width & height floor. Matches 1:1.
- **Tests (`BoundedEscalationEngineTest`, `EscalationAlarmCoordinatorTest`, `ExactAlarmSchedulerTest`, `TaskAlarmSchedulerImplTest`, `CarryForwardUseCasesTest`, `TaskManagementUseCasesTest`, `SettingsViewModelTest`):** All 540 JVM unit tests (Debug & Release) pass with 0 failures and 0 skipped.

---

## 3. Drift Verdict

**Zero architectural drift detected (0.0%).** All code, DI wiring, receivers, UI controls, and verification gates conform to `spec-5-2-acknowledgement-based-bounded-escalation-engine.md` (Iteration 1).
