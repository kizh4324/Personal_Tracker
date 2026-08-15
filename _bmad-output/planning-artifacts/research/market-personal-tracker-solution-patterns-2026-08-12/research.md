---
title: 'product research: personal tracker solution patterns'
type: 'market'
topic: 'personal tracker solution patterns'
decision: 'which proven solution patterns to adopt, adapt, combine, or avoid in a production-quality, single-user, Android-only, free, local-first personal app'
source: 'native run'
status: complete
preset: 'deep'
validation: 'normal'
created: '2026-08-12'
updated: '2026-08-14'
---

# product research: personal tracker solution patterns

**Decision this research serves:** Product/problem/solution validation for a production-quality, single-user, **Android-only**, 100% free, local-first personal app for tasks, habits, daily routines, reminders, focus/app-blocking, and voice capture. For each problem in `PROBLEM_AND_SOLUTION_BRIEF.md`, establish whether the problem is real and meaningful, how existing solutions actually work end-to-end (problem → action → intervention → execution → completion → verification → reward → long-term behavior), which mechanisms are proven or dead ends, and what is realistically implementable at production quality on Android. Out of scope: market sizing, GTM, competitive positioning for acquisition, investor framing, monetization.

---

# Executive Summary

This research synthesizes empirical evidence, competitor end-to-end workflows, behavioral science literature, and Android OS technical realities across 8 product dimensions (A through H) for a production-quality, single-user, Android-only, free, local-first personal app covering tasks, habits, routines, study tracking, focus intervention, and voice capture.

### Key Architectural & Design Decisions

1. **First-Class Day-Type Scheduling Engine (Dimension A)**
   - **Adopt / Adapt:** Day-type templates (Weekday vs. Weekend) are first-class schedule engines (proven by TimeTune [34] and Structured [35]). They swap a full day's routine structure in 1 tap and feed the rest of the application (routine steps, study logging, focus sessions, heatmaps).
   - **Avoid:** Multi-tier free-tier routine paywalls (Routinery/RoutineFlow [5,33]), social/comparison leaderboards (YPT pressure [13]), and 10-year unshipped completion-based rescheduling [9,24,25].

2. **Task-Aware Adaptive Intervention Ladder (Dimension B)**
   - **Adopt / Adapt:** Replace static app timers with a task/focus-state aware **Adaptive Intervention Ladder** (`DayType → Routine/Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation → unlock`).
   - **Security Constraint:** Pure-software single-user self-enforcement is always overridable [54]. The app uses deliberate override friction (confirmation + cool-down) rather than pretending to be unbypassable.

3. **Scarcity-Enforced Urgency & In-App Auto-Repeat (Dimension C)**
   - **Adopt / Adapt:** Todoist-style 4-tier priority flags collapse into priority inflation ("everything is P1") [19]. Adopt Things 3's time-bucket placement (Today / Evening) plus a single strictly capped "Urgent" status (max 1–2 items/day). Use Due-style auto-repeat nagging only for Urgent items [r2:10], backed by distinct audio/haptics (12× attendance lift [Chang 2019]).

4. **Non-Coercive Break Prompts & Transparent "Do Tomorrow" Rescheduling (Dimension D)**
   - **Adopt / Adapt:** Pomodoro RCTs (Smits 2025; Biwer 2023; Albulescu 2022) show no objective productivity advantage from hard-enforced breaks, and forced breaks increase fatigue slopes. Use suggested break prompts with 1-tap extensions. For missed tasks, avoid black-box AI auto-rescheduling (Motion opacity churn [d1:15]) and use a transparent, 1-tap "Do Tomorrow" shift confirmation modal.

5. **Frictionless Capture & Earned Streak Slack (Dimension E)**
   - **Adopt / Adapt:** One-line natural-language quick-add (Todoist gold standard [e1:1]) is primary. Voice capture (Ramble model) routes low-confidence inputs to an un-filed Inbox. Avoid hard streak resets ("miss = 0") which trigger the "streak cliff" [Silverman & Barasch 2023]. Provide 1–2 earned monthly streak freezes (Sharif & Shu goal-slack model) and 3-day post-miss recovery.

6. **Non-Punishing Companion Persona & Single Currency (Dimension F)**
   - **Adopt / Adapt:** Center gamification on a supportive companion mascot (Finch pet model [f1:1]) with zero HP loss or character death. Channel all achievements into a single shared currency (Coins) spendable on companion items or custom real-life rewards.

7. **Android Technical Feasibility & Local Architecture (Dimension G)**
   - **Feasibility:** `SCHEDULE_EXACT_ALARM` + `setAlarmClock()` for exact time triggers; `AccessibilityService` + `PACKAGE_USAGE_STATS` overlay redirect for task-aware app blocking; local ML Kit / SpeechRecognizer for voice capture; optional AICore / Gemini Nano for on-device AI. 100% free, zero-network, local-first.

8. **Empirical Problem Validation (Dimension H)**
   - **Validation:** Procrastination (~60%), irregular routine health risks (CVD HR 1.24–1.49), and phone overuse (6.1h/day) are strongly validated problems. Zeigarnik effect (unfinished task anxiety) fails meta-analysis (dz=0.15) and is avoided, while Implementation Intentions (if-then d=0.43) and Ovsiankina resumption prompts are adopted.

---


# Dimension A — Daily routines & study-time tracking (product dimension 1 of 6)

**Questions this dimension answers:** How do existing apps handle day-type-varying routines (Monday ≠ Sunday) and per-subject study-time tracking? Which patterns are proven, mixed, or dead ends — and what should the app adopt, adapt, or avoid?

## A.1 Evidence-backed findings

- **Template-based day scheduling is a proven, shipping pattern** — TimeTune's reusable Templates (groups of time blocks applied whole to any calendar date via a Template calendar; multiple templates per day; one-off per-day overrides that never mutate the template; "save this planned day as a template"; template edits propagate to future applied dates only) [34], and Structured's whole-day copy ("Copy Tasks From Day" / "Paste from _Date_", useful for repeating similar days such as exam study; **iOS-only — not available on Android or Web**) [35]. TimeTune's own staff describe templates as "basically the same as 1-day routines" that can be applied "on any calendar date or following any kind of repetition pattern" [34]. So **day-type scheduling itself is proven**; the limitation is that these mechanisms are *scheduling conveniences only* — the applied template does not feed routine-step completion, subject study logging, habit ticks, focus sessions, completion history, or behavioral feedback. By contrast, the day-*object* model in the tracker category is still per-day-of-week masks: Routinery schedules each routine to specific days/times and prescribes building a weekend routine as a "completely separate structure" [1][2][3]; Streaks and Habitica use per-habit day toggles with no day-type grouping [7][8]. Round-2 verification added: no per-weekday completion histogram or weekday-vs-weekend breakdown exists in Streaks or Habitify [7][21][22]. *Confidence: high for the apps and mechanisms verified; the blanket "no app integrates day-type scheduling with the rest of a tracker" is an unverified negative.*
- **Timed-sequence routines per day-of-week are a proven, strongly validated pattern** for fixed AM/PM routines: Routinery is used with multi-type setups (morning/evening weekdays, Saturday cleaning, Sunday week-prep) [3], has strong positive ADHD-community sentiment [4], and its day-mask model directly handles the "5am commute weekday vs other days" case users describe [6].
- **Free-tier routine caps are a documented blocker for the day-type use case**: Routinery allows 2 routines free, RoutineFlow 1 routine (≤5 steps) free, and multiple reviewers report deleting the app because they need separate multi-type routines without paying [5][33]. *Confidence: high for the caps, medium for pricing specifics (conflicting catalog quotes).*
- **Completion-based rescheduling ("due X days after last completion") has never shipped in a major gamified tracker** — a 10-year story: requested on Habitica in 2016, maintainers confirmed it unimplemented, current docs still say completing a Daily never changes its schedule, and a fresh 2025 request for the same behavior remains unshipped [9][24][25]. What *did* ship (2015) is fixed calendar-period repeats ("Every X days", Monthly) [23]. Treat completion-based rescheduling as a **dead end / never-proven** pattern.
- **Manual backfill of forgotten logging is a shipped, documented feature with genuinely split design sentiment**: Study Bunny officially supports adding/editing logged time in week view ("when you forgot to use the in-game timer") [14][26]; Toggl Track ships "Manual Mode" [27]. But sentiment is split three ways — forbid ("NO CHEATING, NO CATCHING UP", Revel), allow-with-trust (Fabulous), allow-with-rule (habittracker.io: "only cheating if you're marking days you didn't do") [29][30][31]. *Confidence: high that both positions exist.*
- **Per-subject study-time tracking with day/week/month stats is battle-proven**: YPT (Yeolpumta) logs per subject with a color-deepening heat statistic and ~5M users [11][12]; Study Bunny tracks by color-coded subject tags with month+week views [14][15].
- **YPT's social/comparison layer produces documented downsides** — comparison pressure, "YPTunnel vision," dishonest clocking (minutes logged during non-study time), and burnout risk [13]. Any social layer is a **dead end for this app** (also excluded by the non-goals).
- **Flexible weekly targets ("3×/week, any days", completion against scheduled opportunities only) have a clear design rationale** — unscheduled days are not failures — but the evidence is small-vendor design writing, not user validation, and Habitica's decade of "weeklies" requests shows the flexible-scheduling problem is genuinely hard to build [9][10][25]. *Verdict: mixed, low-to-medium confidence.*

## A.2 Observed existing-product behavior (end-to-end workflows)

- **Routinery (timed routine sequence):** build a routine → assign weekday mask + start time → a timed step-by-step sequence runs (navigates user through the day's steps) → completion surfaces as checkmarks → heatmap history. Reported friction (unverified this run, from community lead): a sequence left running carries into the next day and auto-end is missing — *sequence end-state handling is a real UX trap* for timed routines [1][3][4].
- **Streaks (tap-to-complete habit):** pick up to 24 habits → per-habit weekday mask or x-per-week → tap the circle when done → streak + stats. Long-proven (2016 Apple Design Award), but hard 24-habit cap and no day-type grouping [7].
- **Study Bunny (subject-tagged timer):** start a session tagged by subject → grows a pet → color-coded month/week trackers → forgotten sessions backfilled in week view. Dominant 1–3★ pain is the **ad wall** (ads on open/pause/resume/stats; Lifehacker 2.5/5) [14][15][16][26].
- **Toggl repurposed for study ("project = subject"):** manual start/stop (or Manual Mode) → per-project weekly/monthly rollups that demonstrably surface neglected subjects over multi-year personal use [19][20][27].
- **Forest (gamified focus timer):** grow a tree while focused, whitelist allowed apps → subject breakdown via tags is a paid Plus feature and secondary to the blocking timer. A 2025 qualitative thesis (n=5) shows a churn pattern: 2/5 quit after ~3 months, 3/5 retained 3+ years at 1–3 sessions/day [17][18].
- **Habitica (gamified dailies):** day-toggle Dailies inside an RPG; flexible weeklies unsupported 2015–2025 and the penalty model (HP loss) is a poor tonal fit for a non-upsetting app [8][9][24][25].

## A.3 Technical constraints (product-behavioral)

- **Timed routine sequences need reliable mid-day triggers and explicit end-state handling** — the Routinery carry-into-next-day report shows the failure mode (unverified this run); a sequence that is paused, abandoned, or never-started must resolve deterministically [1].
- **Study timers are foreground-bound** in practice; long background duration-tracking reliability is an OS-level question handled in **Dimension G**.
- TimeTune provides the reference implementation for template-based day scheduling (apply day templates per-date, one-off overrides, history preserved); no incumbent integrates that mechanism with routine sequences, subject study, habits, focus sessions, completion history, or behavioral feedback (see A.4).

## A.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **Day-type scheduling integrated with the rest of a tracker, not day-type scheduling itself**: the template/day-copy mechanism is proven (TimeTune templates [34]; Structured whole-day copy, iOS-only [35]), but in every incumbent it is a *scheduling convenience only*. The app's stated "Monday differs from Sunday" pain maps onto integrating the proven template mechanism with routine sequences, subject study logging, habit ticks, focus sessions, completion history, and behavioral feedback — a per-day-type consistency view being part of that. *Confidence: high that the template mechanism is proven and that incumbents stop at scheduling; the integration itself is inference.*
- **Combined day-type routines + subject-level study logging in one app**: the template/day-copy mechanism is proven (TimeTune templates [34]; Structured whole-day copy, iOS-only [35]), and no incumbent examined combines that mechanism with subject-level study logging — an absence found in the apps examined (**unverified negative**, not a confirmed gap). The opportunity is adapting the proven mechanism into a deeper integrated workflow that includes study logging [1][7][14][21][34][35].
- **Backfill with a guardrail** (same-day/lost-log editing allowed, retroactive marking of not-done days as done blocked): resolves the documented Revel-vs-Fabulous split rather than picking a side blindly [29][30][31].
- **Positive-reinforcement-only gamification over routines/study** (no HP loss, no hard resets): Habitica's decade proves the demand exists; its penalty model is what conflicts with the tone requirement — the opportunity is the same reward layer without the punishment (see Dimension F) [8][9][25].

## A.5 Proposed product implications (adopt / adapt / avoid)

- **ADAPT — day-type scheduling as the base mechanism** (proven via TimeTune templates and Structured whole-day copy, plus Routinery/Streaks day-masks), and **promote day-TYPES to first-class objects** — a weekday/weekend/college-day template that swaps a whole day's structure in one action *and feeds the rest of the product*: routine-step completion, subject study logging, habit ticks, focus sessions, completion history, and behavioral feedback, with the heatmap built from completion history, not the template. The template/day-copy mechanism is proven; the integration with the rest of a tracker is the adaptation. *Confidence basis: high for the template mechanism [34][35] and for day masks [1][7]; the integrated day-type layer is an evidence-based opportunity, not incumbent-validated.*
- **ADOPT — per-subject study logging with a color-depth heat visual** (YPT/Study Bunny pattern) and subject-neglect detection from it. *Confidence: high.*
- **ADAPT — backfill with an honesty rule**: allow editing *forgotten* logs (Fabulous / habittracker.io position), disallow marking non-done days as done (Revel's concern), keep it same-day/flagged. *Confidence: high that both positions exist; medium that the middle ground reduces abandonment.*
- **AVOID — free-tier routine caps** (Routinery/RoutineFlow blocker), **social/comparison clocking** (YPT), **ad-walls** (Study Bunny), **paywalled subject tags** (Forest), **penalty gamification** (Habitica) [5][13][15][16][17][33].
- **AVOID as a design hinge — completion-based rescheduling**: a 10-year unshipped request across the major player [9][24][25]. The "sudden 2-day unavailability" edge case is better served by an explicit unavailable-window shift the user confirms once (simpler, user-visible), not by latent completion-based rules.
- **AVOID — any social leaderboard/league layer** (YPT's documented downsides + explicit non-goal) [13].

*Contradictions reported:* backfill forbid-vs-allow is a real split, both sides cited [29][30][31]; flexible weekly targets are rationally argued but unvalidated [10][9][25]. Confidence is per-claim above; anything marked "gap/opportunity" is inference, not validated fact.

---

# Sources (running table)

| # | Supports | Publisher | Pub date | Accessed | Confidence |
|---|---|---|---|---|---|
| [1] | Routinery scheduled-per-day routines, listing | Apple App Store / Routinery — https://apps.apple.com/us/app/routinery-routine-planner/id1450486923 | current | 2026-08-12 | high |
| [2] | Routinery "separate weekend structure" guidance | Routinery blog — https://www.routinery.app/blog/stay-at-home-weekend-getaway | 2026-01 | 2026-08-12 | medium |
| [3] | Routinery founder multi-type routine setup | Ness Labs — https://nesslabs.com/routinery-featured-tool | 2024-12 | 2026-08-12 | medium |
| [4] | Routinery ADHD-community sentiment | Reddit r/adhdwomen — https://www.reddit.com/r/adhdwomen/comments/shx88m/ and /1hcazg7/ | 2022-02, 2024-12 | 2026-08-12 | high |
| [5] | Routinery/RoutineFlow free-tier caps | AppBrain + AppRecs — https://www.appbrain.com/app/routineflow-routine-for-adhd/app.routineflow.routineflow | unknown | 2026-08-12 | high |
| [6] | "Different routines for different days" user ask | Reddit r/AuDHDWomen — https://www.reddit.com/r/AuDHDWomen/comments/1bwx920/ | 2024-04 | 2026-08-12 | high |
| [7] | Streaks per-habit weekday masks, stats | Crunchy Bagel — https://streaksapp.com/ and https://crunchybagel.com/now-available-streaks-10/ | 2024 / 2024-09 | 2026-08-12 | high |
| [8] | Habitica day-toggle Dailies, no day-type grouping | Habitica Wiki — https://habitica.fandom.com/wiki/Task_Type_Choice:_Habit,_Daily,_or_To_Do | unknown | 2026-08-12 | high |
| [9] | Habitica completion-based rescheduling never shipped (2016) | GitHub HabitRPG/habitica#7824 — https://github.com/HabitRPG/habitica/issues/7824 | 2016-07 | 2026-08-12 | high (historical) |
| [10] | Flexible weekly targets / scheduled-opportunities rationale | init.Habits + DropDrop — https://inithabits.com/blog/weekly-habit-tracker and https://www.dropdrophabit.com/blog/weekly-habit-tracker | 2026-06/07 | 2026-08-12 | medium |
| [11] | YPT per-subject stats, ~5M users | Google Play / PYStudio — https://play.google.com/store/apps/details?id=com.pallo.passiontimerscoped | current | 2026-08-12 | high |
| [12] | YPT per-subject day/week/year statistics | Mahidol Univ. journal — https://shee.si.mahidol.ac.th/knowledge/index.php/en/journals-en/issue2-2026/13-2-2026 | 2026 | 2026-08-12 | high |
| [13] | YPT social layer downsides (comparison pressure, dishonest clocking) | Raffles Press — https://rafflespress.com/2025/02/13/yawns-pains-and-tears-on-ypt/ | 2025-02 | 2026-08-12 | medium |
| [14] | Study Bunny week-view backfill, subject tags | SuperByte official tutorial — https://superbyte.site/tutorial | unknown | 2026-08-12 | high |
| [15] | Study Bunny scale + ad complaints | Google Play — https://play.google.com/store/apps/details?id=com.superbyte.studybunny | current | 2026-08-12 | high |
| [16] | Study Bunny "cute timer but nothing else" | Lifehacker — https://lifehacker.com/tech/study-bunny-productivity-app-review | 2025-04 | 2026-08-12 | high |
| [17] | Forest tag-based subject breakdown, Plus paywall | Seekrtech — https://forestapp.cc/ and https://apps.apple.com/us/app/forest-focus-for-productivity/id866450515 | current | 2026-08-12 | high |
| [18] | Forest long-term churn pattern (n=5) | DiVA thesis — https://www.diva-portal.org/smash/get/diva2:1882077/FULLTEXT01.pdf | 2025 | 2026-08-12 | medium |
| [19] | "Project = subject" study repurposing, multi-year | Adi Muthukumar substack — https://adimuthukumar.substack.com/p/tracking-your-trail-to-see-how-far | 2024-06 | 2026-08-12 | high |
| [20] | Toggl weekly per-project rollups | Toggl Community + API docs — https://community.toggl.com/t/weekly-report/57 and https://engineering.toggl.com/docs/reports/weekly_reports/ | 2024-11 | 2026-08-12 | high |
| [21] | Habitify stats: no weekday-vs-weekend dimension | Habitify — https://habitify.me/blog/let-data-tell-your-story | ~2020-08 | 2026-08-12 | medium |
| [22] | Habitify API v2: no day-type dimension | Habitify — https://api-docs.habitify.me/api | current (v2.0.0) | 2026-08-12 | medium |
| [23] | Habitica fixed-interval Dailies shipped (calendar-period only) | Habitica Weekly Status Report — https://blog.habitrpg.com/post/121616648391/ | 2015-06-15 | 2026-08-12 | high (historical) |
| [24] | Habitica current docs: completion never changes schedule | Habitica Wiki "Dailies" — https://habitica.fandom.com/wiki/Dailies | current | 2026-08-12 | high |
| [25] | Habitica flexible weeklies still unshipped (2025) | GitHub HabitRPG/habitica#15469 — https://github.com/HabitRPG/habitica/issues/15469 | 2025-07 | 2026-08-12 | high |
| [26] | Study Bunny week-view backfill user corroboration | The Student Room — https://www.thestudentroom.co.uk/showthread.php?t=7569133 | 2025-02 | 2026-08-12 | medium |
| [27] | Toggl Track Manual Mode | Toggl support — https://support.toggl.com/en/articles/2527693-manual-mode | ~2026 | 2026-08-12 | high |
| [28] | YPT retroactive editing unverified | Reddit r/yeolpumta | undated | 2026-08-12 | low |
| [29] | Backfill forbidden (Revel) | Revel App Store listing | 2026-era | 2026-08-12 | high (position exists) |
| [30] | Backfill allowed-with-trust (Fabulous) | thefabulous.co tracker page | 2026-era | 2026-08-12 | high (position exists) |
| [31] | Backfill allowed-with-rule (habittracker.io) | habittracker.io FAQ | 2026-era | 2026-08-12 | high (position exists) |
| [32] | RoutineFlow weekly planner, per-day assignment | Google Play / System Two GmbH — https://play.google.com/store/apps/details?id=app.routineflow.routineflow | 2026-04 | 2026-08-12 | medium |
| [33] | RoutineFlow free tier = 1 routine | chrome-stats — https://chrome-stats.com/d/app.routineflow.routineflow | 2024–2026 | 2026-08-12 | high (free-tier limit) |
| [34] | TimeTune template-based day scheduling (templates as reusable block-groups, apply per-date, multiple/day, one-off overrides, history preserved) | TimeTune official help — https://timetune.help/en/basic-guide/ and https://timetune.app/templates/ + Google Play — https://play.google.com/store/apps/details?id=com.gmail.jmartindev.timetune | current | 2026-08-12 | high |
| [35] | Structured whole-day copy (Copy Tasks From Day / Paste from Date; iOS-only, not Android/Web) | Unorderly GmbH help — https://help.structured.app/en/articles/1901058 and App Store — https://apps.apple.com/us/app/structured-daily-planner-todo/id1499198946 | current | 2026-08-12 | high |
| [36] | one sec intentional-delay field experiment (N=280, 6wk: 36% dismissed, opens −57%) + online experiment (N=500: continue/dismiss = active ingredient, delay some, message none) | PNAS — https://www.pnas.org/doi/abs/10.1073/pnas.2213114120 | 2023-02 | 2026-08-12 | high |
| [37] | one sec pause decays after first weeks (tap-through); attempt counter more behavior-changing than breathing exercise; friction-only overlay (disable automation/permission, nothing in browsers) | WhistleOut — https://www.whistleout.com/CellPhones/Guides/one-sec-app-review and browwwser — https://www.browwwser.com/resources/one-sec-app-review-2026/ | 2026-06 / 2026-04 | 2026-08-12 | high |
| [38] | Opal escalation ladder (Normal → Timeout → Deep Focus); Deep Focus "genuinely hard to bypass" but still bypassable via Settings/Screen Time/VPN | ScreenBuddy — https://www.screenbuddyapp.com/blog/opal-app-review and MakeUseOf — https://www.makeuseof.com/opal-screen-time-limiting-app-helps-use-phone-less/ | 2026-02 / 2024-11 | 2026-08-12 | medium-high |
| [39] | Opal self-defeat loop (edit/reset limit during 1-minute warning; "Unblock all" = ultimate bypass; warning-banner fix later shipped) | Opal Community Forum — https://community.opalapp.com/t/disable-ability-to-edit-app-limits-within-1-min-of-reaching-limit/1750 and /t/option-to-remove-the-unblock-all-command-on-all-levels-of-block/5348 | 2023-08 / 2024-09 | 2026-08-12 | high |
| [40] | Screen Time "Ignore Limit / One More Minute / Remind Me in 15 min" on every expiry; option cannot be disabled | Mac Observer — https://www.macobserver.com/tips/how-to/disable-one-more-minute-screen-time-limit-on-iphone/ and Apple Support Community — https://discussions.apple.com/thread/253076978 | 2024-08 / unknown | 2026-08-12 | high |
| [41] | Screen Time silent-failure history (Safari URL-bar bypass reported 2021, ignored 3 years; unenforced limits, blank charts) | MacRumors — https://www.macrumors.com/2024/06/05/apple-parental-control-bug-fix/ and The Verge — https://www.theverge.com/2024/6/5/24172008/apple-fixing-screen-time-bug-x-rated-sites-parental-controls | 2024-06 | 2026-08-12 | high |
| [42] | Digital Wellbeing app timers: "just tedious enough" (2018) → muscle-memory two-tap bypass, "illusion of control" (2025) | Android Police — https://www.androidpolice.com/2018/08/07/hands-android-9-pies-digital-wellbeing-phone-control-without-self-control/ and How-To Geek — https://www.howtogeek.com/how-androids-digital-wellbeing-can-actually-make-your-screen-time-worse/ | 2018-08 / 2025-09 | 2026-08-12 | high |
| [43] | Google Pause Point announced (10-sec pause, substitute suggestions, restart-required opt-out) | Google blog — https://blog.google/products-and-platforms/platforms/android/pause-point/ and 9to5Google — https://9to5google.com/2026/05/12/android-digital-wellbeing-pause-point-upgrade/ | 2026-05-12 | 2026-08-12 | high |
| [44] | Pause Point NOT shipped as of mid-2026 (absent from Android 17 stable + June Pixel Feature Drop) | Android Authority — https://androidauthority.com/how-to-replicate-android-17-pause-point-right-now-3687961/ and TechCabal — https://techcabal.com/2026/08/07/how-pause-point-on-android-17-works-and-why-it-matters/ and Android Headlines — https://androidheadlines.com/2026/06/16/ | 2026-07 / 2026-08 / 2026-06 | 2026-08-12 | high |
| [45] | Forest "tree dies" sustained positive user evidence (4.8★, years-long use, ADHD recommendations) | App Store reviews — https://apps.apple.com/us/app/forest-focus-for-productivity/id866450515?see-all=reviews and Mashable — https://mashable.com/article/forest-app-productivity-focus-review | 2022-11 / 2019-05 | 2026-08-12 | high |
| [46] | Forest commitment is self-initiated (tree exists only after "plant"); Deep Focus = escalation step blocking non-allowlisted apps | Forest official — https://forestapp.cc/ and Mashable — https://mashable.com/article/forest-app-productivity-focus-review | 2026 / 2019-05 | 2026-08-12 | medium |
| [47] | Freedom Locked Mode positive "really feel locked in" reviews; bypass via permission-disable, Wi-Fi off, uninstall | App Store — https://apps.apple.com/us/app/freedom-screen-time-control/id1269788228?see-all=reviews and Google Play — https://play.google.com/store/apps/details?id=to.freedom.android2 and MakerStack — https://makerstack.co/reviews/freedom-review/ | 2026-03 / 2026-08 / 2026-07 | 2026-08-12 | medium-high |
| [48] | iOS Screen Distance: full-screen block until device >12in from eyes; default-on for under-13s | Apple Support — https://support.apple.com/en-us/105007 and MacRumors — https://www.macrumors.com/2023/06/15/ios-17-screen-distance-feature/ | 2023-06 | 2026-08-12 | high |
| [49] | Screen Distance behavior change anecdotal (posture changed where reminders didn't); AAO: myopia link unproven, feature harmless | Yahoo News Canada (HuffPost) — https://ca.news.yahoo.com/underrated-iphone-feature-could-much-110018213.html and AAO — https://www.aao.org/eye-health/tips-prevention/apple-time-daylight-screen-distance-myopia-strain | 2025-10 / 2024-01 | 2026-08-12 | medium |
| [50] | YouTube/TikTok take-a-break + bedtime reminders: opt-in (adults), full-screen on-by-default (under-18s), one-tap dismissible; experts call them "cosmetic," "small proportion," "no consequence" | Fast Company — https://www.fastcompany.com/91291280/youtube-is-doubling-down-on-bedtime-reminders-do-they-work | 2025-03 | 2026-08-12 | high |
| [51] | 2026 field study (N=104, within-subject): gradual visual/haptic escalation sustains acceptance longest; explicit pop-up most effective for high-impulsivity users; trade-off between behavior change and acceptance | arXiv preprint — https://arxiv.org/html/2607.15818v1 | 2026 | 2026-08-12 | medium-high (preprint, not peer-reviewed) |
| [52] | iOS 26: 0-minute per-app limit (true full block) confirmed; "One More Minute" NOT removed, still present + buggy (One Minute spills across categories) | Tech Lockdown — https://techlockdown.com/articles/ios-26-screen-time-changes and Timing App — https://timingapp.com/blog/screen-time-on-iphone-and-ipad/ and Mac Observer — https://macobserver.com/tips/how-to/ios-screen-time-issue-one-minute-setting-affects-all-apps-on-iphone/ | 2026-04 / 2026-04 / 2025-11 | 2026-08-12 | high |
| [53] | iOS 26.4: Screen Time PIN (not device passcode) required to revoke third-party Screen Time permission | Tech Lockdown — https://techlockdown.com/articles/ios-26-screen-time-changes | 2026-04-20 | 2026-08-12 | medium (single secondary) |
| [54] | Apple primary: Screen Time limits ignore-able by default; "Block at End of Limit" only with passcode; passcode-holder can still override | Apple support guide — https://support.apple.com/guide/iphone/set-schedules-with-screen-time-iphb0c7313c9/ios | current (iOS 26 guide) | 2026-08-12 | high |
| [55] | kSafe-style timer lockboxes override-proof by design (no reset/code/backdoor); independent evidence experience-based, not experimental (Wirecutter: fails without intention-setting work first; BI 2-year positive) | Lockbox Timer — https://lockboxtimer.com/ksafe-lock-box-review/ and NYT Wirecutter — https://nytimes.com/wirecutter/reviews/break-up-with-your-phone/ and Business Insider — https://businessinsider.com/guides/home/ksafe-review | 2025-08 / 2024-02 / 2023-12 | 2026-08-12 | high (mechanics) / medium (outcomes) |
| [56] | NFC blockers (Blok/Brick/Bloom/Unpluq) not override-proof — all ship built-in paid/limited emergency exits; Android blocking inconsistent | Accountable AI — https://accountableai.xyz/blog/best-nfc-phone-blockers-2026 | 2026-01-19 | 2026-08-12 | medium |
| [57] | Proactive free-time suggestion unproven: only research prototypes (Borapp Buzz, Smart Time, Active-Hour) and pre-launch startups (Noll, Sukima, Rimuse, Otium); no user evidence | Pielot — https://pielot.org/borappbuzz/ and CEUR-WS — https://ceur-ws.org/Vol-2500/paper_21.pdf and Noll — https://www.nollapp.com/ | various to 2026 | 2026-08-12 | high (unproven) |

---

# Dimension A — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension A. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement. Gamification items are drawn from Dimension F digests and are marked **pending** until Dimension F is synthesized into `research.md`.*

1. **Day type is a core concept, not a label.** Template-based day scheduling is a **proven pattern** (TimeTune reusable day templates applied per-date with one-off overrides [34]; Structured whole-day copy, iOS-only [35]; day-mask base from Routinery/Streaks [1][7]). We **adapt** it into a deeper integrated workflow: a weekday/weekend/college-day template that swaps a whole day's structure in one action **and** feeds routine sequences, subject study logging, habit ticks, focus sessions, completion history, and behavioral feedback. No incumbent examined in this research was found to integrate day-type scheduling with the full routine + study + habit + focus + history workflow. *Status: day-type scheduling = validated pattern (high confidence); the integration layer = evidence-based opportunity (medium-high), needs design validation.*
2. **Routines are linked sequences with explicit lifecycle states.** Timed sequential routines are a proven pattern [3][4]; end-state handling is the known failure mode (community-reported, unverified this run) [1]. Sequences must resolve deterministically (not-started / running / paused / abandoned / completed) at day's end. *Status: validated mechanism (medium confidence); state-machine design needs validation.*
3. **Reminders are a delivery attribute, not the core engine.** Reminder-driven behavior builds over-reliance and decays (cross-dimension, H — see H section when synthesized) [H:29,30]; escalation is reserved for urgent items; routine steps surface contextually. *Status: evidence-based (medium-high); escalation thresholds need validation.*
4. **Study is subject-based.** Subject-tagged sessions with day/week/month stats, heat visual, and neglect detection; resolves the §2.7 open question in favor of per-subject breakdown. *Status: validated requirement (high confidence).* Planned-vs-logged reconciliation is a separate evidence-based opportunity (medium confidence, needs validation).
5. **Backfill with an honesty rule.** Forgotten logging is editable and visibly flagged; inventing a completed day is not. *Status: mechanism validated (high confidence); the specific flagged rule needs validation.*
6. **Gamification = companion persona + single shared currency + streak grace/slack, no penalties, no social, no guilt-voice.** [**PENDING — Dimension F not yet synthesized into `research.md`; findings below are digest-level.**] Companion persona (Finch D1/D7 54/37) and single-currency economies are the proven patterns [F r1:1,13,14]; slack/grace is experimentally proven (Sharif & Shu +20% steps / +40% goal-days) [F r2:11,12]; Habitica-style penalties and YPT-style social comparison have documented harm [F r1:23][A:13]; stake-scaled reward magnitude is **unvalidated** (no positive evidence) [F r1:29]; guilt-based notification voice is **mixed-to-unvalidated** (lift from template selection, not voice; documented anxiety) [F r2:8,10]. *Status: ADOPT/AVOID items validated at digest level; confirm when F is synthesized.*
7. **Scheduling: fixed calendar repetition + day-type schedules core; completion-based rescheduling avoided; unavailable-window shift adopted as a user-confirmed, explicit mechanism.** Fixed repetition is the only shipped variant [23][24]; completion-based rescheduling is a 10-year dead end [9][24][25]. *Status: validated (high) for fixed repetition; the unavailable-window shift is a proposed design (medium), needs validation.*
8. **No monetization layers.** No free-tier caps, ads, or paywalled features (documented abandonment drivers [5][15][17][33]); alignment with the locked constraints is a stated advantage. *Status: validated (high).*
9. **Core abstraction: Day Type → Routine → Routine Step → Subject → Study Session → Habit → Task → Completion → History**, with ReminderPolicy as attached configuration on steps/tasks/urgent items. *Status: each piece is evidence-grounded (medium-high); the integrated model needs design validation.*

**Open decisions carried forward (need validation):** day-type count/definitions and today-mapping UX (auto vs manual); routine end-of-day auto-resolution rules; escalation thresholds for urgent vs normal; flagged-backfill grace window; whether a Forest-style light stake is included (tone fit with the non-upsetting requirement — closest to the line); stake-scaled rewards (kept as unvalidated hypothesis, not a requirement); flexible weekly targets (deferred); planned-vs-logged study reconciliation UX.

*Corrections applied:* the earlier "explicit day-type template system is an unoccupied market gap" claim was overturned after user correction + verification — TimeTune ships template-based day scheduling [34] and Structured ships whole-day copy (iOS-only) [35]; the surviving claim is that no incumbent integrates day-type scheduling with the rest of a tracker.

---

# Dimension B — In-the-moment interruption & focus (product dimension 2 of 6)

**Questions this dimension answers:** How do existing products intervene at the moment of distraction — before, during, and at the limit of app use? Which interruption patterns are proven, mixed, or dead ends — and what should the app adopt, adapt, or avoid? (Android feasibility of blocking, usage-access, and focus mechanisms is handled in **Dimension G**.)

## B.1 Evidence-backed findings

- **The intentional-delay wrapper (pause-before-open) is strongly supported by field experiment, but long-term decay evidence is reviewer-observed.** one sec's peer-reviewed field experiment (N=280, 6 weeks) showed the interruption dismissing 36% of target-app open attempts, with attempted opens down 37% and actual opens down 57%; a follow-up online experiment (N=500) isolated the explicit "continue or dismiss" choice as the active ingredient, the time delay adding some effect and the deliberation message none [36]. Independent 2026 hands-on reviews report the pause loses power over time as users tap through on autopilot — effective for mild habit scrolling, but insufficient alone for deliberate/compulsive use [37]. *Confidence: high for the core mechanism; medium for long-term decay (reviewer observations rather than peer-reviewed experiment).*
- **Self-set app timers enforced at the limit moment are routinely bypassed.** Apple Screen Time offers "Ignore Limit / One More Minute / Remind Me in 15 min" on every expiry, the option cannot be disabled, and "Block at End of Limit" only appears once a passcode is set — which an adult setting limits on themselves can still override [40][54]. Android Digital Wellbeing app timers pause the app for the day, but removal requires digging into Settings; reviewers moved from calling it "just tedious enough" (2018) to a two-tap muscle-memory bypass producing an "illusion of control" (2025) [42]. Screen Time also has a documented silent-failure history (a WSJ investigation publicized a Safari URL-bar bypass reported to Apple in 2021 and ignored for three years) [41]. *Confidence: high.*
- **An adaptive intervention/escalation ladder is one of the best-supported patterns among the approaches reviewed — but never truly unbypassable.** Opal's Normal → Timeout → Deep Focus, Freedom's Locked Mode, and Forest's Deep Focus are repeatedly described as "genuinely hard to bypass," and the self-defeat loop — editing the limit during the "app will be blocked in 1 minute" warning — drops away once the tier blocks in-session editing [38][39][47]. Structural escapes remain: Settings, VPN profile, permission revocation, uninstall, Wi-Fi off [38][47]. A 2026 field study (N=104, within-subject) found gradual visual/haptic escalation sustained user acceptance longest, while explicit pop-ups were most objectively effective for high-impulsivity users [51]. The system should remain gentle when appropriate and escalate only after user behavior demonstrates that stronger intervention is needed. *Confidence: medium-high (evidence base mixed between product reviews and a single 2026 preprint).*
- **Deliberate opt-out friction is the OS-level design direction.** Google announced Pause Point in May 2026 (inserting a 10-second pause before opening distracting apps with substitute options and restart-required opt-out) [43]; however, it was not available in the verified Android 17/build/device checks conducted during this research round [44]. It remains an evidence-backed design direction with no published user-outcome evidence, not a validated product requirement. On iOS 26, Apple added a 0-minute per-app full block, and iOS 26.4 requires the Screen Time PIN to revoke permission [52][53]. (iOS mechanisms set market expectations but Android-specific feasibility is handled in Dimension G.) *Confidence: high on design facts; zero published user-outcome evidence for Pause Point.*
- **One-tap dismissible well-being nags are cosmetic.** YouTube/TikTok "take a break" and "bedtime" reminders are opt-in for adults, full-screen and on-by-default for under-18s, and dismissible in one tap; a Rutgers behavioral-science professor calls them "cosmetic," likely working "for a small proportion," with "no consequence" for ignoring [50]. *Confidence: high.*
- **Forced-physical-condition interruption is implementable but weak on durable-behavior evidence.** iOS Screen Distance blocks use with a full-screen alert until the device is moved more than 12 inches from the eyes (default-on for under-13s) [48]; one strong personal account reports it changed posture where gentler reminders had not, while an AAO expert notes the myopia link is unproven but the feature is harmless [49]. *Confidence: high that it ships; medium on behavior change.*
- **Gamified consequence (Forest's dying tree) is proven, but only as a self-initiated commitment device.** Sustained positive user evidence (4.8★ reviews, years-long use, ADHD recommendations) supports the tree-dies mechanic [45]; however the tree only exists after the user presses "plant," so the pattern does not fire at the moment free time opens — it solves "staying on task," not "what do I do right now" [46]. *Confidence: high.*
- **Hardware locks split: timer lockboxes are genuinely override-proof; NFC tags are not.** kSafe-style lockboxes have no reset, code, or backdoor [55]; but independent press evidence is experience-based, not experimental (Wirecutter: lockboxes fail "unless you have done the intention-setting work first") [55]. NFC blockers (Blok/Brick/Bloom/Unpluq) all ship built-in emergency exits (paid or limited) and have inconsistent Android blocking [56]. *Confidence: high on mechanics; medium on outcomes.*
- **Ambient proactive free-time detection/suggestion remains deferred/unvalidated.** Only research prototypes (Borapp Buzz, Smart Time, Active-Hour) and pre-launch startups (Noll, Sukima, Rimuse, Otium) exist, none with published user evidence; Google's Pause Point is the first platform-native design, announced May 2026, but was unavailable in verified checks and lacks user-outcome data [57][43][44]. *Confidence: high that it is unproven; status: deferred/unvalidated.*

## B.2 Observed existing-product behavior (end-to-end workflows)

- **one sec (intentional-delay overlay):** attempt to open a target app → breathing pause + "continue or dismiss" → 36% dismissed at the moment, opens −57% over six weeks [36]; daily attempt counter ("you've tried to open YouTube 47 times today") reported as more behavior-changing than the breathing exercise [37]; friction-only — disabling the automation/permission removes it, does nothing inside browsers [37].
- **Opal (escalation ladder):** Normal (cancel anytime) → Timeout (increasing delay) → Deep Focus (cannot end session early, premium). Self-defeat loop: when "app will be blocked in 1 minute" appears, users open Settings and edit/reset the limit; "Unblock all" called "the ultimate bypass"; vendor later shipped a fix suppressing the warning banner [38][39].
- **Apple Screen Time (self-set per-app limits):** expiry → One More Minute / Ignore Limit escape; passcode + Block at End of Limit for stricter enforcement; adults enforcing on themselves can still override [40][54]; documented silent failures (Safari bypass) [41]; iOS 26 adds a 0-minute full block, but "One More Minute" still exists and is currently buggy (spilling the minute across all app categories) [52].
- **Android Digital Wellbeing (app timers):** limit reached → app paused for the day → Settings to disable; "just tedious enough" friction in 2018, two-tap muscle-memory bypass by 2025 [42].
- **Forest (self-initiated focus timer):** press "plant" → tree grows while in-app → leave = tree dies; Deep Focus blocks non-allowlisted apps until "Give Up" [45][46].
- **Freedom Locked Mode:** cannot end session early → positive "really feel locked in" reviews; bypass by disabling app permissions, turning off Wi-Fi, or uninstalling [47].
- **iOS Screen Distance (physical-condition block):** full-screen alert until device >12in from eyes [48].
- **YouTube/TikTok (well-being nags):** full-screen break reminder, one-tap dismissible, on-by-default for under-18s [50].
- **kSafe (hardware lockbox):** phone in box → timer set → no escape; override-proof by design [55].

## B.3 Technical constraints (product-behavioral)

- **Product/Security Constraint — Single-user self-enforcement is always overridable**: the enforcing agent and the tempted agent are the same person on their own Android device (Apple's docs similarly state limits are ignore-able by default [54]). The app must not promise unbypassable self-enforcement; it should enforce deliberate override friction, confirmation/cool-down, and optional attempt transparency.
- **Structural escape paths exist for every pure-software blocker**: Settings overrides, permission revocation, VPN profile, Wi-Fi off, uninstall [38][47][42].
- **Android-specific enforcement capability** (usage access, FSI/blocking overlays, accessibility, foreground services, Doze) is a **Dimension G** scope item, not repeated here; iOS findings (0-minute limit, Screen Time PIN) validate market patterns but are not Android specifications.
- **Intervention design must account for behavior-response adaptation**: gradual escalation buys acceptance while explicit pop-ups buy objective effectiveness [51]; the system should adapt based on repeated bypasses rather than static user profiling.
- **Reference implementations are unstable or unshipped**: Pause Point unavailable in verified checks [44]; iOS 26's One Minute extension is buggy [52]; the delay-wrapper decays into tap-through [37].

## B.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **Cross-Dimension Integration (Dimension A + Dimension B)**:
  `DayType → Routine/RoutineStep or Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation → task/session completion → unlock → completion history`
  *Description:* Dimension A defines what the user is supposed to do and when, while Dimension B defines what happens when the user attempts to enter a distracting app during that task/focus state. The app-blocking system must therefore be task/focus-state aware rather than an isolated standalone blocker. *Status: evidence-based product opportunity / integrated workflow (needs design validation).*
- **Honest override-with-friction as a security/product stance**: instead of pretending to be unbypassable (impossible on the user's own device), make overrides deliberate and costly — confirm + cool-down, count overrides, show the user their own attempts. *Confidence: high that unbypassability is impossible; friction design is an evidence-based constraint.*
- **In-the-moment substitute activity** at the block moment (Pause Point / one sec direction): the "what do I do right now" gap that self-initiated Forest doesn't cover. *Confidence: evidence-based opportunity (unproven — Pause Point announced May 2026, unavailable in verified checks, no user-outcome data).*
- **Behavior-response adaptive intervention (formerly trait-modulated escalation)**: rather than assuming the app can or should classify a user as high-impulsivity, adopt behavior-response adaptation (gentle intervention → repeated bypass → stronger intervention). *Status: preliminary hypothesis requiring validation (single preprint [51]).*
- **Override transparency / attempt counting**: surface user bypass attempts as a behavior signal. *Confidence: low-to-medium product hypothesis (cited evidence limited to single reviewer report).*

## B.5 Proposed product implications (adopt / adapt / avoid)

- **ADOPT — adaptive intervention ladder as the interruption backbone**: gentle intervention → warning → full block at the limit, with deliberate opt-out friction (Opal/Freedom/Forest Deep Focus pattern; Pause Point restart-required direction) [38][47][43]. Escalates only after user behavior demonstrates that stronger intervention is needed.
- **ADOPT — honest override contract (Product/Security Constraint)**: never claim a block is unbypassable; make overrides deliberate (confirmation + cool-down) and optionally transparent [54].
- **ADAPT — cross-dimension integration between Dimension A routines/tasks and Dimension B interruption**: link focus state and routine execution directly to intervention policy (`DayType → Routine/Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation`). Blocker is task-state aware.
- **ADAPT — in-the-moment substitute activity** at block fire (Pause Point / one sec mechanic), user-curated; Android feasibility in Dimension G [43][57].
- **ADAPT — intentional-delay wrapper** for mild distraction, aware of long-term reviewer-observed decay (medium confidence); pair with adaptive escalation rather than relying on it alone [36][37].
- **ADAPT — behavior-response adaptive intervention**: gentle default; escalate upon repeated bypass rather than static impulsivity profiling [51].
- **AVOID — one-tap dismissible well-being nags** (cosmetic, no consequence) [50].
- **AVOID — ambient proactive free-time detection/suggestion** (deferred/unvalidated; research prototypes + unshipped platform attempt only) [57].
- **AVOID as a promise — unbypassable pure-software blocking** (every shipped blocker has a documented bypass; over-promising invites the self-defeat loop) [38][41][47].
- **AVOID (defer) — hardware pairing** (kSafe lockbox is outside an app and experience-only evidence; NFC tags are not override-proof) [55][56].

*Contradictions reported:* intentional-delay proven-but-decays (reviewer-observed, medium confidence) vs adaptive intervention ladder proven-and-sticky (resolved: wrapper = first line, adaptive ladder = enforcement); gradual escalation (acceptance) vs explicit pop-up (effectiveness) is behavior-response dependent, not contradictory [51]. Confidence is per-claim above; anything marked "gap/opportunity" is inference, not validated fact.

---

# Dimension B — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension B. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement. Android-feasibility questions are delegated to Dimension G (not yet synthesized into `research.md`) and marked **pending** accordingly.*

1. **Adaptive intervention ladder is the interruption backbone, not static reminders or nags.** Gentle intervention → warning → full block at the limit with deliberate opt-out friction (confirm + cool-down); pattern supported by product evidence [38][47] and OS design directions [43]. System remains gentle when appropriate and escalates only after user behavior demonstrates that stronger intervention is needed. *Status: evidence-based opportunity (medium-high confidence; mixed evidence base between product reviews and a single 2026 preprint); needs validation.*
2. **Honest override contract (Product/Security Constraint).** Pure-software self-enforcement on the user's own Android device is always overridable (Apple's own docs similarly state limits are ignore-able [54]); the app must not promise unbypassable self-enforcement and should make overrides deliberate (confirm + cool-down) and optionally transparent. *Status: validated constraint (high confidence) + design decision.*
3. **Explicit Cross-Dimension Integration (Dimension A + Dimension B workflow).** `DayType → Routine/RoutineStep or Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation → task/session completion → unlock → completion history`. Dimension A defines what the user is supposed to do and when, while Dimension B defines what happens when the user attempts to enter a distracting app during that task/focus state. The app-blocking system must therefore be task/focus-state aware rather than an isolated standalone blocker. *Status: evidence-based opportunity / integrated workflow (medium confidence); needs validation.*
4. **In-the-moment substitute activity.** When a block fires, offer a productive alternative ("what do I do right now"). *Status: evidence-based opportunity (unproven — Pause Point announced May 2026, unavailable in verified checks, no published user-outcome data [43][44]); needs validation + Android feasibility (G, pending).*
5. **Behavior-response adaptive intervention.** Gentle visual/haptic escalation by default; escalate upon repeated bypass rather than assuming static user impulsivity classification. *Status: preliminary hypothesis (medium-high preprint [51]); needs validation.*
6. **Override transparency / attempt counting.** Surface the user's own bypass attempts/counters as a behavior signal. *Status: preliminary hypothesis (low-to-medium confidence, limited evidence); needs validation.*
7. **Gamified commitment (Forest-style tree/companion) is self-initiated only** — it solves staying-on-task, not in-the-moment interruption; keep it optional and layered under the adaptive intervention ladder. *Status: validated mechanism for self-initiated focus (high confidence); placement under adaptive intervention ladder needs validation.*
8. **Ambient proactive free-time detection/suggestion.** *Status: deferred / unvalidated (no published user-outcome evidence).*
9. **Hardware pairing.** kSafe lockbox is external and experience-only evidence; NFC tags are not override-proof. *Status: deferred / out of scope.*

**Open decisions carried forward (need validation):** adaptive intervention step count and threshold timing; opt-out cool-down length and confirmation UX; whether overrides are counted/displayed; substitute-activity source (user-curated vs curated); exact trigger binding for task/focus-state aware blocking (Dimension A integration); how to counter delay-wrapper decay (frequency caps, adaptive delays).

---

# Dimension C — Urgency, notification delivery & attention (product dimension 3 of 6)

**Questions this dimension answers:** How do existing apps deliver urgent, time-sensitive reminders without triggering alert fatigue or total notification dismissal? Which delivery channels and urgency mechanisms are proven, mixed, or dead ends on Android (and iOS market references) — and what should the app adopt, adapt, or avoid? (Android technical feasibility of notification permissions, channels, exact alarms, FSI limits, and Doze is handled in **Dimension G**.)

## C.1 Evidence-backed findings

- **Ordinary third-party apps face strict OS entitlement & permission boundaries for urgent delivery.** iOS Critical Alerts (`com.apple.developer.usernotifications.critical-alerts`) require manual Apple approval reserved for health/safety apps (requests take weeks, non-health apps rejected) [10][11]; Full-Screen Intent (FSI) on Android 14+ is auto-granted only to calling/alarm apps and Play Store revokes FSI for productivity apps [16][17][g:10,11]. Third-party apps cannot force OS-level un-silenceable alarms or full-screen takeovers without qualifying as an alarm clock app. *Confidence: high (validated technical constraint).*
- **iOS 26.2 Reminders introduced first-party urgent alarms + non-dismissable Live Activity (iOS market reference only).** Marking a reminder "Urgent" in iOS 26.2+ schedules a system alarm using a first-party "Alarms" permission, rendering a full-screen alarm screen with Snooze/Complete and a non-dismissable Live Activity [r2:1,2,3,5]. This is a first-party Apple path (not a public third-party API) and serves as an iOS market reference and OS design signal only, not an Android implementation requirement (Android OS feasibility is delegated to **Dimension G**) [r2:5,29][g:8,10]. *Confidence: high on design facts.*
- **Due-style acknowledgement-based urgent reminder escalation is a validated market pattern.** Due (4.7★, ~2.8K ratings) re-fires reminders at short intervals until acknowledged [r2:10,11]. r/ADHD users report acknowledgement-based escalation as a validated market pattern ("the only app that works" because "it won't stop until I do the thing") [r2:11,31]. However, repeated escalation carries documented failure modes: alert fatigue, habituation (reminders become invisible within weeks if overused), and notification stacking [r2:13,20,40]. *Confidence: high for the market pattern; medium for habituation risks.*
- **Non-social app notifications are cleared on sight, and louder banners increase dismissal rates.** Empirical research on 794,525 notifications (N=278) shows non-social productivity notifications are cleared on sight or left pending indefinitely [21][59]. Experience-sampling data proves each unit increase in notification disruption reduces acceptance odds by factor 0.581 (p<0.001) — louder banners increase user dismissal and app notification revocation [22][60]. Distinctive audio/haptic alert signals drive immediate attendance compared to silent banners [23][61]. Distinctive audio/haptic signalling is an evidence-backed delivery strategy, though exact sound and vibration profiles remain implementation hypotheses requiring personal validation. *Confidence: high.*
- **Priority tier inflation ("everything is P1") neutralizes flag-based urgency systems; time-bucket organization is a validated market pattern.** Todoist-style P1–P4 flags suffer from user degradation: users report collapsing every task into P1/P2 until flags distinguish nothing ("when everything is a priority, nothing is") [18][19][30]. Users who succeed impose strict scarcity (1–2 P1s/day max) [19]. Things 3 avoids priority flags entirely, expressing urgency through time-bucket placement (Today / This Evening), red deadline dates, and dedicated lists [20][31]. Priority flag inflation is a high-confidence validated finding, and Things 3 time-bucket organization is a validated market pattern [19][20][30][31]. *Confidence: high.*
- **Android notification importance caps at IMPORTANCE_HIGH (sound + heads-up), user-downgradeable.** Android notifications cap at `IMPORTANCE_HIGH` (level 4), which is user-adjustable per channel at any time [14][15][r2:6,13]. Android 15/16 notification cooldown exempts critical/alarm/call notifications, but no third-party declarable "critical" tier exists outside standard channels [r2:7,8,9,30]. Widgets, persistent status-bar indicators, and Android 16 `Notification.ProgressStyle` provide non-clearing glance surfaces [25][r2:14,18,32]. *Confidence: medium-high.*

## C.2 Observed existing-product behavior (end-to-end workflows)

- **Due (acknowledgement-based urgent reminder escalation):** task due → loud alert fires → unacknowledged → re-fires until acknowledged or snoozed → 4.7★ ratings; high ADHD praise, but requires strict item scarcity to prevent alert burnout [r2:10,11,12].
- **Apple Reminders (iOS 26.2 Urgent Alarms — iOS Market Reference Only):** set task as Urgent → system alarm fires at due time (breaks Focus/mute) → Snooze / Complete → non-dismissable Live Activity on lock screen [r2:1,2,3]. (Market reference only; not an Android requirement).
- **Things 3 (time-bucket placement & red deadlines):** Today / This Evening buckets + red deadline text + single "Important" tag; no multi-tier P1–P4 priority flags; avoids priority inflation [20].
- **Todoist (P1–P4 priority flags):** P1 (red, top-sorted) to P4 (default, uncolored); high friction when users mark everything P1 [18][19].
- **Mindr / GoodTask (widget/complication delivery):** Home screen interactive widgets and Watch complications displaying overdue counts and active items; bypasses notification drawer clearing [25].

## C.3 Technical constraints (product-behavioral)

- **Product Principle — Separation of Task Importance & Notification Delivery Intensity**: Task importance and notification delivery intensity are separate concepts. An important task (e.g., a multi-week project goal) does not require disruptive delivery, while a genuinely time-sensitive urgent task (e.g., leaving for an appointment) requires stronger delivery. The notification model structures delivery around `Routine → Important → Urgent` delivery classes without turning these into P1/P2/P3/P4 priority flags.
- **No OS-level un-silenceable alerts or unbypassable full-screen takeovers for ordinary productivity apps**: Critical Alerts (iOS) and auto-granted Full Screen Intent (Android 14+) are OS-gated and Play-scrutinized [10][16][g:10,11]. (Android exact alarms, permissions, FSI, Doze, and OS feasibility are delegated to **Dimension G**.)
- **Priority flag inflation is human behavior**: without hard scarcity limits, multi-tier priority flags collapse into clutter [19].
- **Alert disruption vs. attendance trade-off**: louder/more disruptive notifications increase user dismissal rates (factor 0.581 per unit disruption) [22]; attendance is driven by distinctive sound/haptic cues [23].
- **Android channel importance is user-controlled**: users can downgrade channel importance or disable notification channels at any time [15][r2:6].
- **Scope Restriction on Persistent Surfaces**: persistent status-bar notifications and progress surfaces are reserved primarily for active sessions/routines and genuinely active states (e.g., running focus timer, active routine step); they must NOT become universal notification behavior for static tasks.

## C.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **Single Urgent Delivery Class with Scarcity Guardrail**: establish a single `Urgent` delivery class separate from `Important` tasks to prevent Todoist-style priority inflation. *Status: evidence-backed product opportunity.*
- **Acknowledgement-Based Urgent Reminder Escalation**: adopt Due's repeated reminder pattern for true urgent items, bounded by explicit auto-resolution / cool-down rules so escalation loops do not induce alert fatigue. *Status: evidence-backed product opportunity.*
- **Persistent Active-Session Progress Surface**: leverage Android 16's `Notification.ProgressStyle` and persistent status-bar indicators strictly for active routine/study sessions, complementing push alerts with a glance surface. *Status: evidence-backed product opportunity.*

## C.5 Proposed product implications (adopt / adapt / avoid)

- **ADOPT — Things 3-style time-bucket organization (Today / Evening)**: separate task importance from notification delivery intensity; avoid 4-tier P1–P4 priority flags (Todoist failure) [19][20][30][31]. *Status: validated market pattern.*
- **ADAPT — Single Urgent delivery class with scarcity quota**: adopt a single `Urgent` delivery class with a quota (e.g., 1–2 items/day) to enforce scarcity [19]. *Status: evidence-backed product adaptation (scarcity = validated principle; exact 1–2 quota = preliminary product hypothesis).*
- **ADAPT — Acknowledgement-based urgent reminder escalation**: re-fire alerts at intervals only for items explicitly assigned to the `Urgent` delivery class, with auto-resolution after N re-fires [r2:10,11,31]. *Status: evidence-backed product adaptation (acknowledgement escalation = validated market pattern; exact 5-min interval & 3 re-fires = preliminary product hypotheses requiring personal validation).*
- **ADAPT — Distinctive audio/haptic alert signalling**: reserve specific audio/haptics for Urgent delivery class items to drive immediate attendance [23][61]. *Status: evidence-backed product adaptation (distinctive signalling = evidence-backed delivery strategy; exact sound/vibration profile = preliminary product hypothesis).*
- **ADAPT — Persistent active-session progress surface**: use persistent status-bar notifications strictly for active focus/routine sessions [25][r2:14,18]. *Status: evidence-backed product opportunity (reserved for active states, not universal).*
- **AVOID — multi-tiered P1–P4 priority flags** (documented inflation trap) [19][30]. *Status: validated finding.*
- **AVOID — relying on OS Critical Alerts or Full Screen Intent (FSI)** (inaccessible to ordinary productivity apps; rejected in Play reviews; Android OS feasibility in Dimension G) [10][16][g:10,11]. *Status: validated technical constraint.*
- **AVOID — uncapped repeated notifications** (causes notification revocation and alert fatigue) [22][r2:13,20]. *Status: validated finding.*

---

# Dimension C — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension C. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement. Android-feasibility questions are delegated to Dimension G.*

1. **Separation of Task Importance & Notification Delivery Intensity (Routine / Important / Urgent delivery classes).** Task importance does not equal delivery loudness; `Routine → Important → Urgent` delivery classes structure delivery without P1–P4 flags. Things 3 time-bucket placement (Today / Evening) as primary organizer. *Status: validated market pattern (high confidence).*
2. **Single Urgent Delivery Class & Scarcity Quota.** Establish a single `Urgent` delivery class separate from `Important`. *Status: evidence-backed product adaptation (scarcity = validated principle; exact 1–2 items/day quota = preliminary product hypothesis).*
3. **Acknowledgement-Based Urgent Reminder Escalation.** Re-fire alerts until acknowledged for Urgent delivery class items only (Due pattern), bounded by max retry count / auto-cool-down. *Status: validated market pattern (high confidence for acknowledgement escalation; exact 5-min interval & 3 re-fires = preliminary product hypotheses requiring personal validation).*
4. **Distinctive Audio/Haptic Alert Signalling.** Reserve distinctive sound/vibrations for Urgent delivery items to drive immediate attendance (Chang 2019 [23][61]). *Status: evidence-backed delivery strategy (high confidence for distinctive signalling; exact sound/vibration profile = preliminary product hypothesis).*
5. **Persistent Active-Session Surface (Scope-Restricted).** Use persistent status-bar / progress notifications (`Notification.ProgressStyle`) strictly during active focus/routine states; never as universal notification behavior for static tasks. *Status: evidence-based product opportunity (medium-high confidence).*
6. **No OS Entitlement Dependencies (Delegated to Dimension G).** Rely strictly on standard Android `IMPORTANCE_HIGH` channels and exact alarms, not FSI or Critical Alerts. Apple Urgent Alarms serve as an iOS market reference only. *Status: validated technical constraint (high confidence).*

**Open decisions carried forward (need validation):** exact urgent reminder escalation interval (e.g. 5 vs 10 min) and max retry count; Urgent item quota UX (1–2 vs dynamic); audio/haptic sound profile selection; active-session persistent notification layout.

---

# Dimension D — Breaks, fatigue, & adaptive rescheduling (product dimension 4 of 6)

**Questions this dimension answers:** Should the app enforce rigid break intervals (Pomodoro 25/5) or suggest flexible breaks? How should uncompleted tasks/routines be rescheduled without overwhelming the user or triggering streak-break abandonment? Which rescheduling and break mechanisms are proven, mixed, or dead ends — and what should the app adopt, adapt, or avoid?

## D.1 Evidence-backed findings

- **Enforced rigid breaks (Pomodoro 25/5) show NO objective productivity advantage over self-regulated breaks; hard breaks disrupt flow.** Direct RCT evidence (Smits et al. 2025, N=94; Biwer et al. 2023, N=87; Albulescu et al. 2022 meta-analysis, N=2,335) demonstrates that fixed 25/5 Pomodoro breaks produce no statistically significant improvement in task completion, flow, or overall productivity compared to self-regulated breaks [Smits 2025][Biwer 2023][Albulescu 2022]. Smits 2025 found fixed Pomodoro breaks actually produced a faster rise in fatigue and steeper drop in motivation across a 2-hour session, whereas Biwer 2023 showed fixed 24/6 breaks improved subjective concentration. Hard-enforced mandatory breaks (e.g. Flowkeeper) receive persistent user complaints about interrupting active flow states [Flowkeeper GH #205]. Flexible break suggestions support flow, while exact break triggers (e.g., 20–25m vs 50m) depend on user task context. *Confidence: high (validated finding).*
- **Automated AI rescheduling reduces planning effort but introduces opacity risks; transparent user-confirmed planning preserves trust.** Automated rescheduling tools (Motion, SkedPal) reduce manual planning effort but introduce documented trust and opacity risks when tasks move without explicit user understanding or consent [SkedPal docs][Motion reviews]. Conversely, Sunsama's manual daily planning with workload overcommitment guardrails won industry awards (NYT Wirecutter) for restoring user agency [Sunsama / Wirecutter]. Transparent, user-confirmed rescheduling ("Do Tomorrow") preserves user trust while eliminating manual re-entry friction. *Confidence: high (validated market pattern).*
- **Habit automaticity, goal slack, and streak grace outperform hard streak resets.** Habit science (Lally et al. 2010; Polivy & Herman 2002) demonstrates that a single missed day does not disrupt long-term habit automaticity, whereas hard streak resets trigger the "what-the-hell effect" where users abandon the app entirely [Lally 2010][goalsandprogress]. Peer-reviewed goal slack research (Sharif & Shu 2017/2021) proves that earned streak protection and emergency reserves boost long-term goal persistence [Sharif & Shu]. Streak grace and miss-recovery mechanisms are evidence-backed, while specific rules (such as "Never Miss Twice") are product heuristics requiring validation. *Confidence: high (validated mechanism).*
- **Workload overcommitment guardrails prevent daily task overload.** Overcommitment ("planning 12 hours of work into an 8-hour day") is a primary driver of daily task rollover. Sunsama enforces an explicit daily workload ceiling warning [Sunsama UI]. Workload capacity warnings are an evidence-backed existing product pattern, while exact capacity formulas, thresholds, and warning UX remain preliminary product hypotheses. *Confidence: medium-high (validated market pattern).*

## D.2 Observed existing-product behavior (end-to-end workflows)

- **Forest (focus timer without forced breaks):** user sets timer → tree grows → optional rest break prompt at end → no mandatory break enforcement or locked screens.
- **Motion / SkedPal (auto-rescheduling):** task missed or day shifts → AI algorithm recalculates schedule across calendar → high automation convenience, but arbitrary task moves cause user opacity concerns [Motion reviews].
- **Sunsama (manual ritual + overcommitment cap):** daily planning step → drag tasks into daily plan → total hours calculated → overcommitment warning triggered if total > capacity → user manually shifts excess tasks [Sunsama UI].
- **Reclaim / Griply (availability-aware habit shifting):** habits auto-adjust within open calendar windows; missed instance shifted to next day.

## D.3 Technical constraints (product-behavioral)

- **Product Principle — Item-Type Aware Rescheduling**: Rescheduling must be item-type aware. Fixed-time events, flexible tasks, routine occurrences, habits, and study sessions must NOT share one universal "missed → move to tomorrow" rule.
  - *Fixed-Time Event* (e.g. appointment/interview): Missed event must NOT silently move to tomorrow; requires manual re-keying or explicit prompt.
  - *Flexible Task* (e.g. study assignment): May offer 1-tap "Do Tomorrow" shift.
  - *Routine Occurrence* (e.g. daily morning stretch): Missed instance should expire/close for the day rather than stacking up tomorrow.
  - *Habit* (e.g. daily reading): Retains its scheduled cadence with streak-grace protection.
  *Status: evidence-informed product opportunity / design principle requiring validation.*
- **Cross-Dimension Integration with Frozen A/B Model (Pacing & Recovery Layer)**:
  - *Pacing & Recovery Workflow*: `DayType → Routine/Task/Study → FocusSession → work segment → break suggestion → Take Break / Keep Working → completion`.
  - *Miss Recovery Workflow*: `Scheduled item → missed → item-type-aware ReschedulingPolicy → transparent user confirmation → updated schedule → preserved history`.
  *(Note: Dimension D acts as the pacing and recovery layer without altering frozen A/B workflows.)*
- **Flow state vs. break enforcement trade-off**: hard-locking breaks interrupts active creative/study flow; suggesting breaks preserves user agency.
- **Algorithmic opacity barrier**: auto-moving tasks without showing the user *why* or requiring 1-tap confirmation destroys user trust.
- **Streak punishment trap**: resetting streaks to 0 on a single missed day causes user abandonment (what-the-hell effect).

## D.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **Non-Coercive, Flow-Aware Break Suggestions**: offer configurable break prompts at natural session checkpoints or user-set intervals with a 1-tap "Keep Working" option instead of hard session locks. *Status: evidence-backed / validated pattern (break suggestions); preliminary product hypothesis (exact break timing & triggers).*
- **Transparent User-Confirmed Rescheduling ("Do Tomorrow")**: when a flexible task or study session is missed, offer a transparent prompt ("Missed yesterday — move to today?") rather than black-box AI auto-shifting or silent dropping. *Status: evidence-backed product opportunity (transparent shift concept); preliminary product hypothesis (exact button, wording, trigger).*
- **Streak Grace & Miss Recovery**: implement 1-day streak freezes/slack and miss recovery to prevent abandonment after a missed day. *Status: evidence-backed mechanism (streak grace & goal slack); preliminary product hypothesis / product heuristic ("Never Miss Twice" rule).*
- **Workload Overcommitment Guardrails**: display visual warnings when total scheduled task time exceeds available daily capacity. *Status: evidence-backed existing product pattern (capacity warnings); preliminary product hypothesis (exact capacity calculation & threshold).*

## D.5 Proposed product implications (adopt / adapt / avoid)

- **ADOPT — suggested, non-coercive break prompts**: suggest breaks at focus session checkpoints with flexible 1-tap "Keep Working" extensions; do not hard-lock device during breaks. *Status: validated market pattern.*
- **ADAPT — transparent, user-confirmed rescheduling ("Do Tomorrow" modal)**: auto-detect missed flexible tasks and offer a 1-tap transparent move ("Reschedule to Today") with visible reasons, avoiding black-box AI confusion. *Status: evidence-backed product opportunity.*
- **ADAPT — streak grace / miss-recovery guardrail**: provide streak protection/slack for single missed days to prevent catastrophic drop-off. *Status: evidence-backed mechanism.*
- **ADAPT — workload overcommitment capacity warning**: warn users when daily scheduled time exceeds available hours during planning. *Status: evidence-backed existing product pattern.*
- **AVOID — hard-enforced mandatory breaks (Flowkeeper pattern)**: avoids flow disruption. *Status: product-default decision supported by flow-disruption evidence.*
- **AVOID — black-box AI auto-rescheduling without user visibility (Motion opacity trap)**: avoids user distrust and schedule instability. *Status: product-design decision requiring validation.*
- **AVOID — hard streak resets on single missed days**: avoids triggering the what-the-hell abandonment effect. *Status: validated finding.*

---

# Dimension D — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension D. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement.*

1. **Suggested, Non-Coercive Break Prompts.** Flexible, configurable break notifications with 1-tap "Keep Working" extension; never force-lock during active flow states. *Status: evidence-backed / validated pattern (high confidence for suggestion pattern; exact break timing = preliminary product hypothesis).*
2. **Transparent, User-Confirmed Rescheduling ("Do Tomorrow").** Transparent 1-tap prompt for missed flexible items with clear reasons; avoids opaque AI rescheduling. *Status: evidence-backed product opportunity (medium-high confidence for transparent shifting; exact button/trigger UX = preliminary product hypothesis).*
3. **Streak Grace & Miss Recovery.** Protect streaks on single missed days; treat single misses as data rather than failure. *Status: evidence-backed mechanism (high confidence for streak grace; "Never Miss Twice" rule = product heuristic / preliminary product hypothesis).*
4. **Item-Type Aware Rescheduling Policy.** Rescheduling logic differs by item type (fixed event vs flexible task vs routine occurrence vs habit). *Status: evidence-informed product opportunity / design principle requiring validation (medium-high confidence).*
5. **Workload Overcommitment Capacity Guardrails.** Highlight daily capacity caps during planning rituals to prevent unrealistic scheduling. *Status: evidence-backed existing product pattern (medium-high confidence for warning pattern; exact capacity formula = preliminary product hypothesis).*
6. **Default Choice Against Hard-Enforced Breaks.** App defaults to flexible break prompts rather than mandatory screen locks. *Status: product-default decision supported by flow-disruption evidence (high confidence).*

**Open decisions carried forward (need validation):** default break suggestion timing (25m vs 50m vs custom session checkpoint); exact UX and triggers for 1-tap reschedule confirmation; item-type specific rescheduling rules matrix; exact capacity calculation formula; streak grace token economy binding.

---

# Dimension E — Capture friction, voice logging, & failure tone (product dimension 5 of 6)

**Questions this dimension answers:** How do top apps make task and study entry frictionless (natural language, voice capture)? How should the app handle missed targets and broken streaks without demotivating the user or encouraging complete abandonment? Which capture mechanisms and failure tones are proven, mixed, or dead ends — and what should the app adopt, adapt, or avoid?

## E.1 Evidence-backed findings

- **One-line natural language quick-add (Todoist/TickTick) is a mature, widely validated low-friction capture pattern.** Product iteration across leading tools demonstrates that one-line text parsing (extracting title, dates, times, duration, subject, and tags in real time) is a mature, widely validated low-friction capture pattern [Todoist Help][ClickUp review]. Inline syntax highlighting, 1-tap tag pills, tap-to-revert pills, and tap-to-undo prevent multi-field form friction and resolve mis-parsing edge cases. *Confidence: high (validated market pattern).*
- **Voice-to-task capture is a validated complementary capture modality.** Peer-reviewed research (NoteWordy, N=17) confirms speech input significantly speeds entry times compared to typing (p=0.004), though social and acoustic context constraints (privacy, ambient noise, public settings) limit speech to a secondary role (<20% of total entries) [Luo CHI 2023]. Strong product-adoption signals from leading tools (e.g. Doist Ramble) demonstrate high user demand for voice capture [PR Newswire 2026][Product Talk 2026], though company-reported adoption metrics reflect user demand rather than independent causal proof that voice drives long-term retention. System safety requires a confidence floor with raw Inbox fallback when speech intent is ambiguous [quik.md]. *Confidence: medium-high (evidence-backed complementary modality).*
- **Hard streak resets ("miss = 0") trigger the "streak cliff" and lead to user abandonment.** Primary research in consumer psychology (Silverman & Barasch, JCR 2023, N=7 studies) proves that broken streaks durably suppress subsequent user engagement compared to intact streaks [Silverman & Barasch JCR 2023]. Qualitative user data (Oulu 2025) documents users quitting entirely after losing long streaks [Julkunen Oulu 2025]. *Confidence: high (validated finding).*
- **Goal "slack" (streak freezes / emergency reserves) protects long-term persistence after failure.** Peer-reviewed behavioral research (Sharif & Shu, OBHDP 2021, 1 field + 4 lab studies) proves that framing goals with emergency reserves (slack / streak freezes) significantly increases persistence after subgoal failure compared to rigid no-slack goals [Sharif & Shu 2021]. Goal slack and streak grace are evidence-backed mechanisms (aligned with frozen **Dimension D**), while exact parameters (such as 1–2 earned monthly streak freezes and 3-day post-miss recovery) are preliminary product hypotheses requiring validation. *Confidence: high (validated mechanism).*
- **Punishing HP-loss mechanics (Habitica) produce counterproductive evasion and anxiety.** Peer-reviewed evaluation of Habitica (IJHCS 2019) documents counterproductive effects across all user groups: users relabel tasks to dodge damage, avoid opening the app during busy days, or quit [Diefenbach IJHCS 2019]. Habitica itself shipped an opt-out "Pause Damage" setting to prevent user churn [Habitica FAQ]. *Confidence: high (validated finding).*
- **Guilt-free failure communication prevents task evasion and app abandonment.** Maintaining a non-judgmental, guilt-free failure tone during missed days prevents task evasion and app abandonment [Finch FAQ][Engadget review]. Guilt-free failure communication is an evidence-backed design direction, while exact persona wording and message templates require UX validation. Full mascot/companion persona implementation is delegated to **Dimension F**. Paid/monetized streak repairs are excluded primarily due to incompatibility with this project's locked 100% free / non-monetized personal-use constraint [afterburnout.co]. *Confidence: medium-high (evidence-backed design direction).*

## E.2 Observed existing-product behavior (end-to-end workflows)

- **Todoist / TickTick (one-line NLP quick-add):** type "Read chapter 4 tomorrow 5pm #study" → real-time visual syntax highlighting parses title, date, time, tag → tap submit → instant task creation with tap-to-undo option.
- **Doist Ramble (voice-to-task capture):** speak freely → speech model extracts title, due date, project, tag → surfaces structured preview → high-confidence: creates task; low-confidence: files into raw Inbox.
- **Duolingo / Finch (slack & earned streak repair):** miss a day → streak freeze auto-equips or 3-day grace window opens → earned recovery via consecutive completions → streak preserved without guilt.
- **Habitica (HP loss / death penalty):** miss Daily → lose HP → HP zero = level loss & equipment drop → documented user stress & task cheat re-labeling.

## E.3 Technical constraints (product-behavioral)

- **Product Principle — End-to-End Task Lifecycle Capture**: Capture is not complete when a database task record is created. The full lifecycle workflow is:
  `Capture → Parse → Confirm → Structured Task → Schedule/DayType → Execute → Completion/Recovery`.
  The captured object must immediately participate in the existing scheduling (Dimension A), focus (Dimension B), reminder (Dimension C), and recovery (Dimension D) workflows rather than becoming an isolated task record.
  *Status: evidence-informed product opportunity / design principle requiring validation.*
- **NLP DayType Extraction Model (Dimension A Alignment)**: `DayType` is NOT a mandatory NLP-extracted field. The NLP parser extracts an explicitly stated day-type only if the user explicitly specifies one (e.g., "for weekend routine"); otherwise, the scheduler derives the active `DayType` from the target date/calendar established by frozen **Dimension A**. NLP primarily extracts title, date, time, duration, subject, and tags.
- **Voice Capture Engine Android Separation (Dimension G Delegation)**: E defines voice-capture behavior, confidence-floor thresholds, and raw Inbox fallback UX; exact engine selection (`ML Kit` / `SpeechRecognizer` / local STT) is delegated to **Dimension G**.
- **NLP mis-parsing guardrails**: auto-parsing must render a clear visual preview, inline highlighting, and a tap-to-revert pill UI before locking parsed dates/tags.
- **Voice capture context constraint**: speech input is constrained by social environment and background noise (<20% primary usage); must complement, not replace, quick text entry.

## E.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **One-Line NLP Quick-Add with Tap-to-Revert**: implement inline natural language parsing for title, dates, times, duration, subject, and tags with real-time visual highlighting and tap-to-revert pills. *Status: validated market pattern (high confidence).*
- **Voice Quick-Capture with Confidence Floor & Inbox Fallback**: provide voice logging; if parsing confidence falls below floor, route raw text/audio into Inbox for 1-tap sorting instead of wrong auto-classification. *Status: evidence-backed product opportunity (medium-high confidence).*
- **Earned Streak Slack & Recovery Window**: implement earned streak freezes and post-miss recovery windows (aligned with frozen **Dimension D**). *Status: evidence-backed mechanism (streak grace & goal slack); preliminary product hypothesis (exact quantities: 1–2 earned freezes / 3-day recovery).*

## E.5 Proposed product implications (adopt / adapt / avoid)

- **ADOPT — one-line natural-language text quick-add**: instant real-time parsing of task title, date, time, duration, subject, and tags with a tap-to-revert pill. *Status: validated market pattern.*
- **ADAPT — voice quick-capture with confidence floor & Inbox fallback**: allow voice logging; route low-confidence voice input into an un-filed Inbox for 1-tap review (Android STT engine in Dimension G). *Status: evidence-backed complementary modality.*
- **ADAPT — earned streak slack & guilt-free failure tone**: provide streak grace and post-miss recovery; adopt a non-judgmental failure tone (exact wording requires UX validation; full companion in Dimension F). *Status: evidence-backed mechanism (slack) / evidence-backed design direction (failure tone).*
- **AVOID — hard streak resets ("miss = 0")**: avoids triggering the streak cliff and user dropout. *Status: validated finding.*
- **AVOID — Habitica-style HP penalties / punishment mechanics**: avoids task evasion, cheating, and anxiety. *Status: validated finding.*
- **AVOID — monetized or paid streak freezes/repairs**: incompatible with locked 100% free / non-monetized personal-use constraint. *Status: validated technical constraint / project boundary.*

---

# Dimension E — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension E. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement.*

1. **One-Line Natural-Language Text Quick-Add.** Instant parsing for title/date/time/subject/tags with visual highlighting and tap-to-revert UI; DayType derived from date via Dimension A. *Status: validated market pattern (high confidence).*
2. **Voice Quick-Capture with Inbox Safety Net.** Speech capture routed through confidence floor; low-confidence inputs saved to raw Inbox for 1-tap review (Android STT engine in Dimension G). *Status: evidence-backed complementary modality (medium-high confidence for voice modality; confidence floor + Inbox fallback = evidence-backed product opportunity).*
3. **Earned Streak Slack & Grace Periods.** Protect streaks on single missed days (Sharif & Shu goal-slack model; aligned with frozen Dimension D); never reset long streaks to 0 on a single miss. *Status: evidence-backed mechanism (high confidence for streak grace; exact quantities of 1–2 earned freezes & 3-day recovery = preliminary product hypotheses).*
4. **Guilt-Free Failure Communication Principle.** Non-judgmental, zero-penalty messaging on missed days to prevent evasion; companion mascot implementation delegated to Dimension F. *Status: evidence-backed design direction (high confidence for failure principle; exact persona wording = UX validation required).*
5. **End-to-End Task Lifecycle Capture Principle.** `Capture → Parse → Confirm → Structured Task → Schedule/DayType → Execute → Completion/Recovery`. Captured tasks immediately enter existing scheduling, focus, and reminder workflows. *Status: evidence-informed product opportunity / design principle requiring validation (medium-high confidence).*
6. **No Paid/Monetized Streak Repairs.** Streak protection is strictly earned through activity; monetized repairs excluded due to locked 100% free app constraint. *Status: validated technical constraint / project boundary (high confidence).*

**Open decisions carried forward (need validation):** local NLP parser library selection; exact confidence floor threshold for voice parsing; exact persona wording and message templates for failure feedback; exact earned-freeze formula (e.g. 1 freeze per 10 active days).

---

# Dimension F — Gamification, companion persona, & long-term retention (product dimension 6 of 6)

**Questions this dimension answers:** How should gamification and mascot/companion elements be designed to sustain multi-month user retention without triggering novelty burnout, notification anxiety, or reward-engine friction? Which gamification mechanics, currency models, and companion patterns are proven, mixed, or dead ends — and what should the app adopt, adapt, or avoid?

## F.1 Evidence-backed findings

- **Companion / mascot personas are an evidence-backed engagement and retention pattern.** Companion / mascot personas are an evidence-backed engagement and retention pattern; Finch provides strong product-level retention signals (54% D1 / 37% D7; ~10M MAU; 4.9★ rating), but the available evidence does not establish that the mascot itself causally produces retention [Deconstructor of Fun 2026][Finch Care]. Duolingo's mascot serves as a "relationship vector," providing emotional anchor points for reminders [Blake Crosley]. *Confidence: high (evidence-backed product pattern).*
- **Gamification can experience novelty decay within the first several weeks; continued engagement requires lightweight variation.** Gamification can experience novelty decay within the first several weeks; continued engagement therefore requires lightweight variation and progression without turning the companion into a secondary chore [ERIC EJ1325797]. Longitudinal peer-reviewed research (14-week study) demonstrates that gamification's initial lift wanes after 2–6 weeks as novelty fades, but recovers via a familiarization phase to establish a positive long-term baseline [ERIC EJ1325797]. User reviews corroborate that rigid mascot mechanics start to feel like "one more chore" after month 3 if progression stagnates [HabitBox Finch review]. *Confidence: high (validated finding).*
- **Single shared reward currency (Coins) provides a simple, low-complexity reward architecture.** A single shared reward currency provides a simple, low-complexity reward architecture across task types; exact reward values and economy balance require validation. Existing products demonstrate both unified and multi-currency systems; for this product, a single shared currency is preferred for simplicity and consistency, but universal superiority over multi-currency systems is not established by the available evidence [Finch Help][Habitica Wiki][Forest official][Paschmann JM 2024]. Coins can be spent on companion cosmetics, environments, accessories, and other non-essential progression items; optional user-defined real-life rewards may be explored as a future reward sink and require product validation [Habitica Wiki]. *Confidence: high (evidence-backed product design choice).*
- **Goal "slack" and streak protection support long-term goal persistence.** Implement earned streak protection so an isolated miss does not destroy long-term progress; exact freeze quantity, earning rate, banking limit, and recovery window remain preliminary product hypotheses requiring validation (aligned with frozen **Dimensions D** and **E**). Peer-reviewed field and lab experiments (Sharif & Shu, JMR 2017 & OBHDP 2019) prove that granting goal reserves (slack) yields up to +20% step counts and +40% more goal-days, significantly boosting persistence after a missed day compared to hard no-slack rules [Sharif & Shu 2017/2019]. Duolingo experimentation provides supporting evidence that streak protection can improve engagement/retention metrics; the exact effect size should not be treated as universally causal or directly transferable to this product [Duolingo blog 2022]. *Confidence: high (evidence-backed mechanism for streak grace; supporting product evidence for effect size).*
- **Punishing mechanics (Habitica HP loss) cause user evasion, cheating, and dropouts.** Peer-reviewed evaluation of Habitica (IJHCS 2019) documents counterproductive effects across all users: users relabel tasks to dodge damage, avoid opening the app during busy days, or quit [Diefenbach IJHCS 2019]. Self-initiated, recoverable session stakes such as Forest's tree mechanic provide a possible accountability layer without introducing persistent punishment; suitability for this product requires validation [Forest official]. *Confidence: high (validated finding for punishment avoidance; evidence-backed mechanism / product opportunity for self-initiated session stakes).*
- **Guilt-toned notifications risk sentiment backlash; notification policy is delegated to Dimensions C/E.** Companion messaging follows the supportive, guilt-free tone established in Dimension E; notification delivery, frequency, escalation, and template rotation remain governed by Dimension C, with this behavior delegated to frozen **Dimensions C/E** [Yancey & Settles KDD '20][Julkunen Oulu 2025]. Tone must remain supportive and gentle rather than passive-aggressive. *Confidence: medium-high (cross-dimension delegation requirement).*
- **Stake-weighted reward scaling is an unproven hypothesis.** Weighting reward magnitude by task priority/urgency exists in Habitica (difficulty scaling) and Forest (session length), but no empirical evidence proves it increases completion or retention. *Confidence: medium (unproven hypothesis).*

## F.2 Observed existing-product behavior (end-to-end workflows)

- **Finch (self-care pet loop):** complete daily tasks → earn Energy & Rainbow Stones → send pet on adventure → unlock clothing/room decor → no death/guilt if days are missed.
- **Forest (focus tree planting):** start focus session → plant tree → complete session → earn coins → spend coins to unlock new tree species or plant real-world trees via Trees for the Future.
- **Habitica (RPG tasks & custom gold rewards):** complete habits/dailies → earn XP & Gold → spend Gold on gear, quests, or custom real-life rewards (e.g. 15 min gaming) → missing Dailies causes HP loss (opt-outable via Pause Damage).
- **Duolingo (streak freezes & Duo owl nudges):** complete lesson → maintain streak → 2 Streak Freezes automatically buffer missed days → Duo owl nudges return inactive users.

## F.3 Technical constraints (product-behavioral & product principles)

- **Product Principle — Companion Non-Interference**: The companion is a supportive reward and feedback layer, NOT a second task manager, notification engine, or enforcement system, and must reinforce existing workflows without creating additional mandatory actions.
- **Product Principle — Lightweight Companion Progression**: Companion progression remains subordinate to the user's real goals; feeding, maintaining, leveling, decorating, or interacting with the companion must NEVER become a mandatory daily task or create a second productivity system.
- **Product Principle — Meaningful-Completion Rewarding**: Coins are awarded for meaningful completion events defined by existing task, habit, study, and focus systems — NOT arbitrary taps, app opens, notification interactions, or companion interactions.
- **Product Principle — Reward-Farming Constraint**: The reward economy must prevent trivial repeated actions from generating disproportionate Coins; reward eligibility and frequency should be tied to actual completion state rather than interaction count.
- **Product Principle — Separation of Companion Rewards & Real-Life Rewards**: Coins can be spent on companion cosmetics, environments, accessories, and other non-essential progression items. Optional user-defined real-life rewards may be explored as a future reward sink and require product validation.
- **Product Principle — Missed-Day Companion Behavior**: When the user misses a task or habit, the companion must NOT become weaker, sadder, damaged, or unavailable, but should acknowledge the missed activity neutrally and direct attention toward the next actionable opportunity.
- **Product Principle — Architectural Separation of Streak & Companion Systems**: The streak system and companion system remain architecturally separate; completion feeds both a streak/progress system and a reward system, while the companion consumes progress/reward signals rather than owning the streak.
- **Novelty decay curve**: mascot mechanics experience a novelty decline over time; progression systems must remain lightweight and avoid becoming a tedious "secondary chore".
- **Multi-currency friction**: avoiding split currencies (e.g. separate habit coins vs. task gems); a single unified currency prevents postreward engagement drops.
- **Notification fatigue delegation**: C/E own notification behavior, while F defines companion feedback without creating additional notification burden.

## F.4 Inferred opportunities (gaps — evidence-poor, reasoning-based)

- **Supportive Companion Layer**: adopt a supportive companion persona (Finch model) that celebrates progress and provides non-judgmental feedback without HP penalties. *Status: evidence-backed product opportunity.*
- **Single Shared Coins Economy**: implement a single reward currency ("Coins") earned across meaningful completions in all habits, study sessions, routines, and tasks. *Status: evidence-backed product design choice with exact reward values requiring validation.*
- **Companion Cosmetic Progression**: allow Coins to unlock optional companion cosmetics, environments, and accessories without affecting core functionality. *Status: validated market pattern with progression depth requiring validation.*
- **Optional Real-Life Reward Sinks**: allow users to optionally define personal real-life reward sinks for accumulated Coins. *Status: preliminary product hypothesis.*
- **Earned Streak Slack**: implement earned streak freezes and post-miss recovery (aligned with frozen **Dimensions D** and **E**). *Status: evidence-backed mechanism with exact parameters requiring validation.*
- **Lightweight Progression**: structure companion progression to provide lightweight variation without creating mandatory companion-maintenance chores. *Status: evidence-backed design direction.*

## F.5 Proposed product implications (adopt / adapt / avoid)

- **ADOPT — non-punishing companion persona**: supportive mascot that grows with meaningful progress, with no HP loss, character death, or guilt-based reactions.
- **ADOPT — single shared Coins currency**: one unified currency across meaningful completion events.
- **ADAPT — companion cosmetic progression**: Coins unlock optional companion customization and progression without affecting core functionality.
- **ADAPT — earned streak slack**: provide streak protection consistent with frozen **Dimensions D/E**, with exact quantities and recovery rules remaining hypotheses.
- **ADAPT — optional real-life reward sinks**: allow users to define personal rewards for accumulated Coins only if validated.
- **ADAPT — lightweight progression variation**: introduce new cosmetic/progression content without creating mandatory companion-maintenance work.
- **AVOID — HP penalties or character death**.
- **AVOID — guilt-inducing or passive-aggressive companion behavior**.
- **AVOID — mandatory companion maintenance**.
- **AVOID — reward farming through trivial interactions**.
- **AVOID — companion-driven notification spam**, because notification policy remains governed by Dimension C.

---

# Dimension F — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension F. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement.*

1. **Non-Punishing Companion Persona** — supportive mascot with no HP loss, character death, or guilt-based reactions; *Status: evidence-backed design direction, high confidence.*
2. **Single Shared Coins Currency** — one unified reward currency across meaningful completion events; *Status: evidence-backed product design choice, high confidence, with exact reward values requiring validation.*
3. **Companion Cosmetic & Progression Layer** — optional cosmetics, environments, accessories, and progression; *Status: validated market pattern, with progression depth requiring validation.*
4. **Earned Streak Slack** — protect long-term continuity after isolated misses; exact parameters remain unvalidated; *Status: evidence-backed mechanism.*
5. **Lightweight Companion Progression** — enough variation to reduce novelty without creating another chore; *Status: evidence-backed design direction.*
6. **Meaningful-Completion Rewarding** — reward genuine completion rather than taps, opens, or interactions; *Status: product design principle requiring validation.*
7. **Optional Real-Life Reward Sinks** — future/optional feature requiring validation; *Status: preliminary product hypothesis.*
8. **Companion Non-Interference** — companion does not become a second task manager or enforcement system; *Status: product design principle.*
9. **Notification Ownership Delegation** — C/E own notification behavior while F defines companion behavior; *Status: cross-dimension consistency requirement.*
10. **Avoid Punitive Gamification** — no HP loss, character death, guilt, or catastrophic punishment for misses; *Status: validated finding / frozen product direction.*

**Open decisions carried forward (need validation):** companion visual identity and emotional personality; companion progression depth and content cadence; Coin reward values for each meaningful completion type; Coin economy balancing and anti-farming rules; companion cosmetic categories and unlock structure; whether custom real-life rewards are included in v1; streak-freeze earning rate, banking limit, and recovery parameters aligned with frozen D/E and explicitly unvalidated; how companion feedback is surfaced without creating additional notification burden; and whether companion progression is purely cosmetic or includes limited functional unlocks.

---

# Dimension G — Android production feasibility (product dimension 7)

**Questions this dimension answers:** What are the hard OS-level capability and permission constraints for an Android-only, free, local-first personal app targeting modern Android versions (API 33–36 / Android 13–16)? Which technical mechanisms for notifications, alarms, app-blocking, voice capture, and on-device AI are implementable at production quality — and what are the OS boundaries?

## G.1 Evidence-backed findings

- **Notification Permissions & Delivery Channels (Android 13+)**: `POST_NOTIFICATIONS` runtime permission is required on Android 13+ (API 33+); users can deny it or select "Don't allow again" [Android Devs notification-permission]. Notification channels (mandatory since API 26) cap at `IMPORTANCE_HIGH` (heads-up + sound); importance is fixed at channel creation and user-adjustable at any time in Settings [Pushwoosh / Android Devs]. Heads-up display is system-controlled (shown only under OS conditions, not app-forceable) [Android Devs time-sensitive]. Notification trampolines (`startActivity()` from background receivers/services) are blocked on API 31+ unless direct PendingIntents or `SYSTEM_ALERT_WINDOW` are used [Android 12 behavior-changes]. *Confidence: high (validated technical constraint).*
- **Exact Alarms, Full-Screen Intents, & Foreground Services (Android 14+)**: For newly installed apps targeting Android 13+ (API 33+) running on Android 14+ (API 34+), `SCHEDULE_EXACT_ALARM` is denied by default; the app must check `AlarmManager.canScheduleExactAlarms()` and guide the user to system Alarms & reminders access settings when exact alarms are required [Android 14 schedule-exact-alarms]. `setAlarmClock()` is the preferred mechanism for genuinely time-critical, user-facing alarm-style events, but exact alarms must not be used for every routine, habit, reminder, or background job because they are power-sensitive; `WorkManager` is used for deferrable/persistent background work [Android Doze]. `USE_EXACT_ALARM` is a separate permission intended for apps whose core functionality genuinely relies on exact alarms and must not be selected merely as an easier alternative to `SCHEDULE_EXACT_ALARM` [Android Devs]. Android 14+ and Google Play impose strict restrictions on Full-Screen Intent (FSI) for productivity apps; FSI must never be a core dependency, and the app must gracefully fall back to standard high-importance notifications / heads-up UI when FSI is unavailable [AOSP fsi-limits]. Foreground Service (FGS) background starts face 6-hour/24h timeouts on Android 15+ [Android FGS timeouts]. *Confidence: high (validated technical constraint).*
- **App Usage Tracking & Accessibility-Based Interruption**: `AccessibilityService` is a technically viable privileged mechanism for detecting relevant foreground UI/app transitions and presenting an intervention overlay, but it is user-enabled and carries privacy, security, OEM, and distribution-policy considerations; for this personal-use app it may be used as an explicit opt-in enforcement layer [Coinage Software]. `UsageStatsManager` (`PACKAGE_USAGE_STATS`) provides interval-aggregated app usage data (daily/weekly/monthly) and is primarily used for historical usage analytics, daily/weekly distraction statistics, and validation rather than as the real-time blocker trigger [UsageStatsManager]. App Standby buckets (`ACTIVE` to `RESTRICTED`) defer jobs and alarms on battery [App Standby]. Android Digital Wellbeing provides no public third-party API [Digital Wellbeing listing]. *Confidence: high (technically viable privileged mechanism).*
- **Voice Architecture Separation (Speech Recognition & Intent Parsing)**: Voice capture operates through separate speech-to-text and intent-parsing layers: `Audio → on-device speech recognition when available → text → deterministic/structured intent parser → confidence validation → preview/confirmation → task service`. On-device speech recognition (`createOnDeviceSpeechRecognizer` or Google ML Kit) is supported on API 31+ with downloaded language packs, but availability and quality vary by device/OEM [DevGex 2025][Google ML Kit]. If on-device speech recognition is unavailable, the system falls back to manual text entry. *Confidence: medium-high (technically viable capability).*
- **On-Device AI (Gemini Nano / AICore) & Cloud Separation**: Gemini Nano runs locally via AICore (system service in Android 14 QPR1+ / Android 16) with Private Compute Core isolation [Android Devs Blog Gemini Nano]. Gemini Nano / AICore availability is device-dependent and must be detected at runtime; on-device Gemini is an optional enhancement rather than a hard dependency for task creation, reminders, routines, blocking, scheduling, or any core tracker feature [Jetstream blog]. Local Gemini Nano / AICore (optional local enhancement) and Cloud Gemini API (network-dependent optional capability) are separate architecture options; neither is required for core functionality, and the core application remains fully functional without Gemini. *Confidence: medium (optional platform capability).*
- **Battery Optimization & OEM Variances**: Doze mode defers standard alarms and network access; `setExactAndAllowWhileIdle()` and `setAlarmClock()` fire during Doze (~15 min quota) [Android Doze]. Aggressive OEM battery managers (e.g. Samsung "Sleeping apps", Xiaomi MIUI) silently suppress notifications, exact alarms, and background services unless battery optimization is disabled; capabilities must be verified on the actual target device configuration. *Confidence: high (validated technical constraint).*

## G.2 Observed existing-product behavior & mechanisms

- **Alarm & Calendar Apps:** Declare `USE_EXACT_ALARM` or request `SCHEDULE_EXACT_ALARM` + use `setAlarmClock()` for user-facing time-critical alarms; fallback to `WorkManager` for background sync.
- **App Interruption Tools (Digital Mindfulness / Freedom Android):** Request user opt-in `AccessibilityService` for real-time foreground package change detection and display an accessibility overlay redirect.
- **Voice Capture Tools:** Use Google ML Kit / `SpeechRecognizer` API 31+ for local STT, with manual text entry fallback when offline speech is unavailable.

## G.3 Technical constraints & Product Principles

- **Local-First, Network-Optional Architecture**: The application uses a 100% free, local-first, network-optional architecture. All core task, habit, routine, reminder, alarm, blocking, voice-fallback, and reward functionality MUST remain fully functional without network access.
- **AI Action Boundary**: AI models, speech models, or future Gemini integrations may interpret user input and propose structured commands, but model output MUST NEVER directly mutate the database or execute privileged actions. Every AI-generated action MUST pass deterministic schema validation, authorization/business-rule checks, and the normal application task service before modifying tasks, routines, reminders, blocking state, rewards, or persistent data.
- **Minimum Accessibility Data Principle**: The `AccessibilityService` must collect ONLY the minimum information required to identify configured target-app transitions and enforce the user's configured intervention; it must NEVER persist, transmit, or analyze unrelated screen content, passwords, OTPs, private messages, or sensitive UI text, and should avoid requesting window-content access unless a specific feature genuinely requires it.
- **Local-First Security Principle**: Implements encrypted local storage where appropriate, zero advertising/analytics SDKs, zero telemetry, no cloud sync by default, no unnecessary network access, and immediate deletion/disposal of raw voice/audio after processing unless the user explicitly chooses to retain it.
- **OEM Reliability Constraint**: Exact alarms, notifications, background execution, `AccessibilityService` behavior, and on-device AI capabilities must be tested on the actual target device/OEM configuration. Includes a capability diagnostics screen reporting notification permission, exact-alarm access, `AccessibilityService` state, battery optimization status, speech recognition availability, and Gemini/AICore availability.
- **Capability / Fallback Matrix**:
  - `Notifications`: Core capability; fallback to in-app notification list.
  - `Exact Alarms`: Required only for precise alarm-style events (`setAlarmClock()`); fallback to normal notifications.
  - `Full-Screen Intent (FSI)`: Optional enhancement; fallback to high-importance heads-up notifications.
  - `AccessibilityService`: Required only for real-time app-blocking; blocking is disabled gracefully if service is off.
  - `UsageStatsManager`: Optional historical analytics.
  - `On-Device STT`: Preferred voice input; fallback to manual text entry.
  - `Gemini Nano / AICore`: Optional enhancement; fallback to deterministic local rule-based parser.
  - `Cloud Gemini API`: Optional external capability; never core.
  - `WorkManager`: Deferrable background maintenance.
  - `Internet Access`: Not required for core functionality.

## G.4 Proposed product technical choices (adopt / adapt / avoid)

- **ADOPT — standard notification channels plus setAlarmClock() only for genuinely time-critical user-facing alarms and WorkManager for deferrable background work**.
- **ADAPT — AccessibilityService as an explicit opt-in, task-aware interruption mechanism with minimal data collection and accessibility overlay, with UsageStatsManager used mainly for historical analytics**.
- **ADAPT — hybrid local voice capture with on-device speech recognition where available and manual fallback**.
- **ADAPT — optional Gemini Nano/AICore enhancement with deterministic local fallback**.
- **AVOID — making FSI a core dependency**.
- **AVOID — making cloud APIs or API keys a core dependency**.
- **AVOID — collecting unnecessary AccessibilityService screen data**.
- **AVOID — making Gemini responsible for direct database or privileged actions**.

---

# Dimension G — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension G. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement.*

1. **Exact Alarms & WorkManager Architecture.** `setAlarmClock()` for genuinely time-critical alarms; `WorkManager` for deferrable background tasks; exact alarms restricted from power-sensitive routine jobs. *Status: validated technical architecture, high confidence.*
2. **AccessibilityService Interruption Mechanism.** `AccessibilityService` overlay redirect for real-time task-aware app blocking, with explicit opt-in and minimal data collection. *Status: technically viable privileged mechanism, medium confidence, device/OEM/security validation required.*
3. **UsageStatsManager Historical Analytics.** `UsageStatsManager` (`PACKAGE_USAGE_STATS`) used for historical analytics and distraction statistics, not real-time blocking. *Status: validated analytics capability, high confidence, not primary real-time blocker.*
4. **Local Speech Recognition Modality.** On-device speech recognition (`SpeechRecognizer` / ML Kit) for voice capture, with manual text entry fallback. *Status: technically viable capability, medium-high confidence, device/language validation required.*
5. **AICore / Gemini Nano Optional Integration.** Local LLM features enabled only when AICore is present; deterministic rule-based fallback for all core features. *Status: optional platform capability, medium confidence, runtime availability required.*
6. **Deterministic Rule-Based Fallback Architecture.** Deterministic schema validation and business rules gate all AI actions and provide full offline fallback. *Status: required architecture, high confidence.*
7. **Local-First Security Architecture.** 100% free, local-first, network-optional security model with local data encryption, zero telemetry/ads, and audio disposal. *Status: product/security requirement, high confidence.*

**Open decisions carried forward (need validation):** exact `AccessibilityService` event strategy and overlay design; which minimum accessibility information is actually required; OEM-specific reliability behavior; exact-alarm permission onboarding; capability diagnostics UX; on-device STT engine selection and language availability; Gemini Nano capability detection; deterministic parser schema and confidence thresholds; AI action validation rules; and whether any future optional cloud AI is permitted.

---

# Dimension H — Problem validation & mechanism evidence (product dimension 8)

**Questions this dimension answers:** Are the core target problems (procrastination, notification overload, irregular routines, task initiation, digital distraction, habit maintenance) supported by empirical prevalence and cost evidence? Which behavioral mechanisms (implementation intentions, JITAI, Zeigarnik effect, reminders) are scientifically validated vs. over-hyped?

## H.1 Evidence-backed findings

- **Procrastination is a prevalent and costly problem among students.** The reviewed literature reports substantial prevalence among students, with meta-analytic evidence showing a small but consistent negative association with academic performance (r = -0.18) [Steel 2007][MDPI 2024]. *Confidence: high (validated problem).*
- **Notification overload is a validated problem; important-notification miss frequency is an unresolved personal-use hypothesis.** Diary studies document ~63.5 notifications/day causing alert fatigue and task disruption [MDPI Applied Sci 2024]. However, the research does not establish how frequently users actually miss critical emails, OTPs, interview invitations, payment alerts, or other high-priority notifications. A landmark 2020 CHB study (37 users, 200h video, 1,130 interactions) found that 89% of observed smartphone interactions were user-initiated (every ~5 min) [CHB 2020]. (Dual-Attention Principle: evidence indicates that both externally delivered notifications and user-initiated phone checking contribute to attention disruption; the product addresses both restrained notification delivery and state-aware in-the-moment intervention). *Confidence: high (validated problem for notification volume & alert fatigue; unresolved personal-use hypothesis for critical-notification miss frequency).*
- **Irregular daily routines are associated with health risks.** Large longitudinal studies (N=13,600 across Germany, China, Russia; N=78,115 Japanese cohort) associate irregular daily routines with increased risk of depression, anxiety, and elevated cardiovascular disease risk (HR 1.24–1.49) [Current Psych 2026][Sci Rep 2022]. Mobile-sensing data (1,086 participants, 153k person-days) confirms daily life resolves into ~8 routine types, with person-specific routine fingerprints covering >50% of days [arXiv 2026]. (Health-Outcome Boundary: health-risk evidence supports routine regularity as a relevant behavioral problem, but the product must not claim to prevent, treat, or reduce depression, anxiety, cardiovascular disease, or other medical conditions without direct clinical evidence). *Confidence: high (validated problem for routine irregularity).*
- **The Zeigarnik Effect (remembering unfinished tasks) FAILS to replicate; Ovsiankina task resumption is reliable.** A 2025 meta-analysis (Humanities & Social Sciences Communications) proves the classic Zeigarnik recall effect does not replicate (recall ratio 0.99, dz = 0.15) [HSSC 2025]. It is an overstated, dead-end mechanism for UI design. Conversely, the Ovsiankina effect (spontaneous resumption tendency of interrupted tasks) remains reliable [HSSC 2025]. Product principle: use resumption cues and recovery support rather than guilt, anxiety, or persistent reminders based on unfinished-task tension. *Confidence: high (dead-end mechanism for Zeigarnik; validated behavioral mechanism for Ovsiankina/resumption).*
- **Implementation Intentions (If-Then planning) show a robust behavioral effect.** Updated meta-analysis (Sheeran et al. 2025, 642 tests) confirms implementation intentions promote goal attainment (adjusted d=0.36 overall; contingent if-then format d=0.43 beats simple schedule format d=0.29) [Gollwitzer 2006][Sheeran 2025]. If-Then planning serves as an augmentation to the frozen **Dimension A** DayType/scheduling model, using the architecture: `DayType → scheduled routine/task → optional If-Then condition → contextual execution cue`. Exact implementation requires product validation. *Confidence: high (validated behavioral mechanism).*
- **Just-In-Time Adaptive Interventions (JITAI) show promising behavioral effects depending on study design.** Meta-analyses (Digital Health 2023; BMJ Mental Health 2025; JAMA Netw Open 2025 RCT) report a large pre-post effect (g ≈ 0.77) alongside a much smaller controlled comparison (g ≈ 0.15) [Digital Health 2023][BMJ Mental Health 2025][JAMA 2025]. JITAI is therefore a supported intervention framework, not a guaranteed large-effect mechanism. (JITAI Restraint Principle: contextual interventions should occur only when the system has sufficient evidence that intervention is useful; JITAI should reduce unnecessary interruption rather than become another source of notification overload). *Confidence: medium-high (evidence-backed mechanism).*
- **Repeated reliance on external reminders may reduce performance when reminders are absent.** Research (npj Science of Learning 2024) shows study reminders boost same-day goal completion (OR 1.77) but cause students to study LESS on non-reminder days than controls (OR 0.45), creating over-reliance [npj Sci Learn 2024]. Reminders are a documented double-edged mechanism; product implication: reminders are contextual support, not the sole execution mechanism. Habit automaticity develops gradually (median ~66 days, range 18–254 days; Lally et al. 2010), and missing a single day does not disrupt habit trajectory [Lally 2010]. *Confidence: high (documented double-edged mechanism for reminders; validated process for gradual automaticity).*

## H.2 Product Boundaries & Principles

- **Problem Validation Boundary**: The research validates several broad behavioral problems — procrastination, digital distraction, irregular routines, notification overload, and habit-formation challenges — but does not establish the prevalence or magnitude of every personal problem from the original user statement; specifically, frequency of missed critical notifications, missed interview opportunities, and specific personal habit categories remain unvalidated personal-use hypotheses rather than population-level facts.
- **Mechanism-to-Product Boundary**: Evidence that a behavioral mechanism works in controlled research does not establish that the exact implementation proposed in this application will produce the same effect; mechanisms should therefore inform product hypotheses and experiments rather than be presented as guaranteed product outcomes (e.g. controlled JITAI research works ≠ our JITAI will work automatically; If-Then planning works ≠ our routine builder will improve academic performance; Lally 66-day automaticity evidence ≠ our habit tracker creates habits in 66 days).

## H.3 Evidence Boundaries Matrix

| Finding / Topic | Evidence Status | Product Use | Evidence Boundary (What Evidence Does NOT Prove) |
|---|---|---|---|
| **Procrastination Prevalence** | Validated Problem (High) | Target task initiation & execution friction | Does not guarantee app will eliminate user procrastination. |
| **Notification Overload** | Validated Problem (High) | Restrain push delivery; prioritize attention | Does not prove critical notifications (OTPs, emails) are frequently missed. |
| **Critical-Notification Miss Frequency** | Unresolved Personal-Use Hypothesis | Option to pin/resurface important alerts | Population prevalence of missed critical alerts is unverified. |
| **Irregular Routines** | Validated Problem (High) | Day-Type template scheduling (Dimension A) | Does not prove app will prevent/treat depression or CVD. |
| **Digital Distraction** | Validated Problem (High) | Task-aware adaptive intervention ladder (B/G) | Does not prove 89% user-initiated check rate applies universally. |
| **Zeigarnik Effect** | Dead-End / Overstated Mechanism | AVOID UI built on unfinished-task tension | Classic recall-tension effect fails meta-analysis (dz=0.15). |
| **Ovsiankina / Task Resumption** | Validated Behavioral Mechanism | 1-tap resumption prompts after interruption | Does not guarantee user will complete resumed tasks. |
| **If-Then Implementation Intentions** | Validated Behavioral Mechanism | Augment DayType schedule with If-Then rules | Exact rule builder UX requires product validation. |
| **JITAI Interventions** | Evidence-Backed Mechanism (Medium-High) | State-aware in-the-moment prompts (B/G) | Controlled effect (g=0.15) is much smaller than pre-post (g=0.77). |
| **Reminder Dependence** | Documented Double-Edged Mechanism | Restrained contextual prompts, not constant spam | Reminders boost same-day work but lower non-reminder work (OR 0.45). |
| **Habit Automaticity (Lally)** | Validated Process (High) | Gradual automaticity design & miss tolerance | 66 days is a variable median (18–254d), not a fixed deadline. |
| **Exact 2-Day Recovery Rule** | Preliminary Product Hypothesis | Product heuristic for habit recovery | 2-day rule is an unvalidated product heuristic, not clinical law. |

---

# Dimension H — Product Changes We Should Carry Forward

*The strongest evidence-supported changes from Dimension H. Confidence levels, validation statuses, and open questions are preserved from the analysis; nothing below promotes an unvalidated hypothesis to a confirmed requirement.*

1. **If-Then Implementation Intentions.** Augment DayType schedule setup with contingent If-Then rules (`IF [trigger/context] THEN [action]`) rather than relying solely on static time alarms. *Status: validated behavioral mechanism (high confidence; exact UX requires validation).*
2. **Contextual JITAI Interventions over Constant Pushes.** Deliver in-the-moment prompts triggered by state changes rather than high-frequency push reminders (avoids habit automaticity erosion). *Status: evidence-backed mechanism (medium-high confidence; JITAI restraint principle applies).*
3. **Resumption Prompts (Ovsiankina Effect).** Offer 1-tap resumption prompts when interrupted, avoiding Zeigarnik anxiety assumptions. *Status: validated behavioral mechanism (high confidence).*
4. **Habit Automaticity & Miss-Tolerant Progression.** Design habit progression around gradual, variable automaticity rather than promising a fixed completion timeline; use miss-tolerant recovery as the default, while any specific 2-day rule or recovery threshold remains a preliminary product hypothesis requiring validation. *Status: validated process for gradual automaticity / preliminary hypothesis for exact recovery rule.*

**Open decisions carried forward (need validation):** default If-Then template library; JITAI trigger condition tuning; exact miss-recovery threshold rules.

---

# Cross-dimension insights

**Purpose:** Synthesize recurring, high-leverage findings and structural synergies that bridge multiple product dimensions (A through H) into overarching architectural and design principles.

## 1. Integrated Day-Type & Focus-State Aware Interruption Engine (A + B + G + H)
- **Insight:** Isolated app blockers fail because they treat distraction as a static schedule, while routine trackers fail because they lack moment-of-temptation enforcement.
- **Cross-Dimension Synthesis:** Dimension A establishes that daily routines vary by day-type (weekday, weekend, exam day), while the reviewed 2020 smartphone-interaction study found that 89% of observed smartphone interactions were user-initiated. Dimension B defines the adaptive intervention ladder, and Dimension G provides the technical implementation (`AccessibilityService` + `UsageStatsManager`). By linking task/focus state directly to the intervention policy (`DayType → Routine/Task → FocusSession → InterventionPolicy → blocked-app attempt → pause/substitute/escalation → completion`), the app becomes context-aware: blocking distracting apps only when the user is in an active focus or routine state, and releasing blocks upon session completion.

## 2. Scarcity & Escalation over Notification Bombardment (C + H + B)
- **Insight:** Flooding users with high-priority notifications and persistent nags leads directly to alert fatigue, notification clearing on sight, and channel revocation.
- **Cross-Dimension Synthesis:** Dimension H shows users receive ~63.5 notifications/day, and Dimension C proves each unit increase in notification disruption reduces acceptance odds by factor 0.581 (with 4-tier P1–P4 priority flags collapsing into inflation). Meanwhile, Dimension B demonstrates that OS-level opt-out friction (Pause Point direction) and deliberate override friction are the effective intervention models. The synthesis dictates a strict scarcity rule: scarcity of the Urgent delivery class is evidence-backed while the exact quota (for example 1–2 items/day) remains a preliminary product hypothesis (consistent with frozen **Dimension C**), using distinct sound/haptics for Urgent items and reserving the adaptive intervention ladder for active distraction moments rather than spamming push banners.

## 3. Earned Slack & Non-Punishing Companion vs. The "Streak Cliff" (D + E + F + H)
- **Insight:** Punishing failure (HP loss, hard streak resets to 0) creates catastrophic user dropout ("streak cliff"), task evasion, and app deletion.
- **Cross-Dimension Synthesis:** Hard punishment can create abandonment/evasion risk → streak slack is an evidence-backed mechanism → supportive companion behavior is an evidence-backed design direction → exact freeze/recovery parameters require validation. Consumer psychology (Silverman & Barasch 2023, JCR; Dimension E) and Habitica evaluation (IJHCS 2019; Dimension F) prove that rigid failure penalties trigger evasion and permanent abandonment. Conversely, peer-reviewed goal-slack research (Sharif & Shu 2017/2021; Dimensions E, F) supports streak slack as an evidence-backed mechanism for preserving goal continuity after setbacks, while exact parameters such as 1–2 freezes and a 3-day recovery window remain preliminary product hypotheses requiring validation. Combined with a single shared currency (Coins) and a guilt-free companion persona (Finch model; Dimension F), gamification acts as a supportive engine rather than an anxiety-inducing tyrant.

## 4. Contingent If-Then Triggers & Frictionless Capture over Static Reminders (A + C + E + H)

**Purpose:** Stress-test the proposed architecture against counter-evidence, failure modes, and operational risks identified during research.

## 1. Challenge: Will Day-Type Template Setup Create Onboarding Friction?
- **Counter-Evidence:** TimeTune's complex template engine receives user complaints about setup friction, and Structured's whole-day copy is restricted to manual copy-paste. If day-type setup requires tedious configuration, users will default to flat daily lists.
- **Mitigation / Guardrail:** Ship with 2 pre-configured default day-types (Weekday vs. Weekend) auto-assigned by calendar day of week. Allow 1-tap template overrides from the main dashboard without forcing upfront multi-template authoring.

## 2. Challenge: Does AccessibilityService App Blocking Face Play Store and User Trust Barriers?
- **Counter-Evidence:** Google Play Store policies scrutinize `AccessibilityService` declarations, and privacy-conscious users hesitate to grant full accessibility access to third-party apps.
- **Mitigation / Guardrail:** Enforce a strict 100% local-first, zero-internet architecture. Provide an explicit onboarding disclosure demonstrating that `AccessibilityService` is used exclusively for local package-name matching, with zero keystroke or screen content logging.

## 3. Challenge: Does Zero-Penalty Permissiveness Weaken Accountability?
- **Counter-Evidence:** Counseling reviews of Finch note that zero-consequence streaks can reduce urgency, allowing users to slip into passive procrastination. Conversely, Forest's session-level tree death demonstrates that mild loss aversion is highly motivating.
- **Mitigation / Guardrail:** Balance non-punishing long-term streaks (earned slack / freezes) with self-initiated session stakes (Forest-style focus commitment). Use the adaptive intervention ladder to increase friction upon repeated distraction attempts, providing accountability without catastrophic punishment.

## 4. Challenge: Will Voice Capture & NLP Suffer from Contextual Decay?
- **Counter-Evidence:** NoteWordy (CHI 2023) showed voice input accounts for <20% of entries due to social context and noise, and Siri reminder capture has documented failure rates.
- **Mitigation / Guardrail:** Treat voice capture as a secondary, complementary lane. Make one-line text quick-add the primary capture surface. Ensure all voice inputs pass through a confidence floor (low-confidence inputs file into an un-filed Inbox for 1-tap sorting).

---

# Recommendations: Evidence -> Insight -> Design Principle -> Workflow -> Product Requirement

**Purpose:** Translate empirical research findings into actionable, traceable product requirements.

| # | Evidence Base | Behavioral / Market Insight | Design Principle | Target End-to-End Workflow | Concrete Product Requirement |
|---|---|---|---|---|---|
| **1** | TimeTune day templates [34]; Structured day-copy [35]; YPT study stats [11,12]; Irregular routine health risks [h:11,13]. | Routine structures vary by day-type; study tracking without day-type context leads to fragmented logging. | **Day Type is a First-Class Schedule Engine.** | User selects/auto-maps DayType (e.g. Weekday) → App loads linked routine steps & study subjects → User completes steps & logs study time → Rollup heatmap by subject & DayType. | First-class `DayType` entity containing linked `RoutineStep` & `Subject` tags; 1-tap template swap; per-subject heat visual. |
| **2** | Opal/Freedom ladders [38,47]; one sec delay experiment [36]; Accessibility blocking [g:18]; Single-user override docs [54]. | Isolated blockers are easily turned off; interruption must activate during active focus/routine states and adapt to user behavior. | **Task-Aware Intervention over Static Restrictions.** | User starts Focus Session → `AccessibilityService` monitors target apps → User opens distracting app → Intentional delay/substitute prompt → Repeated attempt → Adaptive escalation block → Session completes → Apps unlock. | `InterventionPolicy` bound to active `FocusSession`/`RoutineStep`; `AccessibilityService` overlay redirect; adaptive intervention ladder; 1-tap cool-down override. |
| **3** | ~63.5 notifications/day [h:5]; factor 0.581 disruption penalty [Mehrotra CHI16]; 12× attendance lift from sound [Chang 2019]; Due nagging [r2:10]. | Flag systems collapse under priority inflation; loud pushes trigger channel revocation; urgency requires strict scarcity. | **Urgency Demands Scarcity.** | User marks task Urgent (Quota: Max 1–2/day) → Due time reached → Distinct sound/haptic alert fires → Unacknowledged → In-app auto-repeat nag (5-min interval, max 3 re-fires) → Auto-cool-down. | Strict single-tier "Urgent" status capped at max 2 active items; `setAlarmClock()` trigger; distinct sound/haptic profile; auto-repeat nag with retry cap. |
| **4** | Smits 2025 / Biwer 2023 RCTs (no productivity gain from enforced breaks) [d1:1, d2:8]; Motion AI opacity complaints [d1:15]; Sunsama planning [d1:19]. | Hard-locking breaks interrupts creative flow; opaque AI auto-rescheduling destroys user trust. | **Suggest Breaks, Confirm Reschedules.** | Focus session ends → App displays suggested break prompt with 1-tap "Keep Working" extension → Uncompleted task at end of day → App displays transparent "Do Tomorrow" shift confirmation modal. | Non-coercive break suggestion prompts; 1-tap break extension button; transparent "Do Tomorrow" shift confirmation UI for missed non-daily tasks. |
| **5** | Todoist NLP quick-add [e1:1,2]; Ramble voice capture 5× retention [e1:3]; NoteWordy speech speed (p=0.004) [e1:5]. | Entry friction causes task drop-off; text NLP is primary, while voice capture requires an un-filed Inbox safety net for ambiguous parses. | **Capture at the Speed of Thought.** | User types one-line text OR speaks voice input → Local parser extracts title/date/tag → High confidence: instant task creation with tap-to-revert pill; Low confidence: routes to un-filed Inbox. | One-line NLP quick-add parser (dates, times, tags); local voice capture engine (ML Kit / SpeechRecognizer); tap-to-revert UI; Inbox fallback queue. |
| **6** | Silverman & Barasch JCR 2023 streak cliff [e1:11]; Sharif & Shu OBHDP 2021 goal slack (+20% steps) [e1:8]; Finch D1/D7 retention (54%/37%) [f1:1]. | Resetting streaks to 0 and HP penalties cause task evasion and app deletion; earned slack and companion pets sustain retention without guilt. | **Support Through Failure, Reward Through Growth.** | User completes routine/task → Earns Coins & companion progress → Misses a day → Earned Streak Freeze auto-buffers miss (max 1–2/month) or 3-day recovery window opens → Companion offers encouragement. | Non-punishing mascot persona (zero HP loss); single reward currency (Coins); 1–2 earned monthly streak freezes; 3-day post-miss recovery UI. |

---

# Open questions + source appendix + staleness map

## 1. Open Questions (Carried Forward for Design/Spec Phase)
- **Day-Type Mapping UX:** Should DayType auto-switch based on calendar day-of-week (e.g. Saturday = Weekend) with 1-tap manual override, or require explicit user confirmation on morning launch?
- **Adaptive Intervention Escalation Tuning:** What is the exact threshold timing (e.g., 10s delay → 30s substitute activity → full 5m block) and opt-out cool-down duration (e.g. 2 min vs 5 min)?
- **Urgent Item Quota & Nag Cadence:** Is max 2 Urgent items/day the optimal hard ceiling? What is the ideal nag re-fire interval (5 min vs 10 min, max 3 re-fires)?
- **Earned Freeze Economy Rate:** How many completed active days should unlock 1 earned Streak Freeze (e.g. 7 days = 1 freeze, max 2 bankable)?
- **Android Accessibility Onboarding:** How to best frame the `AccessibilityService` permission prompt during onboarding to maximize user trust without obscuring purpose?

## 2. Additional Sources Appendix

| # | Topic / Supports | Publisher | Pub Date | Accessed | Confidence |
|---|---|---|---|---|---|
| [58] | Steel 2007 (Procrastination meta-analysis, 80-95% student prevalence) | APA Psych Bulletin | 2007 | 2026-08-12 | high (canonical) |
| [59] | Pielot et al. 2018 (794k notifications, non-social cleared on sight) | ACM MobileHCI | 2018 | 2026-08-12 | high |
| [60] | Mehrotra et al. 2016 (Disruption reduces acceptance by factor 0.581) | ACM CHI | 2016 | 2026-08-12 | high |
| [61] | Chang et al. 2019 (Audio/haptics drive 12x immediate attendance) | ACM MobileHCI | 2019 | 2026-08-12 | high |
| [62] | Smits et al. 2025 (Pomodoro RCT, enforced breaks increase fatigue slope) | MDPI Behav Sci | 2025-06 | 2026-08-12 | high |
| [63] | Biwer et al. 2023 (Pomodoro RCT, systematic breaks improve concentration) | BJEP (Wiley) | 2023-08 | 2026-08-12 | high |
| [64] | Albulescu et al. 2022 (Micro-breaks meta-analysis, vigor/fatigue lift) | PLOS ONE | 2022-08 | 2026-08-12 | high |
| [65] | Silverman & Barasch 2023 (Broken streaks suppress engagement, JCR) | J. Consumer Res | 2023-04 | 2026-08-12 | high |
| [66] | Sharif & Shu 2017/2021 (Goal slack / emergency reserves boost persistence) | JMR / OBHDP | 2017/2021 | 2026-08-12 | high |
| [67] | Yancey & Settles 2020 (Duolingo push notification bandit algorithm, KDD) | ACM SIGKDD | 2020-08 | 2026-08-12 | high |
| [68] | Lally et al. 2010 (Habit automaticity median 66 days, 1 miss ok) | Eur J Social Psych | 2010 | 2026-08-12 | high (canonical) |
| [69] | Sheeran et al. 2025 (Updated implementation intentions meta, d=0.36) | Eur Rev Social Psych | 2025 | 2026-08-12 | high |

## 3. Staleness Map

| Source Category | Canonical / Dated Reference | Fresh 2025–2026 Verification | Staleness Risk & Mitigation |
|---|---|---|---|
| **Procrastination Prevalence** | Steel 2007 (80-95% student rate) | MDPI Education Sciences 2024 meta-analysis (~60% academic self-report) | **Medium Staleness:** Canonical 2007 number is dated; use 2024 meta-analysis (N=55,477) for current baseline. |
| **Habit Automaticity** | Lally et al. 2010 (median 66 days) | Supported by 2024 npj Science of Learning & 2025 Sheeran meta | **Low Staleness Risk:** 66-day median remains undisputed canonical science. |
| **Implementation Intentions** | Gollwitzer & Sheeran 2006 (d=0.65) | Sheeran et al. 2025 meta-analysis (adjusted d=0.36; if-then d=0.43) | **Mitigated:** Updated to 2025 robust-Bayesian meta-analysis numbers. |
| **OS Capabilities & Limits** | Android 12/13 behavior docs | Android 15/16 docs, AOSP FSI limits, Android 17 jetstream checks | **Fresh (2026):** Verified against mid-2026 Android specs and iOS 26.2/26.4 releases. |
| **Duolingo Gamification Data** | Duolingo 2020 KDD bandit paper | Duolingo June 2026 Streak Revival event & 2026 Deconstructor of Fun report | **Fresh (2026):** Updated with 2026 event mechanics and 2026 retention teardowns. |





