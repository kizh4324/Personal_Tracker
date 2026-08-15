---
name: Personal-Tracker
status: draft
sources:
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/prd.md
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/addendum.md
  - docs/specifications/architecture.md
  - docs/specifications/TECHSTACK.md
  - docs/design/design-system.md
updated: 2026-08-15
---

# Personal-Tracker — Experience Spine

> Authoritative UX interaction specification for Personal-Tracker v1. Single-surface Android phone. Local-first, online/offline, network-optional. Paired with `DESIGN.md` (visual identity). This spine owns *how it works*; DESIGN.md owns *how it looks*. Spines win on conflict with any mock, wireframe, or import.

## 1. Foundation

- **Form factor**: Single-surface Android phone (API 33–36 provisional target). No tablet, fold, or wearable surfaces in v1.
- **UI system**: Jetpack Compose + Material 3 with custom design tokens from `design-system.md`. `DESIGN.md` is the visual identity reference.
- **Connectivity posture**: Local-first, online/offline, network-optional. Every core feature operates without network. Gemini Live API (online voice) is additive; offline SpeechRecognizer + manual text entry are the baseline.
- **Theme**: Dark mode + light mode with system-following. Both modes must receive equal design attention and testing.
- **Architecture boundary**: AI models (Gemini Live, SpeechRecognizer) NEVER directly mutate the database. All proposals pass through validation before persistence. [FR-7.6, §5.2 Architecture]

## 2. Information Architecture

| Surface | Reached From | Purpose | PRD Trace |
|---|---|---|---|
| **Home** | App launch / Bottom Nav "Home" | Hero Card + Today's Schedule list + Action List. Primary interaction surface. | FR-1.1, FR-1.2 |
| **DayType Timeline** | Home → persistent bottom sheet swipe-up | Day's schedule rendered as a vertical timeline with DayType color coding. | FR-3.1–3.5 |
| **Task Detail / Edit** | Home → tap task card | View/edit a single task: title, description, date, time, duration, delivery intensity, state. | FR-2.1, FR-2.2 |
| **Routine Execution** | Home → tap routine card | Step-by-step sequential execution of a routine's ordered steps. | FR-2.3 |
| **Habits** | Bottom Nav "Habits" | Habit list with streak visualization, completion toggles, cadence info, freeze status. | FR-2.4 |
| **Habit Detail** | Habits → tap habit card | Individual habit history, streak analytics, cadence settings, grace/freeze management. | FR-2.4, FR-8.5 |
| **Study** | Bottom Nav "Study" | Study session list, subject heatmap rollup, session history. | FR-2.5 |
| **Study Session (Active)** | Study → start session / Home → start focus | Active timer with subject tag, duration tracking, and optional focus mode. | FR-2.5, FR-4.1 |
| **Focus Session (Active)** | Any domain item → "Start Focus" | Active countdown timer. Triggers AccessibilityService overlay blocking. App-wide state. | FR-4.1–4.6 |
| **Companion & Shop** | Bottom Nav "Companion" | Companion pet display (Rive animation), Coin balance, Shop browse/purchase. | FR-8.1–8.5 |
| **Shop** | Companion → "Shop" button | Browse/purchase cosmetics: outfits, accessories, background decor. | FR-8.4 |
| **Unfiled Capture Inbox** | Home → inbox indicator / notification | Low-confidence voice captures awaiting manual 1-tap categorization. | FR-7.5 |
| **Settings** | Bottom Nav "Settings" | App configuration, DayType template management, notification preferences, backup, permissions. | — |
| **DayType Management** | Settings → "Day Types" | Create, edit, and manage DayType templates and day-of-week defaults. | FR-3.1 |
| **Backup & Restore** | Settings → "Data & Backup" | Export `.ptbackup` / Import `.ptbackup` with password and validation UI. | FR-9.3–9.6 |
| **Notification Management** | Settings → "Notifications" | Configure delivery intensity preferences per channel. | FR-5.1 |
| **Permissions & Diagnostic** | Settings → "Permissions" / First launch | Capability diagnostic: AccessibilityService, Exact Alarms, Notifications, Usage Stats, STT. | FR-10.1 |

**Navigation model**: Bottom navigation bar with 5 destinations: **Home** · **Habits** · **Study** · **Companion** · **Settings**. No drawer. Modal stacks one level deep, never two. The persistent bottom sheet (DayType timeline) sits above the bottom nav on the Home surface only.

**Voice FAB**: Floating above bottom nav on Home, Habits, and Study surfaces. Available app-wide for quick capture. Repositions above keyboard when active.

## 3. Voice and Tone (Microcopy)

Microcopy. Brand voice and aesthetic posture live in `DESIGN.md`.

| Context | Do | Don't | PRD Constraint |
|---|---|---|---|
| **DayType morning banner** | "Good morning! Active: Weekday Routine" / "Looks like a Heavy Study day." | "You have 12 tasks. Time to grind!" | FR-3.3: non-blocking banner |
| **DayType swap** | "Switched to Weekend for today only." | "Template permanently changed." | FR-3.4: single-date only |
| **Focus session prompt** | "Math Study starts now — Start Focus?" | "Don't you dare procrastinate!" | FR-4.1: non-blocking prompt |
| **JITAI distraction intercept** | "Active Schedule: Math Study — Start Focus or Snooze" | "STOP! You're wasting time!" | FR-4.2: ONE contextual prompt |
| **Focus override friction** | "Hold to exit focus" | "Are you sure? You'll lose progress!" | FR-4.4: deliberate friction |
| **Carry-forward review** | "3 items still open. What would you like to do?" | "You failed to complete 3 tasks." | FR-6.1: neutral framing |
| **Workload advisory** | "Tomorrow has 6h 45m scheduled; adding these brings it to 8h 10m" | "WARNING: Tomorrow is overloaded!" | FR-6.5: advisory only |
| **Urgency scarcity** | "You already have 2 Urgent items today. Keep Urgent or change to Important?" | "LIMIT REACHED. Downgrading." | FR-5.2: soft guidance, FR-5.3: never hard-block |
| **High-confidence commit** | "Added: Read Chapter 4 tonight" (with Undo) | "Task auto-saved! ✓ Success!" | FR-7.3: 5s undo toast |
| **Medium-confidence review** | "Does this look right?" (with editable chips) | "AI created your task. Verify immediately." | FR-7.4: 1-tap editable |
| **Low-confidence inbox** | "Couldn't quite catch that — saved for review" | "ERROR: Parse failed." | FR-7.5: inbox routing |
| **STT unavailable** | "Voice not available — type instead" | "CRITICAL: Speech engine missing!" | FR-7.5: manual fallback |
| **Companion celebration** | (Animation only — no text needed) | "Great job! You're amazing! 🎉🎉🎉" | FR-8.1: non-punishing |
| **Companion resting** | (Sleeping animation — no text needed) | "Your companion is lonely." | FR-8.1: zero guilt |
| **Streak at risk** | "Streak at 14 days — you've got this" | "You're about to LOSE your streak!" | FR-8.5: non-punitive |
| **Streak freeze used** | "Freeze day applied. Streak safe." | "Lucky you didn't lose it." | FR-8.5: graceful recovery |
| **Coin earned** | (Animation + brief "+5" overlay) | "COINS EARNED: 5 COINS ADDED TO BALANCE!" | FR-8.2: subtle reward |
| **Empty schedule** | "All caught up for today. Rest well." | "Nothing planned. Add something!" | — |
| **Empty habits** | "Ready to build a new habit?" | "No habits tracked! Get started!" | — |
| **Backup export** | "Backup saved to [location]" | "Export completed successfully! ✅" | FR-9.3 |
| **Backup wrong password** | "Incorrect password. Database unchanged." | "ACCESS DENIED." | FR-9.4 |
| **Backup corrupted** | "This backup file appears damaged. Your current data is safe." | "CORRUPT FILE DETECTED!" | FR-9.6 |
| **Permission needed** | "Personal-Tracker needs Accessibility access to block distracting apps during focus sessions." | "Grant permission to continue." | — |
| **Notification escalation** | "Reminder: Math Study" (bounded re-fire) | "NAG: You haven't responded!" | FR-5.4: "nag" prohibited |
| **Offline indicator** | Subtle icon — no banner | "YOU ARE OFFLINE!" | NFR-2 |

## 4. Component Patterns (Behavioral)

Behavioral rules. Visual specs live in `DESIGN.md.Components`.

### 4.1 Hero Card (State Machine)

The Hero Card is a **state-driven container** at the top of the Home surface. Its content changes based on the user's current actionable state. Priority order (highest wins):

| Priority | State | Content | Actions | PRD Trace |
|---|---|---|---|---|
| 1 | **Active Focus/Task** | Currently executing item title + countdown timer | Pause, End Session | FR-1.1 |
| 2 | **Interrupted/Resumable** | Last interrupted item + elapsed-since timestamp | Resume, Review/Adjust, Mark Complete | FR-1.1, FR-1.3, PRD Rule 1 |
| 3 | **Morning DayType Banner** | Active DayType name + 1-tap swap | Swap DayType, Dismiss | FR-3.3, AC-1 |
| 4 | **Upcoming Scheduled Item** | Next scheduled item title + "starts in X min" | Start Now, Snooze | FR-1.1 |
| 5 | **Carry-Forward Review** | Count of uncompleted items from yesterday | Review Items | FR-6.1, AC-5 |
| 6 | **Intentional Idle** | Companion resting + "All caught up" | — | FR-1.1 |

**Transition rules**:
- State changes animate with 300ms `FastOutSlowIn` cross-fade. Content inside the card replaces; the card container itself remains stable. [design-system.md: State transitions 300–400ms]
- NEVER auto-dismiss the Interrupted/Resumable state. It persists until explicit user action. [FR-1.4, FR-1.5]
- Morning banner auto-dismisses after first interaction with any item (not time-based).
- Only ONE Hero Card state visible at a time — priority order resolves conflicts.

### 4.2 Task Card

| Attribute | Behavior |
|---|---|
| **States** | `PENDING` → `IN_PROGRESS` → `COMPLETED` / `INTERRUPTED` / `CANCELLED` |
| **Delivery intensity** | Left accent stripe color: Routine (`{colors.info}`), Important (`{colors.warning}`), Urgent (`{colors.danger}` + 6dp stripe + lightning icon) |
| **Tap** | Opens Task Detail / Edit |
| **Completion toggle** | Checkbox tap → 300ms transition to subtle background + strikethrough + haptic. Awards Coins (1× idempotent). [FR-8.2, FR-8.3] |
| **Long-press** | Context menu: Edit, Reschedule, Change Intensity, Cancel |
| **Swipe right** | Quick complete (same as checkbox) |
| **Urgent variant** | 6dp accent stripe + lightning icon top-right. Must remain visually distinct. [FR-5.1] |
| **Carry-forward badge** | If `isCarryForward`: small "↻" indicator + carry count. [FR-2.2] |
| **Scheduling** | Shows `scheduledDate` + `scheduledTime` if set; omits if flexible. |
| **INTERRUPTED state** | Yellow-tinted accent stripe. Resumption prompt in Hero Card. [PRD Rule 1] |

### 4.3 Habit Card

| Attribute | Behavior |
|---|---|
| **Tap** | Opens Habit Detail |
| **Completion toggle** | 1-tap marks today as COMPLETED. Triggers coin animation. Idempotent. [FR-8.3] |
| **Streak display** | Flame visualization: `{colors.streak-flame-core}` / `{colors.streak-flame-tip}`. Days count in `{typography.data}`. |
| **Streak at risk** | Companion enters "concerned" state (not "sad"). Banner: "Streak at X days — you've got this" [FR-8.1: non-punishing] |
| **Freeze indicator** | `{colors.info}` snowflake icon + "Freeze active today". Streak preserved. [FR-2.4] |
| **Freeze used** | Info banner: "Freeze day applied. Streak safe." No negative visual. |
| **Miss (no freeze)** | Streak resets silently. Companion enters "encouraging" state (wave gesture). No guilt mechanics. |
| **Cadence indicator** | Label showing DAILY / WEEKDAYS / CUSTOM with applicable days |

### 4.4 Routine Card & Execution

| Attribute | Behavior |
|---|---|
| **Routine card (Home)** | Shows title + step progress (e.g., "2 of 5 steps") + `{colors.companion}` accent |
| **Tap** | Opens Routine Execution screen |
| **Execution model** | Sequential: current step highlighted, previous steps shown completed, future steps dimmed. One active step at a time. [FR-2.3] |
| **Step completion** | Tap to mark step done → auto-advances to next step. Awards Coins per step or on completion. **[OPEN DECISION: Routine reward granularity — per-step vs completion-only]** [FR-8.2] |
| **Full completion** | All steps done → Companion "celebrating" animation. Return to Home. |
| **Partial exit** | Progress preserved. Card on Home shows current step. Resumable anytime. |

### 4.5 Study Session Card & Execution

| Attribute | Behavior |
|---|---|
| **Card (Study surface)** | Subject tag chip + last session date + total hours in `{typography.data}` |
| **Start session** | Opens active timer in Study Session surface. Subject pre-selected. |
| **Active timer** | Countdown or count-up in `{typography.data-large}`. Pause/Resume/End controls. |
| **Focus integration** | Optional "Start Focus" button activates AccessibilityService blocking for this session. [FR-4.1] |
| **Completion** | Logs `StudySessionEntity`. Duration tracked. Awards Coins. Heatmap updates. [FR-2.5] |
| **Minimum duration for coins** | **[HYPOTHESIS: 10-minute minimum]** Sessions shorter than threshold log but don't award Coins. [PRD Addendum §A.7] |

### 4.6 Voice Capture FAB

State machine for the floating action button:

| State | Visual | Behavior | Exit |
|---|---|---|---|
| **Idle** | 56dp circle, `{colors.primary}`, waveform icon | Tap to begin capture | → Listening |
| **Listening** | Morphs to stadium pill (300ms). Pulsing waveform + live preview chips | Audio → STT engine (Gemini Live if online, SpeechRecognizer if offline) | → Parsing (on speech end) |
| **Parsing** | Brief spinner in pill | Deterministic parser extracts attributes. Confidence scored. | → Review / Committed / Inbox |
| **Committed** | Pill collapses. Undo toast appears (5s). | High-confidence auto-commit. Audio deleted immediately. [FR-7.2, FR-7.3] | → Idle (after toast) |
| **Review** | Confirmation chips appear in Hero Card area | Medium-confidence. 1-tap editable chips (Title, Date, Time, Type, Subject, Duration, Urgency). Confirm or Edit buttons. [FR-7.4, PRD Rule 3] | → Committed (on Confirm) / Task Detail (on Edit) |
| **Inbox** | Pill collapses. Brief "Saved for review" toast. | Low-confidence / ambiguous. Transcript + draft → `unfiled_capture_inbox`. [FR-7.5] | → Idle |
| **Fallback** | Pill collapses. Text input field appears. | STT unavailable or failed. Keyboard opens for manual text quick-add. [FR-7.5] | → Parsing (on submit) |

**Network failover**: If Gemini Live WebSocket disconnects mid-capture, router falls back to on-device SpeechRecognizer seamlessly. If on-device STT is also unavailable, immediately transition to Fallback (text entry). [Architecture §3.2, §7]

### 4.7 Confirmation Chips (Medium-Confidence Capture)

| Attribute | Behavior |
|---|---|
| **Display** | Horizontal scrollable row of editable chips in Hero Card area |
| **Fields** | Title, Date, Time, Type, Subject, Duration, Urgency. Unset fields show as "+" add chip. |
| **Edit** | Tap chip → inline edit (text field for Title, date picker for Date, etc.) |
| **Confirm** | "Confirm" button → commits to database. Audio already deleted. [PRD Rule 3] |
| **Cancel** | "Discard" → removes proposal. No trace persisted. |
| **Timeout** | No auto-timeout. Chips persist until explicit user action. |

### 4.8 Undo Toast (High-Confidence Auto-Commit)

| Attribute | Behavior |
|---|---|
| **Display** | Bottom toast pill: "Added: [item title]" + "Undo" tap target |
| **Duration** | 5 seconds. **[HYPOTHESIS: 5s default duration]** [PRD Addendum §A.3] |
| **Tap Undo** | Deletes the auto-committed item. Confirmation: brief "Removed" toast. |
| **Timeout** | Toast disappears. Item remains committed. |
| **Stacking** | New captures replace existing toast (only 1 visible at a time). |

### 4.9 Companion Widget

| Rive State | Trigger | Visual | Emotional Register |
|---|---|---|---|
| `0: Idle/Content` | Default resting state | Relaxed breathing animation | Calm, present |
| `1: Celebrating` | Task/habit/study completion | Arms up, `{colors.accent-warm}` aura, confetti particles | Joy, shared accomplishment |
| `2: Encouraging` | App return after absence, freeze day used | Wave gesture | Warm welcome, no judgment |
| `3: Concerned` | Streak at risk (≥14 days, no completion today) | Slight furrowed brow — never sad or crying | Gentle awareness, never guilt |
| `4: Focused` | Focus session active | Eyes closed, meditative pose | Solidarity, shared concentration |
| `5: Sleeping/Resting` | Off-day, late evening, no scheduled items | Sleep animation | Rest is valid, permission to stop |
| `6: Excited` | Major milestone (streak record, level up, shop purchase) | Bouncing celebration | Genuine delight |

**Critical rules**:
- Companion NEVER enters a negative emotional state (sad, angry, disappointed, sick, dying). [FR-8.1]
- Companion NEVER displays text-based guilt ("I missed you", "Feed me", "I'm lonely"). [FR-8.1]
- State transitions use 600–1000ms custom spring curves. [design-system.md: Reward reveals]
- **[OPEN DECISION: Companion species]** — Fixed generic mascot vs. user-selectable species during onboarding. [PRD Addendum §B.13]

### 4.10 Coin HUD & Earn Animation

| Attribute | Behavior |
|---|---|
| **Position** | Top area of Home screen, persistent |
| **Display** | `{colors.coin-gold}` balance in `{typography.data}` with `{colors.coin-glow}` background pill |
| **Earn animation** | Coin icon arcs from completion source to HUD via quadratic Bézier curve (`CoinArcAnimation.kt`). Uses `withFrameNanos` for VSYNC correctness across 60Hz/120Hz. [Architecture §5.1, design-system.md: VSYNC invariant] |
| **Duration** | 600–1000ms spring curve. [design-system.md: Reward reveals] |
| **Reduce Motion** | Skip arc animation; increment counter directly. |
| **Idempotency** | UI shows "+X" only on first earn event for that item. Repeated toggles show nothing. [FR-8.3, PRD Rule 5] |

### 4.11 Focus Timer & Intervention

| Focus State | Visual | System Behavior | PRD Trace |
|---|---|---|---|
| **Prompt** | Hero Card: "Math Study starts now — Start Focus?" | Non-blocking. User can ignore. | FR-4.1 |
| **JITAI intercept** | Full-screen overlay: "Active Schedule: Math Study — Start Focus or Snooze" | ONE prompt per window. Fires only if user opens distracting app during scheduled block without active session. Never spams. | FR-4.2 |
| **Active** | Countdown timer in `{typography.data-large}`. Companion enters "focused" state. | AccessibilityService overlay blocking activates. | FR-4.3 |
| **Distraction detected** | Intervention overlay: blurred `{colors.surface-overlay}` + friction controls | Dimension B Adaptive Intervention Ladder. [FR-4.4] | FR-4.4 |
| **Override friction** | Hold-to-exit button (continuous hold). **[VALIDATION REQUIRED: exact duration, cooldown, escalation]** | Deliberate friction. Escalation on repeat breach within session. [PRD Addendum §A.8] | FR-4.4 |
| **Phone call** | Overlay immediately suspended. Timer behavior: **[OPEN DECISION: pause vs. continue]** | `TelephonyManager.CALL_STATE_RINGING` detected. Session stays `IN_PROGRESS`. | FR-4.6, FR-10.1 |
| **Emergency/dialer** | All blocking bypassed completely. No overlay. | Dynamic whitelist from `TelecomManager.getDefaultDialerPackage()`. | FR-4.6, PRD Rule 2 |
| **Early exit** | Item → `INTERRUPTED/RESUMABLE`. Resumption Card in Hero Card. | NEVER auto-complete. NEVER auto-reschedule. | FR-4.5, PRD Rule 1 |
| **Window expiry** | If item still `IN_PROGRESS` when window ends → `INTERRUPTED/RESUMABLE`. | Resumption Card surfaces. | FR-4.5, PRD Rule 1 |
| **Explicit cancel** | Item → `CANCELLED`. No resumption prompt. | Clean exit. | FR-10.3 |
| **OS kill / reboot** | On next launch: check for incomplete sessions → reconstruct as `INTERRUPTED/RESUMABLE`. | `BOOT_COMPLETED` re-registers alarms. | FR-10.2, PRD Rule 2 |

### 4.12 Resumption Card (Ovsiankina Effect)

| Attribute | Behavior |
|---|---|
| **Trigger** | Focus session exited early OR scheduled window expired with item `IN_PROGRESS` [PRD Rule 1] |
| **Priority** | Hero Card priority 2 (above morning banner, below active session) |
| **Content** | Item title + "X min remaining" (or elapsed since interruption) |
| **Actions** | **Resume** (re-enters focus session), **Review/Adjust** (opens task detail to modify before resuming), **Mark Complete** (marks done, awards Coins) |
| **Persistence** | Remains until explicit user action. NEVER auto-dismisses. [FR-1.4, FR-1.5] |
| **Multiple interrupted** | Stack as scrollable list within Hero Card area. Most recent on top. |

### 4.13 Carry-Forward Card

| Attribute | Behavior |
|---|---|
| **Trigger** | Evening review window (configurable, default ~9 PM) or next morning launch [FR-6.1] |
| **Display** | List of uncompleted flexible Tasks with action buttons per item |
| **Actions per item** | `Do Tomorrow`, `Keep Unscheduled`, `Choose Another Day` (date picker), `Cancel` [FR-6.2] |
| **Fixed-Time Events** | NOT shown here. Must be explicitly re-keyed if needed. [FR-6.3] |
| **Routine Occurrences** | Expire for that day without stacking. Not carry-forwardable. [FR-6.4] |
| **Workload indicator** | Advisory capacity bar shown when selecting "Do Tomorrow". Updates dynamically. NEVER hard-blocks. [FR-6.5, AC-5] |
| **Batch actions** | "Move all to tomorrow" convenience action available |

### 4.14 DayType Banner & Swap

| Attribute | Behavior |
|---|---|
| **Morning display** | Hero Card state (priority 3): "Active: [DayType Name]" with swap icon [FR-3.3] |
| **Tap swap** | Opens bottom sheet picker with available DayType templates |
| **Swap effect** | Applies to TODAY ONLY. Does NOT mutate base templates. [FR-3.4, AC-1] |
| **Mid-day swap** | Preserves completed + in-progress items as immutable. Recalculates remaining unstarted schedule. [FR-3.5, PRD Rule 4] |
| **Resolution hierarchy** | 1) Date-specific user override > 2) Day-of-week default. [FR-3.1] |
| **Auto-load** | On launch: resolves DayType with zero mandatory setup taps. [FR-3.2, AC-1] |

### 4.15 Notification Cards (Urgency Classes)

| Channel | Android Importance | Behavior | PRD Trace |
|---|---|---|---|
| **Routine** | `IMPORTANCE_LOW` | Quiet, in-app only. No sound, no heads-up. | FR-5.1 |
| **Important** | `IMPORTANCE_DEFAULT` | Standard OS notification. Sound + vibration. | FR-5.1 |
| **Urgent** | `IMPORTANCE_HIGH` | Heads-up display + distinctive audio + heavy haptics. Acknowledgement-based bounded escalation. | FR-5.1, FR-5.4 |

**Urgency scarcity**: Marking 3rd active item as Urgent → soft guidance prompt. NEVER hard-blocks. NEVER silently downgrades. [FR-5.2, FR-5.3, AC-4]

**Escalation cadence**: Bounded re-fire for unacknowledged Urgent items. **[HYPOTHESIS: 5-minute interval, max 3 re-fires]** [PRD Addendum §A.5]. Word "nag" strictly prohibited. [FR-5.4]

### 4.16 Unfiled Capture Inbox

| Attribute | Behavior |
|---|---|
| **Entry** | Low-confidence voice captures: raw transcript + partial draft attributes + confidence badge |
| **Display** | List of unfiled items with timestamp, transcript preview, and extracted fields |
| **1-tap categorize** | Tap → pre-populated Task Detail with draft attributes. User confirms or edits to commit. |
| **Bulk actions** | Select multiple → categorize or dismiss |
| **Empty state** | "Inbox zero!" with subtle checkmark illustration. Not demanding. |
| **Badge** | Home surface shows inbox count badge on an indicator. Non-blocking. |

### 4.17 Bottom Sheet (Persistent — DayType Timeline)

| Attribute | Behavior |
|---|---|
| **Position** | Home surface only, above bottom nav |
| **Snap points** | Peek (~100dp: time indicator visible), Half (~50%: several hours visible), Full (~100%: full day visible) |
| **Default** | Peek on cold launch. User preference remembered. |
| **Interaction** | Drag handle at top. Content behind remains interactive at Peek/Half. |
| **Content** | Vertical timeline with DayType-colored blocks. Items positioned by `scheduledTime`. Gaps shown. |
| **Tap timeline item** | Opens corresponding detail screen |

### 4.18 Shop & Item Cards

| Attribute | Behavior |
|---|---|
| **Categories** | Outfits, Accessories, Background Decor. [FR-8.4] |
| **Locked items** | Dimmed with `{colors.surface-overlay}` tint + lock icon + price in `{colors.coin-gold}` |
| **Purchase flow** | Tap → confirmation modal → Coins deducted → unlock animation → item equipped |
| **Equipped indicator** | Checkmark badge on currently active items |
| **Insufficient coins** | Purchase button disabled. Shows required amount. No pressure copy. |

## 5. State Patterns

| State | Surface(s) | Treatment | PRD Trace |
|---|---|---|---|
| **Cold app launch** | Home | Skeleton loading on Hero Card. Timeline loads from local Room cache. Target < 1.5s. | NFR-1 |
| **Empty schedule** | Home | Companion in "sleeping/resting" state + "All caught up for today. Rest well." | — |
| **Empty habit list** | Habits | "Ready to build a new habit?" + primary button to create. | — |
| **Empty study sessions** | Study | "Ready to learn?" + subject creation prompt. | — |
| **Empty unfiled inbox** | Inbox | "Inbox zero!" Subtle checkmark. | — |
| **Empty shop** | Shop | Should not occur (pre-seeded items). If no items: "Shop coming soon." | — |
| **No carry-forward items** | Home (evening) | No carry-forward card displayed. Hero Card falls to Idle state. | — |
| **Offline mode** | App-wide | Subtle offline icon in top app bar. No banner. All core features work. Gemini Live unavailable; voice falls back to on-device STT or manual text. | NFR-2 |
| **Online mode** | App-wide | No explicit indicator. Gemini Live available for enhanced voice capture. | — |
| **STT unavailable** | Voice capture | FAB transitions directly to text input fallback. "Voice not available — type instead." | FR-7.5 |
| **Gemini Live disconnected** | Voice capture | Seamless fallback to on-device SpeechRecognizer. Lower confidence likely. No error banner. | Architecture §7 |
| **On-device STT language missing** | Settings / Voice capture | Diagnostic card: "Download speech language pack for offline voice." Link to Google Play. | — |
| **Permission: Accessibility denied** | Focus features | Focus session available without overlay blocking. Passive notification instead. Diagnostic card in Settings. | — |
| **Permission: Exact Alarms denied** | Reminders | Fallback to inexact alarms. Reminders may be delayed. Diagnostic card in Settings. | NFR-3 |
| **Permission: Notifications denied** | All notifications | No notifications delivered. Diagnostic card in Settings urging enable. | — |
| **Permission: Usage Stats denied** | Analytics | Screen time data unavailable. Feature hidden. Diagnostic card in Settings. | — |
| **Backup in progress** | Backup & Restore | Modal overlay with progress indicator. Blocking — no other interactions until complete. | FR-9.3 |
| **Restore in progress** | Backup & Restore | Modal overlay: "Restoring... This may take a moment." App restarts on success. | FR-9.4–9.6 |
| **Restore failed** | Backup & Restore | Error banner with reason. "Your current data is safe." Pre-restore snapshot auto-restored. | FR-9.6, PRD Rule 6 |
| **Focus session active** | App-wide | Hero Card overrides to Active Focus state. Companion enters "focused" state. All surfaces still accessible but distraction apps blocked. | FR-4.3 |
| **Item INTERRUPTED** | Home | Resumption Card in Hero Card (priority 2). Persists until user acts. | FR-1.3, PRD Rule 1 |
| **Multiple items interrupted** | Home | Scrollable list in Hero Card. Most recent first. | PRD Rule 1 |
| **Companion low energy** | Companion | "Sleeping/Resting" state. No negative implication. | FR-8.1 |
| **Companion max energy** | Companion | "Excited" state on milestone. "Idle/Content" otherwise. | FR-8.1 |

## 6. Interaction Primitives

| Primitive | Usage | Rules |
|---|---|---|
| **Tap** | Primary interaction. All actions. | Every tappable element has ≥48dp touch target. |
| **Long-press** | Context menu on task/habit/routine cards | Edit, Reschedule, Change Intensity, Cancel. Native ripple. |
| **Swipe right** | Quick complete (task cards) | Reveals green completion background. Matches checkbox behavior. |
| **Swipe left** | Dismiss/delete with confirmation | Reveals red delete background. Confirmation bottom sheet for destructive action. |
| **Drag-to-reorder** | Task priority within Action List | Haptic feedback on grab + drop. Reorder persisted immediately. |
| **Hold-to-override** | Focus session exit friction | **[VALIDATION REQUIRED]** Continuous hold duration, cooldown, escalation. [PRD Addendum §A.8] |
| **Voice FAB tap** | Initiate voice capture | Explicit user action only. No wake-word, no always-listening. [Decision 10] |
| **Haptic: completion** | Task/habit completion toggle | Satisfying single pulse. |
| **Haptic: urgent notification** | Urgent delivery channel | Distinct heavy double-pulse pattern. |
| **Haptic: focus breach** | Distraction detected during focus | Sustained vibration (brief). |
| **Haptic: drag** | Card grab for reorder | Light continuous feedback. |
| **Pull-to-refresh** | NONE | Reactive Flow updates from Room. No manual refresh needed. |
| **Infinite scroll** | BANNED | All lists are finite and loaded from local DB. |
| **Pagination** | BANNED | All data is local; load complete lists. |
| **Auto-play audio** | BANNED | All audio is user-initiated. |
| **Wake-word** | BANNED | No background audio monitoring. [Decision 10] |

## 7. Accessibility Floor

Behavioral. Visual contrast lives in `DESIGN.md`.

| Requirement | Implementation | Standard |
|---|---|---|
| **TalkBack / Screen Reader** | Every interactive element: semantic role + state + action hint. "Task, Math Homework, In Progress, Double tap to complete." | WCAG 2.1 AA |
| **State change announcements** | Proactive announcements: "Task completed, 5 coins earned", "Focus session started", "Backup complete". | — |
| **Dynamic type** | All text uses Compose `sp` units. UI remains legible at largest system font scale without truncation. | — |
| **Reduce Motion** | OS `prefers-reduced-motion`: skip coin arc, reward burst, companion transition animations. Show results immediately. Timer continues to update. | — |
| **Touch targets** | ≥48dp on every interactive element. Chips with 32dp visual height expand hit-target to 48dp. | Material 3 |
| **Focus traversal** | Logical top-to-bottom, left-to-right reading order on every surface. Custom `contentDescription` where semantic meaning differs from visual text. | — |
| **AccessibilityService privacy** | Event-driven `TYPE_WINDOW_STATE_CHANGED` package detection ONLY. Zero text logging, zero keystroke capture, zero password/OTP/message access. | FR-10.1, Architecture §4 |
| **Color contrast** | Minimum 4.5:1 ratio for normal text, 3:1 for large text (≥18sp or ≥14sp bold). All `{colors.*}` token pairs verified. | WCAG 2.1 AA |
| **Error identification** | Never rely on color alone. Error states include icon + text description. | WCAG 2.1 1.3.3 |
| **Keyboard navigation** | Full keyboard traversal for external keyboard use. Focus indicators visible. | — |

## 8. Key Flows (Named Protagonist Journeys)

### Flow 1 — Daily Morning Kickoff (Arjun, college student, 7:15 AM)

*Maps to: UJ-1, FR-3.1–3.5, AC-1. Architecture: `ResolveDayTypeUseCase` → `DayTypeRepository` → Room/SQLCipher.*

1. Arjun picks up his phone, opens Personal-Tracker.
2. **Auto-resolution**: The system evaluates `Date-Specific User Override > Day-of-Week Default` and activates "Heavy Study" (Saturday override he set last night).
3. **Hero Card** displays morning DayType banner: *"Good morning! Active: Heavy Study"* with a swap icon.
4. **DayType Timeline** loads in the persistent bottom sheet (Peek state): study blocks, routine slots, and free gaps are visible.
5. Arjun glances at the banner — it's correct. He swipes up the timeline to Half to scan his day.
6. **Climax**: The schedule is laid out with zero taps. Arjun knows his day.

**Failure**: If no DayType override exists and no day-of-week default matches, system falls back to a generic "Day" template. Banner prompts: "No template set for Saturday — choose one?"

**Alternative**: Arjun taps swap → bottom sheet shows available templates → selects "Weekend" → applies to TODAY ONLY. Timeline recalculates. Completed items preserved. [FR-3.4, FR-3.5, PRD Rule 4]

### Flow 2 — Voice Capture & Confidence Routing (Arjun, between classes, 11:30 AM)

*Maps to: UJ-2, FR-7.1–7.6, AC-2. Architecture: `VoiceRouterService` → `GeminiLiveEngine`/`OfflineSTTEngine` → `DeterministicParser` → `TaskRepository`.*

1. Walking between buildings, Arjun taps the **Voice FAB**.
2. FAB morphs to listening pill (300ms). Pulsing waveform appears.
3. Arjun speaks: *"Add read chapter 4 of history tonight at 8."*
4. Audio → Gemini Live (online) → transcription → Deterministic Parser.
5. Parser extracts: Title="Read chapter 4 of history", Time=20:00, Type=Task. **Confidence: High (≥0.85)**.
6. Task auto-commits. Pill collapses. **Undo Toast**: *"Added: Read chapter 4 of history"* (5 seconds).
7. **Climax**: Arjun pockets his phone. Task is captured in 4 seconds without stopping to type.

**Medium-confidence path** (Confidence 0.60–0.84):
- Step 5 alternative: Parser extracts Title="Read chapter 4" but Time is ambiguous. Confidence: Medium.
- **Confirmation Chips** appear in Hero Card area: `[Read chapter 4] [Today] [+Time] [Task] [+Urgency]`.
- Arjun taps the "+Time" chip → time picker → selects 8:00 PM → taps "Confirm".
- Task commits.

**Low-confidence path** (Confidence <0.60):
- Step 5 alternative: Parser can't reliably extract attributes. Confidence: Low.
- Transcript saved to **Unfiled Capture Inbox** with draft fields. Brief toast: *"Couldn't quite catch that — saved for review."*
- Arjun can triage it later.

**Failure — STT unavailable**: FAB tap → on-device SpeechRecognizer not available → immediate transition to text input field. Keyboard opens. Arjun types instead.

**Failure — Gemini Live network loss mid-capture**: WebSocket disconnects → router falls back to on-device SpeechRecognizer seamlessly (lower confidence likely). If on-device STT also unavailable → text fallback.

### Flow 3 — Focus Session & Distraction Intercept (Arjun, evening study, 7:00 PM)

*Maps to: UJ-3, FR-4.1–4.6, FR-10.1–10.3, AC-3. Architecture: `AccessibilityInterventionService` → `FocusSessionManager` → Room.*

1. Schedule time arrives. **Hero Card** shows non-blocking prompt: *"Math Study starts now — Start Focus?"* [FR-4.1]
2. Arjun taps **Start Focus**. Timer activates in Hero Card: countdown from 25:00. Companion enters "focused" state. AccessibilityService overlay blocking activates. [FR-4.3]
3. 10 minutes in, Arjun instinctively opens Instagram.
4. **Distraction detected**: AccessibilityService detects foreground package change via `TYPE_WINDOW_STATE_CHANGED`. [Architecture §1.1]
5. **Intervention overlay** covers the screen immediately: blurred background + *"You're in focus mode. 15:23 remaining."* + Hold-to-exit button. [FR-4.4]
6. Arjun pauses, reads the prompt, and presses Back. Overlay dismisses. Timer continues.
7. **Climax**: Arjun finishes the 25-minute session. Timer reaches 0:00. Task → COMPLETED. Coin animation plays. Companion → "celebrating". Resumption Card does NOT appear (clean completion).

**Override path**: At step 6, Arjun holds the exit button for [VALIDATION REQUIRED] duration. Focus session ends. Task → `INTERRUPTED/RESUMABLE`. Resumption Card appears in Hero Card. [FR-4.5, PRD Rule 1]

**Phone call during focus**: Mom calls. `TelephonyManager.CALL_STATE_RINGING` detected → overlay immediately suspended. Call proceeds unblocked. Timer behavior: **[OPEN DECISION: pause vs. continue]**. After call ends, overlay re-activates if session still active. [FR-4.6, FR-10.1]

**Emergency/dialer**: Always bypassed. No overlay. No interaction. [PRD Rule 2]

**OS kill / reboot**: On next launch, system checks for incomplete `IN_PROGRESS` sessions → reconstructs as `INTERRUPTED/RESUMABLE` → Resumption Card. [FR-10.2]

**JITAI path (no active session)**: Arjun has a scheduled Math Study block but hasn't tapped "Start Focus" yet. He opens Instagram. System shows ONE contextual JITAI prompt: *"Active Schedule: Math Study — Start Focus or Snooze."* If dismissed or snoozed, NO further prompts for this window. [FR-4.2]

### Flow 4 — Rescheduling & Carry Forward Review (Arjun, 9:15 PM)

*Maps to: UJ-4, FR-6.1–6.5, AC-5. Architecture: `CarryForwardUseCase` → `TaskRepository` → Room.*

1. Evening review window triggers (configurable time). System identifies 3 uncompleted flexible Tasks.
2. **Hero Card** shows Carry-Forward Card (priority 5): *"3 items still open. What would you like to do?"*
3. Arjun reviews each item:
   - "Buy groceries" → taps **Do Tomorrow**
   - "Email professor" → taps **Choose Another Day** → date picker → selects Wednesday
   - "Organize notes" → taps **Cancel** (removes from schedule entirely)
4. On selecting "Do Tomorrow", **capacity indicator** appears: *"Tomorrow has 6h 45m scheduled; adding this brings it to 7h 15m"* — bar shows green. Advisory only. [FR-6.5]
5. **Climax**: All items resolved. Hero Card transitions to Intentional Idle. Companion enters "sleeping/resting" state. Arjun's tomorrow is set.

**Fixed-Time Events**: NOT shown in carry-forward. If a fixed-time event was missed, it's surfaced separately with a re-key prompt. [FR-6.3]

**Routine Occurrences**: Expire silently. Not carry-forwardable. [FR-6.4]

**Overloaded tomorrow**: If capacity indicator turns red (>100% planned), advisory copy: *"Tomorrow looks full. Consider spreading items out."* Button: "Move anyway." NEVER blocks. [FR-6.5]

### Flow 5 — Companion Progression & Shop (Arjun, after completing tasks)

*Maps to: UJ-5, FR-8.1–8.5, AC-6. Architecture: `AwardCoinsUseCase` → `CoinRepository` → Room `@Transaction`.*

1. Arjun completes "Math Study" via focus session completion.
2. `AwardCoinsUseCase` validates completion. Checks idempotency key (`TASK_<id>_<timestamp>`). First earn → award Coins. [FR-8.3, PRD Rule 5]
3. **Coin animation**: Coin icon arcs from task card to Coin HUD via Bézier path. HUD counter increments (+5). [design-system.md: VSYNC invariant]
4. **Companion**: Transitions from "focused" → "celebrating" (arms up, confetti). [FR-8.1]
5. Later, Arjun navigates to **Companion** tab. Sees his companion with current cosmetics.
6. Taps **Shop**. Browses Accessories category. Sees a hat for 50 coins. He has 65.
7. Taps hat → confirmation modal: *"Get Explorer Hat for 50 coins?"* → Confirms.
8. **Climax**: Hat appears on companion. Companion → "excited" (bouncing). Balance now 15.

**Rapid toggle farming**: Arjun quickly toggles task complete/incomplete/complete. Second and subsequent `TASK_<id>_<timestamp>` insertions fail silently (unique key constraint). Coins awarded exactly once. [FR-8.3, PRD Rule 5]

**Streak independence**: Coins CANNOT purchase or control Streak Freezes. Streak system is architecturally separate. [FR-8.5]

**[OPEN DECISION: Routine reward granularity]**: Per-step coins (5 coins × 5 steps = 25) vs. completion-only bonus (30 coins on finish). [PRD Addendum §B.9]

### Flow 6 — Encrypted Backup & Transactional Restore (Arjun, new phone)

*Maps to: UJ-6, FR-9.1–9.6, AC-7. Architecture: `BackupManager` → `SecurityKeyStoreManager` → Room → SAF.*

**Export (old phone)**:
1. Arjun goes to **Settings → Data & Backup → Export Backup**.
2. Password entry: two fields (enter + confirm). Strength indicator shown.
3. Taps **Export**. Progress modal with spinner.
4. `PBKDF2-HMAC-SHA512` derives key (100k iterations). `AES-256-GCM` encrypts gzipped JSON of all domain entities. `.ptbackup` written via SAF.
5. **Climax**: *"Backup saved to Downloads/PersonalTracker-2026-08-15.ptbackup"*.
6. Arjun copies file to new phone.

**Import (new phone)**:
1. Arjun installs app on new phone. Goes to **Settings → Data & Backup → Import Backup**.
2. SAF file picker opens. Selects `.ptbackup` file.
3. Password entry: single field.
4. System executes 4-step validation pipeline:
   - Step 1: Password verification (KDF + GCM auth tag check). If wrong → *"Incorrect password. Database unchanged."*
   - Step 2: Cryptographic integrity check (GCM authentication). If tampered → *"This backup file appears damaged. Your current data is safe."*
   - Step 3: Format check (magic bytes, version code). If invalid → *"Unrecognized file format."*
   - Step 4: Schema compatibility check. If mismatch → *"This backup was created by a different version."*
5. Pre-restore safety snapshot created automatically. [FR-9.5]
6. Restore executes atomically.
7. **Climax**: *"Restore complete. Restarting..."* App restarts with full state intact. [AC-7]

**Failure**: Any validation step fails → import halts → pre-restore snapshot automatically restores active database. *"Your current data is safe."* [FR-9.6, PRD Rule 6]

## 9. Platform & Responsive

- **Form factor**: Android phone only (v1). No tablet, fold, or wearable optimization. Portrait orientation is primary; landscape is tolerated (scaled, not optimized).
- **System navigation**: Full support for gesture navigation (swipe from edge) and 3-button navigation (Back, Home, Recents). System Back always navigates up one level or dismisses modal.
- **Edge-to-edge**: Content extends under transparent system bars using Compose `WindowInsets`. Status bar and navigation bar areas padded with system inset values.
- **Keyboard handling**: Voice FAB and text inputs reposition dynamically above the IME. Bottom sheet adjusts. No content hidden behind keyboard.
- **Split-screen**: Basic support — app scales without crashing. No custom dual-pane behavior.
- **Picture-in-Picture**: Not supported in v1.
- **16KB page kernel**: App and native libraries (SQLCipher `libsqlcipher.so`) built with 16KB ELF segment alignment for Android 15+ compatibility. [Architecture §4.2, §9.1]

## 10. Offline / Online States

| Feature | Online | Offline | Transition |
|---|---|---|---|
| **Voice Capture (Gemini Live)** | Full real-time bidirectional voice | Unavailable | Automatic fallback to on-device STT |
| **Voice Capture (On-device STT)** | Available | Available (if language pack installed) | No transition needed |
| **Voice Capture (Manual text)** | Available | Available | Always available as final fallback |
| **Task/Routine/Habit/Study CRUD** | Fully functional | Fully functional | No transition — local Room DB |
| **Focus Session** | Fully functional | Fully functional | No transition — local state |
| **Companion & Shop** | Fully functional | Fully functional | No transition — local Coin ledger |
| **Notifications** | Fully functional | Fully functional | No transition — local AlarmManager |
| **Backup Export** | Fully functional | Fully functional | No transition — local file |
| **Backup Import** | Fully functional | Fully functional | No transition — local file |

**Network state indicator**: Subtle offline icon in top app bar when network is unavailable. No blocking banner. No interruption to workflow. Icon disappears when connectivity returns. [NFR-2]

**Gemini Live failover**: WebSocket disconnect → automatic re-connect attempt (exponential backoff). If capture is in progress, seamlessly falls back to on-device STT. User may notice lower confidence but flow is uninterrupted. [Architecture §7]

## 11. Permissions & Capability Diagnostic

The Permissions & Diagnostic surface serves both onboarding (first launch) and troubleshooting (Settings).

| Capability | Why Needed | Grant Flow | Denied Degradation |
|---|---|---|---|
| **AccessibilityService** | Block distracting apps during focus sessions | Explain purpose → deep link to system Accessibility settings → user toggles on | Focus sessions work without overlay blocking; passive notification-only reminders |
| **Exact Alarms** (`SCHEDULE_EXACT_ALARM`) | Precise timing for routine/task reminders | Runtime `canScheduleExactAlarms()` check → if denied, prompt with explanation | Fallback to inexact `WorkManager` scheduling; reminders may be delayed |
| **Notifications** (`POST_NOTIFICATIONS`, API 33+) | Deliver Routine/Important/Urgent reminders | Standard runtime permission dialog | No notifications delivered; in-app banners only |
| **Usage Stats** (`USAGE_STATS_ACCESS`) | Screen time analytics and distraction history | Explain purpose → deep link to system Usage Access settings | Screen time feature hidden; no data collected |
| **On-device STT Language Pack** | Offline voice capture | Check `SpeechRecognizer.isRecognitionAvailable()` → prompt Google Play download | Offline voice unavailable; manual text fallback |

**First launch flow**: After initial app open, a non-blocking diagnostic card shows recommended permissions. User can grant, skip, or defer each independently. App is fully functional (with graceful degradation) regardless of permission state. No blocking "grant all permissions" gate.

**Settings diagnostic**: Persistent card in Settings showing current permission status with color-coded indicators (green = granted, amber = degraded, gray = not needed for current usage).

## 12. Inspiration & Anti-patterns

### Lifted From (Acknowledged Influences)

- **Todoist** — Quick-add speed. Sub-second capture from any surface. The gold standard for low-friction task entry.
- **Forest** — Focus gamification concept. The idea that a companion stakes something on your focus time. But Personal-Tracker strips out the punitive "dead tree" mechanic entirely — the companion never suffers.
- **Habitica** — Virtual companion concept. Rewarding real-world habits with virtual progression. But zero HP, zero character death, zero stress. The companion is always supportive.
- **Structured (iOS)** — DayType timeline concept. A structured day view that adapts to different day types. But with deeper template engine support.
- **Things 3** — Clean, intentional task management aesthetic. Generous whitespace, calm surfaces, focus on what matters today.

### Rejected Anti-patterns

- **Duolingo streaks** — Weaponized calendar. Passive-aggressive notifications. Guilt-trip copy ("Duo is sad!"). Personal-Tracker's streak system uses earned grace/freeze days and never punishes. [Research Dimension F]
- **Social accountability** — Leaderboards, shared progress, accountability partners. Not in v1 scope. [Decision 10]
- **Always-on monitoring** — Background audio, wake-word detection, continuous keystroke logging. Privacy violation. The AccessibilityService uses event-driven package detection ONLY. [Research Dimension G, Decision 10]
- **Punishment mechanics** — HP loss, character death, negative reinforcement, guilt notifications. Anti-research across Dimensions B, F, and H. The companion is NEVER disappointed.
- **Dark patterns in notifications** — Re-engagement nudges, FOMO copy, "You haven't opened the app in 3 days!" Notification copy is always informational and actionable.

---

## Implementation Notes

### Items Marked [HYPOTHESIS] — Require Runtime Validation

| Item | Current Baseline | Validation Method | PRD Reference |
|---|---|---|---|
| STT confidence thresholds | High ≥0.85, Med 0.60–0.84, Low <0.60 | A/B testing with real speech patterns | Addendum §A.2 |
| Voice undo toast duration | 5 seconds | User feedback on revert frequency | Addendum §A.3 |
| Urgent item quota ceiling | 2 active items before soft guidance | Monitor how often users override | Addendum §A.4 |
| Acknowledgement escalation cadence | 5-min retry, max 3 re-fires | Notification engagement analytics | Addendum §A.5 |
| Workload capacity formula | Sum of `estimatedDurationMinutes` | User satisfaction with advisory accuracy | Addendum §A.6 |
| Focus minimum duration for coins | 10 minutes | Observe session duration patterns | Addendum §A.7 |
| Focus override friction parameters | Duration, cooldown, escalation | Measure override frequency and session completion rates | Addendum §A.8 |

### Items Marked [OPEN DECISION] — Require Product Decision

| Decision | Options | Blocking? | PRD Reference |
|---|---|---|---|
| Phone-call timer behavior during focus | A) Pause timer automatically B) Continue timer (strict time-boxing) | Non-blocking (configurable via policy) | Addendum §B.10 |
| Routine reward granularity | A) Per-step coins B) Completion-only bonus C) Both (per-step + completion bonus) | Non-blocking (configurable via `RewardPolicy`) | Addendum §B.9 |
| Companion species selection | A) Fixed generic mascot B) User-selectable during onboarding | Non-blocking (UI-only; Rive state machine is species-agnostic) | Addendum §B.13 |
| DayType calendar exceptions | Future scope — not v1 | Non-blocking (excluded from v1) | Addendum §B.11 |

### UX Consistency Audit Notes

1. ✅ All 10 FRs have corresponding component patterns and flow coverage.
2. ✅ All 5 NFRs have corresponding state or interaction specifications.
3. ✅ All 8 ACs are traceable through flows.
4. ✅ All 6 UJs are covered as named-protagonist key flows.
5. ✅ All [HYPOTHESIS] items are marked, not assumed.
6. ✅ All [OPEN DECISION] items are marked with options, not resolved.
7. ✅ All [EXCLUDED] items (cloud sync, social, monetization, punitive, wake-word, calendar sync) are absent.
8. ✅ `DESIGN.md` token references use `{path.to.token}` syntax throughout.
9. ✅ design-system.md component specs are referenced, not duplicated.
10. ✅ Architecture AI Action Boundary (§5.2) is reflected in Voice FAB state machine.
