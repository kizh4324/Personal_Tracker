# TECHSTACK.md — Personal-Tracker v1

> **Document Role:** Architectural Technical Stack Specification & Machine-Readable Implementation Contract  
> **Persona:** BMad System Architect (Winston)  
> **Platform:** Native Android (Kotlin / Jetpack Compose)  
> **Scope:** v1, Single-User, Personal-Use, Local-First, 100% Offline-Capable  
> **Source of Truth:** Aligned 1:1 with `PROBLEM_AND_SOLUTION_BRIEF.md`, `PROJECT_MASTER_OUTLINE.md`, and `design-system.md`.

---

## 0. Document Metadata & Status Tag Legend

This document uses a strict eight-tag status taxonomy to distinguish frozen product requirements from implementation recommendations and architectural decisions. Tags may combine (e.g., `[CONFIRMED / TECHNICAL CONSTRAINT]`) where a fixed requirement is bounded by Android platform behavior.

| Status Tag | Operational Definition for Developers & Coding Agents |
|---|---|
| **`[CONFIRMED]`** | Mandatory requirement frozen by the approved PRD scope. Must not be altered or omitted. |
| **`[RECOMMENDED]`** | Selected engineering technology or architectural pattern matching modern Android standards. Follow unless an explicit architectural override is logged. |
| **`[OPTIONAL]`** | Supplementary capability that may be integrated without introducing a core v1 dependency. |
| **`[OPEN DECISION]`** | Requires an explicit product or architecture resolution before implementation. Traced to PRD requirements. |
| **`[TECHNICAL CONSTRAINT]`** | Hard boundary imposed by Android OS platform rules, hardware compatibility, or permission models. |
| **`[ARCHITECTURE PHASE]`** | Implementation detail intentionally left flexible during PRD authoring, to be resolved during technical architecture design. |
| **`[DEFERRED]`** | Out of v1 scope. Architectural hooks may exist, but zero v1 code or dependencies may rely on it. |
| **`[EXCLUDED]`** | Ruled out entirely by explicit product decision (see Non-Goals). Must not be introduced. |

---

## 1. Executive Stack Decision Matrix

| Layer / Subsystem | Selected Technology | Status Tag | Architectural Rationale |
|---|---|---|---|
| **Target Platform** | Native Android | `[CONFIRMED]` | Single-platform Android focus; no multiplatform overhead |
| **Language** | Kotlin 2.4.x | `[RECOMMENDED]` | Official Android language, coroutines, type safety |
| **Build & Toolchain** | Gradle Kotlin DSL + AGP + KSP | `[RECOMMENDED]` | Version catalog centralization, modern annotation processing |
| **UI Toolkit** | Jetpack Compose + Material 3 | `[RECOMMENDED]` | Declarative UI, dynamic theming, smooth animation engine |
| **UI State Architecture** | ViewModel + StateFlow + UDF | `[RECOMMENDED]` | Unidirectional Data Flow, lifecycle-aware state holders |
| **App Architecture** | Layered Architecture + Repository Pattern | `[RECOMMENDED]` | Strict boundary separation: UI $\leftrightarrow$ Domain $\leftrightarrow$ Data |
| **Domain Logic** | Selective Use Cases / Domain Services | `[RECOMMENDED]` | Encapsulates complex rules (rescheduling, rewards, day types) |
| **Dependency Injection** | Hilt (Dagger) | `[RECOMMENDED]` | Compile-time dependency graph, Android lifecycle integration |
| **Primary Database** | Room over SQLite | `[CONFIRMED]` | Local-first relational storage, type-safe compile-time DAOs |
| **Database Encryption** | SQLCipher for Android (`sqlcipher-android`) | `[CONFIRMED]` | Full database encryption at rest with 16KB page support |
| **Key Management** | Android Keystore System | `[CONFIRMED]` | Hardware-backed cryptographic key isolation |
| **Lightweight Preferences** | Jetpack DataStore (Preferences) | `[RECOMMENDED]` | Non-relational key-value settings, async Flow API |
| **Asynchronous Engine** | Kotlin Coroutines | `[RECOMMENDED]` | Structured concurrency, non-blocking disk/CPU execution |
| **Reactive Streams** | Kotlin Flow / StateFlow | `[RECOMMENDED]` | Cold and hot reactive streams from DB to UI |
| **Screen Navigation** | Navigation Compose | `[RECOMMENDED]` | Type-safe declarative destination routing |
| **Standard Notifications** | Android Notification Channels | `[CONFIRMED]` | Tiered delivery: Routine, Important, Urgent |
| **Exact Reminders** | Android `AlarmManager` | `[CONFIRMED / TECHNICAL CONSTRAINT]` | Precise user-facing alerts with fallback handling |
| **Deferred Background Work** | Jetpack `WorkManager` | `[CONFIRMED]` | Guaranteed execution for maintenance, cleanup, and backups |
| **Distraction Intervention** | `AccessibilityService` (User Opt-in) | `[CONFIRMED / TECHNICAL CONSTRAINT]` | Soft/hard intervention on target distraction apps |
| **Usage Analytics** | Android `UsageStatsManager` | `[CONFIRMED]` | Historical screen time analytics (read-only audit) |
| **Voice Capture** | Android `SpeechRecognizer` (On-Device) | `[CONFIRMED / ARCHITECTURE PHASE]` | Offline-first speech-to-text with manual fallback |
| **Natural Language Parser**| Deterministic Kotlin Parser | `[CONFIRMED]` | 100% offline schema validation and attribute extraction |
| **Data Serialization** | `kotlinx.serialization` | `[RECOMMENDED]` | Compile-time JSON parsing and backup encoding |
| **Backup & Restore** | Encrypted `.ptbackup` Container | `[CONFIRMED]` | User-owned, password-protected local backup export/import |
| **Mascot Animation** | Rive Android Runtime (`.riv`) | `[CONFIRMED]` | 7-state interactive state machine (`companion_sm`) |
| **One-Shot Animations** | Lottie Compose + Hand-built Compose | `[RECOMMENDED]` | Celebration reveals + VSYNC Bézier coin arc |
| **Test Suite** | JUnit + Compose UI Test + Turbine + Real-Device | `[RECOMMENDED]` | Unit, stream, UI, and instrumented system testing |

---

## 2. Scope Boundaries: What Is Intentionally Excluded

To prevent architectural drift and scope creep, the following technologies and patterns are explicitly excluded or deferred from v1.

| Technology / Pattern | Status Tag | Decision & Rationale | PRD / Outline Trace |
|---|---|---|---|
| **Supabase / PostgreSQL** | `[DEFERRED]` | **Zero cloud database.** v1 is 100% local-first; Room is the sole source of truth. | PRD §1.6 Hard Constraints |
| **Cloud Synchronization** | `[DEFERRED]` | No multi-device sync or remote conflict resolution in v1. | PRD §1.6, Master Outline §2.5 |
| **User Authentication / Accounts** | `[DEFERRED]` | Single-user personal application; no login, email auth, or OAuth. | Master Outline §2.5 |
| **Cloud Telemetry & Analytics** | `[DEFERRED]` | No Firebase Analytics, Mixpanel, or remote crash reporting. 100% private. | PRD §1.6, Tech Stack §16.2 |
| **Cloud Storage** | `[DEFERRED]` | Backups are local `.ptbackup` files saved to user-selected device storage. | PRD §4.9 |
| **Always-Listening Wake Word** | `[EXCLUDED]` | Battery drain and privacy risk. Voice is tap-to-talk via the UI FAB. | PRD §2.5 Non-Goals |
| **Cross-Platform Frameworks** | `[EXCLUDED]` | No Flutter, React Native, or Web frontend. Native Android Jetpack Compose only. | PRD §1.6, Tech Stack §3.1 |
| **Remote HTTP Clients (Retrofit / Ktor)** | `[DEFERRED]` | No REST API endpoints exist in the core v1 product. | Tech Stack §22 |
| **Jetpack Paging (`androidx.paging`)** | `[OPTIONAL]` | Dataset is personal-scale (<5,000 items); standard Room Flow lists are optimal. | Tech Stack §23 |
| **Social Leagues / Leaderboards** | `[EXCLUDED]` | Single-user wellness product; social comparison induces anxiety. | PRD §2.5, Brief §266 |
| **Punitive Gamification (HP Loss / Hard Resets)** | `[EXCLUDED]` | Habitica-style damage and Duolingo-style zero resets cause guilt and churn. | Brief §240, PRD §2.5 |

---

## 3. Architecture Dependency Map & Core Data Flow

### 3.1 Structural System Hierarchy

```text
                               ┌────────────────────────────────────────┐
                               │          PERSONAL-TRACKER v1           │
                               │        Native Android / Kotlin         │
                               └───────────────────┬────────────────────┘
                                                   │
                               ┌───────────────────┴────────────────────┐
                               │         Presentation Layer             │
                               │  Jetpack Compose + Material 3 + Theme  │
                               └───────────────────┬────────────────────┘
                                                   │
                               ┌───────────────────┴────────────────────┐
                               │           UI State Layer               │
                               │     ViewModel + UDF + StateFlow        │
                               └─────────┬────────────────────┬─────────┘
                                         │                    │
                    ┌────────────────────┴─────┐              │
                    │   Domain Layer (Rules)   │              │
                    │  Selective Use Cases     │              │
                    └────────────┬─────────────┘              │
                                 │                            │
                    ┌────────────┴────────────────────────────┴─────────┐
                    │               Repository Pattern                  │
                    │    Single Boundary for Business Data Access       │
                    └────────────┬────────────────────────────┬─────────┘
                                 │                            │
             ┌───────────────────┴──────────┐   ┌─────────────┴─────────┐
             │     Local Storage Engine     │   │   Android Framework   │
             ├──────────────────────────────┤   ├───────────────────────┤
             │ • Room (SQLite)              │   │ • Notification API    │
             │ • SQLCipher Encryption       │   │ • AlarmManager (Exact)│
             │ • Jetpack DataStore (Prefs)  │   │ • WorkManager (Defer) │
             │ • Android Keystore (Keys)    │   │ • AccessibilityService│
             │ • .ptbackup File I/O         │   │ • UsageStatsManager   │
             │ • kotlinx.serialization      │   │ • SpeechRecognizer    │
             └──────────────────────────────┘   └───────────────────────┘
```

### 3.2 Core Data Ownership & Invariants
1. **Room + SQLCipher is the Primary and Only Source of Truth**: There are no remote replicas, no background sync jobs, and no conflicting data sources.
2. **Unidirectional Data Access**: UI Composables $\rightarrow$ ViewModels $\rightarrow$ Repositories $\rightarrow$ Room DAOs. UI components *never* access DAOs, preferences, or disk storage directly.
3. **Immutability**: Data emissions from repositories to ViewModels to UI are strictly immutable Kotlin `StateFlow` streams.

---

## 4. Consolidated Machine-Readable Dependency Manifest

A future coding agent or developer can generate the project's `gradle/libs.versions.toml` directly from this manifest.

```toml
# ==============================================================================
# GRADLE VERSION CATALOG MANIFEST (libs.versions.toml)
# Sourced 1:1 from TECHSTACK.md specifications
# ==============================================================================

[versions]
# Toolchain & Kotlin
kotlin = "2.4.0"                   # [RECOMMENDED] Latest stable 2.4.x line
agp = "8.8.0"                      # [RECOMMENDED] Android Gradle Plugin
ksp = "2.4.0-1.0.30"               # [RECOMMENDED] Kotlin Symbol Processing matching Kotlin version

# AndroidX Core & Lifecycle
coreKtx = "1.15.0"                 # [RECOMMENDED] Core Kotlin extensions
lifecycle = "2.8.7"                # [RECOMMENDED] ViewModel, Runtime Compose, StateFlow

# Jetpack Compose & UI
composeBom = "2026.02.00"          # [RECOMMENDED] Compose Bill of Materials (or latest stable BOM)
activityCompose = "1.10.0"         # [RECOMMENDED] ComponentActivity Compose integration
navigationCompose = "2.8.7"        # [RECOMMENDED] Type-safe Navigation Compose

# Dependency Injection
hilt = "2.55"                      # [RECOMMENDED] Google Hilt Dependency Injection
hiltNavigationCompose = "1.2.0"    # [RECOMMENDED] Hilt ViewModel injection in Navigation Compose

# Local Database & Storage
room = "2.7.0"                     # [CONFIRMED] Room database abstraction (KSP supported)
sqlcipher = "4.6.1"                # [CONFIRMED] net.zetetic:sqlcipher-android (16KB page support)
sqliteBundled = "2.5.0"            # [CONFIRMED] AndroidX SQLite support for Room
datastore = "1.1.2"                # [RECOMMENDED] Jetpack DataStore Preferences

# Background Work & System Services
workManager = "2.10.0"             # [CONFIRMED] Deferrable persistent background tasks

# Serialization & Cryptography
kotlinxSerialization = "1.8.0"     # [RECOMMENDED] JSON & binary backup serialization
securityCrypto = "1.1.0-alpha06"   # [CONFIRMED] Android Keystore encryption wrapper

# Animation & Media
rive = "9.1.0"                     # [CONFIRMED] Rive Android Runtime (companion_sm state machine)
lottie = "6.6.2"                   # [OPTIONAL / RECOMMENDED] Lottie Compose for reward bursts

# Optional Cloud AI (Deferred)
firebaseBom = "33.9.0"             # [OPTIONAL / DEFERRED] Firebase Bill of Materials
firebaseAi = "16.0.0-beta01"       # [OPTIONAL / DEFERRED] Firebase AI Logic for Gemini

# Testing
junit = "4.13.2"                   # [RECOMMENDED] Unit testing engine
turbine = "1.2.0"                  # [RECOMMENDED] Flow & StateFlow assertion utility
coroutinesTest = "1.10.1"          # [RECOMMENDED] Standard test dispatchers and scopes
androidxTestExt = "1.2.1"          # [RECOMMENDED] AndroidJUnit4 runner
espresso = "3.6.1"                 # [RECOMMENDED] Android instrumentation tests

[libraries]
# Kotlin & Coroutines
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutinesTest" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutinesTest" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# AndroidX Core & Compose
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-animation = { group = "androidx.compose.animation", name = "animation" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# Lifecycle & ViewModel
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Hilt Dependency Injection
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Database & Storage
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
sqlcipher-android = { group = "net.zetetic", name = "sqlcipher-android", version.ref = "sqlcipher" }
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqliteBundled" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Security & Background Work
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }

# Animation Runtimes
rive-android = { group = "app.rive", name = "rive-android", version.ref = "rive" }
lottie-compose = { group = "com.airbnb.android", name = "lottie-compose", version.ref = "lottie" }

# Testing Libraries
junit = { group = "junit", name = "junit", version.ref = "junit" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

---

## 5. Platform, Language & Toolchain

### 5.1 Operating System & Platform Boundary
* **`[CONFIRMED]` Native Android Platform**: Targeted strictly for Android mobile devices. No iOS, Web, Desktop, or hybrid runtimes exist in v1.
* **`[RECOMMENDED]` Kotlin 2.4.x**: Primary language across all application layers. Python is not part of the Android build; it is reserved for external scripts or prompt tooling only.

### 5.2 Build System & IDE Strategy
* **`[RECOMMENDED]` Android Studio**: Use the latest stable Android Studio release (e.g., Quail stable line) at implementation kickoff.
* **`[RECOMMENDED]` Gradle Kotlin DSL**: Build configuration uses `build.gradle.kts`, `settings.gradle.kts`, and `gradle/libs.versions.toml`. Hardcoded library strings in module build files are prohibited.
* **`[RECOMMENDED]` KSP (Kotlin Symbol Processing)**: Primary annotation processor for Room and Hilt. KAPT is prohibited unless an unavoidable transitive dependency requires it.

### 5.3 Android SDK Version Strategy
* **`[RECOMMENDED]` Implementation Baseline**:
  * `compileSdk`: Latest stable Android SDK available at bootstrap.
  * `targetSdk`: Latest stable target API supported by Android Studio / AGP.
  * `minSdk`: **API 33 (Android 13)**.
* **`[ARCHITECTURE PHASE]` SDK Compatibility Validation Gate**:  
  *(Traced to Decision §1.1 and PRD §1.6 Hard Constraints)*  
  The selected `minSdk`/`targetSdk` combination must be validated during Phase 1 against:
  1. `AccessibilityService` event delivery and permission survival across reboots.
  2. `AlarmManager.canScheduleExactAlarms()` behavior and `SCHEDULE_EXACT_ALARM` permissions.
  3. `sqlcipher-android` native `.so` binary loading on **16KB page-size Android hardware**.
  4. `SpeechRecognizer.createOnDeviceSpeechRecognizer()` availability across target devices.

---

## 6. Architecture, State & Dependency Injection

### 6.1 Architectural Pattern
* **`[RECOMMENDED]` Layered Architecture with UDF**:
  ```text
  Presentation (Composables)
       ↓ (User Actions / Events)
  ViewModel (State Holders)
       ↓ (Executes Business Logic)
  Domain Use Cases (Selective: Rescheduling, DayType, Coins, Urgency)
       ↓ (Requests Data Streams)
  Repository Layer (Single Source of Data Orchestration)
       ↓ (Reads / Writes)
  Data Sources (Room DAOs, DataStore, Android System APIs)
  ```

### 6.2 Domain Layer Guidelines
* **`[RECOMMENDED]` Selective Use Cases**: Introduce domain use-case classes only where multi-repository orchestration or non-trivial business algorithms exist:
  * `ResolveDayTypeUseCase` (merges recurring templates with calendar date overrides).
  * `RescheduleTasksUseCase` (shifts soft-deadline tasks forward upon unavailability).
  * `AwardCoinsUseCase` (calculates stake-scaled Coin rewards and milestone triggers).
  * `EvaluateInterventionPolicyUseCase` (determines soft breathing vs. hard block intervention).
  * `ClassifyUrgentNotificationUseCase` (enforces high-priority heads-up rules).
* Simple single-entity CRUD operations route directly from ViewModel to Repository.

### 6.3 Asynchronous Execution & State Management
* **`[RECOMMENDED]` Kotlin Coroutines**: Structured concurrency for all background, disk, parsing, and system coordination tasks.
* **`[RECOMMENDED]` Flow & StateFlow**: Repositories expose cold `Flow<T>` from Room DAOs. ViewModels convert these to hot `StateFlow<UIState>` using `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialState)`.
* **`[RECOMMENDED]` Dependency Injection with Hilt**: Hilt manages singletons and lifecycle-scoped instances across `@AndroidEntryPoint` activities, ViewModels (`@HiltViewModel`), and background workers (`@HiltWorker`).

---

## 7. Presentation & UI Layer

### 7.1 Jetpack Compose & Material 3
* **`[RECOMMENDED]` Jetpack Compose**: Primary declarative UI framework. Eliminates XML layouts, fragments, and data-binding adapters.
* **`[RECOMMENDED]` Material 3 Foundation**: Baseline component behavior (surfaces, buttons, sheets, dialogs) derived from Material 3 and styled 1:1 with [`design-system.md`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/design-system.md) via [`Theme.kt`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/app/src/main/java/com/personaltracker/ui/theme/Theme.kt).

### 7.2 Navigation Architecture
* **`[RECOMMENDED]` Navigation Compose**: Type-safe destination routing using Kotlin `@Serializable` route objects. Destinations include:
  * `HomeTimelineRoute` (DayType schedule & persistent bottom sheet)
  * `TasksRoute` (Action Priority Matrix)
  * `HabitsRoute` (Streaks & consistency heatmap)
  * `FocusRoute` (Deep work timer & intervention rules)
  * `CompanionRoute` (Pet status, adventure logs & accessories shop)
  * `SettingsRoute` & `BackupRestoreRoute`

### 7.3 Motion, Animation & Mascot Delivery Pipeline
* **`[RECOMMENDED]` Compose Animation Engine**: Native Compose APIs (`AnimatedVisibility`, `animateFloatAsState`, `Animatable`) handle 90% of UI transitions (card expansions, progress fills, list item checks).
* **`[CONFIRMED]` Hand-Built VSYNC Coin Arc Motion**:  
  Implemented in [`CoinArcAnimation.kt`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/app/src/main/java/com/personaltracker/ui/components/animation/CoinArcAnimation.kt). Uses `withFrameNanos` to calculate quadratic Bézier flight paths based on monotonic elapsed time, ensuring uniform 600ms duration across 60Hz, 90Hz, and 120Hz displays.
* **`[CONFIRMED]` Rive Mascot State Machine**:  
  Implemented in [`CompanionView.kt`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/app/src/main/java/com/personaltracker/ui/components/companion/CompanionView.kt). Uses `app.rive:rive-android` to bind the 7-state `CompanionState` enum (`Idle=0` to `Excited=6`) to the `state` input on `companion_sm`.
* **`[OPTIONAL / RECOMMENDED]` Lottie Compose**:  
  Implemented in [`RewardAnimation.kt`](file:///C:/Users/User/OneDrive/Music/Personal-Tracker/app/src/main/java/com/personaltracker/ui/components/animation/RewardAnimation.kt) for one-shot milestone celebration bursts (`milestone_full_reveal.json`) and micro coin glows (`coin_earn_micro.json`).

---

## 8. Local Storage & Security

### 8.1 Primary Database (Room over SQLite)
* **`[CONFIRMED]` Room Database**: Sole local relational data store. Stores entities for DayTypes, Tasks, Habits, Routines, Study Sessions, Focus Sessions, Coins Ledger, and History Logs.

### 8.2 Database Encryption (SQLCipher for Android)
* **`[CONFIRMED]` SQLCipher Engine**: Uses `net.zetetic:sqlcipher-android` integrated via AndroidX SQLite factory (`SupportFactory`).
* **`[TECHNICAL CONSTRAINT]` 16KB Page Size Compatibility**:  
  The legacy `android-database-sqlcipher` artifact is **prohibited** because it does not support 16KB memory page sizes required by modern Android hardware. The project strictly requires `net.zetetic:sqlcipher-android`.
* **`[ARCHITECTURE PHASE]` Key Derivation & Cipher Parameters**:  
  *(Traced to PRD §4.8 Security & Privacy / Decision §16.1)*  
  The implementation must finalize:
  1. SQLCipher KDF iteration count and memory parameters.
  2. Master key generation via `AndroidKeyStore` (`AES-256-GCM`).
  3. Key wrapping mechanism for database passphrase storage.
  4. Migration procedure for schema updates under encrypted SQLite.

### 8.3 Lightweight Preferences (Jetpack DataStore)
* **`[RECOMMENDED]` Preferences DataStore**: Stores non-relational settings (active theme mode, notification sound toggles, onboarding flag, speech recognition preferences). Relational data is strictly prohibited in DataStore.

---

## 9. System Services & Background Execution

### 9.1 Notification Delivery Engine
* **`[CONFIRMED]` Android Notification Channels**: Three distinct notification channels:
  1. `Channel_Routine` (Low/Default importance, standard chimes).
  2. `Channel_Important` (High importance, heads-up display).
  3. `Channel_Urgent` (Max importance, bypasses DND where permitted, distinct companion alert sound).

### 9.2 Exact Reminders & Alarms
* **`[CONFIRMED / TECHNICAL CONSTRAINT]` `AlarmManager`**:
  * Exact alarms (`setExactAndAllowWhileIdle()`) are reserved strictly for time-critical items (urgent meetings, scheduled routine starts).
  * The application must verify `AlarmManager.canScheduleExactAlarms()` at runtime and provide a fallback warning flow if permission is revoked.

### 9.3 Deferrable Background Work
* **`[CONFIRMED]` Jetpack `WorkManager`**: Manages non-exact maintenance tasks:
  * Midnight DayType schedule generation.
  * Routine consistency heatmap history compilation.
  * Temporary cache cleanup and automated local backup rotation.

### 9.4 Focus & Distraction Intervention Engine
* **`[CONFIRMED / TECHNICAL CONSTRAINT]` `AccessibilityService`**:
  * Selected mechanism for real-time app intervention (detecting target distracting packages).
  * **Hard Security Constraints**: Requires explicit user settings enablement; collects zero keystrokes/passwords/OTPs; discards window text; immediately whitelists emergency/system dialer packages.
  * **Expectation Management**: The app never promises unbypassable blocking; OEM task-killers or user revocation must be handled gracefully.
* **`[CONFIRMED]` `UsageStatsManager`**: Read-only background query API for compiling daily/weekly historical screen time analytics.

---

## 10. Voice Capture, NLP & Serialization

### 10.1 Speech-to-Text Pipeline
* **`[CONFIRMED / ARCHITECTURE PHASE]` Android `SpeechRecognizer`**:  
  *(Traced to PRD §2.4 Voice Task Assignment / Problem Statement 9)*
  * Primary voice interface initiated via the UI Waveform FAB.
  * Evaluates `createOnDeviceSpeechRecognizer()` first. If on-device models are missing, uses system speech recognition or falls back gracefully to standard manual text input.
  * Raw audio recordings are processed in-memory and immediately destroyed.

### 10.2 Natural Language Parsing Engine
* **`[CONFIRMED]` Deterministic Kotlin Parser**:
  * 100% offline, regex and rule-based token extractor.
  * Extracts: Title, Date, Time, Duration, Item Type (Task/Habit/Routine), Priority Tier (P1–P4), and Subject.
  * Emits strongly-typed data classes validated against business schemas before reaching Repositories.
* **`[RECOMMENDED]` `kotlinx.serialization`**: Standard JSON engine for parser output validation and backup encoding.

---

## 11. Security, Backup & Local-First Boundary

### 11.1 Key Management Boundary
* **`[CONFIRMED]` Android Keystore**: Cryptographic boundary for master keys. Encryption keys are never hardcoded, never logged, and never transmitted over any network.

### 11.2 Encrypted Backup & Restore (`.ptbackup`)
* **`[CONFIRMED]` Backup Archive Pipeline**:
  ```text
  Room Database State
        ↓
  Validated Domain Export DTO
        ↓
  kotlinx.serialization (JSON payload)
        ↓
  AES-GCM-256 Encryption (User Password + Salt via PBKDF2 / Argon2)
        ↓
  Encrypted .ptbackup File (Saved to User-Selected Document Storage)
  ```
* **`[ARCHITECTURE PHASE]` Restore Verification & Rollback Protocol**:  
  *(Traced to PRD §4.9 Offline Resilience / Backup Design)*
  * The restore engine must verify password and HMAC/GCM authentication tags before modifying local storage.
  * A temporary safety snapshot of the existing database is retained until the restored database passes full schema integrity validation.

---

## 12. AI Layer Boundaries

### 12.1 Core Principle: AI Is Optional
* **`[CONFIRMED]` Network & AI Independence**: The application is 100% functional without internet connectivity, API keys, or cloud models.

### 12.2 Deferred Cloud AI (Firebase AI Logic)
* **`[OPTIONAL / DEFERRED]` Firebase AI Logic / Gemini SDK**:  
  *(Traced to PRD §1.5 and Decision §1.1)*
  * If cloud AI is enabled in a future release, it must route through Firebase AI Logic with **App Check attestation**. Raw Gemini API keys must never be packaged into the client APK.
* **`[ARCHITECTURE PHASE]` App Check Debug Provider Setup**:  
  *(Verified July 2026 update)*: Firebase AI Logic console enforces App Check. Local debug builds will require the debug attestation provider configured to test cloud AI calls.

### 12.3 Real-Time Voice Models (Gemini Live API)
* **`[DEFERRED]` Gemini Live API**: Excluded from v1. Bidirectional streaming WebSocket sessions are not required for single-phrase quick capture.

---

## 13. Testing & Verification Strategy

### 13.1 Automated Test Pyramid
* **`[RECOMMENDED]` Unit Tests (`JUnit`)**: Validates domain use cases, scheduling math, Coin reward calculations, streak freeze logic, parser tokenization, and schema migration logic.
* **`[RECOMMENDED]` Reactive Stream Tests (`Turbine`)**: Asserts emissions, buffer states, and debouncing across Repository and ViewModel `StateFlow` pipelines.
* **`[RECOMMENDED]` UI & Component Tests (`Compose UI Test`)**: Validates Hero Card states, bottom sheet detents, quick-capture previews, and theme switching in isolated composable harnesses.

### 13.2 Real-Device & Instrumented Verification
* **`[CONFIRMED]` Mandatory Instrumented Verification Gate**:  
  The following subsystems cannot be validated solely via unit tests and require real Android device or emulator execution:
  1. `AccessibilityService` event interception and foreground window tracking.
  2. `AlarmManager` exact alarm firing from Doze mode.
  3. `SpeechRecognizer` audio capture and on-device transcription accuracy.
  4. Database loading on physical 16KB memory page hardware.
  5. 60Hz vs. 120Hz VSYNC frame rendering for `CoinArcAnimation.kt`.

---

## 14. Core Engineering Invariants

Every engineer or AI agent writing code for Personal-Tracker must uphold these twelve invariants:

1. **Local-First is Reality**: The app never assumes network access exists.
2. **Room is Sole Source of Truth**: No remote replicas or competing caches.
3. **UI Never Writes to Storage**: All mutations flow strictly through ViewModels and Repositories.
4. **Composables Are Clean**: Business logic and calculations remain in Use Cases and ViewModels.
5. **AI Never Directly Mutates Data**: Parser output is staged in preview models for validation before persisting.
6. **Voice Always Has Text Fallback**: Speech failures degrade gracefully to manual keyboard entry.
7. **System Services Are Constraints**: OS permissions and OEM power management are handled defensively.
8. **Exact Alarms Are Rare**: Reserved exclusively for genuinely urgent user-facing alerts.
9. **AccessibilityService is Ethical & Minimal**: Zero telemetry, zero keystroke logging, explicit opt-in.
10. **Cloud AI is Optional**: Core app never blocks on network or model quotas.
11. **Cryptographic Integrity First**: Master keys reside exclusively in Android Keystore.
12. **Scope Discipline**: Zero implementation of deferred or excluded features without an updated PRD.

---

## 15. Open Decisions & Architecture Phase Traceability Matrix

| Item | Status Tag | PRD / Outline Trace | Open Question / Technical Context |
|---|---|---|---|
| **SQLCipher KDF & Cipher Parameters** | `[ARCHITECTURE PHASE]` | PRD §4.8 / Tech Stack §8.2 | Determine exact PBKDF2 iteration count, memory cache size, and Keystore key-wrapping mechanism. |
| **SDK min/target/compile Matrix** | `[ARCHITECTURE PHASE]` | PRD §1.6 / Tech Stack §5.3 | Validate API 33+ compatibility against `AccessibilityService`, `AlarmManager`, and 16KB page hardware. |
| **SpeechRecognizer Device Availability** | `[ARCHITECTURE PHASE]` | PRD §2.4 / Tech Stack §10.1 | Determine fallback behavior when on-device speech language packs are not installed. |
| **Backup KDF & Integrity Validation** | `[ARCHITECTURE PHASE]` | PRD §4.9 / Tech Stack §11.2 | Select cryptographic algorithm (e.g. AES-GCM-256 with Argon2/PBKDF2) and transactional rollback model. |
| **Firebase App Check Debug Provider** | `[ARCHITECTURE PHASE]` | PRD §1.5 / Tech Stack §12.2 | Configure local debug provider tokens if optional cloud AI integration is tested during development. |
| **Subject-Level Study Breakdown** | `[OPEN DECISION]` | PRD §2.7 / Master Outline §2.7 | Confirm whether study analytics requires pie-chart breakdown by subject or simple hours-logged tracking. |
| **Break Cadence Configuration** | `[OPEN DECISION]` | PRD §2.7 / Master Outline §2.7 | Resolve whether mandatory break intervals are fixed at 50 min or configurable per session. |
| **Bad-Habit Substitute Activity Pool** | `[OPEN DECISION]` | PRD §2.7 / Master Outline §2.7 | Decide whether replacement activities are pre-selected by user or generated dynamically from a catalog. |
| **Companion Species / Selection** | `[OPEN DECISION]` | PRD §2.7 / Master Outline §2.7 | Decide between a single fixed companion character vs. user-selectable options during onboarding. |

---

## 16. Developer & Coding Agent Handbook ("How to Use This Document")

If you are a human software engineer, Developer agent (Amelia), Codex, or another AI model picking up this project cold:

### 1. What to Read First
1. Start with **§1 (Executive Stack Decision Matrix)** and **§2 (Scope Boundaries)** to understand the exact technical constraints and non-goals.
2. Read **§4 (Consolidated Machine-Readable Dependency Manifest)** to verify or generate `gradle/libs.versions.toml`.
3. Check **§14 (Core Engineering Invariants)** before writing any Kotlin or Compose code.

### 2. What Is Frozen vs. What Is Open
* **FROZEN (`[CONFIRMED]`, `[EXCLUDED]`)**: Do not propose architectural changes, cloud synchronization, alternate databases, or cross-platform rewrites. Implement exactly as specified.
* **FROZEN RECOMMENDATIONS (`[RECOMMENDED]`)**: Follow the specified libraries (Hilt, Navigation Compose, DataStore, Turbine).
* **OPEN FOR RESOLUTION (`[ARCHITECTURE PHASE]`, `[OPEN DECISION]`)**: Consult **§15 (Traceability Matrix)** before finalizing encryption parameters, backup file formats, or speech fallback mechanisms.

### 3. How to Log New Architectural Decisions
If an unforeseen Android platform constraint or library conflict arises during implementation:
1. Log the decision in `PROJECT_MASTER_OUTLINE.md` under **Appendix A (Decision Log)**.
2. Update the status tag in this document if a recommendation or phase item is resolved.
3. Never silently add a network dependency or cloud service without updating the project status tags.
