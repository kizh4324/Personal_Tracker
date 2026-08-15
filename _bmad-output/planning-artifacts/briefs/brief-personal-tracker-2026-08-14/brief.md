---
title: Personal-Tracker Product Brief
status: complete
created: 2026-08-14
updated: 2026-08-14
author: STIFLER
project: Personal-Tracker
---

# Personal-Tracker — Product Brief

> [!NOTE]
> **Product Brief Classification**: Personal-Use Mobile Android Application  
> **Target Environment**: Local-First, 100% Free, Network-Optional Android Mobile Application (API 33–36)  
> **Source Traceability**: Built from [`PROBLEM_AND_SOLUTION_BRIEF.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/PROBLEM_AND_SOLUTION_BRIEF.md) and the frozen 8-dimension [`research.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/planning-artifacts/research/market-personal-tracker-solution-patterns-2026-08-12/research.md).

---

## 1. Executive Summary & Core Purpose

**Personal-Tracker** is an integrated personal mobile application for Android designed to solve six core behavioral and operational challenges faced by self-regulated learners and individuals: **procrastination, digital distraction, irregular daily routines, task initiation/execution friction, notification overload, and habit maintenance decay** `[EVIDENCE-BACKED: Dimension H]`.

Rather than relying on fragmented single-purpose tools (a separate habit tracker, pomodoro timer, app blocker, task list, and study logger), Personal-Tracker unifies these capabilities into a single **State-Aware Focus & Resumption Hub** `[DECISION 1]`. The system is built around a first-class `DayType` schedule engine `[EVIDENCE-BACKED: Dimension A]`, task-aware distraction interruption `[EVIDENCE-BACKED: Dimension B]`, priority scarcity notification delivery `[EVIDENCE-BACKED: Dimension C]`, transparent item-type-aware rescheduling `[EVIDENCE-BACKED: Dimension D]`, low-friction text/voice capture `[EVIDENCE-BACKED: Dimension E]`, non-punishing companion gamification `[EVIDENCE-BACKED: Dimension F]`, and encrypted local-first security `[EVIDENCE-BACKED: Dimension G]`.

The product is explicitly designed for personal use, 100% free, zero-monetization, and fully functional offline without mandatory cloud dependencies or third-party servers `[DECISION 9 & 10]`.

---

## 2. Target User & Use-Case Scope

- **Primary Persona**: Self-regulated learner, student, or individual seeking structured daily routines, focused study sessions, and distraction control without invasive management software or punitive streak resetting.
- **Form Factor**: Android Mobile Application targeting modern Android releases (Android 13–16 / API 33–36) `[EVIDENCE-BACKED: Dimension G]`.
- **Distribution Scope**: Personal-use application; non-monetized, single-user deployment `[DECISION 10]`.

---

## 3. Confirmed V1 Operating Model & Product Decisions

### Decision 1: Primary Home Surface — State-Aware Focus & Resumption Hub
- **Hero Card**: The primary home surface centers on a dynamic **Hero Card** representing the user's current actionable state: *Active Focus/Task Session*, *Upcoming Scheduled Item*, *Interrupted/Resumable Task* (Ovsiankina prompt), or *Intentional Idle State* `[DECISION 1]`.
- **Subordinate Views**: "Today's Schedule" and "Action List" sit as secondary views below the Hero Card, prioritizing immediate action over high information density `[DECISION 1]`.
- **Status**: `[DECISION]` / `[HYPOTHESIS: v1 personal validation required]`.

### Decision 2: Item Architecture — Unified Capture with Specialized Domain Execution
- **Unified Capture Layer**: Single text/voice capture bar infers item type when confidence is high; prompts for 1-tap confirmation when ambiguous `[DECISION 2]`.
- **Specialized Domain Models**:
  - **Tasks**: Scarcity-ranked execution, "Do Tomorrow" user-confirmed rescheduling.
  - **Routines**: Sequential `RoutineStep` items bound to `DayType` schedule engine.
  - **Habits**: Cadence tracking, streak/slack buffer, miss-tolerant recovery.
  - **Study Sessions**: Subject tags, duration timer, subject heatmap rollup.
- **Shared Services**: Scheduling, completion events, notifications, focus sessions, Coin rewards, and history logging operate across all domain types `[DECISION 2]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions A, D, E, F]`.

### Decision 3: DayType Assignment & Resolution Hierarchy
- **Resolution Order**:
  1. Date-specific user override for today.
  2. Explicitly configured calendar/date exception rule (if enabled).
  3. Default saved day-of-week mapping (`Mon–Fri = Weekday`, `Sat–Sun = Weekend`).
- **Zero Mandatory Friction**: Today's `DayType` auto-activates on launch. Hero Card banner displays active template (`Active: Weekday Routine`) with a 1-tap swap option (`Change to Heavy Study?`). Swapping applies to today only and does NOT mutate base templates `[DECISION 3]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions A & H]`.

### Decision 4: Focus Sessions & Distraction Interventions
- **Schedule Prompting**: Scheduled focus block start presents a non-blocking Hero Card prompt (*"Math Study starts now — Start Focus?"*).
- **Distraction Intercept (JITAI Restraint)**: Opening a distracting app during a scheduled focus block without an active session triggers **ONE** contextual JITAI prompt (*"Active Schedule: Math Study — Start Focus or Snooze"*). Never spams repeatedly `[DECISION 4]`.
- **Authoritative Focus State**: Once explicitly launched, `FocusSession` becomes the authoritative state for `AccessibilityService` overlay blocking and the Dimension B Adaptive Intervention Ladder `[DECISION 4]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions B, G, H]`.

### Decision 5: Notification Urgency & Scarcity Guidance
- **Delivery Classes**:
  - `Routine`: Quiet / in-app delivery.
  - `Important`: Standard OS notification.
  - `Urgent`: Strongest available notification channel (`IMPORTANCE_HIGH`, heads-up display, distinctive audio/haptics).
- **Soft-Cap Scarcity Guidance**: Attempting to mark a 3rd active item as Urgent displays soft guidance (*"You already have 2 Urgent items today. Keep Urgent or Change to Important?"*). Never hard-blocks `[DECISION 5]`.
- **Escalation**: Uses **acknowledgement-based escalation** (bounded escalation); word "nag" is prohibited `[DECISION 5]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions C & G]`.

### Decision 6: Rescheduling & Item-Type Aware Carry Forward
- **Carry Forward Review**: Uncompleted flexible Tasks trigger an evening/morning review card with explicit options: `[Do Tomorrow]`, `[Keep Unscheduled]`, `[Choose Another Day]`, `[Cancel]` `[DECISION 6]`.
- **Item-Type Safeguards**: Fixed-Time Events require re-keying; Routine Occurrences expire without stacking; Habits use streak slack; Study Sessions follow configured scheduling `[DECISION 6]`.
- **Advisory Workload Indicator**: Displays calculated planned workload (*"Tomorrow currently has 6h 45m scheduled; moving these 3 tasks brings it to 8h 10m"*). Advisory only; never hard-blocks `[DECISION 6]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimension D]`.

### Decision 7: Voice Capture & AI Action Boundary
- **Voice Pipeline**: `Audio → STT → raw text → parser/local AI → proposal → schema/business validation → confidence routing → Task Service → DB` `[DECISION 7]`.
- **Routing Tiers**: High Confidence (immediate + 5s Undo toast); Medium Confidence (compact confirmation card); Low/Failed Confidence (Unfiled Capture Inbox) `[DECISION 7]`.
- **AI Action Boundary**: Speech models, STT engines, or local LLMs **NEVER** directly mutate persistent database records without schema/business rule validation `[EVIDENCE-BACKED: Dimension G]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions E, G, H]`.

### Decision 8: Companion Persona & Coins Economy Scope
- **v1 Companion Scope**: Non-punishing visual buddy on Hero Card. Coins earned from genuine completions and spent **ONLY** on companion cosmetics, outfits, accessories, and environment decor `[DECISION 8]`.
- **Companion Non-Interference**: Never a second task manager, notification engine, or maintenance chore `[DECISION 8]`.
- **Architectural Separation**: Streak system operates independently of Coins. Coins cannot purchase or modify Streak Freezes in v1 `[DECISION 8]`.
- **Anti-Farming**: Coins awarded strictly for genuine completions; never for app opens or taps `[DECISION 8]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimension F]`.

### Decision 9: Data Storage, Local Security & Backup Model
- **Encrypted Storage**: Core data encrypted at rest (Room + SQLCipher) with OS-level Android sandbox protection `[DECISION 9]`.
- **Local-First & Offline**: 100% local-first; zero default cloud synchronization, zero network dependencies `[DECISION 9]`.
- **Encrypted Manual Backup**: User-initiated export/import via *Settings → Data & Backup*. Produces password-protected encrypted backup file `[DECISION 9]`.
- **Voice Privacy**: Raw audio deleted immediately post-parsing `[DECISION 9]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimension G]`.

### Decision 10: Explicit V1 Scope Boundaries & Non-Goals
- **Excluded from V1**: Multi-user accounts, social sharing, leaderboards, cloud-sync servers, monetization/subscriptions, paid streak repairs, punitive gamification (HP loss/character death), real-life reward sinks (Coins for gaming), direct AI DB mutation, hard break locks, mandatory cloud AI dependencies, and system calendar import `[DECISION 10]`.
- **Included in V1**: Hands-free Voice Capture, Encrypted Local Storage, Encrypted Manual Backup, and Cosmetic Companion Progression `[DECISION 10]`.
- **Status**: `[DECISION]` / `[EVIDENCE-BACKED: Dimensions A–H]`.

---

## 4. Key Evidence vs. Hypothesis Matrix

| Domain Layer | Evidence-Backed Foundation | Preliminary Product Hypothesis (Validation Required) |
|---|---|---|
| **DayType Schedule** | First-class `DayType` schedule engine `[A]` | Exact DayType template count & morning greeting copy |
| **Interruption** | `AccessibilityService` overlay + B intervention ladder `[B, G]` | Exact bypass friction timing & intervention prompt limits |
| **Notifications** | Routine/Important/Urgent delivery classes & scarcity `[C]` | Exact 1–2 Urgent quota ceiling & retry nag intervals |
| **Rescheduling** | Item-type aware review & "Do Tomorrow" rescheduling `[D]` | Exact workload capacity formulas & button copy |
| **Capture** | One-line NLP quick-add & voice confidence floor `[E]` | Exact confidence thresholds & 5s Undo duration |
| **Gamification** | Non-punishing companion & Coins cosmetic shop `[F]` | Exact Coin award values, item prices, & shop categories |
| **Security** | Encrypted local DB & zero telemetry/ads `[G]` | Cryptographic algorithm selection & key derivation (Architecture) |
| **Behavioral** | Ovsiankina resumption & If-Then rules `[H]` | Exact miss-recovery thresholds & habit automaticity curves |

---

## 5. Non-Functional & Security Constraints

1. **Local-First Security**: 100% offline-functional; encrypted at rest via Room + SQLCipher; zero third-party analytics/ads; zero network transmission of personal data `[EVIDENCE-BACKED: Dimension G]`.
2. **Privacy Boundary**: `AccessibilityService` collects ONLY foreground package name transitions for blocking; NEVER records, logs, or transmits screen text, passwords, OTPs, or private UI text `[EVIDENCE-BACKED: Dimension G]`.
3. **AI Safety Boundary**: Model outputs (STT, LLMs) are strictly untrusted proposals that MUST pass schema validation and Task Service business rules before persistence `[EVIDENCE-BACKED: Dimension G]`.
4. **Android Performance**: Main thread remains non-blocking; background tasks use `WorkManager`; alarms use `setAlarmClock()` for time-critical items `[EVIDENCE-BACKED: Dimension G]`.

---

## 6. Explicit V1 Non-Goals

1. **No Cloud Backend or User Accounts**: No multi-device cloud sync, authentication servers, or web apps.
2. **No Monetization or Paid Mechanics**: No subscriptions, paywalls, or paid streak repairs.
3. **No Punitive Gamification**: No HP loss, character death, or guilt-inducing notifications.
4. **No Real-Life Reward Exchanges**: No trading Coins for gaming time or real-world perks in v1.
5. **No System Calendar Sync**: No reading or writing Android system calendars in v1.
