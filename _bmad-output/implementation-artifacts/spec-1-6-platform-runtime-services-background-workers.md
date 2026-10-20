---
title: 'Story 1.6: Platform Runtime Services & Background Workers'
type: 'feature'
created: '2026-10-18'
status: 'done'
baseline_commit: 'c39b4bd0132115297f3eb7feb1e8a408c360d79a'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Focus timers terminate when the app is backgrounded, daily streaks and routine templates do not roll over at midnight automatically, and scheduled reminders fail to wake the device or recover after reboots. Furthermore, voice capture requires secure zero-disk in-memory audio buffering to prevent privacy leaks.

**Approach:** Implement core Android platform runtime components in `com.personaltracker.platform`: a foreground service for lock-screen focus countdowns (`StudySessionForegroundService`), a `WorkManager` worker for midnight streak and DayType rollover (`DayRolloverWorker`), an exact alarm scheduler (`ExactAlarmScheduler`), an in-memory RMS-emitting voice recorder with zero-disk purge guarantees (`QuickCaptureVoiceRecorder`), and a boot receiver for alarm and session recovery (`BootCompletedReceiver`).

## Boundaries & Constraints

**Always:**
- Keep `com.personaltracker.domain` 100% pure Kotlin; zero `android.*` or `androidx.*` imports (AD-1, AD-3).
- Zero-guilt, non-punishing copy across all notification channels, titles, and body strings (AD-4).
- In-memory microphone recording only; raw audio buffers must be unconditionally zeroed and dereferenced in `finally` blocks immediately post-transcription with zero disk writes (AD-8, NFR-4).
- Multi-entity database updates during rollover or boot recovery must execute in Room `@Transaction` blocks on `Dispatchers.IO` (AD-2).
- Check `canScheduleExactAlarms()` on Android 12+ (API 31+) before scheduling exact alarms.

**Ask First:**
- Adding any third-party background scheduling or audio recording library not already declared in `libs.versions.toml`.

**Never:**
- Never write raw audio recording buffers to disk, cache directories, or SharedPreferences (AD-8).
- Never nag or shame the user in notification copy or alarm titles (AD-4).
- Never use battery-draining continuous background wake-locks or polling loops (NFR-3).
- Never use `OnConflictStrategy.REPLACE` on entities with cascading foreign keys.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Lock-screen study timer | User starts 25m session | `StudySessionForegroundService` posts ongoing notification with remaining time; ticks down every second | If service killed by system, session recorded as interrupted on restart |
| Timer pause/resume | User taps "Pause" on notification | Notification updates to "Paused", timer freezes; tapping "Resume" continues countdown | Intent action handled cleanly without recreating notification channel |
| Custom midnight rollover | Midnight boundary arrives (e.g. 00:00) | `DayRolloverWorker` runs on `Dispatchers.IO`, evaluates uncompleted habits with `StreakStateMachine.evaluateMissedDay`, resolves active `DayType` | If worker fails, WorkManager retries with exponential backoff |
| Exact alarm trigger | Task scheduled for 15:00 | `ExactAlarmScheduler` sets alarm via `AlarmManager.setAlarmClock()`; triggers notification at exact minute | If exact alarm permission missing on API 31+, falls back to `setAndAllowWhileIdle()` |
| Voice capture streaming | User presses voice FAB | `QuickCaptureVoiceRecorder` streams PCM audio in RAM, emits normalized RMS `Flow<Float>` (0.0..1.0) | `finally` block zeroes buffer with `Arrays.fill(buffer, 0)` and releases `AudioRecord` |
| Device reboot recovery | System sends `ACTION_BOOT_COMPLETED` | `BootCompletedReceiver` reschedules active task alarms and marks active study session as `isInterrupted = true` | Catches exceptions, logs quietly without crashing broadcast receiver |

</frozen-after-approval>

## Code Map

- `app/src/main/AndroidManifest.xml` -- Register service, receivers, and permissions (`FOREGROUND_SERVICE`, `RECORD_AUDIO`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`).
- `app/src/main/java/com/personaltracker/platform/timer/StudySessionForegroundService.kt` -- Foreground service with countdown logic, notification actions (`Pause`, `Resume`, `End`), and state broadcast.
- `app/src/main/java/com/personaltracker/platform/worker/DayRolloverWorker.kt` -- `CoroutineWorker` evaluating habit streaks and DayType resolution on `Dispatchers.IO`.
- `app/src/main/java/com/personaltracker/platform/alarm/ExactAlarmScheduler.kt` -- Schedules tiered alarms via `AlarmManager` with API 31+ permission checks.
- `app/src/main/java/com/personaltracker/platform/alarm/AlarmNotificationReceiver.kt` -- BroadcastReceiver receiving alarm trigger intents and displaying notification.
- `app/src/main/java/com/personaltracker/platform/voice/QuickCaptureVoiceRecorder.kt` -- AudioRecord wrapper in RAM only, RMS amplitude calculation, zeroing buffers in `finally`.
- `app/src/main/java/com/personaltracker/platform/receiver/BootCompletedReceiver.kt` -- Reschedules alarms and transitions active focus session to `isInterrupted = true`.
- `app/src/main/java/com/personaltracker/di/PlatformModule.kt` -- Hilt module providing `ExactAlarmScheduler`, `QuickCaptureVoiceRecorder`, and notification managers.
- `app/src/test/java/com/personaltracker/platform/` -- Unit tests verifying timer state logic, rollover logic, audio zeroing, alarm calculation, and boot recovery.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/AndroidManifest.xml` -- Add foreground service, timer property, boot receiver, alarm receiver, and required permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`).
- [x] `app/src/main/java/com/personaltracker/platform/timer/StudySessionForegroundService.kt` -- Implement foreground service managing lock-screen countdown, notification actions (`ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_STOP`), zero-guilt copy, and observable timer state flow.
- [x] `app/src/main/java/com/personaltracker/platform/worker/DayRolloverWorker.kt` -- Implement `CoroutineWorker` using `EntryPointAccessors` to evaluate habit streaks with `StreakStateMachine` and resolve `DayType` on `Dispatchers.IO`.
- [x] `app/src/main/java/com/personaltracker/platform/alarm/ExactAlarmScheduler.kt` -- Implement exact alarm scheduler with Android 12+ `canScheduleExactAlarms()` checks and immutable PendingIntents.
- [x] `app/src/main/java/com/personaltracker/platform/alarm/AlarmNotificationReceiver.kt` -- Implement broadcast receiver posting calm, non-punishing reminder notifications.
- [x] `app/src/main/java/com/personaltracker/platform/voice/QuickCaptureVoiceRecorder.kt` -- Implement RAM-only audio capture emitting normalized RMS `Flow<Float>` and zeroing byte arrays in `finally`.
- [x] `app/src/main/java/com/personaltracker/platform/receiver/BootCompletedReceiver.kt` -- Implement boot receiver that reschedules alarms and marks active sessions as interrupted.
- [x] `app/src/main/java/com/personaltracker/di/PlatformModule.kt` -- Provide platform singletons via Hilt.
- [x] `app/src/test/java/com/personaltracker/platform/StudySessionTimerTest.kt` -- Unit-test timer countdown, pause/resume transitions, and completion callbacks.
- [x] `app/src/test/java/com/personaltracker/platform/DayRolloverLogicTest.kt` -- Unit-test streak miss evaluation, freeze preservation, and DayType resolution during rollover.
- [x] `app/src/test/java/com/personaltracker/platform/QuickCaptureVoiceRecorderTest.kt` -- Unit-test RMS calculation, normalization, and memory buffer purge/zeroing verification.
- [x] `app/src/test/java/com/personaltracker/platform/ExactAlarmSchedulerTest.kt` -- Unit-test trigger calculation, permission fallback, and cancel operations.

**Acceptance Criteria:**
- Given an active study session, when app is backgrounded or device is locked, then `StudySessionForegroundService` maintains ongoing notification with active countdown and zero-guilt copy.
- Given habit entities with active streaks, when `DayRolloverWorker` executes at midnight, then uncompleted habits are evaluated via `StreakStateMachine` with freeze/slack grace rules and zero shame copy.
- Given a scheduled task, when trigger time arrives, then `ExactAlarmScheduler` fires an exact notification via `AlarmManager.setAlarmClock()`.
- Given microphone recording in `QuickCaptureVoiceRecorder`, when recording completes or errors, then raw audio buffers are unconditionally zeroed with `Arrays.fill(buffer, 0)` in a `finally` block and never written to disk.
- Given device reboot, when `ACTION_BOOT_COMPLETED` is received, then active alarms are restored and interrupted focus sessions are flagged `isInterrupted = true`.
- Given unit tests and linting, when `.\gradlew test` and `.\gradlew lintDebug` are run, then all tests pass with 0 errors.

## Spec Change Log

- Completed implementation of Story 1.6 platform runtime services, background workers, and alarm scheduling. All 12 execution tasks verified with JVM unit tests, Android lint (0 errors), debug APK build, and architecture mechanical lint.

## Design Notes

- **Android 14+ FGS Type**: `StudySessionForegroundService` uses `foregroundServiceType="specialUse"` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Study session focus timer countdown" />` to satisfy API 34+ foreground service requirements.
- **Worker Injection**: `DayRolloverWorker` uses `EntryPointAccessors.fromApplication(context, RolloverEntryPoint::class.java)` to decouple from custom WorkManager factory setup while accessing Hilt-managed repositories.
- **Memory Audio Zeroing**:
```kotlin
val buffer = ShortArray(bufferSize)
try {
    // Read audio and compute RMS
} finally {
    java.util.Arrays.fill(buffer, 0.toShort())
}
```

## Verification

**Commands:**
- `.\gradlew test` -- expected: BUILD SUCCESSFUL (all unit tests passing)
- `.\gradlew lintDebug` -- expected: BUILD SUCCESSFUL (0 errors)
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: ok: true, total_findings: 0
