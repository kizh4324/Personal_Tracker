# Harness² Experience Library — Personal-Tracker

Authoritative repository of verified engineering heuristics, operational invariants, hard-won debugging lessons, and anti-patterns for autonomous agents operating in `Personal-Tracker`.

---

## 1. Toolchain & Runtime Environment

### EXP-001: Pinned JDK 21 Daemon Execution
* **Context:** Gradle build execution and Android Gradle Plugin (AGP) model synchronization.
* **Heuristic:** The Gradle daemon and build scripts must execute under JDK 21 (Eclipse Temurin 21.0.6). Java 26 (or newer system default JDKs on PATH) causes `The supplied phased action failed with an exception. 26` due to Gradle 8.11 / AGP 8.8 bytecode tooling incompatibility.
* **Rule:** Always verify `JAVA_HOME` or `org.gradle.java.home` points to JDK 21 before invoking Gradle commands.

### EXP-002: KSP Exclusivity (Zero KAPT)
* **Context:** Room and Dagger Hilt code generation.
* **Heuristic:** KAPT is prohibited across all modules. All code generation must be performed strictly using KSP (`com.google.devtools.ksp`).
* **Rule:** Never apply `kotlin-kapt` or configure `kapt(...)` dependencies in `app/build.gradle.kts`. Use `ksp(...)` configurations exclusively.

---

## 2. Persistence & Cryptography (Room / SQLCipher)

### EXP-003: Cascade Deletion Hazard with `OnConflictStrategy.REPLACE`
* **Context:** Room entity insertions and updates for tables with child foreign keys (e.g., `DayTypeEntity` -> `DayOverrideEntity`, `HabitEntity` -> `HabitLogEntity`).
* **Heuristic:** Room's `@Insert(onConflict = OnConflictStrategy.REPLACE)` executes an SQLite `DELETE` followed by `INSERT` under the hood. On tables with cascading foreign keys (`onDelete = ForeignKey.CASCADE`), this silently purges child completion logs and historical streaks.
* **Rule:** Standardize on Room `@Upsert` or `@Insert(onConflict = OnConflictStrategy.ABORT)` within `@Transaction` blocks on `Dispatchers.IO`.

### EXP-004: Multi-Entity Cross-Domain Transaction Safety
* **Context:** Actions modifying state across domain boundaries (e.g., completing tasks/habits, updating streak state machine, recording coin transactions, awarding companion energy).
* **Heuristic:** Dispersed mutations without atomic transaction management lead to database desynchronization and orphaned coin balances.
* **Rule:** All cross-domain database writes must execute inside Room `@Transaction` blocks on `Dispatchers.IO`.

---

## 3. Pure Kotlin Domain & Architectural Boundaries

### EXP-005: 100% Pure Domain Isolation (AD-1)
* **Context:** Domain models, use cases, and behavioral engines (`com.personaltracker.domain.*`).
* **Heuristic:** Importing Android framework classes (`android.*`, `androidx.*`, `Context`, `SharedPreferences`, `Bundle`) into `domain/` breaks unit testing on standard JVM, couples business logic to the OS, and violates layer boundaries.
* **Rule:** The domain layer must remain pure Kotlin (`kotlin.*`, `kotlinx.coroutines.*`, `java.time.*`). All Android OS capabilities must reside in `platform/` or `data/` and be accessed via pure domain interfaces.

### EXP-006: Unidirectional Reactive MVI Contract (AD-2)
* **Context:** Screen presentation and ViewModel architecture (`com.personaltracker.ui.screens.*`).
* **Heuristic:** Mutable state properties and ad-hoc event channels cause state divergence and race conditions.
* **Rule:** Every screen must expose:
  1. `UiState`: Single immutable data class.
  2. `UiIntent`: Sealed interface for user actions and lifecycle events.
  3. `UiEffect`: Sealed interface backed by `Channel<UiEffect>(Channel.BUFFERED)` and exposed as `Flow<UiEffect>`.
  Composables must be split into a stateful `*Route` collector and a 100% stateless `*ScreenContent` renderer (Figma-Ready Split).

---

## 4. UI Design Tokens & Animations

### EXP-007: Strict Design Token Conformance (AD-7)
* **Context:** Jetpack Compose UI composables (`ui/theme/`, `ui/components/`, `ui/screens/`).
* **Heuristic:** Hardcoded hex colors (`Color(0xFF...)`), literal `dp`, or `sp` values break dark/light mode consistency and violate design system integrity.
* **Rule:** Resolve exclusively from `PersonalTrackerTheme.colors`, `spacing`, `typography`, `shapes`, and `elevation`.

### EXP-008: 48dp Minimum Interactive Touch Target Floor
* **Context:** Clickable chips, icon buttons, and list cards.
* **Heuristic:** Interactive UI targets below 48dp fail Android Accessibility Lint audits and create usability frustration.
* **Rule:** All clickable composables must specify `.defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)` or visually expand their touch hit target to at least 48dp.

### EXP-009: Monotonic Frame Clock VSYNC Sync (`withFrameNanos`)
* **Context:** Hardware-accelerated reward animations (`CoinArcAnimation.kt`).
* **Heuristic:** Using standard `System.currentTimeMillis()` or non-vsync clocks causes animations to run 2x too fast on 120Hz displays (such as modern flagship devices) and stutter on 60Hz displays.
* **Rule:** Use `withFrameNanos` monotonic frame delta calculations for quadratic Bézier curve interpolation.

### EXP-010: Viewport-Hoisted Motion Stage (`RewardMotionStage.kt`)
* **Context:** Particle burst and coin arc reward layers.
* **Heuristic:** Nesting overlay animations inside scrollable columns or sheet composables causes layout clipping at container boundaries.
* **Rule:** Hoist `RewardMotionStage` to `AppShellScaffold` at the root navigation window level.

---

## 5. Non-Punitive Psychological Design & Voice Capture

### EXP-011: Zero Guilt & Non-Punitive Companion Mechanics (AD-4)
* **Context:** Microcopy, notification copy, habit streaks, companion pet states.
* **Heuristic:** Shame and guilt copy (e.g., "You failed your habit", "Pet is starving", "Streak reset to 0") induce user avoidance and abandon.
* **Rule:** The companion pet has zero HP loss, hunger decay, or death mechanics. Missed habits trigger grace/freeze rules. Microcopy is supportive and grounded.

### EXP-012: Deterministic 3-Tier Confidence Routing (AD-8)
* **Context:** Quick thought capture via voice and text.
* **Heuristic:** Unbounded LLMs directly mutating persistence causes data corruption and unintended destructive actions.
* **Rule:**
  - **High ($\ge 0.85$):** Instant commit with a 4–5 second reversible undo toast.
  - **Medium ($0.50..0.85$):** 5 editable confirmation chips (Title, Date, Time, Type, Urgency); zero DB mutation until user confirms.
  - **Low ($< 0.50$):** Safe triage to unfiled capture inbox.
  - Raw audio buffers must be kept strictly in RAM and unconditionally zeroed in `finally` blocks with zero disk writes.

---

## 6. Development & Tooling Operations

### EXP-013: PowerShell Binary Image Capture
* **Context:** Capturing device screenshots via ADB.
* **Heuristic:** Running `adb exec-out screencap -p > file.png` in PowerShell introduces UTF-16LE text byte conversion, corrupting the PNG binary header.
* **Rule:** Always pull directly through device filesystem:
  ```powershell
  adb shell screencap -p /sdcard/temp.png; adb pull /sdcard/temp.png screenshot.png; adb shell rm /sdcard/temp.png
  ```
