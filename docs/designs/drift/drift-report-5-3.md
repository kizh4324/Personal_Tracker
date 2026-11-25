# Architectural Drift Report: Story 5.3 (Urgency Soft-Cap Scarcity Guidance)

**Target Spec:** `docs/designs/drafts/spec-5-3-urgency-soft-cap-scarcity-guidance.md` (Iteration 1)  
**Date:** 2026-11-25  
**Audit Scope:** `app/src/main/` Kotlin/XML sources & unit tests vs approved specification  
**Drift Verdict:** **ZERO DRIFT (100% SPEC CONFORMANCE)**

---

## 1. Architectural Spine Invariant Audits (AD-1 to AD-8)

| Invariant | Standard | Implementation Verification | Status |
| :--- | :--- | :--- | :--- |
| **AD-1: Domain Purity** | Zero `android.*` or `androidx.*` imports in `domain/` layer. | `UrgencyScarcityPolicy.kt` and `EvaluateTaskUrgencyScarcityUseCase.kt` contain 0 Android/Compose imports and no UI dialog strings. `domain_purity_check.py` passed. | **PASSED** |
| **AD-2: Unidirectional Reactive MVI** | Immutable `UiState`, sealed `UiIntent`, atomic state synchronization. | `HomeUiState` adds `UrgencyScarcityPromptState`, `homeActiveUrgentCount`, `homeUrgencyScarcityTier`, `maxActiveUrgent`, `remainingUrgentSlots`, `urgencyScarcityTier`, and `isSavingTask`; resolved via `ResolveUrgencyScarcity` & `DismissUrgencyScarcityPrompt`. | **PASSED** |
| **AD-4: Non-Punishing Autonomy** | Zero hard blocks, zero silent downgrades, zero restricted `n-a-g` words. | `UrgencyScarcityGuidanceDialog` offers `"Change to Important"`, `"Keep Urgent"`, and `"Keep Editing"`. Completing/deleting/cancelling active `URGENT` items reactively frees slots. 0 restricted words in Story 5.3 files. | **PASSED** |
| **AD-7: Design Tokens & Accessibility** | 100% `PersonalTrackerTheme` tokens, 48dp floor, TalkBack semantics. | Zero raw hex/dp/sp literals. Vertical 48dp buttons in dialog, 48dp close & `"Switch to Important"` buttons in `TaskEditorSheet`, `clearAndSetSemantics {}` on `"⚡"`, `semantics(mergeDescendants = true)` on `UrgencySlotStatusPill`, calm `colors.warning` at cap. | **PASSED** |

---

## 2. File & Component Level Drift Analysis

- **`UrgencyScarcityPolicy.kt` `[Created]`:** Enforces `maxActiveUrgent in 1..5` (default `2`) via `require(...)`. Matches 1:1.
- **`EvaluateTaskUrgencyScarcityUseCase.kt` `[Modified]`:** Defines `UrgencyScarcityTier` (`AVAILABLE`, `APPROACHING`, `AT_OR_OVER_CAP`), preserves 2-arg `invoke` delegates + `SOFT_CAP_THRESHOLD = 2` + backward-compatible `advisoryMessage` (with singular/plural polish), and adds non-defaulted 3-arg `invoke(..., policy)` overloads. Matches 1:1.
- **`HomeUiState.kt`, `HomeUiIntent.kt`, `HomeViewModel.kt` `[Modified]`:** Preserves constructor params, separates `homeUrgencyEvaluation` (`excludingTaskId = null`) from `editorUrgencyEvaluation` (`excludingTaskId = taskEditor.task?.id`), guards double-saves (`isSavingTaskInFlight` + `isSavingTask`), normalizes blank/whitespace dates, bypasses modal for same-date active-`URGENT` edits via `taskRepository.getTaskById`, resolves atomically with `getAndUpdate { null }`, and clears prompt in `OpenTaskEditor`, `DismissTaskEditor`, `DeleteTaskItem`, and `Refresh`. Matches 1:1 (plus defensive `isSavingTask` / `CancellationException` Step-4 patches).
- **`UrgencyScarcityGuidanceDialog.kt` `[Created]` & `TaskEditorSheet.kt`, `HomeScreen.kt`, `strings.xml` `[Modified]`:** Implements tokenized `UrgencySlotStatusPill`, editor `APPROACHING` hint + inline switch + `isSubmitting` reset, vertical 48dp dialog buttons with today/future-date & singular/plural resources, and 48dp `"Keep Editing"` accessible dismiss button.
- **Unit Tests:** `TaskManagementUseCasesTest.kt`, `HomeViewModelTest.kt`, and `ComponentStateTest.kt` verify all I/O Matrix cases (`558` Debug / `558` Release JVM tests, `0` failures).

---

## 3. Drift Verdict

**Zero architectural drift detected (0.0%).** All code, DI, MVI contracts, UI composables, and verification gates conform to `spec-5-3-urgency-soft-cap-scarcity-guidance.md` (Iteration 1).
