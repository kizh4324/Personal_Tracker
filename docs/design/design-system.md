---
name: Personal-Tracker
description: >
  Android-first personal productivity companion — task, habit, and routine tracking
  with a supportive gamified companion, focus sessions, and intelligent notifications.
  Restrained premium base with warmth at reward and companion moments.
status: draft
updated: 2026-08-15

colors:
  # ─── Light Mode Surfaces ───
  surface-base: '#F8F7F4'
  surface-raised: '#FFFFFF'
  surface-sunken: '#F0EEEA'
  surface-overlay: 'rgba(0, 0, 0, 0.4)'

  # ─── Dark Mode Surfaces ───
  surface-base-dark: '#121214'
  surface-raised-dark: '#1C1C1F'
  surface-sunken-dark: '#0A0A0C'
  surface-overlay-dark: 'rgba(0, 0, 0, 0.6)'

  # ─── Primary ───
  primary: '#2563EB'
  primary-light: '#3B82F6'
  primary-subtle: '#DBEAFE'
  primary-dark: '#60A5FA'
  primary-subtle-dark: '#1E3A5F'

  # ─── Secondary ───
  secondary: '#6366F1'
  secondary-light: '#818CF8'
  secondary-subtle: '#E0E7FF'
  secondary-dark: '#A5B4FC'
  secondary-subtle-dark: '#272566'

  # ─── Accent / Reward (Warm) ───
  accent-warm: '#D97706'
  accent-warm-light: '#F59E0B'
  accent-warm-subtle: '#FEF3C7'
  accent-warm-dark: '#FBBF24'
  accent-warm-subtle-dark: '#422006'

  # ─── Companion / Growth ───
  companion: '#059669'
  companion-light: '#10B981'
  companion-subtle: '#D1FAE5'
  companion-dark: '#34D399'
  companion-subtle-dark: '#064E3B'

  # ─── Semantic: Success ───
  success: '#16A34A'
  success-subtle: '#DCFCE7'
  success-dark: '#4ADE80'
  success-subtle-dark: '#14532D'

  # ─── Semantic: Warning ───
  warning: '#CA8A04'
  warning-subtle: '#FEF9C3'
  warning-dark: '#FACC15'
  warning-subtle-dark: '#422006'

  # ─── Semantic: Danger ───
  danger: '#DC2626'
  danger-subtle: '#FEE2E2'
  danger-dark: '#F87171'
  danger-subtle-dark: '#450A0A'

  # ─── Semantic: Info ───
  info: '#0284C7'
  info-subtle: '#E0F2FE'
  info-dark: '#38BDF8'
  info-subtle-dark: '#0C4A6E'

  # ─── Ink / Text ───
  ink-primary: '#18181B'
  ink-secondary: '#52525B'
  ink-tertiary: '#A1A1AA'
  ink-disabled: '#D4D4D8'
  ink-primary-dark: '#FAFAFA'
  ink-secondary-dark: '#A1A1AA'
  ink-tertiary-dark: '#71717A'
  ink-disabled-dark: '#3F3F46'

  # ─── Borders ───
  border-default: '#E4E4E7'
  border-subtle: '#F4F4F5'
  border-default-dark: '#27272A'
  border-subtle-dark: '#1C1C1F'

  # ─── DayType Color Coding ───
  daytype-weekday: '#3B82F6'
  daytype-weekend: '#8B5CF6'
  daytype-college: '#06B6D4'
  daytype-special: '#F59E0B'

  # ─── Coin / Currency ───
  coin-gold: '#F59E0B'
  coin-gold-dark: '#FBBF24'
  coin-glow: 'rgba(245, 158, 11, 0.2)'

  # ─── Streak / Fire ───
  streak-flame-core: '#F97316'
  streak-flame-tip: '#FDE047'
  streak-cool: '#94A3B8'

typography:
  display:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 28sp
    fontWeight: 700
    lineHeight: 36sp
    letterSpacing: -0.02em
  headline:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 22sp
    fontWeight: 600
    lineHeight: 28sp
    letterSpacing: -0.01em
  title:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 18sp
    fontWeight: 600
    lineHeight: 24sp
    letterSpacing: 0em
  body:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 15sp
    fontWeight: 400
    lineHeight: 22sp
    letterSpacing: 0.01em
  body-medium:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 15sp
    fontWeight: 500
    lineHeight: 22sp
    letterSpacing: 0.01em
  label:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 13sp
    fontWeight: 500
    lineHeight: 18sp
    letterSpacing: 0.02em
  caption:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: 11sp
    fontWeight: 400
    lineHeight: 16sp
    letterSpacing: 0.03em
  data:
    fontFamily: 'Inter'
    fontSize: 13sp
    fontWeight: 500
    lineHeight: 18sp
    letterSpacing: 0.01em
  data-large:
    fontFamily: 'Inter'
    fontSize: 32sp
    fontWeight: 700
    lineHeight: 40sp
    letterSpacing: -0.02em

rounded:
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 20px
  xxl: 24px
  full: 9999px
  DEFAULT: 12px

spacing:
  '0.5': 2dp
  '1': 4dp
  '2': 8dp
  '3': 12dp
  '4': 16dp
  '5': 20dp
  '6': 24dp
  '8': 32dp
  '10': 40dp
  '12': 48dp
  '16': 64dp
  gutter: 12dp
  margin-mobile: 16dp
  section-gap: 24dp
  screen-padding-horizontal: 16dp
  screen-padding-vertical: 16dp

components:
  button-primary:
    background: '{colors.primary}'
    background-dark: '{colors.primary-dark}'
    text: '#FFFFFF'
    text-dark: '{colors.surface-base-dark}'
    border-radius: '{rounded.md}'
    height: 48dp
    padding-horizontal: '{spacing.6}'
    font: '{typography.body-medium}'
  button-secondary:
    background: 'transparent'
    border: '{colors.border-default}'
    border-dark: '{colors.border-default-dark}'
    text: '{colors.ink-primary}'
    text-dark: '{colors.ink-primary-dark}'
    border-radius: '{rounded.md}'
    height: 48dp
  button-text:
    background: 'transparent'
    text: '{colors.primary}'
    text-dark: '{colors.primary-dark}'
    height: 40dp
  chip:
    background: '{colors.surface-sunken}'
    background-dark: '{colors.surface-sunken-dark}'
    text: '{colors.ink-secondary}'
    text-dark: '{colors.ink-secondary-dark}'
    border-radius: '{rounded.full}'
    height: 32dp
    padding-horizontal: '{spacing.3}'
    font: '{typography.label}'
  chip-active:
    background: '{colors.primary-subtle}'
    background-dark: '{colors.primary-subtle-dark}'
    text: '{colors.primary}'
    text-dark: '{colors.primary-dark}'
  card-task:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    elevation: low
  card-habit:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    elevation: low
  card-routine:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    elevation: low
  progress-bar:
    track: '{colors.surface-sunken}'
    track-dark: '{colors.surface-sunken-dark}'
    fill: '{colors.companion}'
    fill-dark: '{colors.companion-dark}'
    height: 6dp
    border-radius: '{rounded.full}'
  bottom-sheet-persistent:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    border-radius-top: '{rounded.xl}'
    handle-color: '{colors.ink-disabled}'
    handle-color-dark: '{colors.ink-disabled-dark}'
    handle-width: 32dp
    handle-height: 4dp
    elevation: high
  bottom-sheet-modal:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    border-radius-top: '{rounded.xxl}'
    overlay: '{colors.surface-overlay}'
    overlay-dark: '{colors.surface-overlay-dark}'
    elevation: high
  navigation-bar:
    background: '{colors.surface-raised}'
    background-dark: '{colors.surface-raised-dark}'
    active-icon: '{colors.primary}'
    active-icon-dark: '{colors.primary-dark}'
    inactive-icon: '{colors.ink-tertiary}'
    inactive-icon-dark: '{colors.ink-tertiary-dark}'
    height: 64dp
    elevation: flat
  input-field:
    background: '{colors.surface-sunken}'
    background-dark: '{colors.surface-sunken-dark}'
    border: '{colors.border-default}'
    border-dark: '{colors.border-default-dark}'
    border-focus: '{colors.primary}'
    border-focus-dark: '{colors.primary-dark}'
    border-radius: '{rounded.md}'
    height: 48dp
    padding-horizontal: '{spacing.4}'
  voice-capture-button:
    background: '{colors.primary}'
    background-dark: '{colors.primary-dark}'
    icon-color: '#FFFFFF'
    border-radius: '{rounded.full}'
    size: 56dp
  coin-badge:
    background: '{colors.coin-glow}'
    text: '{colors.coin-gold}'
    text-dark: '{colors.coin-gold-dark}'
    border-radius: '{rounded.full}'
    icon-size: 20dp
  streak-badge:
    active-color: '{colors.streak-flame-core}'
    inactive-color: '{colors.streak-cool}'
    font: '{typography.data}'
---

## Brand & Style

Personal-Tracker is a single-user Android productivity companion that merges daily routines, task management, habit tracking, focus sessions, screen-time control, and intelligent notifications into one cohesive system — held together by a supportive companion mascot and a single universal reward currency (Coins).

The visual language solves a specific tension: the app must feel **calm and data-forward** during routine use (checking today's schedule, scanning a habit list), then shift into **warm, expressive moments** when the user completes something, earns a reward, or interacts with the companion. This is not a uniformly bright app, nor a uniformly dark-premium one — it's a restrained base that comes alive at earned moments.

*Derived from the research doc's Final Synthesis: Structured's calm information architecture + CRED's premium reward treatment + Finch's companion warmth, without collapsing into any one of their narrower identities.*

**Aesthetic posture:**
- **Neutral resting state**: Clean, low-chrome surfaces with generous breathing room. Data is the decoration — color-coded timeline blocks, completion states, and progress indicators carry the visual interest, not ornamental illustration or ambient animation. *(Structured)*
- **Activated state**: When the user completes a task, earns Coins, hits a streak milestone, or the companion reacts — warmth and expression switch on. Reward reveals use modal bottom sheets with focused animation; the companion's expression changes; Coins animate into the wallet. Then the app returns to calm. *(CRED, Finch, Duolingo)*
- **Never**: The whole screen buzzing with gamification at all times. Gamification is a deliberate accent, not ambient wallpaper. *(Anti-Habitica, per research doc's explicit AVOID on visual density)*

## Colors

### The palette logic

The palette serves three distinct registers, and each color belongs to exactly one:

1. **Structure register** — surfaces, text, borders. These are the neutral tones that build the app's information architecture. Warm enough to avoid feeling clinical (a lesson from Structured's calm-but-not-cold approach), cool enough to stay professional (a lesson from CRED's premium restraint).

2. **System register** — primary, secondary, semantic colors. These communicate system state: a selected tab, a focused input, a success/warning/danger condition. They are functional, not decorative.

3. **Expression register** — coin gold, streak flame, companion green, accent warm. These colors appear *only* at moments the user has earned: a reward reveal, a streak milestone, a companion reaction, a completion animation. Their visual warmth is the user's emotional payoff. When they appear, they feel earned. When they're absent, the app reads as calm.

### Per-color decisions

- **`surface-base` (#F8F7F4 light / #121214 dark)** — Slightly warm off-white in light mode (not clinical #FFFFFF), deep near-black in dark mode. Dark mode is treated as a primary use case for Android, not an afterthought. *(Structured's warm neutral + CRED's dark premium)*

- **`surface-raised` (#FFFFFF light / #1C1C1F dark)** — Cards, bottom sheets, and elevated surfaces. The light/dark delta between base and raised is subtle — hierarchy comes from layout and border, not dramatic tonal shifts. *(Quill/Structured pattern: paper-like surfaces, not flashy cards)*

- **`primary` (#2563EB light / #60A5FA dark)** — The primary action color. A clear, functional blue — not a personality color, a system color. Used for selected states, focused inputs, primary buttons, active navigation icons. Never decorative. Blue was chosen over Structured's iOS-blue (#007AFF) to have a slightly deeper, more intentional tone on Android's wider gamut. *(Adapted from Structured's functional blue accent)*

- **`secondary` (#6366F1 light / #A5B4FC dark)** — An indigo used sparingly for secondary differentiation: habit categories, filter chips, or DayType-specific timeline accents. This is not a second "brand" color — it's a second functional tone for cases where primary-only creates ambiguity.

- **`accent-warm` (#D97706 light / #FBBF24 dark)** — The "reward register" anchor. Coin icons, reward-reveal cards, special-milestone celebrations, the warm glow when the companion reacts to a completion. This color is earned, not ambient. Its appearance should feel like a small event. *(CRED's gold currency treatment + Finch's warm reward moments)*

- **`companion` (#059669 light / #34D399 dark)** — The companion mascot's signature color, used for companion-specific UI: the companion widget, the energy/growth bar, adventure progress, and companion reaction animations. A warm-leaning green (emerald, not lime) that reads as growth and nurturing without feeling like a "success" system color. *(Finch's growth-green + Forest's nature-green, adapted to be distinct from semantic success)*

- **`coin-gold` (#F59E0B light / #FBBF24 dark)** — Specifically the Coin icon and currency display color. Given legible weight by being tied to a real in-app value (not an abstract number), per CRED's documented pattern of anchoring currency to a comprehensible worth. The `coin-glow` (20% opacity) creates a soft highlight behind the coin in reward moments. *(CRED's coin wallet treatment)*

- **`streak-flame-core` (#F97316) / `streak-flame-tip` (#FDE047)** — Two-tone flame for the streak visualization. The core is a warm orange; the tip is a bright yellow. Together they create a campfire-style visual (per Fabulous's documented campfire-streak metaphor) that feels warmer and more tangible than Duolingo's flat flame icon. `streak-cool` (#94A3B8) is the inactive/frozen streak color — the visual "ember" state when a streak freeze is active. *(Fabulous's campfire + Duolingo's Streak Freeze, combined)*

- **`daytype-*` colors** — Four DayType-specific accent colors used exclusively in the timeline/schedule screen to color-code blocks by day type. Each is selected to be legibly distinct at small sizes and to pass WCAG contrast against both `surface-base` and `surface-raised`. *(Structured's color-coded timeline blocks, adapted to Personal-Tracker's DayType model)*

### What's NOT in the palette

- No saturated red/green/yellow as ambient background fills — these are semantic-only (danger/success/warning), never decorative. *(Anti-Duolingo: their bright fills work for a language app's playful tone but would undermine this app's calm resting state)*
- No gradient fills. Surfaces are flat tones. The only gradient is the streak flame's two-tone treatment, which is illustrative (a drawn flame), not applied as a UI surface gradient.
- No "cute pastel" ambient palette. Warmth is concentrated at earned moments (companion, rewards), not spread across every surface. *(Research doc's explicit guidance: "warmth as a deliberate accent, not the whole palette")*

## Typography

### Font selection

**Primary family: Plus Jakarta Sans** (Google Fonts, OFL license, free). A modern geometric sans-serif with slightly rounded terminals that create approachability without sacrificing professionalism. Available weights: ExtraLight 200 through ExtraBold 800, both roman and italic. Excellent x-height for small-screen legibility. Variable font available for Android's smooth weight interpolation.

**Data family: Inter** (Google Fonts, OFL license, free). Used exclusively for numeric-heavy contexts: timer displays, streak counters, coin amounts, study-hour breakdowns, analytics charts. Inter's tabular figures and precise geometry make numbers instantly scannable at small sizes. This pairing avoids the visual jarring of a single font being asked to serve both warm headings and cold data.

*Why not the reference apps' fonts: Structured uses SF Pro (Apple-only); CRED uses Inter for everything (too clinical for a companion-driven app); Finch uses custom hand-drawn lettering (charming but not reproducible without a bespoke typeface budget); Duolingo uses DIN Rounded/Feather (too playful for a premium base). Plus Jakarta Sans sits exactly at the intersection needed: geometric enough for data density, rounded enough for warmth.*

### Type scale

| Token | Family | Size | Weight | Line Height | Spacing | Use |
|---|---|---|---|---|---|---|
| `display` | Plus Jakarta Sans | 28sp | Bold 700 | 36sp | -0.02em | Onboarding headlines, reward-reveal titles |
| `headline` | Plus Jakarta Sans | 22sp | SemiBold 600 | 28sp | -0.01em | Screen titles, section headers |
| `title` | Plus Jakarta Sans | 18sp | SemiBold 600 | 24sp | 0em | Card titles, subsection headers |
| `body` | Plus Jakarta Sans | 15sp | Regular 400 | 22sp | 0.01em | Primary readable text, descriptions |
| `body-medium` | Plus Jakarta Sans | 15sp | Medium 500 | 22sp | 0.01em | Emphasized body text, button labels |
| `label` | Plus Jakarta Sans | 13sp | Medium 500 | 18sp | 0.02em | Chip text, tab labels, metadata |
| `caption` | Plus Jakarta Sans | 11sp | Regular 400 | 16sp | 0.03em | Timestamps, helper text, footnotes |
| `data` | Inter | 13sp | Medium 500 | 18sp | 0.01em | Inline metrics, table data, timer readouts |
| `data-large` | Inter | 32sp | Bold 700 | 40sp | -0.02em | Hero metrics (today's coin count, focus timer) |

### Rules

- **`display` is rare.** Used only in onboarding, reward reveals, and full-screen milestone celebrations — never as a screen title in normal navigation. *(Per Structured's lesson: keep data-screen typography small and functional)*
- **Negative letter-spacing on large sizes, positive on small.** Large text (display/headline) uses tighter tracking for visual density; small text (label/caption) uses looser tracking for legibility.
- **Android's sp unit is the spec.** All sizes in sp (scale-independent pixels), which means the type scale automatically respects the user's system-level font size preference. Every screen must remain usable at the "Largest" accessibility text size without layout breaking.
- **Never all-caps except for the `label` token** in specific UI contexts (tab bar labels, badge labels). All-caps body or headline text is prohibited.

## Layout & Spacing

### Base unit: 4dp

The entire spacing scale is built on a 4dp base, producing the sequence: 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 dp. This is an 8dp-dominant system (most component internals use 8/12/16dp) with a 4dp half-step available for tight groupings.

### Screen layout

- **Single-column layout** on all phone widths. No multi-column grids on mobile — even on large phones (430dp+), content spans the full width with consistent horizontal padding. *(Structured's single-column timeline pattern)*
- **Horizontal screen padding: 16dp** on both sides. This is the Android Material Design convention and matches every Tier 1 reference.
- **Section gap: 24dp** between major content sections within a scrolling screen.
- **Card gutter: 12dp** between vertically stacked cards (tasks, habits, routine items).
- **Bottom navigation safe area**: 64dp nav bar height + any Android gesture-nav inset. Content must never be obscured by the nav bar.

### Density philosophy

The resting state of the app is **medium density** — closer to Structured's airy timeline than Habitica's dense lists, but not as sparse as a meditation app. Enough items should be visible on one screen to give the user a sense of their day without scrolling, but each item should have enough breathing room to be individually tappable without precision.

*Specific density targets:*
- A single habit/task row: 56dp minimum height (48dp content + 8dp vertical padding)
- A timeline block on the DayType screen: 48dp minimum height for a 30-min block
- Maximum recommended items visible on a 400dp-wide, 800dp-tall screen without scrolling: 8–10 list items or 6–8 timeline blocks

## Elevation & Depth

Elevation is used sparingly — the app reads as mostly flat, with elevation reserved for specific interactive moments.

| Level | Shadow (light mode) | Shadow (dark mode) | Use |
|---|---|---|---|
| **Flat** | none | none | Resting cards, list rows, navigation bar |
| **Low** | `0 1dp 3dp rgba(0,0,0,0.08)` | `0 1dp 3dp rgba(0,0,0,0.24)` | Cards on press/hover, active input fields |
| **Medium** | `0 4dp 12dp rgba(0,0,0,0.10)` | `0 4dp 12dp rgba(0,0,0,0.32)` | Floating action button, persistent bottom sheet |
| **High** | `0 8dp 24dp rgba(0,0,0,0.14)` | `0 8dp 24dp rgba(0,0,0,0.40)` | Modal bottom sheets, reward-reveal overlays, dragged items |

### Rules

- **Resting cards are flat.** Task cards, habit rows, and routine blocks sit on `surface-raised` with a `border-subtle` divider or `border-default` outline — no resting shadow. The background tone difference alone (surface-raised vs. surface-base) creates enough layering. *(Structured's flat-card approach; research doc's Section 14 synthesis on avoiding decorative elevation)*
- **Elevation appears only during interaction.** A card being dragged on the timeline, a bottom sheet being pulled up, a reward-reveal modal appearing — these are the moments shadows activate.
- **Dark mode shadows are stronger** (higher opacity multiplier) because tonal contrast between surfaces is lower in dark mode, so shadows carry more of the layering burden.

## Shapes

### Corner radius scale

| Token | Value | Applied to |
|---|---|---|
| `xs` | 4px | Small interactive elements: checkboxes, toggle backgrounds |
| `sm` | 8px | Input fields, small cards, list item highlights |
| `md` | 12px | Standard cards (task, habit, routine), buttons, chips |
| `lg` | 16px | Large cards, timeline blocks, section containers |
| `xl` | 20px | Persistent bottom sheet top corners |
| `xxl` | 24px | Modal bottom sheet top corners, reward-reveal cards |
| `full` | 9999px | Pill shapes: avatar circles, coin badges, progress bar fill, FAB |

### Aesthetic logic

The app uses **generous but not extreme** rounding. Corners are soft enough to feel approachable (rejecting the cold precision of zero-radius enterprise UI) but not so round that every surface looks like a pill (rejecting the over-rounded trend that sacrifices information density for visual softness).

*(Structured's "soft rounded blocks" set the reference point — their 12-16px range for standard cards anchors the scale. Finch's 16-24px for companion-related surfaces provides the warmth-tier uplift.)*

**Rule**: corner radius increases with emotional warmth. Data-dense elements (inputs, small cards) use `sm/md`. Standard interactive cards use `md/lg`. Companion and reward surfaces use `xl/xxl`. This creates a subtle visual hierarchy where warmer, more expressive UI is literally softer-edged.

## Illustration & Companion/Mascot Style

### Companion visual direction

The companion is the emotional center of the app — it delivers reminders, celebrates completions, expresses concern during streak-risk moments, and creates a reason to return (via "adventures" it narrates after earning energy from completed tasks). Its visual style must thread a specific needle:

| Constraint | Source | Decision |
|---|---|---|
| Not exclusively "cute pastel" — needs broader appeal | Research doc Section 9 (Finch critique) | **Semi-flat illustration style with a clear, expressive face but not chibi/baby proportions** |
| Must carry distinct expression states | Duolingo's mascot expressions (Section 5) | **7 named expression states** (see map below) |
| Must work at notification icon size (24dp) AND full-screen moments | Duolingo + Finch | **Simple silhouette that reads at 24dp; detail appears at larger sizes** |
| Must feel native to a premium, restrained UI, not bolted-on | CRED's premium tone (Section 8) | **Muted color palette for the companion itself** — uses `companion` green and `accent-warm` amber, not the full rainbow |
| Must be shame-free and supportive, never guilt-inducing | Finch's explicit shame-free design (Section 9) | **No "sad/disappointed" expression state.** A "concerned" state reads as caring, not guilt-tripping |

### Expression state map

| State | Trigger | Visual description | Notification variant |
|---|---|---|---|
| **Idle / Content** | Default resting state | Relaxed posture, soft smile, gentle slow breathing motion | Standard notification icon (neutral) |
| **Celebrating** | Task/habit completed, coin earned, streak milestone | Arms up or equivalent joy gesture, warm glow aura using `accent-warm`, subtle confetti particles | Celebratory notification with companion face |
| **Encouraging** | User returns after absence, streak freeze used | Gentle wave or beckoning gesture, warm expression | Re-engagement notification: "Welcome back" tone |
| **Concerned** | Streak at risk, routine running behind schedule | Slightly furrowed brow, leaning forward, "checking in" posture — NOT sad, NOT guilt-tripping | Just-in-time warning (per Opal's streak-loss warning pattern) |
| **Focused** | Focus session active | Eyes closed or meditative posture, calm breathing | Dynamic Island / notification: session in progress |
| **Sleeping / Resting** | Past user's configured shutdown time, or off-day | Curled up, eyes closed, peaceful — signals "it's okay to rest" | No notification sent in this state |
| **Excited** | Rare: major milestone (7-day streak, 100 coins, level-up) | Bouncing or equivalent high-energy gesture, full celebration animation | Special milestone notification |

### Illustration rules

- **Flat to semi-flat.** No 3D rendering, no photorealistic textures. Clean vector paths with subtle shading (soft gradient on the companion body, not hard shadows).
- **Limited palette.** The companion uses at most 4–5 colors: `companion`, `companion-light`, a warm accent tone, the ink-primary, and white. No rainbow.
- **Expression is the primary visual signal.** The companion's face (especially eyes and mouth shape) is what communicates state — body posture is secondary. At 24dp notification size, only the face needs to be legible.
- **Adventuring illustrations** (companion returning from an "adventure" with a story/reward, per Finch's model) use the same flat style but with environment details: simple illustrated scenes using the DayType color coding as background tints.

## Motion & Animation Principles

### Governing rule

> **Motion communicates real state change resulting from a real user action. Never decoration.**

This is the single rule that governs every animation decision, derived from the research doc's Section 14 synthesis across all strong references (Habitify's row-level completion change, Streaks' incrementing counter, CRED's reward reveal, Forest's growing tree, Structured's inline drag). If an animation doesn't map to a state change the user just caused, it doesn't exist.

### Timing bands

| Category | Duration | Easing | Use |
|---|---|---|---|
| **Micro-interaction** | 150–250ms | `FastOutSlowIn` (Android standard) | Checkbox check, chip selection, row highlight, button press feedback |
| **State transition** | 300–400ms | `FastOutSlowIn` | Card completion state change, progress bar fill, bottom sheet snap |
| **Screen transition** | 350–500ms | `FastOutSlowIn` with shared-element where applicable | Screen-to-screen navigation, modal bottom sheet entrance |
| **Reward reveal** | 600–1000ms | Custom spring curve (overshoot + settle) | Coin earn animation, streak milestone, companion celebration, reward-card reveal |
| **Continuous session** | Varies (session length) | Linear or gentle ease-in-out | Focus session growth metaphor, streak flame flicker |

### Device refresh rate awareness

Reward-reveal animations (coin earn, scratch/spin mechanics, companion celebration) **must** be implemented using the device's actual refresh rate, not a fixed frame rate. Android's wide device-performance spread means a 60fps-designed animation looks choppy on 120Hz and janky on 30fps budget devices. Use `Choreographer.FrameCallback` (native) or the equivalent compose animation API that syncs to VSYNC.

*(This is a direct implementation lesson from CRED's documented experience — their gamified reward animations had a bug where scratch-card physics felt "unfairly fast" on high-refresh devices because they were tied to a fixed frame count rather than elapsed time. Section 8 of the research doc flags this explicitly.)*

### Named motion patterns to implement

| Pattern | Source reference | Description |
|---|---|---|
| **Row-level completion** | Habitify (Sec 2), Streaks (Sec 2) | When a habit/task/routine item is checked off, the row itself visually changes — not just a checkbox flip. The completion state (background tint, checkmark animation, text treatment) transitions smoothly. A small haptic pulse accompanies the visual. |
| **Inline drag-to-reschedule** | Structured (Sec 1) | Timeline blocks on the DayType screen can be long-pressed and dragged to a new time slot. During drag: the block lifts (elevation to High), other blocks shift to make room, and a time indicator follows the dragged block's top edge. |
| **Live-preview voice capture** | Todoist Ramble (Sec 7) | During voice input, parsed task attributes (title, date, priority, DayType) appear as chips in real-time as the user speaks. Each chip enters with a subtle scale-up animation as it's parsed. |
| **Persistent draggable bottom sheet** | Mobbin pattern library (Sec 14) | The DayType summary sheet is non-modal and persistent — it can be dragged between collapsed (peek), half-expanded, and full-expanded snap points. The drag handle provides the affordance. |
| **Modal reward-reveal sheet** | CRED (Sec 8), Mobbin (Sec 14) | When the user earns a reward (coins, streak milestone, companion evolution), a modal bottom sheet slides up with a dimmed overlay. The reward content reveals with a spring-physics animation. The companion's celebration plays inside this sheet. |
| **Companion reaction** | Finch (Sec 9), Duolingo (Sec 5) | On task completion: the companion's expression changes (idle to celebrating), a warm aura briefly appears around it using `accent-warm` at low opacity, and the earned Coins animate from the completed item toward the coin counter. This is a two-stage reward: immediate expression change + delayed coin-arrive animation. |
| **Streak flame pulse** | Fabulous (Sec 12) | The streak flame visualization gently pulses/flickers at the user's current streak intensity. At 0 days: no flame, just `streak-cool` ember. At 1–3 days: small, gentle flicker. At 7+ days: full flame with `streak-flame-core` and `streak-flame-tip`. At freeze-active: flame holds steady with a cool-blue tint. |
| **Focus session growth** | Forest (Sec 3) | During a focus session, a single visual metaphor (tied to the companion's world, not Forest's literal tree) grows slowly over the session duration. The growth is continuous and gentle — the metaphor IS the timer, not something alongside it. On premature exit: the growth reverses, but gently (not Forest's harsh "death" — per the adapt-not-adopt guidance). |

## Core Components

*Each component is specced with states: default / pressed / disabled / loading where applicable. Light and dark mode values are defined in the frontmatter `components` block; behavioral specs follow here.*

### Buttons

Three tiers: Primary (filled), Secondary (outlined), Text (minimal).

- **Primary** — Solid `primary` fill, white text. Used for one and only one action per screen (the main CTA). `rounded.md` corners. 48dp height minimum (touch target). On press: slight darkening + scale-down (98%) for 150ms.
- **Secondary** — Transparent background, `border-default` outline, `ink-primary` text. For secondary actions that need more prominence than plain text. Same sizing as Primary.
- **Text** — No background, no border. `primary` colored text. 40dp height (slightly smaller). For tertiary or inline actions. Underline on press for 150ms.
- **Disabled state** (all tiers): 40% opacity. No press feedback.

### Chips / Tags

Used for DayType labels, parsed voice-capture attributes, filter selections, and category tags.

- **Default**: `surface-sunken` background, `ink-secondary` text, `rounded.full` (pill shape). 32dp height.
- **Active/Selected**: `primary-subtle` background, `primary` text.
- **DayType chips**: Use the `daytype-*` color as a small left-edge dot (4dp circle) inside the chip, not as the chip's full background — keeping chips legible while still color-coded.

### Cards (Task, Habit, Routine)

All three share the same structural template — they differ only in their left-edge treatment and metadata:

- **Base**: `surface-raised` background, `rounded.lg` corners, `spacing.4` padding. Flat elevation at rest.
- **Left edge**: A 3dp-wide vertical accent stripe on the left edge, using the item's category color (DayType color for routines, priority-tier color for tasks, habit-category color for habits).
- **Completion state**: On completion, the card's background tints to the corresponding `*-subtle` color, the accent stripe fills fully, and the title text gets a strikethrough with `ink-tertiary` color. This transition is animated (300ms, per "Row-level completion" motion pattern).
- **Pressed**: Elevation lifts from Flat to Low (shadow appears). Background does not change.
- **Drag state**: Elevation lifts to High. Card scales to 102%. Other cards in the list shift to create a drop zone.
- **Urgent/Special card variant**: The accent stripe is wider (6dp) and uses `danger` color. A small lightning icon appears in the top-right corner. *(Research doc Section 5: urgent items need deliberately different visual weight)*

### Progress Bar / Streak Visualization

Two distinct progress visualizations:

1. **Linear progress bar** — Used for daily completion %, habit consistency, study session progress.
   - Track: `surface-sunken`, 6dp height, `rounded.full`.
   - Fill: `companion` color by default; `accent-warm` for coin-progress bars; semantic colors when showing warning/danger states.
   - Fill animation: smooth transition (300ms) on value change. *(Habitify's progress-bar pattern, Section 2)*

2. **Streak flame** — Used for the streak counter.
   - Not a bar but an illustrative flame icon with intensity mapped to streak length.
   - Sits alongside a numeric counter in `data` font.
   - States: cold (0 days, `streak-cool` gray), warming (1–3 days, small `streak-flame-core` only), burning (4–6 days, both core and tip visible), blazing (7+ days, full flame with subtle continuous flicker animation).
   - Freeze state: flame holds steady with a cool-blue (#60A5FA) tint overlaid, and a small snowflake icon appears next to the number.
   - *(Fabulous's campfire + Duolingo's streak flame + Duolingo's Streak Freeze, synthesized)*

### Bottom Sheets

Two variants, per Section 14 of the research doc:

1. **Persistent (non-modal)** — The DayType summary / "today at a glance" sheet.
   - Always present at the bottom of the main screen, draggable between snap points (peek: ~100dp showing drag handle + summary line; half: ~50% screen height showing today's timeline; full: full screen with all details).
   - `surface-raised` background, `rounded.xl` top corners, `border-subtle` top edge.
   - Drag handle: `ink-disabled` colored bar, 32x4dp, centered, `rounded.full`.
   - Does NOT dim the background. Content behind remains interactive when sheet is at peek or half.
   - *(Mobbin's Google Maps-style persistent sheet pattern; Section 14)*

2. **Modal** — Used for reward reveals, quick-capture confirmation, companion celebrations.
   - Slides up with `surface-overlay` dimming the background. Background is not interactive.
   - `surface-raised` background, `rounded.xxl` top corners (warmer, softer than persistent).
   - Dismissed via swipe-down, explicit close control, or system back gesture.
   - *(Mobbin's modal sheet pattern; CRED's reward-reveal card treatment)*

### Navigation Bar

Bottom navigation bar, Android Material 3 pattern.

- 4–5 destinations maximum: Home/Timeline, Habits, Tasks, Focus, Companion.
- `surface-raised` background. Flat elevation (no shadow — the bar's position and background tone are sufficient).
- Active icon: `primary` color, filled icon variant. Active label: `label` font, `primary` color.
- Inactive icon: `ink-tertiary` color, outline icon variant. Inactive label: `label` font, `ink-tertiary` color.
- 64dp height (excluding Android gesture-nav inset).
- Navigation transitions: shared-element transitions between screens where content continuity is meaningful (e.g., a task card on the home screen transitioning to the task detail screen).

### List Rows

Used for settings, companion adventure logs, analytics drill-downs.

- 56dp minimum height. Label left, value/chevron right.
- `body` font for label, `body-medium` or `ink-secondary` for value.
- Divider: `border-subtle`, inset from left edge by `spacing.4` (not full-bleed).
- Pressed state: `surface-sunken` background fill, 150ms.

### Badges / Rewards

- **Coin badge**: A pill-shaped badge (`rounded.full`) with a coin icon (20dp) and count in `data` font. Background: `coin-glow` (translucent amber). Text: `coin-gold`. Appears in the top-right of screens (coin wallet HUD).
- **Achievement badge**: Square with `rounded.lg` corners, a centered icon or illustration, title underneath in `label` font. Background varies by achievement tier.
- **Milestone badge**: Same as achievement but with a `accent-warm` border ring and a subtle shimmer animation on unlock.

### Input Fields

- `surface-sunken` background, `border-default` border, `rounded.md` corners.
- 48dp height, `spacing.4` horizontal padding.
- Focus state: border changes to `primary`, label floats above.
- Error state: border changes to `danger`, helper text below in `danger` color and `caption` font.

### Voice Capture Entry

- A floating action button (FAB) positioned above the bottom navigation bar, centered.
- `primary` background, white waveform icon (24dp).
- 56dp diameter, `rounded.full`.
- **Active state** (listening): FAB expands into a wider pill shape (animating from circle to stadium, 300ms). Waveform icon pulses rhythmically with speech amplitude. A live-preview area appears above the FAB showing parsed task attributes as chips (per Todoist Ramble's live-preview pattern).
- **Review state**: FAB contracts back to pill shape but stays expanded. Parsed chips are finalized and editable. Confirm/Edit buttons appear.

### Empty State Templates

> **Note**: The research doc's Section 13 explicitly flagged that no strong, named-product references exist for empty/error/success states. The following are **original proposals**, not extracted from documented references.

- **Empty habit list**: Companion in `idle` state, centered, with a speech bubble: "Let's start your first habit!" and a primary button: "+ Add Habit". No generic illustration, no sad emoji — the companion IS the empty state.
- **Empty task list**: Same pattern, companion speech: "Nothing on your list today. That's okay!" — framing emptiness as peaceful, not as a failure.
- **Empty timeline (no DayType configured)**: Companion in `encouraging` state, speech bubble: "Set up your first day type to see your schedule here." with a walkthrough CTA.
- **Error state**: Companion in `concerned` state, speech bubble with the specific error in plain language. A "Try Again" button in `primary`.
- **Success state (all done)**: Companion in `celebrating` state, fullscreen moment with warm `accent-warm-subtle` background tint, confetti particles, and "All done for today!" headline in `display` font.

## Gamification Visual System

### Coin / Currency

- **Visual**: A small, flat-style coin icon in `coin-gold`, with a subtle "C" or custom glyph embossed. Not a skeuomorphic 3D coin (too heavy for a restrained UI); not a bare number (too abstract, per CRED's lesson on anchoring currency to legible worth).
- **HUD placement**: Always visible in the top-right corner of main screens as a `coin-badge` component. Tapping it opens a wallet sheet showing total balance, recent earnings, and available redemptions.
- **Earn animation**: When coins are earned, small coin icons animate from the completed item (task card, habit row, etc.) along a curved path toward the coin HUD badge. The badge counter increments with each arriving coin. Duration: 600ms per coin, staggered if multiple.
- *(CRED's coin wallet + Duolingo's XP-earn animation, adapted for a calmer visual register)*

### Streak visualization

- **Numeric counter**: Always displayed alongside the flame icon in `data` font. "Day 7" format, not bare "7".
- **Flame metaphor**: See "Streak flame" component spec above.
- **Calendar heat-strip**: Below the flame on the streak detail view, a horizontal row of small circles for the last 14–30 days. Each day is colored: `companion` for completed, `streak-cool` for missed, `info` for freeze-used, transparent for future. *(Streaks app's calendar heat-strip, adapted for DayType-aware history — each day's tooltip shows which DayType was active)*
- **Streak freeze**: Visually, a small snowflake badge appears on the flame icon when a freeze is active. The flame's animation holds steady (no flicker) and takes on a cool-blue tint. The calendar heat-strip shows freeze days in `info` blue, distinct from completed (green) and missed (gray). *(Duolingo's Streak Freeze, with visual treatment adapted for a calmer aesthetic)*

### Reward reveal

Two tiers, matching the research doc's "dual instant/deferred reward loop" from CRED (Section 8):

1. **Instant reward** (every completion): Small coin-earn animation (micro-interaction tier, 300ms). Companion's expression shifts to `celebrating` briefly. No full-screen takeover — this happens inline on the current screen.

2. **Milestone reward** (streak milestones, level-ups, accumulation thresholds): Modal bottom sheet slides up. Inside: companion in `excited` state, the reward title in `display` font, and the reward content (new companion accessory, coin bonus, achievement badge) with a spring-physics reveal animation (reward starts small and scales up with overshoot). Confetti particles using `accent-warm`, `coin-gold`, and `companion` colors. Duration: 800–1000ms for the full reveal.

- **No scratch cards or spin wheels.** The research doc's adapt-not-adopt guidance on CRED specifically warns that "artificial scarcity around self-improvement rewards risks feeling manipulative in a wellbeing-adjacent product." Reward reveals are direct and celebratory, not gated behind chance mechanics.

## Iconography

### Style rules

- **Line icons** as the default — 1.5dp stroke weight, 24dp grid, rounded line caps and joins. *(Consistent with Structured's minimal iconography)*
- **Filled variants** for active/selected states in the navigation bar only. All other contexts use the line variant.
- **Geometric-organic hybrid**: Icons are built on a geometric grid but with slightly rounded corners and terminals (matching Plus Jakarta Sans's rounded terminals), so type and iconography feel native to each other.
- **24dp standard size.** 20dp compact size for inline icons (inside chips, badges, list rows). 32dp for prominent standalone icons (empty states, section headers).
- **Source recommendation**: Material Symbols (Rounded variant, weight 300) as the base icon set. Custom icons needed for: the companion face (notification icon), coin icon, flame/streak icon, DayType-specific icons, voice waveform.

## Accessibility

### Contrast requirements

- All text/background pairs **must** meet WCAG 2.1 AA contrast minimums:
  - Normal text (14sp or less): **4.5:1** minimum contrast ratio
  - Large text (>18sp or >14sp bold): **3:1** minimum contrast ratio
- Both light and dark mode palettes have been selected to meet these ratios. Specific pairs to verify:
  - `ink-primary` on `surface-base`: Light 14.5:1, Dark 15.2:1
  - `ink-secondary` on `surface-base`: Light 5.4:1, Dark 5.8:1
  - `primary` on `surface-base`: Light 4.8:1
  - `primary-dark` on `surface-base-dark`: Dark 5.1:1
  - `ink-tertiary` on `surface-base`: Light 3.4:1 (used only for `label` and `caption` — large enough or bold enough to meet 3:1 requirement)

### Touch targets

- **Minimum touch target: 48x48dp** for all interactive elements (buttons, checkboxes, list rows, chips, icons). This matches Android's Material Design accessibility guidelines and exceeds the WCAG minimum of 44x44.
- Chips (32dp visual height) must have **48dp total tap target** — the tap area extends beyond the visible chip boundaries.

### Color-independent states

The following states **must never rely on color alone** to communicate their meaning:

| State | Color signal | Non-color signal |
|---|---|---|
| Streak at risk | `warning` | Companion `concerned` expression + text label "Streak at risk" |
| Task overdue | `danger` | "Overdue" text label + different icon treatment |
| Habit completed | `success` | Checkmark icon + strikethrough text |
| Focus session active | `primary` | Timer display + "In Session" text + companion `focused` expression |
| App blocked | `danger` | Lock icon + explanatory text + companion intervention |
| Streak freeze active | `info` blue tint | Snowflake icon + "Freeze active" label |

### Screen reader support

- Every interactive element must have a `contentDescription` (Android) that describes its action, not its visual appearance.
- The companion's expression state must be announced as an accessibility label change when it transitions: "Companion is celebrating your completion" not just "image changed."
- Coin-earn animations must be accompanied by a TalkBack announcement: "Earned 5 coins."

---

## Do's and Don'ts

| Do | Don't |
|---|---|
| Use `accent-warm` and `companion` colors only for earned reward and companion moments — they should feel like events | Spread warm colors across ambient surfaces — warmth becomes noise |
| Keep the DayType timeline as the calm, data-forward resting state of the app | Gamify the timeline itself — streaks, badges, and animations belong on completion moments, not the schedule view |
| Use the companion's expression as the primary emotional signal in notifications and feedback | Use generic text-only notifications — route notification tone through the companion's "voice" |
| Let one Coin currency thread every feature together (tasks + habits + routines + focus + breaks all earn the same thing) | Create separate currencies per feature — fragmentation destroys the single-game feeling |
| Use a streak freeze/grace mechanic so one bad day doesn't erase weeks of investment | Hard-reset streaks to zero on a miss — this is the single most documented source of user churn and resentment |
| Implement motion that communicates real state change from a real user action | Add decorative animations, ambient particle effects, or background motion loops |
| Design the companion in a "concerned" state that reads as caring | Design a "sad" or "disappointed" companion state — the app must never add guilt |
| Test all reward-reveal animations at both 60Hz and 120Hz refresh rates | Use fixed-frame-count animations — CRED documented this exact bug |
| Keep bottom sheets to 5 options or fewer, or one focused interaction | Use bottom sheets for deep multi-step flows — route those to full screens |
| Show the companion in empty states as the emotional anchor instead of generic illustrations | Use stock "nothing here" illustrations with a generic sad character |

---

## Reference Traceability

Every design decision in this document traces back to one or more named references from the research phase. Below is the full traceability map:

| Design system element | Primary reference(s) | Tier | Verdict applied |
|---|---|---|---|
| Warm off-white / deep near-black surfaces | Structured, CRED | 1 | Adopt (both) |
| Functional blue primary accent | Structured | 1 | Adapt (deeper blue for Android) |
| Coin gold / reward warm accent | CRED | 1 | Adopt |
| Companion green accent | Finch, Forest | 1, 2 | Adapt (distinct from semantic success) |
| Streak flame two-tone | Fabulous, Duolingo | 2, 1 | Adopt (Fabulous) + Adopt (Duolingo freeze) |
| DayType color-coding | Structured | 1 | Adopt |
| Plus Jakarta Sans font | Original — no reference used this font | — | Original synthesis |
| Inter for data displays | CRED | 1 | Adapt (CRED uses Inter for everything; we scope it to data) |
| 4dp spacing base / 8dp dominant | Material Design, Structured | 1 | Adopt |
| Flat resting cards | Structured | 1 | Adopt |
| Generous corner radii (12-16dp standard) | Structured, Finch | 1 | Adopt |
| Persistent bottom sheet for DayType | Mobbin pattern library (Google Maps example) | 2 | Adopt |
| Modal bottom sheet for reward reveals | Mobbin, CRED | 2, 1 | Adopt |
| Companion semi-flat illustration style | Finch (adapted), Duolingo (expression range) | 1 | Adapt (broader appeal than Finch's "cute" style) |
| Companion expression states | Duolingo (mascot expressions), Finch (shame-free) | 1 | Adopt (Duolingo's range) + Adopt (Finch's shame-free principle) |
| No "disappointed" companion state | Finch | 1 | Adopt |
| One universal Coin currency | Duolingo (XP model) | 1 | Adopt |
| Streak freeze / grace mechanic | Duolingo (Streak Freeze) | 1 | Adopt |
| Dual instant/deferred reward loop | CRED | 1 | Adopt (structure), Avoid (scratch/spin chance mechanics) |
| No artificial scarcity in rewards | CRED (cautionary) | 1 | Avoid (daily spin caps) |
| Row-level completion animation | Habitify, Streaks | 2 | Adopt |
| Inline drag-to-reschedule | Structured | 1 | Adopt |
| Live-preview voice capture | Todoist Ramble | 2 | Adopt |
| Device-refresh-rate-aware animation | CRED (documented bug) | 1 | Adopt (implementation lesson) |
| Focus session single-metaphor growth | Forest | 2 | Adapt (companion-world metaphor, not literal tree) |
| Focus session gentle failure (no "death") | Forest (cautionary) | 2 | Avoid (harsh "death" animation) |
| Two-distinct-defer-actions (reschedule) | Sunsama | 3 (concept only) | Adopt (concept, not mobile execution) |
| Waiting room interstitial for app blocking | Opal | 2 | Adopt |
| Just-in-time streak-loss warning | Opal | 2 | Adopt |
| Companion-as-empty-state | Finch (implicit), Fabulous (micro-feedback) | 1, 2 | Original synthesis |
| Onboarding as companion origin story | Finch | 1 | Adopt |
| Calendar heat-strip for streak history | Streaks | 2 | Adapt (DayType-aware) |
| Sign-to-commit onboarding gesture | Fabulous | 2 | Adopt (optional, for pre-commitment psychology) |

---

## Gaps & Open Questions

This section explicitly lists areas where the design system is incomplete or where the research source material was insufficient, per the research doc's own honest gap flags:

### 1. Recovery / Rescheduling visual patterns
**Gap source**: Research doc Section 11 flagged this as under-documented. No strong, named-product reference with full UI detail was found.
**Current state**: The design system specifies the *actions* (two distinct defer options per Sunsama's pattern, a dedicated recovery screen state per Opal) and the *component specs* (cards, bottom sheets) these would use, but no reference-backed visual template exists for the recovery flow's specific screen layout.
**Recommended follow-up**: A dedicated Mobbin/Page Flows browsing session for "reschedule," "overdue," and "streak repair" tagged flows.

### 2. Empty / Error / Success state visual references
**Gap source**: Research doc Section 13 flagged this as under-documented. No named-product references with analyzable empty/error/success state screens were found.
**Current state**: The design system proposes companion-driven empty/error/success states as an original design direction (using the companion's expression states as the emotional anchor), but these are original proposals, not extracted from documented reference.
**Recommended follow-up**: Mobbin's and Page Flows' tagged "empty state" and "error state" screen libraries.

### 3. One Sec / soft-intervention UI detail
**Gap source**: Research doc Section 4 flagged as "directional only."
**Current state**: The design system recognizes tiered intervention severity (soft pause vs. hard block) as a design principle but has no visual spec for the soft-intervention interstitial screen.
**Recommended follow-up**: Hands-on app inspection of One Sec.

### 4. Companion species / character design
**Open question**: The design system specifies the companion's visual *rules* (semi-flat, limited palette, expression states, scaling behavior) but does not specify what *species* or *character* the companion is. The research doc notes Finch's bird is "divisive" and suggests user-selectable companion styles for broader appeal. This needs a design decision:
- Option A: A single, pre-defined companion character (less setup friction, stronger brand identity)
- Option B: User-selectable from 3–4 companion options at onboarding (broader taste appeal, per Finch critique)
- This decision should be made before illustration work begins.

### 5. Sound design
**Not covered in research**: The research doc does not cover audio/sound design. Fabulous's "sound + visual micro-feedback" (Section 12) is the only mention of sound. The design system currently specifies visual-only feedback. Haptic feedback is mentioned for row-level completion. A full sound design pass (completion sounds, notification tones, reward reveals, companion "voice") is a separate workstream.

### 6. Android-specific density bucket behavior
**Partially covered**: The design system uses dp/sp units and specifies minimum touch targets, but does not include per-density-bucket (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) asset export rules or drawable-size specifications. This is an implementation concern for the architecture phase, not a design-system gap — but it's flagged here so it's not forgotten.

### 7. Widget design
**Partially covered**: The research doc notes Streaks' "widget-first" approach (Section 2) as a strong pattern (streak visible without opening the app). The design system's color and typography tokens apply to widgets, but no widget-specific layout spec is included. Widget design should be a follow-up deliverable after the core app screens are designed.

---

*This design system is a living document. It is traceable to the research phase, honest about its gaps, and ready for the next phase: UX flow design (EXPERIENCE.md) and component implementation. No screens, components, or code have been produced in this document — output is the design-system specification only, per the extraction prompt's instruction.*
