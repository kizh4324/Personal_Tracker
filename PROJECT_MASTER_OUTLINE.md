# Project Master Outline — Personal Task / Habit / Routine App

> **Status:** Template, partially filled. §1.1 (incl. negative consequences),
> §1.2, §1.3 (incl. gamification research), §1.5 (desired outcomes), §2.4,
> §2.5, §2.7, and §2.8 (gamification system) now have real content.
> Everything else is still a placeholder for you to complete yourself (or
> delegate a section at a time to an agent) before moving to the next phase.
> **Build model:** Solo builder acting as Product Owner + System Architect +
> Developer, assisted by AI agents (spec-kit / BMAD-METHOD + Codex / Antigravity / Claude).
> **Constraints locked in:** personal use only, mobile-only (no web), 100% free,
> no paid components, production-quality target.

---

## 0. Project Meta
- [ ] Project name / codename:
- [ ] Repo location:
- [ ] Owner (you):
- [ ] Primary build tools of record (agent, IDE, model per role):
- [ ] Target OS(es): Android / iOS / both
- [ ] Target device(s) you'll actually run this on:

---

## 1. Discovery — Problem Definition & Market Research
*(Your initial step — leave content to yourself; this is scaffolding only.)*

### 1.1 Problem Statement
- Losing track of daily routines, learnings, tasks, work timings (doing a task, catching a train, arriving at college, etc.), habits, bad habits, and production/output timing.
- Wasting time scrolling on the phone during free time, even when there are important meetings or things that need to get done.
- Falling into a pattern of laziness and not working, running late for the train, arriving late for college, and doing everything late — a general "slow and undisciplined" pattern rather than a single isolated incident.

**Negative consequences if left unaddressed:**
- Missed opportunities that don't come back around — a dismissed notification isn't just annoying, it can mean a missed interview slot, deadline, or time-sensitive opportunity.
- Falling behind academically/professionally in a way that compounds — each missed session or late arrival makes the next one likelier, not less.
- A recurring cycle of regret — noticing at the end of the day what didn't get done, without a system that helps break the pattern rather than just registering it after the fact.
- Erosion of trust in your own tools, and by extension yourself — every abandoned productivity app becomes evidence that "this doesn't work for me," which is itself a barrier to trying the next fix.

### 1.2 Personal Pain Point Log
- Daily routines differ completely day to day (a Monday routine differs from a Sunday routine) — a single fixed daily template doesn't reflect reality.
- Learning/study time is inconsistent — no tracking of how many hours are actually spent, or on what.
- Bad habits get triggered during unstructured free time, with no system catching or redirecting that in the moment.
- Important/urgent items (a task, a meeting) need to stand out from routine ones, and currently don't.
- Long study/task sessions run without enforced breaks.
- No priority system for tasks — no handling for what happens when something becomes suddenly unavailable/blocked for a couple of days and needs to reschedule.
- No screen-time visibility or app-locking to stop the drift into distraction.
- Plain text/notification reminders don't work — get dismissed unread (see §1.4).

### 1.3 Existing Alternatives Audit

#### 1.3.1 Focus / Distraction Blocking
- **Opal** — blocks distracting apps with a "Deep Focus" mode; tracks screen-time patterns.
- **One Sec** — doesn't block outright; forces a breathing pause before opening a distracting app.
- **Forest** — gamifies focus with a growing virtual tree; offers a whitelist mode for allowed apps during a session.
- **Freedom** — multi-device play; one session blocks the same sites/apps across Android, iOS, Mac, and Windows simultaneously.
- **Category verdict:** None of these touch tasks, habits, or notifications — they're pure attention-blockers, usually subscription-priced.

#### 1.3.2 Habit Tracking
- **Habitica** — gamifies habits/dailies/to-dos with RPG-style rewards and social party accountability.
- **Streaks** — the deliberate opposite of Habitica; a handful of habits as tap-to-complete circles, iOS/Mac only, one-time purchase, intentionally light on features.
- **Habitify** — differentiates good vs. bad habits explicitly; computes a "Habit Strength" score from streak length, consistency, and frequency.
- **Category verdict:** All habit-only — no task/calendar integration, no notification handling.

#### 1.3.3 AI Scheduling / Planning
- **Motion** — auto-schedules the whole day continuously and rebuilds it as things shift; aimed at users who want the AI to just decide, not approve first.
- **Sunsama** — deliberately the opposite of Motion; a guided daily planning ritual where the user chooses and time-estimates each task rather than letting an algorithm do it.
- **Reclaim.ai** — defends focus time and auto-reschedules, built specifically around Google Calendar.
- **Category verdict:** Calendar/task-scheduling tools built for professionals with meeting-heavy calendars — none handle habits as a first-class object, notification triage, or app blocking. All are cloud SaaS with monthly fees.

#### 1.3.4 Competitive Gap / Differentiation
No product found combines private notification-priority-resurfacing (so a dismissed OTP or interview invite isn't actually lost) with task/habit/routine tracking **and** focus/app-blocking **and** voice capture, as one local-first, account-free, single-user system. Closest partial overlaps: Opal (screen-time analytics, no tasks) and Habitify (Focus Mode timer, no notification handling). The "critical item resurfacing" concept (see §6) is not something any mainstream competitor does — most apps treat a dismissed notification as simply gone. This is the differentiator to design around, not the voice/AI layer (several competitors are already racing to add that).

| Tool tried | What it does well | Why it fails for you | Verdict |
|---|---|---|---|
| Google Tasks | | | |
| | | | |

#### 1.3.5 Gamification Patterns (research for the gamified-UI requirement)
- **Duolingo** — XP is the single unifying currency across every mechanic (streaks, leagues, achievements), so progress feels coherent instead of fragmented. Streaks lean hard on loss aversion — missing a day threatens the whole streak — softened only by a purchasable "Streak Freeze" insurance mechanic. Leagues add social competition, not relevant here since this is a single-user app.
- **Habitica** — a full RPG framework: habits/dailies/to-dos as quests, streaks as "proof," social party accountability. Closest existing analog to what was originally sketched in the handwritten feature notes (§2.4), but its penalty mechanics (losing HP for missed dailies) run counter to the explicit "don't make me feel more upset" requirement already locked in for feature (iv).
- **Finch** — the strongest tonal fit. A virtual pet grows only through positive reinforcement tied to completing real, user-defined self-care goals; deliberately avoids streak-based guilt and frames missed days gently rather than punishing them. Layers a shop, daily quests, and small celebratory micro-interactions (confetti, pet reactions) on top of one core loop, and lets the user set their own goals rather than following a fixed program.
- **Forest** — single-metaphor gamification (a tree grows while you stay focused, dies if you leave) — effective specifically because it's simple and tied to one clear behavior, not spread across a whole app.
- **What this means here:** the gentle, positive-reinforcement-only model (Finch) fits the stated tone requirement far better than the loss-aversion/streak-guilt model (Duolingo, Habitica) — a punishing gamification layer would directly contradict the "don't make me feel more upset" requirement already set for the AI-persona feature (iv). Duolingo's unifying-currency idea is still worth keeping, decoupled from its guilt mechanics — one shared point system across every feature keeps the whole app feeling like one game rather than several disconnected ones. See §2.8 for how this gets applied.
-

### 1.5 Success Criteria / Definition of Done

**Desired outcomes if this works:**
- Routines, habits, and study time actually get tracked and followed — not just recorded after the fact, but genuinely followed because reminders reach you.
- Important/urgent items never silently get lost — even when everything else gets cleared.
- Bad-habit trigger windows get caught and redirected in the moment, not noticed only in hindsight.
- The regret-review-at-the-end-of-the-day pattern shrinks, because the system catches things earlier in the day instead of just logging failures at night.
- You trust the app enough to keep using it past week one — the actual failure mode of every tool tried so far.

### 1.6 Hard Constraints
- Free only:
- No web version:
- No paid components:
- Single user (you):
- Other:

---

## 2. Product Definition (PRD)
*(Leave empty — this is your PRD to write, not to be pre-filled.)*

### 2.1 Vision Statement
-

### 2.2 User Persona (you)
-

### 2.3 Jobs-to-be-Done
-

### 2.4 Feature List (MoSCoW)
*(Sourced from your handwritten feature notes. Original numbering (i–x) kept for traceability. Refinement/solution notes added where a feature was underspecified or unsolved — kept non-technical per your request; deeper technical treatment lives in §4–§6.)*

**Must have**
- **(i) Daily Routines** — Morning-to-night routine tracking with a heatmap view (✓/✗ per item), informing you of the routine as it happens.
  - *Refinement:* Your own note that "Monday can differ, Sunday can differ" means one fixed daily template won't work — support multiple routine templates keyed by day-of-week (or day-type: weekday/weekend/college-day), with the heatmap built from actual completion history, not the template.
- **(v) Habits / Bad Habits — Streaks & Proof** — New habits to follow, tracked as streaks; daily tick mark (✓/✗) as the "proof," entered manually; consistency/progress view; tick mark surfaced on the calendar UI. Same mechanism applies to bad habits (tracking non-occurrence).
  - *Refinement:* This is explicitly self-reported, not auto-verified — worth stating as a deliberate MVP scope decision (honesty-based logging), with any auto-verification left as a future idea rather than a v1 requirement.
- **(vii) Special Reminders** — A mandatory "urgent/compulsory" category for important tasks or meetings that need extra visual/behavioral weight beyond a normal reminder.
  - *Refinement:* This is the same mechanism as the notification-priority-resurfacing differentiator already identified in market research (§1.3.4) — build it once, reuse it for both.
- **(ix) Tasks — Action Priority Matrix** — Priority-ranked tasks with reminders at each level (weekly/monthly/specific-date), and state tracking (before start / in progress / end-if-complete / not complete).
  - *Proposed solution — edge case (sudden 2-day unavailability):* when you mark yourself unavailable for a stretch, the app should automatically shift every reminder and soft-deadline that falls inside that window forward, rather than letting them fire (and get missed) or silently vanish. You confirm the shift once; you don't have to reschedule each item individually.
- **(x) Screen-time Viewer / Warning + App Locking** — Visibility into screen time, with reminders/locking on chosen apps, framed around you deciding upfront (while calm and rational) what "high" usage means for you, rather than the app deciding for you in the moment.
  - *Refinement:* Matches the phased approach already agreed in the architecture discussion — soft warning/interstitial first, hard lock as a later escalation once the feature proves useful.
- **(iv) AI-Persona Supporter** — A consistent, named/personified support presence (not just text notifications) — real UI and light animation, speaks supportively on failure or a missed task, encourages without adding to guilt or making you feel worse.
  - *Refinement:* This persona should be the *delivery mechanism* for Special Reminders (vii) and habit feedback (v), not a separate feature — one voice/presence across the app rather than a generic notification tone plus a separate "AI supporter" bolted on.
- **Voice task assignment** — "Hi Gemini, assign task with good precision" — natural-language task capture via voice, expected to extract the task accurately.
  - *Refinement:* This is the voice front-end to the Gemini-as-language-layer architecture already defined (§6) — precision here means good date/time/priority extraction, which is a prompt-and-validation problem, not a new subsystem.

**Should have**
- **(ii) Learning / Study Tracking** *(a sub-type of habit tracking)* — Completion percentage or date, how many hours studied, on a flexible schedule, with a breakdown by subject/topic.
  - *Clarification needed:* your note mentions a chart for tracking "particulars" — read as wanting a per-subject/topic breakdown (e.g. a pie chart of hours by subject) rather than one lump study-time number. Flagged in Open Questions below to confirm.
- **(viii) Breaks** — Mandatory breaks during long continuous study/task sessions (your example: a 3-hour study session needing breaks worked in across it).
  - *Proposed solution:* a plain, non-negotiable cadence — e.g. every ~50–60 minutes of continuously tracked activity, a mandatory 10–15 minute break is enforced before the session can continue. For your 3-hour example that lands around 2 breaks, matching the "2 breaks" figure in your notes. Flagged below so you can confirm or adjust the exact interval.
- **(vi) Bad Habit Trigger Avoidance** — Notice when unstructured free time opens up (a likely trigger window) and proactively suggest a productive activity to fill it, rather than leaving it open to drift into the bad habit.
  - *Proposed solution:* this is a known behavior-change technique (pre-committing a replacement activity for a known trigger window, sometimes called an "if-then" plan) rather than something that needs to be invented from scratch — when free time is detected, offer one or two pre-chosen substitute activities instead of leaving the slot empty.

**Could have**
- Richer AI-persona animation/voice styling beyond the baseline supportive presence.
- Pie-chart / visual analytics for learning-time breakdown by subject.
- Calendar-UI polish for the habit "roadmap" view.

**Won't have (v1)**
- Social features, leaderboards, or leagues (see §2.5).
- Loss-based/punishing gamification mechanics — no hard streak resets, no HP-loss penalties (see §2.5, §2.8).

### 2.5 Non-Goals
- No social features, leaderboards, or leagues — single-user app, nothing to compare against.
- No punishing/loss-based gamification mechanics (hard streak resets, HP loss, guilt-inducing messaging) — conflicts with the supportive, non-upsetting tone already required for the AI-persona feature (iv). See §2.8.

### 2.6 Assumptions
-

### 2.7 Open Questions
- **Learning tracking (ii):** does "particulars" mean you want hours broken down by subject/topic, or something else? Confirms whether a per-subject pie chart is the right visual.
- **Breaks (viii):** is the break cadence "every hour of continuous work" or "2 breaks total across a 3-hour session"? Both were mentioned; they imply slightly different rules.
- **Bad-habit trigger avoidance (vi):** how many substitute-activity suggestions should the app keep on hand, and do you want to pick them in advance per trigger, or have the app propose new ones each time?
- **AI-persona supporter (iv):** how much animation/voice is "compulsory" for v1 vs. a later polish pass — full custom character, or a simpler consistent visual identity to start?
- **Gamification (§2.8):** does the companion need a name/species chosen by you at onboarding (Finch-style), or should it start pre-defined to reduce setup friction?

### 2.8 Gamification System (Cross-Cutting)
*(Applies across every feature in §2.4, not as a separate bolt-on — added per the explicit requirement for a fun, motivating, gamified UI and flow throughout. Grounded in §1.3.5: Finch's shame-free companion-growth model + Duolingo's single unifying currency, deliberately without Duolingo/Habitica's loss-punishing streak and HP mechanics, since those conflict with the non-upsetting tone already locked in for feature (iv).)*

**Core model:**
- The AI-persona supporter (iv) *is* the gamified companion — merge them rather than building two systems. The companion visually grows, evolves, and reacts as things get completed, the same role Finch's bird plays, but it's also the voice that delivers reminders and encouragement (vii). One character, one relationship, instead of a persona plus an unrelated separate pet.
- A single XP/point currency, shared across every feature, so nothing feels like a side-system: routine items, habit ticks, study sessions, tasks, and even taking an enforced break all feed the same number.
- No mechanic that punishes a miss. Streaks stay visible and satisfying to build, but use a Duolingo-style grace/freeze concept instead of a hard reset to zero that triggers guilt — matching the "don't make me feel more upset" requirement directly.

**Per-feature gamification mapping:**
| Feature | Gamified treatment |
|---|---|
| (i) Daily Routines | Each completed item = small XP + companion reacts; the heatmap doubles as a visual progress trail |
| (ii) Learning/Study | Session completion = XP + a subject-specific progress bar (ties into the pie-chart idea already flagged in §2.7) |
| (v) Habits/Bad Habits | Streak counter with a grace-day mechanic instead of a hard reset; a tick = XP; consistency is rewarded, not perfection |
| (vi) Trigger Avoidance | Choosing a substitute activity during a detected trigger window = bonus XP — reframes resisting a trigger as a visible win, not an invisible non-event |
| (vii) Special Reminders | Completing an urgent item on time = bonus XP + a distinct celebratory animation, since these are the highest-stakes items |
| (viii) Breaks | Taking the mandatory break = small XP, so breaks read as a positive action rather than just an enforced rule |
| (ix) Tasks / Priority Matrix | XP scaled by priority tier — a Special/urgent task completed is worth more than a routine one |
| (x) Screen-time Control | Staying under a self-set limit = XP; framed only as a reward for staying under, never a penalty for going over |
| Voice task assignment | Quick, accurately-captured voice tasks get a small "efficiency" flourish — positive reinforcement for using the fastest input path |

**Explicitly out of scope:** leaderboards, leagues, or any social/comparative mechanic — those exist in Duolingo specifically because it's a multi-user product, and there's no one here to compare against. See Non-Goals above.

---

## 3. UX / Interaction Design

### 3.1 Core User Flows
-

### 3.2 Screen Inventory
| Screen | Purpose | Entry point |
|---|---|---|
| | | |

### 3.3 Reminder & Notification UX
*(Central problem to solve: you clear notifications/emails/WhatsApp on sight.)*
-

### 3.4 Wireframes / Mockups
- (link or attach)

### 3.5 Accessibility Notes
-

---

## 4. System Architecture

### 4.1 Architecture Style
- [ ] Local-first / offline-only
- [ ] Client-server
- [ ] Hybrid
- Decision + rationale:

### 4.2 High-Level Component Diagram
- (attach diagram)

### 4.3 Data Model / Core Entities
- Task:
- Habit:
- Routine:
- StudySession:
- Meeting:
- ReminderPolicy:
- (add more as needed)

### 4.4 State Management Approach
-

### 4.5 Local Storage / Database Design
-

### 4.6 Background Scheduling & Notification/Alarm Architecture
- Android specifics:
- iOS specifics:
- Escalation strategy beyond a single dismissible notification:

### 4.7 Sync Strategy
- Decision:

### 4.8 Security & Data Privacy
-

### 4.9 Offline Resilience
-

---

## 5. Tech Stack Decisions
*(Log the decision + the "why" for each — not just the choice.)*

### 5.1 Mobile Framework
| Option | Pros | Cons | Fit for solo/free build |
|---|---|---|---|
| Flutter | | | |
| React Native | | | |
| Native (Kotlin/Swift) | | | |

**Decision:**

### 5.2 Language(s)
-

### 5.3 Local Database
- Options considered:
- Decision:

### 5.4 Notification / Alarm APIs
-

### 5.5 State Management Library
-

### 5.6 Testing Frameworks
-

### 5.7 CI/CD (if any)
-

### 5.8 Open-Source Dependency Policy
- Criteria for accepting a dependency (license, maintenance activity, size, audit effort):
- Decision log:

### 5.9 Distribution Method
- [ ] Sideloaded APK / TestFlight ad hoc
- [ ] Play Store internal testing track
- [ ] Other:

---

## 6. AI-Agent-Assisted Build Workflow

### 6.1 Tooling Stack
| Role | Tool/Model | Notes |
|---|---|---|
| Planning / spec authoring | | |
| Architecture | | |
| Implementation | | |
| Review / QA | | |

### 6.2 Agent Role Assignment
- Product/PM agent:
- Architect agent:
- Developer agent:
- QA/Reviewer agent:

### 6.3 Spec-Driven Development Flow (spec-kit)
- [ ] `/specify` — spec written
- [ ] `/plan` — technical plan generated
- [ ] `/tasks` — task breakdown generated
- [ ] `/analyze` — consistency check run
- [ ] `/implement` — implementation executed

### 6.4 BMAD Phase Mapping
- [ ] Analysis
- [ ] Planning
- [ ] Solutioning
- [ ] Implementation

### 6.5 Prompt / Spec Templates
- (link to templates as you create them)

### 6.6 Review & Verification Checkpoints
-

---

## 7. Build Plan / Milestones

- [ ] **M0** — Environment & repo setup
- [ ] **M1** — Core data layer
- [ ] **M2** — Task / habit / routine CRUD
- [ ] **M3** — Reminder & notification engine
- [ ] **M4** — Study session & meeting tracking
- [ ] **M5** — Habit analytics / insights
- [ ] **M6** — Polish & production hardening
- [ ] **M7** — Personal release & install on device

---

## 8. Testing & QA

### 8.1 Unit Test Plan
-

### 8.2 Integration Test Plan
-

### 8.3 Manual QA Checklist
-

### 8.4 Device / OS Coverage
-

---

## 9. Release & Distribution (Free, Personal, No Web)

### 9.1 Build Signing
-

### 9.2 Install Method
-

### 9.3 Update Strategy
-

---

## 10. Post-Launch — Enhancements & Iteration

### 10.1 Backlog
-

### 10.2 Future Insight / Analytics Features
-

### 10.3 Self-Review Cadence
-

---

## 11. Maintenance

### 11.1 Dependency Updates
-

### 11.2 Local Data Backup Strategy
-

### 11.3 Device Migration Plan
-

---

## Appendix A — Tool / Model Decision Log
| Date | Decision | Options considered | Chosen | Why |
|---|---|---|---|---|
| | | | | |

## Appendix B — Glossary
-

## Appendix C — Change Log
| Date | Change |
|---|---|
| | |
