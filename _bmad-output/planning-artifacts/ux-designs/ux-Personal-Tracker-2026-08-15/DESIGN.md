---
name: Personal-Tracker
description: >
  UX Design Spine for Personal-Tracker v1 — a local-first, online/offline, network-optional
  Android productivity app with voice assistant, focus sessions, habit tracking, and a
  non-punishing companion pet.
status: draft
updated: 2026-08-15
sources:
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/prd.md
  - _bmad-output/planning-artifacts/prds/prd-personal-tracker-2026-08-14/addendum.md
  - docs/specifications/architecture.md
  - docs/specifications/TECHSTACK.md
  - docs/design/design-system.md

# Visual identity inherits from design-system.md.
# Tokens below reference the design system; only deltas are specified here.

colors:
  # Surfaces
  surface-base: { note: 'Inherited — {colors.surface-base} Light #F8F7F4 / Dark #121214' }
  surface-raised: { note: 'Inherited — {colors.surface-raised} Light #FFFFFF / Dark #1C1C1F' }
  surface-sunken: { note: 'Inherited — {colors.surface-sunken} Light #F0EEEA / Dark #0A0A0C' }
  surface-overlay: { note: 'Inherited — {colors.surface-overlay} Light rgba(0,0,0,0.4) / Dark rgba(0,0,0,0.6)' }
  # Primary
  primary: { note: 'Inherited — {colors.primary} Light #2563EB / Dark #60A5FA' }
  primary-light: { note: 'Inherited — {colors.primary-light} #3B82F6' }
  primary-subtle: { note: 'Inherited — {colors.primary-subtle} Light #DBEAFE / Dark #1E3A5F' }
  # Secondary
  secondary: { note: 'Inherited — {colors.secondary} Light #6366F1 / Dark #A5B4FC' }
  secondary-subtle: { note: 'Inherited — {colors.secondary-subtle} Light #E0E7FF / Dark #272566' }
  # Accent / Reward
  accent-warm: { note: 'Inherited — {colors.accent-warm} Light #D97706 / Dark #FBBF24' }
  accent-warm-subtle: { note: 'Inherited — {colors.accent-warm-subtle} Light #FEF3C7 / Dark #422006' }
  # Companion
  companion: { note: 'Inherited — {colors.companion} Light #059669 / Dark #34D399' }
  companion-subtle: { note: 'Inherited — {colors.companion-subtle} Light #D1FAE5 / Dark #064E3B' }
  # Semantic
  success: { note: 'Inherited — {colors.success} Light #16A34A / Dark #4ADE80' }
  warning: { note: 'Inherited — {colors.warning} Light #CA8A04 / Dark #FACC15' }
  danger: { note: 'Inherited — {colors.danger} Light #DC2626 / Dark #F87171' }
  info: { note: 'Inherited — {colors.info} Light #0284C7 / Dark #38BDF8' }
  # Ink
  ink-primary: { note: 'Inherited — {colors.ink-primary} Light #18181B / Dark #FAFAFA' }
  ink-secondary: { note: 'Inherited — {colors.ink-secondary} Light #52525B / Dark #A1A1AA' }
  ink-tertiary: { note: 'Inherited — {colors.ink-tertiary} Light #A1A1AA / Dark #71717A' }
  ink-disabled: { note: 'Inherited — {colors.ink-disabled} Light #D4D4D8 / Dark #3F3F46' }
  # DayType
  daytype-weekday: { note: 'Inherited — {colors.daytype-weekday} #3B82F6' }
  daytype-weekend: { note: 'Inherited — {colors.daytype-weekend} #8B5CF6' }
  daytype-college: { note: 'Inherited — {colors.daytype-college} #06B6D4' }
  daytype-special: { note: 'Inherited — {colors.daytype-special} #F59E0B' }
  # Currency & Streak
  coin-gold: { note: 'Inherited — {colors.coin-gold} Light #F59E0B / Dark #FBBF24' }
  coin-glow: { note: 'Inherited — {colors.coin-glow} rgba(245,158,11,0.2)' }
  streak-flame-core: { note: 'Inherited — {colors.streak-flame-core} #F97316' }
  streak-flame-tip: { note: 'Inherited — {colors.streak-flame-tip} #FDE047' }
  streak-cool: { note: 'Inherited — {colors.streak-cool} #94A3B8' }

typography:
  display: { note: 'Inherited — Plus Jakarta Sans 28sp Bold 700, -0.02em' }
  headline: { note: 'Inherited — Plus Jakarta Sans 22sp SemiBold 600, -0.01em' }
  title: { note: 'Inherited — Plus Jakarta Sans 18sp SemiBold 600' }
  body: { note: 'Inherited — Plus Jakarta Sans 15sp Regular 400, 0.01em' }
  body-medium: { note: 'Inherited — Plus Jakarta Sans 15sp Medium 500, 0.01em' }
  label: { note: 'Inherited — Plus Jakarta Sans 13sp Medium 500, 0.02em' }
  caption: { note: 'Inherited — Plus Jakarta Sans 11sp Regular 400, 0.03em' }
  data: { note: 'Inherited — Inter 13sp Medium 500, 0.01em' }
  data-large: { note: 'Inherited — Inter 32sp Bold 700, -0.02em' }

rounded:
  xs: { note: 'Inherited — 4px (checkboxes, toggles)' }
  sm: { note: 'Inherited — 8px (inputs, compact cards)' }
  md: { note: 'Inherited — 12px (standard cards, buttons, DEFAULT)' }
  lg: { note: 'Inherited — 16px (large cards, timeline blocks)' }
  xl: { note: 'Inherited — 20px (persistent bottom sheet corners)' }
  xxl: { note: 'Inherited — 24px (modal reward sheets, companion celebration cards)' }
  full: { note: 'Inherited — 9999px (pills, badges, FAB, progress bars)' }

spacing:
  '0.5': { note: 'Inherited — 2dp' }
  '1': { note: 'Inherited — 4dp' }
  '2': { note: 'Inherited — 8dp' }
  '3': { note: 'Inherited — 12dp' }
  '4': { note: 'Inherited — 16dp' }
  '5': { note: 'Inherited — 20dp' }
  '6': { note: 'Inherited — 24dp' }
  '8': { note: 'Inherited — 32dp' }
  '10': { note: 'Inherited — 40dp' }
  '12': { note: 'Inherited — 48dp' }
  '16': { note: 'Inherited — 64dp' }

components:
  hero-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    elevation: Low
    min-height: '120dp'
    note: 'Dynamic state-driven card at top of Home. States determine content.'
  task-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    accent-stripe-width: '3dp'
    accent-stripe-urgent-width: '6dp'
    accent-stripe-color: 'Delivery intensity color'
    elevation: Flat
    note: 'Left accent stripe driven by delivery intensity. Urgent variant uses danger red + lightning icon.'
  habit-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    accent-stripe-color: '{colors.secondary}'
    elevation: Flat
  routine-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    accent-stripe-color: '{colors.companion}'
    elevation: Flat
  study-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    accent-stripe-color: '{colors.info}'
    elevation: Flat
  voice-capture-fab:
    size: '56dp'
    background: '{colors.primary}'
    icon-color: '#FFFFFF'
    icon-size: '24dp'
    border-radius: '{rounded.full}'
    elevation: Medium
    listening-shape: 'Stadium pill (300ms morph)'
    note: 'Morphs to pill during listening state with pulsing waveform.'
  companion-widget:
    background: '{colors.companion-subtle}'
    border-radius: '{rounded.xxl}'
    padding: '{spacing.4}'
    note: 'Rive animation container. 7-state state machine.'
  coin-hud:
    background: '{colors.coin-glow}'
    text-color: '{colors.coin-gold}'
    font: '{typography.data}'
    border-radius: '{rounded.full}'
    padding-h: '{spacing.3}'
    padding-v: '{spacing.1}'
  bottom-sheet-persistent:
    background: '{colors.surface-raised}'
    border-radius-top: '{rounded.xl}'
    drag-handle-width: '32dp'
    drag-handle-height: '4dp'
    drag-handle-color: '{colors.ink-disabled}'
    snap-peek: '~100dp'
    snap-half: '~50%'
    snap-full: '~100%'
    elevation: Medium
  bottom-sheet-modal:
    background: '{colors.surface-raised}'
    border-radius-top: '{rounded.xxl}'
    backdrop: '{colors.surface-overlay}'
    elevation: High
    animation: 'Spring physics reveal'
  day-type-banner:
    background: '{colors.primary-subtle}'
    text-color: '{colors.primary}'
    border-radius: '{rounded.md}'
    padding: '{spacing.3}'
    note: 'Non-blocking morning banner with 1-tap swap.'
  resumption-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    border: '1dp solid {colors.border-default}'
    padding: '{spacing.4}'
    note: 'Ovsiankina resumption prompt with Resume/Review/Complete actions.'
  carry-forward-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.4}'
    note: 'Evening review with actions and advisory workload indicator.'
  confirmation-chips:
    background: '{colors.surface-sunken}'
    text-color: '{colors.ink-primary}'
    border-radius: '{rounded.full}'
    padding-h: '{spacing.3}'
    min-height: '32dp visual / 48dp hit-target'
    note: 'Medium-confidence capture editable chips.'
  unfiled-inbox-row:
    background: 'transparent'
    border-bottom: '1dp solid {colors.border-subtle}'
    padding-v: '{spacing.3}'
    note: 'Low-confidence transcript with draft attributes.'
  focus-timer:
    font: '{typography.data-large}'
    color: '{colors.ink-primary}'
    background: '{colors.surface-raised}'
    border-radius: '{rounded.full}'
  intervention-overlay:
    background: '{colors.surface-overlay}'
    blur: '16dp'
    note: 'Full-screen accessibility overlay for focus session distraction blocking.'
  notification-urgent:
    border-left: '4dp solid {colors.danger}'
    border-radius: '{rounded.md}'
    background: '{colors.surface-raised}'
  notification-important:
    border-left: '4dp solid {colors.warning}'
    border-radius: '{rounded.md}'
    background: '{colors.surface-raised}'
  notification-routine:
    border-left: '4dp solid {colors.info}'
    border-radius: '{rounded.md}'
    background: '{colors.surface-raised}'
  shop-item-card:
    background: '{colors.surface-raised}'
    border-radius: '{rounded.lg}'
    padding: '{spacing.3}'
    locked-overlay: '{colors.surface-overlay}'
  capacity-indicator:
    background: '{colors.surface-sunken}'
    fill-normal: '{colors.success}'
    fill-warning: '{colors.warning}'
    fill-overloaded: '{colors.danger}'
    height: '8dp'
    border-radius: '{rounded.full}'
---

## Brand & Style

Personal-Tracker is a **warm, supportive, grounded** Android productivity companion. It exists at the intersection of honest utility and emotional intelligence — a tool that understands structure without becoming structure's warden. Where most productivity apps weaponize calendars with streak guilt and notification anxiety, Personal-Tracker insists on something quieter: a framework for your day, a companion that celebrates your wins, and the unspoken assurance that an imperfect day is still a good day.

The visual language follows this philosophy. Calm surfaces in warm off-white (light) or deep neutral (dark). Generous breathing room between cards. A single warm accent (`{colors.accent-warm}`) that activates only when something is genuinely earned. Typography that prioritizes legibility over personality — **Plus Jakarta Sans** for approachable geometric UI text, **Inter** for honest tabular data where numbers matter. Material 3 foundations with carefully tuned design tokens that feel personal rather than corporate.

The companion pet lives at the emotional center of this aesthetic: supportive, never punishing, never disappointed. It celebrates, encourages, rests, and focuses alongside the user — but it never guilts. This single constraint informs every color choice, every animation, and every piece of microcopy in the system.

## Colors

The palette from `design-system.md` is the canonical source. Key application rules:

- **Surface hierarchy** (`{colors.surface-base}` → `{colors.surface-raised}` → `{colors.surface-sunken}`) structures depth. Cards float on `surface-raised`; inputs sit in `surface-sunken`. Shadows are a last resort — tonal separation does the work.
- **Primary** (`{colors.primary}`) is the functional action color: CTAs, active states, focused borders. Never decorative.
- **Accent Warm** (`{colors.accent-warm}`) is the **earned reward register**. It appears only when the user has genuinely accomplished something — coin arcs, milestone celebrations, reward sheets. Never used for navigation, status, or decoration.
- **Companion Green** (`{colors.companion}`) signals growth and nurturing. Used for the companion UI, habit streaks, and routine progress. Never used for financial or transactional elements.
- **Semantic colors** (`{colors.success}`, `{colors.warning}`, `{colors.danger}`, `{colors.info}`) are strictly functional. Danger red appears only for destructive actions and urgent delivery intensity — never for streak breaks or companion distress.
- **DayType accents** provide color-coded schedule differentiation without competing with semantic colors.
- **Ink hierarchy** builds information density without relying on font weight alone. `{colors.ink-primary}` for headlines and body, `{colors.ink-secondary}` for metadata, `{colors.ink-tertiary}` for timestamps, `{colors.ink-disabled}` for inactive elements.

**Avoid**: Using `{colors.danger}` for anything except genuinely destructive states. Gradients on any surface. Saturated accent fills behind text. Color-coding by mood, sentiment, or emotional state. Punitive color shifts on the companion widget.

## Typography

Two families serve distinct roles:

- **Plus Jakarta Sans** — All UI text. Approachable geometric sans-serif with excellent legibility at small sizes. The `{typography.display}` token is rare (onboarding, full-screen milestone reveals); `{typography.headline}` anchors screen titles; `{typography.body}` and `{typography.body-medium}` handle the bulk of content; `{typography.label}` and `{typography.caption}` serve metadata.
- **Inter** — Tabular data exclusively. Timer countdowns (`{typography.data-large}`), streak counters, coin balances, study hour breakdowns, and analytics (`{typography.data}`). Inter's tabular figures prevent layout shift as numbers change.

Dynamic type (Compose `sp` units) is honored at every level. The largest accessibility font setting must render legibly without truncation or control clipping.

## Layout & Spacing

4dp base grid from `design-system.md`. Key layout invariants:

- Horizontal screen padding: 16dp (`{spacing.4}`)
- Major section gap: 24dp (`{spacing.6}`)
- Card gutter (vertical): 12dp (`{spacing.3}`)
- Bottom Navigation height: 64dp + system gesture insets
- Minimum touch target: 48×48dp (chips with 32dp visual height expand hit-target to 48dp)

Single-column layout always. Modal stacks one level deep, never two. The persistent bottom sheet (DayType timeline) overlays the bottom portion of the screen with three snap points.

## Elevation & Depth

Elevation is used sparingly to reduce visual noise:

- **Flat** (0dp): Resting cards, list rows, navigation bar. The default.
- **Low**: Card press states, focused inputs. Subtle shadow.
- **Medium**: FAB, persistent bottom sheet. Enough to signal "floating."
- **High**: Modal bottom sheets, dragged cards, intervention overlay. Reserved for moments requiring spatial separation.

Hierarchy comes primarily from surface tone and typography weight, not shadow depth.

## Shapes

- `{rounded.md}` (12dp) is the default card corner radius — the workhorse.
- `{rounded.lg}` (16dp) for larger cards, timeline blocks, and section containers.
- `{rounded.xl}` (20dp) for persistent bottom sheet top corners.
- `{rounded.xxl}` (24dp) for modal reward sheets and companion celebration cards — emotional warmth.
- `{rounded.full}` (9999px) for FAB, pills, badges, and progress bar fills.
- `{rounded.xs}` (4px) for checkboxes and toggle switches.

Nothing in the system uses square corners. The aesthetic is "paper with soft edges."

## Components

Visual specifications. Behavioral rules live in `EXPERIENCE.md.Component Patterns`.

- **Hero Card** — `{colors.surface-raised}`, `{rounded.lg}`, `{spacing.4}` padding. State-driven content replaces the card's interior; the container remains stable. Low elevation to float slightly above the timeline.
- **Task Card** — `{colors.surface-raised}`, `{rounded.lg}`. 3dp left accent stripe in delivery intensity color (Routine: `{colors.info}`, Important: `{colors.warning}`, Urgent: `{colors.danger}` at 6dp width with lightning icon top-right). Completion triggers 300ms transition to `*-subtle` background + filled accent stripe + strikethrough text in `{colors.ink-tertiary}` + haptic pulse.
- **Habit Card** — `{colors.surface-raised}`, `{rounded.lg}`. `{colors.secondary}` left accent stripe. Streak flame visualization uses `{colors.streak-flame-core}` and `{colors.streak-flame-tip}`; frozen streak uses `{colors.streak-cool}`.
- **Routine Card** — `{colors.surface-raised}`, `{rounded.lg}`. `{colors.companion}` accent stripe. Step progress indicator.
- **Study Card** — `{colors.surface-raised}`, `{rounded.lg}`. `{colors.info}` accent stripe. Subject tag chip.
- **Voice Capture FAB** — 56dp circle, `{colors.primary}` background, white waveform icon. Listening state: 300ms morph to stadium pill with pulsing waveform + live chip preview. Review state: editable chips with Confirm/Edit buttons.
- **Companion Widget** — `{colors.companion-subtle}` background, `{rounded.xxl}`. Rive animation container with 7-state machine. Never shows distress, sadness, or guilt visuals.
- **Coin HUD** — `{colors.coin-glow}` background pill, `{colors.coin-gold}` text in `{typography.data}`. Persistent in top area.
- **Bottom Sheets** — Persistent: `{rounded.xl}` top corners, drag handle in `{colors.ink-disabled}`, no backdrop dim. Modal: `{rounded.xxl}` top corners, `{colors.surface-overlay}` backdrop, spring-physics reveal.
- **DayType Banner** — `{colors.primary-subtle}` background, `{colors.primary}` text, `{rounded.md}`. Non-blocking morning greeting.
- **Resumption Card** — `{colors.surface-raised}`, `{rounded.lg}`, subtle `{colors.border-default}` border. Ovsiankina-inspired return prompt.
- **Confirmation Chips** — `{colors.surface-sunken}` background, `{rounded.full}`, 32dp visual / 48dp hit-target. 1-tap to edit field.
- **Focus Timer** — `{typography.data-large}` in `{colors.ink-primary}`. Circular progress optional.
- **Intervention Overlay** — Full-screen `{colors.surface-overlay}` with 16dp blur. Friction ladder UI within.
- **Capacity Indicator** — 8dp horizontal bar. `{colors.success}` (normal), `{colors.warning}` (heavy), `{colors.danger}` (overloaded). Advisory only.
- **Shop Item Card** — `{colors.surface-raised}`, `{rounded.lg}`. Locked items show `{colors.surface-overlay}` tint with lock icon. Price in `{colors.coin-gold}`.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Use `{colors.accent-warm}` exclusively for earned rewards | Use warm accent for navigation, status, or decoration |
| Trust ink hierarchy for information density | Rely on font size alone to distinguish content levels |
| Honor 48dp touch targets on every interactive element | Make visual hit-targets smaller than 48dp (even if visual size is 32dp) |
| Support dark mode with equal rigor — test both | Treat dark mode as an inverted afterthought |
| Use `{colors.danger}` only for destructive actions and urgent delivery | Use red for streak breaks, missed habits, or companion distress |
| Let the companion be supportive in every state | Show a sad, angry, or disappointed companion — ever |
| Animate in response to user action or state change | Animate for decoration or attention-grabbing |
| Use tonal surface separation for depth | Over-rely on shadows for hierarchy |
| Keep completion animations brief (300ms) and satisfying | Make completion feel like an obligation (long mandatory sequences) |
| Let empty states feel restful ("All caught up") | Let empty states feel demanding ("Nothing here! Add something!") |
