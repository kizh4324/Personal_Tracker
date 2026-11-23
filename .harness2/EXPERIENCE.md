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

---

## 13. Deterministic NLP & Pure Domain Date Arithmetic

### EXP-027: NLP Prefix Anchoring & Defensive Temporal Arithmetic (AD-1, AD-8)
* **Context:** Pure Kotlin deterministic command parsing (`DeterministicNlpCommandParser.kt`) and relative date/time resolution (`RelativeDateTimeParser.kt`).
* **Heuristic:** In multi-pass token extraction, stripping leading temporal or intensity tokens (e.g., `"Tomorrow remind me to buy milk"`) leaves leading whitespace. Caret-anchored intent regexes (`^remind me to`) fail to match across leading whitespace, causing intent prefixes to leak into task titles. Additionally, regex digit matching on ISO dates (`\d{4}-\d{2}-\d{2}`) accepts impossible dates (`2026-02-31`), and routine shift patterns risk capturing the English article `"the"` as a target routine name.
* **Rule:** Always execute `cleaned.trimStart()` immediately before applying caret-anchored intent prefix replacements. Validate ISO date matches with `runCatching { LocalDate.parse(...) }.getOrNull()` before accepting them into `ParsedCommand`. Guard routine names against the article `"the"`, support multi-word routine identifiers (`([a-zA-Z0-9_ -]+?)\s+routine`), and safely convert hour duration units to minutes with `Long` overflow protection.---

## 17. Distraction Interception & System Overlay Privacy

### EXP-033: Standalone Lifecycle & ViewTree Owners for WindowManager Compose Overlays (AD-1, AD-7)
* **Context:** Rendering Jetpack Compose within a system alert window (`SYSTEM_ALERT_WINDOW`) or platform service context (`InterventionOverlay.kt`).
* **Heuristic:** When instantiating `ComposeView` directly within `WindowManager` outside an `Activity` lifecycle, Compose throws `IllegalStateException: ViewTreeLifecycleOwner not found from ...` or `ViewTreeSavedStateRegistryOwner not found...`.
* **Rule:** Create an isolated `OverlayLifecycleOwner` implementing `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner` with initialized state registry controllers. Explicitly attach owners via `setViewTreeLifecycleOwner`, `setViewTreeViewModelStoreOwner`, and `setViewTreeSavedStateRegistryOwner` on the `ComposeView` instance before setting content. Handle `onResume` upon window addition and `onDestroy` upon removal to prevent memory leaks.

### EXP-034: Event-Driven Zero-Keylogging Privacy Invariant in Accessibility Services (AD-8, NFR-4)
* **Context:** Detecting foreground app transitions during focus blocks (`DistractionInterceptionService.kt`).
* **Heuristic:** Broad accessibility filters or enabling `canRetrieveWindowContent = true` exposes sensitive user keystrokes, password fields, and on-screen content, violating the project's strict privacy invariants and inviting OS security warnings.
* **Rule:** Restrict `accessibility_service_config.xml` strictly to `android:accessibilityEventTypes="typeWindowStateChanged"` and set `android:canRetrieveWindowContent="false"`. In `onAccessibilityEvent`, immediately discard any event where `eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`, extracting only `event.packageName?.toString()`. Never inspect `event.text`, `event.source`, or traverse accessibility node hierarchies under any circumstances.

### EXP-035: Emergency Telecom & Dialer Immunity Heuristics (AD-1, FR-4.6, FR-10.1)
* **Context:** Preventing distraction interception from obstructing emergency services, dialers, or active telecom calls (`InterceptDistractionUseCase.kt`, `DistractionInterceptionService.kt`).
* **Heuristic:** Naive substring matching (`lower.contains("phone")` or `lower.contains("sos")`) creates false exemptions for unrelated apps (e.g., `com.phonepe.app` or `com.sostravel.app`). Conversely, failing to dismiss the overlay during `CALL_STATE_RINGING` or `CALL_STATE_OFFHOOK` traps the user behind an intervention card during an incoming call. Furthermore, dismissing the overlay on generic `com.android.systemui` window transitions opens an exploit loophole where pulling down the notification shade permanently dismisses the blocker.
* **Rule:** Restrict safety token matching strictly to package dot segments (`val segments = lower.split('.'); segments.any { ... }`). Dynamic dialers must be queried via `TelecomManager.getDefaultDialerPackage()`. Active calls (`CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`) must unconditionally bypass interception and dismiss active overlays, while generic system UI (`com.android.systemui`) transitions must NOT dismiss the intervention overlay.

### EXP-036: Non-Punitive App Whitelist Inversion & Exemption Deletion (AD-1, AD-4)
* **Context:** Managing user exemptions for utility tools (`ToggleAppWhitelistUseCase.kt`, `SettingsScreen.kt`).
* **Heuristic:** Inverting `isStrictBlocked = !existing.isStrictBlocked` when a user deselects a whitelisted utility preset (such as Authenticator or Password Manager) flips `isStrictBlocked` to `true`, inadvertently converting essential security tools into blocked distraction apps.
* **Rule:** When toggling an already whitelisted app (`!existingRule.isStrictBlocked`), toggling it off must delete the rule via `ruleRepository.deleteRule(packageName)` to restore the app to neutral unlisted state. Only apps explicitly created as blocked distraction targets should have `isStrictBlocked = true`.

---

## 18. Artemis Real-Device Autonomous Testing

### EXP-037: Autonomous Real-Device E2E Verification via Artemis MCP (Gate 6)
* **Context:** Testing UI transitions, theme token fidelity, and crash-free runtime stability on physical devices (`com.personaltracker`, Model `I2223`).
* **Heuristic:** Unit tests and headless JVM simulations pass even when UI layouts clip on specific screen resolutions, system alert overlays fail to attach, or JNI libraries fail to link at runtime. Manual exploratory testing after every build is slow and prone to missed regression paths.
* **Rule:** Enforce Gate 6 via the Artemis MCP toolchain (`mobile_get_device_state`, `mobile_run_task --profile flash`, `mobile_diagnose`) whenever a physical device (`adb devices`) or emulator is attached. Adhere strictly to the "Dynamic-First, Coordinate-Fallback" locating philosophy, verify 0 Logcat runtime crashes, and confirm Warm Organic Tactile visual rendering before marking stories complete.

---

## 19. Multi-Agent Design Architecture (Elephant-Goldfish Model)

### EXP-038: Elephant-Goldfish Model (EGM) Zero-Context Spec & Review Gate
* **Context:** Story design, spec drafting, and pre-/post-implementation code review across BMAD, Google Agent Harness, and Artemis Gate 6 cycles.
* **Heuristic:** Accumulating long multi-turn context (the "Elephant") causes cognitive bias and sycophancy, where the authoring agent rationalizes ambiguities in its own specifications and overlooks subtle violations of architectural invariants (`AD-1` to `AD-8`, design tokens, Room transactions). Conversely, designing without repository context leads to hallucinated dependencies and duplicate implementations.
* **Rule:** Decouple specification authoring from verification using the Elephant-Goldfish Model (EGM):
  1. **The Elephant (Spec Designer):** Retains full project context (`docs/okf/index.md`, `ARCHITECTURE-SPINE.md`, `.harness2/EXPERIENCE.md`, `HANDOFF.md`) to synthesize story requirements under a strict **"No Code During Design"** invariant (`/egm:egm-brainstorm` & `/egm:egm-draft`).
  2. **The Goldfish Swarm (Stateless Zero-Context Reviewers):** Before implementation begins (and during post-implementation review), spawn fresh, zero-history subagents (`/egm:egm-review` / `/egm:egm-review-loop`) receiving ONLY the spec file and referenced Kotlin source files (zero parent conversation tokens) to execute the 3-stage gate:
     - *a) Comprehension Check (`egm-comprehension`):* Stateless Goldfish summarizes architecture, domain boundaries, and MVI data flow back to the Elephant to prove zero ambiguity.
     - *b) Antagonistic Review (`egm-design`, `egm-codebase-details`, `egm-inventive-catalyst`):* Probes for `AD-1` domain purity leaks, `AD-4` punitive copy, `AD-7` raw hex/dp token violations, concurrency races, and edge cases.
     - *c) Implementation Readiness Go/No-Go Check (`egm-implementation-readiness`):* Confirms exact file paths, Kotlin data structures, MVI signatures, and JVM test targets are 100% unambiguous before code execution.
  3. **Drift Detection (`/egm:egm-drift-report`):** Compares checked-in specs against live `app/src/main/` Kotlin code after implementation to guarantee zero architectural or contract drift.

### EXP-039: Soft Keyboard & Input Method Immunity in Accessibility Interception (AD-1, FR-4.6, FR-10.1)
* **Context:** Preventing accessibility services from triggering distraction interception or obstructing text entry during IME (keyboard) transitions (`InterceptDistractionUseCase.kt`, `DistractionInterceptionService.kt`).
* **Heuristic:** Virtual keyboards (such as Gboard, Samsung Keyboard, SwiftKey) transition between window states during text input. If soft keyboard package transitions (`com.google.android.inputmethod.latin`, `com.touchtype.swiftkey`, `com.samsung.android.honeyboard`) are evaluated as potential distractions or treated as target foreground apps, text input during legitimate note-taking, quick-capture, or whitelisted apps gets obstructed or causes spurious overlay re-evaluations.
* **Rule:** Unconditionally exempt all input method editors and soft keyboard packages via explicit package matching and segment filtering (`.inputmethod.`, `.keyboard.`, `swiftkey`, `honeyboard`, `ime`) in `InterceptDistractionUseCase`. Treat IME window transitions as non-distracting interactions that neither trigger an intervention card nor dismiss active overlays.

---

## 21. Habits, Streak State Machine & Cadence Immunity

### EXP-043: Non-Punitive Habit Streaks, Cadence Isolation & Grace Shield State Machine (AD-1, AD-4, AD-7, FR-2.4, FR-8.5)
* **Context:** Flexible habit cadences (`Daily`, `SpecificDays`, `WeeklyFrequency`), DayType immunity (`LOW_CAPACITY`, `DayType.REST`), and 5-state flame progression (`COLD`, `WARMING`, `BURNING`, `BLAZING`, `GRACE_SHIELD`) in `StreakStateMachine.kt`, `EvaluateHabitStreaksUseCase.kt`, and `LogHabitUseCase.kt`.
* **Heuristic:** Rigid daily habit tracking triggers false miss penalties when users schedule flexible cadences (e.g. Mon/Wed/Fri evaluated on Tuesday) or encounter low-capacity/rest days. Furthermore, calculating flame visual states purely from an integer streak count causes `GRACE_SHIELD` indicators to remain unreachable in the UI. In addition, blocking completion because a day was previously logged as a rest or missed day prevents users from completing habits later in the day.
* **Rule:** (1) Pure Kotlin cadence domain models must isolate scheduled vs. unscheduled days; unscheduled days receive `REST_DAY` without consuming freeze or slack days. (2) `LOW_CAPACITY` and `REST` DayTypes grant automatic immunity (`DAY_TYPE_IMMUNITY`) without streak loss. (3) Overload `getFlameState(streak: Int, isGraceShieldActive: Boolean)` and pass `flameState` through ViewModel, Screen, and Card to `StreakFlameBadge`. (4) `LogHabitUseCase` must allow upgrading non-completed logs (`REST_DAY`, `MISSED`, `SLACK_USED`, `GRACE_SHIELD_ABSORBED`) to `COMPLETED` and awarding coins, while enforcing compound idempotency `HABIT_LOG_{habitId}_{date}`.

### EXP-044: Non-Destructive Room Migration with Pre-Existing Constraint Alignment (AD-3, AD-6)
* **Context:** Adding cadence scheduling and goal configuration columns to `habits` table in Room `MIGRATION_2_3`.
* **Heuristic:** Using `ALTER TABLE habits ADD COLUMN cadenceType TEXT NOT NULL DEFAULT 'DAILY'` silently overrides pre-existing habits configured with custom days (e.g. `targetDaysOfWeek = "1,3,5"`), converting them into daily habits and causing immediate false miss penalties upon midnight rollover post-migration.
* **Rule:** Always follow `ALTER TABLE` additions with conditional SQL updates (e.g. `UPDATE habits SET cadenceType = 'SPECIFIC_DAYS' WHERE targetDaysOfWeek != '1,2,3,4,5,6,7' AND targetDaysOfWeek != ''`) and implement defensive fallback inference in `DataMappers.toDomain()`, guaranteeing 100% backward compatibility and data preservation across database schema migrations.

---

## 22. Gamification Coin Ledger & Daily Earn Caps

### EXP-045: Idempotent Gamification Ledger, Gross Spend Defense & Two-Tier Daily Caps (AD-1, AD-3, AD-4, AD-6, FR-8.1, FR-8.2)
* **Context:** Pure Kotlin gamification ledger engine, anti-farming earn caps, gross spend defense, and duplicate idempotency handling in `GamificationLedgerEngine.kt`, `CoinLedgerDao.kt`, `EconomyRepositoryImpl.kt`, `AwardCoinsUseCase.kt`, `CompleteTaskUseCase.kt`, and `LogHabitUseCase.kt`.
* **Heuristic:** Naive sum queries (`SELECT SUM(deltaAmount) ...`) return SQL `NULL` when the ledger is empty and allow negative transactions (shop purchases, undo clawbacks) to deflate daily earned totals, enabling infinite coin re-farming. Furthermore, uncoordinated cross-repository transactions split atomic database operations, and unhandled duplicate idempotency collisions throw uncaught `IllegalStateException` or `SQLiteConstraintException` on rapid UI taps.
* **Rule:**
  1. **Gross Spend Defense & Null Safety:** All daily sum queries in Room DAOs must execute `SELECT COALESCE(SUM(deltaAmount), 0) ... WHERE timestamp >= :startOfDayTimestamp AND deltaAmount > 0` (with optional `eventType` filter) to prevent spend/clawback deflation and runtime null crashes.
  2. **Two-Tier Clamping & Focus Exemption:** Daily caps (`DAILY_TASK_CAP = 50`, `DAILY_HABIT_CAP = 40`, `DAILY_FOCUS_CAP = 120`, `DAILY_GLOBAL_FARMABLE_CAP = 100`) must evaluate category caps first, then global farmable caps for farmable items (`TASK_COMPLETION`, `HABIT_COMPLETION`, `ROUTINE_COMPLETION`). Deep work (`STUDY_SESSION`) is exempt from the 100 farmable cap and governed exclusively by the 120 focus cap.
  3. **Graceful Duplicate Handling (AD-6):** Rapid double taps must never throw unhandled exceptions or crash. `EconomyRepositoryImpl` must catch duplicate key collisions inside `TransactionRunner` and return `AwardCoinsResult(coinsAwarded = 0, isDuplicate = true)` cleanly.
  4. **Single-Transaction Atomicity (AD-3, AD-6):** Use cases (`CompleteTaskUseCase`, `LogHabitUseCase`) compute the capped coin award first, then pass it into atomic repository methods (`completeTaskWithReward`, `logHabitWithReward`) inside Room `@Transaction` blocks on `Dispatchers.IO`.
  5. **Non-Negative Clawback Floor & Non-Punitive Copy (AD-4):** 5-second quick-capture undo reversals (`CAPTURE_REVERSAL`) clamp deductions to `minOf(amount, currentBalance)`, guaranteeing `balanceAfter >= 0`. When earn caps are saturated, emit affirmative celebratory copy ("Daily coin goal reached! Awesome work today.") without blocking task/habit completion.

---

## 23. VSYNC-Aligned Bézier Trajectories & Persistent Coin HUD

### EXP-046: Zero-Allocation Bézier Particles, HUD Arrival Coordination & RenderThread Badge Transforms (AD-1, AD-7, Story 4.3)
* **Context:** High-performance particle animation along quadratic Bézier curves, top-bar HUD balance roll-up, 1.18x spring scale bursts, and floating badges in `BezierMath.kt`, `CoinArcAnimation.kt`, `RewardMotionStage.kt`, and `CoinPill.kt`.
* **Heuristic:**
  1. Multiple independent `Animatable` coroutines or allocations (`Offset`, `Color`, lists) inside `Canvas(fillMaxSize) { ... }` cause GC thrashing and frame drops at 60Hz and 120Hz.
  2. Dynamic apex calculation $\min(P_0.y, P_2.y) - \text{arcHeight}$ yields negative Y coordinates when targets are near the top of the window, driving coins off-screen into status bars and camera notches.
  3. Room commits coin transactions immediately at $t = 0\text{ms}$, causing `CoinPill` to roll up balance 600ms–1080ms before coins arrive at the HUD if observing the database balance directly.
  4. Reading animated progress values in composition-phase modifiers (`Modifier.offset`, `Modifier.alpha`) forces the entire HUD and its text/icon child trees to recompose 60–120 times per second during spring bursts.
  5. Single-slot state machines in hoisted overlay layers cancel and obliterate in-flight particles when a user rapidly completes multiple items.
* **Rule:**
  1. **Monotonic Single-Loop Clock & Zero Allocation:** Drive multi-coin staggered progression ($T = D + (N - 1) \times S$) via a single monotonic frame clock loop (`withFrameNanos`), precomputing control points in outer scope and pre-caching colors/radii as primitive floats before `onDraw`.
  2. **Safe Viewport Headroom Clamping:** Dynamic curve apex control points must clamp $\text{peakY} = \max(\text{minHeadroomPx}, \text{rawPeakY})$ using `PersonalTrackerTheme.spacing.minTouchTarget`, guaranteeing trajectories remain strictly inside the visible viewport.
  3. **Arrival Coordination Protocol:** `RewardMotionStageState` must coordinate coin arrival via `onCoinArrival: (count: Int) -> Unit`. `CoinPill` holds its displayed balance until coins land at the target `Offset`, smoothly rolling up with Framer spring physics (`shopPurchaseBurstSpringSpec`) on impact.
  4. **RenderThread Transforms:** Animate floating `"+X"` badge pills strictly via `Modifier.graphicsLayer { translationY = -spacing.spacing4.toPx() * progress; alpha = 1f - progress }` to bypass composition and execute exclusively on the RenderThread.
  5. **Concurrent Multi-Burst State:** `RewardMotionStageState` must store `activeCoinEvents: List<CoinRewardEvent>`, rendering concurrent bursts independently without mid-air particle cancellation.
  6. **Accessibility & Cold-Start Guards:** Check `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (or reduced-motion) to bypass flight arcs and roll up immediately. Guard initial mount with `isInitialLoad` to suppress spurious `+X` badges and bursts on app launch.

---

## 24. Non-Punishing Companion Engine & Atomic Cosmetics Shop

### EXP-047: 7-State Rive Ordinal Alignment, DayType Rest Immunity & Atomic Idempotent Cosmetics Shop (AD-1, AD-3, AD-4, AD-6, AD-7, Story 4.4)
* **Context:** Pure Kotlin companion pet behavioral engine, 7-state emotional register, cosmetics wardrobe shop, and home widget integration in `CompanionState.kt`, `CompanionStateEngine.kt`, `ResolveCompanionMoodUseCase.kt`, `PurchaseOrEquipShopItemUseCase.kt`, `EconomyRepositoryImpl.kt`, `CompanionView.kt`, `CompanionScreen.kt`, and `HomeScreen.kt`.
* **Heuristic:**
  1. Defining separate enum registers across domain and UI causes ordinal collisions and inversions (e.g. legacy `CompanionMood` swapping `CELEBRATING` and `ENCOURAGING` vs the Rive binary contract `1=Celebrating, 2=Encouraging`), leading to incorrect character animations.
  2. Double-tapping purchase buttons or re-purchasing unlocked items throws unhandled `IllegalStateException("Duplicate transaction idempotency key")` in production if transactions are not designed with idempotent fallbacks.
  3. Empty databases on cold start leave catalog and companion profile queries null, triggering uncaught exceptions or rendering empty wardrobe grids.
  4. Using compact chip sizing (`spacing.chipHeight = 32dp`) on interactive shop buttons or rendering static unclickable greeting avatars violates touch target floors (AD-7) and fragments navigation pathways.
* **Rule:**
  1. **Canonical 7-State Domain Source of Truth (AD-1, AD-4):** Maintain a single pure Kotlin domain enum `CompanionState(0..6)` (`IDLE_COZY=0`, `CELEBRATING_REWARD=1`, `ENCOURAGING_WARM=2`, `CONCERNED_CHECKIN=3`, `FOCUSING_STUDY=4`, `RESTING_PEACEFUL=5`, `HAPPY_BOUNCE=6`) aligned 1:1 with Rive runtime state machine inputs. Legacy domain enums must alias ordinals and deprecate safely.
  2. **DayType Rest Immunity (AD-4):** Rest days (`"dt_rest"`, `"dt_low_capacity"`, `LOW_CAPACITY`, `REST_RECOVERY`) must unconditionally resolve to `RESTING_PEACEFUL` with non-guilt supportive copy ("Rest day preserved. Taking it easy today."). Missing tasks or broken streaks never trigger sad, sick, or angry states.
  3. **Atomic Purchase & Auto-Equip (AD-3, AD-6):** Purchases deduct coins (`deltaAmount = -priceCoins`), unlock items (`isUnlocked = true`), and equip items (`activeAccessoryId = itemId`) inside a single atomic Room `@Transaction` block on `Dispatchers.IO`. Double taps or unlocked re-taps must execute idempotently without throwing.
  4. **Catalog Cold-Start Seeding:** Repositories must auto-seed baseline items (`DEFAULT_SHOP_ITEMS`: Cozy Scarf, Study Cap, Golden Crown, Warm Coffee) and default `CompanionProfile` (`Sproutling`, Level 1, Energy 0) when the database is empty.
  5. **48dp Interactive Touch Target Floor (AD-7):** All wardrobe purchase and equip buttons must enforce `Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)` (48dp). The home greeting avatar must be an interactive 48dp clickable target navigating directly to the Companion destination.

---

## 25. Consumable Utility vs Permanent Cosmetic Decoupling & Milestone Choreography

### EXP-048: Consumable Utility Shop Decoupling, Atomic Streak Shield Banking & Reduced-Motion Milestone Choreography (AD-1, AD-3, AD-4, AD-6, AD-7, Story 4.5)
* **Context:** Consumable utility items (Grace Shields / Slack Bank Days), dynamic shop catalog categories, atomic multi-dao Room `@Transaction` boundaries, and modal milestone full-reveal celebrations in `ShopItem.kt`, `PurchaseStreakShieldUseCase.kt`, `HabitRepositoryImpl.kt`, `EconomyRepositoryImpl.kt`, `RewardMotionStage.kt`, `HabitStreakCard.kt`, `HabitsViewModel.kt`, and `CompanionViewModel.kt`.
* **Heuristic:**
  1. Conflating consumable items (e.g. Grace Shields) with permanent cosmetics corrupts wardrobe state (`activeAccessoryId`) and locks repeatable purchases if `isUnlocked = true` is persisted.
  2. Executing coin deduction in `EconomyRepository` and habit streak shield increment in `HabitRepository` across separate domain use-case calls creates race conditions and partial transaction failures where coins are lost if shield increment fails.
  3. When purchasing shields from the Companion Screen shop without an explicit habit context, attempting a blind purchase fails unless the use case dynamically resolves the active habit with the lowest slack days.
  4. Firing micro coin animations simultaneously with modal milestone full-reveal celebrations causes Lottie visual collision, frame drop, and conflicting audio/visual cues.
  5. Playing full-screen modal celebrations when the system has reduced-motion enabled violates accessibility standards and blocks user workflows.
* **Rule:**
  1. **Strict Category Decoupling (`ShopItem.kt`):** Categorize items into `CATEGORY_OUTFIT`, `CATEGORY_ACCESSORY`, `CATEGORY_BACKGROUND_DECOR`, and `CATEGORY_UTILITY`. Consumable utility items (`CATEGORY_UTILITY`) must bypass `activeAccessoryId` updates and must never persist `isUnlocked = true`, ensuring repeatable purchases.
  2. **Single Atomic Room `@Transaction` Boundary (AD-3, AD-6):** Consolidate consumable purchases (`HabitRepository.purchaseStreakShield`) into a single Room `@Transaction` block on `Dispatchers.IO` joining `CoinLedgerDao` (deducting coins with unique idempotency key `SHIELD_BUY_{habitId}_{timestamp}`) and `HabitDao` (incrementing `slackBankDays` capped at 3).
  3. **Null Target Habit Resolution (`PurchaseStreakShieldUseCase.kt`):** When `targetHabitId` is null (e.g. bought from Companion Shop), automatically query all active habits and target the habit with the lowest `slackBankDays < 3`. If all active habits are at max capacity (3/3), cleanly return `PurchaseStreakShieldResult.CapReached`.
  4. **Milestone Preemption Hierarchy (`HabitsViewModel.kt`):** On 7-day consistency milestones (`streak > 0 && streak % 7 == 0`), the full milestone reveal (`MilestoneFullReveal`) supersedes `CoinEarnMicro` to eliminate Lottie slot collisions, while setting companion expression to `CompanionState.CELEBRATING_REWARD`.
  5. **Reduced-Motion Modal Bypass (`RewardMotionStage.kt`):** When `isReducedMotion == true`, modal celebrations (`MilestoneFullReveal`) must immediately invoke `onLottieComplete()` via `LaunchedEffect` without composing animations or blocking user navigation.

---

## 26. Tiered Notification Channels & Defensive Exact Alarm Scheduling

### EXP-049: Tiered Notification Channels, Differentiated Alarm Wakeups & Defensive Permission Fallbacks (AD-1, AD-3, AD-4, AD-7, Story 5.1)
* **Context:** Differentiated notification channels (`channel_routine` [LOW], `channel_important` [DEFAULT], `channel_urgent` [HIGH] with companion double-pulse haptics), battery-conscious alarm scheduling, Android 14+ `SCHEDULE_EXACT_ALARM` defensive fallbacks, and Settings permission deep-links in `TieredNotificationManager.kt`, `ExactAlarmScheduler.kt`, `AlarmNotificationReceiver.kt`, `TaskAlarmScheduler.kt`, `SettingsViewModel.kt`, and `SettingsScreen.kt`.
* **Heuristic:**
  1. Invoking `AlarmManager.setAlarmClock()` indiscriminately for all tasks clutters the Android status bar and lockscreen with a persistent alarm clock icon, inducing psychological fatigue and anxiety (violating AD-4).
  2. Calling `setExactAndAllowWhileIdle()` on Android 14+ (API 34+) when `SCHEDULE_EXACT_ALARM` has been revoked by the user or OS throws an uncaught `SecurityException`, crashing the application immediately.
  3. Catching `SecurityException` inside low-level delegates (e.g. `SystemAlarmManagerDelegate`) prevents callers from executing coordinated fallback strategies and returning failure indicators to domain engines.
  4. Attempting to schedule alarms for past timestamps (`triggerEpochMillis <= System.currentTimeMillis()`) triggers immediate spuriously delivered notifications during startup or recovery sweeps.
  5. Posting notifications (`notificationManager.notify()`) on Android 13+ (API 33+) without verifying `POST_NOTIFICATIONS` triggers fatal Android lint errors and security warnings.
* **Rule:**
  1. **Tier-Differentiated Wakeup Precision:** Differentiate alarm delivery strictly by intensity: `ROUTINE` tasks use inexact `setAndAllowWhileIdle()` with zero status bar clock icon; `IMPORTANT` tasks use exact `setExactAndAllowWhileIdle()`; and only `URGENT` tasks use `setAlarmClock()` with heads-up display and status bar icons.
  2. **Single-Responsibility Exception Handling:** Low-level delegates (`SystemAlarmManagerDelegate`) must let `SecurityException` bubble up. The orchestration scheduler (`ExactAlarmScheduler`) catches `SecurityException` at a single explicit boundary, immediately invokes `scheduleInexactFallback()`, and returns `false` to communicate exact scheduling unavailability.
  3. **Temporal Invariant & Past Rejection:** `ExactAlarmScheduler` must guard against past timestamps (`triggerEpochMillis <= System.currentTimeMillis()`), returning `false` immediately without scheduling.
  4. **Domain Purity Bridge (AD-1):** Expose exact alarm capability through pure Kotlin domain interface `TaskAlarmScheduler.canScheduleExactAlarms(): Boolean` with zero `android.*` imports, implemented in platform layer via `exactAlarmScheduler.canScheduleExact()`.
  5. **Direct Settings Deep-Links & Non-Punitive Copy (AD-4, AD-7):** In `SettingsScreen`, present notification channel preferences and exact alarm permission status using 100% `PersonalTrackerTheme` tokens (48dp touch targets). Deep-link directly to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with `package:${context.packageName}` and `ACTION_APP_NOTIFICATION_SETTINGS` with `EXTRA_APP_PACKAGE`.

















