# EGM Pre-Code Gate Review & Synthesis: Story 4.1

**Artifact Under Review:** [`spec-4-1-habit-entity-models-cadence-tracking-streak-grace-engine.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/_bmad-output/implementation-artifacts/spec-4-1-habit-entity-models-cadence-tracking-streak-grace-engine.md)  
**Date:** 2026-11-14  
**Epic:** Epic 4 — Gamification Economy, Streak State Machine & Non-Punishing Companion  
**Story:** Story 4.1 — Habit Entity Models, Cadence Tracking & Streak Grace Engine  
**Participants:**
- **The Elephant (Context Author / Spec Designer)**
- **Goldfish Swarm Reviewers:**
  - `egm-comprehension` (Comprehension Auditor)
  - `egm-design` (Hostile Design Critic)
  - `egm-codebase-details` (Technical Quality Auditor)
  - `egm-implementation-readiness` (Readiness Gatekeeper)

---

## 1. Summary of Goldfish Swarm Findings

| ID | Reviewer | Severity | Area | Summary of Finding | Resolution in Revised Spec |
|:---|:---|:---|:---|:---|:---|
| **F-01** | `egm-comprehension`, `egm-design`, `egm-implementation-readiness` | **Blocker** | Domain Model | `WeeklyFrequency` had no operational daily evaluation rules; risked rapid false penalties. | Formalize Sunday-boundary weekly evaluation and flexible weekday non-penalization (`FLEXIBLE_PENDING` / `REST_DAY`). |
| **F-02** | `egm-design` | **Blocker** | Migration | `MIGRATION_2_3` used `DEFAULT 'DAILY'`, corrupting existing custom `targetDaysOfWeek` habits. | Add conditional SQL migration: `UPDATE habits SET cadenceType = 'SPECIFIC_DAYS' WHERE targetDaysOfWeek != '1,2,3,4,5,6,7' AND targetDaysOfWeek != ''` + fallback in mapper. |
| **F-03** | `egm-comprehension`, `egm-design`, `egm-codebase-details`, `egm-implementation-readiness` | **Blocker** | UI / Engine | `FlameState.GRACE_SHIELD` was disconnected: `getFlameState` took only `Int`, `HabitStreakCard` dropped `flameState`. | Update `getFlameState(streak: Int, isGraceShieldActive: Boolean)` and pass `flameState` through ViewModel, Screen, and Card to `StreakFlameBadge`. |
| **F-04** | `egm-design`, `egm-implementation-readiness`, `egm-comprehension` | **Blocker** | Domain Engine | `StreakStateMachine.evaluateDay` lacked `DayType` parameter; `DayRolloverCoordinator` resolved today's DayType instead of target date's DayType. | Update signature to `evaluateDay(habit, isCompleted, logDate, activeDayType: DayType?)` and resolve DayType for the evaluation `targetDate`. |
| **F-05** | `egm-implementation-readiness` | **Blocker** | Use Case | `EvaluateHabitStreaksUseCase` lacked constructor dependencies, invocation signature, and persistence contract. | Fully specify class declaration, dependencies (`HabitRepository`, `DayTypeRepository`, `ResolveDayTypeUseCase`, `StreakStateMachine`, `TransactionRunner`), and signature. |
| **F-06** | `egm-codebase-details` | **Blocker** | Build / Compile | Adding `GRACE_SHIELD` to `FlameState` enum broke exhaustive `when` in `StreakFlameBadge.kt`. | Group addition of `FlameState.GRACE_SHIELD` with `ic_flame_shield.xml` and `when` branches (`colors.info`) in atomic task. |
| **F-07** | `egm-design` | **Major** | Domain / Repo | `LogHabitUseCase` blocked completion if a prior `REST_DAY` or `MISSED` log existed for today; concurrent taps threw uncaught errors. | Allow upgrading existing non-completed logs to `COMPLETED` with coins, and handle duplicate idempotency conflicts gracefully. |
| **F-08** | `egm-comprehension`, `egm-codebase-details`, `egm-implementation-readiness` | **Major** | Data / UI | `VerticalCapsuleBars` hardcoded fitness data ("440 lb", "200 ft"); `HabitsUiState` had no fields for dynamic bars. | Add `capsuleMetrics: List<VerticalBarMetric>` to `HabitsUiState`, specify 7-day compliance formula, and replace fitness stats with habit insights. |
| **F-09** | `egm-comprehension`, `egm-codebase-details`, `egm-implementation-readiness` | **Major** | Domain Model | Status string divergence (`"FROZEN"` vs `"GRACE_SHIELD_ABSORBED"`). | Define canonical `HabitLogStatus` object with backward-compatible aliases in `HabitLog.kt`. |
| **F-10** | `egm-codebase-details` | **Major** | Persistence | `HabitDao.kt` missing from Code Map & Tasks; lacked batch/list queries. | Add `HabitDao.kt` to Code Map and Tasks with non-Flow list accessor. |
| **F-11** | `egm-design` | **Major** | Platform Worker | Multi-day gaps ignored by rollover if device was powered off across days. | `EvaluateHabitStreaksUseCase` supports chronological gap iteration across missing dates. |
| **F-12** | `egm-design` | **Major** | Persistence | `goalTargetCount` column lacked multi-count progression in logs. | Clarify `goalTargetCount` baseline contract in v1 (default 1) without premature partial-log overhead. |
| **F-13** | `egm-design` | **Nit** | ViewModel | `HabitsViewModel.handleCreateHabit` hardcoded `slackBankDays = 2`. | Set default `slackBankDays = 0` on creation, adhering to domain rule `SLACK_EARN_CADENCE_DAYS = 7`. |
| **F-14** | `egm-implementation-readiness` | **Nit** | Testing | Test target path clarification. | Reference verified path `app/src/test/java/com/personaltracker/ui/HabitsAndStudyViewModelTest.kt` and add unit test tasks. |

---

## 2. Elephant Architectural Resolutions

### Resolution 1: Formalizing `HabitCadence` and Weekly Evaluation
`HabitCadence` hierarchy in pure Kotlin:
```kotlin
enum class CadenceType {
    DAILY,
    SPECIFIC_DAYS,
    WEEKLY_FREQUENCY
}

sealed interface HabitCadence {
    val type: CadenceType

    data object Daily : HabitCadence {
        override val type = CadenceType.DAILY
    }

    data class SpecificDays(val daysOfWeek: Set<Int>) : HabitCadence {
        override val type = CadenceType.SPECIFIC_DAYS
    }

    data class WeeklyFrequency(val targetTimesPerWeek: Int) : HabitCadence {
        override val type = CadenceType.WEEKLY_FREQUENCY
    }
}
```
**Weekly Cadence Evaluation Rules:**
- For `Daily`: scheduled on all days (1..7).
- For `SpecificDays`: scheduled on matching `daysOfWeek` (1..7). Unscheduled days receive status `REST_DAY` (0 slack consumed).
- For `WeeklyFrequency`: On Mon–Sat daily evaluation, uncompleted habits receive status `REST_DAY` without penalty. On Sunday midnight rollover, if total completions during the calendar week (Mon–Sun) is $\ge \text{targetTimesPerWeek}$, streak is preserved. If $<\text{targetTimesPerWeek}$, grace shield or slack bank day is absorbed, or streak resets if protections are exhausted.

### Resolution 2: Non-Destructive `MIGRATION_2_3`
```sql
ALTER TABLE habits ADD COLUMN cadenceType TEXT NOT NULL DEFAULT 'DAILY';
ALTER TABLE habits ADD COLUMN weeklyTargetFrequency INTEGER NOT NULL DEFAULT 7;
ALTER TABLE habits ADD COLUMN reminderTime TEXT;
ALTER TABLE habits ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE habits ADD COLUMN goalTargetCount INTEGER NOT NULL DEFAULT 1;
ALTER TABLE habits ADD COLUMN category TEXT;
UPDATE habits SET cadenceType = 'SPECIFIC_DAYS' WHERE targetDaysOfWeek != '1,2,3,4,5,6,7' AND targetDaysOfWeek != '';
```

### Resolution 3: Complete `EvaluateHabitStreaksUseCase` Contract
```kotlin
class EvaluateHabitStreaksUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val dayTypeRepository: DayTypeRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val streakStateMachine: StreakStateMachine,
    private val transactionRunner: TransactionRunner
) {
    suspend operator fun invoke(
        targetDate: String = LocalDate.now().minusDays(1).toString()
    ): EvaluateHabitStreaksResult
}
```
Evaluates each habit against the resolved `DayType` for `targetDate`. If a habit has missing logs spanning multiple dates, evaluates dates chronologically.

### Resolution 4: End-to-End `FlameState.GRACE_SHIELD` Pipeline
- `StreakStateMachine.getFlameState(streak: Int, isGraceShieldActive: Boolean = false): FlameState`
- `StreakFlameBadge.kt`: Added `R.drawable.ic_flame_shield` and `colors.info` token.
- `HabitStreakCard.kt`: Parameter `flameState: FlameState? = null` passed directly to `StreakFlameBadge`.
- `HabitsViewModel.kt`: Computes `flameState = streakStateMachine.getFlameState(habit.currentStreak, isGraceShieldActive = (habit.isFreezeActive || latestLog?.status in setOf(HabitLogStatus.GRACE_SHIELD_ABSORBED, HabitLogStatus.SLACK_USED)))`.

### Resolution 5: Dynamic `VerticalCapsuleBars` with Warm Organic Tokens
- `HabitsUiState` contains `capsuleMetrics: List<VerticalBarMetric>` and `weeklySummaryPoints: Int`.
- `HabitsViewModel` calculates 7-day completion percentages per habit, colored by `colors.barDeepCocoa`, `colors.barWarmRust`, `colors.barMatchaOlive`, `colors.barSoftOrchid`.
- `VerticalCapsuleBars.kt` replaces fitness dummy copy with: Active Streaks, Weekly Consistency %, and Total Completions.

---

## 3. Pre-Code Gate Verdict

With all 14 findings comprehensively resolved in the updated specification:
- **Comprehension Check:** PASSED
- **Hostile Design Review:** PASSED
- **Codebase Details Audit:** PASSED
- **Implementation Readiness:** **GO (READY FOR DEVELOPMENT)**
