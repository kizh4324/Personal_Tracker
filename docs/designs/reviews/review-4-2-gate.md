# EGM Stage 2 Review Report: Story 4.2 (Idempotent Gamification Coin Ledger & Daily Earn Caps)

**Date:** 2026-11-14  
**Story:** 4.2 (Mapped to `4-2-idempotent-coin-ledger-engine-anti-farming-model`)  
**Spec Audited:** [`spec-4-2-idempotent-gamification-coin-ledger-daily-earn-caps.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/implementation-artifacts/spec-4-2-idempotent-gamification-coin-ledger-daily-earn-caps.md)  
**Goldfish Reviewers:**
1. `egm-comprehension` (Conversation `3bb912c1-f5f8-4f16-b0ea-ffbf847c5999`)
2. `egm-design` (Conversation `bcf4001b-54f6-4170-b0e6-62d90e5146e2`)
3. `egm-codebase-details` (Conversation `d386702f-d7ea-4e11-9b78-28f2e34c07dd`)
4. `egm-implementation-readiness` (Conversation `26f6c842-c7f3-4e5b-b909-9e9a443e2a16`)

---

## 1. Executive Summary & Review Verdict

All 4 stateless Goldfish reviewers completed exhaustive, zero-context evaluations of the initial Story 4.2 draft spec against the production Kotlin codebase.
The reviewers reached unanimous consensus on 8 key architectural and contract requirements across domain purity (AD-1), persistence atomicity (AD-3), non-punishing UX (AD-4), idempotency (AD-6), and design system tokens (AD-7).

The Elephant has resolved every finding directly in the spec (Iteration 1).

**Pre-Review Initial Verdict:** ⚠️ ACTION REQUIRED (Blockers & Majors identified)  
**Post-Synthesis Final Verdict:** 🟢 **GO FOR HUMAN CHECKPOINT 1 APPROVAL**

---

## 2. Review Findings & Resolution Matrix

| # | Severity | Finding | Reviewer(s) | Resolution in Spec Iteration 1 |
|---|---|---|---|---|
| 1 | **Blocker** | **Event Type String Drift**: Initial spec referenced `HABIT_LOG`, `FOCUS_SESSION`, `ROUTINE_STEP`, whereas codebase entities and existing DAOs use `HABIT_COMPLETION`, `STUDY_SESSION`, `ROUTINE_COMPLETION`. Category sum queries would have returned 0. | `egm-codebase-details`, `egm-implementation-readiness` | Defined explicit `EconomyEventTypes` object matching canonical strings: `TASK_COMPLETION`, `HABIT_COMPLETION`, `STUDY_SESSION`, `ROUTINE_COMPLETION`, `CAPTURE_REVERSAL`, `SHOP_PURCHASE`. |
| 2 | **Blocker** | **DAO SQL Nullability & Negative Delta Leak**: `SELECT SUM(deltaAmount) ...` returns SQL `NULL` on empty days (causing crashes) and includes negative transactions (shop spend and undo clawbacks), which would decrement the earned total and allow infinite cap refarming. | `egm-design`, `egm-codebase-details`, `egm-implementation-readiness` | Mandated `COALESCE(SUM(deltaAmount), 0)` and `WHERE deltaAmount > 0` across all daily sum queries in `CoinLedgerDao`. Provided both `suspend` and `Flow` signatures. |
| 3 | **Major** | **Idempotent Key Collision Handling**: Existing `EconomyRepositoryImpl` throws `IllegalStateException` on duplicate keys, violating the requirement that rapid double-taps return 0 coins without UI errors. | `egm-design`, `egm-codebase-details` | Specified `AwardCoinsResult(coinsAwarded = 0, isDuplicate = true)` contract. `EconomyRepositoryImpl` catches key collision inside `TransactionRunner` and returns the existing transaction cleanly. |
| 4 | **Major** | **Transaction Atomicity Split (AD-3, AD-6)**: Having `CompleteTaskUseCase` and `LogHabitUseCase` delegate coin persistence to a separate `AwardCoinsUseCase` would split database mutations across two independent transactions. | `egm-design`, `egm-codebase-details` | Preserved single-transaction atomicity: `CompleteTaskUseCase` / `LogHabitUseCase` compute the capped coin amount first and pass it into atomic `completeTaskWithReward` / `logHabitWithReward` in Room `@Transaction`. `AwardCoinsUseCase` orchestrates standalone events (`STUDY_SESSION`, `ROUTINE_COMPLETION`, `CAPTURE_REVERSAL`). |
| 5 | **Major** | **Two-Tier Clamping & Focus Exemption**: If `DAILY_GLOBAL_FARMABLE_CAP = 100` applied to all events, deep focus sessions could never reach `DAILY_FOCUS_CAP = 120`. | `egm-design`, `egm-codebase-details`, `egm-comprehension` | Defined `FARMABLE_EVENT_TYPES = setOf(TASK_COMPLETION, HABIT_COMPLETION, ROUTINE_COMPLETION)`. `STUDY_SESSION` is exempt from the 100 global farmable cap, governed exclusively by `DAILY_FOCUS_CAP = 120`. Defined `calculateCappedReward` mathematical contract. |
| 6 | **Major** | **Missing Data Model Contracts**: `DailyEconomySummary` and `AwardCoinsResult` lacked explicit fields and nullability contracts. | `egm-implementation-readiness`, `egm-comprehension` | Added full data class definitions with non-nullable primitive fields and category booleans (`isTaskCapReached`, `isHabitCapReached`, `isFocusCapReached`, `isGlobalFarmableCapReached`). |
| 7 | **Major** | **CoinPill Compatibility & 48dp Touch Floor (AD-7)**: Adding progress indicators must not break existing callers or drop the 48dp interactive floor. | `egm-codebase-details`, `egm-implementation-readiness` | Specified optional default arguments: `dailyEarned: Int? = null, dailyCap: Int? = null`, 48dp container touch target floor, and semantic accessibility text format: `"$balance coins • Today: $dailyEarned/$dailyCap"`. |
| 8 | **Nit** | **Habit Streak Evaluation Timing**: Passing pre-completion habit to `calculateHabitReward` misses streak increment and 7-day milestone bonus. | `egm-codebase-details` | Explicitly specified that `calculateHabitReward` receives `evaluation.updatedHabit` produced by `StreakStateMachine.evaluateCompletion`. |
| 9 | **Nit** | **Non-Punitive Cap Feedback (AD-4)**: When daily cap is saturated, user must receive affirmative feedback without interrupting task completion. | `egm-design`, `egm-comprehension` | Added affirmative snackbar copy: `"Daily coin goal reached! Awesome work today."` Completions remain 100% successful. |

---

## 3. Ready for Human Checkpoint 1

The spec is fully tightened, unambiguous, and locked under EGM rules. Proceed to prompt user for Checkpoint 1 confirmation (`[A] Approve` | `[E] Edit`).
