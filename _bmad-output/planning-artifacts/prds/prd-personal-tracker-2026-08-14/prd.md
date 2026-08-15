---
title: Personal-Tracker Product Requirements Document
status: final
created: 2026-08-14
updated: 2026-08-14
author: STIFLER
project: Personal-Tracker
---

# Personal-Tracker — Product Requirements Document (PRD)

> [!NOTE]
> **Document Purpose**: Product Requirements Document (PRD) defining functional requirements, user flows, system boundaries, security rules, failure recovery behaviors, and v1 acceptance criteria for the Personal-Tracker Android application.  
> **Source Traceability**: Derived strictly from approved [`brief.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/planning-artifacts/briefs/brief-personal-tracker-2026-08-14/brief.md), [`addendum.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/planning-artifacts/briefs/brief-personal-tracker-2026-08-14/addendum.md), and frozen [`research.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/planning-artifacts/research/market-personal-tracker-solution-patterns-2026-08-12/research.md) (Dimensions A–H).

---

## 1. Executive Overview & Target Environment

**Personal-Tracker** is an integrated personal mobile application for Android (with API 33–36 / Android 13–16 as a `[PROVISIONAL TECHNICAL TARGET]` subject to Architecture/technical validation) engineered to solve six core behavioral challenges: **procrastination, digital distraction, irregular daily routines, task initiation/execution friction, notification overload, and habit decay** `[EVIDENCE-BACKED: Dimension H]`.

The product operates as a **100% free, local-first, network-optional, single-user application** `[DECISION 9 & 10]`. It features an encrypted Room + SQLCipher database at rest `[DECISION 9]`, a state-aware focus hub `[DECISION 1]`, a first-class `DayType` schedule engine `[DECISION 3 & Dimension A]`, task-aware distraction interruption `[DECISION 4 & Dimension B]`, priority scarcity notifications `[DECISION 5 & Dimension C]`, transparent carry-forward rescheduling `[DECISION 6 & Dimension D]`, local voice capture with AI Action Boundaries `[DECISION 7 & Dimension E/G]`, and a non-punishing cosmetic companion `[DECISION 8 & Dimension F]`.

---

## 2. Target User Persona & Operational Scope

- **Target Persona**: Self-regulated learner, student, or individual seeking structured daily routines, focused study blocks, and distraction control without subscription fees, cloud tracking, or punitive streak resets.
- **Platform Scope**: Android Mobile Application (provisional target API 33–36 / Android 13–16 `[PROVISIONAL TECHNICAL TARGET]`).
- **Deployment Scope**: Local-first single-user personal application; no cloud sync backend, no user authentication servers, zero telemetry/ads `[DECISION 10]`.

---

## 3. User Journeys & End-to-End Workflows

### UJ-1: Daily Morning Kickoff & DayType Resolution
1. **Launch**: User opens Personal-Tracker on their phone.
2. **Auto-Resolution**: System evaluates the v1 resolution hierarchy (`Date-Specific User Override > Day-of-Week Default`) and auto-activates today's `DayType` template (e.g. `Weekday Routine`) with zero mandatory setup taps `[DECISION 3]` (future calendar exception support is classified as `[FUTURE / OPEN DECISION]`).
3. **Hero Banner**: The Hero Card displays a subtle morning banner (*"Active: Weekday Routine — Change to Heavy Study?"*). User can tap to swap templates for today only, or ignore it `[DECISION 3]`.
4. **Timeline**: Today's schedule timeline populates with routine steps, study blocks, and scheduled items.

### UJ-2: Low-Friction Voice/Text Capture & Confidence Routing
1. **Initiation**: User taps the microphone icon for local user-initiated voice capture or types into the quick-add bar on the Hero Card.
2. **Local STT & Parser**: If voice is used, audio is transcribed locally (SpeechRecognizer / ML Kit) and processed by the parser/local model. Audio file is immediately deleted `[DECISION 7 & 9]`.
3. **Confidence Routing & Fallback**:
   - **High Confidence**: Item is created instantly, showing a 5-second tap-to-revert toast pill `[DECISION 7]`.
   - **Medium Confidence**: Hero Card presents a compact confirmation card with 1-tap editable chips (`Title`, `Date`, `Time`, `Type`, `Urgency`). User taps *Confirm* to commit to the database `[PRD RULE 3]`.
   - **Low Confidence / Ambiguous Parsing**: When STT succeeds but parsing is low-confidence or ambiguous, the transcript and safely extracted fields are preserved in the *Unfiled Capture Inbox* for 1-tap categorization `[PRD RULE 3]`.
   - **STT Unavailable / Failure**: If on-device STT is unavailable or fails entirely, the system falls back directly to manual text entry `[PRD RULE 3]`.

### UJ-3: Focus Session Initiation, Distraction Intercept & Resumption
1. **Schedule Prompt**: As a scheduled study block arrives, the Hero Card displays a non-blocking prompt (*"Math Study starts now — Start Focus?"*) `[DECISION 4]`.
2. **Distraction Intercept**: If user opens a distracting app during the scheduled window without starting a session, the system displays **ONE** contextual JITAI prompt (*"Active Schedule: Math Study — Start Focus or Snooze"*). Never spams `[DECISION 4]`.
3. **Active Session**: User taps *Start Focus*. `AccessibilityService` overlay blocking activates.
4. **Interruption / Resumption**: If session is exited early or scheduled window expires with task `IN_PROGRESS`, system tags state as `INTERRUPTED/RESUMABLE` and surfaces a Resumption Card on Hero Card (*"Resume Math Study — 18m remaining"*) `[PRD RULE 1]`.

### UJ-4: Rescheduling & Carry Forward Review
1. **Review Trigger**: At evening review (or next morning launch), system identifies uncompleted flexible tasks.
2. **Review Card**: Hero Card displays Carry Forward Review card with actions: `[Do Tomorrow]`, `[Keep Unscheduled]`, `[Choose Another Day]`, `[Cancel]` `[DECISION 6]`.
3. **Capacity Check**: Selecting *Do Tomorrow* displays an advisory workload capacity indicator (*"Tomorrow has 6h 45m scheduled; moving these 3 tasks brings it to 8h 10m"*). User confirms or adjusts `[DECISION 6]`.

### UJ-5: Companion Progression & Shop Interaction
1. **Completion**: User completes a task, routine step, study session, or focus block.
2. **Idempotent Coins**: Application service validates completion and awards Coins once to local auditable ledger `[PRD RULE 5]`.
3. **Hero Companion**: Companion on Hero Card plays a supportive completion animation (zero HP loss or guilt mechanics) `[DECISION 8]`.
4. **Cosmetic Shop**: User spends earned Coins in local Shop on outfits, accessories, or room decor `[DECISION 8]`.

### UJ-6: Encrypted Manual Backup & Transactional Restore
1. **Export**: User taps *Export Backup* in Settings, enters a backup password, and saves an encrypted `.ptbackup` artifact `[DECISION 9]`.
2. **Import**: User taps *Import Backup*, selects `.ptbackup` file, and enters password `[PRD RULE 6]`.
3. **Validation & Restore**: System validates password, cryptographic integrity/authentication, format, and schema. It creates a temporary pre-restore safety snapshot, executes restore atomically, and recovers previous DB if restore fails `[PRD RULE 6]`.

---

## 4. Functional Requirements (Globally Numbered Stable IDs)

### FR-1: Home Surface & State-Aware Hero Card
- **FR-1.1**: The primary home surface MUST feature a dynamic Hero Card representing the user's current actionable state: *Active Focus/Task*, *Upcoming Scheduled Item*, *Interrupted/Resumable Task*, or *Intentional Idle State* `[DECISION 1]`.
- **FR-1.2**: Secondary views (*Today's Schedule* and *Action List*) MUST sit below the Hero Card `[DECISION 1]`.
- **FR-1.3**: When an item is `INTERRUPTED`, the Hero Card MUST surface a Resumption Card with actions `[Resume]`, `[Review/Adjust]`, and `[Mark Complete]` `[PRD RULE 1]`.
- **FR-1.4**: System MUST NEVER automatically mark an item complete merely because its scheduled window expires `[PRD RULE 1]`.
- **FR-1.5**: System MUST NEVER silently reschedule an interrupted item without user confirmation `[PRD RULE 1]`.

### FR-2: Domain Item Models & Specialized Execution
- **FR-2.1**: System MUST support unified capture entry while persisting distinct domain models for **Tasks**, **Routines**, **Habits**, and **Study Sessions** `[DECISION 2]`.
- **FR-2.2**: Tasks MUST support priority ranking and transparent "Do Tomorrow" carry forward rescheduling `[DECISION 2 & 6]`.
- **FR-2.3**: Routines MUST consist of sequential `RoutineStep` items linked to first-class `DayType` schedules `[DECISION 2 & 3]`.
- **FR-2.4**: Habits MUST support cadence tracking, earned streak slack, and miss-tolerant recovery `[DECISION 2 & 6]`.
- **FR-2.5**: Study Sessions MUST support subject tags, duration timers, and subject heatmap rollups `[DECISION 2]`.
- **FR-2.6**: Scheduling, notifications, focus sessions, Coin rewards, and history services MUST operate across all four domain models `[DECISION 2]`.

### FR-3: DayType Engine & Template Swapping
- **FR-3.1**: System MUST resolve today's active `DayType` in v1 using the hierarchy: 1) Date-Specific User Override > 2) Day-of-Week Default `[DECISION 3]`. Future calendar exception integration is classified as `[FUTURE / OPEN DECISION]`.
- **FR-3.2**: Today's active `DayType` MUST load automatically on app launch with zero mandatory setup taps `[DECISION 3]`.
- **FR-3.3**: The Hero Card MUST display a non-blocking morning banner showing the active template and offering a 1-tap swap option `[DECISION 3]`.
- **FR-3.4**: Template swaps applied via the Hero Card MUST apply strictly to the current date and MUST NOT mutate underlying base templates `[DECISION 3]`.
- **FR-3.5**: Mid-day template swaps MUST preserve all completed and in-progress items as immutable, recalculating only the remaining unstarted schedule for today `[PRD RULE 4]`.

### FR-4: Focus Sessions & Adaptive Distraction Interventions
- **FR-4.1**: Scheduled focus/study block start MUST present a non-blocking Hero Card prompt (`"Math Study starts now — Start Focus?"`) `[DECISION 4]`.
- **FR-4.2**: Launching a distracting app during a scheduled focus block without an active session MUST trigger at most **ONE** contextual JITAI prompt (`"Active Schedule: Math Study — Start Focus or Snooze"`) `[DECISION 4]`.
- **FR-4.3**: Explicitly starting a `FocusSession` MUST activate `AccessibilityService` overlay blocking and the Dimension B Adaptive Intervention Ladder `[DECISION 4]`.
- **FR-4.4**: Bypassing an active focus block MUST require deliberate override friction `[DECISION 4 & Dimension B]`. Exact duration, cooldown, confirmation behavior, and escalation parameters remain `[HYPOTHESIS] / [VALIDATION ITEMS]`.
- **FR-4.5**: Exiting a `FocusSession` early or letting a scheduled block expire while item is `IN_PROGRESS` MUST mark item state as `INTERRUPTED/RESUMABLE` `[PRD RULE 1]`.
- **FR-4.6**: Emergency calls, telecom dialers, and OS-critical interfaces MUST bypass distraction blocking completely `[PRD RULE 2]`.

### FR-5: Notification Urgency Classes & Scarcity Guidance
- **FR-5.1**: Notifications MUST be categorized into three delivery classes: `Routine` (quiet/in-app), `Important` (standard OS notification), and `Urgent` (`IMPORTANCE_HIGH` heads-up display + distinctive audio/haptics) `[DECISION 5]`.
- **FR-5.2**: Marking a 3rd active item as Urgent MUST trigger a soft guidance prompt (`"You already have 2 Urgent items today. Keep Urgent or Change to Important?"`) `[DECISION 5]`.
- **FR-5.3**: System MUST NEVER hard-block or silently downgrade Urgent items `[DECISION 5]`.
- **FR-5.4**: Urgent notifications MUST use acknowledgement-based escalation (bounded escalation); the word "nag" is prohibited `[DECISION 5]`.
- **FR-5.5**: Completing an Urgent item MUST release its quota slot for active Urgent items `[DECISION 5]`.

### FR-6: Rescheduling, Carry Forward & Capacity Guardrails
- **FR-6.1**: Uncompleted flexible Tasks MUST trigger a Carry Forward review card during evening review or next morning launch `[DECISION 6]`.
- **FR-6.2**: Review card MUST provide explicit actions: `[Do Tomorrow]`, `[Keep Unscheduled]`, `[Choose Another Day]`, and `[Cancel]` `[DECISION 6]`.
- **FR-6.3**: Fixed-Time Events MUST NOT silently roll over and MUST require explicit re-keying `[DECISION 6]`.
- **FR-6.4**: Routine Occurrences MUST expire for that day without stacking `[DECISION 6]`.
- **FR-6.5**: Moving flexible items to tomorrow MUST calculate and display an advisory workload capacity indicator (`"Tomorrow has 6h 45m scheduled; moving these 3 tasks brings it to 8h 10m"`). Indicator MUST be advisory only and NEVER hard-block `[DECISION 6]`.

### FR-7: Low-Friction Capture, Voice Pipeline & AI Action Boundary
- **FR-7.1**: System MUST support one-line natural-language text quick-add and local user-initiated voice capture `[DECISION 7]`.
- **FR-7.2**: Voice pipeline MUST process audio through on-device STT, delete raw audio immediately post-parsing, and run structured proposal validation before persistence `[DECISION 7 & 9]`.
- **FR-7.3**: High-confidence captures MUST commit immediately and display a temporary 5-second Undo toast pill `[DECISION 7]`.
- **FR-7.4**: Medium-confidence captures MUST display an inline Hero Card confirmation card with 1-tap editable chips (`Title`, `Date`, `Time`, `Type`, `Urgency`), requiring explicit user *Confirm* action before persistence `[PRD RULE 3]`.
- **FR-7.5**: If on-device STT succeeds but parsing is low-confidence or ambiguous, the resulting transcript MUST be saved to an *Unfiled Capture Inbox* for 1-tap categorization `[PRD RULE 3]`. If on-device STT is unavailable or fails, the system MUST fall back directly to manual text entry `[PRD RULE 3]`.
- **FR-7.6**: AI Action Boundary: Model outputs (STT, LLMs) MUST NEVER directly mutate persistent database records without schema/business rule validation `[DECISION 7 & Dimension G]`.

### FR-8: Non-Punishing Companion & Coins Economy
- **FR-8.1**: System MUST feature a supportive, non-punishing companion pet on the Hero Card (zero HP loss, zero character death, zero guilt mechanics) `[DECISION 8]`.
- **FR-8.2**: Genuine completions (Tasks, Routine Steps, Habits, Study Sessions, Focus Sessions) MUST award Coins to a local auditable ledger `[DECISION 8 & PRD RULE 5]`.
- **FR-8.3**: Reward-event idempotency MUST prevent duplicate Coin awards on repeated UI toggles or app restarts `[PRD RULE 5]`.
- **FR-8.4**: Coins in v1 MUST be spent EXCLUSIVELY in the local Shop on companion cosmetics, outfits, accessories, and background decor `[DECISION 8]`.
- **FR-8.5**: Streak system MUST operate architecturally independent of Coins; Coins MUST NOT purchase or control Streak Freezes in v1 `[DECISION 8]`.

### FR-9: Encrypted Local Storage & Transactional Restore
- **FR-9.1**: All application data MUST be stored in a local Room + SQLCipher database encrypted at rest with OS-level application sandbox protection `[DECISION 9]`.
- **FR-9.2**: System MUST operate 100% offline without default cloud synchronization or mandatory network access `[DECISION 9]`.
- **FR-9.3**: Manual backup export MUST produce a password-protected encrypted `.ptbackup` file on local storage `[DECISION 9]`.
- **FR-9.4**: Backup import MUST execute a 4-step validation pipeline (password verification, cryptographic integrity/authentication verification, format check, schema check) before modifying the active database `[PRD RULE 6]`.
- **FR-9.5**: Backup import MUST create a temporary pre-restore safety snapshot before replacing active DB files `[PRD RULE 6]`.
- **FR-9.6**: If import fails or validation errors occur, system MUST restore the pre-restore safety snapshot, substantially protecting the active database from failed or interrupted restores and leaving the previous database 100% intact `[PRD RULE 6]`.

### FR-10: System Immunity & OS Recovery Handlers
- **FR-10.1**: Emergency calls, phone dialer UIs, and OS critical interfaces MUST bypass distraction blocking completely `[PRD RULE 2]`.
- **FR-10.2**: Unexpected OS kills, low-memory terminations, battery exhaustion, or reboots MUST reconstruct session state into `INTERRUPTED/RESUMABLE` on next launch when persisted state supports that an active session was incomplete `[PRD RULE 2]`.
- **FR-10.3**: Selecting "Cancel Session" explicitly MUST set item state to `CANCELLED` and MUST NOT generate an automatic resumption prompt `[PRD RULE 2]`.

---

## 5. Non-Functional Requirements (NFRs)

- **NFR-1 (Local Latency)**: `[ENGINEERING TARGET / ARCHITECTURE VALIDATION]`: Cold app launch to interactive Hero Card state is targeted to execute in < 1.5 seconds on modern Android hardware. Quick-add NLP parsing is targeted to complete in < 300 ms.
- **NFR-2 (Offline Reliability)**: 100% of core task, routine, habit, focus, reminder, voice-fallback, and reward features MUST operate without network connectivity.
- **NFR-3 (Battery & Resource Efficiency)**: Background task execution MUST use `WorkManager`; exact alarms MUST be restricted to user-facing `setAlarmClock()` calls. `[ENGINEERING TARGET / ARCHITECTURE VALIDATION]`: `AccessibilityService` background CPU usage is targeted to remain < 2% during idle monitoring.
- **NFR-4 (Security & Privacy)**: Database MUST be encrypted at rest using the approved Room + SQLCipher approach `[DECISION 9]`. Specific cipher parameters, KDF configuration, key derivation, and Android Keystore integration are `[ARCHITECTURE PHASE]` decisions. Raw audio MUST be purged immediately post-STT. Zero telemetry SDKs.
- **NFR-5 (Data Portability)**: Encrypted `.ptbackup` artifacts MUST be restorable on replacement Android devices when valid backup credentials are provided.

---

## 6. Failure Modes & Recovery Matrix

| Failure Mode | Root Cause / Trigger | System Reaction & Recovery Protocol |
|---|---|---|
| **Incoming Emergency / Phone Call** | User receives call or interacts with telecom/dialer UI during active FocusSession | System telecom, emergency calls, dialers, and OS-critical interfaces bypass distraction blocking completely `[PRD RULE 2]`. Active session state is preserved where technically possible; exact call pause/resume/interruption behavior is `[OPEN DECISION / TECHNICAL VALIDATION]`. |
| **OS Process Termination / Reboot** | Low-memory kill or device reboot during focus block | `BOOT_COMPLETED` receiver restores DB state; next app launch reconstructs incomplete session as `INTERRUPTED/RESUMABLE` `[PRD RULE 2]`. |
| **Corrupted Backup File Import** | User attempts to import invalid/tampered `.ptbackup` | 4-step validation fails (cryptographic integrity / format / schema error); system displays error banner; pre-restore safety snapshot restores active database untouched `[PRD RULE 6]`. |
| **Incorrect Backup Password** | Password mismatch during import | Password check fails before DB write; active database remains untouched `[PRD RULE 6]`. |
| **STT Engine Unavailable / Parsing Failure** | Offline STT unavailable OR speech parsing is low-confidence | If STT is unavailable, system falls back to manual text entry. If STT succeeds but parsing is low-confidence, transcript routes to *Unfiled Capture Inbox* for 1-tap manual sorting `[PRD RULE 3]`. Raw audio is immediately deleted `[DECISION 9]`. |
| **Repeated UI Toggle Farming** | User toggles task `COMPLETED` ↔ `INCOMPLETE` rapidly | Reward idempotency ledger detects duplicate event and awards Coins ONCE `[PRD RULE 5]`. |

---

## 7. Preliminary Hypotheses, Open Decisions & Technical Allocations Registry

The following items are explicitly categorized to prevent unvalidated parameters from being treated as confirmed requirements:

### A. Preliminary Product Hypotheses `[HYPOTHESIS]` (Validation Required during Personal Use)
1. **State-Aware Hero Card Layout & Transitions**: Exact card transition physics, animations, and layout styling.
2. **STT Confidence Floor Numbers**: Exact numeric thresholds for high/medium/low confidence STT routing.
3. **Voice Undo Toast Duration**: 5-second default display duration for tap-to-undo toast.
4. **Urgent Item Quota Ceiling & Accounting**: 2 active Urgent items ceiling and active vs completed accounting rules.
5. **Acknowledgement Escalation Cadence**: 5-minute retry interval and max 3 re-fires.
6. **Workload Capacity Formula & Copy**: Exact algorithm for calculating planned daily hours and advisory prompt copy.
7. **Focus Session Minimum Duration for Coins**: 10-minute session validity threshold for Coin eligibility.
8. **Focus Override Friction Parameters**: Exact duration, cooldown period, confirmation modal behavior, and escalation timing for deliberate focus override friction.

### B. Open Decisions `[OPEN DECISION]` (Product / Behavioral Validation)
9. **Routine Reward Granularity**: Per-step Coin allocation vs full-sequence completion bonus.
10. **Phone-Call Interruption Mechanics**: Exact pause/resume/interruption behavior and timer buffering when incoming calls occur during focus blocks `[OPEN DECISION / TECHNICAL VALIDATION]`.
11. **DayType Calendar Exceptions**: Future optional calendar exception detection and auto-assignment rules `[FUTURE / OPEN DECISION]`.

### C. Engineering Performance Targets `[ENGINEERING TARGET]`
12. **Application Performance Targets**: Cold app launch < 1.5s, quick-add NLP parsing < 300ms, and idle `AccessibilityService` background CPU < 2%.
13. **Provisional Android Version Scope**: Target range API 33–36 / Android 13–16 `[PROVISIONAL TECHNICAL TARGET]`.

### D. Architecture / Security Phase Decisions `[ARCHITECTURE PHASE]`
14. **Cryptographic Implementation & Key Derivation**: Room + SQLCipher cipher configuration, KDF algorithms/parameters, Android Keystore integration, backup integrity/authentication algorithm, secure deletion protocols, and schema versioning.
15. **OS Telecom Package Detection & Reliability**: Specific Android package detection rules for emergency/dialer bypass and OEM-specific background service execution rules.

---

## 8. Explicit V1 Non-Goals

1. **No Cloud Sync Backend or Web App**: No multi-device sync servers, cloud DBs, or web interfaces.
2. **No Monetization or Paid Features**: 100% free app; no subscriptions, paywalls, or paid streak repairs.
3. **No Punitive Gamification**: No HP loss, character death, or guilt notifications.
4. **No Real-Life Reward Exchanging**: No trading Coins for gaming time or real-world perks in v1.
5. **No System Calendar Synchronization**: No reading or writing Android system calendars in v1; no calendar exception dependencies in v1 DayType resolution.
6. **No Direct AI Database Mutation**: No model outputs directly mutating persistent database tables.
7. **No Wake-Word / Always-Listening Voice**: No background audio recording or wake-phrase detection (`Hey Tracker`).

---

## 9. V1 Acceptance Criteria

- [ ] **AC-1**: App launches to active `DayType` schedule using `Date-Specific User Override > Day-of-Week Default` with zero setup taps; Hero Card banner offers 1-tap single-date swap without mutating base templates.
- [ ] **AC-2**: High-confidence text/voice entries commit instantly with a 5s Undo toast; medium-confidence entries present Hero Card 1-tap editable chips requiring explicit Confirm; low-confidence entries route to Unfiled Capture Inbox; STT unavailability falls back directly to manual text entry.
- [ ] **AC-3**: Exiting an active FocusSession early or letting a scheduled block expire with task `IN_PROGRESS` transforms the Hero Card into a Resumption Card with `Resume`, `Review/Adjust`, and `Mark Complete` actions.
- [ ] **AC-4**: Marking a 3rd active item as Urgent displays soft scarcity guidance without hard-blocking.
- [ ] **AC-5**: Carry-forward review card presents uncompleted flexible tasks with `Do Tomorrow`, `Keep Unscheduled`, `Choose Another Day`, and `Cancel` actions alongside an advisory workload capacity indicator.
- [ ] **AC-6**: Genuine completions award Coins once to an idempotent ledger; Coins spend exclusively on companion cosmetics/decor; Streaks operate independently.
- [ ] **AC-7**: Local database is encrypted at rest; manual backup produces a password-protected `.ptbackup` file; import executes 4-step validation (password, cryptographic integrity/authentication, format, schema) and transactional safety snapshot rollback if restore fails.
- [ ] **AC-8**: Core app operates 100% offline without network connectivity, cloud sync servers, monetization, punitive mechanics, wake-word audio, or direct AI database mutation.
