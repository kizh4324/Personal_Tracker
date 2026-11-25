---
title: 'Story 5.3: Urgency Soft-Cap Scarcity Guidance'
type: 'feature'
created: '2026-11-25'
status: 'done'
baseline_commit: 'd96bf3701b93343780926a3b83299a19fc5e29fc'
review_loop_iteration: 1
context:
  - '_bmad-output/implementation-artifacts/epic-5-context.md'
  - '_bmad-output/implementation-artifacts/spec-5-2-acknowledgement-based-bounded-escalation-engine.md'
  - 'ARCHITECTURE-SPINE.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `EvaluateTaskUrgencyScarcityUseCase` hardcodes `SOFT_CAP_THRESHOLD = 2` without tiered slot states (`AVAILABLE`, `APPROACHING`, `AT_OR_OVER_CAP`), and neither `HomeScreen` nor `TaskEditorSheet` offers slot visibility or the non-blocking `"Keep Urgent"` vs `"Change to Important"` choice dialog.

**Approach:** Add pure Kotlin `UrgencyScarcityPolicy` (`1..5`, default `2`) and `UrgencyScarcityTier` with unambiguous 3-arg `EvaluateTaskUrgencyScarcityUseCase` overloads, expose Home slot status and `pendingUrgencyScarcityPrompt` in `HomeUiState`/`HomeViewModel` with safe target-date checks and submission-lock resets, and render tokenized `UrgencySlotStatusPill`, editor inline switch, and `UrgencyScarcityGuidanceDialog`.

## Boundaries & Constraints

**Always:**
- **AD-1 Domain Purity:** `UrgencyScarcityPolicy`, `UrgencyScarcityTier`, `TaskUrgencyScarcityResult`, and `EvaluateTaskUrgencyScarcityUseCase` in `domain/` must have zero `android.*` / `androidx.*` imports and zero UI copy constants beyond preserving existing `advisoryMessage`.
- **AD-4 Non-Blocking Autonomy & Zero-Guilt Copy:** Marking a 3rd+ active `URGENT` task never blocks saving, disables the `"Urgent"` chip, or silently downgrades intensity. Completing, deleting, or cancelling an `URGENT` task releases its slot immediately. Zero restricted `n-a-g` words anywhere.
- **Overload & Test Compatibility:** Preserve `EvaluateTaskUrgencyScarcityUseCase(taskRepository)`, `invoke(tasks, excludingTaskId = null)`, `invoke(date, excludingTaskId = null)`, `SOFT_CAP_THRESHOLD = 2`, `advisoryMessage`, and `HomeViewModel` constructor params. The new 3-arg `invoke(..., policy: UrgencyScarcityPolicy)` overloads MUST omit default parameter values to avoid Kotlin overload ambiguity.
- **AD-2 MVI & Concurrency Safety:** `pendingUrgencyScarcityPrompt: UrgencyScarcityPromptState?` lives in immutable `HomeUiState`, cleared atomically (`getAndUpdate { null }`) on resolution and reset on `OpenTaskEditor`, `DismissTaskEditor`, `DeleteTaskItem`, and `Refresh`. `TaskEditorSheet` resets `isSubmitting` when `isScarcityPromptOpen` changes to `false`.
- **AD-7 Token Conformance & Accessibility:** Use `PersonalTrackerTheme` tokens exclusively (0 raw hex/dp/sp literals), `stringResource(...)`, vertical `fillMaxWidth()` dialog buttons (`48dp` floor via `spacing.minTouchTarget`), TalkBack semantics, and `clearAndSetSemantics {}` on decorative emojis.
- **Strict No Code During Design:** Do not edit `app/src/` until Checkpoint 1 approval.

**Ask First:**
- Persisting user-edited `UrgencyScarcityPolicy` ceilings in Settings UI / `SharedPreferences` (kept as a pure Kotlin policy in Story 5.3 unless requested).

**Never:**
- Never hard-block saving an `URGENT` task or disable `"Keep Urgent"`.
- Never open `UrgencyScarcityGuidanceDialog` for `COMPLETED`/`CANCELLED` tasks, or when editing an existing active `URGENT` task on the same `scheduledDate` (`wasAlreadyActiveUrgent == true`).
- Never use `colors.danger` on `UrgencySlotStatusPill` at soft cap (use calm `colors.warning` / `colors.warningSubtle`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| **Policy Bounds Guard** | `UrgencyScarcityPolicy(0)` or `(6)` | Throws `IllegalArgumentException` (`1..5` valid; default `2`) | Constructor `require(...)` |
| **Tier Rule (`max=1..5`)** | `active >= max` / `active > 0 && active >= max - 1` / else | `AT_OR_OVER_CAP` (check first) -> `APPROACHING` (`max=5, active=4`; `max=2, active=1`) -> `AVAILABLE` (`max=1, active=0`; `max=5, active=1`) | `remainingSlots` coerced `>= 0` |
| **Blank Date Normalization** | `SaveTaskItem` with `scheduledDate = ""` | Normalized to `LocalDate.now().toString()` before scarcity check | Matches `SaveTaskUseCase` |
| **Save 3rd Active Urgent** | Target date has `>= maxActiveUrgent` other active `URGENT`; incoming active `URGENT` & `!wasAlreadyActiveUrgent` | `HomeViewModel` sets `pendingUrgencyScarcityPrompt`; `UrgencyScarcityGuidanceDialog` opens; `isSubmitting` resets on dismiss | Dismiss returns to editor with draft preserved |
| **Choose "Keep Urgent"** | `ResolveUrgencyScarcity(keepUrgent = true)` | Saves `URGENT` task, closes dialog & editor, emits `"Task saved as Urgent."` | Atomic `getAndUpdate`, snackbar on error |
| **Choose "Change to Important"** | `ResolveUrgencyScarcity(keepUrgent = false)` | Saves `IMPORTANT` task, closes dialog & editor, emits `"Saved as Important."` | Schedules `IMPORTANT` alarm |
| **Edit Existing Urgent Task** | Existing active `URGENT` on same `scheduledDate`, still `URGENT` | Saves immediately without modal dialog (`wasAlreadyActiveUrgent == true`) | Moving to full new date prompts |
| **Future-Date Scarcity Check** | Today `0` Urgent, tomorrow `2` Urgent; save Urgent for tomorrow | Evaluates tomorrow -> triggers `UrgencyScarcityGuidanceDialog` with date-appropriate wording | Today count unchanged |
| **Slot Release** | Complete, delete, or cancel active `URGENT` | `homeActiveUrgentCount` & `HomeScreen` pill reactively decrement and shift tier | Verified in Home MVI |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/UrgencyScarcityPolicy.kt` -- `[Create]` `data class UrgencyScarcityPolicy(val maxActiveUrgent: Int = DEFAULT_MAX_ACTIVE_URGENT)` with `DEFAULT_MAX_ACTIVE_URGENT = 2`, `MIN_MAX_ACTIVE_URGENT = 1`, `MAX_MAX_ACTIVE_URGENT = 5`, `init { require(maxActiveUrgent in MIN_MAX_ACTIVE_URGENT..MAX_MAX_ACTIVE_URGENT) }`.
- `app/src/main/java/com/personaltracker/domain/usecase/EvaluateTaskUrgencyScarcityUseCase.kt` -- `[Modify]` Add `enum class UrgencyScarcityTier { AVAILABLE, APPROACHING, AT_OR_OVER_CAP }`; extend `TaskUrgencyScarcityResult` with `val maxActiveUrgent: Int = SOFT_CAP_THRESHOLD`, `val remainingSlots: Int = (maxActiveUrgent - activeUrgentCount).coerceAtLeast(0)`, `val tier: UrgencyScarcityTier = if (isSoftCapReached) UrgencyScarcityTier.AT_OR_OVER_CAP else UrgencyScarcityTier.AVAILABLE`; keep existing 2-arg `invoke` delegates + add non-defaulted 3-arg `invoke(tasks, excludingTaskId, policy)` and `suspend invoke(date, excludingTaskId, policy)`. Retain `const val SOFT_CAP_THRESHOLD = UrgencyScarcityPolicy.DEFAULT_MAX_ACTIVE_URGENT`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiState.kt` -- `[Modify]` Add `data class UrgencyScarcityPromptState(val pendingTask: Task, val activeUrgentCount: Int, val maxActiveUrgent: Int, val isTargetDateToday: Boolean = true)`; add `maxActiveUrgent: Int = 2`, `remainingUrgentSlots: Int = 2`, `urgencyScarcityTier: UrgencyScarcityTier = UrgencyScarcityTier.AVAILABLE`, `homeActiveUrgentCount: Int = 0`, `homeUrgencyScarcityTier: UrgencyScarcityTier = UrgencyScarcityTier.AVAILABLE`, `pendingUrgencyScarcityPrompt: UrgencyScarcityPromptState? = null` to `HomeUiState`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeUiIntent.kt` -- `[Modify]` Add `data class ResolveUrgencyScarcity(val keepUrgent: Boolean) : HomeUiIntent` and `object DismissUrgencyScarcityPrompt : HomeUiIntent`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt` -- `[Modify]` Track `pendingUrgencyScarcityControl` in `HomeControlData` / `observeDataStreams()`; compute `homeUrgencyEvaluation` (`excludingTaskId = null`) and `editorUrgencyEvaluation` (`excludingTaskId = taskEditor.task?.id`); in `handleSaveTaskItem(task)`, guard double-saves (`isSavingTaskInFlight` + active prompt), normalize `targetDate`, verify `wasAlreadyActiveUrgent` via `taskRepository.getTaskById(task.id)`, and intercept active `URGENT` tasks at cap; implement `ResolveUrgencyScarcity` & `DismissUrgencyScarcityPrompt` and clear prompt in `OpenTaskEditor`, `DismissTaskEditor`, `DeleteTaskItem`, and `observeDataStreams()`.
- `app/src/main/java/com/personaltracker/ui/components/overlays/UrgencyScarcityGuidanceDialog.kt` -- `[Create]` Stateless composable `UrgencyScarcityGuidanceDialog(promptState, onKeepUrgent, onChangeToImportant, onDismissRequest, modifier)` matching `CarryForwardReviewDialog` (`Dialog` + `Surface(shape = shapes.lg, color = colors.surfaceRaised, border = BorderStroke(spacing.spacing0_5, colors.warning))`) with vertical `fillMaxWidth()` buttons (`spacing.minTouchTarget` height floor).
- `app/src/main/java/com/personaltracker/ui/components/overlays/TaskEditorSheet.kt` -- `[Modify]` Append `maxActiveUrgent: Int = 2`, `urgencyScarcityTier: UrgencyScarcityTier = UrgencyScarcityTier.AVAILABLE`, `isScarcityPromptOpen: Boolean = false`; reset `isSubmitting = false` via `LaunchedEffect(isScarcityPromptOpen)`; keep close button `spacing.minTouchTarget`; add `APPROACHING` inline hint (`colors.infoSubtle`/`colors.info`) when `URGENT` & `!showSoftCap`; tokenize soft-cap banner (`isSoftCapReached || urgencyScarcityTier == AT_OR_OVER_CAP`) using `stringResource(...)` + `clearAndSetSemantics {}` on `"⚡"` + inline 48dp `"Switch to Important"` `TextButton` setting `deliveryIntensity = "IMPORTANT"`.
- `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- `[Modify]` Place `UrgencySlotStatusPill` inside Today's Focus Tasks header `Column(Modifier.weight(1f))` (`colors.surfaceSunken`/`infoSubtle`/`warningSubtle`, TalkBack `contentDescription`); pass `maxActiveUrgent`, `urgencyScarcityTier`, `isScarcityPromptOpen` to `TaskEditorSheet`; host `UrgencyScarcityGuidanceDialog`.
- `app/src/main/res/values/strings.xml` -- `[Modify]` Add `urgency_slot_pill_available`, `urgency_slot_pill_approaching`, `urgency_slot_pill_at_cap`, `urgency_slot_pill_cd`, `urgency_scarcity_approaching_hint`, `urgency_scarcity_banner_title`, `urgency_scarcity_banner_subtitle`, `urgency_scarcity_switch_to_important`, `urgency_scarcity_dialog_title`, `urgency_scarcity_dialog_message_today_{one,many}`, `urgency_scarcity_dialog_message_date_{one,many}`, `urgency_scarcity_dialog_subtitle`, `urgency_scarcity_action_change_important`, `urgency_scarcity_action_keep_urgent`, `urgency_scarcity_dialog_cd`.
- `app/src/test/java/com/personaltracker/domain/TaskManagementUseCasesTest.kt` & `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- `[Modify]` Test `UrgencyScarcityPolicy` bounds, `UrgencyScarcityTier` rules (`max=1,2,5`), 3rd-Urgent save interception, `"Keep Urgent"`, `"Change to Important"`, dismiss unsaved, existing-Urgent-edit bypass, and future-date (`scheduledDate != today`) scarcity evaluation.

## Tasks & Acceptance

**Execution:**
- [ ] `domain/model/UrgencyScarcityPolicy.kt` & `domain/usecase/EvaluateTaskUrgencyScarcityUseCase.kt` -- Implement configurable policy, `UrgencyScarcityTier`, and non-defaulted 3-arg overloads -- Enforce AD-1 and zero overload ambiguity.
- [ ] `ui/screens/home/HomeUiState.kt`, `HomeUiIntent.kt`, `HomeViewModel.kt` -- Wire Home/editor scarcity metrics, target-date normalization, `wasAlreadyActiveUrgent` lookup, concurrency guards, and prompt resolution -- Prevent double saves and stale prompt locks.
- [ ] `res/values/strings.xml`, `UrgencyScarcityGuidanceDialog.kt`, `TaskEditorSheet.kt`, `HomeScreen.kt` -- Implement tokenized slot pill, `isScarcityPromptOpen` `isSubmitting` reset, inline switch button, and vertical 48dp guidance dialog -- Enforce AD-4 and AD-7.
- [ ] `test/.../TaskManagementUseCasesTest.kt` & `HomeViewModelTest.kt` -- Cover all I/O matrix cases including single-slot policy and future-date saves -- Verify 100% pass rate on JVM.

**Acceptance Criteria:**
- Given 2 active `URGENT` items today, when the user saves a 3rd active item marked `URGENT`, `HomeUiState.pendingUrgencyScarcityPrompt` triggers `UrgencyScarcityGuidanceDialog` (`"You already have 2 Urgent items today. Keep Urgent or Change to Important?"`), and dismissing resets `isSubmitting` cleanly.
- Given the guidance dialog, `"Keep Urgent"` saves as `URGENT` while `"Change to Important"` saves as `IMPORTANT`, and completing, deleting, or cancelling an active `URGENT` item reactively updates Home's slot pill.
- Given zero `android.*` imports in `domain/`, zero hardcoded hex/dp/sp literals, and zero restricted `n-a-g` words, all 5 static/Gradle gates pass.

## Spec Change Log

- **Iteration 1 (2026-11-25):** Resolved Goldfish Swarm findings (`FAIL` / `NO-GO`): (1) added `isScarcityPromptOpen` prop + `LaunchedEffect` reset in `TaskEditorSheet` to eliminate `isSubmitting` deadlock on dialog dismiss; (2) required non-defaulted 3-arg `EvaluateTaskUrgencyScarcityUseCase.invoke` overloads to prevent Kotlin overload ambiguity; (3) defined strict `APPROACHING` rule (`active > 0 && active >= max - 1` checked after cap), removed `choicePromptMessage` from `domain/`, and split `homeActiveUrgentCount` from `activeUrgentTaskCount` so the Home pill stays stable while editing; (4) specified `taskRepository.getTaskById` `wasAlreadyActiveUrgent` check (including `scheduledDate` match), blank-date normalization, `HomeControlData` flow wiring, atomic `getAndUpdate`, and `isSavingTaskInFlight` guard; (5) defined all `strings.xml` IDs (with singular/future-date copy), vertical 48dp dialog buttons, `UrgencySlotStatusPill` tokens (`colors.warning` at cap), and future-date `HomeViewModelTest` coverage. KEEP: AD-1 domain purity, non-blocking AD-4 autonomy, backward-compatible `advisoryMessage`.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All 8 H0 components valid`
- `.\gradlew test lintDebug` -- expected: all JVM unit tests pass, 0 lint errors
- `adb devices -l` -- expected: Gate 6 Artemis verification if `10BD9D24YF000BE` is connected
