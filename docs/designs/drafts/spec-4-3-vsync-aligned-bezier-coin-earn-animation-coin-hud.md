---
title: 'Story 4.3: VSYNC-Aligned Bézier Coin Earn Animation & Coin HUD'
type: 'feature'
created: '2026-11-14'
status: 'done'
baseline_commit: '60f9f1545fc7bf5962f604d91511d1e9939d0926'
review_loop_iteration: 1
context:
  - '_bmad-output/implementation-artifacts/epic-4-context.md'
  - 'ARCHITECTURE-SPINE.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Earning coins across tasks, habits, routines, and study sessions currently triggers disconnected micro-animations or instant balance jumps without a continuous, physical sense of reward conveyance. Disconnected animations risk frame stutters on variable refresh displays (60Hz vs 120Hz), clipping across nested scrollable lists, race conditions during rapid taps, and lack of visual impact when coins arrive at the top-bar HUD.

**Approach:** Implement a deterministic VSYNC-aligned quadratic Bézier particle trajectory engine (`CoinArcAnimation.kt`, `BezierMath.kt`) rendered on a root-hoisted unclipped motion layer (`RewardMotionStage.kt`), coupled to a responsive `CoinPill` HUD with Framer-calibrated spring counter roll-up, floating `"+X"` badge pills, and subtle scale burst physics (`1.0f -> 1.18f -> 1.0f` via `shopPurchaseBurstSpringSpec`), coordinated via arrival callbacks with full accessibility reduced-motion bypass and MVI effect deduplication (`coinReward > 0` / `coinsEarned > 0`).

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** Presentation-layer motion and animation math resides strictly in `ui/components/animation/` and `ui/components/atoms/`. Domain models and use cases remain 100% pure Kotlin with zero `android.*` or `androidx.*` dependencies. ViewModels do not consume or emit Compose UI `Offset` coordinates.
- **AD-4 Non-Punishing UX:** Hitting daily earn caps or duplicate actions never displays punitive or error states. Coin animations trigger only for genuine positive coin awards (`coinReward > 0` / `coinsEarned > 0`). When an action awards 0 coins due to reaching a cap, display the affirmative notice ("Daily coin goal reached! Awesome work today.") without launching coin arcs.
- **AD-7 Token Conformance & Touch Targets:** 100% of colors, dimensions, and curves resolve from `PersonalTrackerTheme` and `AnimationTokens`. Zero raw hex (`#...`), `dp`, or `sp` literals in composables. Minimum interactive touch target floor is 48dp (`spacing.minTouchTarget`).
- **VSYNC Frame Alignment & Zero Allocation:** Motion progress is driven by Compose's monotonic frame clock (`withFrameNanos` / `withInfiniteAnimationFrameMillis`), computing interpolation $t \in [0.0, 1.0]$ with zero object allocations inside the `Canvas` `onDraw` loop.
- **Render-Thread Performance:** Animate floating badge translation and alpha strictly in the draw/layout phase using `Modifier.graphicsLayer { translationY = ...; alpha = ... }` to prevent 60Hz/120Hz recomposition storms.
- **Safe Viewport Clamping:** Dynamic curve apex $\text{peakY}$ is clamped to `minHeadroomPx` (`PersonalTrackerTheme.spacing.minTouchTarget`) so coins never fly off-screen into the status bar or display notch.
- **Accessibility Reduced-Motion Honor:** When system animation scale is 0f or reduced-motion is enabled, bypass flight trajectories entirely and update HUD coin balance and state immediately without scale bursts.

**Ask First:**
- Changing the baseline flight duration (600ms) or coin stagger interval (80ms).
- Changing the Framer spring physics parameters (`shopPurchaseBurstSpringSpec`: stiffness 400, damping 18).

**Never:**
- Never perform object allocations (lists, strings, new `Paint` / `Color` objects) inside the 60Hz/120Hz `Canvas` draw loop.
- Never clip coin flight arcs inside nested scroll containers or screen boundaries; all coin animations must render hoisted at `RewardMotionStage`.
- Never write production code during design phase (Strict EGM Elephant invariant).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **Standard Coin Earn** | Task completed (+7 coins), start offset measured from checkbox | 7 coins launch staggered (80ms apart) along quadratic Bézier curve to `CoinPill` HUD; upon arrival at HUD (~1080ms total), HUD rolls up count +7 with 1.18x spring burst and floating `"+7"` badge | If `endOffset` unmeasured, falls back safely to top-right screen coordinate |
| **Cap Reached (0 Award)** | Task completed when daily task cap reached (`coinReward = 0`, `isCapReached = true`) | No coin arc animation spawned; non-punitive toast "Daily coin goal reached! Awesome work today." displayed; HUD stays steady | `coinReward > 0` guard short-circuits animation dispatch |
| **Cap Boundary Award** | Important task completed (+7 requested), 3 coins left under cap (`coinReward = 3`, `isCapReached = true`) | 3 coins animate to HUD; HUD rolls up +3 on arrival; toast "Daily coin goal reached! Awesome work today." displayed | Both coin animation and non-punitive toast trigger harmoniously |
| **Rapid Multi-Tap Duplicate** | User double-taps checkbox within 100ms | First tap commits coins (`coinReward > 0`); second tap returns duplicate with `coinReward = 0` and does not restart or stutter flight | `coinReward > 0` prevents duplicate animation triggers |
| **Study Session Finish** | Focus timer reaches 00:00 (+8 coins) on Study screen | `StudyViewModel` emits `ShowSessionCompleteCelebration(8)`; `StudyScreen` measures timer dial center and dispatches 8 coins to `RewardMotionStage`; HUD rolls up +8 on arrival | If screen navigates before completion, hoisted `RewardMotionStage` completes flight safely |
| **Cold Start / Initial Mount** | App launches with existing balance (e.g. 150 coins from DB) | `CoinPill` initializes `displayedBalance = 150` immediately with zero scale burst and zero floating badge | `isInitialLoad` guard in `CoinPill` suppresses spurious earn animations |
| **Simultaneous Coin Bursts** | Rapid completion of two items while first arc in flight | `RewardMotionStageState` maintains concurrent `activeCoinEvents: List<CoinRewardEvent>`, rendering both bursts independently without mid-air cancellation | Each burst completes its own trajectory and triggers arrival roll-up |
| **Reduced Motion / Scale 0** | System animations disabled (`ANIMATOR_DURATION_SCALE = 0f`) | Coin arc skipped (`isPlaying = false`); HUD counter snaps immediately to new balance; no scale burst | Immediate invocation of `onComplete()` callback |
| **Off-Screen Anchor** | Item completed from off-screen list scroll position (`startOffset.y < 0`) | Clamps start anchor to viewport edge or card origin to prevent inverted curve flight | Coordinate sanitization in `dispatchCoinReward` |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/ui/components/animation/BezierMath.kt` -- `[Create]` Pure Kotlin Bézier curve algorithms (`evaluateQuadraticBezier`, `evaluateQuadraticBezierX`, `evaluateQuadraticBezierY`, `calculateArcControlPoint`, `calculateStaggeredProgress`), 100% deterministic, zero allocation, primitive-friendly for JVM testing.
- `app/src/main/java/com/personaltracker/ui/components/animation/CoinArcAnimation.kt` -- `[Modify]` High-performance VSYNC-aligned Canvas particle renderer consuming precomputed Bézier paths and monotonic frame nanos with zero allocations in draw loop.
- `app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt` -- `[Modify]` Persistent Coin HUD pill with arrival-triggered spring counter roll-up (`displayedBalance`), subtle 1.18f spring scale burst (`shopPurchaseBurstSpringSpec`), floating `"+X"` badge via `Modifier.graphicsLayer`, initial-mount suppression, and reduced-motion bypass.
- `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- `[Modify]` Viewport-hoisted motion layer hosting unclipped `CoinArcAnimation` and `RewardAnimation` above navigation hosts, managing concurrent bursts (`activeCoinEvents: List<CoinRewardEvent>`), and coordinating arrival triggers.
- `app/src/main/java/com/personaltracker/ui/theme/AnimationTokens.kt` -- `[Modify]` Add `coinFlightDurationMs = 600L` and `coinStaggerDelayMs = 80L`, re-exporting existing `shopPurchaseBurstSpringSpec`.
- `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- `[Modify]` Scaffolds persistent top context bar, measures `CoinPill` global coordinates, wires `RewardMotionStageState` arrival events to `CoinPill`.
- `app/src/main/java/com/personaltracker/ui/navigation/AppShellViewModel.kt` -- `[Unchanged]` Connects `EconomyRepository.observeLatestBalance()` to HUD balance state.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` & `HomeScreen.kt` -- `[Modify]` Emits `ShowCoinReward` strictly when `coinReward > 0` / `coinsEarned > 0`, emitting non-punitive toast when `isCapReached && coinReward == 0`.
- `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsViewModel.kt` & `HabitsScreen.kt` -- `[Modify]` Emits `ShowCoinReward` on habit completion when `coinsEarned > 0`; fixes `habitOffsets` map per habit ID.
- `app/src/main/java/com/personaltracker/ui/screens/study/StudyViewModel.kt` & `StudyScreen.kt` -- `[Modify]` `StudyScreen` measures circular dial center `Offset` via `onGloballyPositioned` and dispatches coin arc on `ShowSessionCompleteCelebration`.
- `app/src/test/java/com/personaltracker/ui/components/animation/CoinArcAnimationTest.kt` -- `[Create]` Comprehensive JVM unit test suite verifying Bézier curve math, control point calculation with apex headroom clamping, staggered progress distribution, and multi-coin timing math.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/ui/theme/AnimationTokens.kt` -- Add `coinFlightDurationMs = 600L` and `coinStaggerDelayMs = 80L` constants (AD-7).
- [x] `app/src/main/java/com/personaltracker/ui/components/animation/BezierMath.kt` -- Create pure Kotlin Bézier math helper with `evaluateQuadraticBezier`, primitive `Float` evaluation overloads, `calculateArcControlPoint` with apex headroom clamping (`minHeadroomPx`), and `calculateStaggeredProgress(masterProgress, index, totalCount, flightDurationMs, staggerDelayMs)`.
- [x] `app/src/test/java/com/personaltracker/ui/components/animation/CoinArcAnimationTest.kt` -- Unit test Bézier trajectory calculations at boundaries $t = 0f, 0.5f, 1f$, control point headroom clamping ($peakY \ge minHeadroom$), staggered progress calculations, and total duration math $T = D + (N - 1) \times S$.
- [x] `app/src/main/java/com/personaltracker/ui/components/animation/CoinArcAnimation.kt` -- Refactor to single-loop monotonic frame clock (`withFrameNanos`), reading `BezierMath`, pre-caching colors and radius values, eliminating allocations inside `Canvas` `onDraw`.
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt` -- Add arrival-triggered spring counter roll-up (`displayedBalance`), scale burst (`1.0f -> 1.18f -> 1.0f` using `shopPurchaseBurstSpringSpec`), floating `"+X"` badge via `Modifier.graphicsLayer { translationY = ...; alpha = ... }`, initial-mount suppression, and reduced-motion bypass.
- [x] `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- Support concurrent bursts (`activeCoinEvents: List<CoinRewardEvent>`), arrival notification dispatch (`onCoinArrival`), stage-relative coordinate adjustment, and reduced-motion bypass.
- [x] `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- Connect `RewardMotionStageState.onCoinArrival` to `CoinPill` arrival trigger.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` & `HomeScreen.kt` -- Verify `HomeUiEffect.ShowCoinReward` emits strictly when `result.coinReward > 0` (or `coinsEarned > 0`), and snackbar emits when `coinReward == 0 && isCapReached`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsViewModel.kt` & `HabitsScreen.kt` -- Verify `HabitsUiEffect.ShowCoinReward` emits strictly when `result.coinsEarned > 0`, and fix `habitOffsets` map per habit ID in `HabitsScreen.kt`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/StudyScreen.kt` -- Measure circular timer dial center `Offset` with `onGloballyPositioned` and dispatch `LocalRewardMotionStage.current.dispatchCoinReward` upon collecting `StudyUiEffect.ShowSessionCompleteCelebration`.

**Acceptance Criteria:**
- Given a task completed for +7 coins, when the checkbox is tapped, then 7 coins animate along an upward quadratic Bézier arc from the checkbox position to `CoinPill`, arriving consecutively between 600ms and 1080ms without dropping frames at 60Hz or 120Hz.
- Given coins arriving at `CoinPill`, when the flight arc finishes, then the displayed balance rolls up to the new total with a 1.18x spring scale burst and a floating `"+7"` badge pill that fades as it ascends, executing on the RenderThread without recomposition loops.
- Given a task completed when the daily earn cap is reached (0 coins awarded), when tapped, then no coin arc is spawned and a non-punitive toast "Daily coin goal reached! Awesome work today." is displayed while the task completes normally.
- Given an action that awards coins while reaching the daily cap (e.g. +3 coins reaching 50/50), when completed, then both the 3-coin arc and the affirmative cap toast are displayed.
- Given a rapid double-tap on a checkbox, when processed, then exactly one coin arc burst is launched and the duplicate tap returns 0 coins with no visual glitch.
- Given two rapid completions on different items, when launched, then `RewardMotionStage` renders both coin bursts concurrently without canceling in-flight particles.
- Given an app launch on cold start with an existing balance, when `CoinPill` first composes, then it displays the balance immediately without triggering a spurious `+X` badge or scale burst.
- Given an Android device with animations disabled in developer options (or reduced-motion enabled), when coins are awarded, then the flight trajectory is bypassed and `CoinPill` updates its balance count immediately without delay or scale burst.
- Given any interactive element or pill in `CoinPill`, when rendered, then it strictly adheres to a minimum 48dp touch target and 100% `PersonalTrackerTheme` semantic tokens.

## Spec Change Log

- **Iteration 1 (Post-Goldfish Swarm Audit):**
  - Standardized all award conditions: replaced phantom `AwardCoinsResult` and `isDuplicate` with actual use case contracts (`CompleteTaskResult.coinReward > 0`, `LogHabitResult.coinsEarned > 0`).
  - Fixed cap inversion bug: removed `!isCapReached` from coin award guard to ensure cap boundary rewards (e.g. +3 reaching 50/50) launch coin arcs while also displaying the affirmative cap toast.
  - Resolved arrival timing race condition: added arrival coordination contract connecting `RewardMotionStageState` completion to `CoinPill` roll-up, holding visual update until coins physically land at the HUD.
  - Eliminated recomposition storms: mandated `Modifier.graphicsLayer` for floating badge translation and alpha in `CoinPill`.
  - Added apex headroom clamping: clamped $\text{peakY} \ge \text{minHeadroomPx}$ to prevent negative Y coordinates shooting into display notch/bezel.
  - Added multi-burst concurrency: upgraded `RewardMotionStageState` to hold `activeCoinEvents: List<CoinRewardEvent>` so rapid sequential taps don't cancel in-flight coins.
  - Fixed `HabitsScreen.kt` shared `habitOffset` bug: replaced single mutable state with `mutableStateMapOf<String, Offset>()` keyed by habit ID.
  - Fully documented `calculateStaggeredProgress` mathematical formula in Design Notes §1.
  - Added cold-start initial load suppression in `CoinPill` to eliminate spurious `+X` badge on app launch.
  - Added missing `coinFlightDurationMs` and `coinStaggerDelayMs` constants task for `AnimationTokens.kt`.
  - Kept `StudyViewModel` pure: `StudyScreen` captures dial center `Offset` in UI layer and dispatches to `RewardMotionStage`.

## Design Notes

### 1. Bézier Trajectory Math Formulation
Quadratic Bézier curve:
$$B(t) = (1-t)^2 P_0 + 2(1-t)t P_1 + t^2 P_2, \quad t \in [0, 1]$$
- $P_0$: Origin anchor `Offset` (task checkbox, habit row, or timer center).
- $P_2$: Target anchor `Offset` (center of `CoinPill` HUD via `onGloballyPositioned`).
- $P_1$: Dynamic apex control point with safe headroom clamping:
  $$\text{midX} = \frac{P_0.x + P_2.x}{2}$$
  $$\text{rawPeakY} = \min(P_0.y, P_2.y) - \max(\text{minArcHeight}, |P_0.x - P_2.x| \times 0.35 + |P_0.y - P_2.y| \times 0.25)$$
  $$\text{peakY} = \max(\text{minHeadroomPx}, \text{rawPeakY})$$

Staggered Multi-Coin Timeline Math:
- Total duration for $N$ coins: $T = D + (N - 1) \times S$ (where $D = 600\text{ms}$ flight duration, $S = 80\text{ms}$ stagger delay).
- For coin index $i \in [0, N-1]$ and master progress $M \in [0.0, 1.0]$:
  $$\text{startFraction}_i = \frac{i \times S}{T}, \quad \text{durationFraction} = \frac{D}{T}$$
  $$\text{progress}_i = \left(\frac{M - \text{startFraction}_i}{\text{durationFraction}}\right)\text{.coerceIn}(0f, 1f)$$

### 2. Zero-Allocation Draw Scope Guarantee
Inside `Canvas(modifier.fillMaxSize())`:
- Precompute `controlPoint` once per animation event in the outer scope, not per frame tick.
- Cache `coinColor`, `coinGlowColor`, `highlightColor`, and base radii as primitive floats before iterating coins.
- Use inline arithmetic on primitive floats (`x`, `y`) instead of allocating temporary `Offset` objects or collections inside `onDraw`.

### 3. Framer-to-Compose Spring Alignment & Arrival Coordination
- `shopPurchaseBurstSpringSpec`: Reused directly from `AnimationTokens.shopPurchaseBurstSpringSpec()` (stiffness 400, damping ratio 0.50f).
- `CoinPill` scale burst: Chained sequentially via coroutine: `animateTo(1.18f, shopPurchaseBurstSpringSpec)` followed by `animateTo(1.0f, shopPurchaseBurstSpringSpec)`.
- Floating Badge: `Modifier.graphicsLayer { translationY = -spacing.spacing4.toPx() * badgeProgress; alpha = 1f - badgeProgress }` executing directly on RenderThread.
- Cold Start Guard: `remember { mutableStateOf(true) }` ensures initial balance load does not fire spring burst or badge.

## Verification

**Commands:**
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `total_findings: 0`.
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`.
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid.
- `.\gradlew test` -- expected: All unit tests pass, including new `CoinArcAnimationTest`.
- `.\gradlew lintDebug` -- expected: Android lint completes with 0 errors.
- Artemis Gate 6 Real-Device Verification (if device `10BD9D24YF000BE` connected): `mobile_get_device_state` and `mobile_run_task` confirming 60/120Hz smooth coin arc and HUD update.

## Suggested Review Order

**Bézier Mathematical Engine & Frame Timing**

- Pure Kotlin quadratic Bézier curve evaluation and dynamic apex control point calculation with notch headroom clamping.
  [`BezierMath.kt:21`](../../app/src/main/java/com/personaltracker/ui/components/animation/BezierMath.kt#L21)

- Single-loop monotonic frame clock (`withFrameNanos`) driving multi-coin particles with zero in-draw allocations.
  [`CoinArcAnimation.kt:62`](../../app/src/main/java/com/personaltracker/ui/components/animation/CoinArcAnimation.kt#L62)

- Duration and stagger delay tokens (`coinFlightDurationMs`, `coinStaggerDelayMs`).
  [`AnimationTokens.kt:37`](../../app/src/main/java/com/personaltracker/ui/theme/AnimationTokens.kt#L37)

**Viewport-Hoisted Overlay & Multi-Burst State**

- Concurrent burst management (`activeCoinEvents: List<CoinRewardEvent>`) and arrival notification dispatch.
  [`RewardMotionStage.kt:47`](../../app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt#L47)

- Top bar wiring connecting arrival notifications to `CoinPill` balance roll-up.
  [`AppShellScaffold.kt:173`](../../app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt#L173)

**HUD Atom & RenderThread Transforms**

- Arrival-triggered spring counter roll-up, 1.18x spring scale burst, and `graphicsLayer` floating badge.
  [`CoinPill.kt:45`](../../app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt#L45)

**Feature Screens & Choreography**

- Deduplication guard (`coinReward > 0`), affirmative cap snackbars, and coin flight triggers.
  [`HomeViewModel.kt:622`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt#L622)

- Per-habit ID coordinate mapping and streak bonus coin dispatch.
  [`HabitsScreen.kt:156`](../../app/src/main/java/com/personaltracker/ui/screens/habits/HabitsScreen.kt#L156)

- Timer dial center coordinate measurement and session completion coin reward flight.
  [`StudyScreen.kt:286`](../../app/src/main/java/com/personaltracker/ui/screens/study/StudyScreen.kt#L286)

**Verification & Tests**

- 13 comprehensive unit tests validating trajectory math, headroom clamping, and burst concurrency.
  [`CoinArcAnimationTest.kt:16`](../../app/src/test/java/com/personaltracker/ui/components/animation/CoinArcAnimationTest.kt#L16)
