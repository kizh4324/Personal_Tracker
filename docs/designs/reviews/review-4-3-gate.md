# EGM 3-Stage Pre-Implementation Gate Review: Story 4.3

**Document Audited:** `_bmad-output/implementation-artifacts/spec-4-3-vsync-aligned-bezier-coin-earn-animation-coin-hud.md`  
**Date:** 2026-11-14  
**Gate Status:** **APPROVED WITH MANDATED REFINEMENTS (All findings synthesized and resolved)**

---

## 1. Subagent Swarm Audit Summary

Four isolated, stateless Goldfish subagents audited the specification against the actual Kotlin codebase:
- **`egm-comprehension` (Naive Reader):** Identified phantom type `AwardCoinsResult`, non-existent `isDuplicate` property, arrival timing decoupling between `RewardMotionStage` and `CoinPill`, and missing math for `calculateStaggeredProgress`.
- **`egm-design` (Hostile Design Critic):** Identified premature HUD balance roll-up race condition, recomposition storm from composition-phase modifiers in `CoinPill`, negative apex calculation shooting off-screen into display notch, single-slot event obliteration on rapid multi-taps, and shared mutable `habitOffset` bug in `HabitsScreen.kt`.
- **`egm-codebase-details` (Codebase Quality Auditor):** Verified exact types (`CompleteTaskResult.coinReward`, `LogHabitResult.coinsEarned`, `StudyUiEffect.ShowSessionCompleteCelebration`), noted missing action tags in Code Map, identified missing constants in `AnimationTokens.kt`, and flagged cold-start initial load spurious badge in `CoinPill`.
- **`egm-implementation-readiness` (Implementation Readiness Tech Lead):** Verified dependency ordering, testability, and rendered NO-GO with a checklist of required refinements to reach GO.

---

## 2. Synthesized Findings & Resolutions Matrix

| # | Severity | Category | Finding Description | Resolution Implemented in Refined Spec |
|---|---|---|---|---|
| 1 | 🔴 **Blocker** | API & Invariant | Phantom `AwardCoinsResult` & `isDuplicate` fields; `!isCapReached` erroneously suppresses legitimate coins at cap boundary. | Aligned to actual return types (`CompleteTaskResult.coinReward`, `LogHabitResult.coinsEarned`). Condition is strictly `coinReward > 0`. Non-punitive snackbar triggers on `coinReward == 0 && isCapReached`. |
| 2 | 🔴 **Blocker** | MVI Architecture | Spec required `StudyViewModel` to emit UI `Offset`, violating AD-1 and ViewModel isolation. | `StudyViewModel` emits pure `StudyUiEffect.ShowSessionCompleteCelebration(coinsEarned)`. `StudyScreen` (UI layer) measures timer dial center `Offset` and dispatches to `LocalRewardMotionStage`. |
| 3 | 🔴 **Blocker** | Timing & Race Condition | Room commits immediately ($t=0\text{ms}$), causing `CoinPill` to roll up 600–1080ms before coins land at the HUD. | Implemented arrival coordination protocol: `RewardMotionStageState` triggers an arrival callback/token on arc finish. `CoinPill` holds displayed balance until arrival, rolling up smoothly on coin impact. |
| 4 | 🟡 **Major** | Performance (60/120Hz) | `CoinPill` reading animated progress in `Modifier.offset` and `alpha` causes 60–120Hz recomposition storms. | Mandated draw/render-phase modifiers: `Modifier.graphicsLayer { translationY = ...; alpha = ... }` executing directly on RenderThread. |
| 5 | 🟡 **Major** | Visual Geometry | $\text{peakY} = \min(P_0.y, P_2.y) - \text{arcHeight}$ produces negative Y coordinates, shooting coins off-screen into notch/bezel. | Clamped apex: $\text{peakY} = (\min(P_0.y, P_2.y) - \text{arcHeight})\text{.coerceAtLeast}(\text{minHeadroomPx})$, keeping trajectories strictly inside viewport. |
| 6 | 🟡 **Major** | State & Concurrency | Single `activeCoinEvent` slot in `RewardMotionStageState` cancels in-flight coins when a second item is tapped. | Upgraded `RewardMotionStageState` to hold `activeCoinEvents: List<CoinRewardEvent>`, rendering concurrent bursts cleanly. |
| 7 | 🟡 **Major** | UI Bug | `HabitsScreen.kt` shares a single `var habitOffset` in a loop, so every card overwrites it and only the last card's offset is used. | Replaced with `val habitOffsets = remember { mutableStateMapOf<String, Offset>() }` keyed by habit ID, matching `HomeScreen.kt`. |
| 8 | 🟡 **Major** | Deterministic Math | Formula for `calculateStaggeredProgress` was missing from Design Notes. | Formally documented timeline math: $T = D + (N-1) \times S$, $\text{progress}_i = \left(\frac{\text{masterProgress} - \frac{i \times S}{T}}{\frac{D}{T}}\right)\text{.coerceIn}(0f, 1f)$. |
| 9 | 🟡 **Major** | UX Edge Case | On cold start, `0 -> balance` was treated as an earn event, spuriously triggering a `+X` badge and 1.18x burst. | Added `isInitialLoad` guard in `CoinPill` to snap `displayedBalance = balance` without animation or badge on first composition. |
| 10 | 🟢 **Nit** | Tokens & Clean Code | Missing constants in `AnimationTokens.kt` (`coinFlightDurationMs`, `coinStaggerDelayMs`); Code Map missing `[Create]`/`[Modify]` tags. | Added execution task for `AnimationTokens.kt`; annotated all Code Map entries with explicit action tags; cleansed raw literals to tokens. |

---

## 3. Implementation Gate Verdict

With all 10 findings incorporated into the updated specification, the specification satisfies:
- **Actionable:** Every task has an explicit file path and action.
- **Logical:** Dependencies ordered pure math -> atom/animation -> screen/orchestration -> verification.
- **Testable:** Given/When/Then ACs with boundary conditions.
- **Complete:** Zero TBDs, zero phantom APIs, exact Kotlin signatures.
- **Sufficient:** No unaddressed edge cases.

**Final Gate Verdict:** **GO (Ready for Development)**
