---
title: 'Story 3.3: Emergency Telecom & Dynamic Whitelist Bypass'
type: 'feature'
created: '2026-11-13'
status: 'done'
baseline_commit: '88144788f0712ff0ef7f4186b9fa53ca13024bc9'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
  - '.harness2/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Distraction interception during focus sessions risks obstructing life-critical communications, emergency dialers, system UI, input method editors (keyboards), or vital productivity tools (authenticators, password managers), jeopardizing user safety and causing workflow disruption (FR-4.6, FR-10.1, ARCH-5).

**Approach:** Strengthen `InterceptDistractionUseCase` with automatic emergency/telecom/dialer and IME immunity; implement pure Kotlin interactors (`GetInterventionRulesUseCase.kt`, `TogglePackageInterceptionRuleUseCase.kt`, `AddWhitelistedAppUseCase.kt`, `RemoveWhitelistedAppUseCase.kt`) managing categorized package rules on `Dispatchers.IO`; wire categorized rules, search filtering, and toggle intents into `SettingsViewModel` MVI; and provide a token-strict stateless `WhitelistManagerSheet.kt` and `SettingsScreen.kt` whitelist section in the Warm Organic Tactile design language (AD-1, AD-2, AD-4, AD-7, AD-8).

## Boundaries & Constraints

**Always:**
- Keep all domain logic in `com.personaltracker.domain` 100% pure Kotlin with zero `android.*` or `androidx.*` dependencies (AD-1).
- Unconditionally exempt emergency services, telecom dialers, default dialers (`TelecomManager.defaultDialerPackage`), system UI, and input method editors (IMEs / keyboards) from interception under all circumstances (FR-4.6, FR-10.1).
- Immediately dismiss any active intervention overlay upon detecting incoming or outgoing phone calls (`CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`) or default dialer foregrounding.
- Respect explicit user whitelist exemptions (`isStrictBlocked = false`) during active work sessions so configured tools (e.g. authenticators, notes) open unimpeded.
- Adhere strictly to Warm Organic Tactile design tokens in `SettingsScreen` and `WhitelistManagerSheet`: zero raw color hex literals, zero raw `dp`/`sp` literals, and 48dp minimum touch target floor (`spacing.minTouchTarget`) (AD-7).
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
| Emergency Dialer Foregrounded | Active `DEEP_WORK` timer; package `com.android.emergency`, `com.google.android.dialer`, or `com.android.incallui` opened | `InterceptDistractionUseCase` returns `shouldIntercept = false`; zero overlay shown; call connects immediately | Guaranteed by system bypass set |
| Default Dialer Dynamic Bypass | Active focus timer; device's `defaultDialerPackage` opened | `shouldIntercept = false`; overlay dismissed if active; dialer unrestricted | Null-safe fallback to regex heuristic |
| Incoming / Active Phone Call | Phone rings (`CALL_STATE_RINGING`) or call active (`CALL_STATE_OFFHOOK`) while overlay displayed | Overlay dismissed immediately; focus timer handles pause/continue per `FocusTimerPolicy` | Try/catch around Telephony listener |
| Soft Keyboard / IME Transition | Virtual keyboard or input method (e.g. `com.google.android.inputmethod.latin`, SwiftKey) transitions | `shouldIntercept = false`; zero overlay triggered | Filtered via IME segment check |
| User Whitelisted App Opened | Active `DEEP_WORK` timer; user whitelisted app (e.g. `com.google.android.apps.authenticator2`, `isStrictBlocked = false`) opened | `InterceptDistractionUseCase` returns `shouldIntercept = false`; app opens without friction | Matched rule returns non-blocking |
| Blacklisted App Opened | Active `DEEP_WORK` timer; blacklisted app (e.g. `com.instagram.android`, `isStrictBlocked = true`) opened | `InterceptDistractionUseCase` returns `shouldIntercept = true`; intervention card surfaces | Displays 1-tap options |
| System UI Transition | Navigation bar or notification shade (`com.android.systemui`) transitions | `shouldIntercept = false`; does NOT dismiss active overlay (prevents shade bypass exploit) | Segment check + service check |
| Search Whitelist Rules | User enters "auth" in Whitelist Manager search bar | `SettingsUiState.searchQuery` updates; rules filtered in-memory to matching apps | Empty query restores full list |
| Toggle App Whitelist Intent | User clicks toggle on app in Whitelist Manager | `TogglePackageInterceptionRuleUseCase` updates Room entity (`isStrictBlocked = !current` or deletes whitelist preset); Flow SSOT updates UI | Room `@Upsert` on IO dispatcher |
| Add Custom Package Exemption | User enters package name and display name in whitelist sheet | `AddWhitelistedAppUseCase` inserts `InterventionRule(isStrictBlocked = false)`; immediate UI reflection | Validates non-blank package name |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/domain/model/FocusTimerPolicy.kt` -- Pure Kotlin enum (`AUTO_PAUSE`, `CONTINUE_COUNTDOWN`) specifying timer behavior during active telecom calls.
- `app/src/main/java/com/personaltracker/domain/repository/InterventionRuleRepository.kt` -- Repository interface providing reactive flows for all, whitelisted, and blocked rules.
- `app/src/main/java/com/personaltracker/data/local/db/daos/InterventionRuleDao.kt` -- SQLite queries for rule operations (`getAllRules`, `getWhitelistedRules`, `@Upsert`).
- `app/src/main/java/com/personaltracker/data/repository/InterventionRuleRepositoryImpl.kt` -- Repository implementation executing Room operations on `Dispatchers.IO`.
- `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Core domain use case with automatic emergency telecom exemption (`com.android.dialer`, `com.google.android.dialer`, `com.android.incallui`, `com.android.server.telecom`), IME keyboards, dynamic `defaultDialerPackage`, call state bypass, and user whitelist rules.
- `app/src/main/java/com/personaltracker/domain/usecase/GetInterventionRulesUseCase.kt` -- Pure Kotlin interactor providing categorized rules (allowed vs blocked) with search query filtering on `Dispatchers.IO`.
- `app/src/main/java/com/personaltracker/domain/usecase/TogglePackageInterceptionRuleUseCase.kt` -- Pure Kotlin interactor toggling interception or whitelist status on `Dispatchers.IO`.
- `app/src/main/java/com/personaltracker/domain/usecase/GetWhitelistRulesUseCase.kt` & `AddWhitelistedAppUseCase.kt` & `RemoveWhitelistedAppUseCase.kt` -- Additional whitelist CRUD interactors.
- `app/src/main/java/com/personaltracker/platform/accessibility/DistractionInterceptionService.kt` -- Queries `TelecomManager.defaultDialerPackage`, observes `TelephonyManager` call states, and dismisses overlay on call ringing/offhook.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` -- Extended with `whitelistedRules`, `blockedRules`, `searchQuery`, `focusTimerPolicy`, and `isWhitelistSheetOpen`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiIntent.kt` -- Extended with `SearchWhitelistRules`, `TogglePackageInterceptionRule`, `ToggleAppWhitelist`, `AddCustomWhitelistPackage`, `RemoveWhitelistPackage`, and `ToggleWhitelistSheet`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Injects rule interactors, binds combined reactive streams with search filtering, and handles MVI intents.
- `app/src/main/java/com/personaltracker/ui/screens/settings/WhitelistManagerSheet.kt` -- Dedicated stateless Warm Organic Tactile modal bottom sheet with Allowed/Blocked tabs, search bar, quick presets, and custom exemption entry.
- `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Settings screen hosting the Whitelist section and invoking `WhitelistManagerSheet`.
- `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- JVM unit tests covering emergency bypass, default dialer, active call, IME keyboards, and whitelist exemptions.
- `app/src/test/java/com/personaltracker/domain/usecase/InterventionRuleUseCasesTest.kt` -- JVM unit tests for categorized rules, search filtering, and rule toggling.
- `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Turbine unit tests asserting whitelist search, toggle, and dialog state transitions.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/domain/usecase/InterceptDistractionUseCase.kt` -- Add `com.android.incallui`, generic IME keyboards (`.inputmethod.`, `ime`), and emergency dialers to system bypass -- Ensures emergency actions and text input are never blocked.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/GetInterventionRulesUseCase.kt` -- Implement pure Kotlin interactor streaming categorized and filtered rules -- Delivers clean domain separation for settings management.
- [x] `app/src/main/java/com/personaltracker/domain/usecase/TogglePackageInterceptionRuleUseCase.kt` -- Implement pure Kotlin interactor toggling package interception state on `Dispatchers.IO` -- Handles rule inversion or deletion per EXP-036.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsUiState.kt` & `SettingsUiIntent.kt` -- Add categorized rules (`whitelistedRules`, `blockedRules`), `searchQuery`, and `SearchWhitelistRules` / `TogglePackageInterceptionRule` intents -- Completes MVI contract.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsViewModel.kt` -- Wire `GetInterventionRulesUseCase` and `TogglePackageInterceptionRuleUseCase`, handle search and toggle intents -- Drives presentation state.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/WhitelistManagerSheet.kt` -- Create stateless Warm Organic Tactile sheet for Allowed & Blocked app management with search, presets, and custom exemption dialog -- Delivers token-strict UI.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/SettingsScreen.kt` -- Embed Whitelist Manager summary card and sheet wiring -- Integrates into Settings screen.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/InterceptDistractionUseCaseTest.kt` -- Add tests for `com.android.incallui` and IME keyboards -- Verifies telecom and input bypass.
- [x] `app/src/test/java/com/personaltracker/domain/usecase/InterventionRuleUseCasesTest.kt` -- Add unit tests for categorized rule retrieval, search filtering, and toggling -- Verifies pure domain logic.
- [x] `app/src/test/java/com/personaltracker/ui/SettingsViewModelTest.kt` -- Add tests for search query filtering and rule toggling via Turbine -- Validates reactive MVI pipeline.

**Acceptance Criteria:**
- Given an active focus session, when an emergency dialer (`com.android.dialer`, `com.google.android.dialer`, `com.android.incallui`, `com.android.server.telecom`, or containing emergency/dialer/telecom) or the system's `defaultDialerPackage` is opened, then `InterceptDistractionUseCase` returns `shouldIntercept = false` and no overlay is displayed (FR-4.6, FR-10.1).
- Given any input method editor or soft keyboard (e.g. `com.google.android.inputmethod.latin`), when opened, then `InterceptDistractionUseCase` returns `shouldIntercept = false`.
- Given an active distraction overlay, when an incoming or outgoing call occurs (`isCallActive = true`), then the overlay window is immediately dismissed and `shouldIntercept = false` is returned (FR-4.6).
- Given an app marked as whitelisted (`isStrictBlocked = false`), when opened during `DEEP_WORK` or `POMODORO_WORK`, then `InterceptDistractionUseCase` returns `shouldIntercept = false` with reason "Package is explicitly whitelisted by user."
- Given `WhitelistManagerSheet`, when rendered, then it strictly conforms to Warm Organic Tactile design tokens (0 raw hex colors, 0 raw `dp`/`sp` literals, 48dp minimum touch targets).
- Given `SettingsScreen`, when the user searches or toggles an app's whitelist status, then `SettingsViewModel` dispatches the interactor and the UI updates reactively via Flow SSOT.
- Given `com.personaltracker.domain`, when analyzed, then it contains zero imports of `android.*` or `androidx.*` (AD-1).

## Spec Change Log

_None._

## Design Notes

- **Warm Organic Tactile Palette & Tokens:**
  - Card & sheet surface: `PersonalTrackerTheme.colors.surfaceCard` (Warm Cream `#F9F6F0`)
  - Primary accent / active toggles: `PersonalTrackerTheme.colors.primaryOrange` (Vibrant Orange `#F26B1D`)
  - Typography & secondary controls: `PersonalTrackerTheme.colors.espressoDark` (Deep Espresso `#59280E`)
  - Muted borders & track: `PersonalTrackerTheme.colors.sandTrack` (`#EBE5DC`)
  - Touch targets: minimum 48dp (`spacing.minTouchTarget`)

## Verification

**Commands:**
- `python .agents/scripts/domain_purity_check.py` -- expected: `ALL ARCHITECTURAL PURITY CHECKS PASSED`
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: `ok: true, total_findings: 0`
- `python .agents/scripts/harness2_bridge.py --check` -- expected: `All 8 H0 components and Harness² core are active and valid.`
- `.\gradlew test` -- expected: `BUILD SUCCESSFUL` with all unit tests passing
- `.\gradlew lintDebug` -- expected: `BUILD SUCCESSFUL` with 0 lint errors
