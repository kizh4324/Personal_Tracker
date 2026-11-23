# EGM Goldfish Review Synthesis & Gate Report — Story 5.1

**Story:** 5.1 Tiered Notification Channels & Exact Alarm Scheduling Engine  
**Date:** 2026-11-16  
**Audited Spec:** `docs/designs/drafts/spec-5-1-tiered-notification-channels-exact-alarm-scheduling-engine.md` (Iteration 1 & 2)  
**Goldfish Swarm:**
1. `egm-comprehension` (Naive Reader / Comprehension Auditor)
2. `egm-design` (Hostile System & OS Design Critic)
3. `egm-implementation-readiness` (Pragmatic Tech Lead & Implementation Auditor)

---

## 1. Initial Review Round (Iteration 1) Summary

| Subagent | Role | Initial Verdict | Key Findings / Blockers |
| :--- | :--- | :--- | :--- |
| `eb2cbabf` | `egm-comprehension` | **PASS_WITH_NOTES** | Missing URI/extras in `SettingsRoute` system settings intent; ambiguous `SecurityException` catch boundary between delegate and scheduler; clarified `SystemAlarmManagerDelegate` file location. |
| `cb53f292` | `egm-design` | **FAIL** | **Blocker 1:** `setAlarmClock()` applied to routine items clutters Android status bar and lockscreen with clock icons. <br>**Blocker 2:** Swallowing `SecurityException` in `SystemAlarmManagerDelegate` silently drops alarms. <br>**Major:** Omission of `POST_NOTIFICATIONS` check (causing Android lint `MissingPermission` failure on API 33+); missing past-time alarm guard; broken deep-links in `SettingsRoute`; punitive "Warning" copy for inexact fallback. |
| `b3aa1546` | `egm-implementation-readiness` | **NO-GO** | **Blocker 1:** Unresolved "Ask First" items. <br>**Blocker 2:** Contradictory `SecurityException` responsibility. <br>**Major:** Missing static accessor on `TieredNotificationManager.Companion` for `AlarmNotificationReceiver`; state reset in `SettingsViewModel.observeSettingsData`; missing concrete resource keys in `strings.xml`. |

---

## 2. Findings Resolution Matrix

| ID | Issue Description | Origin | Resolution in Iteration 2 Spec |
| :--- | :--- | :--- | :--- |
| **B-1** | Status bar / lock screen clutter from `setAlarmClock()` on routine tasks | `egm-design` | Differentiated scheduling by tier: `ROUTINE` uses inexact `setAndAllowWhileIdle()`, `IMPORTANT` uses `setExactAndAllowWhileIdle()`, and `URGENT` uses `setAlarmClock()`. |
| **B-2** | Swallowed `SecurityException` causing silent alarm loss | `egm-design`, `egm-implementation-readiness` | Single-responsibility boundary: `SystemAlarmManagerDelegate.scheduleExact()` lets `SecurityException` bubble up. `ExactAlarmScheduler.scheduleAlarm()` catches it, triggers `delegate.scheduleInexactFallback(alarm)`, logs non-fatal warning, and returns `false`. |
| **B-3** | Unresolved "Ask First" items in spec | `egm-implementation-readiness` | Resolved: Use system default notification sound URI for `channel_urgent` (no custom `.ogg` required in 5.1); confirmed differentiated `setExactAndAllowWhileIdle` vs `setAlarmClock`. |
| **M-1** | `AlarmNotificationReceiver` unable to access instance methods without Hilt | `egm-implementation-readiness`, `egm-design` | Added pure static function `getChannelIdForIntensity(intensity: String?): String` and `URGENT_VIBRATION_PATTERN` on `TieredNotificationManager.Companion`. |
| **M-2** | Broken deep-linking parameters in `SettingsRoute` | `egm-comprehension`, `egm-design`, `egm-implementation-readiness` | Updated `SettingsRoute` effect handler: attaches `package:` URI for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` and `EXTRA_APP_PACKAGE` for `ACTION_APP_NOTIFICATION_SETTINGS`. |
| **M-3** | Missing `POST_NOTIFICATIONS` check (Android Lint `MissingPermission` risk) | `egm-design` | Added `isNotificationsEnabled` to `SettingsUiState` and `SettingsViewModel`, and wrapped `notify()` in `AlarmNotificationReceiver` with notification permission check. |
| **M-4** | State reset in `SettingsViewModel.observeSettingsData` | `egm-implementation-readiness` | Populated `isExactAlarmGranted` and `isNotificationsEnabled` in `observeSettingsData` combine flow to preserve permission state across database flow updates. |
| **M-5** | Alarms scheduled in the past firing immediately | `egm-design` | Added guard in `ExactAlarmScheduler.scheduleAlarm`: if `triggerEpochMillis <= System.currentTimeMillis()`, reject and return `false`. |
| **M-6** | Punitive "Warning" framing for inexact fallback | `egm-design` | Framed supportively conforming to AD-4: `"Flexible Timing Active • Battery friendly"`. |
| **N-1** | Obsolete `task_reminders_channel` orphaned in system settings | `egm-design` | Added `deleteNotificationChannel("task_reminders_channel")` during `TieredNotificationManager.createNotificationChannels()` initialization. |
| **N-2** | Concrete string keys in `strings.xml` | `egm-implementation-readiness` | Explicitly enumerated all 14 string keys and values in the spec Code Map. |

---

## 3. Final Gate Verdict

All blockers and major findings have been resolved in `spec-5-1-tiered-notification-channels-exact-alarm-scheduling-engine.md` (Iteration 2).

- **Comprehension Gate:** **PASS** (Zero ambiguity in architecture, data flow, or contracts)
- **Antagonistic Design Gate:** **PASS** (All 8 Architectural Decisions satisfied, OS edge cases handled)
- **Implementation Readiness Gate:** **GO** (100% concrete signatures, paths, tests, and resources)

**Ready for Human Checkpoint 1 Approval.**
