# Antigravity Agentic Prompt — Personal-Tracker Design System Extraction

Copy the block in **"MASTER PROMPT"** below directly into Antigravity's task box (Manager Surface, so it can run as a long, multi-step autonomous task using its Browser Sub-Agent). Two honesty notes before you paste it, so the run doesn't surprise you:

1. **Antigravity's Browser Sub-Agent can only click through real, live web pages.** It cannot open the native Duolingo/Finch/Streaks/Opal *mobile apps* and tap around them the way a human tester would. What it *can* do is visit every **web-based surface** your research doc already cites for those apps — Mobbin's hosted screen pages, App Store/Play Store listing pages (which render real screenshots), Dribbble/Behance shot pages, and the UX-teardown articles (Screensdesign, Pratt IXD, UX Planet, Medium) that embed timestamped screenshots and named color/type observations. The prompt below routes it to exactly those pages per reference, and is explicit that "simulate clicking through the app" means *navigate every linked web surface for that reference and visually inspect every embedded screenshot*, not literally operate the native app.
2. **BMAD's exact command name for a "UI extraction" workflow isn't fixed across versions.** BMAD-METHOD ships a `ux-designer` agent, but the precise trigger (a numbered menu item, a `*` shortcut, or a custom workflow you've wired up) depends on your installed module version. The prompt tells the agent to activate `ux-designer`, run `/bmad-help` (or the agent's own menu) to confirm the live command list, and pick whichever menu item extracts/synthesizes a design system from references — rather than hardcoding a command name that might not exist in your install and silently fail.

---

## MASTER PROMPT (paste into Antigravity)

```
ROLE
You are acting as a senior Design Systems Engineer operating inside Antigravity's
agentic environment, with access to the Editor, Terminal, Browser Sub-Agent, and
(if the bmad-method package is installed in this workspace) the BMAD agent system.

GOAL
Read the file `personal-tracker-design-system-research.md` in this workspace end
to end, extract a complete, production-ready `design-system.md` for the Android
app "Personal-Tracker," and save it to the project root. Do not design or generate
any actual screens, components, or code in this task — output is the design-system
document only.

STEP 0 — Load context
1. Open and fully read `personal-tracker-design-system-research.md`.
2. Build an internal index of every reference cited in it: product name, category
   (Daily Schedule/DayType, Task & Habit, Focus/Deep Work, App Blocking, Notification
   & Urgency, Calendar/Timeline, Voice/NLP Capture, Gamification & Rewards,
   Companion/Mascot, Progress/Streak, Recovery/Rescheduling, Onboarding,
   Empty/Error/Success States, Motion & Interaction, Complete Design Systems),
   every URL attached to it, and its documented Adopt / Adapt / Avoid verdict.
3. Discard anything the research doc marked AVOID before extraction — do not pull
   design tokens from avoided patterns, even if they're visually appealing.

STEP 1 — If BMAD-METHOD is installed in this workspace
1. Activate the `ux-designer` agent.
2. Run `/bmad-help` (or the agent's own numbered menu) to list its current
   workflows. Identify whichever workflow is meant for extracting or synthesizing
   a design system from a set of references (naming varies by version — look for
   something like "extract design system," "UI audit," or "design token synthesis").
   If no such workflow exists in this install, say so explicitly in your final
   report and fall back to Steps 2–6 below using your own reasoning instead of a
   BMAD workflow. Do not invent or guess a command name that isn't actually in the
   agent's menu.
3. If a matching workflow exists, run it with the reference index built in Step 0
   as its input, then continue to Step 5 (Synthesis) using its output as a draft.

STEP 2 — Browser-based extraction pass (Browser Sub-Agent)
For EVERY reference in the index that has at least one real, navigable URL
(Mobbin screen pages, official app sites, App Store/Play Store listings, Dribbble/
Behance shot pages, UX-teardown articles with embedded screenshots):

1. Open the URL in the Browser Sub-Agent.
2. Take a full-page screenshot.
3. If the page links to additional screens/shots for the same product (e.g. a
   Mobbin app page with multiple screen thumbnails, an App Store screenshot
   carousel, a Behance project with multiple frames, a UX-teardown article with
   several timestamped images) — treat this as "simulating a user's navigation
   through the app": open/expand each linked screen or image in turn and
   screenshot it too, so you build a multi-screen picture of that product rather
   than judging it from one static shot.
4. From the screenshots and any surrounding text on the page, extract and log:
   - Approximate color palette (hex values where a color picker / inspectable CSS
     is available; otherwise named approximations, e.g. "warm coral accent,
     near-black background")
   - Typography impression: typeface family if named on the page, weight contrast
     (how bold vs. regular text is used), approximate type scale (how many
     distinct text sizes are visible), letter-spacing/casing conventions
   - Spacing/density impression: tight vs. generous padding, card gutter width
     relative to screen width, whether the layout reads dense or airy
   - Corner radius and elevation: sharp vs. rounded corners, flat vs. shadowed
     cards, how strong the shadow/elevation looks
   - Iconography style: line vs. filled, geometric vs. organic, weight
   - Illustration/mascot style if present: flat vs. 3D, color saturation, linework
   - Any motion/animation described in the surrounding article text (most native
     app motion won't be visible in a static screenshot — rely on the
     research doc's Section 14 motion notes plus any motion described in the
     teardown article's own text)
   - Component inventory visible on screen: buttons, chips, bottom sheets, cards,
     progress bars, badges, nav bars, list rows, empty states — note their shape
     and treatment
5. Log all of this per reference in a working extraction table before moving to
   the next reference. Do not skip a reference just because it only has one
   screenshot — log what's extractable from that one shot and note the limitation.
6. For references with NO real navigable URL (a few "directional only" entries in
   Sections 4, 11, and 13 of the research doc are flagged as under-documented) —
   do not fabricate visual data for these. List them explicitly as
   "insufficient source material for token extraction" in your final report
   instead of guessing.

STEP 3 — Cross-check against the research doc's own verdicts
For each reference, re-read its Adopt / Adapt / Avoid section in the research
doc BEFORE folding its extracted tokens into the synthesis:
- ADOPT → carry its tokens/patterns through largely as-is.
- ADAPT → carry the underlying pattern through, but note in your synthesis
  what specifically should change (tone, density, mechanic) per the research
  doc's own adaptation notes.
- AVOID → tokens already excluded in Step 0; do not reintroduce them here.

STEP 4 — Reconcile conflicting visual directions
The research doc's "Final Synthesis" section already establishes the intended
direction: a restrained, premium base (closer to Structured/CRED) with warmth
concentrated specifically in companion and reward moments (Finch/Fabulous), NOT
a uniformly bright/cartoonish palette (Duolingo/Habitica) and NOT a uniformly
cold/clinical one (Structured alone). Use this as the tie-breaker whenever two
references suggest conflicting palettes, density, or tone. Do not average every
reference equally — weight Tier 1 references above Tier 2, and Tier 2 above Tier 3,
per the research doc's own tiering.

STEP 5 — Synthesize the final design system
Produce ONE cohesive system (not a per-reference dump) covering:

1. **Foundations**
   - Color palette: primary/secondary/accent, semantic colors (success, warning,
     danger, info), neutral/grayscale ramp, full light AND dark mode variants
     (Android-first, so assume dark mode is a primary use case, not an
     afterthought), each color with a hex value and a one-line usage rule
   - Typography: font family recommendation(s) suited to Android (e.g. a
     Google Fonts-available family), full type scale (display/headline/title/
     body/label sizes), weight usage rules, line-height rules
   - Spacing: a base unit and full spacing scale (e.g. 4/8/12/16/24/32/48),
     applied consistently to padding, gutters, and component internal spacing
   - Grid & layout: column/margin rules for common Android screen widths,
     safe-area and density-bucket notes (mdpi/hdpi/xhdpi behavior if relevant)
   - Elevation & shadow: a defined elevation scale (flat, low, medium, high)
     with concrete shadow values for each, mapped to when each level is used
     (resting card vs. active/dragged bottom sheet vs. modal)
   - Corner radius scale: a small set of consistent radii and where each applies
   - Iconography: style rules (line weight, corner treatment, grid size),
     sourced/adapted from which reference(s)

2. **Illustration & Companion/Mascot style**
   - Visual style direction (flat/semi-flat/3D, linework, palette range) with
     an explicit decision on the Finch-vs-broader-appeal tension the research
     doc raised in Section 9
   - Expression/state range the mascot needs (idle, celebrating, concerned,
     reacting to a streak risk, etc.) mapped to the notification and
     companion-reaction use cases in Sections 5 and 9 of the research doc

3. **Motion & animation principles**
   - A short, explicit set of motion rules derived from Section 14's synthesis
     ("motion communicates real state change, never decoration")
   - Timing/easing guidance (rough duration bands for micro-interactions vs.
     screen transitions vs. reward reveals), including the CRED-sourced note
     on matching reward-reveal animation timing to device refresh rate rather
     than a fixed frame rate
   - Named motion patterns to implement: row-level completion state change
     (Habitify/Streaks), inline drag-to-reschedule (Structured), live-preview
     voice capture chips (Todoist Ramble), persistent draggable bottom sheet
     (Mobbin pattern), modal reward-reveal sheet, mascot reaction animation

4. **Core components** (spec each with states: default/pressed/disabled/loading
   where applicable, plus light+dark values)
   - Buttons (primary/secondary/text), chips/tags, cards (task, habit, routine),
     progress bar / streak flame or equivalent metaphor, bottom sheets (modal
     and persistent variants per Section 14), navigation bar, list rows,
     badges/rewards, input fields, the voice-capture entry affordance,
     empty-state and success-state templates (flagging clearly, per the
     research doc's own Section 13 gap note, that these are original proposals
     rather than extracted from a documented reference, since no strong
     reference existed for this category)

5. **Gamification visual system**
   - Coin/currency visual treatment, streak visualization (numeric + metaphor,
     per Section 10's synthesis), reward-reveal visual treatment, dual
     instant/deferred reward presentation per Section 8

6. **Accessibility**
   - Minimum contrast ratios for text/background pairs in both light and dark
     mode, minimum touch target size, and any state that must never rely on
     color alone (streak-risk warnings, blocked-app states)

STEP 6 — Output
1. Write the complete result to `design-system.md` in the project root.
2. Every section must cite which reference(s) it was derived from (by product
   name, matching the research doc's naming) so the design system stays
   traceable back to the research — do not present any token as if it came from
   nowhere.
3. End the file with a short "Gaps & Open Questions" section listing anything
   Step 2 flagged as insufficient source material, so this is visibly a living
   document rather than a false claim of completeness.
4. Do not proceed to implement any component in code. Stop after the document
   is written and present it for review.
```

---

## Why this prompt is structured this way

- **It routes the "click through the app" instruction to what's actually clickable.** Antigravity's Browser Sub-Agent is real and does navigate/click/screenshot live web pages — but your reference list is mostly native mobile apps documented *through* web pages (Mobbin, App Store, teardown articles). The prompt tells it explicitly to treat "navigating the app" as exhausting every linked screen/shot on those pages, so you get a faithful multi-screen pass instead of a shallow single-screenshot glance — without setting it up to fail trying to open a native iOS app it has no access to.
- **It won't silently invent a BMAD command.** Since I can't confirm the exact menu trigger your installed BMAD version uses for design-system extraction, the prompt has the agent check its own live menu first and explicitly report back if no matching workflow exists, rather than guessing a command name and failing partway through a long autonomous run.
- **It enforces the Adopt/Adapt/Avoid and Tier 1/2/3 logic you already did the work of establishing** — so the output doesn't flatten CRED, Duolingo, Structured, and Finch into an averaged, personality-less palette; it uses your research doc's own stated priorities as the tie-breaker.
- **It forces honesty about the two weak categories** (Recovery/Rescheduling visuals, Empty/Error/Success states) instead of letting the agent quietly fabricate a "reference-backed" answer for something your own research already flagged as under-sourced.

Want me to also draft the actual **starter `design-system.md` skeleton** (section headers + placeholder token tables) so Antigravity has a template to fill in rather than starting from a blank file?
