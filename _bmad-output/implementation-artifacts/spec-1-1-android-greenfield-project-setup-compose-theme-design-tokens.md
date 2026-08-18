---
title: 'Story 1.1: Android Greenfield Project Setup, Compose Theme & Design Tokens'
type: 'feature'
created: '2026-08-16'
status: 'done'
baseline_commit: '6d360d0420fbfe3ac4d163cad24701d1dfa2add3'
review_loop_iteration: 0
context:
  - docs/specifications/TECHSTACK.md
  - docs/specifications/architecture.md
  - docs/design/design-system.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The repository lacks standard Gradle Kotlin DSL build scripts (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`), Android manifest, Hilt entry point, and 5-tab Bottom Navigation scaffold required to run, build, and test the application on Android.

**Approach:** Initialize the standard Android build scaffolding with Gradle Version Catalog, Kotlin 2.4.0, Compose BOM 2026.02.00, Hilt 2.55, and implement `MainActivity.kt` / `PersonalTrackerApp.kt` rendering the 5-tab Bottom Navigation bar (`Home`, `Habits`, `Study`, `Companion`, `Settings`) with edge-to-edge insets, 48dp minimum touch targets, and full Dark/Light theme token support.

## Boundaries & Constraints

**Always:**
- Use Gradle Kotlin DSL (`.gradle.kts`) with Version Catalog (`gradle/libs.versions.toml`).
- Enforce API 33–36 baseline (minSdk 33, targetSdk 36, compileSdk 36).
- Use Hilt DI (`@HiltAndroidApp`, `@AndroidEntryPoint`) with zero legacy KAPT (KSP only).
- Implement 5-tab navigation (`Home`, `Habits`, `Study`, `Companion`, `Settings`) with minimum 48dp touch targets and edge-to-edge Compose `WindowInsets`.
- Style UI strictly using `PersonalTrackerTheme` design tokens from `docs/design/design-system.md` and `DESIGN.md`.
- Support 100% offline baseline with zero telemetry dependencies.

**Ask First:**
- Any changes to Gradle plugin versions or Android SDK targets beyond `docs/specifications/TECHSTACK.md`.
- Any modification to domain entity schemas or navigation tab destinations.

**Never:**
- Never use KAPT; use KSP for annotation processing.
- Never hardcode color hexes or raw dp dimensions in composables; use `PTColors`, `PTSpacing`, `PTShape`, `PTElevation`, `PTTypography`.
- Never introduce network calls or cloud SDKs during startup.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Cold Launch (Light Mode) | App launch on system light theme | `PersonalTrackerApp` renders with `PTColors.Light`, surfaces at `#FFFFFF`/`#F8FAFC`, active tab = `Home` | Fallback to `PTColors.Light` if theme undetermined |
| Cold Launch (Dark Mode) | App launch on system dark theme | `PersonalTrackerApp` renders with `PTColors.Dark`, surfaces at `#0F172A`/`#1E293B`, active tab = `Home` | Fallback to `PTColors.Dark` if dark mode enabled |
| Tab Navigation | User taps `Habits`, `Study`, `Companion`, `Settings` | Screen transitions to target destination with 48dp touch target and selected tab icon/label highlight | 1-level deep modal navigation stack |
| Dynamic Font Scaling | System font scale set to 1.5x | Text scales properly via `sp` units without text clipping or button overflow | Responsive Compose containers |

</frozen-after-approval>

## Code Map

- `gradle/libs.versions.toml` -- Version catalog defining AGP 8.8.0, Kotlin 2.4.0, Compose BOM 2026.02.00, Hilt 2.55, KSP, SQLCipher 4.6.1, Rive 9.1.0.
- `settings.gradle.kts` -- Root repository and plugin management configuration.
- `build.gradle.kts` -- Root project buildscript declaring plugins.
- `gradle.properties` -- JVM arguments, AndroidX, KSP flags, and 16KB native page alignment.
- `app/build.gradle.kts` -- App module buildscript applying Android, Kotlin, Compose, KSP, and Hilt plugins.
- `app/src/main/AndroidManifest.xml` -- App manifest declaring application class, `MainActivity`, permissions baseline.
- `app/src/main/java/com/personaltracker/PersonalTrackerApplication.kt` -- Hilt Application class (`@HiltAndroidApp`).
- `app/src/main/java/com/personaltracker/MainActivity.kt` -- Main entry activity with `enableEdgeToEdge()` and Compose root.
- `app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt` -- Navigation host & bottom navigation bar defining 5 tabs (`Home`, `Habits`, `Study`, `Companion`, `Settings`).
- `app/src/main/java/com/personaltracker/ui/screens/` -- 5 placeholder screens (`HomeScreen`, `HabitsScreen`, `StudyScreen`, `CompanionScreen`, `SettingsScreen`) demonstrating theme integration.
- `app/src/test/java/com/personaltracker/NavigationTest.kt` -- Unit test verifying navigation destination routing and token adherence.

## Tasks & Acceptance

**Execution:**
- [x] `gradle/libs.versions.toml` -- Create version catalog with locked dependencies -- Sourced from `docs/specifications/TECHSTACK.md`.
- [x] `settings.gradle.kts` -- Configure plugin repositories and module structure -- Greenfield project baseline.
- [x] `build.gradle.kts` -- Configure root buildscript -- Greenfield project baseline.
- [x] `gradle.properties` -- Set AndroidX and JVM properties -- Platform build configuration.
- [x] `app/build.gradle.kts` -- Configure app module with Compose, Hilt, KSP -- Sourced from `TECHSTACK.md`.
- [x] `app/src/main/AndroidManifest.xml` -- Declare `PersonalTrackerApplication` and `MainActivity` -- Android manifest baseline.
- [x] `app/src/main/java/com/personaltracker/PersonalTrackerApplication.kt` -- Implement `@HiltAndroidApp` class -- DI foundation.
- [x] `app/src/main/java/com/personaltracker/MainActivity.kt` -- Implement `@AndroidEntryPoint` with `enableEdgeToEdge()` -- Main activity entry.
- [x] `app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt` -- Implement 5-tab Navigation bar and NavHost -- Sourced from `EXPERIENCE.md` §2 & `UX-DR18`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/HomeScreen.kt` -- Implement Home destination placeholder with theme tokens -- UI scaffold.
- [x] `app/src/main/java/com/personaltracker/ui/screens/HabitsScreen.kt` -- Implement Habits destination placeholder -- UI scaffold.
- [x] `app/src/main/java/com/personaltracker/ui/screens/StudyScreen.kt` -- Implement Study destination placeholder -- UI scaffold.
- [x] `app/src/main/java/com/personaltracker/ui/screens/CompanionScreen.kt` -- Implement Companion destination placeholder -- UI scaffold.
- [x] `app/src/main/java/com/personaltracker/ui/screens/SettingsScreen.kt` -- Implement Settings destination placeholder -- UI scaffold.
- [x] `app/src/test/java/com/personaltracker/NavigationTest.kt` -- Add unit tests verifying tab routes, icon mappings, and theme integration -- Acceptance verification.

**Acceptance Criteria:**
- Given clean project build scripts configured with Kotlin 2.4.0, Compose BOM 2026.02.00, and Hilt 2.55, when build files are parsed, then zero legacy KAPT plugins are present and KSP is used.
- Given the application launches on light or dark mode, when `PersonalTrackerApp` renders, then all theme tokens (`PTColors`, `PTTypography`, `PTSpacing`, `PTShape`) load with 100% parity to `docs/design/design-system.md`.
- Given 5 navigation tabs (`Home`, `Habits`, `Study`, `Companion`, `Settings`), when any tab is selected, then navigation updates state with 48dp touch targets and edge-to-edge system insets.

## Spec Change Log

*(Empty — initial draft)*

## Verification

**Commands:**
- `git status` -- expected: Clean working tree and all new files tracked.
- `python -c "import os; assert os.path.exists('gradle/libs.versions.toml')"` -- expected: Version catalog present.

## Suggested Review Order

**Entry Point & Navigation**

- Main activity bootstrap configuring edge-to-edge insets and theme container
  [`MainActivity.kt:15`](../../app/src/main/java/com/personaltracker/MainActivity.kt#L15)

- Root 5-tab Bottom Navigation bar and NavHost destination routing
  [`PersonalTrackerNavHost.kt:39`](../../app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt#L39)

- Navigation destination enum and content description mappings
  [`PersonalTrackerDestination.kt:7`](../../app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerDestination.kt#L7)

**Application Baseline & Build Configuration**

- Application manifest declaring Hilt application class and activity intent filters
  [`AndroidManifest.xml:10`](../../app/src/main/AndroidManifest.xml#L10)

- Centralized Gradle version catalog with locked dependencies
  [`libs.versions.toml:1`](../../gradle/libs.versions.toml#L1)

- App module build configuration applying Compose, KSP, and Hilt
  [`app/build.gradle.kts:1`](../../app/build.gradle.kts#L1)

**Screen Skeletons & Verification Tests**

- Home tab destination scaffold integrating design tokens
  [`HomeScreen.kt:22`](../../app/src/main/java/com/personaltracker/ui/screens/HomeScreen.kt#L22)

- Unit test suite verifying tab routes, accessibility descriptions, and icon mappings
  [`NavigationTest.kt:12`](../../app/src/test/java/com/personaltracker/NavigationTest.kt#L12)

