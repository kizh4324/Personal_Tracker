---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/prd.md
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/addendum.md
  - docs/specifications/architecture.md
  - docs/specifications/TECHSTACK.md
  - _bmad-output/planning-artifacts/briefs/brief-personal-tracker-2026-08-14/brief.md
  - docs/design/design-system.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md
---

# Personal-Tracker - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Personal-Tracker v1, decomposing the requirements from the PRD (+ Addendum), UX Design contract (DESIGN.md & EXPERIENCE.md), and Architecture requirements into 6 vertical-slice epics and 29 implementable user stories with Gherkin acceptance criteria.

---

## Requirements Inventory

### Functional Requirements

- **FR-1.1**: The primary home surface MUST feature a dynamic Hero Card representing the user's current actionable state: *Active Focus/Task*, *Upcoming Scheduled Item*, *Interrupted/Resumable Task*, or *Intentional Idle State* `[DECISION 1]`.
- **FR-1.2**: Secondary views (*Today's Schedule* and *Action List*) MUST sit below the Hero Card `[DECISION 1]`.
- **FR-1.3**: When an item is `INTERRUPTED`, the Hero Card MUST surface a Resumption Card with actions `[Resume]`, `[Review/Adjust]`, and `[Mark Complete]` `[PRD RULE 1]`.
- **FR-1.4**: System MUST NEVER automatically mark an item complete merely because its scheduled window expires `[PRD RULE 1]`.
- **FR-1.5**: System MUST NEVER silently reschedule an interrupted item without user confirmation `[PRD RULE 1]`.
- **FR-2.1**: System MUST support unified capture entry while persisting distinct domain models for **Tasks**, **Routines**, **Habits**, and **Study Sessions** `[DECISION 2]`.
- **FR-2.2**: Tasks MUST support priority ranking and transparent "Do Tomorrow" carry forward rescheduling `[DECISION 2 & 6]`.
- **FR-2.3**: Routines MUST consist of sequential `RoutineStep` items linked to first-class `DayType` schedules `[DECISION 2 & 3]`.
- **FR-2.4**: Habits MUST support cadence tracking, earned streak slack, and miss-tolerant recovery `[DECISION 2 & 6]`.
- **FR-2.5**: Study Sessions MUST support subject tags, duration timers, and subject heatmap rollups `[DECISION 2]`.
- **FR-2.6**: Scheduling, notifications, focus sessions, Coin rewards, and history services MUST operate across all four domain models `[DECISION 2]`.
- **FR-3.1**: System MUST resolve today's active `DayType` in v1 using the hierarchy: 1) Date-Specific User Override > 2) Day-of-Week Default `[DECISION 3]`. Future calendar exception integration is classified as `[FUTURE / OPEN DECISION]`.
- **FR-3.2**: Today's active `DayType` MUST load automatically on app launch with zero mandatory setup taps `[DECISION 3]`.
- **FR-3.3**: The Hero Card MUST display a non-blocking morning banner showing the active template and offering a 1-tap swap option `[DECISION 3]`.
- **FR-3.4**: Template swaps applied via the Hero Card MUST apply strictly to the current date and MUST NOT mutate underlying base templates `[DECISION 3]`.
- **FR-3.5**: Mid-day template swaps MUST preserve all completed and in-progress items as immutable, recalculating only the remaining unstarted schedule for today `[PRD RULE 4]`.
- **FR-4.1**: Scheduled focus/study block start MUST present a non-blocking Hero Card prompt (`"Math Study starts now — Start Focus?"`) `[DECISION 4]`.
- **FR-4.2**: Launching a distracting app during a scheduled focus block without an active session MUST trigger at most **ONE** contextual JITAI prompt (`"Active Schedule: Math Study — Start Focus or Snooze"`) `[DECISION 4]`.
- **FR-4.3**: Explicitly starting a `FocusSession` MUST activate `AccessibilityService` overlay blocking and the Dimension B Adaptive Intervention Ladder `[DECISION 4]`.
- **FR-4.4**: Bypassing an active focus block MUST require deliberate override friction `[DECISION 4 & Dimension B]`. Exact duration, cooldown, confirmation behavior, and escalation parameters remain `[HYPOTHESIS] / [VALIDATION ITEMS]`.
- **FR-4.5**: Exiting a `FocusSession` early or letting a scheduled block expire while item is `IN_PROGRESS` MUST mark item state as `INTERRUPTED/RESUMABLE` `[PRD RULE 1]`.
- **FR-4.6**: Emergency calls, telecom dialers, and OS-critical interfaces MUST bypass distraction blocking completely `[PRD RULE 2]`.
- **FR-5.1**: Notifications MUST be categorized into three delivery classes: `Routine` (quiet/in-app), `Important` (standard OS notification), and `Urgent` (`IMPORTANCE_HIGH` heads-up display + distinctive audio/haptics) `[DECISION 5]`.
- **FR-5.2**: Marking a 3rd active item as Urgent MUST trigger a soft guidance prompt (`"You already have 2 Urgent items today. Keep Urgent or Change to Important?"`) `[DECISION 5]`.
- **FR-5.3**: System MUST NEVER hard-block or silently downgrade Urgent items `[DECISION 5]`.
- **FR-5.4**: Urgent notifications MUST use acknowledgement-based escalation (bounded escalation); the word "nag" is prohibited `[DECISION 5]`.
- **FR-5.5**: Completing an Urgent item MUST release its quota slot for active Urgent items `[DECISION 5]`.
- **FR-6.1**: Uncompleted flexible Tasks MUST trigger a Carry Forward review card during evening review or next morning launch `[DECISION 6]`.
- **FR-6.2**: Review card MUST provide explicit actions: `[Do Tomorrow]`, `[Keep Unscheduled]`, `[Choose Another Day]`, and `[Cancel]` `[DECISION 6]`.
- **FR-6.3**: Fixed-Time Events MUST NOT silently roll over and MUST require explicit re-keying `[DECISION 6]`.
- **FR-6.4**: Routine Occurrences MUST expire for that day without stacking `[DECISION 6]`.
- **FR-6.5**: Moving flexible items to tomorrow MUST calculate and display an advisory workload capacity indicator (`"Tomorrow has 6h 45m scheduled; moving these 3 tasks brings it to 8h 10m"`). Indicator MUST be advisory only and NEVER hard-block `[DECISION 6]`.
- **FR-7.1**: System MUST support one-line natural-language text quick-add and local user-initiated voice capture `[DECISION 7]`.
- **FR-7.2**: Voice pipeline MUST process audio through STT (Gemini Live WebSocket when online, on-device `SpeechRecognizer` when offline), delete raw audio immediately post-parsing, and run structured proposal validation before persistence `[DECISION 7 & 9]`.
- **FR-7.3**: High-confidence captures MUST commit immediately and display a temporary 5-second Undo toast pill `[DECISION 7]`.
- **FR-7.4**: Medium-confidence captures MUST display an inline Hero Card confirmation card with 1-tap editable chips (`Title`, `Date`, `Time`, `Type`, `Urgency`), requiring explicit user *Confirm* action before persistence `[PRD RULE 3]`.
- **FR-7.5**: If on-device STT succeeds but parsing is low-confidence or ambiguous, the resulting transcript MUST be saved to an *Unfiled Capture Inbox* for 1-tap categorization `[PRD RULE 3]`. If on-device STT is unavailable or fails, the system MUST fall back directly to manual text entry `[PRD RULE 3]`.
- **FR-7.6**: AI Action Boundary: Model outputs (STT, LLMs) MUST NEVER directly mutate persistent database records without schema/business rule validation `[DECISION 7 & Dimension G]`.
- **FR-8.1**: System MUST feature a supportive, non-punishing companion pet on the Hero Card (zero HP loss, zero character death, zero guilt mechanics) `[DECISION 8]`.
- **FR-8.2**: Genuine completions (Tasks, Routine Steps, Habits, Study Sessions, Focus Sessions) MUST award Coins to a local auditable ledger `[DECISION 8 & PRD RULE 5]`.
- **FR-8.3**: Reward-event idempotency MUST prevent duplicate Coin awards on repeated UI toggles or app restarts `[PRD RULE 5]`.
- **FR-8.4**: Coins in v1 MUST be spent EXCLUSIVELY in the local Shop on companion cosmetics, outfits, accessories, and background decor `[DECISION 8]`.
- **FR-8.5**: Streak system MUST operate architecturally independent of Coins; Coins MUST NOT purchase or control Streak Freezes in v1 `[DECISION 8]`.
- **FR-9.1**: All application data MUST be stored in a local Room + SQLCipher database encrypted at rest with OS-level application sandbox protection `[DECISION 9]`.
- **FR-9.2**: System MUST operate 100% offline without default cloud synchronization or mandatory network access `[DECISION 9]`.
- **FR-9.3**: Manual backup export MUST produce a password-protected encrypted `.ptbackup` file on local storage `[DECISION 9]`.
- **FR-9.4**: Backup import MUST execute a 4-step validation pipeline (password verification, cryptographic integrity/authentication verification, format check, schema check) before modifying the active database `[PRD RULE 6]`.
- **FR-9.5**: Backup import MUST create a temporary pre-restore safety snapshot before replacing active DB files `[PRD RULE 6]`.
- **FR-9.6**: If import fails or validation errors occur, system MUST restore the pre-restore safety snapshot, substantially protecting the active database from failed or interrupted restores and leaving the previous database 100% intact `[PRD RULE 6]`.
- **FR-10.1**: Emergency calls, phone dialer UIs, and OS critical interfaces MUST bypass distraction blocking completely `[PRD RULE 2]`.
- **FR-10.2**: Unexpected OS kills, low-memory terminations, battery exhaustion, or reboots MUST reconstruct session state into `INTERRUPTED/RESUMABLE` on next launch when persisted state supports that an active session was incomplete `[PRD RULE 2]`.
- **FR-10.3**: Selecting "Cancel Session" explicitly MUST set item state to `CANCELLED` and MUST NOT generate an automatic resumption prompt `[PRD RULE 2]`.

---

### NonFunctional Requirements

- **NFR-1 (Local Latency)**: Cold app launch to interactive Hero Card state is targeted to execute in < 1.5 seconds on modern Android hardware `[ENGINEERING TARGET]`. Quick-add deterministic NLP parsing is targeted to complete in < 300 ms `[ENGINEERING TARGET]`.
- **NFR-2 (Offline Reliability)**: 100% of core task, routine, habit, focus, reminder, voice-fallback, and reward features MUST operate without network connectivity `[CONFIRMED]`.
- **NFR-3 (Battery & Resource Efficiency)**: Background task execution MUST use `WorkManager`; exact alarms MUST be restricted to user-facing `setAlarmClock()` calls `[CONFIRMED]`. `AccessibilityService` background CPU usage is targeted to remain < 2% during idle monitoring `[ENGINEERING TARGET]`.
- **NFR-4 (Security & Privacy)**: Database MUST be encrypted at rest using Room + SQLCipher (`AES-256-CBC`, 256k PBKDF2 iterations, 4096-byte DB page size, 16 KB native ELF alignment `[ARCHITECTURE RESOLVED]`). Raw audio MUST be purged immediately post-STT `[CONFIRMED]`. Zero telemetry SDKs `[CONFIRMED]`.
- **NFR-5 (Data Portability)**: Encrypted `.ptbackup` artifacts (`PBKDF2-HMAC-SHA512` 256,000 iterations + `AES-256-GCM` 128-bit authentication tag `[ARCHITECTURE RESOLVED]`) MUST be restorable on replacement Android devices with valid credentials.

---

### Additional Requirements

- **ARCH-1 (Greenfield Starter Setup)**: Initialize Android project with Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`), AGP 8.8.0, Kotlin 2.4.0, KSP, Hilt DI, Jetpack Compose BOM 2026.02.00, and baseline API 33–36 target `[PROVISIONAL TECHNICAL TARGET]`.
- **ARCH-2 (Database & Security Engine)**: Implement Room over SQLCipher with `net.zetetic:sqlcipher-android:4.6.1` (16 KB page-aligned), Android Keystore master key abstraction (`SecurityKeyStoreManager`), and secure passphrase lifecycle `[ARCHITECTURE RESOLVED]`.
- **ARCH-3 (Strict AI Action Boundary)**: Implement decoupled pipeline: AI/STT $ightarrow$ `UnvalidatedTaskProposal` DTO $ightarrow$ Domain/Schema Validation $ightarrow$ `ValidatedTaskCreationCommand` $ightarrow$ `TaskRepository` $ightarrow$ Room DAO. AI packages are strictly prohibited from DAO dependencies `[CONFIRMED]`.
- **ARCH-4 (Dual Voice Architecture)**: Implement `VoiceRouterService` orchestrating real-time online Gemini Live over WebSocket with on-device `SpeechRecognizer` (`createOnDeviceSpeechRecognizer`) fallback and manual text quick-add fallback `[CONFIRMED]`.
- **ARCH-5 (Event-Driven Accessibility Intervention)**: Implement `AccessibilityService` using `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` package detection on supported/configured devices with strict zero-screen-reading privacy, dynamic `TelecomManager` dialer whitelist, and telephony state listener `[CONFIRMED / TECHNICAL CONSTRAINT]`.
- **ARCH-6 (Idempotent Reward Ledger)**: Implement `CoinLedgerEntity` with compound unique keys (`TASK_<id>_<timestamp>`, `HABIT_<id>_<date>`) and transactional repository logic to prevent duplicate awards on rapid UI toggling or app restarts `[CONFIRMED]`.
- **ARCH-7 (Encrypted Backup & Transactional Restore)**: Implement `.ptbackup` binary container serialization (`PTBK` magic bytes, salt, IV, GCM tag, Gzipped JSON), 4-step validation pipeline, and pre-restore SQLite safety snapshots with automatic rollback `[ARCHITECTURE RESOLVED]`.
- **ARCH-8 (State Reconstruction Engine)**: Implement `BootCompletedReceiver` and startup lifecycle hooks to reconstruct uncompleted `IN_PROGRESS` sessions into `INTERRUPTED/RESUMABLE` and re-register exact alarms upon device reboot or process termination `[ARCHITECTURE RESOLVED]`.
- **ARCH-9 (Threading & Dispatcher Mapping)**: Strictly bind Compose rendering to `Dispatchers.Main`, DB/Crypto/IO to `Dispatchers.IO`, and Deterministic Regex/PBKDF2/Bézier calculations to `Dispatchers.Default` `[RECOMMENDED]`.

---

### UX Design Requirements

- **UX-DR1 (Design Tokens Implementation)**: Implement complete Compose Theme design tokens (Colors, Typography ramps using Plus Jakarta Sans & Inter, Spacing 4dp grid, Shapes `xs` to `xxl`, Elevation, and DayType palettes) for both Light and Dark modes matching `design-system.md` and `DESIGN.md`.
- **UX-DR2 (State-Aware Hero Card)**: Implement dynamic Hero Card container managing 6 priority states: Active Focus, Interrupted/Resumable, Morning DayType Banner, Upcoming Scheduled Item, Carry-Forward Review, and Intentional Idle with 300ms `FastOutSlowIn` transitions.
- **UX-DR3 (DayType Timeline & Morning Swap)**: Implement persistent bottom sheet DayType timeline (Peek ~100dp, Half ~50%, Full ~100% snap points) and non-blocking morning banner with single-date-only template swapping.
- **UX-DR4 (Specialized Domain Cards)**: Implement dedicated visual cards for Tasks (with delivery intensity stripes and carry-forward badges), Habits (with streak flames and freeze indicators), Routines (with sequential step progress), and Study Sessions (with subject tags and heatmap links).
- **UX-DR5 (Voice Capture FAB & Morphing Interaction)**: Implement Voice FAB that morphs from 56dp circle to stadium pill during speech capture with pulsing waveform and live token chip previews, repositioning above the IME keyboard.
- **UX-DR6 (Confidence Routing UI)**: Implement 5-second Undo Toast for high-confidence auto-commits, inline Confirmation Chips with 1-tap inline editing for medium confidence, and Unfiled Capture Inbox routing for low-confidence transcript triage.
- **UX-DR7 (Focus Timer & Adaptive Intervention Overlay)**: Implement full-screen blurred distraction intercept overlay (`{colors.surface-overlay}`) with hold-to-override friction ladder, emergency call suspension, and pause/resume controls.
- **UX-DR8 (Ovsiankina Resumption Card)**: Implement persistent Resumption Card in Hero Card displaying interrupted item, elapsed time, and `[Resume]`, `[Review/Adjust]`, `[Mark Complete]` actions.
- **UX-DR9 (Carry-Forward Review & Advisory Capacity)**: Implement evening/morning Carry-Forward review card with per-item actions (`Do Tomorrow`, `Keep Unscheduled`, `Choose Another Day`, `Cancel`) and dynamic advisory workload capacity indicator.
- **UX-DR10 (Urgency Scarcity & Notification Channels)**: Implement soft-cap guidance prompt upon adding 3rd active Urgent item, and configure Routine, Important, and Urgent Android notification channels.
- **UX-DR11 (Companion Widget & 7-State Rive Machine)**: Implement companion pet widget container integrated with Rive state machine (`companion_sm`, states 0–6: Idle, Celebrating, Encouraging, Concerned, Focused, Sleeping, Excited) with zero guilt or negative emotional states.
- **UX-DR12 (Coin HUD & Parabolic Bézier Earn Animation)**: Implement persistent Coin HUD badge and VSYNC-aligned quadratic Bézier coin arc animation (`CoinArcAnimation.kt`) using `withFrameNanos` across 60Hz and 120Hz displays.
- **UX-DR13 (Cosmetic Shop Experience)**: Implement local Shop UI for browsing, previewing, and purchasing Companion Outfits, Accessories, and Background Decor using Coins.
- **UX-DR14 (Backup & Restore Experience)**: Implement password-protected export UI with strength indicators, and import UI with 4-step validation progress and safe error feedback.
- **UX-DR15 (Permissions & Capability Diagnostic)**: Implement non-blocking onboarding and Settings diagnostic card for AccessibilityService, Exact Alarms, Notification channels, Usage Stats, and STT language packs with graceful degradation.
- **UX-DR16 (Offline/Online Experience)**: Implement seamless failover from Gemini Live to on-device STT/manual text with subtle top-bar offline status icon.
- **UX-DR17 (Accessibility Floor Implementation)**: Implement TalkBack semantics, live region state change announcements, Compose dynamic font scaling (`sp`), WCAG 2.1 AA contrast compliance, and 48dp minimum touch targets.
- **UX-DR18 (Navigation & Screen Insets)**: Implement 5-tab Bottom Navigation (`Home`, `Habits`, `Study`, `Companion`, `Settings`), 1-level deep modal navigation stacks, and edge-to-edge Compose `WindowInsets` handling.

---

### FR Coverage Map

- **FR-1.1**: Epic 1 (Story 1.4) — Core Home Surface & State-Aware Hero Card (`UJ-1`, Decision 1, `UX-DR2`)
- **FR-1.2**: Epic 1 (Story 1.4) — Subordinate Today's Schedule & Action List Views (Decision 1, `UX-DR18`)
- **FR-1.3**: Epic 3 (Story 3.5) — Interrupted Item Resumption Card on Hero Card (`UJ-3`, PRD Rule 1, `UX-DR8`)
- **FR-1.4**: Epic 3 (Story 3.5) — Prohibition of Auto-Completion on Scheduled Block Expiry (PRD Rule 1)
- **FR-1.5**: Epic 3 (Story 3.5) — Prohibition of Silent Rescheduling of Interrupted Items (PRD Rule 1)
- **FR-2.1**: Epic 1 (Story 1.5) — Unified Capture Entry with Specialized Domain Persistence (`UJ-1`, Decision 2, `ARCH-2`)
- **FR-2.2**: Epic 1 (Story 1.5) — Task Delivery-Intensity Classification and Model Baseline (Decision 2, `UX-DR4`)
- **FR-2.3**: Epic 1 (Story 1.6) — Sequential RoutineStep Domain Model & DayType Binding (Decision 2, `UX-DR4`)
- **FR-2.4**: Epic 4 (Story 4.1) — Habit Cadence Tracking, Streak Slack Bank & Miss-Tolerant Recovery (Decision 2, `UX-DR4`)
- **FR-2.5**: Epic 3 (Story 3.1) — Study Session Subject Tags, Duration Timers & Heatmaps (Decision 2, `UX-DR4`)
- **FR-2.6**: Epic 4 (Story 4.2) — Cross-Domain Shared Service Operation (Scheduling, Coins, History) (Decision 2, `ARCH-6`)
- **FR-3.1**: Epic 1 (Story 1.3) — DayType Resolution Hierarchy: User Override > Day Default (`UJ-1`, Decision 3, `ARCH-2`)
- **FR-3.2**: Epic 1 (Story 1.3) — Zero-Tap Automatic DayType Schedule Activation on Launch (`UJ-1`, Decision 3, `AC-1`)
- **FR-3.3**: Epic 1 (Story 1.3) — Non-Blocking Morning DayType Banner with 1-Tap Swap Option (`UJ-1`, Decision 3, `UX-DR3`)
- **FR-3.4**: Epic 1 (Story 1.3) — Single-Date Scope for DayType Swapping without Template Mutation (`UJ-1`, Decision 3, `UX-DR3`)
- **FR-3.5**: Epic 1 (Story 1.6) — Mid-Day Swap Immutable History Preservation & Remaining Schedule Recalculation (PRD Rule 4, `UX-DR3`)
- **FR-4.1**: Epic 3 (Story 3.1) — Scheduled Focus/Study Block Hero Card Prompt (`UJ-3`, Decision 4, `UX-DR7`)
- **FR-4.2**: Epic 3 (Story 3.4) — Contextual Single-Prompt JITAI Distraction Intercept (`UJ-3`, Decision 4, `UX-DR7`)
- **FR-4.3**: Epic 3 (Story 3.2) — AccessibilityService Active Focus Distraction Overlay Blocking (`UJ-3`, Decision 4, `ARCH-5`)
- **FR-4.4**: Epic 3 (Story 3.4) — Deliberate Focus Override Friction Ladder (`UJ-3`, Decision 4, Dimension B, `UX-DR7`)
- **FR-4.5**: Epic 3 (Story 3.5) — Early Exit/Expiry Transition to INTERRUPTED/RESUMABLE (`UJ-3`, PRD Rule 1, `UX-DR8`)
- **FR-4.6**: Epic 3 (Story 3.3) — Telecom Dialer & Emergency Call Distraction Blocking Bypass (`UJ-3`, PRD Rule 2, `ARCH-5`)
- **FR-5.1**: Epic 5 (Story 5.1) — Tiered Notification Delivery Classes: Routine, Important, Urgent (Decision 5, `UX-DR10`)
- **FR-5.2**: Epic 5 (Story 5.3) — Soft-Cap Scarcity Guidance Prompt on 3rd Active Urgent Item (Decision 5, `UX-DR10`, `AC-4`)
- **FR-5.3**: Epic 5 (Story 5.3) — Strict Prohibition of Silent Urgent Downgrades or Hard-Blocks (Decision 5, `UX-DR10`)
- **FR-5.4**: Epic 5 (Story 5.2) — Acknowledgement-Based Bounded Escalation without "Nag" Copy (Decision 5, `UX-DR10`)
- **FR-5.5**: Epic 5 (Story 5.2) — Urgent Item Quota Release on Completion (Decision 5, `UX-DR10`)
- **FR-6.1**: Epic 5 (Story 5.4) — Evening/Morning Carry-Forward Review Card Trigger (`UJ-4`, Decision 6, `UX-DR9`)
- **FR-6.2**: Epic 5 (Story 5.4) — Explicit Carry-Forward Per-Item Actions: Do Tomorrow, Unschedule, Pick Date, Cancel (`UJ-4`, Decision 6, `UX-DR9`, `AC-5`)
- **FR-6.3**: Epic 5 (Story 5.4) — Fixed-Time Event Expiry Requiring Explicit Re-Keying (Decision 6, `UX-DR9`)
- **FR-6.4**: Epic 5 (Story 5.4) — Routine Occurrence Non-Stacking Expiry (Decision 6, `UX-DR9`)
- **FR-6.5**: Epic 5 (Story 5.5) — Advisory Workload Capacity Guardrail Indicator (`UJ-4`, Decision 6, Dimension D, `UX-DR9`)
- **FR-7.1**: Epic 2 (Story 2.1) — One-Line Natural Language Text Quick-Add & Voice Capture FAB (`UJ-2`, Decision 7, `UX-DR5`)
- **FR-7.2**: Epic 2 (Story 2.3) — Streaming Voice Pipeline with Immediate Post-STT Audio Destruction (`UJ-2`, Decision 7, `ARCH-4`)
- **FR-7.3**: Epic 2 (Story 2.5) — High-Confidence Instant Commit with 5-Second Undo Toast Pill (`UJ-2`, Decision 7, `UX-DR6`, `AC-2`)
- **FR-7.4**: Epic 2 (Story 2.5) — Medium-Confidence Inline Confirmation Card with 1-Tap Editable Chips (`UJ-2`, PRD Rule 3, `UX-DR6`, `AC-2`)
- **FR-7.5**: Epic 2 (Story 2.3, 2.5) — Low-Confidence Unfiled Capture Inbox & Offline STT Manual Fallback (`UJ-2`, PRD Rule 3, `UX-DR6`, `AC-2`)
- **FR-7.6**: Epic 2 (Story 2.4) — Strict AI Action Boundary: Model Output Schema/Business Validation Gate (Decision 7, Dimension G, `ARCH-3`)
- **FR-8.1**: Epic 4 (Story 4.4) — Supportive Non-Punishing Companion Pet with Zero Guilt Mechanics (`UJ-5`, Decision 8, Dimension F, `UX-DR11`)
- **FR-8.2**: Epic 4 (Story 4.2) — Genuine Cross-Domain Completion Coin Rewards into Auditable Ledger (`UJ-5`, Decision 8, PRD Rule 5, `ARCH-6`, `AC-6`)
- **FR-8.3**: Epic 4 (Story 4.2) — Reward-Event Idempotency Preventing Duplicate Coin Awards (`UJ-5`, PRD Rule 5, `ARCH-6`)
- **FR-8.4**: Epic 4 (Story 4.5) — Exclusive Coin Sinks: Cosmetic Outfits, Accessories & Background Decor (`UJ-5`, Decision 8, `UX-DR13`)
- **FR-8.5**: Epic 4 (Story 4.1) — Architectural Separation of Streaks and Coins: No Paid Freezes (Decision 8, `UX-DR4`)
- **FR-9.1**: Epic 1 (Story 1.2) & Epic 6 (Story 6.4) — Room + SQLCipher At-Rest Encryption & OS Sandbox Protection (Decision 9, `ARCH-2`, `NFR-4`, `AC-7`)
- **FR-9.2**: Epic 6 (Story 6.4) — 100% Offline-Capable Local-First Core Architecture (Decision 9, `NFR-2`, `AC-8`)
- **FR-9.3**: Epic 6 (Story 6.1) — Password-Protected AES-256-GCM .ptbackup Export (`UJ-6`, Decision 9, `ARCH-7`, `UX-DR14`, `AC-7`)
- **FR-9.4**: Epic 6 (Story 6.2) — 4-Step Backup Import Validation Pipeline (`UJ-6`, PRD Rule 6, `ARCH-7`, `UX-DR14`)
- **FR-9.5**: Epic 6 (Story 6.2) — Pre-Restore SQLite Safety Snapshot Creation (`UJ-6`, PRD Rule 6, `ARCH-7`)
- **FR-9.6**: Epic 6 (Story 6.2) — Automatic Safety Snapshot Rollback on Import Failure (`UJ-6`, PRD Rule 6, `ARCH-7`, `UX-DR14`)
- **FR-10.1**: Epic 3 (Story 3.3) — Emergency Call & Dialer System Immunity (`UJ-3`, PRD Rule 2, `ARCH-5`)
- **FR-10.2**: Epic 3 (Story 3.6) — OS Process Kill/Reboot INTERRUPTED/RESUMABLE State Reconstruction (`UJ-3`, PRD Rule 2, `ARCH-8`)
- **FR-10.3**: Epic 3 (Story 3.5) — Explicit Session Cancellation to CANCELLED State without Resumption Prompt (PRD Rule 2)

---

## Epic List

### Epic 1: Project Foundation, DayType Schedule Engine & Core Home Experience
Users can launch the app to their automatically resolved daily schedule, view their time-blocked schedule and action list beneath a dynamic Hero Card, perform 1-tap single-date DayType template swaps, and create/manage core Tasks and sequential Routines in an encrypted, dark/light theme environment.
- **FRs Covered:** FR-1.1, FR-1.2, FR-2.1, FR-2.2, FR-2.3, FR-3.1, FR-3.2, FR-3.3, FR-3.4, FR-3.5
- **NFRs Covered:** NFR-1 (Cold launch < 1.5s `[ENGINEERING TARGET]`), NFR-2 (Offline reliability `[CONFIRMED]`), NFR-4 (Encrypted storage baseline `[CONFIRMED]`)
- **ARCH / UX-DR Mapping:** ARCH-1, ARCH-2, ARCH-9, UX-DR1, UX-DR2, UX-DR3, UX-DR4, UX-DR18
- **Acceptance Criteria & User Journeys:** AC-1, UJ-1

### Epic 2: Low-Friction Multimodal Capture & Confidence Routing Pipeline
Users can capture tasks and schedule intents in seconds through streaming online voice (Gemini Live over WebSocket), offline on-device speech recognition (`createOnDeviceSpeechRecognizer`), or one-line text quick-add, with deterministic attribute parsing, strict AI Action Boundary enforcement, 5-second tap-to-undo auto-commits, 1-tap editable confirmation chips for medium-confidence captures, and an unfiled inbox for ambiguous transcripts.
- **FRs Covered:** FR-7.1, FR-7.2, FR-7.3, FR-7.4, FR-7.5, FR-7.6
- **NFRs Covered:** NFR-1 (Deterministic NLP parse < 300ms `[ENGINEERING TARGET]`), NFR-2 (Offline capture `[CONFIRMED]`), NFR-4 (Immediate raw audio purge post-STT `[CONFIRMED]`)
- **ARCH / UX-DR Mapping:** ARCH-3, ARCH-4, UX-DR5, UX-DR6, UX-DR16
- **Acceptance Criteria & User Journeys:** AC-2, UJ-2

### Epic 3: Focus Mode, Distraction Interception & Resumption Engine
Users can initiate focused work and study sessions with distraction interception and intervention on supported and configured Android devices via an opt-in event-driven `AccessibilityService`, experience gentle single-prompt JITAI nudges during scheduled blocks, navigate a deliberate override friction ladder when attempting to open blocked apps, enjoy automatic immunity for telecom and emergency calls, and resume interrupted sessions seamlessly from the Hero Card without guilt or data loss.
- **FRs Covered:** FR-1.3, FR-1.4, FR-1.5, FR-2.5, FR-4.1, FR-4.2, FR-4.3, FR-4.4, FR-4.5, FR-4.6, FR-10.1, FR-10.2, FR-10.3
- **NFRs Covered:** NFR-3 (Idle AccessibilityService CPU < 2% `[ENGINEERING TARGET]`), NFR-4 (Zero text/password logging `[CONFIRMED / TECHNICAL CONSTRAINT]`)
- **ARCH / UX-DR Mapping:** ARCH-5, ARCH-8, UX-DR7, UX-DR8
- **Acceptance Criteria & User Journeys:** AC-3, UJ-3

### Epic 4: Habits, Non-Punishing Companion & Coins Economy
Users can track sustainable habits with miss-tolerant streak slack and freeze days, watch a non-punishing virtual companion pet celebrate their accomplishments on the Hero Card (with zero guilt, HP loss, or character death), earn Coins into an idempotent ledger from genuine completions across all domain items, and spend earned Coins in a local cosmetic shop on companion outfits, accessories, and room decor.
- **FRs Covered:** FR-2.4, FR-2.6, FR-8.1, FR-8.2, FR-8.3, FR-8.4, FR-8.5
- **NFRs Covered:** NFR-2 (Local economy operation `[CONFIRMED]`)
- **ARCH / UX-DR Mapping:** ARCH-6, UX-DR11, UX-DR12, UX-DR13
- **Acceptance Criteria & User Journeys:** AC-6, UJ-5

### Epic 5: Tiered Notifications, Scarcity Guardrails & Rescheduling Review
Users receive intentional, tiered notifications (Routine quiet, Important standard, Urgent heads-up) with acknowledgement-based bounded escalation, get gentle soft-cap guidance against overbooking urgent items, and resolve uncompleted flexible tasks through an evening/morning Carry-Forward review card equipped with an advisory workload capacity indicator.
- **FRs Covered:** FR-5.1, FR-5.2, FR-5.3, FR-5.4, FR-5.5, FR-6.1, FR-6.2, FR-6.3, FR-6.4, FR-6.5
- **NFRs Covered:** NFR-3 (Exact alarms via `setAlarmClock()`, deferred maintenance via `WorkManager` `[CONFIRMED]`)
- **ARCH / UX-DR Mapping:** UX-DR9, UX-DR10
- **Acceptance Criteria & User Journeys:** AC-4, AC-5, UJ-4

### Epic 6: Local Data Security, Encrypted Backup & System Settings
Users maintain full, private ownership of their data through AES-256-GCM encrypted `.ptbackup` export/import containers, pre-restore SQLite safety snapshots that automatically roll back if import validation fails, and transparent capability diagnostics for all system permissions and offline/online modes.
- **FRs Covered:** FR-9.1, FR-9.2, FR-9.3, FR-9.4, FR-9.5, FR-9.6
- **NFRs Covered:** NFR-4 (Room + SQLCipher encryption, Android Keystore master key, zero telemetry `[CONFIRMED]`), NFR-5 (Encrypted portable backup `[CONFIRMED]`)
- **ARCH / UX-DR Mapping:** ARCH-7, UX-DR14, UX-DR15, UX-DR17
- **Acceptance Criteria & User Journeys:** AC-7, AC-8, UJ-6

---

## Epic 1: Project Foundation, DayType Schedule Engine & Core Home Experience

**Epic Goal:** Users can launch the app to their automatically resolved daily schedule, view their time-blocked schedule and action list beneath a dynamic Hero Card, perform 1-tap single-date DayType template swaps, and create/manage core Tasks and sequential Routines in an encrypted, dark/light theme environment.

### Story 1.1: Android Greenfield Project Setup, Compose Theme & Design Tokens

As a user,
I want the application to launch into a beautifully themed, responsive Android interface supporting dark and light modes,
So that I have a consistent, accessible visual foundation for managing my daily routines.

**Acceptance Criteria:**

**Given** a clean development environment targeting Android 13–16 (API 33–36 baseline `[PROVISIONAL TECHNICAL TARGET]`)
**When** the project is initialized with Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`), AGP 8.8.0, Kotlin 2.4.0, KSP, and Jetpack Compose BOM 2026.02.00
**Then** the project compiles with zero legacy KAPT dependencies and configures Hilt DI (`com.google.dagger:hilt-android:2.55`)
**And** Jetpack Compose Material 3 theme tokens are fully implemented from `design-system.md` and `DESIGN.md` (Surfaces, Primary `#2563EB`/`#60A5FA`, Secondary `#6366F1`/`#A5B4FC`, Warm Accent `#D97706`/`#FBBF24`, Companion Green `#059669`/`#34D399`, Semantic colors, Ink hierarchy, Plus Jakarta Sans UI typography, Inter tabular typography, 4dp spacing grid, and shapes `xs` to `xxl`)
**And** 5-tab Bottom Navigation (`Home`, `Habits`, `Study`, `Companion`, `Settings`) is rendered with edge-to-edge `WindowInsets` handling and minimum 48dp touch targets (`UX-DR1`, `UX-DR18`, `ARCH-1`).

### Story 1.2: SQLCipher Encrypted Database Baseline & Master Key Management

As a user,
I want all my local data stored in a hardware-isolated, encrypted database on my device,
So that my private tasks and schedules are completely protected at rest without cloud exposure.

**Acceptance Criteria:**

**Given** an Android device with Android Keystore capability
**When** the app initializes for the first time
**Then** `SecurityKeyStoreManager` creates an AES-256-GCM hardware-backed master key under alias `PersonalTracker_MasterDB_Key` with `setUserAuthenticationRequired(false)` to allow background alarm dispatching
**And** a 32-byte randomized passphrase is generated via `SecureRandom`, encrypted by the Keystore master key, and stored in app-private storage
**And** Room initializes over SQLCipher (`net.zetetic:sqlcipher-android:4.6.1` with 16 KB native ELF page alignment `[ARCHITECTURE RESOLVED]`, 4096-byte page size, and `PBKDF2WithHmacSHA512` at 256,000 iterations)
**And** all Room DAOs are declared internal within `:data:local:db` and bounded strictly to `Dispatchers.IO` (`ARCH-2`, `ARCH-9`, `FR-9.1`, `NFR-4`, `AC-7`).

### Story 1.3: DayType Entity Models, Resolution Engine & Morning Swap

As a user,
I want the app to automatically determine today's active schedule template upon launch with an option to swap it for today only,
So that my schedule adapts to my day without requiring repetitive daily setup.

**Acceptance Criteria:**

**Given** default saved DayType templates (`Weekday Routine`, `Weekend`, `Heavy Study`) and day-of-week mappings
**When** the app opens or a new day begins
**Then** `ResolveDayTypeUseCase` evaluates the v1 resolution hierarchy: `1) Date-Specific User Override > 2) Day-of-Week Default` `[CONFIRMED]` with zero mandatory taps (`FR-3.1`, `FR-3.2`, `AC-1`)
**And** calendar synchronization is excluded from v1 resolution (`[EXCLUDED]`)
**And** a non-blocking morning DayType banner appears on Home displaying active template and 1-tap swap option (`FR-3.3`)
**When** the user taps swap and selects a different DayType
**Then** a `DayOverrideEntity` is saved for today's date only, leaving base templates unmutated (`FR-3.4`, `AC-1`)
**And** the active schedule refreshes to reflect the newly selected template (`UX-DR3`, `UJ-1`).

### Story 1.4: Dynamic State-Aware Hero Card Container & Intentional Idle State

As a user,
I want a dynamic Hero Card at the top of my Home screen that clearly shows my current actionable priority,
So that I know what to focus on immediately without visual clutter.

**Acceptance Criteria:**

**Given** the Home screen surface
**When** `HeroViewModel` evaluates current system state
**Then** the Hero Card renders the highest-priority active state among: 1) Active Focus/Task, 2) Interrupted/Resumable, 3) Morning DayType Banner, 4) Upcoming Scheduled Item, 5) Carry-Forward Review, 6) Intentional Idle State (`FR-1.1`, `UX-DR2`)
**And** state transitions animate with 300ms `FastOutSlowIn` cross-fade while card container dimensions remain stable
**And** when no items are active, upcoming, or interrupted, the Hero Card displays the Intentional Idle state featuring the resting companion and calm copy ("All caught up for today. Rest well.")
**And** Today's Schedule and Action List views sit as subordinate layouts below the Hero Card (`FR-1.2`, `NFR-1`, `UJ-1`).

### Story 1.5: Core Task Management & Delivery-Intensity Classification

As a user,
I want to create, classify, and complete discrete tasks using delivery-intensity levels (Routine, Important, Urgent),
So that I can organize and execute my daily actionable workload while keeping task importance separate from notification delivery behavior.

**Acceptance Criteria:**

**Given** `TaskEntity` persisted in the local encrypted database (`id`, `title`, `description`, `scheduledDate`, `scheduledTime`, `estimatedDurationMinutes`, `deliveryIntensity`, `state`, `isCarryForward`, `carryForwardCount`, `completionTimestamp`, `createdTimestamp`)
**When** the user creates or edits a Task
**Then** the Task supports delivery intensity classes (`ROUTINE`, `IMPORTANT`, `URGENT` `[CONFIRMED]`) which drive distinct visual left accent stripes (Routine: 3dp `{colors.info}`, Important: 3dp `{colors.warning}`, Urgent: 6dp `{colors.danger}` + lightning badge; `FR-2.1`, `FR-2.2`, `UX-DR4`)
**And** delivery intensity is decoupled from arbitrary numeric priority scales (P1-P4 scales are prohibited)
**When** the user toggles a task checkbox or swipes right
**Then** the task transitions to `COMPLETED` state with a 300ms subtle background fill, strikethrough text in `{colors.ink-tertiary}`, and a satisfying haptic pulse
**And** flexible tasks support manual ordering within the Action List.

### Story 1.6: Sequential Routine Domain Engine & Mid-Day Schedule Recalculation

As a user,
I want to execute structured multi-step routines bound to my DayType, and have my schedule intelligently adapt if I swap templates mid-day,
So that I can build structured habits without losing credit for steps already completed.

**Acceptance Criteria:**

**Given** `RoutineEntity` and sequential `RoutineStepEntity` linked to a `DayTypeEntity` (`FR-2.3`, `UX-DR4`)
**When** the user starts a routine from the Home surface
**Then** the Routine Execution UI displays sequential step progression, highlighting the current step and dimming future steps
**When** the user performs a mid-day DayType swap after completing some routine steps or tasks
**Then** the mid-day swap engine preserves all completed and in-progress items as immutable historical records (`FR-3.5`, `PRD RULE 4`)
**And** recalculates only the remaining unstarted schedule for today from the newly selected DayType
**And** the persistent bottom sheet timeline (Peek ~100dp, Half ~50%, Full ~100%) updates to reflect remaining unstarted blocks (`UX-DR3`, `AC-1`).

---

## Epic 2: Low-Friction Multimodal Capture & Confidence Routing Pipeline

**Epic Goal:** Users can capture tasks and schedule intents in seconds through streaming online voice (Gemini Live over WebSocket), offline on-device speech recognition (`createOnDeviceSpeechRecognizer`), or one-line text quick-add, with deterministic attribute parsing, strict AI Action Boundary enforcement, 5-second tap-to-undo auto-commits, 1-tap editable confirmation chips for medium-confidence captures, and an unfiled inbox for ambiguous transcripts.

### Story 2.1: Morphing Voice Capture FAB & Audio Input Lifecycle

As a user,
I want a responsive floating action button that morphs into a live audio visualizer when tapped,
So that I can capture thoughts and tasks hands-free with immediate visual feedback.

**Acceptance Criteria:**

**Given** the Voice Capture FAB resting above the bottom navigation bar
**When** the user taps the FAB and grants microphone permission
**Then** the FAB animates in 300ms from a 56dp circle into a stadium pill displaying a pulsing waveform and real-time live preview chips (`UX-DR5`, `FR-7.1`)
**And** the FAB dynamically repositions above the IME keyboard when soft keyboard appears
**And** audio recording streams directly to memory buffers with zero persistent raw audio file writes to disk (`NFR-4`, `FR-7.2`)
**When** speech input ceases
**Then** the raw audio buffer is immediately purged from memory upon transcript generation (`FR-7.2`, `NFR-4`).

### Story 2.2: Deterministic Kotlin Regex & Rule NLP Parser

As a user,
I want natural language text and speech parsed offline into structured task attributes in milliseconds,
So that dates, times, durations, and priority levels are extracted accurately without cloud dependency.

**Acceptance Criteria:**

**Given** a raw transcript string (from voice capture or one-line text quick-add)
**When** `DeterministicTaskParser` processes the input on `Dispatchers.Default`
**Then** parsing completes in < 300ms (`NFR-1`)
**And** correctly extracts Title, Date (relative/explicit), Time, Duration (minutes), Item Type (`TASK`, `ROUTINE`, `HABIT`, `STUDY`), Delivery Intensity (`ROUTINE`, `IMPORTANT`, `URGENT`), and Subject Tag
**And** outputs an `UnvalidatedTaskProposal` DTO containing extracted fields and a calculated parser confidence score (0.0 to 1.0; `ARCH-3`).

### Story 2.3: Dual Voice Engine & Network Failover Architecture

As a user,
I want to use real-time streaming voice when online and automatic offline speech recognition when offline,
So that voice capture is fast and reliable regardless of internet connectivity.

**Acceptance Criteria:**

**Given** `VoiceRouterService` orchestrating speech capture (`ARCH-4`, `FR-7.2`)
**When** network connectivity is active
**Then** voice input connects to Gemini Live API over WebSocket for real-time bidirectional streaming assistance `[CONFIRMED]` and produces an `UnvalidatedTaskProposal` DTO
**When** network connectivity is unavailable or the WebSocket disconnects unexpectedly mid-stream
**Then** `VoiceRouterService` automatically and gracefully falls back to Android `SpeechRecognizer` (`createOnDeviceSpeechRecognizer` when available) without crashing or blocking (`UX-DR16`, `NFR-2`)
**When** on-device speech recognizer is unavailable or missing language packs
**Then** the system falls back directly to manual text entry quick-add (`FR-7.5`).

### Story 2.4: Strict AI Action Boundary & Command Validation Pipeline

As a user,
I want AI and speech recognition outputs validated against strict business rules before touching my database,
So that hallucinations or parsing errors never corrupt my saved tasks and schedules.

**Acceptance Criteria:**

**Given** an `UnvalidatedTaskProposal` emitted by Gemini Live, SpeechRecognizer, or NLP parser
**When** the proposal enters the domain validation engine
**Then** domain rules verify title non-emptiness, date/time validity, and enum bounds (`ARCH-3`, `FR-7.6`)
**And** only upon passing schema validation is a `ValidatedTaskCreationCommand` instantiated
**And** `TaskRepository` maps the command to an immutable `TaskEntity` and executes the Room insert
**And** AI packages (`system.ai.gemini`, `system.speech`) are strictly prohibited from having direct Room DAO dependencies or write access (`ARCH-3`, `FR-7.6`).

### Story 2.5: Three-Tier Confidence Routing, Instant Commit & Undo Toast

As a user,
I want high-confidence voice captures saved immediately with an undo option, while ambiguous captures ask for quick confirmation,
So that fast entries require zero extra taps while uncertain entries are never misfiled.

**Acceptance Criteria:**

**Given** a validated task proposal evaluated by `ConfidencePolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`, baseline: High >= 0.85, Med 0.60-0.84, Low < 0.60; `FR-7.3`, `FR-7.4`, `FR-7.5`, `UX-DR6`)
**When** confidence is High (>= 0.85)
**Then** the task auto-commits instantly and displays a 5-second Undo Toast pill (`[HYPOTHESIS]`); tapping Undo deletes the task (`AC-2`)
**When** confidence is Medium (0.60-0.84)
**Then** an inline Confirmation Card appears in the Hero Card with 1-tap editable chips (`Title`, `Date`, `Time`, `Type`, `Subject`, `Duration`, `Urgency`); user must tap Confirm to persist (`FR-7.4`, `PRD RULE 3`)
**When** confidence is Low (< 0.60) or parsing is ambiguous
**Then** the raw transcript and partial draft are preserved in `UnfiledCaptureInboxEntity` for 1-tap categorization (`FR-7.5`, `AC-2`, `UJ-2`).

---

## Epic 3: Focus Mode, Distraction Interception & Resumption Engine

**Epic Goal:** Users can initiate focused work and study sessions with distraction interception and intervention on supported and configured Android devices via an opt-in event-driven `AccessibilityService`, experience gentle single-prompt JITAI nudges during scheduled blocks, navigate a deliberate override friction ladder when attempting to open blocked apps, enjoy automatic immunity for telecom and emergency calls, and resume interrupted sessions seamlessly from the Hero Card without guilt or data loss.

### Story 3.1: Focus & Study Session Models, Timer & Heatmap Tracking

As a user,
I want to start timed focus and study sessions with subject tags and view historical study heatmaps,
So that I can dedicate focused time to specific subjects and track my study consistency.

**Acceptance Criteria:**

**Given** `FocusSessionEntity` and `StudySessionEntity` persisted in the local encrypted database
**When** the user starts a Focus or Study session
**Then** a prominent countdown/count-up timer displays in `{typography.data-large}` on the Hero Card (priority 1 state) and Study surface (`FR-2.5`, `FR-4.1`, `UX-DR2`)
**When** a study session completes
**Then** session duration is logged and aggregated into the Subject Study Heatmap rollup
**And** sessions meeting the validity threshold (`[HYPOTHESIS: 10-minute minimum]`) qualify for Coin reward allocation (`FR-2.5`, `FR-8.2`).

### Story 3.2: Event-Driven Accessibility Distraction Interception & Privacy Invariant

As a user,
I want distracting apps intercepted on my supported Android device during active focus sessions with complete assurance that my screen content and keystrokes are never logged,
So that I can stay on task without compromising my personal privacy.

**Acceptance Criteria:**

**Given** an active `FocusSession` and user-configured distraction packages in `InterventionRuleEntity` on a supported and configured Android device
**When** `AccessibilityInterventionService` monitors app transitions via user-granted opt-in accessibility access
**Then** it uses event-driven `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED` detection only, maintaining < 2% idle CPU usage (`ARCH-5`, `NFR-3`, `FR-4.3`)
**And** strictly adheres to zero-logging: no inspection, recording, or storage of screen text, edit fields, passwords, OTPs, or messages (`[CONFIRMED / TECHNICAL CONSTRAINT]`, `NFR-4`)
**When** a configured distracting app enters foreground while focus is active
**Then** the service immediately surfaces the distraction intervention overlay.

### Story 3.3: Emergency & Telecom Dynamic Whitelist Bypass

As a user,
I want incoming phone calls, dialers, and emergency services to bypass distraction blocking immediately,
So that critical communications and emergencies are never obstructed by focus mode.

**Acceptance Criteria:**

**Given** an active `FocusSession` with distraction overlay active
**When** an incoming or outgoing call occurs, or an emergency dialer is opened
**Then** `TelecomManager.getDefaultDialerPackage()` dynamic whitelist and `TelephonyManager` listener (`CALL_STATE_RINGING`/`CALL_STATE_OFFHOOK`) immediately suspend the overlay window (`FR-4.6`, `FR-10.1`, `ARCH-5`, `PRD RULE 2`)
**And** emergency calls and telecom dialers are NEVER blocked (`[CONFIRMED / TECHNICAL CONSTRAINT]`)
**And** FocusSession timer behavior during active phone calls is handled per configurable `FocusTimerPolicy` (`[OPEN DECISION / TECHNICAL VALIDATION]`: auto-pause vs continue countdown without hardcoded bias).

### Story 3.4: Adaptive Intervention Ladder, JITAI Restraint & Override Friction

As a user,
I want gentle single nudges if I open distracting apps during scheduled blocks, and deliberate friction if I try to break an active focus session,
So that I am helped to stay focused without experiencing annoying notification spam.

**Acceptance Criteria:**

**Given** a scheduled focus block without an active session
**When** the user opens a distracting app during the scheduled window
**Then** the system displays at most ONE contextual JITAI prompt ("Active Schedule: [Title] — Start Focus or Snooze") and never spams repeatedly (`FR-4.2`, `PRD RULE 2`)
**Given** an active focus session
**When** the user attempts to exit through the blurred intervention overlay (`{colors.surface-overlay}`)
**Then** exiting requires deliberate hold-to-override friction (`FrictionPolicy` `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`, e.g. continuous hold with escalation upon repeat breach; `FR-4.4`, `UX-DR7`, `AC-3`, `UJ-3`).

### Story 3.5: Ovsiankina Resumption Engine & Hero Resumption Card

As a user,
I want interrupted tasks surfaced on my Hero Card with 1-tap resumption actions when I return,
So that I can resume interrupted work effortlessly without guilt or lost progress.

**Acceptance Criteria:**

**Given** an active focus session exited early or a scheduled block expiring with item `IN_PROGRESS`
**When** the interruption occurs
**Then** the item state transitions to `INTERRUPTED/RESUMABLE` (`FR-4.5`, `PRD RULE 1`)
**And** the Hero Card displays a persistent Resumption Card (priority 2 state) showing remaining duration and actions: `[Resume]`, `[Review/Adjust]`, `[Mark Complete]` (`FR-1.3`, `UX-DR8`, `AC-3`)
**And** system MUST NEVER auto-complete or silently reschedule interrupted items (`FR-1.4`, `FR-1.5`)
**When** user explicitly taps Cancel Session, state transitions to `CANCELLED` with zero resumption prompt (`FR-10.3`).

### Story 3.6: OS Process Termination & Reboot State Reconstruction

As a user,
I want my active focus session safely reconstructed after an unexpected phone reboot or low-memory process kill,
So that system crashes or battery deaths never erase my in-progress work.

**Acceptance Criteria:**

**Given** an incomplete `IN_PROGRESS` session when an OS kill, low-memory termination, or reboot occurs
**When** `BootCompletedReceiver` receives `BOOT_COMPLETED`
**Then** all scheduled exact alarms are re-registered with `AlarmManager` (`ARCH-8`, `FR-10.2`)
**When** the app is next launched
**Then** startup lifecycle hooks detect the unclosed `IN_PROGRESS` session from persistent Room storage and reconstruct it into `INTERRUPTED/RESUMABLE` state (`FR-10.2`, `PRD RULE 2`)
**And** the Hero Card surfaces the Resumption Card allowing immediate resumption.

---

## Epic 4: Habits, Non-Punishing Companion & Coins Economy

**Epic Goal:** Users can track sustainable habits with miss-tolerant streak slack and freeze days, watch a non-punishing virtual companion pet celebrate their accomplishments on the Hero Card (with zero guilt, HP loss, or character death), earn Coins into an idempotent ledger from genuine completions across all domain items, and spend earned Coins in a local cosmetic shop on companion outfits, accessories, and room decor.

### Story 4.1: Habit Entity Models, Cadence Tracking & Streak Grace Engine

As a user,
I want to track habits with flexible cadences and earned streak freezes,
So that missing a single day due to life circumstances does not destroy weeks of momentum.

**Acceptance Criteria:**

**Given** `HabitEntity` (`id`, `title`, `cadenceType` DAILY/WEEKDAYS/CUSTOM, `customDaysMask`, `currentStreakDays`, `bestStreakDays`, `earnedSlackBankDays`, `isFreezeActiveToday`)
**When** the user completes a habit on its scheduled cadence
**Then** streak increments and `earnedSlackBankDays` accumulates per configurable `StreakPolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`; `FR-2.4`, `UX-DR4`)
**When** a scheduled cadence day is missed but earned streak slack is available
**Then** a freeze day is absorbed automatically, marking status `FREEZE_ABSORBED` and preserving the streak count without guilt visuals (`FR-2.4`)
**And** streak freeze quantities, earning rates, banking limits, and recovery windows are governed by `StreakPolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`)
**And** streak system operates completely independent of Coins; Coins MUST NOT purchase Streak Freezes in v1 (`FR-8.5`).

### Story 4.2: Idempotent Coin Ledger Engine & Anti-Farming Model

As a user,
I want to earn Coins into an auditable ledger only from genuine completions,
So that I am fairly rewarded for real accomplishments while preventing accidental duplicate awards.

**Acceptance Criteria:**

**Given** genuine completion of a Task, Routine Step, Habit, or Study Session (`FR-8.2`, `FR-2.6`)
**When** `AwardCoinsUseCase` processes the completion event
**Then** a `CoinLedgerEntity` record is inserted with unique compound `idempotencyKey` (e.g. `"TASK_<id>_<timestamp>"`, `"HABIT_<id>_<date>"`, `"STUDY_<id>_<timestamp>"`; `ARCH-6`, `FR-8.3`, `PRD RULE 5`)
**And** database unique constraint guarantees atomic execution where duplicate UI toggles or app restarts award Coins exactly once (`AC-6`)
**And** Coin reward amounts are mapped via domain `RewardPolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`)
**And** Routine reward granularity is preserved as `[OPEN DECISION / TECHNICAL VALIDATION]` (per-step vs full-sequence completion bonus governed by `RewardPolicy`).

### Story 4.3: VSYNC-Aligned Bézier Coin Earn Animation & Coin HUD

As a user,
I want to see a smooth, rewarding coin animation arc into my balance when I complete an item,
So that my accomplishments feel tangible and visually satisfying across any screen refresh rate.

**Acceptance Criteria:**

**Given** a persistent Coin HUD badge pill (`{colors.coin-glow}`, `{colors.coin-gold}`, `{typography.data}`) on the Home screen
**When** a genuine completion triggers a Coin reward
**Then** `CoinArcAnimation.kt` renders a quadratic Bézier arc from completion source to Coin HUD (`UX-DR12`, `FR-8.2`)
**And** animation uses `withFrameNanos` / `Choreographer` elapsed-time timing to guarantee identical visual duration (600–1000ms spring curve) on both 60Hz and 120Hz displays (`design-system.md`)
**When** system "Reduce Motion" accessibility setting is enabled
**Then** the arc animation is skipped and the HUD counter increments immediately.

### Story 4.4: Non-Punishing Companion Widget & 7-State Rive Machine

As a user,
I want a supportive virtual companion on my Hero Card that celebrates my wins without ever punishing or guilting me,
So that my productivity app feels encouraging rather than judgmental.

**Acceptance Criteria:**

**Given** `CompanionProfileEntity` and the Companion widget container on the Hero Card (`UX-DR11`, `FR-8.1`)
**When** user actions trigger companion state transitions
**Then** the Rive Android Runtime (`app.rive:rive-android:9.1.0`, `companion_sm`) evaluates states 0–6: `0: Idle/Content`, `1: Celebrating` (completion particles), `2: Encouraging` (warm wave), `3: Concerned` (streak at risk — gentle awareness, never crying), `4: Focused` (eyes closed meditation during focus session), `5: Sleeping/Resting` (off-day/evening rest), `6: Excited` (milestone celebration; `AC-6`, `UJ-5`)
**And** strict non-punishing invariant is enforced: zero HP loss, zero character death, zero sadness visuals, zero guilt copy (`FR-8.1`)
**And** Companion species selection is preserved as `[OPEN DECISION / TECHNICAL VALIDATION]` (fixed mascot vs user-selectable species).

### Story 4.5: Cosmetic Shop UI & Companion Customization

As a user,
I want to spend my earned Coins in a local shop on companion outfits, accessories, and room decor,
So that I have meaningful, fun cosmetic customization goals without paywalls or real-money purchases.

**Acceptance Criteria:**

**Given** `ShopItemEntity` catalog pre-seeded with Outfits, Accessories, and Background Decor categories (`FR-8.4`, `UX-DR13`)
**When** the user opens the Shop from the Companion tab
**Then** item cards display unlocked/locked status, preview art, and price in `{colors.coin-gold}`
**When** the user confirms purchase of an affordable locked item
**Then** `CoinLedgerEntity` atomically records the `SHOP_PURCHASE` deduction, unlocks the item, and equips it on the companion
**And** Coins in v1 are spent EXCLUSIVELY in the local Shop on companion cosmetics (`FR-8.4`, `[CONFIRMED]`).

---

## Epic 5: Tiered Notifications, Scarcity Guardrails & Rescheduling Review

**Epic Goal:** Users receive intentional, tiered notifications (Routine quiet, Important standard, Urgent heads-up) with acknowledgement-based bounded escalation, get gentle soft-cap guidance against overbooking urgent items, and resolve uncompleted flexible tasks through an evening/morning Carry-Forward review card equipped with an advisory workload capacity indicator.

### Story 5.1: Tiered Notification Channels & Exact Alarm Scheduling Engine

As a user,
I want notifications delivered in distinct intensity tiers with exact reminder timing,
So that critical items get my immediate attention while routine items remain quiet and non-intrusive.

**Acceptance Criteria:**

**Given** Android Notification Channels initialization (`UX-DR10`, `FR-5.1`)
**When** the app creates channels
**Then** three distinct channels are configured: `Channel_Routine` (`IMPORTANCE_LOW`, quiet/in-app), `Channel_Important` (`IMPORTANCE_DEFAULT`, standard OS notification), and `Channel_Urgent` (`IMPORTANCE_HIGH`, heads-up display + distinctive companion audio/haptic pattern)
**When** exact reminders are scheduled
**Then** `AlarmManager` registers `setAlarmClock()` / `setExactAndAllowWhileIdle()` with runtime `canScheduleExactAlarms()` verification and fallback handling (`NFR-3`, `AC-4`).

### Story 5.2: Acknowledgement-Based Bounded Escalation Engine

As a user,
I want urgent notifications to re-fire boundedly until acknowledged without experiencing spammy or derogatory copy,
So that I don't miss time-critical obligations while maintaining a respectful user experience.

**Acceptance Criteria:**

**Given** an uncompleted `URGENT` delivery intensity item scheduled for reminder
**When** the notification triggers and is not dismissed or completed
**Then** `EscalationPolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`, with configurable retry cadence and count, e.g. 5-minute interval, max 3 re-fires) executes bounded escalation (`FR-5.4`, `AC-4`)
**And** the word "nag" is strictly prohibited across all notification strings, code, comments, and logs (`FR-5.4`)
**When** the item is completed or dismissed
**Then** escalation halts immediately and the active Urgent quota slot is released (`FR-5.5`).

### Story 5.3: Urgency Soft-Cap Scarcity Guidance

As a user,
I want gentle guidance when I mark too many items as Urgent, without being hard-blocked from setting my own priorities,
So that I keep urgent priorities genuinely scarce while retaining full autonomy.

**Acceptance Criteria:**

**Given** 2 active Urgent items already present today
**When** the user attempts to mark a 3rd active item as Urgent (`UrgencyScarcityPolicy` `[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`, `FR-5.2`, `UX-DR10`)
**Then** the system displays a soft guidance dialog: "You already have 2 Urgent items today. Keep Urgent or Change to Important?" (`AC-4`)
**When** the user selects "Keep Urgent"
**Then** the item is saved as Urgent without hard-blocking, silent downgrading, or disabling the Urgent option (`FR-5.3`, `[CONFIRMED]`)
**When** the user selects "Change to Important"
**Then** the item is saved with `IMPORTANT` delivery intensity.

### Story 5.4: Evening/Morning Carry-Forward Review Card

As a user,
I want a dedicated review card for unfinished flexible tasks at evening review or next morning,
So that uncompleted work is intentionally handled rather than silently rolling over or getting lost.

**Acceptance Criteria:**

**Given** uncompleted flexible Tasks present at evening review window (configurable time) or next morning launch
**When** `CarryForwardUseCase` runs
**Then** the Hero Card surfaces the Carry-Forward Review Card (priority 5 state; `FR-6.1`, `UX-DR9`, `AC-5`)
**And** provides explicit per-item action buttons: `[Do Tomorrow]`, `[Keep Unscheduled]`, `[Choose Another Day]` (date picker), and `[Cancel]` (`FR-6.2`, `UJ-4`)
**And** Fixed-Time Events expire without auto-rollover, requiring explicit re-keying (`FR-6.3`)
**And** Routine Occurrences expire for that day without stacking (`FR-6.4`).

### Story 5.5: Advisory Workload Capacity Indicator & Guardrails

As a user,
I want an advisory capacity indicator showing tomorrow's planned workload when I carry tasks forward,
So that I avoid overbooking tomorrow while retaining final control over my schedule.

**Acceptance Criteria:**

**Given** the Carry-Forward Review Card
**When** the user selects "Do Tomorrow" for one or more tasks
**Then** `CapacityPolicy` (`[VALIDATION REQUIRED / CONFIGURABLE HEURISTIC]`) calculates tomorrow's total planned duration (`FR-6.5`, `UX-DR9`)
**And** displays an advisory capacity bar and text: "Tomorrow currently has 6h 45m scheduled; moving these 3 tasks brings it to 8h 10m" (`AC-5`, `UJ-4`)
**And** the bar shifts color (`{colors.success}` normal $ightarrow$ `{colors.warning}` heavy $ightarrow$ `{colors.danger}` overloaded)
**And** strict invariant: the workload capacity indicator is strictly advisory and MUST NEVER hard-block rescheduling (`FR-6.5`, `[CONFIRMED]`).

---

## Epic 6: Local Data Security, Encrypted Backup & System Settings

**Epic Goal:** Users maintain full, private ownership of their data through AES-256-GCM encrypted `.ptbackup` export/import containers, pre-restore SQLite safety snapshots that automatically roll back if import validation fails, and transparent capability diagnostics for all system permissions and offline/online modes.

### Story 6.1: Encrypted Portable `.ptbackup` Export Engine

As a user,
I want to export a password-protected, encrypted backup file of all my data to my local storage,
So that I can safeguard and transfer my tracker history securely without relying on cloud servers.

**Acceptance Criteria:**

**Given** the Settings $ightarrow$ Data & Backup surface
**When** the user taps Export Backup and provides a password
**Then** a strength indicator validates password input (`UX-DR14`, `FR-9.3`)
**And** `BackupManager` derives a 256-bit key using `PBKDF2WithHmacSHA512` at **256,000 iterations** with 32-byte `SecureRandom` salt (`ARCH-7`, `NFR-5`, `[ARCHITECTURE RESOLVED]`)
**And** serializes all domain entities into Gzipped JSON, encrypts payload using `AES-256-GCM` with a 128-bit authentication tag, and writes the binary container (`PTBK` magic bytes, version `0x0001`, salt, IV, tag, ciphertext) via Android Storage Access Framework (SAF; `FR-9.3`, `AC-7`, `UJ-6`).

### Story 6.2: 4-Step Validation Pipeline, Pre-Restore Safety Snapshot & Rollback

As a user,
I want backup files thoroughly authenticated and validated before restoring, with automatic rollback if anything goes wrong,
So that my current active database is never corrupted or lost by a failed or tampered restore.

**Acceptance Criteria:**

**Given** a selected `.ptbackup` file and password input on the Import Backup screen
**When** `BackupManager` processes the import
**Then** it executes the 4-step validation pipeline: 1) Password verification, 2) Cryptographic integrity & GCM authentication tag verification, 3) Magic bytes and format check, 4) Schema compatibility check (`FR-9.4`, `ARCH-7`, `PRD RULE 6`)
**And** creates a temporary pre-restore SQLite safety snapshot prior to database alteration (`FR-9.5`)
**When** validation passes and schema deserializes successfully
**Then** database is restored atomically and the app restarts with state intact (`AC-7`)
**When** password is wrong, file is tampered, or schema fails
**Then** the pre-restore safety snapshot is automatically restored, leaving the previous database 100% intact, and displaying a clear, safe error message (`FR-9.6`, `PRD RULE 6`).

### Story 6.3: Permissions & Capability Diagnostic Screen

As a user,
I want a clear diagnostic screen explaining all optional and required system permissions with direct settings links,
So that I understand why capabilities are needed and can easily manage my permissions.

**Acceptance Criteria:**

**Given** the Permissions & Diagnostic screen in Onboarding and Settings (`UX-DR15`, `FR-10.1`, `NFR-3`)
**When** the user views the screen
**Then** status indicators display current status for AccessibilityService, Exact Alarms, Notification Channels, Usage Stats, and SpeechRecognizer language pack
**And** provides deep links to Android system settings with plain-language rationale copy
**And** when any permission is denied, the application degrades gracefully without blocking core functionality or showing modal error walls.

### Story 6.4: App Settings Management & Local Privacy Baseline

As a user,
I want a comprehensive Settings screen to manage DayType templates, notification intensity, and theme preferences under 100% offline privacy,
So that I can customize my application experience without my data ever leaving my device.

**Acceptance Criteria:**

**Given** the Settings tab (Bottom Navigation "Settings")
**When** the user navigates Settings
**Then** they can manage DayType template defaults, customize notification delivery channel behaviors, toggle Light/Dark/System themes, and execute backup actions (`UX-DR18`, `FR-9.2`)
**And** the entire application operates 100% offline with zero network dependencies for core features (`NFR-2`, `AC-8`)
**And** zero telemetry, analytics, or tracking SDKs exist in the application codebase (`NFR-4`, `[CONFIRMED]`).
