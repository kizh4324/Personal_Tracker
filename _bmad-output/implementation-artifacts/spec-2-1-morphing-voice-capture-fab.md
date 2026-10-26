---
title: 'Story 2.1: Morphing Voice Capture FAB & Audio Input Lifecycle'
type: 'feature'
created: '2026-10-26'
status: 'done'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - 'docs/design/framer_motion_spec.json'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users need to capture fleeting thoughts, tasks, and schedule intents in seconds without friction. A static FAB with abrupt modal jumps creates visual disorientation, while voice capture requires immediate, reliable visual feedback (RMS waveform) and strict guarantees that uncommitted microphone audio is never written to disk or retained in memory.

**Approach:** Implement a morphing shared-element container transition driven by the Framer spring physics spec (`fabToModalSpringSpec`, stiffness 320, damping 26) transitioning the `#59280E` Deep Espresso FAB into the `QuickCaptureSheet` modal without viewport clipping. Wire `QuickCaptureVoiceRecorder` to `WaveformVisualizer` rendering live RMS amplitude in Vibrant Orange (`#F26B1D`, `colors.primary`) while enforcing strict RAM-only buffer zeroing (AD-8). Present the complete 3-tier NLP confidence routing UI (High 5s undo toast, Medium 1-tap confirmation chips, Low unfiled inbox notice) using semantic `ui.theme` tokens and 48dp touch targets.

## Boundaries & Constraints

**Always:**
- Persist audio buffers exclusively in RAM and unconditionally zero them out in `finally` blocks via `QuickCaptureVoiceRecorder.purgeBuffer()` (AD-8, NFR-4).
- Use `AnimationTokens.fabToModalSpringSpec()` (`spring(dampingRatio = 0.75f, stiffness = 320f)`) for the FAB-to-modal transition (matching Framer project `QaEW7GnAiltJMNVpWjTs-dKIrE`).
- Use `colors.primaryDark` (`#59280E` Deep Espresso) for the FAB resting container and `colors.primary` (`#F26B1D` Vibrant Orange) for the active waveform visualizer bars.
- Enforce a 48dp minimum touch target floor for all interactive elements in `QuickCaptureSheet`, `ConfirmationChip`, and FAB (AD-7).
- Resolve all colors, typography, shapes, and spacing exclusively from `com.personaltracker.ui.theme` tokens; zero raw literals (AD-7).
- Execute all Room database operations on `Dispatchers.IO` and regex/audio calculations on `Dispatchers.Default` (AD-1).

**Ask First:**
- Modifying Room database entity schemas or adding new persistent tables.
- Altering the confidence score thresholds for High ($\ge 0.85$) or Medium ($0.50..0.85$) routing tiers.

**Never:**
- Write raw PCM audio or audio recordings to persistent disk storage (AD-8, NFR-4).
- Allow AI or speech recognition packages to directly mutate Room DAOs without validation (AD-1, ARCH-3).
- Import `android.*` or `androidx.*` framework dependencies into domain packages (AD-1).
- Use hardcoded hex colors, `dp`, or `sp` literals in UI composables (AD-7).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| FAB Tap (Permission Granted) | User taps resting 56dp FAB; `RECORD_AUDIO` granted | FAB expands via `fabToModalSpringSpec` into `QuickCaptureSheet`, initiates recording, and streams RMS amplitude to `WaveformVisualizer` | If AudioRecord fails to initialize, gracefully fall back to text quick-add |
| FAB Tap (Permission Denied) | User taps resting 56dp FAB; `RECORD_AUDIO` denied | FAB expands into `QuickCaptureSheet` in text input mode, shows permission snackbar / notice | Graceful fallback to manual keyboard entry |
| Speech Ceiling / Silence | Silence or 0 amplitude detected | `WaveformVisualizer` maintains minimum idle bar height (`spacing.spacing2`), RMS emits 0.0f | Amplitude clamped cleanly with `clampAmplitude()` |
| High Confidence Auto-Commit | Parsed command confidence $\ge 0.85$ | Task inserted into `TaskRepository`, sheet closes, 5s reversible `HighConfidenceUndoToast` appears | Tapping "Undo" within 5s deletes task without penalty |
| Medium Confidence Capture | Parsed command confidence $0.50..0.85$ | Sheet remains open displaying `MediumConfidenceProposalCard` with 5 editable chips; zero DB writes | User taps "Confirm" to save or "Discard" to dismiss |
| Low Confidence Capture | Parsed command confidence $< 0.50$ | Item saved to `UnfiledInboxRepository`, sheet closes with "Saved to unfiled inbox" snackbar | Transcript preserved in inbox; zero task entity writes |
| Cancel / Dismiss Sheet | User dismisses sheet or taps outside | Active recording immediately stopped, PCM buffer zeroed in memory, state reset | Unconditionally executed via `finally` block |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- Houses the FAB (`colors.primaryDark`), FAB-to-sheet spring transition, and coordinates overlay visibility.
- `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Warm Organic Tactile bottom sheet with animated container entrance, dual input, `WaveformVisualizer`, `MediumConfidenceProposalCard`, and `HighConfidenceUndoToast`.
- `app/src/main/java/com/personaltracker/ui/components/atoms/WaveformVisualizer.kt` -- Animated RMS frequency bars styled in `colors.primary` (`#F26B1D`).
- `app/src/main/java/com/personaltracker/platform/voice/QuickCaptureVoiceRecorder.kt` -- RAM-only AudioRecord streaming with `calculateNormalizedRms` and unconditional `purgeBuffer` in `finally`.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- MVI coordinator managing recording lifecycle, amplitude streaming, text submission, and 3-tier routing.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureUiIntent.kt` -- MVI intent contract including voice toggle, sheet open/dismiss, submit, confirm, undo.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureUiState.kt` -- Immutable UI state exposing sheet visibility, recording state, amplitude, proposals, and undo banner.
- `app/src/main/java/com/personaltracker/ui/theme/AnimationTokens.kt` -- Defines `fabToModalSpringSpec` (`stiffness = 320f, dampingRatio = 0.75f`).
- `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- JVM unit test suite verifying recording lifecycle, amplitude updates, 3-tier confidence routing, and undo.
- `app/src/test/java/com/personaltracker/platform/QuickCaptureVoiceRecorderTest.kt` -- JVM unit tests verifying RMS calculation math and RAM buffer zeroing.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- Restyle FAB to `colors.primaryDark` (`#59280E`) with `colors.surfaceRaised` icon; implement smooth spring morphing entrance using `fabToModalSpringSpec()` and IME keyboard padding avoidance (`Modifier.imePadding()`).
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Verify `QuickCaptureSheet` applies `fabToModalSpringSpec()` to enter/exit animations, displays `WaveformVisualizer` in `colors.primary`, hosts `MediumConfidenceProposalCard` with 48dp touch target chips, and presents `HighConfidenceUndoToast` with 5s auto-dismiss.
- [x] `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- Ensure `OpenSheet` can auto-initiate recording when microphone permission is already granted, properly cleans up audio resources on sheet dismissal, and maintains robust state flows.
- [x] `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- Add test cases verifying FAB tap auto-recording flow, IME / state dismiss cleanups, and seamless confidence transitions.
- [x] `app/src/test/java/com/personaltracker/ui/components/QuickCaptureSheetVisualTest.kt` -- Add Compose JVM unit test validating token resolution (no raw literals, 48dp touch targets, semantic colors).

**Acceptance Criteria:**
- Given the Voice Capture FAB resting above bottom navigation, when the user taps the FAB, then the FAB animates smoothly into the Quick Capture container using `fabToModalSpringSpec()` without viewport boundary clipping.
- Given active microphone permission, when voice capture begins, then `WaveformVisualizer` renders animated bars scaling with real-time RMS amplitude using `colors.primary` (`#F26B1D`).
- Given audio recording in progress, when recording completes or is cancelled, then the in-memory PCM audio buffer is unconditionally zeroed with zero bytes written to disk.
- Given a high-confidence capture ($\ge 0.85$), when committed, then the task is saved to `TaskRepository` and a 5-second `HighConfidenceUndoToast` appears; tapping Undo deletes the task.
- Given a medium-confidence capture ($0.50..0.85$), when parsed, then `MediumConfidenceProposalCard` displays 5 editable chips with 48dp touch targets and persists nothing until explicit user confirmation.
- Given all UI composables, all colors, typography, shapes, and dimensions resolve exclusively from `PersonalTrackerTheme` tokens with 0 raw hex literals.

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Design Notes

- **Spring Physics:** `AnimationTokens.fabToModalSpringSpec()` uses `dampingRatio = 0.75f` and `stiffness = 320f`, replicating the Framer prototype physics (`QaEW7GnAiltJMNVpWjTs-dKIrE`).
- **Tactile Color Palette:** FAB container rests in Deep Espresso (`#59280E`, `colors.primaryDark`), while active audio feedback uses Vibrant Orange (`#F26B1D`, `colors.primary`) to maintain the Warm Organic Tactile identity.
- **IME Avoidance:** `AppShellScaffold` and `QuickCaptureSheet` utilize `navigationBarsPadding()` and `imePadding()` to ensure the input field and waveform stay clearly visible above the software keyboard.

## Verification

**Commands:**
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All harness checks pass.
- `python .agents/scripts/domain_purity_check.py` -- expected: 0 architectural purity violations.
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: Architectural spine intact.
- `.\gradlew test` -- expected: All JVM unit tests pass (262+ tests).
- `.\gradlew lintDebug` -- expected: 0 lint errors.

**Manual checks (if no CLI):**
- Verify FAB visual tokens in Light and Dark mode using Compose Previews.
- Confirm 48dp minimum touch target bounding boxes on all confirmation chips.
