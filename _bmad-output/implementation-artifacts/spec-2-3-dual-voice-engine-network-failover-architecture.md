---
title: 'Story 2.3: Dual Voice Engine & Network Failover Architecture'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: 'a79bd295632ed977e5acedb75de1921b21b5a9f0'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Voice capture requires reliable, offline-first speech transcription that strictly protects user privacy (AD-8) by preferring on-device processing, while gracefully handling network drops, recognition timeouts, audio focus loss, and permission revocations without leaving recording hardware active or freezing the UI.

**Approach:** Implement a dual-mode speech recognition architecture via `SpeechRecognitionManager` wrapping Android `SpeechRecognizer` (preferring on-device offline recognition via `createOnDeviceSpeechRecognizer` on API 31+ and `EXTRA_PREFER_OFFLINE`), wire lifecycle and error failovers (offline drop, timeouts, audio focus loss) into `QuickCaptureViewModel`, route successful transcripts directly through `CaptureQuickThoughtUseCase` and `DeterministicNlpCommandParser`, and display live feedback in `QuickCaptureSheet`.

## Boundaries & Constraints

**Always:**
- Keep all domain logic 100% pure Kotlin; encapsulate Android `SpeechRecognizer` within `com.personaltracker.platform.voice` (AD-1).
- Prioritize on-device/offline speech recognition (`createOnDeviceSpeechRecognizer` on API 31+ or `EXTRA_PREFER_OFFLINE = true`) for privacy compliance (AD-8).
- Unconditionally stop audio recording, purge in-memory PCM buffers, and release speech recognition resources on error, completion, or sheet dismissal (AD-5, AD-8, NFR-4).
- Route final transcripts through `CaptureQuickThoughtUseCase` enforcing 3-tier confidence routing: High ($\ge 0.85$), Medium ($0.50..0.84$), Low ($< 0.50$) (AD-8).
- Maintain 48dp minimum touch target floor and resolve all UI styling exclusively from `com.personaltracker.ui.theme` tokens with 0 raw literals (AD-7).
- Dispatch Android `SpeechRecognizer` lifecycle operations on `Dispatchers.Main` as required by the OS, with coroutines managed safely via ViewModel scope.

**Ask First:**
- Adding external cloud Speech-to-Text or third-party proprietary STT SDKs.
- Altering existing confidence tier thresholds in `DeterministicNlpCommandParser`.

**Never:**
- Write raw audio streams or voice recordings to disk storage or persistent caches (AD-5, AD-8).
- Directly mutate Room database DAOs from the speech recognition listener or platform layer without domain usecase validation (AD-1, ARCH-3).
- Leave `SpeechRecognizer` or `AudioRecord` hardware instances unreleased when the quick-capture sheet closes.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| On-Device Voice Capture (Happy Path) | User taps mic; speaks `"Buy groceries tomorrow"`; offline recognition succeeds | `SpeechRecognitionState.Result("Buy groceries tomorrow", isFinal = true)` emitted; text routed to `CaptureQuickThoughtUseCase`; Medium Confidence proposal displayed | Stop recorder; purge buffers in memory |
| High Confidence Auto-Commit via Voice | User speaks `"Study algorithms at 4pm tomorrow urgent"` | Speech transcribes text; `DeterministicNlpCommandParser` scores $\ge 0.85$; task auto-saved to Room; sheet closes; 5s undo toast shown | Undo deletes task if tapped within 5s |
| Network Loss During Cloud Speech | Network disconnects during online speech attempt | Catches network error code; falls back automatically to on-device recognizer or prompts manual text entry with message | No crash; resets recording state safely |
| Speech Recognition Timeout | User starts recording; silence or no match detected | Emits `SpeechRecognitionState.Error(TIMEOUT)`; resets `isRecording = false`; stops recorder | Shows notice `"No speech detected. Tap mic to retry."` |
| Audio Focus Loss / Interruption | Incoming phone call or another app requests audio focus | Speech recognition cancels; audio recorder stops; in-memory buffer zeroed | Sheet remains open with partial text draft |
| Microphone Permission Denied | User taps mic when `RECORD_AUDIO` not granted | Suppresses speech initiation; shows permission snackbar; enables text input | Fallback to keyboard input |
| Cancel / Dismiss While Listening | User dismisses bottom sheet while speech engine is active | `stopRecordingInternal()` cancels speech recognition; releases recognizer; zeroes audio buffer | Clean coroutine and hardware cleanup |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionManager.kt` -- Native Android `SpeechRecognizer` wrapper preferring on-device recognition, streaming `SpeechRecognitionState` events, and handling errors/failover.
- `app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionState.kt` -- Sealed state model representing speech recognition lifecycle (`Idle`, `Listening`, `Processing`, `Result`, `Error`).
- `app/src/main/java/com/personaltracker/di/PlatformModule.kt` -- Provides `SpeechRecognitionManager` singleton via Hilt.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- Coordinates voice transcription, failovers, text routing through `CaptureQuickThoughtUseCase`, and MVI state transitions.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureUiState.kt` -- Extended with speech status (`transcriptionStatus`, `partialTranscript`, `isListening`).
- `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Displays live voice transcription status ("Listening...", "Processing...", partial text) alongside waveform.
- `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- JVM unit tests verifying speech recognition state transitions, network failover, timeout handling, and NLP auto-routing.
- `app/src/test/java/com/personaltracker/platform/voice/SpeechRecognitionStateTest.kt` -- Unit tests validating error mapping, state transitions, and failover models.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionState.kt` -- Create sealed interface `SpeechRecognitionState` (`Idle`, `Listening(rms: Float)`, `Processing`, `Result(text: String, isFinal: Boolean)`, `Error(error: SpeechRecognitionError, message: String)`) and error enum.
- [x] `app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionManager.kt` -- Implement wrapper managing `SpeechRecognizer` lifecycle, favoring `createOnDeviceSpeechRecognizer` on API 31+ and `EXTRA_PREFER_OFFLINE`, with failover error mapping and safe destruction.
- [x] `app/src/main/java/com/personaltracker/di/PlatformModule.kt` -- Bind `SpeechRecognitionManager` in Hilt DI.
- [x] `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureUiState.kt` -- Extend state with `transcriptionStatus: String? = null` and `partialTranscript: String? = null`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt` -- Inject `SpeechRecognitionManager`; collect speech state flow during recording; route final text directly to `CaptureQuickThoughtUseCase`; handle timeouts and network failover gracefully.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt` -- Render live transcription status label and partial text draft using design tokens.
- [x] `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- Add test cases covering: speech transcription to High-confidence commit, Medium-confidence proposal, Low-confidence unfiled inbox, timeout recovery, and network error failover.

**Acceptance Criteria:**
- Given active microphone permission, when voice capture starts, then `SpeechRecognitionManager` initializes speech recognition preferring on-device processing and streams `Listening` with RMS amplitude.
- Given a spoken natural language command, when speech recognition completes with final text, then the text is automatically parsed via `DeterministicNlpCommandParser` and routed according to its 3-tier confidence score.
- Given an offline state or network failure during speech capture, then the system falls back to on-device recognition or enables manual text input with an informative notice without crashing.
- Given a recognition timeout or silence, then recording halts, in-memory buffers are purged, and a retry message is displayed.
- Given sheet dismissal or activity pause during active speech, then `SpeechRecognizer` and `AudioRecord` are immediately cancelled and destroyed.

## Spec Change Log

<!-- Append-only. Populated by step-04 during review loops. -->

## Design Notes

- **Offline-First Privacy Invariant (AD-8):**
  - Android 12+ (API 31+) provides `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)`. When available, the engine uses on-device recognition to guarantee 0 audio bytes leave the device.
  - On API <31 or when on-device recognition is unavailable, `SpeechRecognizer.createSpeechRecognizer(context)` is initialized with `EXTRA_PREFER_OFFLINE = true`.
- **Failover Strategy:**
  - Network error (`ERROR_NETWORK`, `ERROR_NETWORK_TIMEOUT`) -> Notify user and seamlessly switch to manual text input or on-device retry.
  - Timeout / No match (`ERROR_SPEECH_TIMEOUT`, `ERROR_NO_MATCH`) -> Safely reset state to allow immediate re-recording or typing.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `0 findings`
- `.\gradlew test` -- expected: Complete JVM test suite passes cleanly
- `.\gradlew lintDebug` -- expected: 0 errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid


## Suggested Review Order

**Platform Speech Engine & Lifecycle Management**

- Native Android `SpeechRecognizer` wrapper favoring on-device offline recognition
  [`SpeechRecognitionManager.kt:50`](../../app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionManager.kt#L50)

- Sealed state hierarchy and error mapping for recognition lifecycle
  [`SpeechRecognitionState.kt:14`](../../app/src/main/java/com/personaltracker/platform/voice/SpeechRecognitionState.kt#L14)

**MVI Coordination & NLP Auto-Routing**

- Speech stream collection, graceful stop handshake, and confidence routing
  [`QuickCaptureViewModel.kt:290`](../../app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt#L290)

- Extended UI state with transcription status and partial text
  [`QuickCaptureUiState.kt:14`](../../app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureUiState.kt#L14)

**UI Presentation & Feedback**

- Live transcription status labels and bounded partial transcript rendering
  [`QuickCaptureSheet.kt:110`](../../app/src/main/java/com/personaltracker/ui/components/overlays/QuickCaptureSheet.kt#L110)

**Test Verification & Edge Cases**

- Unit tests for speech state transitions, timeout, network failover, and routing
  [`QuickCaptureViewModelTest.kt:310`](../../app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt#L310)

- Unit tests for error mapping, NaN guards, and RMS calculation
  [`SpeechRecognitionStateTest.kt:15`](../../app/src/test/java/com/personaltracker/platform/voice/SpeechRecognitionStateTest.kt#L15)
