---
title: 'Story 3.2: Event-Driven Accessibility Distraction Interception & Privacy Invariant'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: '7c59f6d32cc1a67056cf8ce2d5b166e81fcb1864'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users engaging in focused work or study blocks are vulnerable to reflexive distraction app launches, while typical app-blocking solutions require pervasive permissions that risk logging user keystrokes, passwords, and sensitive on-screen content (FR-4.3, FR-10.1, NFR-4).

**Approach:** Implement a privacy-first `DistractionInterceptionService` via an opt-in Android `AccessibilityService` that restricts monitoring strictly to `TYPE_WINDOW_STATE_CHANGED` with `canRetrieveWindowContent = false` (guaranteeing zero text/keystroke inspection). When a blacklisted package enters the foreground during an active `DEEP_WORK` or `POMODORO_WORK` study session, invoke `InterceptDistractionUseCase` and present a non-punitive "Warm Organic Tactile" `SYSTEM_ALERT_WINDOW` overlay (`InterventionOverlay`) offering 1-tap options to Resume Focus, Take a 5m Break, or End Early, backed by lifecycle & permission MVI states in `SettingsViewModel` (AD-1, AD-4, AD-7, AD-8).

## Boundaries & Constraints

**Always:**
- Keep all domain logic in `com.personaltracker.domain` 100% pure Kotlin with zero `android.*` or `androidx.*` dependencies (AD-1).
- Set `canRetrieveWindowContent="false"` in `accessibility_service_config.xml` and drop all events other than `TYPE_WINDOW_STATE_CHANGED` in `DistractionInterceptionService` to guarantee zero keylogging or screen scraping (AD-8, NFR-4).
- Restrict distraction interception triggers strictly to active work intervals (`FocusIntervalType.DEEP_WORK` and `FocusIntervalType.POMODORO_WORK`); never intercept during rest intervals (`SHORT_BREAK`, `LONG_BREAK`) or when no focus session is active.
- Draw the `InterventionOverlay` using Jetpack Compose (`ComposeView`) adhering strictly to the Warm Organic Tactile design tokens: Warm Cream background (`#F9F6F0`), 48dp minimum touch target floor, and non-punitive copy (AD-4, AD-7).
- Safely check `Settings.canDrawOverlays(context)` before attaching window overlay to prevent system crashes.
- Expose permission readiness (`isAccessibilityGranted`, `isOverlayGranted`) in `SettingsUiState` via unidirectional MVI (AD-2).

**Ask First:**
- Adding mandatory blocking overrides that prevent exiting or trap the user without recourse.
- Storing or persisting opened third-party package names or user app usage timelines in database entities (strict privacy invariant).

**Never:**
- Call `AccessibilityEvent.getText()`, `AccessibilityEvent.getSource()`, or inspect view nodes / input fields (AD-8).
- Display guilt-inducing or shaming copy (e.g. "You failed your focus", "App blocked", "Bad habit") in the intervention overlay (AD-4).
- Write hardcoded color hex literals (`#...`), `dp`, or `sp` literals in UI composables; resolve exclusively from `PersonalTrackerTheme` (AD-7).
- Intercept system packages, dialers, or Personal-Tracker's own package.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Distraction App Launched During Deep Work | Foreground window transitions to `com.instagram.android` while `StudySessionTimerState(isRunning = true, intervalType = DEEP_WORK)` | `InterceptDistractionUseCase` returns `shouldIntercept = true`; `InterventionOverlay` surfaces with "Resume Focus", "Take a 5m Break", "End Early" | N/A |
| Break Interval Active | Foreground window transitions to blacklisted app while `StudySessionTimerState(isRunning = true, intervalType = SHORT_BREAK)` | `InterceptDistractionUseCase` returns `shouldIntercept = false`; zero overlay shown; user browses freely | N/A |
| Timer Inactive / Paused | Foreground window transitions to blacklisted app while timer is idle (`isRunning = false`) or paused | `InterceptDistractionUseCase` returns `shouldIntercept = false`; zero overlay shown | N/A |
| Unlisted / Whitelisted App | Foreground window transitions to unconfigured app (e.g. calculator or notes) during active focus | `shouldIntercept = false`; app opens without obstruction | N/A |
| Non-Window Accessibility Event | Accessibility event received with `eventType = TYPE_VIEW_CLICKED` or `TYPE_VIEW_TEXT_CHANGED` | Event dropped immediately; zero processing or text extraction | Guarded by early return |
| Overlay Permission Missing | Blacklisted app opened during focus, but `canDrawOverlays` is false | Interception logs warning; falls back to bringing `MainActivity` to front without crashing | Try/catch around WindowManager addView |
| User Taps "Resume Focus" | User clicks primary CTA in `InterventionOverlay` | Overlay dismissed; `MainActivity` brought to foreground with focus timer running | Cleans up ComposeView and Lifecycle |
| User Taps "Take a 5m Break" | User clicks break CTA in `InterventionOverlay` | Overlay dismissed; timer paused/transitioned to 5m break; `MainActivity` brought to front | Safely updates timer service |
| User Taps "End Early" | User clicks end CTA in `InterventionOverlay` | Overlay dismissed; focus session gracefully stopped/interrupted; non-punishing completion | Safely stops service |

</frozen-after-approval>

## Code Map

- `app/src/main/res/xml/accessibility_service_config.xml` -- Accessibility service configuration specifying `typeWindowStateChanged`, `canRetrieveWindowContent="false"`, and notification timeout.
- `app/src/main/AndroidManifest.xml` -- Declares `DistractionInterceptionService` with `BIND_ACCESSIBILITY_SERVICE` permission and `SYSTEM_ALERT_WINDOW` permission.
- `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Pure Kotlin use case evaluating package distraction rules against active `StudySession` interval types (`DEEP_WORK`, `POMODORO_WORK`).
- `app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt` -- Platform AccessibilityService filtering strictly for `TYPE_WINDOW_STATE_CHANGED`, dropping all interaction/text events, and triggering the overlay.
- `app/src/main/java/com/personaltracker/platform/accessibility/InterventionOverlay.kt` -- System alert overlay controller hosting ComposeView with isolated `OverlayLifecycleOwner` and Warm Organic Tactile card UI.
- `app/src/main/java/com/personaltracker/ui/components/overlays/InterventionCard.kt` -- Stateless Compose card rendering Warm Cream container (`colors.surfaceCream`), non-punitive copy, and 3 tactile action buttons (48dp min touch target).
- `app/src/main/java/com/personaltracker/platform/timer/StudySessionTimerState.kt` -- Extends timer state with `intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` -- Extended with `isAccessibilityGranted` and `isOverlayGranted`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiIntent.kt` -- Extended with `CheckPermissions`, `OpenAccessibilitySettings`, and `OpenOverlaySettings`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiEffect.kt` -- Extended with `OpenSystemSetting` effect.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Observes and exposes permission states; dispatches navigation intents to system settings.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Renders "Focus Protection & Privacy" card with permission toggles and zero-keylogging reassurance banner.
- `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- Unit tests validating interception across work intervals, break intervals, idle state, and unlisted packages.
- `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Unit tests validating permission states and MVI intent handling.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/res/xml/accessibility_service_config.xml` -- Create accessibility configuration XML with `typeWindowStateChanged` and `canRetrieveWindowContent="false"` -- Enforces AD-8 zero keylogging.
- [x] `app/src/main/AndroidManifest.xml` -- Register `DistractionInterceptionService` and `SYSTEM_ALERT_WINDOW` permission -- Required for OS-level event listening and overlay presentation.
- [x] `app/src/main/java/com/personaltracker/platform/timer/StudySessionTimerState.kt` -- Add `intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK` -- Enables interval awareness across platform services.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Implement pure Kotlin distraction interceptor -- Checks active interval work status and blocked package rules without Android dependencies.
- [x] `app/src/main/java/com/personaltracker/ui/components/overlays/InterventionCard.kt` -- Implement Warm Organic Tactile Compose intervention card -- Renders non-punitive prompt with "Resume Focus", "Take a 5m Break", and "End Early" actions.
- [x] `app/src/main/java/com/personaltracker/platform/accessibility/InterventionOverlay.kt` -- Build WindowManager overlay controller using ComposeView with `OverlayLifecycleOwner` -- Manages SYSTEM_ALERT_WINDOW lifecycle safely.
- [x] `app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt` -- Implement event-driven AccessibilityService tracking only `TYPE_WINDOW_STATE_CHANGED` and triggering overlay -- Delivers core distraction defense.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` & `SettingsUiIntent.kt` & `SettingsUiEffect.kt` -- Add permission fields, intents, and effects -- Supports settings permission MVI flow.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Add permission status checks and intent handlers -- Drives settings UI.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Add "Focus Protection & Privacy" card with permission controls and privacy copy -- Delivers user configuration surface.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- Add unit tests for `InterceptDistractionUseCase` -- Verifies work/break intervals and blacklisted package routing.
- [x] `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Add unit tests for SettingsViewModel permission flows -- Verifies permission state changes and effects.

**Acceptance Criteria:**
- Given an active `DEEP_WORK` or `POMODORO_WORK` focus session, when a configured blacklisted package is opened, then `InterceptDistractionUseCase` returns `shouldIntercept = true` and `InterventionOverlay` is displayed.
- Given an active `SHORT_BREAK` or `LONG_BREAK` session or an idle timer, when any app is opened, then `InterceptDistractionUseCase` returns `shouldIntercept = false` and no overlay is triggered.
- Given `DistractionInterceptionService`, when any event other than `TYPE_WINDOW_STATE_CHANGED` arrives, then the event is dropped immediately with zero text, layout, or keystroke inspection (AD-8).
- Given the `InterventionOverlay`, when rendered, then it displays the Warm Organic Tactile aesthetic (`#F9F6F0` background) with zero hardcoded literals, 48dp minimum touch target floor, and non-punitive copy offering "Resume Focus", "Take a 5m Break", and "End Early" (AD-4, AD-7).
- Given `SettingsScreen`, when viewed, then it displays the user's Accessibility Service and Overlay permission statuses with 1-tap navigation to system settings and zero-keylogging reassurance copy.
- Given `InterceptDistractionUseCase`, when inspected, then it contains zero `android.*` or `androidx.*` imports (AD-1).

## Spec Change Log

- 2026-10-26: Implemented Story 3.2 end-to-end. Created accessibility config XML (`canRetrieveWindowContent="false"`), registered `DistractionInterceptionService` and `SYSTEM_ALERT_WINDOW` in `AndroidManifest.xml`, extracted `StudySessionTimerState` with `intervalType: FocusIntervalType`, implemented pure Kotlin `InterceptDistractionUseCase`, created Warm Organic Tactile `InterventionCard` and WindowManager `InterventionOverlay` with `OverlayLifecycleOwner`, updated `SettingsViewModel` with permission readiness MVI flow and added "Focus Protection & Privacy" card on `SettingsScreen`, and added JVM unit tests passing 100%.

## Design Notes

- **Zero Keylogging Invariant (AD-8, NFR-4):**
  - In `accessibility_service_config.xml`: `android:canRetrieveWindowContent="false"` and `android:accessibilityEventTypes="typeWindowStateChanged"`.
  - In `DistractionInterceptionService.kt`: `if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return`. Never invoke `event.source`, `event.text`, or traverse accessibility nodes.
- **Compose ViewTree Owners for Overlay (EXP-016):**
  - Standalone `OverlayLifecycleOwner` implements `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner` to enable Jetpack Compose inside WindowManager `SYSTEM_ALERT_WINDOW` without an Activity dependency.
- **Warm Organic Tactile Palette Conformance (AD-7):**
  - Overlay Card: `colors.surfaceCream` (`#F9F6F0`), stroke `colors.borderSubtle` (`#EBE5DC`), rounded corners `shapes.xxl` (24dp).
  - Primary CTA: `colors.primary` (`#F26B1D`), 52dp height, bold label `typography.titleMedium` (`#FFFFFF`).
  - Break CTA: `colors.iconSoftPeach` (`#FCD9B8`) or `colors.surfaceSunken`, 48dp min height.
  - End Early: `colors.inkTertiary` (`#8C7A6B`), text button.

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: ALL ARCHITECTURAL PURITY CHECKS PASSED.
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: 0 findings.
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All H0 components valid.
- `.\gradlew test` -- expected: BUILD SUCCESSFUL, all unit tests pass.
- `.\gradlew lintDebug` -- expected: BUILD SUCCESSFUL, 0 lint errors.
