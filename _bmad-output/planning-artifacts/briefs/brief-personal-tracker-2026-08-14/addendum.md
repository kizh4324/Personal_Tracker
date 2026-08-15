# Personal-Tracker — Product Brief Addendum

> [!NOTE]
> **Document Purpose**: Supplementary reference document containing complete decision traceability, research cross-references, and open decision registries for downstream architecture (BMad Spec / Technical Architecture) and PRD phases.

---

## Appendix A: Detailed Decision Audit & User Refinements Log

| Decision # | Decision Topic | Selected Option & User Refinements | Research Grounding & Status |
|---|---|---|---|
| **Decision 1** | **Primary Home Surface & Daily Operating Model** | **Option C (State-Aware Focus & Resumption Hub)**: Dynamic Hero Card shows current actionable state (active focus/task, upcoming scheduled item, interrupted/resumable task Ovsiankina prompt, or intentional idle state). Secondary "Today's Schedule" and "Action List" sit below to minimize information density. | `[DECISION]` / `[HYPOTHESIS: v1 validation required]`. Informed by frozen Dimensions A, B, H. |
| **Decision 2** | **Item Model & Creation Architecture** | **Option C (Unified Capture with Specialized Domain Models)**: Single text/voice capture entry point inferring type (with 1-tap confirmation fallback). Mapped to distinct domain models (Tasks, Routines with DayType, Habits with slack, Study Sessions with heatmaps) sharing scheduling, focus, reward, and reminder services. | `[DECISION]` / `[EVIDENCE-BACKED]`. Type inference thresholds & NLP syntax marked as `[HYPOTHESIS]`. |
| **Decision 3** | **DayType Assignment & Morning Workflow** | **Option C (Auto-Map Default + Non-Blocking Morning Banner)**: Hierarchy: 1) date override > 2) calendar exception > 3) day-of-week default. Zero mandatory friction on launch. Hero Card banner offers 1-tap single-date swap (`Active: Weekday Routine — Change to Heavy Study?`) without mutating base templates. | `[DECISION]` / `[EVIDENCE-BACKED: Dimensions A & H]`. Calendar integration rules marked as `[OPEN DECISION]`. |
| **Decision 4** | **Focus Sessions & Distraction Interventions** | **Option C (State-Aware Smart Prompting)**: Scheduled block start presents Hero Card prompt. Opening distracting app during scheduled focus block triggers **ONE** contextual JITAI prompt. Explicit `FocusSession` start activates `AccessibilityService` overlay blocking and B ladder. | `[DECISION]` / `[EVIDENCE-BACKED: Dimensions B, G, H]`. Snooze duration & prompt caps marked as `[HYPOTHESIS]`. |
| **Decision 5** | **Notification Urgency & Scarcity Guidance** | **Option A (Soft-Cap Quota Guidance)**: Routine quiet/in-app, Important standard, Urgent `IMPORTANCE_HIGH` heads-up + audio/haptics. Marking 3rd active item as Urgent displays soft prompt (`Keep Urgent` vs `Change to Important`). Uses acknowledgement-based escalation. | `[DECISION]` / `[EVIDENCE-BACKED: Dimensions C & G]`. Exact 1–2 quota ceiling & retry intervals marked as `[HYPOTHESIS]`. |
| **Decision 6** | **Rescheduling Policy & Carry Forward** | **Option C (Transparent Carry Forward + Advisory Capacity Indicator)**: Flexible tasks trigger evening/morning review card (`Do Tomorrow`, `Keep Unscheduled`, `Choose Another Day`, `Cancel`). Fixed Events require re-keying; Routines expire without stacking; Habits use slack. Workload indicator is advisory only. | `[DECISION]` / `[EVIDENCE-BACKED: Dimension D]`. Exact capacity formulas & button copy marked as `[HYPOTHESIS]`. |
| **Decision 7** | **Voice Capture & AI Action Boundary** | **Option C (Confidence-Threshold Routing)**: Audio → STT → text → parser → validation → confidence routing → Task Service → DB. High confidence (immediate + 5s Undo toast); Medium (confirmation card); Low/Failed (Unfiled Inbox). Strict AI Action Boundary: model output never directly mutates DB without schema/business rule validation. | `[DECISION]` / `[EVIDENCE-BACKED: Dimensions E, G, H]`. Exact confidence thresholds & 5s Undo duration marked as `[HYPOTHESIS]`. |
| **Decision 8** | **Companion Persona & Coins Economy Scope** | **Option A (Lightweight Cosmetic & Decor Progression)**: Non-punishing buddy on Hero Card. Coins earned for genuine completions, spent ONLY on cosmetics/decor. Streak system is architecturally separate from Coins. Real-life reward sinks excluded from v1. | `[DECISION]` / `[EVIDENCE-BACKED: Dimension F]`. Exact Coin values, item prices, & shop categories marked as `[HYPOTHESIS]`. |
| **Decision 9** | **Data Storage, Security & Backup Model** | **Option B (Encrypted Local DB + Password-Protected Encrypted Backup)**: Room + SQLCipher encryption at rest; OS application sandbox protection. Manual user-initiated backup produces password-encrypted artifact. Audio discarded post-STT. Zero default cloud sync. | `[DECISION]` / `[EVIDENCE-BACKED: Dimension G]`. Cryptographic algorithm selection & key derivation delegated to Architecture phase (`[OPEN DECISION]`). |
| **Decision 10** | **Explicit V1 Exclusions & Non-Goals** | **Option A (Standard Personal-Use Exclusions)**: Excludes multi-user, cloud sync servers, monetization, paid repairs, punitive gamification, real-life reward sinks, direct AI DB mutation, hard break locks, mandatory cloud AI, and system calendar import. Includes voice capture, encrypted local storage, encrypted manual backup, and cosmetic companion. | `[DECISION]` / `[EVIDENCE-BACKED: Dimensions A–H]`. |

---

## Appendix B: Research Dimension Mapping & Architectural Invariants

### Dimension A — Day-Type Schedule Engine
- `DayType` is a first-class scheduling entity (`Weekday`, `Weekend`, `Exam Day`).
- Daily routines and study subjects link directly to `DayType` templates.
- Daily scheduling operates 100% offline without network requirements.

### Dimension B — Adaptive Intervention Ladder & Focus Sessions
- Distraction intervention is task/focus-state aware (`DayType → Routine/Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation → completion`).
- Honest override contract: User can override a block, but override requires deliberate friction (e.g. 5-second pause point or cool-down timer) rather than hard permanent locks.
- `FocusSession` is the authoritative state for real-time `AccessibilityService` overlay redirects.

### Dimension C — Notification Urgency, Scarcity & Escalation
- Separation of Task Importance vs. Notification Delivery Intensity: `Routine → Important → Urgent`.
- Single `Urgent` delivery class with scarcity guidance (soft prompt at 3rd active item).
- Acknowledgement-based urgent reminder escalation (Due-style repeat re-fire).
- Persistent status-bar notifications (`Notification.ProgressStyle`) restricted to active Focus Sessions / routine sessions only.

### Dimension D — Breaks, Rescheduling & Item-Type Aware Recovery
- Flexible break suggestions over hard-enforced break locks.
- Item-Type Aware Rescheduling Policy:
  - Fixed-Time Events: Require re-keying or explicit user action.
  - Flexible Tasks: Transparent user-confirmed "Do Tomorrow" rescheduling.
  - Routine Occurrences: Expire for that day without stacking.
  - Habits: Miss-tolerant recovery / earned streak slack.
  - Study Sessions: Follow configured scheduling policy.
- Advisory workload capacity warnings during carry-forward review.

### Dimension E — Capture Friction, Voice & Guilt-Free Tone
- One-line natural-language quick-add text capture (`validated market pattern`).
- Hands-free voice capture with confidence floor & Unfiled Capture Inbox fallback.
- Guilt-free failure tone: Supportive, non-judgmental communication on missed goals.

### Dimension F — Non-Punishing Companion & Coins Economy
- Non-punishing companion pet/mascot (Finch model; zero HP loss, zero character death, zero guilt reactions).
- Single shared Coins currency awarded for genuine completion events (tasks, routine steps, habits, study sessions, focus sessions).
- Coins spent exclusively on companion cosmetics, outfits, accessories, and background decor in v1.
- Architectural separation between Streak system and Companion economy.

### Dimension G — Android Feasibility & Local Security
- `POST_NOTIFICATIONS` runtime permission on Android 13+ (API 33+).
- `setAlarmClock()` for time-critical urgent alarms; `WorkManager` for deferrable background jobs.
- `AccessibilityService` overlay redirect for real-time task-aware app blocking (opt-in, 100% local, zero text/keystroke logging).
- `UsageStatsManager` (`PACKAGE_USAGE_STATS`) for historical analytics and distraction stats.
- Local database encryption at rest via Room + SQLCipher; password-protected manual encrypted backup.
- Strict AI Action Boundary: Model outputs (STT, LLMs) NEVER directly mutate database records without schema/business rule validation.

### Dimension H — Behavioral Evidence & Validation Boundaries
- Procrastination is a validated problem (`r = -0.18` performance correlation).
- Notification volume (~63.5/day) causes alert fatigue (`Dual-Attention Principle`).
- Zeigarnik effect is a dead-end premise; Ovsiankina task resumption effect is reliable (1-tap resumption prompts).
- Implementation Intentions (If-Then rules) augment `DayType` scheduling (`DayType → scheduled routine/task → optional If-Then condition → contextual execution cue`).
- JITAI interventions require restraint (`JITAI Restraint Principle`: intervene only when sufficient evidence exists).
- Reminders are a documented double-edged mechanism (contextual support, not sole execution driver).
- Habit automaticity is gradual (~66 days median); miss-tolerant recovery is standard.

---

## Appendix C: Complete Open Decision & Hypothesis Registry

The following items are explicitly categorized as **Preliminary Product Hypotheses** or **Open Decisions** to be validated during personal use or specified in the downstream System Architecture / Spec phase:

1. **State-Aware Hero Card UX**: Exact visual layout, transition animations, and hero card card-swapping gestures `[HYPOTHESIS]`.
2. **Type-Inference Confidence Thresholds**: Exact numeric confidence floor for voice/NLP parsing and confirmation card triggering `[HYPOTHESIS]`.
3. **NLP Syntactical Rules**: Exact date, time, subject, and routine tag syntax for one-line quick-add parsing `[HYPOTHESIS]`.
4. **Calendar Exception Rules**: Exact logic for detecting date exceptions and persisting single-day template overrides `[OPEN DECISION]`.
5. **JITAI Intercept & Snooze Tuning**: Exact snooze duration (e.g. 15m), prompt count caps, and escalation timing thresholds `[HYPOTHESIS]`.
6. **Urgent Item Quota Accounting**: Exact calculation for active Urgent items vs completed items and soft-prompt copy `[HYPOTHESIS]`.
7. **Acknowledgement Escalation Cadence**: Exact retry intervals, maximum re-fires, acknowledgement timeouts, and sound/vibration profiles `[HYPOTHESIS]`.
8. **Workload Capacity Formula**: Exact algorithm for calculating planned daily workload (hours/minutes) and advisory threshold copy `[HYPOTHESIS]`.
9. **Voice Undo Toast Duration**: Exact display duration (e.g. 5s vs 8s) for the tap-to-revert toast pill `[HYPOTHESIS]`.
10. **Coin Economy Pricing & Balancing**: Exact Coin award amounts per completion type, cosmetic shop prices, and anti-farming detection thresholds `[HYPOTHESIS]`.
11. **Cryptographic Algorithm & Key Derivation**: Specific encryption cipher (AES-256-GCM / SQLCipher), key derivation function (Argon2 / PBKDF2), and Android Keystore integration strategy `[OPEN DECISION — Architecture Phase]`.
