# Architectural Drift Report: Story 5.1 (Tiered Notification Channels & Exact Alarm Scheduling Engine)

**Target Spec:** `docs/designs/drafts/spec-5-1-tiered-notification-channels-exact-alarm-scheduling-engine.md` (Iteration 2)  
**Date:** 2026-11-23  
**Audit Scope:** `app/src/main/` Kotlin production sources vs approved specification  
**Drift Verdict:** **ZERO DRIFT (100% SPEC CONFORMANCE)**

---

## 1. Architectural Spine Invariant Audits (AD-1 to AD-8)

| Invariant | Standard | Implementation Verification | Status |
| :--- | :--- | :--- | :--- |
| **AD-1: Domain Purity** | Zero `android.*` or `androidx.*` dependencies in `domain/` layer. Pure Kotlin contracts. | `TaskAlarmScheduler.kt` defines `fun canScheduleExactAlarms(): Boolean` in pure Kotlin with 0 Android framework imports. `domain_purity_check.py` passed with 0 findings. | **PASSED** |
| **AD-2: Unidirectional Reactive MVI** | Single immutable `UiState`, sealed `UiIntent`, Channel-backed `UiEffect`. Stateless `Content` composables. | `SettingsUiState` augmented with `isExactAlarmGranted` and `isNotificationsEnabled`. Handled via `OpenExactAlarmSettings` and `OpenNotificationSettings` in `SettingsViewModel`. | **PASSED** |
| **AD-3: Defensive OS & Permission Safety** | Graceful handling of Android 12+ (API 31+) and Android 14+ (API 34+) exact alarm restrictions. | Differentiated scheduling (`ROUTINE` inexact idle-wake, `IMPORTANT` exact idle-wake, `URGENT` alarm clock); single-responsibility `SecurityException` catch falling back to `scheduleInexactFallback()`; past-time rejection. | **PASSED** |
| **AD-4: Non-Punishing Guilt-Free UX** | Zero guilt, shame, or nag words across all notification channels, descriptions, and UI copy. | Status badge uses supportive `"Flexible Timing Active • Battery friendly"` copy. Scanned all strings, comments, and code: 0 occurrences of the word "nag" (FR-5.4). | **PASSED** |
| **AD-7: Design Token Conformance & Touch Targets** | 100% tokens from `PersonalTrackerTheme`. Zero raw hex or `dp`/`sp` literals. 48dp touch floor. | "Notifications & Alarm Timing" surface card in `SettingsScreen.kt` resolves exclusively from `colors`, `typography`, `spacing`, and `shapes`. All buttons enforce `minTouchTarget` (48dp). | **PASSED** |

---

## 2. File & Component Level Drift Analysis

### A. Platform Notification Tiering: `TieredNotificationManager.kt` `[Created]`
- **Spec Contract:**
  - Register 3 notification channels on API 26+: `channel_routine` (`IMPORTANCE_LOW`), `channel_important` (`IMPORTANCE_DEFAULT`), and `channel_urgent` (`IMPORTANCE_HIGH` with `URGENT_VIBRATION_PATTERN` `longArrayOf(0, 250, 150, 250)` and default sound URI).
  - Purge legacy `task_reminders_channel`.
  - Static helper `TieredNotificationManager.Companion.getChannelIdForIntensity()` mapping intensity to channel ID without requiring Android Context.
- **Live Implementation:** Matches 1:1 with specification. Verified in `TieredNotificationManagerTest.kt`.

### B. Application Cold-Start Registration: `PersonalTrackerApplication.kt` `[Modified]`
- **Spec Contract:**
  - Inject `TieredNotificationManager` and call `createNotificationChannels()` in `onCreate()` after SQLCipher native initialization.
- **Live Implementation:** Matches 1:1 with specification.

### C. Alarm Scheduling & Fallbacks: `ExactAlarmScheduler.kt` `[Modified]`
- **Spec Contract:**
  - Past-time rejection: `triggerEpochMillis <= System.currentTimeMillis()` returns `false`.
  - Tier differentiation: `ROUTINE` schedules directly via `scheduleInexactFallback()` and returns `true` (eliminating lock screen clock icon clutter).
  - `IMPORTANT` schedules via `setExactAndAllowWhileIdle()`; `URGENT` schedules via `setAlarmClock()`.
  - Single boundary: `SystemAlarmManagerDelegate.scheduleExact()` allows `SecurityException` to bubble up; `ExactAlarmScheduler.scheduleAlarm()` catches `SecurityException`, triggers `delegate.scheduleInexactFallback()`, and returns `false`.
- **Live Implementation:** Matches 1:1 with specification. Verified across all edge cases in `ExactAlarmSchedulerTest.kt`.

### D. Receiver Channel Routing: `AlarmNotificationReceiver.kt` `[Modified]`
- **Spec Contract:**
  - Resolve channel via `TieredNotificationManager.getChannelIdForIntensity()`.
  - Verify `NotificationManagerCompat.areNotificationsEnabled()` and `POST_NOTIFICATIONS` permission before calling `notify()`.
  - Configure `PRIORITY_HIGH` and companion double-pulse haptics for `URGENT`.
  - Use non-punitive fallback copy.
- **Live Implementation:** Matches 1:1 with specification. Zero Android Lint `MissingPermission` errors.

### E. Pure Domain & Implementation: `TaskAlarmScheduler.kt` & `TaskAlarmSchedulerImpl.kt` `[Modified]`
- **Spec Contract:**
  - Add `fun canScheduleExactAlarms(): Boolean` in pure Kotlin domain interface.
  - Implement in `TaskAlarmSchedulerImpl` delegating to `exactAlarmScheduler.canScheduleExact()`.
- **Live Implementation:** Matches 1:1 with specification. Verified pure Kotlin domain isolation.

### F. Presentation MVI: `SettingsViewModel.kt`, `SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsScreen.kt` `[Modified]`
- **Spec Contract:**
  - Add `isExactAlarmGranted` and `isNotificationsEnabled` to `SettingsUiState`.
  - Handle `OpenExactAlarmSettings` and `OpenNotificationSettings` in `SettingsViewModel` and preserve state during database flow updates.
  - Attach `package:` URI for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` and `EXTRA_APP_PACKAGE` for `ACTION_APP_NOTIFICATION_SETTINGS` in `SettingsRoute`.
  - Render "Notifications & Alarm Timing" surface card with 48dp minimum touch targets and 0 raw literals.
- **Live Implementation:** Matches 1:1 with specification. Verified in `SettingsViewModelTest.kt`.

---

## 3. Drift Verdict

**Zero architectural drift detected (0.0%).** All implemented code files, domain models, alarm scheduling mechanics, UI composables, and MVI contracts strictly conform to `spec-5-1-tiered-notification-channels-exact-alarm-scheduling-engine.md` (Iteration 2).
