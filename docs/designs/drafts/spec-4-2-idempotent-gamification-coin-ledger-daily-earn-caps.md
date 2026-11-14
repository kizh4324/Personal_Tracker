---
title: 'Story 4.2: Idempotent Gamification Coin Ledger & Daily Earn Caps'
type: 'feature'
created: '2026-11-14'
status: 'done'
baseline_commit: '0eff11cd96c83243afdf784718b69751a8bb3c00'
review_loop_iteration: 1
context:
  - '_bmad-output/implementation-artifacts/epic-4-context.md'
  - 'ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Gamified productivity systems suffer from coin inflation and gaming exploits when users repeatedly toggle items off and on or spawn dummy entries to mint infinite rewards. Conversely, clumsy anti-cheat mechanisms punish honest users with harsh lockouts or unexplainable reward losses (violating AD-4).

**Approach:** Implement a pure Kotlin gamification ledger engine and daily earn cap policy (`GamificationLedgerEngine`, `AwardCoinsUseCase`, `GetDailyEconomySummaryUseCase`) with tiered rewards (`TASK_COMPLETION`, `HABIT_COMPLETION` with streak multipliers, `STUDY_SESSION`, `ROUTINE_COMPLETION`). Enforce strict idempotency keys (`TASK_COMP_{taskId}`, `HABIT_LOG_{habitId}_{date}`, `STUDY_SESSION_{sessionId}`, `ROUTINE_COMP_{routineId}_{date}`), non-punitive daily category caps (`TASK_CAP = 50`, `HABIT_CAP = 40`, `FOCUS_CAP = 120`, `GLOBAL_FARMABLE_CAP = 100`), 5-second undo reversal support (`CAPTURE_UNDO_{inboxId}` with `"CAPTURE_REVERSAL"`), and atomic Room ledger queries driving live `CoinPill` balance and unclipped `CoinArcAnimation` triggers.

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** Zero `android.*` or `androidx.*` dependencies in `domain/` (`GamificationLedgerEngine.kt`, `CoinTransaction.kt`, `DailyEconomySummary.kt`, `AwardCoinsResult.kt`, `AwardCoinsUseCase.kt`, `GetDailyEconomySummaryUseCase.kt`).
- **AD-3 Encrypted Persistence & Atomicity:** Multi-entity operations across coin ledger and domain entities execute inside Room `@Transaction` blocks on `Dispatchers.IO` via `TransactionRunner`.
- **AD-4 Non-Punishing UX:** Hitting daily earn caps never blocks completion, never shows red/danger warning copy, and never shame-penalizes the user. Completions are celebrated with gentle affirmative copy ("Daily coin goal reached! Awesome work today.").
- **AD-6 Idempotency Invariant:** Every coin-earning event must enforce compound idempotency `{DOMAIN}_{ACTION}_{ID}` backed by SQLite `UNIQUE` index on `idempotencyKey`. Duplicate submissions return the existing transaction record with 0 duplicate coins without UI error.
- **AD-7 Token Conformance:** All UI additions to `CoinPill`, reward HUDs, and sheets strictly resolve from `PersonalTrackerTheme` tokens (0 raw hex/dp/sp literals, 48dp minimum touch target floor).

**Ask First:**
- Altering the baseline reward values (Task: 5/7/10, Habit base: 5, Study: 5–15) or daily earn caps (Task: 50, Habit: 40, Global: 100).
- Altering the 5-second quick capture undo clawback window.

**Never:**
- Never allow coin balances to drop below 0 (`balanceAfter >= 0`).
- Never allow Coins to purchase streak freezes in v1 (FR-8.5).
- Never write production code during design phase (Strict EGM Elephant invariant).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| :--- | :--- | :--- | :--- |
| **Standard Task Reward** | Urgent task completed, today's task coins = 20 | Awards +10 coins, new balance updated, key `TASK_COMP_{id}` recorded, companion XP +10 | Duplicate submission -> returns existing 0-coin result (`isDuplicate = true`) |
| **Task Daily Cap Reached** | Important task (+7) completed, today's task coins = 48 (cap 50) | Clamps reward to +2 coins, daily task cap now reached (50/50), `isCapReached = true` | Zero negative deltas; logs awarded delta only |
| **Task Cap Exceeded** | Routine task (+5) completed when today's task coins = 50 | Awards 0 coins, task completes normally, non-punitive notice "Daily coin goal reached! Awesome work today." | Task state transitions to `COMPLETED` regardless |
| **Habit Streak Tier Bonus** | Habit with 14-day streak (`BLAZING`) logged | Base 5 + 3 (Blazing) + 10 (7-day milestone multiple) = 18 coins, key `HABIT_LOG_{id}_{date}` | Idempotency guard prevents multi-toggle farm |
| **Focus Session Scaling** | 50-minute deep work focus session completed | (50 / 6).coerceAtLeast(5) = 8 coins awarded into ledger under `STUDY_SESSION_{id}` | Sessions < 10 mins award 0 coins; exempt from farmable cap |
| **Capture 5s Undo Reversal** | High-confidence voice task undone within 5s window | Reversal entry inserted with `CAPTURE_UNDO_{inboxId}`, `eventType = "CAPTURE_REVERSAL"`, delta = -earnedCoins, balance deducted | If balance < earnedCoins, clamps deduction to current balance (`balanceAfter = 0`) |
| **Rapid Double Toggle** | User taps task checkbox twice in 100ms | First commits task and ledger; second detects duplicate key, returns cleanly without crash (`isDuplicate = true`) | Catches conflict inside transaction runner / returns existing record |
| **Daily Rollover Cap Reset** | Device clock crosses midnight (00:00 local time) | Daily earned accumulator resets to 0; new calendar day begins with fresh earn quotas | Start-of-day timestamp computed from `LocalDate.now()` |
| **Gross Spend Query Defense** | User spends 100 coins in shop or reverses capture | Daily earned queries filter `deltaAmount > 0`, so spend/clawback never inflates remaining earn cap | `WHERE deltaAmount > 0` in all sum queries |

</frozen-after-approval>

## Architectural Contracts & Signatures

### 1. Canonical Event Types
```kotlin
object EconomyEventTypes {
    const val TASK_COMPLETION = "TASK_COMPLETION"
    const val HABIT_COMPLETION = "HABIT_COMPLETION"
    const val STUDY_SESSION = "STUDY_SESSION"
    const val ROUTINE_COMPLETION = "ROUTINE_COMPLETION"
    const val CAPTURE_REVERSAL = "CAPTURE_REVERSAL"
    const val SHOP_PURCHASE = "SHOP_PURCHASE"
}
```

### 2. Domain Data Models
```kotlin
package com.personaltracker.domain.model

data class DailyEconomySummary(
    val taskCoins: Int,
    val habitCoins: Int,
    val focusCoins: Int,
    val routineCoins: Int,
    val totalEarnedToday: Int,
    val currentBalance: Int,
    val isTaskCapReached: Boolean,
    val isHabitCapReached: Boolean,
    val isFocusCapReached: Boolean,
    val isGlobalFarmableCapReached: Boolean
)

data class AwardCoinsResult(
    val coinsAwarded: Int,
    val requestedCoins: Int,
    val isCapReached: Boolean,
    val isDuplicate: Boolean,
    val transaction: CoinTransaction? = null
)
```

### 3. GamificationLedgerEngine Pure Math & Caps
```kotlin
package com.personaltracker.domain.engine

object GamificationLedgerEngine {
    const val DAILY_TASK_CAP = 50
    const val DAILY_HABIT_CAP = 40
    const val DAILY_FOCUS_CAP = 120
    const val DAILY_GLOBAL_FARMABLE_CAP = 100

    val FARMABLE_EVENT_TYPES = setOf(
        EconomyEventTypes.TASK_COMPLETION,
        EconomyEventTypes.HABIT_COMPLETION,
        EconomyEventTypes.ROUTINE_COMPLETION
    )

    fun calculateTaskReward(intensity: DeliveryIntensity): Int = when (intensity) {
        DeliveryIntensity.ROUTINE -> 5
        DeliveryIntensity.IMPORTANT -> 7
        DeliveryIntensity.URGENT -> 10
    }

    fun calculateHabitReward(habit: Habit): Int {
        val base = 5
        val streakBonus = when (habit.streakTier) {
            StreakTier.COLD -> 0
            StreakTier.WARMING -> 1
            StreakTier.BURNING -> 2
            StreakTier.BLAZING -> 3
        }
        val milestoneBonus = if (habit.currentStreak > 0 && habit.currentStreak % 7 == 0) 10 else 0
        return base + streakBonus + milestoneBonus
    }

    fun calculateStudyReward(durationMinutes: Long): Int {
        if (durationMinutes < 10) return 0
        return (durationMinutes / 6).toInt().coerceIn(5, 15)
    }

    fun calculateRoutineStepReward(): Int = 2

    fun calculateCappedReward(
        requestedAmount: Int,
        categoryEarned: Int,
        categoryCap: Int,
        globalFarmableEarned: Int = 0,
        globalFarmableCap: Int = Int.MAX_VALUE,
        isFarmableCategory: Boolean = false
    ): Int {
        if (requestedAmount <= 0) return 0
        val categoryRemaining = maxOf(0, categoryCap - categoryEarned)
        val afterCategory = minOf(requestedAmount, categoryRemaining)
        return if (isFarmableCategory) {
            val globalRemaining = maxOf(0, globalFarmableCap - globalFarmableEarned)
            minOf(afterCategory, globalRemaining)
        } else {
            afterCategory
        }
    }

    fun generateTaskCompletionKey(taskId: String): String = "TASK_COMP_$taskId"
    fun generateHabitLogKey(habitId: String, date: LocalDate): String = "HABIT_LOG_${habitId}_$date"
    fun generateStudySessionKey(sessionId: String): String = "STUDY_SESSION_$sessionId"
    fun generateRoutineStepKey(routineId: String, date: LocalDate): String = "ROUTINE_COMP_${routineId}_$date"
    fun generateCaptureUndoKey(inboxId: String): String = "CAPTURE_UNDO_$inboxId"
}
```

### 4. Room DAO Query Contracts (`CoinLedgerDao.kt`)
```kotlin
@Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND deltaAmount > 0")
suspend fun getDailyEarnedTotal(startOfDayTimestamp: Long): Int

@Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND deltaAmount > 0")
fun observeDailyEarnedTotal(startOfDayTimestamp: Long): Flow<Int>

@Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND eventType = :eventType AND deltaAmount > 0")
suspend fun getDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Int

@Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND eventType = :eventType AND deltaAmount > 0")
fun observeDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Flow<Int>

@Query("SELECT * FROM coin_ledger WHERE timestamp >= :timestamp ORDER BY timestamp DESC, id DESC")
fun getTransactionsSince(timestamp: Long): Flow<List<CoinLedgerEntity>>
```

### 5. Repository Contract (`EconomyRepository.kt`)
```kotlin
interface EconomyRepository {
    fun observeBalance(): Flow<Int>
    suspend fun getBalance(): Int
    suspend fun getDailyEarnedTotal(startOfDay: Long): Int
    fun observeDailyEarnedTotal(startOfDay: Long): Flow<Int>
    suspend fun getDailyEarnedForCategory(startOfDay: Long, eventType: String): Int
    fun observeDailyEarnedForCategory(startOfDay: Long, eventType: String): Flow<Int>
    fun observeTransactionsSince(timestamp: Long): Flow<List<CoinTransaction>>
    suspend fun recordTransaction(transaction: CoinTransaction): Result<Unit>
    suspend fun recordReversal(inboxId: String, amount: Int, idempotencyKey: String): Result<CoinTransaction>
    suspend fun awardCoins(
        eventType: String,
        sourceEntityId: String,
        requestedAmount: Int,
        idempotencyKey: String,
        startOfDayTimestamp: Long
    ): AwardCoinsResult
}
```

### 6. Atomic Transaction Boundaries (AD-3, AD-6)
- **Task & Habit Completions:**
  To guarantee atomic database consistency without splitting transactions:
  - `CompleteTaskUseCase`:
    1. Computes capped coin award by querying today's task category total and global farmable total from `EconomyRepository`.
    2. Calls `TaskRepository.completeTaskWithReward(taskId, idempotencyKey, coinReward = cappedCoins)`.
    3. If `isCapReached` and `cappedCoins == 0`, passes an affirmative feedback effect/notification.
    4. If idempotency duplicate occurs, `TaskRepositoryImpl` catches collision gracefully and returns task completion without error.
  - `LogHabitUseCase`:
    1. Evaluates streak transition via `StreakStateMachine.evaluateCompletion(habitToEvaluate)`.
    2. Computes capped coin award using `evaluation.updatedHabit` streak tier and 7-day milestone rules, querying category and global totals from `EconomyRepository`.
    3. Calls `HabitRepository.logHabitWithReward(habitId, logDate, idempotencyKey, coinReward = cappedCoins)`.
- **Standalone Awards & Reversals:**
  - `AwardCoinsUseCase` handles standalone awards (`STUDY_SESSION`, `ROUTINE_COMPLETION`) and coordinates with `EconomyRepository.awardCoins`.
  - Reversals (`CAPTURE_REVERSAL`) execute via `EconomyRepository.recordReversal`, clamping the negative delta so the balance never drops below 0.

### 7. Tactile UI & CoinPill Contract (AD-7)
```kotlin
@Composable
fun CoinPill(
    balance: Int,
    modifier: Modifier = Modifier,
    dailyEarned: Int? = null,
    dailyCap: Int? = null,
    onPositionMeasured: (Offset) -> Unit = {},
    onClick: (() -> Unit)? = null
)
```
- Content description: `if (dailyEarned != null && dailyCap != null) "$balance coins • Today: $dailyEarned/$dailyCap" else "$balance coins"`
- Interactive touch target floor: 48dp minimum touch target preserved on container if interactive.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/EconomyEventTypes.kt` & `DailyEconomySummary.kt` & `AwardCoinsResult.kt` -- Create domain models representing canonical event types, daily economy summary, and safe deduplicated award results (AD-1).
- [x] `app/src/main/java/com/personaltracker/domain/engine/GamificationLedgerEngine.kt` -- Expand engine with reward policies: habit streak tier bonuses (`COLD`: +0, `WARMING`: +1, `BURNING`: +2, `BLAZING`: +3, 7-day milestone: +10), routine step reward (2 coins), daily cap constants (`DAILY_TASK_CAP = 50`, `DAILY_HABIT_CAP = 40`, `DAILY_FOCUS_CAP = 120`, `DAILY_GLOBAL_FARMABLE_CAP = 100`), two-tier clamp function `calculateCappedReward(...)`, and idempotency key generators (AD-1).
- [x] `app/src/main/java/com/personaltracker/data/local/db/daos/CoinLedgerDao.kt` -- Add queries with `COALESCE(SUM(deltaAmount), 0)` and `WHERE deltaAmount > 0`: `getDailyEarnedTotal`, `observeDailyEarnedTotal`, `getDailyEarnedForCategory`, `observeDailyEarnedForCategory`, and `getTransactionsSince`.
- [x] `app/src/main/java/com/personaltracker/domain/repository/EconomyRepository.kt` & `app/src/main/java/com/personaltracker/data/repository/EconomyRepositoryImpl.kt` -- Implement category earn queries, safe duplicate key resolution (returning `AwardCoinsResult(isDuplicate = true)` instead of throwing uncaught exceptions), clamped reversal recording, and `awardCoins` coordination.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/AwardCoinsUseCase.kt` -- Central use case validating daily caps, computing awards, and orchestrating standalone transactions (AD-6).
- [x] `app/src/main/java/com/personaltracker/domain/usecase/GetDailyEconomySummaryUseCase.kt` -- Use case combining balance and category earn totals into reactive `DailyEconomySummary`.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/CompleteTaskUseCase.kt` & `app/src/main/java/com/personaltracker/domain/usecase/LogHabitUseCase.kt` -- Wire cap calculations into atomic `completeTaskWithReward` and `logHabitWithReward` transactions, passing updated habit states for streak bonus evaluation and affirmative cap messages (AD-4).
- [x] `app/src/main/java/com/personaltracker/ui/components/atoms/CoinPill.kt` -- Enhance with backward-compatible optional `dailyEarned` and `dailyCap`, updated accessibility semantics, and 48dp touch floor (AD-7).
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt`, `HabitsViewModel.kt`, `StudyViewModel.kt`, `CompanionViewModel.kt` -- Expose reactive daily economy state and non-punitive cap reached messages.
- [x] `app/src/test/java/com/personaltracker/domain/engine/GamificationLedgerEngineTest.kt` -- Unit test tiered rewards, streak multipliers, two-tier cap clamping, and key generation.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/AwardCoinsUseCaseTest.kt` & `app/src/test/java/com/personaltracker/data/repository/EconomyRepositoryTest.kt` -- Unit test idempotency deduplication, daily cap clamping, 5s undo clawbacks, and concurrent tap safety.

**Acceptance Criteria:**
- Given a task completion event, when processed, then a coin transaction with deterministic key `TASK_COMP_{taskId}` is committed and duplicate completion attempts award 0 coins without throwing UI errors.
- Given a user who has earned 48 task coins today (cap 50), when completing an Important task (+7 coins), then exactly +2 coins are awarded, bringing today's task total to 50, and subsequent task completions today award 0 coins with affirmative notice "Daily coin goal reached! Awesome work today." while tasks still complete normally.
- Given a habit logged with a 14-day streak, when rewarded, then base (5) + Blazing bonus (3) + 7-day milestone bonus (10) = 18 coins are awarded, subject to habit daily caps.
- Given an unconfirmed voice task capture undone within 5 seconds, when undo executes, then a clawback transaction (`CAPTURE_UNDO_{inboxId}`) deducts the awarded coins from the balance without allowing balance to drop below 0.
- Given concurrent completion requests for the same item, when executed in parallel, then SQLite unique constraint guarantees exactly one coin reward is inserted, with duplicate attempts handled gracefully.
- Given a user who spends coins in the shop or reverses a capture, today's daily earned progress does not decrement or allow re-farming of capped rewards.

## Spec Change Log

- **Iteration 1 (Post-Goldfish Swarm Audit):**
  - Standardized all event types to canonical codebase constants (`TASK_COMPLETION`, `HABIT_COMPLETION`, `STUDY_SESSION`, `ROUTINE_COMPLETION`, `CAPTURE_REVERSAL`, `SHOP_PURCHASE`).
  - Added `COALESCE(SUM(deltaAmount), 0)` and `WHERE deltaAmount > 0` to all Room DAO sum queries to prevent SQL nulls and spend leakage.
  - Clarified two-tier clamping logic in `calculateCappedReward` and explicitly restricted `DAILY_GLOBAL_FARMABLE_CAP` to tasks, habits, and routines (exempting `STUDY_SESSION`).
  - Added formal data classes: `DailyEconomySummary`, `AwardCoinsResult`.
  - Clarified single-transaction atomicity: `CompleteTaskUseCase` and `LogHabitUseCase` evaluate capped coin amounts and pass them into atomic repository methods (`completeTaskWithReward` / `logHabitWithReward`).
  - Specified graceful duplicate idempotency handling in `EconomyRepositoryImpl` returning `AwardCoinsResult(isDuplicate = true)` rather than crashing with unhandled `IllegalStateException`.
  - Added backward-compatible parameters to `CoinPill(dailyEarned: Int? = null, dailyCap: Int? = null)` with 48dp touch floor and updated semantics.
  - Specified evaluation of habit streak bonus using `evaluation.updatedHabit` in `LogHabitUseCase`.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED` (0 `android.*` in domain).
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `total_findings: 0`.
- `.\gradlew test` -- expected: All unit tests pass with 0 errors.
- `.\gradlew lintDebug` -- expected: Android lint completes with 0 errors.
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid.
