# Epic 5 Context: Tiered Notifications, Scarcity Guardrails & Rescheduling Review

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Users receive intentional, tiered notifications (Routine quiet, Important standard, Urgent heads-up) with acknowledgement-based bounded escalation, receive gentle soft-cap guidance against overbooking urgent priorities without losing autonomy, and resolve uncompleted flexible tasks through an evening/morning Carry-Forward review card equipped with an advisory workload capacity indicator. This eliminates notification anxiety and silent task rollover while preserving a calm, non-punishing planning workflow.

## Stories

- Story 5.1: Tiered Notification Channels & Exact Alarm Scheduling Engine
- Story 5.2: Acknowledgement-Based Bounded Escalation Engine
- Story 5.3: Urgency Soft-Cap Scarcity Guidance
- Story 5.4: Evening/Morning Carry-Forward Review Card
- Story 5.5: Advisory Workload Capacity Indicator & Guardrails

## Requirements & Constraints

- **Tiered Notification Delivery Classes:** Notifications are partitioned into three classes: `Routine` (`IMPORTANCE_LOW`, quiet/in-app, no sound or heads-up), `Important` (`IMPORTANCE_DEFAULT`, standard OS notification with sound and vibration), and `Urgent` (`IMPORTANCE_HIGH`, heads-up display with distinctive audio and heavy double-pulse haptics).
- **Acknowledgement-Based Bounded Escalation:** Uncompleted `Urgent` items re-fire on a bounded schedule until acknowledged, completed, or dismissed (configurable baseline: 5-minute interval, max 3 re-fires), halting immediately upon completion or dismissal. The word `"nag"` is strictly prohibited in all notification copy, UI strings, code, comments, and logs.
- **Urgency Scarcity Soft-Cap:** Marking a 3rd active item as `Urgent` triggers a non-blocking soft guidance prompt offering to keep `Urgent` or change to `Important` (configurable ceiling baseline: 2 active Urgent items). Completing an Urgent item releases its active quota slot. The system must never hard-block Urgent creation or silently downgrade an item's urgency.
- **Item-Type Aware Carry-Forward:** Uncompleted flexible Tasks trigger a Carry-Forward review card during a configurable evening review window (default ~9 PM) or at next morning launch, presenting explicit per-item actions (`Do Tomorrow`, `Keep Unscheduled`, `Choose Another Day`, `Cancel`) and a batch move-all option. Fixed-Time Events must never silently roll over and require explicit re-keying; Routine Occurrences expire for that day without stacking.
- **Advisory Workload Capacity Guardrails:** Carrying flexible tasks forward to tomorrow computes tomorrow's planned workload (sum of `estimatedDurationMinutes`) and displays an advisory capacity indicator. The indicator must never hard-block rescheduling, even when tomorrow exceeds 100% planned capacity.
- **Offline Reliability & Graceful Permission Degradation:** All notification scheduling, escalation, and carry-forward workflows run 100% locally offline without network dependencies. Exact alarms must check `canScheduleExactAlarms()` at runtime and degrade gracefully to inexact `WorkManager` scheduling if denied; `POST_NOTIFICATIONS` denial degrades gracefully to in-app banners without modal permission walls.
- **Non-Punishing Invariant & Layer Purity:** Copy must remain neutral, informational, and actionable with zero guilt or FOMO framing. Behavioral policies and use cases remain 100% pure Kotlin in `domain/` with zero Android framework imports, UI follows unidirectional MVI (`StateFlow<UiState>`, `UiIntent`, Channel-backed `Flow<UiEffect>`), and multi-entity updates run inside Room `@Transaction` blocks on `Dispatchers.IO`.

## Technical Decisions

- **Configurable Policy Objects:** Because the urgent quota ceiling (2 active items), escalation cadence (5-minute retry, max 3 re-fires), and workload capacity calculation (`estimatedDurationMinutes` summation) are preliminary product hypotheses, encapsulate them in configurable pure Kotlin domain policies (`EscalationPolicy`, `UrgencyScarcityPolicy`, `CapacityPolicy`) rather than hardcoding magic numbers across layers.
- **Alarm & Background Execution Separation:** Reserve `AlarmManager` (`setAlarmClock()` / `setExactAndAllowWhileIdle()`) for user-facing time-critical reminders with defensive permission fallback, and use `WorkManager` (`DayRolloverWorker` and deferred background jobs) for daily rollover, expiry evaluation, and maintenance. Re-register active exact alarms on device restart via `BootCompletedReceiver`.
- **Single-Date Carry-Forward Semantics:** Carry-forward updates target `TaskEntity` fields (`scheduledDate`, `scheduledTime`, `state`, `isCarryForward`, `carryForwardCount`) through `CarryForwardUseCase` and `TaskRepository` without mutating base `DayType` templates.
- **Token-Strict Accessible UI:** Resolve all colors, typography, spacing, shapes, and elevations from `ui/theme/` semantic tokens (no hardcoded hex, `dp`, or `sp` literals). Enforce 48dp minimum interactive touch targets, TalkBack state announcements, dynamic type scaling, and Reduce Motion support.

## UX & Interaction Patterns

- **Hero Card Priority & Transitions:** Carry-Forward Review occupies Hero Card Priority 5 (below Active Focus, Interrupted/Resumable, Morning DayType Banner, and Upcoming Scheduled Item, and above Intentional Idle). Transitions animate with 300ms `FastOutSlowIn` cross-fades; once all carry-forward items are resolved (or if none exist), the Hero Card falls back to Intentional Idle and the companion enters its resting state.
- **Delivery Intensity & Carry-Forward Visuals:** Task cards signal urgency via a left accent stripe—Routine (`{colors.info}`, 3dp), Important (`{colors.warning}`, 3dp), Urgent (`{colors.danger}`, 6dp + top-right lightning icon)—and carried-forward tasks display a small `"↻"` indicator with carry count.
- **Soft-Cap & Escalation Microcopy:** Urgency guidance uses calm choice framing (*"You already have 2 Urgent items today. Keep Urgent or Change to Important?"*). Escalation notifications remain purely informational (*"Reminder: [Item Title]"*).
- **Carry-Forward & Capacity Feedback:** Review header uses neutral framing (*"3 items still open. What would you like to do?"*). Selecting `Do Tomorrow` dynamically reveals an 8dp rounded horizontal capacity bar shifting color (`{colors.success}` normal → `{colors.warning}` heavy → `{colors.danger}` overloaded) with factual summary text (*"Tomorrow has 6h 45m scheduled; adding these 3 tasks brings it to 8h 10m"*) and a non-blocking `"Move anyway"` action when overloaded.

## Cross-Story Dependencies

- **Within Epic 5:**
  - Story 5.1 establishes the 3 Android notification channels and defensive exact-alarm scheduling substrate required by Story 5.2 for bounded urgent escalation re-fires.
  - Story 5.2's completion/dismissal handling releases active Urgent quota slots evaluated by Story 5.3's soft-cap scarcity prompt.
  - Story 5.4's Carry-Forward review card and `Do Tomorrow` flow host and trigger Story 5.5's advisory workload capacity calculation and bar.
- **External Epic Dependencies:**
  - Builds on Epic 1 foundations: `ExactAlarmScheduler`, `DayRolloverWorker`, `BootCompletedReceiver`, `HeroCard` priority state machine, and `TaskRepository`/`DayTypeRepository`.
  - Integrates with Settings permission diagnostics (expanded in Epic 6) for `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, and channel/review-window preferences.
