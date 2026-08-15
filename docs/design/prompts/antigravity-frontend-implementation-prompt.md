# Antigravity Agentic Prompt — Personal-Tracker Frontend Implementation
### (Design tokens + Companion illustration/animation pipeline)

Paste the block in **"MASTER PROMPT"** into Antigravity's task box. Three honesty notes first — these matter more here than in the research/extraction prompt, because this one asks agents to *build*, not just *read*:

1. **No agent can autonomously draw your companion or rig a production Rive file.** Antigravity's agents (and BMAD's dev/architect agents) write code — they don't have a reliable pipeline for hand-illustrating a character in 7 consistent poses or building a polished Rive state machine with real artwork. Asking them to "create the illustrations" would produce something crude at best, or a silent placeholder passed off as final at worst. So this prompt deliberately splits the work: agents build the **entire technical pipeline** (state machine wiring, animation triggers, theming, the code that will consume the final art) using clearly-labeled **placeholder assets**, and separately produce a **creative brief** precise enough to hand to an illustrator, a Rive artist, or a generative-art tool as its own follow-up job. That's the honest scope for what agentic coding tools can do here.
2. **Antigravity's Browser Sub-Agent verifies web pages, not native Android screens.** For a Compose app, "browser-in-the-loop" verification doesn't apply directly — the prompt below routes verification to `adb`/emulator screenshots and Compose UI test capture instead, and says so explicitly, rather than quietly assuming a verification loop that doesn't exist for this platform.
3. **Same BMAD caveat as before applies to the `architect`/`dev`/`po` agents** — exact menu commands vary by install. The prompt has agents check their own live menu before running anything, and fall back to plain step-by-step execution if a named workflow doesn't exist.

---

## MASTER PROMPT (paste into Antigravity)

```
ROLE
You are acting as a senior Android frontend engineering team operating inside
Antigravity's Agent Manager, implementing the frontend foundation for the
"Personal-Tracker" Android app (Jetpack Compose). You may coordinate multiple
parallel agents for this task. If bmad-method is installed in this workspace,
use its architect/po/sm/dev agents for planning and story breakdown before
writing code.

GOAL
Read `design-system.md` in this workspace and implement the full design-token
system PLUS the illustration/animation delivery pipeline it specifies, using
this asset-track split (do not deviate from this split without flagging why):

  - COMPANION (7 expression states + transitions: Idle, Celebrating,
    Encouraging, Concerned, Focused, Sleeping, Excited) → Rive, via a state
    machine driven by a single "state" input.
  - ONE-SHOT REWARD MOMENTS (confetti burst, coin-earn arc, milestone
    spring-reveal) → Lottie, via lottie-compose.
  - STATIC ICONOGRAPHY (nav icons, chip icons, the 4-state flame icon:
    cold/warming/burning/blazing) → Android VectorDrawable XML, sourced from
    Material Symbols Rounded (weight 300) where a matching icon exists.
  - The coin-earn-to-HUD arc motion is a hand-built Compose animation (not
    Rive or Lottie) — flag it for extra QA per the device-refresh-rate note
    in design-system.md's Motion & Animation Principles section.

Do NOT attempt to generate final production illustration artwork or a
polished rigged Rive file yourself. Build the complete technical pipeline
against clearly-labeled PLACEHOLDER assets instead, and produce a creative
brief document precise enough for a human illustrator/Rive artist (or a
dedicated generative-art tool) to execute against later, without any code
changes required when the real assets land.

STEP 0 — Load context
1. Read `design-system.md` fully — the YAML frontmatter (colors, typography,
   rounded, spacing, components tokens) is the literal source of truth for
   every value you write. Do not invent or approximate a value that's already
   defined there.
2. Read the Motion & Animation Principles, Illustration & Companion/Mascot
   Style, and Gamification Visual System sections closely — these define the
   7 companion states, the two Lottie moments, and the flame states.
3. If a `companion-asset-brief.md` or `implementation-guide.md` already exists
   in this workspace, read it too and treat it as authoritative alongside
   design-system.md.

STEP 1 — Planning pass (BMAD if available)
1. If bmad-method is installed: activate the `architect` agent, give it
   design-system.md plus the asset-track split above, and have it produce or
   update `architecture.md` covering: module/package structure, the full
   dependency list (rive-android, lottie-compose, and any Compose theming
   libraries needed), and the CompanionState system design.
2. Then activate the `po` or `sm` agent (check its live menu — do not guess a
   command name) to shard the work into stories matching the six agent tracks
   in Step 2 below.
3. If BMAD isn't installed, or no matching planning workflow exists in its
   menu, write a short `implementation-plan.md` yourself covering the same
   ground, present it, and wait for confirmation before writing code.

STEP 2 — Parallel implementation (Agent Manager / Mission Control)
Run these as separate tracks — in parallel where Antigravity's Agent Manager
supports it, sequentially otherwise:

  TRACK A — Design tokens
  Transcribe design-system.md's YAML frontmatter 1:1 into Kotlin:
  Color.kt (every color token, light + dark), Type.kt (the full type scale as
  Compose TextStyle objects, both Plus Jakarta Sans and Inter families
  registered), Shape.kt (the corner-radius scale as Compose RoundedCornerShape
  objects), Spacing.kt (the spacing scale as dp constants), and
  AnimationTokens.kt (the five timing bands with their durations and easing
  curves as Compose AnimationSpec objects). If a value a later track needs
  isn't in design-system.md, flag it in your final report — do not invent it.

  TRACK B — Iconography
  Source real Material Symbols Rounded (weight 300) vector assets for every
  icon named or implied in design-system.md's component specs, convert to
  Android VectorDrawable XML at the specified sizes (24dp standard, 20dp
  compact, 32dp prominent). Build the 4-state flame icon set (cold/warming/
  burning/blazing) as VectorDrawables using the streak-flame-core and
  streak-flame-tip color tokens from Track A. These are static swaps — no
  runtime animation library needed here.

  TRACK C — Lottie (one-shot reward moments)
  Add the lottie-compose dependency. Build a reusable `RewardAnimation`
  composable accepting an asset name and a trigger boolean/state, wired to
  play once and report completion (so the caller knows when to reset
  companion state). Implement it against a clearly-labeled PLACEHOLDER Lottie
  JSON for both named moments (coin-earn micro version, ~300ms; milestone
  full-reveal version, ~800-1000ms) — either a trivial programmatically
  generated placeholder or a free LottieFiles placeholder explicitly marked
  as temporary in a code comment and in your final report. Leave the asset
  swap point (a single named resource reference) obvious so dropping in the
  real designer-authored file requires zero code changes.

  TRACK D — Rive (companion state machine)
  Add the rive-android dependency. Build a `CompanionView` composable that
  takes a `CompanionState` enum (Idle, Celebrating, Encouraging, Concerned,
  Focused, Sleeping, Excited — matching design-system.md's expression table
  exactly) and drives a Rive state-machine input named "state" to switch/blend
  between them. Implement it against a PLACEHOLDER .riv file — a trivial
  single-shape artboard with a state machine literally named "companion_sm"
  and an input literally named "state" with 7 values matching the enum, so
  the plumbing is fully testable end-to-end before final art exists. Do not
  present this placeholder as finished companion art anywhere in your report.

  TRACK E — Coin-arc animation (hand-built, needs extra care)
  Implement the coin-earn-to-HUD arc motion as a Compose `Animatable`,
  explicitly synced to VSYNC (`withFrameNanos` or the equivalent
  Compose animation API) rather than a fixed frame count or fixed duration
  assumption — this is the one motion in the whole system NOT covered by a
  library that handles refresh-rate sync for you, per design-system.md's own
  flag on this exact risk. If a high-refresh-rate emulator or device is
  available in this environment, verify visually at both 60Hz and 120Hz and
  report the result. If none is available, say so explicitly rather than
  claiming verification that didn't happen.

  TRACK F — Companion asset creative brief
  Produce `companion-asset-brief.md`: a precise, implementation-ready spec for
  a human illustrator or Rive artist. For each of the 7 states, give a pose/
  expression description (from design-system.md's expression table), the
  exact palette (max 4-5 named hex values from Track A's Color.kt), the
  legibility requirement (must read at 24dp notification-icon size), and the
  transition list between states that the state machine needs to support.
  Specify the exact delivery contract the artist must hit so Track D's code
  never needs to change: artboard name, state machine name ("companion_sm"),
  input name ("state"), and the 7 enum values verbatim. Include the same
  brief structure for the "adventuring" illustration scenes (Finch-style,
  per design-system.md) as a secondary, lower-priority section.

STEP 3 — Verification
1. If any of these components are also rendered in a web-based preview surface
   in this workspace (a Compose-web target, a component gallery, Storybook-
   equivalent), use the Browser Sub-Agent to open each state, screenshot it,
   and diff the observed colors/spacing/corner-radius against design-system.md's
   documented values. Flag any drift.
2. For native-Android-only surfaces, state explicitly that the Browser
   Sub-Agent cannot inspect them. Instead, verify via Terminal: run the app on
   an emulator/device, capture screenshots with `adb shell screencap`, and/or
   write Compose UI tests using `createComposeRule().onNodeWithTag(...)
   .captureToImage()` for automatable checks. Do not claim visual verification
   for native screens that only the browser agent performed.

STEP 4 — Output
1. Working code: Color.kt, Type.kt, Shape.kt, Spacing.kt, AnimationTokens.kt,
   the icon VectorDrawable set, RewardAnimation.kt, CompanionView.kt,
   CoinArcAnimation.kt — organized into a clear module/package structure.
2. `companion-asset-brief.md` as its own reviewable artifact.
3. `IMPLEMENTATION_STATUS.md` listing every piece built, and for each one:
   production-ready vs. placeholder-pending-real-assets. Nothing running on a
   placeholder may be marked "done" — flag it in both the code comments and
   this status file.
4. Stop after this. Do not wire these components into full app screens or
   navigation in this task — that's a separate implementation phase.
```

---

## Why this prompt is structured this way

- **It matches the asset-track split from the earlier review exactly** (Rive for stateful companion, Lottie for one-shot rewards, VectorDrawable for static icons) so the code architecture and the design rationale stay in sync — an engineer reading the code later can trace every library choice back to a documented reason.
- **It refuses to let an agent quietly fake illustration work.** Every track that touches the companion or reward art is required to use an explicitly labeled placeholder and to say so in the final report — this is the direct fix for the earlier traceability concern (the design-system.md's Reference Traceability table blurring "extracted" with "assumed"). Track F's creative brief is the honest deliverable for the part that genuinely needs a human.
- **It singles out the coin-arc animation for extra scrutiny** because it's the one motion in the system that doesn't get VSYNC-sync for free from a library — exactly the kind of detail that's easy to lose once a big implementation task is running autonomously.
- **It routes verification to what Antigravity can actually do for a native Android app** rather than assuming the browser-based verification loop that's described in Google's own Antigravity materials for web apps — a platform mismatch worth catching before the agent burns time on a verification step that silently does nothing.

Want me to also draft the **`companion-asset-brief.md` skeleton myself** right now — the 7-state pose/palette/transition table pre-filled from design-system.md — so Track F has a strong starting structure instead of building it from a blank page?
