---
title: 'Story 3.3: Emergency Telecom & Dynamic Whitelist Bypass'
type: 'feature'
created: '2026-10-26'
status: 'done'
baseline_commit: 'ee1dfbc95c4b3ead6fe51090265ceac5b397a2ed'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '_bmad-output/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Distraction interception during deep work sessions risks obstructing emergency communications, system UI, or critical productivity tools (dialers, authenticator apps, password managers) if hardcoded or unconfigurable, endangering user safety and causing workflow friction (FR-4.6, FR-10.1, ARCH-5).

**Approach:** Upgrade `InterceptDistractionUseCase` with automatic emergency/telecom/system bypass rules, implement pure Kotlin whitelist interactors managing user-configured package exemptions, integrate whitelist management and call policy into `SettingsViewModel` MVI, and render a token-strict Warm Organic Tactile whitelist manager in `SettingsScreen` (AD-1, AD-2, AD-4, AD-7, AD-8).

## Boundaries & Constraints

**Always:**
- Keep all domain logic in `com.personaltracker.domain` 100% pure Kotlin with zero `android.*` or `androidx.*` dependencies (AD-1).
- Unconditionally exempt emergency services, telecom dialers, default dialers, and critical system UI from interception under all circumstances (FR-4.6, FR-10.1).
- Immediately suspend or dismiss any active intervention overlay upon detecting incoming or outgoing phone calls (`CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`) or default dialer foregrounding.
- Respect explicit user whitelist exemptions (`isStrictBlocked = false`) during active work sessions so configured tools (e.g. authenticators, notes) open unimpeded.
- Adhere strictly to the Warm Organic Tactile design tokens in `SettingsScreen`: 0 raw color hex literals, 0 raw `dp`/`sp` literals, and 48dp minimum touch target floor (`spacing.minTouchTarget`) (AD-7).
- Follow unidirectional MVI in `SettingsViewModel`: single immutable `SettingsUiState`, sealed `SettingsUiIntent`, and Channel-backed `SettingsUiEffect` (AD-2).

**Ask First:**
- Hardcoding custom third-party vendor package names instead of general pattern heuristics.
- Mutating study session persistence during phone calls without honoring `FocusTimerPolicy`.

**Never:**
- Intercept, block, or delay emergency calls (e.g. 911, 112, SOS) or phone dialers under any condition.
- Import Android framework classes (`Context`, `TelecomManager`, `TelephonyManager`) into `domain/` use cases or models (AD-1).
- Display guilt-inducing, shaming, or alarmist copy when apps are whitelisted or during emergency bypass (AD-4).
- Use `OnConflictStrategy.REPLACE` on Room entities with cascading relationships (AD-3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Emergency Dialer Foregrounded | Active `DEEP_WORK` timer; package `com.android.emergency` or `com.google.android.dialer` opened | `InterceptDistractionUseCase` returns `shouldIntercept = false`; zero overlay shown; call connects immediately | Guaranteed by system bypass set |
| Default Dialer Dynamic Bypass | Active focus timer; device's `defaultDialerPackage` opened | `shouldIntercept = false`; overlay dismissed if active; dialer unrestricted | Null-safe fallback to regex heuristic |
| Incoming / Active Phone Call | Phone rings (`CALL_STATE_RINGING`) or call active (`CALL_STATE_OFFHOOK`) while overlay displayed | Overlay dismissed immediately; focus timer handles pause/continue per `FocusTimerPolicy` | Try/catch around Telephony listener |
| User Whitelisted App Opened | Active `DEEP_WORK` timer; user whitelisted app (e.g. `com.google.android.apps.authenticator2`, `isStrictBlocked = false`) opened | `InterceptDistractionUseCase` returns `shouldIntercept = false`; app opens without friction | Matched rule returns non-blocking |
| Blacklisted App Opened | Active `DEEP_WORK` timer; blacklisted app (e.g. `com.instagram.android`, `isStrictBlocked = true`) opened | `InterceptDistractionUseCase` returns `shouldIntercept = true`; intervention card surfaces | Displays 1-tap options |
| System UI / Keyboard Transition | Navigation bar, notification shade, or soft keyboard (`com.android.systemui`, LatinIME) transitions | `shouldIntercept = false`; zero overlay | Filtered via prefix/excluded set |
| Toggle App Whitelist Intent | User clicks toggle on app in Settings whitelist | `ToggleAppWhitelistUseCase` updates Room entity (`isStrictBlocked = !current`); Flow SSOT updates UI | Room `@Upsert` on IO dispatcher |
| Add Custom Package Exemption | User enters package name and display name in whitelist dialog | `AddWhitelistedAppUseCase` inserts `InterventionRule(isStrictBlocked = false)`; immediate UI reflection | Validates non-blank package name |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/FocusTimerPolicy.kt` -- Pure Kotlin enum (`AUTO_PAUSE`, `CONTINUE_COUNTDOWN`) specifying timer behavior during active telecom calls.
- `app/src/main/java/com/personaltracker/domain/repository/InterventionRuleRepository.kt` -- Extended with `getAllRules(): Flow<List<InterventionRule>>` and `getWhitelistedRules(): Flow<List<InterventionRule>>`.
- `app/src/main/java/com/personaltracker/data/local/db/daos/InterventionRuleDao.kt` -- Queries for `getAllRules()` and `getWhitelistedRules()`.
- `app/src/main/java/com/personaltracker/data/repository/InterventionRuleRepositoryImpl.kt` -- Implements reactive rule queries mapping Room entities to domain models on `Dispatchers.IO`.
- `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Updated with emergency package heuristics, system exclusions, dynamic `defaultDialerPackage`, call state bypass, and explicit user whitelist exemption check.
- `app/src/main/java/com/personaltracker/domain/usecase/GetWhitelistRulesUseCase.kt` -- Interactor streaming user-whitelisted and managed app rules.
- `app/src/main/java/com/personaltracker/domain/usecase/ToggleAppWhitelistUseCase.kt` -- Interactor toggling package strict blocking vs whitelisted exemption.
- `app/src/main/java/com/personaltracker/domain/usecase/AddWhitelistedAppUseCase.kt` -- Interactor adding package exemption.
- `app/src/main/java/com/personaltracker/domain/usecase/RemoveWhitelistedAppUseCase.kt` -- Interactor deleting custom whitelist entry.
- `app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt` -- Queries `TelecomManager.defaultDialerPackage`, listens to `TelephonyManager` call states, and suspends overlay upon call activity.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` -- Extended with `whitelistedRules`, `allInterventionRules`, `focusTimerPolicy`, and `isWhitelistSheetOpen`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiIntent.kt` -- Extended with `ToggleAppWhitelist`, `AddCustomWhitelistPackage`, `RemoveWhitelistPackage`, `SetFocusTimerPolicy`, and `ToggleWhitelistSheet`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Injects whitelist interactors, binds reactive streams, and handles whitelist management intents.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Adds Warm Organic Tactile Whitelist Manager section with chip list, quick presets (Authenticator, Password Manager, Notes, Calculator), and custom exemption dialog.
- `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- JVM unit tests covering emergency bypass, default dialer, active phone call, system UI, and user whitelist exemptions.
- `app/src/test/java/com/personaltracker/domain/usecase/WhitelistUseCasesTest.kt` -- JVM unit tests for whitelist CRUD interactors.
- `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Turbine unit tests asserting whitelist state emissions and intent handling.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/model/FocusTimerPolicy.kt` -- Create pure Kotlin `FocusTimerPolicy` enum -- Governs timer progression during calls without Android dependencies.
- [x] `app/src/main/java/com/personaltracker/data/local/db/daos/InterventionRuleDao.kt` -- Add `getAllRules()` and `getWhitelistedRules()` queries -- Exposes complete and whitelisted rule sets from SQLite.
- [x] `app/src/main/java/com/personaltracker/domain/repository/InterventionRuleRepository.kt` & `InterventionRuleRepositoryImpl.kt` -- Expose reactive flows for all and whitelisted rules -- Connects data layer to domain contracts.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Update with emergency/telecom/dialer sets, dynamic default dialer, active call bypass, and whitelist exemption check -- Guarantees safety and respects user exemptions.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/GetWhitelistRulesUseCase.kt` & `ToggleAppWhitelistUseCase.kt` & `AddWhitelistedAppUseCase.kt` & `RemoveWhitelistedAppUseCase.kt` -- Implement pure Kotlin whitelist interactors -- Delivers business logic for whitelist management.
- [x] `app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt` -- Integrate `TelecomManager` default dialer resolution and call state listener to dismiss overlay -- Connects platform telecom signals to interception.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` & `SettingsUiIntent.kt` -- Add whitelist and call policy fields and intents -- Expands MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Inject whitelist interactors and handle whitelist intents -- Drives presentation state.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Implement stateless Warm Organic Tactile Whitelist Manager section with quick presets and exemption sheet -- Delivers user configuration surface.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- Extend unit tests with telecom bypass, active call, and whitelist cases -- Verifies safety rules.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/WhitelistUseCasesTest.kt` -- Create unit test suite for whitelist domain interactors -- Validates pure Kotlin CRUD logic.
- [x] `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Extend ViewModel test suite with whitelist toggles, preset additions, and policy changes -- Validates reactive MVI pipeline.

**Acceptance Criteria:**
- Given an active focus session, when an emergency dialer (`com.android.emergency`, `com.google.android.dialer`, or package containing "emergency"/"dialer"/"telecom") or the system's `defaultDialerPackage` is opened, then `InterceptDistractionUseCase` returns `shouldIntercept = false` and no overlay is triggered (FR-4.6, FR-10.1).
- Given an active distraction overlay, when an incoming or outgoing call occurs (`isCallActive = true`), then the overlay window is immediately dismissed and `shouldIntercept = false` is returned (FR-4.6).
- Given an app marked as whitelisted (`isStrictBlocked = false`), when opened during `DEEP_WORK` or `POMODORO_WORK`, then `InterceptDistractionUseCase` returns `shouldIntercept = false` with reason "Package is explicitly whitelisted by user."
- Given `SettingsScreen`, when the user toggles an app's whitelist status or adds a custom package exemption, then `SettingsViewModel` dispatches the interactor and the UI updates reactively via Flow SSOT.
- Given the Whitelist Manager UI, when rendered, then it strictly conforms to Warm Organic Tactile design tokens (0 raw hex colors, 0 raw `dp`/`sp` literals, 48dp minimum touch targets).
- Given `com.personaltracker.domain`, when analyzed, then it contains zero imports of `android.*` or `androidx.*` (AD-1).

## Spec Change Log

*(Empty until review)*

## Design Notes

- **Emergency Pattern Heuristic:** In addition to hardcoded sets (`com.android.emergency`, `com.google.android.dialer`, `com.android.systemui`), package names are checked against safety tokens: `emergency`, `dialer`, `telecom`, `incall`, `telephony`, `phone`, `sos`.
- **Preset Quick-Add Entries:** `SettingsScreen` provides convenient 1-tap toggles for common essential packages:
  - Authenticator: `com.google.android.apps.authenticator2` ("Authenticator")
  - Password Manager: `com.bitwarden.bitwarden` ("Bitwarden")
  - Notes: `com.google.android.keep` ("Google Keep")
  - Calculator: `com.google.android.calculator` ("Calculator")

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: ALL ARCHITECTURAL PURITY CHECKS PASSED (0 android.* imports in domain, 0 raw hex in UI)
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: ok: true, total_findings: 0
- `.\gradlew test` -- expected: All JVM unit tests pass clean (0 failures)
- `.\gradlew lintDebug` -- expected: 0 lint errors
- `python .agents/scripts/harness2_bridge.py --check` -- expected: All 8 H0 components valid

## Suggested Review Order

**Emergency Telecom & Safety Bypass**

- Unconditional emergency services, dialers, system UI, and active call bypass
  [`InterceptDistractionUseCase.kt:70`](../../app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt#L70)

- Dynamic dialer query, telephony call listener, and safe overlay dismissal
  [`DistractionInterceptionService.kt:70`](../../app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt#L70)

**Pure Kotlin Whitelist Interactors**

- Toggle app whitelist rule without converting utility apps into blocked targets
  [`ToggleAppWhitelistUseCase.kt:13`](../../app/src/main/java/com/personaltracker/domain/usecase/ToggleAppWhitelistUseCase.kt#L13)

- Reactive streaming of whitelisted and all configured intervention rules
  [`GetWhitelistRulesUseCase.kt:19`](../../app/src/main/java/com/personaltracker/domain/usecase/GetWhitelistRulesUseCase.kt#L19)

- Defensive insertion of custom package exemptions
  [`AddWhitelistedAppUseCase.kt:13`](../../app/src/main/java/com/personaltracker/domain/usecase/AddWhitelistedAppUseCase.kt#L13)

- Exemption removal from Room intervention rule store
  [`RemoveWhitelistedAppUseCase.kt:12`](../../app/src/main/java/com/personaltracker/domain/usecase/RemoveWhitelistedAppUseCase.kt#L12)

**Persistence & Data Schema**

- Room queries for all rules and whitelisted apps
  [`InterventionRuleDao.kt:16`](../../app/src/main/java/com/personaltracker/data/local/db/daos/InterventionRuleDao.kt#L16)

- Repository implementation mapping entity models on IO dispatcher
  [`InterventionRuleRepositoryImpl.kt:21`](../../app/src/main/java/com/personaltracker/data/repository/InterventionRuleRepositoryImpl.kt#L21)

**MVI Settings & Tactile UI Presentation**

- Whitelist state, call policy, and sheet dialog integration in MVI
  [`SettingsViewModel.kt:70`](../../app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt#L70)

- Stateless Warm Organic Tactile Whitelist Manager with presets and dialog
  [`SettingsScreen.kt:417`](../../app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt#L417)

**Peripherals & Verification Suite**

- Emergency dialer and active telecom call bypass unit tests
  [`InterceptDistractionUseCaseTest.kt:190`](../../app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt#L190)

- Whitelist CRUD domain interactor tests
  [`WhitelistUseCasesTest.kt:50`](../../app/src/test/java/com/personaltracker/domain/usecase/WhitelistUseCasesTest.kt#L50)

- MVI ViewModel whitelist toggle, preset, and call policy tests
  [`SettingsViewModelTest.kt:219`](../../app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt#L219)

