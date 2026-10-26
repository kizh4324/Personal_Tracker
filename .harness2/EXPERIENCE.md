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

---

## 7. MVI, StateFlow & Turbine Testing

### EXP-014: StateFlow Testing with Turbine & Dispatcher Rules
* **Context:** Unit-testing `@HiltViewModel` classes with `StateFlow<UiState>` and Channel-backed `Flow<UiEffect>`.
* **Heuristic:** `StateFlow` conflates duplicate values and operates over active coroutine scopes. Testing without a deterministic `TestDispatcher` causes timing races and missed emissions.
* **Rule:** Use a `MainDispatcherRule` (with `StandardTestDispatcher` or `UnconfinedTestDispatcher`) in every ViewModel test suite. In Turbine blocks (`viewModel.uiState.test { ... }`), call `testScheduler.advanceUntilIdle()` before checking expected state transitions.

### EXP-015: Static Service State Cleanup in Unit Tests (@After Teardown)
* **Context:** ViewModels observing static companion StateFlows (e.g., `StudySessionForegroundService.timerState`).
* **Heuristic:** Mutating companion object `StateFlow` instances during test runs pollutes state for subsequent test classes running in the same JVM process, causing unpredictable test order flakiness.
* **Rule:** Every test class that modifies static service or platform state must provide an `@After fun tearDown()` resetting `_timerState.value = StudySessionTimerState()`.

### EXP-016: Figma-Ready Route/ScreenContent Decoupling (AD-2)
* **Context:** Screen presentation architecture and upcoming Figma/Stitch MCP UI synchronization.
* **Heuristic:** Mixing Hilt ViewModel injection and state collection inside rendering composables prevents design tools and subagents from swapping visual components without breaking state contracts.
* **Rule:** Strictly maintain two composables per screen:
  1. `*Route(viewModel: *ViewModel = hiltViewModel(), onNavigate: ...)`: Stateful Hilt collector utilizing `collectAsStateWithLifecycle()` and launching effect collectors.
  2. `*ScreenContent(state: *UiState, onIntent: (*UiIntent) -> Unit)`: 100% stateless Compose UI with zero ViewModel or repository dependencies.

### EXP-017: Multi-Flow Reactive Aggregation with Dynamic Date Resolution
* **Context:** Persistent top context bars (`AppShellViewModel`) and schedule resolution.
* **Heuristic:** Capturing `val today = LocalDate.now().toString()` at ViewModel instantiation freezes date resolution. The app remains on the previous day's schedule after crossing midnight.
* **Rule:** Always evaluate `LocalDate.now().toString()` dynamically inside the `combine(...) { ... }` transform lambda across `DayTypeRepository`, `HabitRepository`, and `EconomyRepository` streams.

### EXP-018: Job Cancellation on UI Refresh Intents
* **Context:** ViewModels handling explicit `UiIntent.Refresh` actions.
* **Heuristic:** Re-launching repository collection coroutines on `Refresh` without cancelling prior observers creates duplicate concurrent collectors, memory leaks, and multiplied database load.
* **Rule:** Store references to observation `Job`s and cancel them prior to launching new observations upon receiving `UiIntent.Refresh`.

---

## 8. Schedule Adaptation & Persistence Invariants

### EXP-019: Non-Destructive Schedule Adaptation & Template Immutability (AD-6)
* **Context:** Dynamic schedule recalculation and mid-day DayType template swaps (`SwitchDayTypeUseCase`).
* **Heuristic:** Overwriting `RoutineStepEntity` durations in SQLite when scaling routines for a day-override permanently corrupts baseline templates, causing mathematical distortion and preventing clean schedule resets.
* **Rule:** Routine duration scaling must be calculated dynamically for the active schedule; base template steps in `routine_steps` must remain immutable in persistent storage.

### EXP-020: Foreign Key Constraint Safety on Single-Date Overrides (AD-3)
* **Context:** Creating date-specific schedule overrides (`DayOverrideEntity` -> `DayTypeEntity`).
* **Heuristic:** Room enforces `ForeignKey(onDelete = RESTRICT)` on `dayTypeId`. Inserting an override with a fallback ID or unseeded DayType throws `SQLiteConstraintException`.
* **Rule:** Always ensure the target `DayType` exists and is confirmed in SQLite persistence before writing a `DayOverrideEntity`.

---

## 9. Domain Priority Resolution & Test Synchronization

### EXP-021: Decoupling Pure Domain Priority Models from UI Lambdas (AD-1, AD-2)
* **Context:** 6-tier Hero priority resolution (`ResolveHeroStateUseCase`) and MVI state mapping.
* **Heuristic:** Embedding UI callback lambdas (`() -> Unit`) directly inside domain models violates AD-1 layer purity and prevents standard JVM test isolation.
* **Rule:** Domain priority models must be pure Kotlin data structures (`HeroPriorityState`). ViewModels map domain state to presentation models and dispatch actions exclusively through sealed `UiIntent` contracts.

### EXP-022: Turbine StateFlow Multi-Emission Synchronization in Fake Repositories
* **Context:** Unit-testing multi-entity updates (`CarryForwardTasksUseCase`) with Turbine and fake repository test doubles.
* **Heuristic:** Emitting repository `StateFlow` updates before synchronizing internal data collections causes intermediate StateFlow emissions with divergent state (e.g., `overdueTasks` count mismatching `heroCardState.CarryForwardReview`).
* **Rule:** Always synchronize backing collections prior to emitting updated flow values in test doubles, and return defensive immutable copies (`toList()`) to prevent in-place mutation races.

---

## 10. Task Management, Alarms & Composite UI Controls

### EXP-023: Pure Domain Alarm Scheduling Abstraction & Key Normalization (AD-1, FR-5)
* **Context:** Coordinating reminder alarms from pure Kotlin domain interactors (`SaveTaskUseCase`, `DeleteTaskUseCase`, `CompleteTaskUseCase`) through `ExactAlarmScheduler`.
* **Heuristic:** Domain use cases cannot directly reference `AlarmManager`, `Context`, or `PendingIntent` without violating AD-1. Furthermore, if the platform adapter constructs alarm keys with a prefix (e.g. `"TASK_$taskId"`), calling cancellation with an already-prefixed ID without defensive normalization (`taskId.removePrefix("TASK_")`) results in mismatched keys (`"TASK_TASK_..."`) and orphaned alarms.
* **Rule:** Expose pure Kotlin contracts (`TaskAlarmScheduler`) in `domain/alarm/`, bind to platform implementations in `di/`, defensively strip redundant prefixes before passing to alarm managers, and suppress alarm registration if scheduled time has already elapsed on the current day to avoid immediate alarm firing upon save.

### EXP-024: Atomic Control Packaging for Composite Dialog State (AD-2)
* **Context:** Managing modal sheet visibility and editing entities concurrently (`TaskEditorSheet`, `HomeViewModel`).
* **Heuristic:** Using distinct `MutableStateFlow<Boolean>` for visibility and `MutableStateFlow<Task?>` for target entity causes intermediate desynchronized emissions across `combine(...)` pipelines where the sheet is flagged open before the entity is populated, leading to UI flicker or stale draft data.
* **Rule:** Encapsulate composite dialog/sheet control state into a single atomic model (e.g. `data class TaskEditorControl(val isOpen: Boolean, val task: Task?)`) and emit it as a single StateFlow. In stateless Compose sheets, reset draft form fields inside `LaunchedEffect(isOpen, task)` to prevent draft leakage across dialog invocations.

---

## 11. Sequential Routines & Mid-Day Recalculation

### EXP-025: Mid-Day Routine Timeline Recalculation & Idempotent Completion (AD-3, AD-6, FR-3.5)
* **Context:** Sequential routine progression (`AdvanceRoutineStepUseCase`), mid-day timeline shifts (`RecalculateRoutineTimelineUseCase`), and 15-coin routine bonus awards in `HomeViewModel`.
* **Heuristic:** Shifting routines forward mid-day can easily trigger two failure modes: (1) wrapping around midnight (e.g. 23:50 + 20 min = 00:10 next day) causing negative intervals and UI crashes, and (2) mutatively overriding base template step durations in SQLite, which permanently corrupts DayType baselines. Additionally, if the user replays or refreshes the routine, re-awarding completion coins causes ledger inflation.
* **Rule:** (1) Always clamp end times at `23:59` via `safePlusMinutes` to avoid cross-day midnight wrap-around. (2) Keep baseline `routine_steps` rows in Room immutable during dynamic timeline shifting and scaling; recalculations must compute purely in-memory models (`ScheduledRoutineStep`). (3) Verify the deterministic idempotency key `ROUTINE_COMP_${routineId}_$date` against `EconomyRepository.getTransactionByIdempotencyKey(key)` inside the completion use case to guarantee strictly one 15-coin reward per routine per day.

---

## 12. Morphing Shared-Element Overlays & Audio Lifecycle

### EXP-026: Shared-Element Spring Physics & IME Avoidance in Morphing Overlays (AD-5, AD-7, AD-8)
* **Context:** Shared-element animation transitions between FAB and modal sheets (`AppShellScaffold.kt`, `QuickCaptureSheet.kt`) and IME keyboard avoidance.
* **Heuristic:** Declaring animation token methods with return type `AnimationSpec<T>` prevents compatibility with Compose enter/exit transitions (`slideInVertically`, `fadeIn`, `scaleIn`, `scaleOut`) which strictly require `FiniteAnimationSpec<T>`. Returning `SpringSpec<T>` provides full compatibility with both finite and general animation APIs. Furthermore, omitting `.imePadding()` alongside `.navigationBarsPadding()` causes the Android soft keyboard to occlude input fields, action buttons, and live waveform visualizers.
* **Rule:** Expose spring physics tokens as `SpringSpec<T>` in `AnimationTokens.kt`. Always chain `.navigationBarsPadding().imePadding()` on modal overlays. Auto-initiate microphone recording immediately upon opening `QuickCaptureSheet` when audio permission is already granted, and ensure unconditional buffer zeroing and recording job cancellation on dismiss.





