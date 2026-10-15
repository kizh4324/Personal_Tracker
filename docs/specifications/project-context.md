# Personal-Tracker — Project Context & Engineering Standards

> **Canonical System Context Specification**  
> **Target Platform:** Native Android (Min SDK 33, Target SDK 36, Compile SDK 36)  
> **Toolchain Baseline:** Kotlin 2.1.0, JDK 21 (LTS), AGP 8.10.1, Compose BOM 2026.02.00, Hilt 2.58, Room 2.7.0, SQLCipher 4.6.1  
> **Authoritative Spines:** `ARCHITECTURE-SPINE.md`, `_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/DESIGN.md`, `_bmad-output/planning-artifacts/ux-designs/ux-Personal-Tracker-2026-08-15/EXPERIENCE.md`

---

## 1. System Orientation & Core Philosophy

Personal-Tracker is a **local-first, online/offline, network-optional native Android personal productivity application**. It unifies task scheduling, sequential routines, habit tracking, focus study sessions, voice capture, and a non-punishing companion pet economy into an encrypted on-device SQLite database.

### Core Product Principles
1. **Local-First & Offline Privacy (Zero Telemetry):** Every core feature (schedule resolution, routine execution, habit logging, focus timers, companion economy, encrypted backups) functions 100% offline without network connectivity. There are zero tracking SDKs, zero analytics pings, and zero remote database dependencies.
2. **Non-Punitive Psychological Design:** The application never shames, nags, or punishes the user:
   - Streaks never hard-reset to 0 on a single missed day when slack bank days or freeze grace rules apply.
   - The companion pet has zero health points (HP), hunger decay, or character death mechanics.
   - Microcopy is empathetic, calm, and grounded (e.g., *"3 items still open. What would you like to do?"* instead of *"You failed to complete tasks"*).
3. **Strict AI Action Boundary:** AI models (Gemini Live API, SpeechRecognizer, and regex parsers) NEVER directly mutate the database. All captured actions flow through deterministic confidence gates with user confirmation or reversible undo toast windows.

---

## 2. Architectural Invariants (AD-1 through AD-8)

Every developer and AI agent must adhere strictly to the 8 Architectural Decisions defined in `ARCHITECTURE-SPINE.md`:

### AD-1: Strict 4-Layer Dependency Direction & UI Isolation
- **Rule:** Dependencies flow strictly downward:
  $$\text{Presentation Layer (ui/)} \longrightarrow \text{Domain Layer (domain/)} \longrightarrow \text{Data Layer (data/)} \ \& \ \text{Platform Layer (platform/)}$$
- The `domain/` layer is **100% pure Kotlin** (`kotlin.*`, `kotlinx.coroutines.*`, `java.time.*`). It must NEVER import Android framework classes (`android.*`, `androidx.*`).
- Presentation components (`ui/screens/`, `ui/components/`) and ViewModels must NEVER import Room DAOs, Entities, or Android system services directly. All data access must pass through Repository interfaces (`data/repository/`).
- Room DAOs and Entities are strictly `internal` to `:data:local:db`.

### AD-2: Unidirectional Reactive MVI Contract
- **Rule:** Every screen surface must be defined by three immutable contracts:
  1. `UiState`: A single, immutable data class representing the complete screen state.
  2. `UiIntent`: A sealed interface representing user interactions and lifecycle events.
  3. `UiEffect`: A sealed interface representing one-shot transient events (haptics, coin arcs, navigation, snackbars).
- ViewModels expose `StateFlow<UiState>` for persistent rendering and Channel-backed `Flow<UiEffect>` (via `receiveAsFlow()`) for transient effects.
- Composables are stateless; they emit intents through lambdas and must contain zero business or mutation logic.

### AD-3: Pure Kotlin Behavioral Engines with Zero UI Coupling
- **Rule:** All complex business heuristics, state transitions, and scheduling calculations are implemented as stateless or repository-driven pure Kotlin classes inside `domain/engine/`:
  - `AdaptiveScheduleEngine`: Scales routine targets and habit minimums across DayType swaps.
  - `StreakStateMachine`: Computes non-punitive 4-state flame transitions (`COLD` -> `WARMING` -> `BURNING` -> `BLAZING`) with miss-tolerant slack banking and freeze grace.
  - `GamificationLedgerEngine`: Calculates coin payouts, enforces idempotency keys, updates companion energy, and emits reward animation payloads.
  - `FocusRescheduleEngine`: Recalibrates remaining study blocks upon pauses or interruptions without guilt copy.
  - `InterventionRuleEngine`: Evaluates distraction rule cooldowns to generate Just-In-Time Adaptive Intervention (JITAI) nudges.
  - `ResolveDayTypeUseCase`: Implements automatic morning resolution hierarchy: `Date-Specific User Override > Day-of-Week Default`.
- All engine unit tests execute on standard JVM without Robolectric or emulator requirements.

### AD-4: Non-Punitive Behavioral State & Atomic Idempotent Ledgering
- **Rule:** 
  - Missed days must trigger slack-banking/freeze grace recovery rather than hard 0 resets (`StreakStateMachine`).
  - Companion pet progression contains zero HP loss, starvation, or death mechanics.
  - Every coin earning or deduction must be committed to `CoinLedgerEntity` with a deterministic, unique compound `idempotencyKey` (`{DOMAIN}_{ACTION}_{ID}`, e.g., `TASK_COMP_e2b9c7`, `HABIT_LOG_water_2026-10-15`).
  - Duplicate keys must be aborted or ignored via Room `@Insert(onConflict = OnConflictStrategy.ABORT)` within a `@Transaction`. Coin balances are derived exclusively from verified ledger sums.

### AD-5: Two-Tier Hardware Cryptography & Local-First Isolation
- **Rule:**
  - **Tier 1 (Database at Rest):** The entire SQLite database file is encrypted at rest using SQLCipher 4.6.1 with PBKDF2-HMAC-SHA512 (256,000 iterations) and 4096-byte page size. The 32-byte encryption key is generated via `SecureRandom` and persisted inside `pt_db_vault.bin`, encrypted by an Android Keystore master key (`PersonalTracker_MasterDB_Key`, AES-256-GCM, `setUserAuthenticationRequired(false)`).
  - **Tier 2 (Envelope Field Encryption):** High-sensitivity fields (voice capture transcripts, free-form notes) are encrypted via `EncryptedPayload` (12-byte random IV + 128-bit GCM tag) via Room TypeConverters.
  - Zero analytics SDKs, zero remote trackers, 100% offline-capable.

### AD-6: Atomic Cross-Domain Behavioral Transactions
- **Rule:** Any operation mutating multiple entities across domain boundaries (such as completing a routine step, updating streak records, awarding coins, and updating companion experience) must execute inside a Room `@Transaction`. All database access must be dispatched on `Dispatchers.IO` through repository facades.
- Never use `OnConflictStrategy.REPLACE` on entities with cascading foreign keys (use Room `@Upsert` or `@Insert(onConflict = ABORT)`).

### AD-7: Strict Design-Token Consumption & Viewport-Hoisted Motion
- **Rule:** All Jetpack Compose UI elements must strictly resolve dimensions, colors, typography, shapes, and elevations from `com.personaltracker.ui.theme` tokens:
  - Colors: `ColorTokens` (Surfaces, Primary, Secondary, Warm Accent, Companion Green, Semantic, Ink, DayType accents, Coin gold, Streak flames).
  - Typography: `TypographyTokens` (Plus Jakarta Sans for UI text, Inter for tabular numbers).
  - Spacing: `SpacingTokens` (4dp base grid: `Spacing.PointFive` = 2dp, `Spacing.One` = 4dp, `Spacing.Two` = 8dp, `Spacing.Three` = 12dp, `Spacing.Four` = 16dp, etc.).
  - Shapes: `ShapeTokens` (`xs` 4dp, `sm` 8dp, `md` 12dp, `lg` 16dp, `xl` 20dp, `xxl` 24dp, `full` 9999dp).
  - Elevation: `ElevationTokens` (Flat, Low, Medium, High).
  - Animation: `AnimationTokens` (`FastOutSlowIn`, durations 150ms/250ms/300ms/400ms).
- Direct hex colors, hardcoded `dp`, or raw `sp` literals in composables are prohibited.
- Particle and reward animations (`CoinArcAnimation`, milestone celebrations) must be hoisted to `RewardMotionStage` at the root scaffold level to prevent layout bounds clipping.
- Minimum interactive touch target: 48dp floor.

### AD-8: Dual-Engine Voice Capture & AI Action Boundary
- **Rule:** Voice capture operates over a local-first dual-engine architecture:
  - **Online Path:** Gemini Live API over WebSocket for conversational parsing and context.
  - **Offline Path (Default Fallback):** On-device Android `SpeechRecognizer` + deterministic Kotlin regex NLP rule parser (`NaturalLanguageCommandParser`).
  - **Manual Fallback:** Immediate manual text quick-add bar.
- Raw audio buffers must be purged from memory immediately post-transcription.
- 3-tier confidence routing governs action commitment:
  - **High Confidence (>0.85):** Instant commit with a 5-second reversible undo toast.
  - **Medium Confidence (0.50–0.85):** 1-tap confirmation chips presented in `QuickCaptureSheet` (Title, Date, Time, Type, Urgency).
  - **Low Confidence (<0.50):** Saved to `UnfiledCaptureInboxEntity` for manual triage.
- No autonomous destructive action may occur without explicit user confirmation or a reversible grace window.

---

## 3. Toolchain & Build Standards

| Component | Pinned Version | Configuration Rules |
|---|---|---|
| **JDK** | 21 (LTS) | Pinned via `org.gradle.java.home` in `gradle.properties` and module-level `jvmToolchain(21)` in `app/build.gradle.kts`. |
| **Android Gradle Plugin** | 8.10.1 | `minSdk = 33`, `targetSdk = 36`, `compileSdk = 36`. Release builds enforce `isMinifyEnabled = true` with SQLCipher keep rules in `proguard-rules.pro`. |
| **Gradle Wrapper** | 8.11.1 | Pinned with `distributionSha256Sum` verification in `gradle-wrapper.properties`. |
| **Kotlin** | 2.1.0 | 100% Kotlin DSL (`build.gradle.kts`). KAPT is completely prohibited; use KSP only. |
| **KSP** | 2.1.0-1.0.29 | Generates Room and Hilt bindings. |
| **Jetpack Compose BOM** | 2026.02.00 | Material 3 foundation with custom design token wrappers. |
| **Dagger Hilt** | 2.58 | Centralized DI modules (`DatabaseModule`, `RepositoryModule`, `DomainEngineModule`, `PlatformModule`). |
| **Room** | 2.7.0 | Backed by SQLCipher Android 4.6.1 with 16KB native ELF alignment and 4096-byte database page size. |

---

## 4. Where Things Are (Repository Map)

```text
app/src/main/java/com/personaltracker/
├── PersonalTrackerApplication.kt      # Hilt Android Application entry point
├── MainActivity.kt                    # Single Activity hosting AppShellScaffold & NavHost
├── di/                                # Dependency injection modules
│   ├── DatabaseModule.kt              # Room, SQLCipher, KeyStore, and DAO providers
│   ├── RepositoryModule.kt            # Repository interface-to-implementation bindings
│   ├── DomainEngineModule.kt          # Behavioral engine providers
│   └── PlatformModule.kt              # ForegroundService & WorkManager providers
├── domain/                            # PURE KOTLIN BUSINESS LOGIC (Zero Android imports)
│   ├── model/                         # Core domain models (immutable data classes)
│   ├── engine/                        # Behavioral engines (Adaptive, Streak, Ledger, Focus, Intervention)
│   ├── parser/                        # Regex NLP command parser & confidence scorer
│   └── usecase/                       # Interactor use cases (ResolveDayType, CompleteTask, etc.)
├── data/                              # PERSISTENCE & CRYPTOGRAPHY
│   ├── repository/                    # Repository implementations exposing Flow<T> SSOT
│   └── local/
│       ├── db/
│       │   ├── AppDatabase.kt         # Room database with SQLCipher SQLiteDatabaseHook
│       │   ├── converters/            # Room type converters (Date, EncryptedPayload)
│       │   ├── daos/                  # Internal Room DAOs (DayType, Task, Habit, CoinLedger, etc.)
│       │   └── entities/              # Room entities (13 tables)
│       └── security/
│           ├── SecurityKeyStoreManager.kt # Android Keystore AES-256-GCM master key
│           ├── DatabaseKeyProvider.kt     # 32-byte vault key generation & atomic file sync
│           └── EncryptedPayload.kt        # 12-byte IV + GCM envelope field encryption
├── platform/                          # ANDROID OS SYSTEM INTEGRATIONS
│   ├── timer/                         # StudySessionForegroundService
│   ├── worker/                        # DayRolloverWorker (WorkManager periodic midnight sync)
│   ├── alarm/                         # ExactAlarmScheduler (setAlarmClock tiered notifications)
│   └── voice/                         # QuickCaptureVoiceRecorder, SpeechRecognizer & WebSocket clients
└── ui/                                # PRESENTATION & DESIGN SYSTEM
    ├── theme/                         # Authoritative design tokens (Color, Type, Shape, Spacing, Animation)
    ├── navigation/                    # 5-tab NavigationBar & PersonalTrackerNavHost
    ├── components/                    # Reusable Compose components
    │   ├── atoms/                     # Low-level primitives
    │   ├── cards/                     # HeroCard, TaskCard, RoutineCard, HabitCard
    │   ├── overlays/                  # AppShellScaffold, QuickCaptureSheet, RewardMotionStage
    │   ├── animation/                 # CoinArcAnimation, WaveformVisualizer
    │   └── companion/                 # Rive companion stage
    └── screens/                       # MVI Feature Screens (Home, Habits, Study, Companion, Settings)
        ├── home/                      # HomeScreen, HomeViewModel, HomeUiState, HomeUiIntent, HomeUiEffect
        ├── habits/                    # HabitsScreen, HabitsViewModel, HabitsUiState...
        ├── study/                     # StudyScreen, StudyViewModel, StudyUiState...
        ├── companion/                 # CompanionScreen, CompanionViewModel, CompanionUiState...
        └── settings/                  # SettingsScreen, SettingsViewModel, SettingsUiState...
```

---

## 5. Verification Commands & Operational Checklist

Before submitting or reviewing any implementation story:

1. **Unit Tests:**
   ```powershell
   .\gradlew test
   ```
   *Must execute and pass 100% on JVM across all domain engines, DAOs, and MVI ViewModels.*

2. **Android Lint:**
   ```powershell
   .\gradlew lint
   ```
   *Zero blocking errors. Must comply with accessibility minimum touch targets and permission rules.*

3. **Debug Compilation:**
   ```powershell
   .\gradlew assembleDebug
   ```
   *Clean assembly with zero KSP or Hilt processing errors.*

4. **Architecture Spine Mechanical Lint:**
   ```powershell
   python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .
   ```
   *Must report `ok: true` with `total_findings: 0`.*

---

## 6. Known Pitfalls & Anti-Patterns to Avoid

1. **`OnConflictStrategy.REPLACE` Cascade Deletion:** Never use `REPLACE` on Room entities that have foreign-key children (such as `DayTypeEntity` or `HabitEntity`). Replacing a row causes SQLite to DELETE and re-INSERT, cascading deletions to completion logs and routines. Standardize on Room `@Upsert` or `@Insert(onConflict = OnConflictStrategy.ABORT)`.
2. **Hardcoded UI Literals:** Never write `Color(0xFF...)`, `16.dp`, or `14.sp` inside screen composables. Always resolve values through `PersonalTrackerTheme` and `ui/theme/` tokens.
3. **Android Framework Imports in Domain:** Never import `android.content.Context`, `android.os.*`, or `androidx.compose.*` in `domain/`. Domain engines and use cases must remain pure Kotlin.
4. **Unpinned Gradle Daemon JVM:** Never execute Gradle commands without ensuring `JAVA_HOME` or `org.gradle.java.home` points to JDK 21. Java 26 on PATH will break Gradle 8.11.1 tooling model syncs.
5. **Direct DB Mutations in ViewModels:** ViewModels must never inject DAOs directly. All persistence calls must traverse Repository interfaces to preserve caching, encryption, and transactional boundaries.
6. **Microcopy Guilt Traps:** Never use guilt language, punitive streak resets, or alarmist copy anywhere in notification or UI strings. Always follow the microcopy guidelines in `EXPERIENCE.md`.
