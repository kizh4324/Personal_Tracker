---
title: 'Story 1.8: DayType Schedule Resolution & Morning Template Swap'
type: 'feature'
created: '2026-10-23'
status: 'done'
baseline_commit: 'ab1fbeb5f896d4df65a72fe92cc4e91d25d81735'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app lacks deterministic provisioning and reactive swapping of DayType schedule templates (Standard, Rest, Crunch, Low Capacity). Tapping the morning check-in banner or DayType chip on the Home screen navigates to Settings instead of enabling 1-tap template selection, and calendar overrides do not dynamically scale uncompleted schedule items.

**Approach:** Implement built-in template provisioning in `DayTypeRepository`, pure domain use case `SwitchDayTypeUseCase` supporting date-specific `DayOverride` persistence and non-destructive schedule scaling via `AdaptiveScheduleEngine`, reactive MVI wiring in `HomeViewModel`, and a stateless, design-token-compliant `DayTypeSelectorDialog` with 48dp touch targets and zero guilt copy.

## Boundaries & Constraints

**Always:**
- Strictly conform to `ARCHITECTURE-SPINE.md` (AD-1 through AD-8) and `_bmad-output/project-context.md`.
- Pure Kotlin domain (`com.personaltracker.domain`): zero `android.*` or `androidx.*` dependencies (AD-1).
- Resolution hierarchy: 1) Date-Specific User Override > 2) Day-of-Week Default > 3) Default Template (`isDefault == true`) > 4) Safe Fallback.
- Dynamic date evaluation (EXP-017): always resolve `LocalDate.now().toString()` inside reactive transforms to support midnight boundary rollovers without stale schedule state.
- Database mutations must execute inside Room `@Transaction` blocks on `Dispatchers.IO` (AD-3). Never use `OnConflictStrategy.REPLACE` on entities with cascading foreign keys (EXP-003).
- Zero-guilt microcopy: all template names, descriptions, and swap confirmations must be supportive, calming, and non-punitive (AD-4).
- Design token exclusivity (AD-7): resolve all colors, typography, shapes, and spacing from `com.personaltracker.ui.theme`. Minimum interactive touch target is 48dp (`spacing.minTouchTarget`, EXP-008).
- Headless-first / Figma-ready UI boundary (EXP-016): maintain strict split between stateful `HomeRoute` and stateless `HomeScreenContent`.

**Ask First:**
- Altering the schema of `DayTypeEntity` or `DayOverrideEntity`.
- Adding additional third-party UI or dialog dependencies.

**Never:**
- Never import Android framework classes into `com.personaltracker.domain`.
- Never penalize user streaks, deduct coins, or reduce companion energy when selecting "Rest" or "Low Capacity" templates (AD-4).
- Never mutate or delete already-completed routine steps or historical logs during mid-day template swaps.
- Never write hardcoded `dp`, `sp`, or hex color literals in UI composables.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default Template Resolution | App opens on Tuesday with no override | Resolves "Standard Routine" (day-of-week match for Tuesday) | If DB empty, seeds default templates and falls back to safe default |
| Morning Template Swap | User taps "Swap DayType" on Morning Banner, selects "Rest / Recovery" | Persists `DayOverride` for today only; `HomeUiState` immediately updates `activeDayType` to Rest; banner reflects update; no base templates mutated | Database write wrapped on `Dispatchers.IO`; emits calming error snackbar on failure |
| Mid-day Template Swap with Completed Items | User swaps to "Low Capacity" mid-day after completing 1 routine step | `SwitchDayTypeUseCase` preserves completed steps; uncompleted steps scaled dynamically via `AdaptiveScheduleEngine.scaleRoutineDurations`; streak intact | Uncompleted step durations clamped to minimum 1 minute |
| Reset Override to Default | User with active override taps "Reset to Default" in selector | Deletes `DayOverride` for today; active schedule reverts to day-of-week default ("Standard Routine") | Safe no-op if no override exists |
| Midnight Rollover | Midnight occurs while app is open | Flow aggregation dynamically queries new `LocalDate.now()`; previous day's override is disregarded; new day's template resolves automatically | Safe resolution without app restart |
| Empty Database | Fresh install with zero DayTypes in DB | `DayTypeRepository` seeds 4 built-in templates (Standard, Rest, Crunch, Low Capacity) deterministically | Fallback to `ResolveDayTypeUseCase.SAFE_DEFAULT_DAY_TYPE` if initial query fails |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/DayType.kt` -- DayType domain model and built-in template catalog definitions.
- `app/src/main/java/com/personaltracker/domain/repository/DayTypeRepository.kt` -- Contract for template queries, override persistence, and default seeding.
- `app/src/main/java/com/personaltracker/data/repository/DayTypeRepositoryImpl.kt` -- Implementation with IO dispatching, DAO mapping, and deterministic default template seeding.
- `app/src/main/java/com/personaltracker/domain/usecase/ResolveDayTypeUseCase.kt` -- 4-tier hierarchy resolution (`Override` > `DayOfWeek` > `Default` > `Fallback`).
- `app/src/main/java/com/personaltracker/domain/usecase/SwitchDayTypeUseCase.kt` -- Domain interactor managing date override persistence, routine duration scaling, and zero-guilt schedule updates.
- `app/src/main/java/com/personaltracker/ui/components/overlays/DayTypeSelectorDialog.kt` -- Stateless Compose dialog displaying available DayTypes, active badge, and reset option with 48dp touch targets and design tokens.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- Expose `availableDayTypes`, `activeDayType`, `isDayOverrideActive`, and `isDayTypeSelectorOpen`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- Add `OpenDayTypeSelector`, `DismissDayTypeSelector`, `SelectDayType(id)`, and `ResetDayTypeOverride`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiEffect.kt` -- Add `DayTypeChanged(name)`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Combine `dayTypesFlow` and `overrideFlow` reactively, handle selector intents, and dispatch `SwitchDayTypeUseCase`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Host `DayTypeSelectorDialog` in `HomeScreenContent` and wire `onSwapDayType` / `DayTypeChip` clicks.
- `app/src/test/java/com/personaltracker/domain/ResolveDayTypeUseCaseTest.kt` -- Unit test resolution hierarchy tiers.
- `app/src/test/java/com/personaltracker/domain/SwitchDayTypeUseCaseTest.kt` -- Unit test override creation, routine scaling, and override deletion.
- `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Unit test reactive DayType state updates and selector intents with Turbine.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/DayType.kt` -- Define built-in template catalog (`Standard`, `Rest / Recovery`, `Crunch Focus`, `Low Capacity`).
- [x] `app/src/main/java/com/personaltracker/domain/repository/DayTypeRepository.kt` & `data/repository/DayTypeRepositoryImpl.kt` -- Add `ensureDefaultDayTypes()` with deterministic seeding when empty.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/ResolveDayTypeUseCase.kt` -- Ensure robust 4-tier resolution hierarchy and fallback to default templates.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/SwitchDayTypeUseCase.kt` -- Implement date override persistence, routine duration scaling via `AdaptiveScheduleEngine`, and override reset.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt`, `HomeUiIntent.kt`, `HomeUiEffect.kt` -- Update MVI contracts to expose available templates, override status, and selector actions.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/DayTypeSelectorDialog.kt` -- Implement 100% stateless dialog with 48dp minimum touch target floor and token conformance.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- Reactively combine `getAllDayTypes()` and `observeOverrideForDate()` (EXP-017) and handle swap/reset intents.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Wire `DayTypeSelectorDialog` and banner/chip click callbacks.
- [x] `app/src/test/java/com/personaltracker/domain/` -- Add unit tests for `ResolveDayTypeUseCase` and `SwitchDayTypeUseCase`.
- [x] `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Update Turbine tests verifying reactive DayType swaps and dialog intents.

**Acceptance Criteria:**
- Given the application database, when empty, then `DayTypeRepository` provisions 4 default templates (`Standard`, `Rest / Recovery`, `Crunch Focus`, `Low Capacity`).
- Given `ResolveDayTypeUseCase`, when resolving schedule for a date, then it strictly respects the hierarchy: `Date-Specific User Override` > `Day-of-Week Default` > `Default Template` > `Safe Fallback`.
- Given `HomeViewModel` and `HomeScreenContent`, when the user taps "Swap DayType" on the Morning Banner or the DayType chip, then `DayTypeSelectorDialog` opens displaying all templates.
- Given the user selects a new DayType in `DayTypeSelectorDialog`, then a `DayOverride` is saved for today only on `Dispatchers.IO`, `HomeUiState` updates reactively, uncompleted routine steps scale safely, and 0 guilt copy is displayed.
- Given the user taps "Reset to Default" when an override is active, then the override is removed and today's schedule reverts to the day-of-week template.
- Given the JVM unit test suite, when `.\gradlew test` and `.\gradlew lintDebug` are executed, then all tests pass with 0 errors.

## Spec Change Log

## Design Notes

- **Zero-Guilt Copy & Hierarchy**:
```kotlin
// Built-in templates in DayType.kt
val BUILT_IN_DAY_TYPES = listOf(
    DayType("dt_standard", "Standard Routine", "#3B82F6", 90, isDefault = true, defaultDaysOfWeek = "1,2,3,4,5"),
    DayType("dt_rest", "Rest / Recovery", "#10B981", 30, isDefault = false, defaultDaysOfWeek = "6,7"),
    DayType("dt_crunch", "Crunch Focus", "#8B5CF6", 180, isDefault = false, defaultDaysOfWeek = ""),
    DayType("dt_low_capacity", "Low Capacity", "#F59E0B", 20, isDefault = false, defaultDaysOfWeek = "")
)
```

## Verification

**Commands:**
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `total_findings: 0`
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `.\gradlew test` -- expected: All tests pass on standard JVM
- `.\gradlew lintDebug` -- expected: 0 errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All H0 baseline components verified`

## Suggested Review Order

**Domain Engine & Interactor**

- Entry point: template swap interactor with non-destructive schedule scaling and FK safety
  [`SwitchDayTypeUseCase.kt:35`](../../app/src/main/java/com/personaltracker/domain/usecase/SwitchDayTypeUseCase.kt#L35)

- 4-tier schedule resolution hierarchy with date-matching override checks
  [`ResolveDayTypeUseCase.kt:33`](../../app/src/main/java/com/personaltracker/domain/usecase/ResolveDayTypeUseCase.kt#L33)

- Built-in DayType catalog definitions (Standard, Rest, Crunch, Low Capacity)
  [`DayType.kt:14`](../../app/src/main/java/com/personaltracker/domain/model/DayType.kt#L14)

- Date-specific override guard in schedule engine preventing yesterday's override bleed
  [`AdaptiveScheduleEngine.kt:41`](../../app/src/main/java/com/personaltracker/domain/engine/AdaptiveScheduleEngine.kt#L41)

**Data Layer & Persistent Seeding**

- Deterministic template auto-seeding on empty database initialization
  [`DayTypeRepositoryImpl.kt:47`](../../app/src/main/java/com/personaltracker/data/repository/DayTypeRepositoryImpl.kt#L47)

- DAO count and list queries supporting synchronous seeding verification
  [`DayTypeDao.kt:23`](../../app/src/main/java/com/personaltracker/data/local/db/daos/DayTypeDao.kt#L23)

**UI Presentation & MVI Wiring**

- Stateless 48dp minimum touch target dialog with token conformance and accessibility
  [`DayTypeSelectorDialog.kt:45`](../../app/src/main/java/com/personaltracker/ui/components/overlays/DayTypeSelectorDialog.kt#L45)

- Reactive multi-flow observation combining DayTypes and date overrides with EXP-017 compliance
  [`HomeViewModel.kt:100`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt#L100)

- Dialog hosting in Home content with Toast feedback integration
  [`HomeScreen.kt:50`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt#L50)

- TopContextBar DayTypeChip click wired to Home selector dialog
  [`PersonalTrackerNavHost.kt:76`](../../app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt#L76)

**Verification & Tests**

- Hierarchy tier unit tests including override priority and fallback
  [`ResolveDayTypeUseCaseTest.kt:35`](../../app/src/test/java/com/personaltracker/domain/usecase/ResolveDayTypeUseCaseTest.kt#L35)

- Date-specific override persistence and non-destructive routine duration scaling tests
  [`SwitchDayTypeUseCaseTest.kt:45`](../../app/src/test/java/com/personaltracker/domain/usecase/SwitchDayTypeUseCaseTest.kt#L45)

- Reactive StateFlow and Channel effect testing with Turbine
  [`HomeViewModelTest.kt:127`](../../app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt#L127)

- Repository auto-seeding unit test against empty DAO
  [`RepositoryTest.kt:425`](../../app/src/test/java/com/personaltracker/data/repository/RepositoryTest.kt#L425)

