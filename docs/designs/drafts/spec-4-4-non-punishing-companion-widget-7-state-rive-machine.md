---
title: 'Story 4.4: Non-Punishing Companion Widget, 7-State Machine & Cosmetics Shop'
type: 'feature'
created: '2026-11-14'
status: 'done'
review_loop_iteration: 1
context:
  - '_bmad-output/implementation-artifacts/epic-4-context.md'
  - 'ARCHITECTURE-SPINE.md'
  - '.harness2/EXPERIENCE.md'
  - 'docs/design/companion-asset-brief.md'
  - 'docs/designs/reviews/review-4-4-gate.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The virtual companion pet is currently a static or disconnected placeholder on the Companion screen without dynamic emotional responsiveness to the user's daily journey (streaks, active focus sessions, or rest days). Furthermore, the cosmetics shop lacks a robust atomic purchase and equip lifecycle, leaving users unable to spend earned Coins or customize their companion with tangible cosmetic items.

**Approach:** Implement a pure Kotlin 7-state non-punishing companion behavioral engine (`CompanionStateEngine.kt`, `ResolveCompanionMoodUseCase.kt`) driving a tactile animated mascot widget across `CompanionScreen.kt` and `HomeScreen.kt`, integrated with an atomic encrypted cosmetics shop purchase and equip engine (`PurchaseOrEquipShopItemUseCase.kt`, `EconomyRepository.purchaseShopItem`, `equipShopItem`) supporting a 2x2 Wardrobe Catalog (`Cozy Scarf`, `Study Cap`, `Golden Crown`, `Warm Coffee`, etc.) with zero guilt, zero HP loss, and zero penalty mechanics (AD-4).

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** Companion mood resolution, leveling math, and shop purchase validation remain 100% pure Kotlin in `domain/` with zero `android.*` or `androidx.*` dependencies.
- **AD-4 Non-Punishing Mechanics:** Zero HP loss, zero starvation, zero happiness decay, and zero character death. Missing habits, overdue tasks, or broken streaks NEVER trigger sad, sick, crying, or angry expressions. Rest days (`LOW_CAPACITY`, `REST_RECOVERY`, `"dt_rest"`, `"dt_low_capacity"`) map exclusively to supportive `RESTING_PEACEFUL` or `ENCOURAGING_WARM` states.
- **AD-6 Idempotent Economy & Gross Spend Defense:** Every cosmetic purchase executes under a deterministic compound idempotency key `SHOP_BUY_{itemId}` inside Room `@Transaction` blocks on `Dispatchers.IO`. Coin deductions record negative `deltaAmount = -price` (`EconomyEventTypes.SHOP_PURCHASE`), preserving the invariant that daily earn caps (`WHERE deltaAmount > 0`, EXP-045) are never deflated by spending.
- **AD-7 Token Conformance & Touch Targets:** 100% of colors, dimensions, and typography resolve from `PersonalTrackerTheme` tokens (0 raw hex/dp/sp literals in UI). Minimum interactive touch target floor is 48dp (`spacing.minTouchTarget`).
- **Figma-Ready MVI Split:** Screens maintain single `UiState`, single `UiIntent`, and Channel-backed `UiEffect`, splitting stateful `CompanionRoute` from 100% stateless `CompanionScreenContent`.

**Ask First:**
- Modifying baseline shop catalog item prices or adding new cosmetic categories beyond Outfit, Accessory, and Background Decor.
- Changing the Level-up XP threshold (`ENERGY_PER_LEVEL = 100`).

**Never:**
- Never allow a shop purchase to complete if coin balance is insufficient (`balance < item.priceCoins`).
- Never allow coin balances to drop below 0 (`balanceAfter >= 0`).
- Never display punitive or shame-inducing speech bubble copy (e.g., "You let me down", "I'm starving").
- Never write production code during design phase (Strict EGM Elephant invariant).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **Standard Shop Purchase** | User taps "Get" on Study Cap (40 coins) with balance = 65 | 40 coins deducted (`balance = 25`), transaction `SHOP_BUY_item_cap` committed, item unlocked and immediately equipped; celebration lottie plays; HUD counter updates | If balance < 40, shows gentle snackbar "Not enough coins yet. Keep going!" and aborts without deduction |
| **Equip / Unequip Toggle** | User taps "Equip" on already unlocked Cozy Scarf | `CompanionProfile.activeAccessoryId` updated to `item_scarf`; button changes to "Equipped" (dark pill); mascot visually wears scarf | Tapping "Equipped" toggles unequip (`activeAccessoryId = null`) |
| **Active Study Session Mood** | User starts Focus timer on Study screen | Companion mood transitions immediately to `FOCUSING_STUDY` (4); displays serene meditative pose with slow breathing cycle | Resets back to `IDLE_COZY` or `CELEBRATING_REWARD` when session ends |
| **Low Capacity / Rest Day** | Active DayType is `LOW_CAPACITY` or `REST_RECOVERY` (`dt_rest` / `dt_low_capacity`) | Companion mood transitions to `RESTING_PEACEFUL` (5) with supportive copy: "Rest day preserved. Taking it easy today." | Missed items do not drop mood or trigger guilt dialogs |
| **Task / Habit Completion** | User checks off high-priority task or habit | Companion transitions to `CELEBRATING_REWARD` (1) with joyful hop and star particles for 3s before settling to Idle | Concurrent coin arc flies to HUD without interrupting companion bounce |
| **7-Day Streak / Level Up** | Habit reaches 7-day streak or energy reaches 100/100 | Companion transitions to `HAPPY_BOUNCE` (6) with celebratory particle ribbons; Level increments | XP wraps cleanly (`energy % 100`) without losing excess energy |
| **Rapid Double Purchase** | User taps purchase button twice in 100ms | First tap commits purchase; second tap detects `isUnlocked = true`, treats as equip toggle without deducting duplicate coins | Handled idempotently inside atomic transaction runner; returns `PurchaseOrEquipResult.AlreadyUnlocked` without crashing |
| **Offline Cold Start** | App launched without network with existing companion state | Companion profile and unlocked wardrobe load instantaneously from encrypted SQLCipher DB; renders equipped accessories | Default Sproutling profile and default 2x2 shop items seeded if database table is empty |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/engine/CompanionState.kt` -- `[Create]` Canonical pure Kotlin 7-state companion emotional register enum (`IDLE_COZY=0`, `CELEBRATING_REWARD=1`, `ENCOURAGING_WARM=2`, `CONCERNED_CHECKIN=3`, `FOCUSING_STUDY=4`, `RESTING_PEACEFUL=5`, `HAPPY_BOUNCE=6`) aligned 1:1 with Rive runtime technical contract and emoji representations (AD-1, AD-4).
- `app/src/main/java/com/personaltracker/domain/engine/CompanionStateEngine.kt` -- `[Create]` Pure Kotlin 7-state behavioral engine evaluating non-punishing mood transitions from DayType (`dt_rest`, `dt_low_capacity`), study timer state, streak events, and XP leveling (AD-1, AD-4).
- `app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt` -- `[Modify]` Align legacy `CompanionMood` ordinals and deprecate in favor of `CompanionState`, preventing symbol redeclaration.
- `app/src/main/java/com/personaltracker/domain/model/CompanionProfile.kt` -- `[Modify]` Pure Kotlin companion profile domain model with species name, level, energy units, active accessory ID, active background ID, and current expression ordinal.
- `app/src/main/java/com/personaltracker/domain/model/ShopItem.kt` -- `[Modify]` Pure Kotlin cosmetic shop item model (`id`, `category`, `name`, `priceCoins`, `isUnlocked`, `unlockedTimestamp`, `iconEmoji`).
- `app/src/main/java/com/personaltracker/domain/usecase/ResolveCompanionMoodUseCase.kt` -- `[Create]` Pure Kotlin use case evaluating current user context (active timer, DayType, streak milestone) into a deterministic `CompanionState`.
- `app/src/main/java/com/personaltracker/domain/usecase/PurchaseOrEquipShopItemUseCase.kt` -- `[Create]` Pure Kotlin use case coordinating atomic purchase validation, coin deduction, and equip/unequip toggling returning typed `PurchaseOrEquipResult`.
- `app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt` -- `[Modify]` Add `equipShopItem(itemId, equip)`, implement idempotent purchase handling (no throwing on duplicate tap/already unlocked), and seed canonical 2x2 catalog (`DEFAULT_SHOP_ITEMS`) and default `CompanionProfile`.
- `app/src/main/java/com/personaltracker/ui/components/companion/CompanionView.kt` -- `[Modify]` Import domain `CompanionState`, enhance mascot stage with spring breathing physics (`shopPurchaseBurstSpringSpec`), equipped accessory badges, and reduced-motion bypass.
- `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionUiState.kt` & `CompanionUiIntent.kt` -- `[Modify]` Update state to use domain `CompanionState`, add `EquipItem(itemId, equip)` intent.
- `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionViewModel.kt` -- `[Modify]` MVI ViewModel integrating `PurchaseOrEquipShopItemUseCase` and `ResolveCompanionMoodUseCase`, exposing reactive `CompanionUiState`.
- `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionScreen.kt` -- `[Modify]` Full 2x2 Wardrobe & Cosmetics Shop grid, Lime Green energy progress indicator, supportive speech bubble copy, dynamic equip checks, remove fake `145` coin mock, and enforce 48dp minimum touch target floor (`spacing.minTouchTarget`, AD-7).
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` & `HomeViewModel.kt` -- `[Modify]` Expose `companionMood: CompanionState` and `activeAccessoryId: String?` derived from companion profile flow.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- `[Modify]` Upgrade top greeting companion avatar badge to 48dp clickable surface navigating to `PersonalTrackerDestination.COMPANION` and displaying live mood and accessory.
- `app/src/test/java/com/personaltracker/domain/engine/CompanionStateEngineTest.kt` -- `[Create]` Comprehensive JVM unit test suite verifying 7-state non-punishing transitions, DayType immunity, XP leveling, duplicate purchase idempotency, and equip toggles.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/engine/CompanionState.kt` -- Create canonical pure Kotlin 7-state enum with Rive ordinals 0..6, names, and emojis.
- [x] `app/src/main/java/com/personaltracker/domain/engine/CompanionStateEngine.kt` -- Implement pure Kotlin behavioral engine with `resolveMood(dayTypeId, isStudying, streakAtRisk, recentEvent)` and `addEnergy(profile, energyDelta)`.
- [x] `app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt` -- Align `CompanionMood` ordinals (1=Celebrating, 2=Encouraging) and deprecate in favor of `CompanionState`.
- [x] `app/src/main/java/com/personaltracker/domain/model/ShopItem.kt` -- Add `iconEmoji: String` helper property without altering Room schema.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/ResolveCompanionMoodUseCase.kt` -- Implement mood resolution use case integrating active DayType (`"dt_rest"`, `"dt_low_capacity"`), study timer, and recent achievements.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/PurchaseOrEquipShopItemUseCase.kt` -- Implement atomic purchase and equip/unequip use case returning `PurchaseOrEquipResult` (Success, InsufficientCoins, AlreadyEquipped, Error).
- [x] `app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt` -- Add `equipShopItem(itemId, equip)`, implement catalog auto-seeding with `DEFAULT_SHOP_ITEMS`, and handle idempotent double-purchases gracefully.
- [x] `app/src/main/java/com/personaltracker/ui/components/companion/CompanionView.kt` -- Upgrade companion mascot composable with 7-state visualizer, spring breathing animation (`shopPurchaseBurstSpringSpec`), equipped accessory badges, and reduced-motion bypass.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionUiState.kt` & `CompanionUiIntent.kt` -- Wire domain `CompanionState` and `EquipItem` intent.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionViewModel.kt` -- Wire `PurchaseOrEquipShopItemUseCase` into `onIntent(CompanionUiIntent.PurchaseOrEquipItem)`, handle equip toggles, and emit celebratory effects.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionScreen.kt` -- Enhance 2x2 wardrobe grid with single-tap equip/unequip states ("Equipped" vs "Equip" vs "Get $X"), Lime Green energy bar (`colors.accentLime`), supportive speech bubble, remove fake `145` coin mock, and enforce 48dp minimum touch target floor (AD-7).
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` & `HomeViewModel.kt` & `HomeScreen.kt` -- Wire reactive companion profile to top greeting avatar badge with 48dp touch floor navigating to `PersonalTrackerDestination.COMPANION`.
- [x] `app/src/test/java/com/personaltracker/domain/engine/CompanionStateEngineTest.kt` -- Unit test 7-state mood resolution, DayType rest mapping, leveling math ($XP \ge 100$), duplicate purchase protection, and equip toggles.

**Acceptance Criteria:**
- Given a user viewing the Companion screen, when an active study session is in progress, then the companion displays the `FOCUSING_STUDY` state with peaceful concentration.
- Given an active DayType of `LOW_CAPACITY` or `REST_RECOVERY` (`dt_rest` or `dt_low_capacity`), when the Companion screen loads, then the companion displays `RESTING_PEACEFUL` with supportive copy ("Rest day preserved. Taking it easy today.") and never displays guilt or sadness.
- Given a user with 50 coins viewing the shop, when tapping "Get" on the Study Cap (40 coins), then 40 coins are deducted, the item is unlocked, automatically equipped, and the companion displays the equipped cap badge.
- Given an already unlocked cosmetic item, when the user taps "Equip", then the item becomes active on the companion and the button updates to "Equipped" (dark pill); tapping again un-equips the item.
- Given a user with insufficient coins (e.g. 10 coins for a 40-coin item), when tapping "Get", then coins are not deducted and a gentle non-punitive notice ("Not enough coins yet. Keep going!") is displayed.
- Given rapid double-tapping on a purchase button, when processed, then coins are deducted only once and the second tap acts idempotently without throwing an exception or crashing the app.
- Given the top greeting row on `HomeScreen`, when rendered, then it displays the active companion mood emoji and accessory, and when tapped (48dp touch floor), navigates smoothly to `PersonalTrackerDestination.COMPANION`.
- Given any interactive button, chip, or item card on `CompanionScreen`, when rendered, then it strictly conforms to a minimum 48dp touch target floor (`spacing.minTouchTarget`) and 100% `PersonalTrackerTheme` semantic tokens (0 raw hex/dp literals).

## Spec Change Log

- **Iteration 1 (2026-11-14):**
  - **Resolution 1 (Enum & Ordinal Collision):** Unified into canonical `CompanionState(0..6)` matching Rive runtime contract; deprecated/aligned `CompanionMood` in `GamificationLedgerEngine.kt`.
  - **Resolution 2 (Signatures):** Defined complete Kotlin signatures for `CompanionStateEngine`, `ResolveCompanionMoodUseCase`, `PurchaseOrEquipShopItemUseCase`, and `PurchaseOrEquipResult`.
  - **Resolution 3 (Concurrency):** Made `EconomyRepositoryImpl.purchaseShopItem` idempotent; returns safe result instead of throwing `IllegalStateException`.
  - **Resolution 4 (Code Map):** Added missing State/Intent targets (`CompanionUiIntent.kt`, `CompanionUiState.kt`, `HomeUiState.kt`, `HomeViewModel.kt`).
  - **Resolution 5 (Touch Floor):** Upgraded purchase buttons in `CompanionScreen.kt` from `spacing.chipHeight` (32dp) to `spacing.minTouchTarget` (48dp).
  - **Resolution 6 (Mock Removal):** Removed hardcoded `145` coin mock in `CompanionScreen.kt#L143`; bound directly to `state.coinBalance`.
  - **Resolution 7 (Equip Logic):** Replaced hardcoded `item_scarf` equip check with dynamic `item.id == state.activeAccessoryId`.
  - **Resolution 8 (Catalog Seeding):** Added `DEFAULT_SHOP_ITEMS` auto-seeding in `EconomyRepositoryImpl` to prevent empty DB crashes.
  - **Resolution 9 (DayType Drift):** Mapped database string IDs (`"dt_rest"`, `"dt_low_capacity"`) in `ResolveCompanionMoodUseCase`.
  - **Resolution 10 (Schema):** Added `iconEmoji` property to pure domain `ShopItem` without modifying `ShopItemEntity` schema.

## Design Notes

### 1. Canonical 7 Non-Punishing Companion States
```kotlin
package com.personaltracker.domain.engine

/**
 * Sourced 1:1 from docs/design/companion-asset-brief.md and design-system.md.
 * Rive State Machine Input: "state" (Number, 0.0 to 6.0).
 */
enum class CompanionState(val ordinalValue: Int, val stateName: String, val emoji: String) {
    IDLE_COZY(0, "Idle / Content", "🦊"),
    CELEBRATING_REWARD(1, "Celebrating", "🎉"),
    ENCOURAGING_WARM(2, "Encouraging", "✨"),
    CONCERNED_CHECKIN(3, "Concerned", "🧐"),
    FOCUSING_STUDY(4, "Focused", "🧘"),
    RESTING_PEACEFUL(5, "Sleeping / Resting", "💤"),
    HAPPY_BOUNCE(6, "Excited", "⚡");

    val riveValue: Int get() = ordinalValue

    companion object {
        fun fromOrdinal(ordinal: Int): CompanionState =
            entries.firstOrNull { it.ordinalValue == ordinal } ?: IDLE_COZY

        fun fromRiveValue(value: Int): CompanionState = fromOrdinal(value)
    }
}
```

### 2. Pure Kotlin Companion Behavioral Engine Contract
```kotlin
package com.personaltracker.domain.engine

class CompanionStateEngine @Inject constructor() {
    companion object {
        const val ENERGY_PER_LEVEL = 100
    }

    fun resolveMood(
        dayTypeId: String?,
        isStudySessionActive: Boolean,
        isStreakAtRisk: Boolean = false,
        recentTrigger: CompanionTriggerEvent = CompanionTriggerEvent.Idle
    ): CompanionState {
        if (isStudySessionActive) return CompanionState.FOCUSING_STUDY
        if (dayTypeId == "dt_rest" || dayTypeId == "dt_low_capacity") return CompanionState.RESTING_PEACEFUL
        return when (recentTrigger) {
            is CompanionTriggerEvent.TaskCompleted -> CompanionState.CELEBRATING_REWARD
            is CompanionTriggerEvent.HabitCompleted -> {
                if (recentTrigger.streak >= 7 && recentTrigger.streak % 7 == 0) CompanionState.HAPPY_BOUNCE
                else CompanionState.CELEBRATING_REWARD
            }
            is CompanionTriggerEvent.MilestoneReached -> CompanionState.HAPPY_BOUNCE
            is CompanionTriggerEvent.StreakAtRisk -> CompanionState.CONCERNED_CHECKIN
            is CompanionTriggerEvent.FreezeUsed,
            is CompanionTriggerEvent.ReturnAfterAbsence -> CompanionState.ENCOURAGING_WARM
            is CompanionTriggerEvent.EveningRest -> CompanionState.RESTING_PEACEFUL
            is CompanionTriggerEvent.FocusSessionStarted -> CompanionState.FOCUSING_STUDY
            is CompanionTriggerEvent.FocusSessionCompleted -> CompanionState.CELEBRATING_REWARD
            is CompanionTriggerEvent.Idle -> {
                if (isStreakAtRisk) CompanionState.CONCERNED_CHECKIN else CompanionState.IDLE_COZY
            }
        }
    }

    fun addEnergy(profile: CompanionProfile, energyDelta: Int): CompanionProfile {
        require(energyDelta >= 0) { "Energy earned cannot be negative: $energyDelta" }
        if (energyDelta == 0) return profile
        val safeCurrentEnergy = maxOf(0, profile.currentEnergyUnits)
        val safeLevel = maxOf(1, profile.level)
        val totalEnergy = try { Math.addExact(safeCurrentEnergy, energyDelta) } catch (_: ArithmeticException) { Int.MAX_VALUE }
        val levelsGained = totalEnergy / ENERGY_PER_LEVEL
        val newLevel = try { Math.addExact(safeLevel, levelsGained) } catch (_: ArithmeticException) { Int.MAX_VALUE }
        val remainderEnergy = totalEnergy % ENERGY_PER_LEVEL
        val newMood = if (levelsGained > 0) CompanionState.HAPPY_BOUNCE.ordinalValue else profile.currentExpressionOrdinal
        return profile.copy(level = newLevel, currentEnergyUnits = remainderEnergy, currentExpressionOrdinal = newMood)
    }
}
```

### 3. Purchase & Equip Use Case Contract
```kotlin
package com.personaltracker.domain.usecase

sealed interface PurchaseOrEquipResult {
    data class PurchasedAndEquipped(val item: ShopItem, val newBalance: Int) : PurchaseOrEquipResult
    data class Equipped(val item: ShopItem) : PurchaseOrEquipResult
    data class Unequipped(val item: ShopItem) : PurchaseOrEquipResult
    data class InsufficientCoins(val required: Int, val current: Int) : PurchaseOrEquipResult
    data class Error(val message: String) : PurchaseOrEquipResult
}

class PurchaseOrEquipShopItemUseCase @Inject constructor(
    private val economyRepository: EconomyRepository
) {
    suspend operator fun invoke(item: ShopItem, equipOnlyToggle: Boolean = false): PurchaseOrEquipResult
}
```

### 4. Wardrobe Catalog Seeding Contract
```kotlin
val DEFAULT_SHOP_ITEMS = listOf(
    ShopItem(id = "item_scarf", category = "OUTFIT", name = "Cozy Scarf", priceCoins = 25, isUnlocked = true),
    ShopItem(id = "item_cap", category = "ACCESSORY", name = "Study Cap", priceCoins = 40, isUnlocked = false),
    ShopItem(id = "item_crown", category = "ACCESSORY", name = "Golden Crown", priceCoins = 100, isUnlocked = false),
    ShopItem(id = "item_coffee", category = "ACCESSORY", name = "Warm Coffee", priceCoins = 15, isUnlocked = false)
)
```

### 5. Atomic Shop Transaction Formulation
- Purchase Key: `SHOP_BUY_${item.id}`
- Ledger Event: `EconomyEventTypes.SHOP_PURCHASE` with `deltaAmount = -priceCoins`
- Single atomic transaction updates:
  1. `coin_ledger` entry inserted with negative delta.
  2. `shop_items` entry updated with `isUnlocked = true`.
  3. `companion_profile` updated with `activeAccessoryId = item.id` (auto-equip).

## Verification

**Commands:**
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `total_findings: 0`.
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`.
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid.
- `.\gradlew test` -- expected: All unit tests pass, including new `CompanionStateEngineTest`.
- `.\gradlew lintDebug` -- expected: Android lint completes with 0 errors.
- Artemis Gate 6 Real-Device Verification (if device connected): `mobile_get_device_state` and `mobile_run_task` confirming companion mood transitions and wardrobe equip on hardware.
