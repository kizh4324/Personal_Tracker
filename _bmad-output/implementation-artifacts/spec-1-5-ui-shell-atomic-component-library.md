---
title: 'Story 1.5: UI Shell & Atomic Component Library'
type: 'feature'
created: '2026-10-16'
status: 'done'
baseline_commit: '1bf52b40eedec7244f1b932615aaf9dd3d413a36'
review_loop_iteration: 0
context:
  - ARCHITECTURE-SPINE.md
  - _bmad-output/project-context.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The presentation layer lacks a unified, token-strict global application shell and atomic/molecular UI component library. Without a persistent top context bar, root-hoisted unclipped motion stages, a confidence-routed quick capture sheet, and state-aware cards, feature screens will implement fragmented, inconsistent UI with clipped animations and hardcoded literals violating AD-7 and AD-8.

**Approach:** Implement atomic primitives (`StreakFlameBadge`, `CoinPill`, `DayTypeChip`, `WaveformVisualizer`), stateless molecular cards (`HeroActionCard`, `HabitStreakCard`, `AdaptiveRoutineStepCard`, `TaskIntensityCard`), global overlays (`QuickCaptureSheet`, `RewardMotionStage`), and `AppShellScaffold` with 5-tab navigation, strictly referencing `com.personaltracker.ui.theme` tokens and enforcing a 48dp minimum touch target floor.

## Boundaries & Constraints

**Always:**
- Strictly follow AD-7: All UI elements must resolve dimensions, colors, typography, shapes, and elevations exclusively from `com.personaltracker.ui.theme` tokens (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Elevation.kt`, `AnimationTokens.kt`). Zero hardcoded hex colors, `dp`, or `sp` literals.
- Strictly enforce 48dp minimum touch target floor (`PersonalTrackerTheme.spacing.minTouchTarget`) for all clickable and interactive composables.
- Strictly follow AD-7: Hoist `RewardMotionStage` (`CoinArcAnimation.kt` + `RewardAnimation.kt`) at the root scaffold level above the navigation host to prevent layout bounds clipping across nested scroll containers.
- Strictly follow AD-8: `QuickCaptureSheet` must provide confidence-tiered UI: High-confidence (>=0.85) 5-second undo toast pill, Medium-confidence (0.50..0.85) 1-tap editable confirmation chips (`Title`, `Date`, `Time`, `Type`, `Urgency`), and Low-confidence (<0.50) unfiled inbox routing.
- Strictly follow non-punishing copy rules: Zero guilt, shaming, or negative copy in UI strings; companion expressions contain 0 HP loss, starvation, or negative emotional states.
- Support full dark/light theme parity with WCAG 2.1 AA contrast compliance and TalkBack accessibility semantics.

**Ask First:**
- Any modification to existing database entities, DAOs, or domain engine contracts.
- Alterations to existing theme color definitions or typography scales.

**Never:**
- Never use raw numeric literals (`16.dp`, `14.sp`, `#FFFFFF`) in composable definitions.
- Never place reward animations inside scrolling item cards or nested viewport-clipped containers.
- Never write nag, guilt, or punitive copy in UI strings or notifications.
- Never import Android framework classes into `com.personaltracker.domain`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Streak Flame Progression | Streak count: 0, 2, 5, 14 days | Displays `ic_flame_cold` (streakCool), `ic_flame_warming` (streakFlameTip), `ic_flame_burning` (streakFlameCore), `ic_flame_blazing` (streakFlameCore + glow) with formatted streak number | Clamps negative counts to 0 |
| Coin Pill Target Measurement | Top Context Bar rendered in window | Measures global window coordinates via `onGloballyPositioned` and exposes `Offset` for `CoinArcAnimation` destination | Defaults to top-right fallback offset if unmeasured |
| Hero Card 6-State Transitions | State changes among ActiveFocus, InterruptedResumable, MorningBanner, UpcomingItem, CarryForwardReview, IntentionalIdle | Smooth 300ms `FastOutSlowIn` cross-fade transition while card container dimensions remain stable | Priority conflict resolves to highest active state |
| Task Delivery Intensity Stripe | DeliveryIntensity: ROUTINE, IMPORTANT, URGENT | ROUTINE: 3dp `colors.info`; IMPORTANT: 3dp `colors.warning`; URGENT: 6dp `colors.danger` + lightning badge | Defaults to ROUTINE stripe if invalid intensity string |
| High-Confidence Quick Capture | NLP Confidence >= 0.85 | Displays 5-second bottom undo toast banner ("Added: {title}" + [Undo] action) | Auto-dismisses after 5s or on new capture |
| Medium-Confidence Quick Capture | NLP Confidence 0.50..0.85 | Displays 1-tap confirmation chips row (`Title`, `Date`, `Time`, `Type`, `Urgency`) with Confirm and Discard actions | Persists until explicit user action |
| Unclipped Reward Arc Animation | Coin reward event triggered with item source offset | `RewardMotionStage` renders quadratic Bézier coin arc from item source offset to `CoinPill` target offset above navigation host | No clipping at scroll container boundaries |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/ui/components/atoms/StreakFlameBadge.kt` -- Streak flame badge binding 4 flame states to drawables (`ic_flame_cold`, `ic_flame_warming`, `ic_flame_burning`, `ic_flame_blazing`) with data typography.
- `app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt` -- Coin HUD pill binding `ic_coin`, displaying balance, and reporting global window coordinates for coin arc animations.
- `app/src/main/java/com/personaltracker/ui/components/atoms/DayTypeChip.kt` -- DayType chip displaying active template name and color coding with 48dp minimum touch target.
- `app/src/main/java/com/personaltracker/ui/components/atoms/WaveformVisualizer.kt` -- Audio amplitude visualizer using `ic_waveform` and theme primary colors for voice capture feedback.
- `app/src/main/java/com/personaltracker/ui/components/cards/HeroActionCard.kt` -- State-aware Hero Card container machine managing 6 priority states with 300ms `FastOutSlowIn` transitions.
- `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- Stateless habit card with flame badge, freeze indicator, and 48dp completion toggle.
- `app/src/main/java/com/personaltracker/ui/components/cards/AdaptiveRoutineStepCard.kt` -- Stateless routine step progression card with order, duration, status dimming, and step advancement action.
- `app/src/main/java/com/personaltracker/ui/components/cards/TaskIntensityCard.kt` -- Stateless task card with 3dp/6dp delivery intensity accent stripes, completion strikethrough, and carry-forward badge.
- `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Universal quick capture floating drawer with voice/text input, medium-confidence confirmation chips, and 5s undo toast banner.
- `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- Viewport-hoisted motion layer hosting unclipped `CoinArcAnimation` and `RewardAnimation` at the root scaffold level.
- `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- Global application scaffold integrating persistent Top Context Bar, 5-tab navigation, root `RewardMotionStage`, and `QuickCaptureSheet`.
- `app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt` -- Refactored to host within `AppShellScaffold` with strict design tokens (replacing hardcoded literals).
- `app/src/test/java/com/personaltracker/ui/components/ComponentStateTest.kt` -- Unit tests verifying atomic and card component state models, flame transitions, confidence tiers, and hero card priority evaluations.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/StreakFlameBadge.kt` -- Implement 4-state flame badge binding `FlameState` to drawables, colors, and data typography.
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt` -- Implement coin pill with `ic_coin`, gold typography, and `onGloballyPositioned` target coordinate measurement.
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/DayTypeChip.kt` -- Implement DayType chip with color mapping (`daytypeWeekday`, `daytypeWeekend`, `daytypeCollege`, `daytypeSpecial`) and 48dp touch target.
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/WaveformVisualizer.kt` -- Implement audio amplitude visualizer with animated waveforms and theme tokens.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/HeroActionCard.kt` -- Implement state-aware Hero Card machine managing 6 priority states with 300ms `FastOutSlowIn` transitions.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- Implement stateless habit streak card with flame badge, freeze indicator, and 48dp toggle.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/AdaptiveRoutineStepCard.kt` -- Implement stateless routine step card with step progression, duration, and advancement action.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/TaskIntensityCard.kt` -- Implement stateless task card with delivery intensity accent stripes (Routine 3dp, Important 3dp, Urgent 6dp + lightning), completion strikethrough, and carry-forward badge.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Implement quick capture overlay supporting dual voice/text inputs, medium-confidence editable chips, and 5s undo toast.
- [x] `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- Implement root-hoisted motion stage hosting `CoinArcAnimation` and `RewardAnimation` without scroll clipping.
- [x] `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` & `PersonalTrackerNavHost.kt` -- Wire Top Context Bar, `RewardMotionStage`, `QuickCaptureSheet`, and 5-tab navigation into `AppShellScaffold` with 100% token compliance.
- [x] `app/src/test/java/com/personaltracker/ui/components/ComponentStateTest.kt` -- Implement comprehensive state and contract unit tests for all components.

**Acceptance Criteria:**
- Given the 4 flame states (COLD, WARMING, BURNING, BLAZING), when `StreakFlameBadge` renders, then the matching drawable (`ic_flame_cold`, `ic_flame_warming`, `ic_flame_burning`, `ic_flame_blazing`) and theme token colors are bound without hardcoded literals.
- Given the `CoinPill` composable in the Top Context Bar, when laid out, then its global window coordinates are measured and made available for `CoinArcAnimation` targeting.
- Given `HeroActionCard`, when state transitions between any of the 6 priority states occur, then content cross-fades over 300ms with `FastOutSlowIn` easing while the card container bounds remain stable.
- Given `TaskIntensityCard`, when delivery intensity is ROUTINE, IMPORTANT, or URGENT, then the left accent stripe renders at 3dp (info), 3dp (warning), or 6dp (danger + lightning) respectively.
- Given a medium-confidence capture (0.50..0.85), when `QuickCaptureSheet` renders proposal, then 1-tap editable confirmation chips for Title, Date, Time, Type, and Urgency are displayed with Confirm/Discard actions.
- Given a coin reward event dispatched anywhere in the app, when `RewardMotionStage` executes, then the coin arc animates from the item position to the Top Context Bar `CoinPill` target coordinates above the navigation viewport without clipping.
- Given all composables in `ui/components/` and `ui/navigation/`, then zero hardcoded `dp`, `sp`, or hex literals exist, and all interactive touch targets meet the 48dp floor.

## Spec Change Log

## Design Notes

- Hero Card 6 Priority Hierarchy: 1) ActiveFocus, 2) InterruptedResumable, 3) MorningBanner, 4) UpcomingItem, 5) CarryForwardReview, 6) IntentionalIdle.
- Touch Target Floor: All clickable surfaces enforce `Modifier.defaultMinSize(minWidth = PersonalTrackerTheme.spacing.minTouchTarget, minHeight = PersonalTrackerTheme.spacing.minTouchTarget)`.
- Accent stripe widths: Standard 3dp (`PersonalTrackerTheme.spacing.spacing0_5 + PersonalTrackerTheme.spacing.spacing0_5 / 2` or `PersonalTrackerTheme.spacing.spacing1 - PersonalTrackerTheme.spacing.spacing0_5 / 2`), Urgent 6dp (`PersonalTrackerTheme.spacing.progressBarHeight`).
- Zero hardcoded colors/dp: All colors from `PersonalTrackerTheme.colors`, all spacing from `PersonalTrackerTheme.spacing`, all shapes from `PersonalTrackerTheme.shapes`, all typography from `PersonalTrackerTheme.typography`.

## Verification

**Commands:**
- `.\gradlew test` -- expected: BUILD SUCCESSFUL, all unit tests pass on JVM.
- `.\gradlew lintDebug` -- expected: BUILD SUCCESSFUL with 0 errors.
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: ok: true, 0 findings.

## Suggested Review Order

**Global App Shell & Unclipped Motion Stage**

- Main application shell integrating top context bar, 5-tab navigation, hoisted motion, and quick capture
  [`AppShellScaffold.kt:54`](../../app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt#L54)

- Root-hoisted viewport overlay hosting unclipped CoinArcAnimation and RewardAnimation above navigation viewports
  [`RewardMotionStage.kt:90`](../../app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt#L90)

- Universal floating capture drawer with dual voice/text inputs, 1-tap editable confirmation chips, and 5s undo toast
  [`QuickCaptureSheet.kt:59`](../../app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt#L59)

**Molecular Feature Cards**

- State-aware Hero Card machine managing 6 priority states with 300ms FastOutSlowIn transitions
  [`HeroActionCard.kt:107`](../../app/src/main/java/com/personaltracker/ui/components/cards/HeroActionCard.kt#L107)

- Stateless task card with 3dp/6dp delivery intensity accent stripes, completion strikethrough, and carry-forward badge
  [`TaskIntensityCard.kt:90`](../../app/src/main/java/com/personaltracker/ui/components/cards/TaskIntensityCard.kt#L90)

- Stateless habit streak card with 3dp secondary accent, flame badge, freeze indicator, and 48dp toggle
  [`HabitStreakCard.kt:44`](../../app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt#L44)

- Routine step progression card with sequential dimming, duration, and 48dp advancement button
  [`AdaptiveRoutineStepCard.kt:55`](../../app/src/main/java/com/personaltracker/ui/components/cards/AdaptiveRoutineStepCard.kt#L55)

**Atomic UI Primitives**

- Streak flame badge binding 4-state flame model to vector drawables with data typography
  [`StreakFlameBadge.kt:69`](../../app/src/main/java/com/personaltracker/ui/components/atoms/StreakFlameBadge.kt#L69)

- Coin HUD pill displaying balance and measuring global window coordinates for coin arc targeting
  [`CoinPill.kt:35`](../../app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt#L35)

- DayType chip with template color coding, text ellipsis, and 48dp touch target
  [`DayTypeChip.kt:45`](../../app/src/main/java/com/personaltracker/ui/components/atoms/DayTypeChip.kt#L45)

- Audio visualizer with real-time amplitude bars and safe NaN clamping
  [`WaveformVisualizer.kt:37`](../../app/src/main/java/com/personaltracker/ui/components/atoms/WaveformVisualizer.kt#L37)

**Navigation Refactor & JVM Verification**

- NavHost integration inside AppShellScaffold with strict 48dp minimum touch targets
  [`PersonalTrackerNavHost.kt:52`](../../app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt#L52)

- Unit test suite validating 4-state flames, DayType mapping, hero priorities, intensity stripes, and motion lifecycle
  [`ComponentStateTest.kt:34`](../../app/src/test/java/com/personaltracker/ui/components/ComponentStateTest.kt#L34)

