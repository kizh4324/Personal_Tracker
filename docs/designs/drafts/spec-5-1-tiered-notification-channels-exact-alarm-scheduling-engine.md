---
title: 'Story 5.1: Tiered Notification Channels & Exact Alarm Scheduling Engine'
type: 'feature'
created: '2026-11-16'
status: 'draft'
baseline_commit: 'ad241ac'
review_loop_iteration: 2
context:
  - '_bmad-output/planning-artifacts/epics.md'
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md'
  - '.harness2/EXPERIENCE.md'
  - 'HANDOFF.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 
Task reminders and scheduled routine alerts currently use a single monolithic notification channel (`task_reminders_channel` with `IMPORTANCE_DEFAULT`), collapsing quiet routine reminders, standard tasks, and urgent alerts into the identical OS alert level. Furthermore, `ExactAlarmScheduler` applies `setAlarmClock()` indiscriminately to all tasks, permanently cluttering the user's Android status bar and lock screen with alarm icons even for quiet routines. Additionally, `ExactAlarmScheduler` lacks defensive `SecurityException` guards and graceful fallback handling on Android 12+ (API 31+) and Android 14+ (API 34+) when exact alarm scheduling permissions (`SCHEDULE_EXACT_ALARM`) are revoked or restricted by OEM battery optimization buckets. In addition, `SettingsScreen` lacks visibility and controls for notification channels, exact alarm permission status, and `POST_NOTIFICATIONS` status.

**Approach:** 
1. **Tiered Notification Architecture (`TieredNotificationManager.kt`):** Implement a dedicated platform singleton registering 3 distinct Android notification channels on API 26+ (with legacy `task_reminders_channel` cleanly purged):
   - `channel_routine`: `IMPORTANCE_LOW` (quiet, no sound, no vibration, in-tray only).
   - `channel_important`: `IMPORTANCE_DEFAULT` (standard OS notification chime + default vibration).
   - `channel_urgent`: `IMPORTANCE_HIGH` (heads-up banner, high priority, system default notification sound, distinctive companion heavy double-pulse haptics `longArrayOf(0, 250, 150, 250)`).
2. **Differentiated Alarm Scheduling & Defensive Fallbacks (`ExactAlarmScheduler.kt`, `AlarmNotificationReceiver.kt`):**
   - Differentiate alarm scheduling by tier to avoid lock screen clutter:
     - `ROUTINE`: Inexact scheduling via `setAndAllowWhileIdle()` (preserves battery; zero status bar clock icon).
     - `IMPORTANT`: Exact scheduling via `setExactAndAllowWhileIdle()` (sub-second precision without persistent clock icon).
     - `URGENT`: Exact scheduling via `setAlarmClock()` (clock-level wakeup with pending intent).
   - Past-time defense: Reject any alarm with `triggerEpochMillis <= System.currentTimeMillis()` with non-fatal warning log and return `false`.
   - Single-responsibility `SecurityException` boundary: `SystemAlarmManagerDelegate.scheduleExact()` lets `SecurityException` bubble up; `ExactAlarmScheduler.scheduleAlarm()` catches `SecurityException`, triggers `delegate.scheduleInexactFallback(alarm)`, logs a non-fatal warning, and returns `false`.
   - `AlarmNotificationReceiver`: Resolves target channel via pure static method `TieredNotificationManager.Companion.getChannelIdForIntensity(intensity)` (zero Hilt injection boilerplate), verifies `POST_NOTIFICATIONS` permission before calling `notify()`, and posts non-punitive copy (AD-4).
3. **Pure Domain Contract & Settings MVI Wiring (`TaskAlarmScheduler.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`):**
   - Extend the pure Kotlin domain interface `TaskAlarmScheduler` with `fun canScheduleExactAlarms(): Boolean` (AD-1).
   - Wire exact alarm and notification permission status (`isExactAlarmGranted`, `isNotificationsEnabled`) into `SettingsUiState`, `SettingsViewModel` (preserving state during database flow updates), and `SettingsScreen`.
   - Enhance `SettingsRoute` to attach required URI (`package:${context.packageName}`) for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` and extra (`EXTRA_APP_PACKAGE`) for `ACTION_APP_NOTIFICATION_SETTINGS`.
   - Render "Notifications & Alarm Timing" surface card in `SettingsScreen` using 100% `PersonalTrackerTheme` semantic tokens, 48dp minimum touch target floor, and non-punitive supportive copy (AD-4, AD-7).

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** `TaskAlarmScheduler`, alarm timing calculations, and delivery intensity types in `com.personaltracker.domain` must remain 100% pure Kotlin with zero imports from `android.*` or `androidx.*`.
- **AD-4 Non-Punishing Guilt-Free UX:** Zero guilt, shame, or penalty copy across all notification channel names, descriptions, titles, and body strings. The word "nag" is strictly prohibited across all code, strings, comments, and logs (FR-5.4). Inexact fallback status must be framed supportively: "Flexible Timing Active • Battery friendly" (never labeled as a warning or error).
- **AD-3 Defensive OS & Permission Safety:** API 31+ `AlarmManager.canScheduleExactAlarms()` check must guard exact alarm scheduling. A defensive `try/catch (e: SecurityException)` in `ExactAlarmScheduler.scheduleAlarm()` must catch permission revocation and execute `scheduleInexactFallback()` without crashing.
- **Strict Tier Mapping:**
  - `ROUTINE`: Channel ID `channel_routine`, `NotificationManager.IMPORTANCE_LOW` (quiet, no sound, no vibration, in-tray only).
  - `IMPORTANT`: Channel ID `channel_important`, `NotificationManager.IMPORTANCE_DEFAULT` (standard OS notification chime + standard vibration).
  - `URGENT`: Channel ID `channel_urgent`, `NotificationManager.IMPORTANCE_HIGH` (heads-up banner, high priority, system default notification sound, distinctive companion heavy double-pulse haptics `longArrayOf(0, 250, 150, 250)`).
- **AD-7 Token Conformance & Touch Targets:** 100% of colors, dimensions, and typography in `SettingsScreen` must resolve from `PersonalTrackerTheme` semantic tokens (zero raw hex/dp/sp literals). All interactive buttons and filter chips must enforce a 48dp minimum touch target floor (`spacing.minTouchTarget`).
- **Strict "No Code During Design" Rule:** Do not write or modify application Kotlin/Compose/Room code until the design checkpoint is reviewed and approved by the human learner (Elephant Invariant & VibeWise Teaching Contract).

**Resolved Decisions:**
- *Audio Asset Strategy:* Use Android system default notification sound URI (`RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)`) for `channel_urgent`. No custom `.ogg` assets bundled in Story 5.1.
- *Tiered Alarm Execution Strategy:*
  - `ROUTINE` tasks: Scheduled via inexact `setAndAllowWhileIdle()` to eliminate status bar icon clutter.
  - `IMPORTANT` tasks: Scheduled via `setExactAndAllowWhileIdle()` for exact timing without clock icon.
  - `URGENT` tasks: Scheduled via `setAlarmClock()` for clock-level wakeup.
- *Legacy Channel Migration:* Delete obsolete `task_reminders_channel` during `TieredNotificationManager.createNotificationChannels()` initialization.

**Never:**
- Never use `IMPORTANCE_NONE` or silently drop scheduled alerts without fallback.
- Never hardcode the word "nag" or derogatory language anywhere in notification templates.
- Never crash on missing `SCHEDULE_EXACT_ALARM` permission; always execute the inexact fallback.
- Never allow background alarm receivers to call `notificationManager.notify()` without verifying `POST_NOTIFICATIONS` permission on API 33+.
- Never use raw hex colors or hardcoded `dp` in `SettingsScreen` or component UI.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **App Startup / Channel Registration** | App cold boot (`PersonalTrackerApplication.onCreate`) | `TieredNotificationManager.createNotificationChannels()` registers `channel_routine`, `channel_important`, and `channel_urgent`; deletes legacy `task_reminders_channel` | Idempotent on API 26+; no-op on older APIs |
| **Routine Task Reminder Fired** | `AlarmNotificationReceiver` receives intent with `deliveryIntensity = "ROUTINE"` | Notification posted to `channel_routine` with `IMPORTANCE_LOW`; silent, no heads-up, no vibration; text: "Ready whenever you want to begin." | Fallback to default gentle message if text is null |
| **Important Task Reminder Fired** | `AlarmNotificationReceiver` receives intent with `deliveryIntensity = "IMPORTANT"` | Notification posted to `channel_important` with `IMPORTANCE_DEFAULT`; standard chime + vibration | Fallback to default message if text is null |
| **Urgent Task Reminder Fired** | `AlarmNotificationReceiver` receives intent with `deliveryIntensity = "URGENT"` | Notification posted to `channel_urgent` with `IMPORTANCE_HIGH`; heads-up banner, heavy double-pulse haptic pattern (`0, 250, 150, 250`) | Fallback to standard vibration if vibrator unavailable |
| **Exact Alarm Permission Granted (API 31+)** | `canScheduleExactAlarms() == true`, scheduled time in future | Registered via `setExactAndAllowWhileIdle()` (Important) or `setAlarmClock()` (Urgent); returns `true` | Catch `SecurityException` and immediately route to `scheduleInexactFallback()` |
| **Exact Alarm Permission Denied (API 31+)** | `canScheduleExactAlarms() == false` | Automatically routes to `scheduleInexactFallback()` using `setAndAllowWhileIdle()`; returns `false` | Alert scheduled without crashing; status reflected in UI |
| **SecurityException on Runtime Revocation** | `canScheduleExactAlarms() == true` but OS revokes mid-call | `ExactAlarmScheduler.scheduleAlarm()` catches `SecurityException`; immediately invokes `scheduleInexactFallback(alarm)`; returns `false` | Log non-fatal warning; zero app crash |
| **Alarm Time in Past** | `triggerEpochMillis <= System.currentTimeMillis()` | Rejected with warning log; does not schedule past alarm; returns `false` | Return `false` gracefully |
| **POST_NOTIFICATIONS Denied (API 33+)** | App notification permission revoked in system settings | `AlarmNotificationReceiver` skips `notify()` call; `SettingsScreen` surfaces notification warning banner + configure button | Prevents lint `MissingPermission` failure |
| **Settings Screen Permission Inspection** | User navigates to Settings screen on API 31+ | Displays exact alarm status: "Precise Reminders: Allowed" (Success) vs "Flexible Timing Active • Battery friendly" (Neutral) + 48dp action button | Dispatches `OpenExactAlarmSettings` intent to system settings |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/platform/notification/TieredNotificationManager.kt` -- `[Create]`
  Platform singleton responsible for registering and managing the 3 notification channels on API 26+:
  ```kotlin
  @Singleton
  class TieredNotificationManager @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
      fun createNotificationChannels()

      companion object {
          const val CHANNEL_ID_ROUTINE = "channel_routine"
          const val CHANNEL_ID_IMPORTANT = "channel_important"
          const val CHANNEL_ID_URGENT = "channel_urgent"
          const val LEGACY_CHANNEL_ID = "task_reminders_channel"
          val URGENT_VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

          fun getChannelIdForIntensity(intensity: String?): String {
              return when (intensity?.uppercase()) {
                  "URGENT" -> CHANNEL_ID_URGENT
                  "IMPORTANT" -> CHANNEL_ID_IMPORTANT
                  else -> CHANNEL_ID_ROUTINE
              }
          }
      }
  }
  ```
- `app/src/main/java/com/personaltracker/PersonalTrackerApplication.kt` -- `[Modify]`
  Inject `TieredNotificationManager` and invoke `createNotificationChannels()` in `onCreate()` after SQLCipher native initialization.
- `app/src/main/java/com/personaltracker/platform/alarm/ExactAlarmScheduler.kt` -- `[Modify]`
  - In `SystemAlarmManagerDelegate.scheduleExact(alarm)`: Differentiate between `URGENT` (`setAlarmClock`) and `IMPORTANT` (`setExactAndAllowWhileIdle`). Allow `SecurityException` to bubble up without swallowing.
  - In `ExactAlarmScheduler.scheduleAlarm(alarm)`:
    - Guard: If `alarm.triggerEpochMillis <= System.currentTimeMillis()`, return `false`.
    - If `alarm.deliveryIntensity.uppercase() == "ROUTINE"`, call `delegate.scheduleInexactFallback(alarm)` and return `true`.
    - For `IMPORTANT` / `URGENT`: Check `delegate.canScheduleExactAlarms()`. If true, wrap `delegate.scheduleExact(alarm)` in `try/catch (e: SecurityException)`. On catch, call `delegate.scheduleInexactFallback(alarm)` and return `false`. If false, call `delegate.scheduleInexactFallback(alarm)` and return `false`.
  - Expose `canScheduleExact(): Boolean = delegate.canScheduleExactAlarms()`.
- `app/src/main/java/com/personaltracker/platform/alarm/AlarmNotificationReceiver.kt` -- `[Modify]`
  - Use `TieredNotificationManager.getChannelIdForIntensity(intensity)` to resolve channel ID.
  - Apply double-pulse haptics (`TieredNotificationManager.URGENT_VIBRATION_PATTERN`) and `PRIORITY_HIGH` heads-up notification for `URGENT`.
  - Verify `NotificationManagerCompat.from(context).areNotificationsEnabled()` before calling `notify()` to prevent lint `MissingPermission` error.
  - Use non-punitive strings (AD-4).
- `app/src/main/java/com/personaltracker/domain/alarm/TaskAlarmScheduler.kt` -- `[Modify]`
  - Add `fun canScheduleExactAlarms(): Boolean` to pure Kotlin domain interface (AD-1).
- `app/src/main/java/com/personaltracker/platform/alarm/TaskAlarmSchedulerImpl.kt` -- `[Modify]`
  - Implement `canScheduleExactAlarms()` delegating to `exactAlarmScheduler.canScheduleExact()`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` -- `[Modify]`
  - Add `val isExactAlarmGranted: Boolean = true` and `val isNotificationsEnabled: Boolean = true` to `SettingsUiState`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiIntent.kt` -- `[Modify]`
  - Add `object OpenExactAlarmSettings : SettingsUiIntent`
  - Add `object OpenNotificationSettings : SettingsUiIntent`
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- `[Modify]`
  - Inject `TaskAlarmScheduler`.
  - In `checkPermissions()`: Update `isExactAlarmGranted = taskAlarmScheduler.canScheduleExactAlarms()` and `isNotificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled()`.
  - In `observeSettingsData()`: Retain `isExactAlarmGranted` and `isNotificationsEnabled` in the combined `SettingsUiState`.
  - Handle `OpenExactAlarmSettings` (emitting `SettingsUiEffect.OpenSystemSetting(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)`) and `OpenNotificationSettings` (emitting `SettingsUiEffect.OpenSystemSetting(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`).
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- `[Modify]`
  - Update `SettingsRoute` effect handler for `OpenSystemSetting`:
    - For `ACTION_MANAGE_OVERLAY_PERMISSION` & `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`: `data = Uri.parse("package:${context.packageName}")`.
    - For `ACTION_APP_NOTIFICATION_SETTINGS`: `putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)`.
  - Add "Notifications & Alarm Timing" surface card in `SettingsScreenContent`:
    - Displays exact alarm status ("Precise Reminders: Allowed" vs "Flexible Timing Active • Battery friendly").
    - Displays notification permission status if disabled.
    - Renders 48dp action buttons to manage exact alarm and notification settings.
    - Renders 3 channel overview capsules (Routine, Important, Urgent) using 100% theme tokens (AD-7).
- `app/src/main/res/values/strings.xml` -- `[Modify]`
  - Add explicit non-punishing string keys:
    - `notification_channel_routine_name`: "Calm Routines"
    - `notification_channel_routine_description`: "Gentle, silent reminders for your daily habits and self-care."
    - `notification_channel_important_name`: "Standard Reminders"
    - `notification_channel_important_description`: "Timely prompts for your scheduled daily tasks."
    - `notification_channel_urgent_name`: "Time-Sensitive Alerts"
    - `notification_channel_urgent_description`: "Priority alerts for commitments requiring your immediate attention."
    - `notification_fallback_message`: "Ready whenever you want to begin."
    - `settings_notifications_title`: "Notifications & Alarm Timing"
    - `settings_exact_alarm_status_granted`: "Precise Reminders: Allowed"
    - `settings_exact_alarm_status_fallback`: "Flexible Timing Active • Battery friendly"
    - `settings_notifications_status_enabled`: "App Notifications: Allowed"
    - `settings_notifications_status_disabled`: "App Notifications: Disabled"
    - `settings_exact_alarm_button`: "Configure Exact Alarm Timing"
    - `settings_notification_channels_button`: "Configure Notification Channels"
- `app/src/test/java/com/personaltracker/platform/ExactAlarmSchedulerTest.kt` -- `[Modify]`
  - Update `FakeAlarmManagerDelegate` to simulate `SecurityException`.
  - Add unit tests verifying `SecurityException` catch and fallback to inexact scheduling.
  - Add unit test verifying past-time alarm rejection.
  - Add unit test verifying `ROUTINE` intensity routes directly to inexact fallback without status bar clutter.
- `app/src/test/java/com/personaltracker/platform/notification/TieredNotificationManagerTest.kt` -- `[Create]`
  - Test channel ID mapping, channel constants, and vibration pattern definitions.
- `app/src/test/java/com/personaltracker/ui/screens/settings/SettingsViewModelTest.kt` -- `[Modify / Create]`
  - Verify exact alarm state updates and intent handling in `SettingsViewModel`.

## Tasks & Acceptance

**Execution Plan:**
1. [ ] Create `TieredNotificationManager.kt` in `platform/notification/` defining channels `channel_routine` (Low), `channel_important` (Default), and `channel_urgent` (High) with haptics and non-punishing copy, purging legacy channel.
2. [ ] Inject and invoke `TieredNotificationManager.createNotificationChannels()` in `PersonalTrackerApplication.kt`.
3. [ ] Strengthen `SystemAlarmManagerDelegate` and `ExactAlarmScheduler.kt` with past-time rejection, differentiated tier scheduling, `SecurityException` protection, and verified inexact fallback.
4. [ ] Refactor `AlarmNotificationReceiver.kt` to route incoming alarms via `TieredNotificationManager.getChannelIdForIntensity()`, configuring heads-up priority and double-pulse haptics for `URGENT`, and guarding `notify()` with notification permission check.
5. [ ] Update pure Kotlin domain interface `TaskAlarmScheduler.kt` with `canScheduleExactAlarms(): Boolean` and implement in `TaskAlarmSchedulerImpl.kt`.
6. [ ] Update `SettingsUiState`, `SettingsUiIntent`, and `SettingsViewModel` with exact alarm and notification permission inspection and system settings navigation intents.
7. [ ] Update `SettingsRoute` effect handling with required URI and package extras, and add "Notifications & Alarm Timing" surface card to `SettingsScreen.kt` using 100% `PersonalTrackerTheme` tokens and 48dp minimum touch targets.
8. [ ] Update `strings.xml` with channel names and descriptions adhering strictly to AD-4 (zero guilt, zero "nag" copy).
9. [ ] Expand `ExactAlarmSchedulerTest.kt` and create `TieredNotificationManagerTest.kt` to achieve 100% test coverage for all edge cases.

**Acceptance Criteria:**
- Given the application initializes on Android 8.0+ (API 26+), when `PersonalTrackerApplication.onCreate` runs, then three distinct notification channels (`channel_routine`, `channel_important`, `channel_urgent`) are registered with `NotificationManager` with correct importance levels (`LOW`, `DEFAULT`, `HIGH`), and legacy `task_reminders_channel` is deleted.
- Given an alarm broadcast received with intensity `ROUTINE`, when posted, then it appears in `channel_routine` without sound or heads-up banner.
- Given an alarm broadcast received with intensity `IMPORTANT`, when posted, then it appears in `channel_important` with standard OS alert sound and vibration.
- Given an alarm broadcast received with intensity `URGENT`, when posted, then it appears in `channel_urgent` with heads-up display and distinct heavy double-pulse haptic pattern.
- Given a routine task, when scheduled, then it uses inexact `setAndAllowWhileIdle()` without displaying an alarm clock icon in the Android status bar.
- Given an alarm with a past timestamp (`triggerEpochMillis <= System.currentTimeMillis()`), when scheduled, then `scheduleAlarm` returns `false` and does not schedule a past alarm.
- Given a device where exact alarm scheduling permission is revoked or restricted (API 31+), when scheduling a task reminder, then `ExactAlarmScheduler` catches any `SecurityException`, registers the alert via inexact fallback, and does not crash.
- Given `SettingsScreen`, when rendered, then it displays exact alarm permission status ("Precise Reminders: Allowed" vs "Flexible Timing Active • Battery friendly") and notification channel options with 48dp minimum touch targets and 0 raw hex/dp/sp literals (AD-4, AD-7).
- Given all notification strings and code, when scanned, then the word "nag" is completely absent (AD-4).
- Given the pure domain layer (`com.personaltracker.domain`), when verified by `domain_purity_check.py`, then zero Android framework imports are present (AD-1).

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED` (0 `android.*` imports in domain)
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All 8 H0 components valid`
- `.\gradlew test` -- expected: All JVM unit tests passing clean (including new `TieredNotificationManagerTest` and expanded `ExactAlarmSchedulerTest`)
- `.\gradlew lintDebug` -- expected: 0 errors
- `adb devices -l` & Artemis Real-Device E2E (Gate 6 if device connected)
