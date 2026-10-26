---
title: 'Story 2.4: Strict AI Action Boundary & Command Validation Pipeline'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: 'aac6c509761d827a7ef95dfd74febfaf3efec3ff'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Natural language parser output can produce semantically invalid commands (e.g., past timestamps, non-existent habit or routine targets, or duplicate active tasks for the same day). Allowing High Confidence commands to directly mutate persistence without validation violates the strict AI action boundary (ARCH-3, FR-7.6) and risks database corruption or duplicate records.

**Approach:** Implement a pure Kotlin `CommandValidationPipeline` residing in `domain/validation` with zero Android framework dependencies (AD-1). Integrate three discrete validators: Temporal Safety (past time rejection), Relational Integrity (habit/routine existence verification), and Task Deduplication (duplicate active task prevention). When a High Confidence command fails validation, gracefully downgrade its confidence tier to Medium, populating the UI chips in `QuickCaptureSheet` while requiring manual user correction without shame or guilt copy (AD-4). Gate ViewModel proposal confirmation behind the pipeline so that zero unvalidated commands ever mutate Room persistence.

## Boundaries & Constraints

**Always:**
- Keep all validation logic 100% pure Kotlin in `com.personaltracker.domain.validation` with zero `android.*` or `androidx.*` dependencies (AD-1).
- Enforce temporal safety: reject scheduled dates in the past, or scheduled times on today that have already elapsed relative to reference time.
- Enforce relational integrity: verify target habits exist in `HabitRepository` and target routines exist in `RoutineRepository` prior to execution.
- Enforce deduplication: reject creating active tasks whose titles duplicate existing non-completed tasks on the same scheduled date.
- Enforce confidence downgrade handshake: if a High Confidence ($\ge 0.85$) parsed command fails validation, downgrade it to Medium Confidence and prompt user correction via editable chips (AD-4, AD-8).
- Keep all validation microcopy non-punishing, supportive, and free of shame or guilt language (AD-4).
- Gate confirmation in `QuickCaptureViewModel` through the validation pipeline before writing to `TaskRepository`.

**Ask First:**
- Relaxing deduplication criteria to allow exact duplicate task names at different times on the same date.
- Altering existing confidence scoring formulas in `DeterministicNlpCommandParser`.

**Never:**
- Allow AI, voice recognition, or parser models to directly mutate Room database entities without passing through validation (AD-1, ARCH-3, FR-7.6).
- Import Android context, framework classes, or UI components into `domain/` (AD-1).
- Silently drop invalid commands; always preserve the parsed draft for user correction.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid High-Confidence Task | High-confidence parsed task scheduled tomorrow at 10:00 | `ValidationResult.Valid`; committed to `TaskRepository`; 5s undo toast displayed | N/A |
| Past Scheduled Time on Today | Spoken command at 14:00: "Meeting at 10:00 today" (High confidence parsed) | Validation fails `TEMPORAL_PAST_TIME`; downgraded to Medium Confidence; chip shows invalid time; warning banner indicates time is in past | User prompted to tap time chip to pick future time |
| Past Scheduled Date | Spoken command: "Buy groceries yesterday" | Validation fails `TEMPORAL_PAST_TIME`; downgraded to Medium Confidence; date chip highlighted; non-punishing notice | User adjusts date to today or future |
| Unknown Target Habit | Spoken command: "Log habit Skydiving" (habit does not exist) | Validation fails `RELATIONAL_HABIT_NOT_FOUND`; downgraded to Medium Confidence; prompt suggests creating habit | Prevents orphaned logs |
| Unknown Target Routine | Spoken command: "Shift Yoga routine by 15 mins" (routine not found) | Validation fails `RELATIONAL_ROUTINE_NOT_FOUND`; downgraded to Medium Confidence; routine notice displayed | Prevents schedule corruption |
| Duplicate Active Task Same Day | Spoken command: "Buy groceries" when active "Buy groceries" already exists for today | Validation fails `DUPLICATE_ACTIVE_TASK`; downgraded to Medium Confidence; notice informs task already exists | Allows user to rename or pick another day |
| Duplicate Completed Task Same Day | Spoken command: "Buy groceries" when previous "Buy groceries" is already completed | `ValidationResult.Valid`; allowed (completed historical tasks do not block new tasks) | N/A |
| Manual Proposal Confirmation Gate | User taps Confirm on Medium proposal with elapsed time | Proposal re-validated; fails; persistence blocked; snackbar displayed: "Please select a future time" | Sheet remains open for correction |
| Low-Confidence Parse | Low-confidence transcript (< 0.50) | Routed to `UnfiledCaptureInboxEntity` without triggering task validation pipeline | Stored safely for triage |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/validation/ValidationResult.kt` -- Sealed hierarchy representing validation status (`Valid`, `Invalid(reason, message, invalidField)`), failure reasons, and invalid field indicators.
- `app/src/main/java/com/personaltracker/domain/validation/CommandValidator.kt` -- Pure Kotlin interface contract for command validators.
- `app/src/main/java/com/personaltracker/domain/validation/TemporalSafetyValidator.kt` -- Pure Kotlin validator verifying scheduled date and time are in the future relative to reference time.
- `app/src/main/java/com/personaltracker/domain/validation/RelationalIntegrityValidator.kt` -- Pure Kotlin validator querying `HabitRepository` and `RoutineRepository` to confirm target existence.
- `app/src/main/java/com/personaltracker/domain/validation/TaskDeduplicationValidator.kt` -- Pure Kotlin validator querying `TaskRepository` to prevent duplicate active tasks on the same date.
- `app/src/main/java/com/personaltracker/domain/validation/CommandValidationPipeline.kt` -- Pure Kotlin pipeline orchestrating validators and providing composite validation outcomes.
- `app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt` -- Integrates `CommandValidationPipeline` into confidence routing; downgrades High Confidence captures failing validation to Medium Confidence with non-punishing feedback (AD-4).
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- Integrates validation pipeline into `handleConfirmProposal`, preventing unvalidated persistence and updating UI state on validation errors.
- `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Provides Hilt singleton bindings for validation pipeline and validators.
- `app/src/test/java/com/personaltracker/domain/validation/TemporalSafetyValidatorTest.kt` -- Unit tests asserting past dates, past times, future times, and flexible dates.
- `app/src/test/java/com/personaltracker/domain/validation/RelationalIntegrityValidatorTest.kt` -- Unit tests asserting habit and routine existence verification.
- `app/src/test/java/com/personaltracker/domain/validation/TaskDeduplicationValidatorTest.kt` -- Unit tests asserting active vs completed task duplication behavior.
- `app/src/test/java/com/personaltracker/domain/validation/CommandValidationPipelineTest.kt` -- Unit tests verifying composite pipeline execution and priority.
- `app/src/test/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCaseValidationTest.kt` -- Unit tests verifying confidence downgrade from High to Medium upon validation failure and zero Room mutation.
- `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- Unit tests verifying ViewModel confirmation gating and validation feedback.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/validation/ValidationResult.kt` -- Create sealed interface `ValidationResult` (`Valid`, `Invalid`), `ValidationFailureReason`, and `InvalidField` enums.
- [x] `app/src/main/java/com/personaltracker/domain/validation/CommandValidator.kt` -- Create `CommandValidator` interface with `suspend fun validate(command: ParsedCommand, referenceDateTime: LocalDateTime): ValidationResult`.
- [x] `app/src/main/java/com/personaltracker/domain/validation/TemporalSafetyValidator.kt` -- Implement temporal validator verifying scheduled date/time is in the future.
- [x] `app/src/main/java/com/personaltracker/domain/validation/RelationalIntegrityValidator.kt` -- Implement relational validator verifying habits and routines exist in respective repositories.
- [x] `app/src/main/java/com/personaltracker/domain/validation/TaskDeduplicationValidator.kt` -- Implement deduplication validator checking active tasks for the target date in `TaskRepository`.
- [x] `app/src/main/java/com/personaltracker/domain/validation/CommandValidationPipeline.kt` -- Implement pipeline orchestrating validators and returning first failure or `Valid`.
- [x] `app/src/main/java/com/personaltracker/di/DomainEngineModule.kt` -- Bind `CommandValidationPipeline` and validators in Hilt.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt` -- Inject `CommandValidationPipeline`; validate commands prior to auto-commit; execute confidence downgrade handshake for High Confidence captures failing validation.
- [x] `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- Gate `handleConfirmProposal` behind `CommandValidationPipeline`; surface non-punishing feedback on validation failure.
- [x] `app/src/test/java/com/personaltracker/domain/validation/TemporalSafetyValidatorTest.kt` -- Test temporal boundary conditions, leap days, and past vs future times.
- [x] `app/src/test/java/com/personaltracker/domain/validation/RelationalIntegrityValidatorTest.kt` -- Test missing vs existing habit and routine names with case-insensitive matching.
- [x] `app/src/test/java/com/personaltracker/domain/validation/TaskDeduplicationValidatorTest.kt` -- Test active duplicate rejection and completed task allowance.
- [x] `app/src/test/java/com/personaltracker/domain/validation/CommandValidationPipelineTest.kt` -- Test composite pipeline execution order and early return on failure.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCaseValidationTest.kt` -- Test end-to-end downgrade handshake from High to Medium confidence with zero database writes.
- [x] `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- Test ViewModel proposal confirmation rejection on validation failure and error state surfacing.

**Acceptance Criteria:**
- Given a High Confidence parsed task with a time that has already passed today, when processed by `CaptureQuickThoughtUseCase`, then the command is downgraded to Medium Confidence, zero tasks are written to `TaskRepository`, and a non-punishing warning message is returned.
- Given a command targeting a habit or routine that does not exist, when evaluated by `RelationalIntegrityValidator`, then validation returns `Invalid` with `RELATIONAL_HABIT_NOT_FOUND` or `RELATIONAL_ROUTINE_NOT_FOUND`.
- Given an active task with title "Gym" already scheduled for today, when a new command attempts to create task "Gym" on the same date, then validation returns `Invalid` with `DUPLICATE_ACTIVE_TASK`.
- Given a previously completed task "Gym" on today, when a new command creates task "Gym" for today, then validation succeeds.
- Given a user attempting to confirm an invalid proposal in `QuickCaptureSheet`, when confirmation is triggered, then `QuickCaptureViewModel` blocks persistence and displays a supportive message prompting field correction.
- Given all components in `com.personaltracker.domain.validation`, when inspected for dependencies, then zero `android.*` or `androidx.*` packages are imported (AD-1).

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Design Notes

- **AI Action Boundary Standard (ARCH-3, FR-7.6):**
  - Parsing is inherently probabilistic. High Confidence ($\ge 0.85$) represents semantic confidence from regex/heuristics, not domain validity.
  - The validation layer acts as a deterministic firewall between parser output and domain use cases.
- **Confidence Downgrade Handshake (AD-4, AD-8):**
  - When validation fails on a High Confidence capture, instead of rejecting the input or discarding user intent, the pipeline downgrades the command to `MediumConfidence`.
  - The UI presents the extracted data in editable chips (`ProposalChipData`), allowing the user to 1-tap edit the invalid field (e.g., tap Time chip to reschedule to tomorrow morning).
  - Microcopy follows AD-4 zero guilt guidelines: "Scheduled time is in the past — pick a future time" rather than "Error: Invalid time".

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED` (0 `android.*` in domain)
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `0 findings`
- `.\gradlew test` -- expected: All JVM unit tests pass cleanly
- `.\gradlew lintDebug` -- expected: 0 errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid

## Suggested Review Order

**Domain Validation Pipeline & Models (AD-1 Purity)**

- Sealed validation status hierarchy and failure reason models
  [`ValidationResult.kt:8`](../../app/src/main/java/com/personaltracker/domain/validation/ValidationResult.kt#L8)

- Command validator interface contract
  [`CommandValidator.kt:11`](../../app/src/main/java/com/personaltracker/domain/validation/CommandValidator.kt#L11)

- Temporal safety validator rejecting elapsed dates and times
  [`TemporalSafetyValidator.kt:15`](../../app/src/main/java/com/personaltracker/domain/validation/TemporalSafetyValidator.kt#L15)

- Relational integrity validator confirming habits and routines exist
  [`RelationalIntegrityValidator.kt:16`](../../app/src/main/java/com/personaltracker/domain/validation/RelationalIntegrityValidator.kt#L16)

- Deduplication validator preventing duplicate active tasks on same day
  [`TaskDeduplicationValidator.kt:15`](../../app/src/main/java/com/personaltracker/domain/validation/TaskDeduplicationValidator.kt#L15)

- Composite pipeline orchestrating validation with early short-circuit
  [`CommandValidationPipeline.kt:14`](../../app/src/main/java/com/personaltracker/domain/validation/CommandValidationPipeline.kt#L14)

**Domain Use Case Integration & Confidence Downgrade Handshake**

- High-to-Medium confidence downgrade handshake preventing direct Room mutations
  [`CaptureQuickThoughtUseCase.kt:60`](../../app/src/main/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCase.kt#L60)

**UI Presentation & Confirmation Gate**

- Proposal confirmation validation gate and warning message surfacing
  [`QuickCaptureViewModel.kt:414`](../../app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt#L414)

- Dynamic invalid chip styling with non-punishing theme tokens
  [`QuickCaptureSheet.kt:465`](../../app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt#L465)

**Comprehensive Test Suite**

- Boundary condition and edge-case testing for temporal validation
  [`TemporalSafetyValidatorTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/validation/TemporalSafetyValidatorTest.kt#L1)

- Habit and routine relational existence tests
  [`RelationalIntegrityValidatorTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/validation/RelationalIntegrityValidatorTest.kt#L1)

- Active vs completed task deduplication tests
  [`TaskDeduplicationValidatorTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/validation/TaskDeduplicationValidatorTest.kt#L1)

- End-to-end confidence downgrade and zero Room mutation tests
  [`CaptureQuickThoughtUseCaseValidationTest.kt:1`](../../app/src/test/java/com/personaltracker/domain/usecase/CaptureQuickThoughtUseCaseValidationTest.kt#L1)

- ViewModel confirmation gating and snackbar error surfacing tests
  [`QuickCaptureViewModelTest.kt:498`](../../app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt#L498)
