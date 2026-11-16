---
title: 'Story 4.5: Cosmetics Wardrobe Shop, Consumable Grace Shields & Milestone Choreography'
type: 'feature'
created: '2026-11-14'
status: 'done'
baseline_commit: 'cfcd58aa87cf36fd30f6e54804f43d1efbce2ed1'
review_loop_iteration: 2
context:
  - '_bmad-output/implementation-artifacts/epic-4-context.md'
  - 'ARCHITECTURE-SPINE.md'
  - '.harness2/EXPERIENCE.md'
  - 'docs/designs/reviews/review-4-5-gate.md'
  - 'HANDOFF.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users need a non-punitive, sustainable mechanism to protect their active habit streaks during overwhelming or busy periods without shame or guilt, and currently cannot spend their earned Coins on consumable streak shields. Furthermore, milestone achievements (7, 14, and 28-day streak milestones and companion level-ups) lack an unclipped, full-reveal celebration stage honoring reduced-motion accessibility standards.

**Approach:** Implement a pure Kotlin consumable grace shield utility engine (`PurchaseStreakShieldUseCase.kt`) enabling users to exchange earned coins for habit streak slack bank days (`slackBankDays` up to `MAX_SLACK_BANK_DAYS = 3`) with atomic coin deduction and cap validation in Room `@Transaction` blocks via `HabitRepository.purchaseStreakShield` (AD-1, AD-4, AD-6), integrated with a viewport-hoisted milestone celebration stage (`RewardMotionStage.kt`, `RewardAnimationType.MilestoneFullReveal`) that triggers joyous companion celebrations (`CompanionState.CELEBRATING_REWARD`) on major consistency milestones and pet level-ups with strict reduced-motion bypass (AD-7).

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** `PurchaseStreakShieldUseCase`, `ShopItem`, and `PurchaseStreakShieldResult` must remain 100% pure Kotlin in `domain/` with zero Android framework imports (`android.*`, `androidx.*`).
- **AD-4 Non-Punishing Guilt-Free UX:** Zero guilt, shame, or penalty copy. Cap-reached notices must be empowering and supportive (e.g. "Streak shield bank is already at maximum capacity (3/3). Keep up the wonderful momentum!"). Missing habits must never decrease best streak or companion affection.
- **AD-6 Idempotent Economy & Gross Spend Defense:** Every shield purchase executes under a deterministic compound idempotency key `SHIELD_BUY_{habitId}_{timestamp}` inside Room `@Transaction` blocks on `Dispatchers.IO` via `HabitRepository.purchaseStreakShield`. Deductions record negative delta amounts (`deltaAmount = -priceCoins`, `EconomyEventTypes.SHOP_PURCHASE`), ensuring daily earn caps are never deflated by utility purchases (EXP-045). In-flight purchasing flags in ViewModels prevent concurrent rapid double-tap dispatch.
- **AD-7 Token Conformance, Touch Targets & Motion:** 100% of colors, dimensions, and typography resolve from `PersonalTrackerTheme` tokens (0 raw hex/dp/sp literals in UI). Minimum interactive touch target floor is 48dp (`spacing.minTouchTarget`). `RewardMotionStage` must query `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` and bypass full-reveal Lottie animations immediately via `LaunchedEffect` without composing `RewardAnimation` or rendering a 1-frame flash when reduced motion is enabled (EXP-046).
- **Figma-Ready MVI Architecture (AD-2):** Screens maintain strict separation between stateful Hilt routes (`HabitsRoute`, `CompanionRoute`) and 100% stateless renderers (`HabitsScreenContent`, `CompanionScreenContent`), dispatching one-shot celebration effects through Channel-backed `UiEffect`.

**Ask First:**
- Increasing the maximum streak shield bank limit beyond `MAX_SLACK_BANK_DAYS = 3`.
- Changing the base coin cost for consumable streak shields (`SHIELD_PRICE_COINS = 30`).

**Never:**
- Never allow purchasing a streak shield when user coin balance is less than the item price (`balance < priceCoins`).
- Never allow a habit's `slackBankDays` to exceed `StreakStateMachine.MAX_SLACK_BANK_DAYS` (3).
- Never allow coin balances to drop below 0 (`balanceAfter >= 0`).
- Never mutate `CompanionProfile.activeAccessoryId` or mark permanent unlock `isUnlocked = true` when purchasing a consumable utility item (`CATEGORY_UTILITY`).
- Never write production code during design phase (Strict EGM Elephant invariant).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **Standard Streak Shield Purchase (Per-Habit)** | User has 50 coins; habit has `slackBankDays = 1`; user taps "+ 🛡️ (30🪙)" on habit card | 30 coins deducted (`balance = 20`); `slackBankDays = 2`; transaction `SHIELD_BUY_{id}_{ts}` committed; celebration micro burst plays; snackbar "Grace Shield activated! Streak protected." displayed | If balance < 30, returns `InsufficientCoins(required = 30, current = balance)` with snackbar "Not enough coins yet. Keep going!" |
| **Shield Bank At Capacity** | User has 100 coins; habit already has `slackBankDays = 3` (or `MAX_SLACK_BANK_DAYS`) | Coins NOT deducted; returns `CapReached(currentShields = 3, maxAllowed = 3)`; displays supportive snackbar "Streak shield bank is already at maximum capacity (3/3)." | Safe abort without DB mutation or coin deduction |
| **General Shop Purchase (Target Habit Null)** | User taps "Buy Grace Shield" in Companion shop without specifying `habitId` | Automatically resolves to active habit with lowest `slackBankDays < 3`; deducts 30 coins; increments slack day on that habit | If no habits exist, returns `Error("No habits available. Create a habit first!")`; if all at 3, returns `CapReached` |
| **7-Day Streak Milestone Completion** | User completes habit reaching `currentStreak = 7` (or 14, 28) | Habit logged; coins awarded; `HabitsUiEffect.ShowLottieReward(MilestoneFullReveal)` dispatched (supersedes `CoinEarnMicro`); companion mood updated in DB to `CELEBRATING_REWARD` | If reduced motion active, animation bypassed and completion callback invoked immediately |
| **Companion Level-Up Event** | User earns energy causing companion level to increment (e.g. Level 1 -> 2 at 100 XP) | `CompanionUiEffect.PlayLottieReward(MilestoneFullReveal)` dispatched; companion mood transitions to `CELEBRATING_REWARD`; Level increments; energy wraps `energy % 100` | Cold-start guarded by `previousLevel != null && newLevel > previousLevel`; runs unclipped in hoisted `RewardMotionStage` |
| **Consumable Repeat Purchase** | User buys Grace Shield, uses it on a busy week (`slackBankDays` drops to 1), then buys another Grace Shield later | Utility item remains available for re-purchase (unlike permanent cosmetics); coins deducted and `slackBankDays` incremented to 2 | Each purchase generates unique timestamped idempotency key preventing collision |
| **Rapid Double Tap on Buy Shield** | User taps "Buy Shield" twice within 80ms | In-flight purchasing flag blocks duplicate execution; atomic Room `@Transaction` in `HabitRepositoryImpl.purchaseStreakShield` prevents double spend or cap breach | Handled via UI debouncing + database transaction atomicity |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/ShopItem.kt` -- `[Modify]` Add `companion object` scoping constants (`CATEGORY_OUTFIT`, `CATEGORY_ACCESSORY`, `CATEGORY_BACKGROUND_DECOR`, `CATEGORY_UTILITY`) and map `"item_streak_shield"` to emoji `"🛡️"` (AD-1).
- `app/src/main/java/com/personaltracker/domain/usecase/PurchaseStreakShieldUseCase.kt` -- `[Create]` Pure Kotlin usecase returning typed sealed interface `PurchaseStreakShieldResult`:
  ```kotlin
  sealed interface PurchaseStreakShieldResult {
      data class Success(val updatedHabit: Habit, val newBalance: Int, val shieldCount: Int) : PurchaseStreakShieldResult
      data class InsufficientCoins(val required: Int, val current: Int) : PurchaseStreakShieldResult
      data class CapReached(val currentShields: Int, val maxAllowed: Int) : PurchaseStreakShieldResult
      data class HabitNotFound(val habitId: String) : PurchaseStreakShieldResult
      data class Error(val message: String) : PurchaseStreakShieldResult
  }
  ```
  Operator: `suspend operator fun invoke(targetHabitId: String? = null): PurchaseStreakShieldResult`. If `targetHabitId == null`, dynamically selects the active habit with lowest `slackBankDays < MAX_SLACK_BANK_DAYS`.
- `app/src/main/java/com/personaltracker/domain/repository/HabitRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/HabitRepositoryImpl.kt` -- `[Modify]` Add `suspend fun purchaseStreakShield(habitId: String, priceCoins: Int = 30, idempotencyKey: String): Habit` executing coin balance verification, coin deduction (`deltaAmount = -priceCoins`, `EconomyEventTypes.SHOP_PURCHASE`), cap check (`slackBankDays < 3`), and `slackBankDays` incrementation in a single atomic Room `@Transaction` using existing `CoinLedgerDao`, `HabitDao`, and `TransactionRunner`.
- `app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt` -- `[Modify]` Add `item_streak_shield` to `DEFAULT_SHOP_ITEMS` with price 30 coins, category `CATEGORY_UTILITY`. In `ensureDefaultCatalogAndProfile()`, insert missing default catalog items individually (safely seeding existing databases). In `purchaseShopItem`, guard with `if (item.category != ShopItem.CATEGORY_UTILITY)` so utility items never alter `CompanionProfile.activeAccessoryId` or mark permanent unlock `isUnlocked = true`.
- `app/src/test/java/com/personaltracker/ui/TestFakes.kt` -- `[Modify]` Update `TestHabitRepository` with `purchaseStreakShield(...)` implementation to preserve JVM test suite compilation.
- `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- `[Modify]` Enforce strict reduced-motion guard on modal Lottie animations (`RewardAnimationType.MilestoneFullReveal`), completing immediately via `LaunchedEffect` without composing `RewardAnimation` when `isReducedMotion == true` (AD-7).
- `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- `[Modify]` Add parameters `slackBankDays: Int = 0` and `onBuyShield: (() -> Unit)? = null`. Render a shield status badge and a 48dp quick purchase pill (`"+ 🛡️ (30🪙)"` when `slackBankDays < 3`, or `"🛡️ Full (3/3)"` when at capacity).
- `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsUiIntent.kt` & `HabitsViewModel.kt` -- `[Modify]` Add `BuyStreakShield(val habitId: String)` intent, inject `PurchaseStreakShieldUseCase` and `EconomyRepository`, debounce purchases with `isPurchasingShield` flag, detect 7/14/28-day streak milestones (`streak > 0 && streak % 7 == 0`), dispatch `MilestoneFullReveal` (superseding `CoinEarnMicro`), and set companion mood to `CompanionState.CELEBRATING_REWARD`.
- `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionViewModel.kt` & `CompanionScreen.kt` -- `[Modify]` Inject `PurchaseStreakShieldUseCase`. Guard companion level-up celebration with `previousLevel != null && newLevel > previousLevel` to suppress cold-start flashes. Route `CompanionUiIntent.PurchaseOrEquipItem` for `"item_streak_shield"` to `PurchaseStreakShieldUseCase(targetHabitId = null)`. In `CompanionScreen.kt`, remove the `.take(4)` truncation so all shop items render, and display consumable copy (`"Buy (30🪙)"` rather than `"Equip"`).
- `app/src/test/java/com/personaltracker/domain/usecase/PurchaseStreakShieldUseCaseTest.kt` -- `[Create]` Unit test suite verifying shield purchase success, coin deduction atomicity, cap limit enforcement (3), insufficient coin handling, dynamic null-habit resolution, and duplicate idempotency defense.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/ShopItem.kt` -- Add `companion object` constants (`CATEGORY_OUTFIT`, `CATEGORY_ACCESSORY`, `CATEGORY_BACKGROUND_DECOR`, `CATEGORY_UTILITY`) and map `"item_streak_shield"` to `"🛡️"` emoji.
- [x] `app/src/main/java/com/personaltracker/domain/repository/HabitRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/HabitRepositoryImpl.kt` -- Implement `purchaseStreakShield(habitId, priceCoins, idempotencyKey)` inside single atomic Room `@Transaction`.
- [x] `app/src/test/java/com/personaltracker/ui/TestFakes.kt` -- Add `purchaseStreakShield` stub to `TestHabitRepository` to ensure test suite integrity.
- [x] `app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt` -- Add `item_streak_shield` to default shop catalog, update `ensureDefaultCatalogAndProfile` to insert missing default catalog items individually, and guard `purchaseShopItem` against modifying `activeAccessoryId` or `isUnlocked` for utility items.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/PurchaseStreakShieldUseCase.kt` -- Implement pure Kotlin usecase returning `PurchaseStreakShieldResult` with coin balance check, cap checks (`slackBankDays < 3`), and null-habit auto-targeting.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/PurchaseStreakShieldUseCaseTest.kt` -- Comprehensive JVM unit test suite covering shield purchase, cap limits, dynamic habit selection, and coin deductions.
- [x] `app/src/main/java/com/personaltracker/ui/components/animation/RewardMotionStage.kt` -- Add reduced-motion bypass for modal Lottie animations (`MilestoneFullReveal`) without 1-frame flashes.
- [x] `app/src/main/java/com/personaltracker/ui/components/cards/HabitStreakCard.kt` -- Add `slackBankDays` badge and 48dp quick shield purchase pill.
- [x] `app/src/main/java/com/personaltracker/ui/screens/habits/HabitsUiIntent.kt` & `HabitsViewModel.kt` -- Implement `BuyStreakShield` intent, milestone detection (`streak > 0 && streak % 7 == 0`), `MilestoneFullReveal` dispatch, and companion celebratory mood update.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionViewModel.kt` & `CompanionScreen.kt` -- Add cold-start guarded level-up detection (`previousLevel != null && newLevel > previousLevel`), wire utility shop purchases, and remove `.take(4)` limitation.

**Acceptance Criteria:**
- Given a user with 45 coins and a habit with 1 slack day, when tapping "+ 🛡️ (30🪙)", then 30 coins are deducted, the habit's slack bank increases to 2, and a reassuring success message is shown.
- Given a habit that already has 3 slack bank days (`MAX_SLACK_BANK_DAYS`), when attempting to buy a grace shield, then coins are not deducted and an empowering message ("Streak shield bank is already at maximum capacity (3/3)") is shown.
- Given a user with 20 coins, when attempting to buy a grace shield (30 coins), then coins are not deducted and a gentle message ("Not enough coins yet. Keep going!") is displayed.
- Given a user on `CompanionScreen` tapping "Buy Grace Shield" without a specific habit selected, when processed, then the shield is automatically applied to the active habit with the lowest slack bank days.
- Given a habit reaching a 7-day, 14-day, or 28-day streak milestone upon completion, when logged, then the unclipped `MilestoneFullReveal` confetti celebration plays (superseding micro coins) and the companion celebrates.
- Given a companion gaining enough XP to level up (e.g. Level 1 -> 2), when evaluated after cold start, then `MilestoneFullReveal` is triggered and the companion mood updates to `CELEBRATING_REWARD`.
- Given a device with reduced motion enabled (`ANIMATOR_DURATION_SCALE == 0`), when a milestone or reward animation triggers, then visual animations are cleanly bypassed without UI hanging or crashes.
- Given all interactive buttons on `HabitsScreen` and `CompanionScreen`, when rendered, then they strictly enforce a 48dp minimum touch target floor (`spacing.minTouchTarget`) and zero hardcoded literals (AD-7).

## Spec Change Log

- **2026-11-14 (EGM Review Loop 1 & 2):**
  - Consolidated atomic shield purchase into `HabitRepository.purchaseStreakShield` using `CoinLedgerDao`, `HabitDao`, and `TransactionRunner` in `HabitRepositoryImpl`.
  - Added `TestFakes.kt` update to prevent compilation breaks in test suite.
  - Added explicit sealed interface `PurchaseStreakShieldResult` with typed outcomes (`Success`, `InsufficientCoins`, `CapReached`, `HabitNotFound`, `Error`).
  - Added dynamic habit selection rule for Companion shop purchases when `targetHabitId == null`.
  - Fixed database seeding for existing installs in `EconomyRepositoryImpl.ensureDefaultCatalogAndProfile`.
  - Added guard in `EconomyRepositoryImpl.purchaseShopItem` against polluting `CompanionProfile.activeAccessoryId` or `isUnlocked` for utility items.
  - Added cold-start guard (`previousLevel != null`) for companion level-up celebrations.
  - Removed `.take(4)` truncation in `CompanionScreen.kt` to accommodate cosmetics + utility items.
  - Reordered task list to prioritize domain contracts and unit tests before UI wiring.

## Design Notes

- **Consumable vs Cosmetic Separation:** In `EconomyRepositoryImpl`, cosmetic items (`isUnlocked = true`) are permanent one-time unlocks. In contrast, utility items (`CATEGORY_UTILITY`) are repeatable consumables: buying a shield deducts coins via `HabitRepository.purchaseStreakShield(...)` and immediately increments the target habit's `slackBankDays` (up to 3) without altering `companionProfile.activeAccessoryId` or setting permanent `isUnlocked = true` on the catalog item.
- **Milestone Detection:** Streak milestones are evaluated at `streak > 0 && streak % 7 == 0` in `HabitsViewModel` to reward weekly consistency.
- **Reduced Motion Contract:** `RewardMotionStage` listens to `isReducedMotion` and triggers `onLottieComplete()` immediately on launch effect without composing `RewardAnimation`, preserving accessibility compliance.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED` (0 `android.*` imports in domain)
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All 8 H0 components valid`
- `.\gradlew test` -- expected: All JVM unit tests passing clean (including new `PurchaseStreakShieldUseCaseTest`)
- `.\gradlew lintDebug` -- expected: 0 errors
