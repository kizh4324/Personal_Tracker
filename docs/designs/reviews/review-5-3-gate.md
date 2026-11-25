# EGM Pre-Implementation Gate Synthesis — Story 5.3

**Specification:** `_bmad-output/implementation-artifacts/spec-5-3-urgency-soft-cap-scarcity-guidance.md`  
**Story:** `5-3-urgency-soft-cap-scarcity-guidance`  
**Date:** `2026-11-25`  
**Iteration:** 0 -> Revision 1 (All Blockers, Criticals, and Majors Resolved)

---

## 1. Goldfish Swarm Gate Results (Iteration 0)

Four stateless zero-context Goldfish subagents audited the initial draft specification alongside referenced Kotlin/XML files with zero parent conversation context:

| Reviewer | Role | Initial Verdict | Key Findings |
| :--- | :--- | :--- | :--- |
| **`egm-comprehension`** | Stage 1 Comprehension Auditor | `FAIL` | Flagged `TaskEditorSheet` `isSubmitting` deadlock on dialog dismiss, undefined `APPROACHING` threshold for `max in 3..5`, Kotlin 3-arg overload ambiguity, Home pill count jumping when editor excludes edited task, missing `TaskEditorSheet` / dialog / `strings.xml` contracts, and incomplete `wasAlreadyActiveUrgent` bypass conditions. |
| **`egm-design`** | Stage 2 Hostile Design Critic | `FAIL` | Flagged the `isSubmitting` lockup (`BLOCKER`), double-tap/stale-prompt `HomeViewModel` concurrency races (`CRITICAL`), `today` vs `task.scheduledDate` dialog copy mismatch (`CRITICAL`), Kotlin overload/equality risks (`MAJOR`), 360dp portrait dialog button crowding & TalkBack gaps (`MAJOR`), and UI `choicePromptMessage` leakage into `domain/` (`MINOR`). |
| **`egm-codebase-details`** | Stage 2b Codebase Details Auditor | `NO-GO (Conditional)` | Independently verified symbols & theme tokens, caught `isSubmitting` lockup and Kotlin overload ambiguity (`BLOCKER`), caught `TaskEditorSheet` generating client `UUID` (`task.id.isNotBlank()` cannot detect new vs edited) and blank `scheduledDate` normalization mismatch (`MAJOR`), and aligned dialog/pill tokens with `CarryForwardReviewDialog` and `HomeScreen` pills. |
| **`egm-implementation-readiness`** | Stage 3 Implementation Auditor | `NO-GO` | Flagged `isSubmitting` deadlock (`BLOCKER`), missing `strings.xml` resource names/plurals (`BLOCKER`), ambiguous tier boundaries/overload collisions (`MAJOR`), `combine` 5-flow arity limit & policy provisioning (`MAJOR`), and missing Composable signatures & future-date test target (`MAJOR`). |

---

## 2. Consolidated Resolution Matrix

All blockers, criticals, majors, and nits were resolved cleanly in **Revision 1** (`review_loop_iteration: 1`):

1. **Eliminated `TaskEditorSheet` `isSubmitting` Deadlock (BLOCKER):**
   - Added `isScarcityPromptOpen: Boolean = false` to `TaskEditorSheet` (wired from `state.pendingUrgencyScarcityPrompt != null`) and reset `isSubmitting = false` via `LaunchedEffect(isScarcityPromptOpen)` so dismissing the guidance dialog restores the Save button immediately while keeping all user draft fields intact.
2. **Eliminated Kotlin Overload Resolution Ambiguity (BLOCKER):**
   - Mandated **non-defaulted** parameters on the 3-arg `EvaluateTaskUrgencyScarcityUseCase.invoke(tasks, excludingTaskId, policy)` and `suspend invoke(date, excludingTaskId, policy)` overloads so all existing 1-arg and 2-arg call sites resolve unambiguously to the preserved 2-arg delegates.
3. **Hardened Tier Rules & `domain/` Purity (MAJOR / MINOR):**
   - Defined strict order: `active >= max -> AT_OR_OVER_CAP`, else `active > 0 && active >= max - 1 -> APPROACHING`, else `AVAILABLE`, plus `remainingSlots = (maxActiveUrgent - activeUrgentCount).coerceAtLeast(0)`.
   - Removed `choicePromptMessage` from `TaskUrgencyScarcityResult` so UI wording stays 100% in `strings.xml` while retaining `advisoryMessage` for test compatibility.
   - Separated `homeActiveUrgentCount` / `homeUrgencyScarcityTier` (`excludingTaskId = null`) from `activeUrgentTaskCount` / `urgencyScarcityTier` (`excludingTaskId = taskEditor.task?.id`) so the Home pill never shifts while editing.
4. **Hardened MVI Concurrency, Target-Date Normalization & Bypass Logic (CRITICAL / MAJOR):**
   - Added `isSavingTaskInFlight` guard, atomic `pendingUrgencyScarcityControl.getAndUpdate { null }`, and cleanup inside `OpenTaskEditor`, `DismissTaskEditor`, `DeleteTaskItem`, and `observeDataStreams()`.
   - Normalized `targetDate = task.scheduledDate.ifBlank { LocalDate.now().toString() }` and resolved `wasAlreadyActiveUrgent` directly from `taskRepository.getTaskById(task.id)` checking intensity, active state (`!COMPLETED && !CANCELLED`), and matching `scheduledDate`.
   - Placed `pendingUrgencyScarcityControl` inside `HomeControlData` to stay cleanly under `combine`'s 5-flow typed arity limit, and kept `UrgencyScarcityPolicy()` defaulted without breaking `HomeViewModel` callers.
5. **Locked Exact `strings.xml`, Dialog, Pill & Test Contracts (BLOCKER / MAJOR):**
   - Defined all `strings.xml` keys (including singular/plural and today/other-date dialog prompts), stacked `fillMaxWidth()` dialog buttons (`spacing.minTouchTarget` floor), `CarryForwardReviewDialog`-aligned dialog styling, `colors.warning` calm cap pill tokens, TalkBack semantics, and future-date `HomeViewModelTest` coverage.

---

## 3. Gate Determination

- **All Critical / Blocker / Major findings resolved:** Yes (`100%`).
- **Ready for VibeWise Design Checkpoint & Human Approval:** Yes.
