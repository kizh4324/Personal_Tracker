---
title: 'Story 1.7: Unidirectional MVI Screen Wiring & ViewModels Baseline'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: 'a1f0e55a6860f9d5f7f613f35769ed793a088dbf'
review_loop_iteration: 0
context:
  - 'ARCHITECTURE-SPINE.md'
  - '_bmad-output/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The existing screen composables (`HomeScreen`, `CompanionScreen`, `HabitsScreen`, `StudyScreen`, `SettingsScreen`) use ephemeral internal `remember { mutableStateOf(...) }` demo state or empty scaffolds, remaining completely decoupled from the encrypted SQLite Flow repositories and platform services. Furthermore, UI composables mix state collection and rendering, which prevents drop-in design swapping from Figma/Stitch MCP tools.

**Approach:** Implement production `@HiltViewModel` MVI classes exposing single immutable `StateFlow<UiState>` and Channel-backed `Flow<UiEffect>` (`HomeViewModel`, `CompanionViewModel`, `QuickCaptureViewModel`, `HabitsViewModel`, `StudyViewModel`, `SettingsViewModel`), enforce a strict Figma-ready split (`*Route` stateful Hilt collector vs. `*ScreenContent` 100% stateless renderer), and wire `AppShellScaffold` and `PersonalTrackerNavHost` to live reactive repositories.

## Boundaries & Constraints

**Always:**
- Strictly conform to `ARCHITECTURE-SPINE.md` (AD-1, AD-2, AD-4, AD-7, AD-8) and `_bmad-output/project-context.md`.
- `UiState` must be a single, immutable data class representing the complete screen state.
- `UiIntent` must be a sealed interface handling all user actions and lifecycle events.
- `UiEffect` must be a sealed interface backed by `Channel<UiEffect>(Channel.BUFFERED)` and exposed as `Flow<UiEffect>` via `receiveAsFlow()`.
- Every screen must separate `*Route` (stateful `@Composable` collecting ViewModel state via `collectAsStateWithLifecycle()` and launching effect collectors) from `*ScreenContent(state: *UiState, onIntent: (*UiIntent) -> Unit)` (100% stateless UI).
- ViewModels must interact exclusively with Repository interfaces (`domain/repository/`), Use Cases (`domain/usecase/`), and Domain Engines (`domain/engine/`). ViewModels must NEVER directly import Room DAOs, Entities, or Android system services (AD-1).
- Strictly non-punishing copy: zero guilt, nag, or shaming copy in UI strings or notifications (AD-4).
- Zero hardcoded colors, `dp`, or `sp` literals in UI composables; resolve exclusively from `com.personaltracker.ui.theme` tokens (AD-7).
- 3-tier confidence routing (AD-8) in `QuickCaptureViewModel`: High ($\ge 0.85$) with 5s undo toast, Medium ($0.50..0.85$) with confirmation chips, Low ($< 0.50$) saved to unfiled inbox.

**Ask First:**
- Introducing new navigation destinations or altering existing destination route strings.
- Adding third-party MVI or state management dependencies beyond official Jetpack Compose and Kotlinx Coroutines.

**Never:**
- Never import Android framework classes (`android.*`, `androidx.*`) into `com.personaltracker.domain` (AD-1).
- Never allow direct Room database mutations from ViewModels without repository interfaces.
- Never write hardcoded hex colors, raw `dp`, or `sp` values in composables (AD-7).
- Never allow punitive companion decay, starvation, HP loss, or death mechanics (AD-4).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Task completion reward | User taps complete on task in `HomeScreenContent` | `HomeViewModel` processes `ToggleTask`, invokes `CompleteTaskUseCase`, emits `HomeUiEffect.ShowCoinReward` and `ShowLottieReward` | If task not found or error, emits `HomeUiEffect.ShowSnackbar` with calm message |
| Habit completion streak | User taps complete on habit in `HomeScreenContent` or `HabitsScreenContent` | `LogHabitUseCase` evaluates streak, awards coins, updates companion mood; ViewModel updates state and emits reward effect | If already completed today, preserves streak without duplicate coin award |
| Hero Card state evaluation | Reactive flows emit active study session or interrupted task | `HomeViewModel` evaluates 6-state hierarchy: `ActiveFocus` > `InterruptedResumable` > `MorningBanner` > `UpcomingItem` > `CarryForwardReview` > `IntentionalIdle` | Falls back to `IntentionalIdle` if daily schedule empty |
| Quick Capture High Confidence | User submits "Dentist appointment tomorrow 3pm" (confidence $\ge 0.85$) | `CaptureQuickThoughtUseCase` commits Task; `QuickCaptureViewModel` exposes 5-second undo toast; sheet dismisses | Tapping "Undo" deletes created task without penalties |
| Quick Capture Medium Confidence | User submits ambiguous command ($0.50 \le \text{confidence} < 0.85$) | `QuickCaptureViewModel` presents 5 editable confirmation chips (Title, Date, Time, Type, Urgency) in sheet; no DB mutation until user confirms | User can edit chip or confirm; dismiss discards uncommitted draft |
| Quick Capture Low Confidence | User captures noisy audio or gibberish (confidence $< 0.50$) | `QuickCaptureViewModel` routes to `UnfiledInboxRepository`; sheet displays non-punishing saved-to-inbox notice | Item triaged safely in unfiled inbox |
| Companion pet emotional display | User opens Companion screen or completes task | `CompanionViewModel` reflects `CompanionMood` in `CompanionUiState.mood` (7 states); XP bar and level progress updated | Non-punishing fallback to `Idle` on unknown state |
| Study timer synchronization | User starts 25m session on `StudyScreenContent` | `StudyViewModel` initiates session, binds to `StudySessionForegroundService.timerState`, updates countdown display and action buttons | If timer interrupted or stopped, records session safely |

</frozen-after-approval>

## Code Map

- `app/src/main/java/com/personaltracker/ui/screens/home/` -- `HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt`, `HomeUiIntent.kt`, `HomeUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/screens/quickcapture/` -- `QuickCaptureViewModel.kt`, `QuickCaptureUiState.kt`, `QuickCaptureUiIntent.kt`, `QuickCaptureUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/screens/companion/` -- `CompanionScreen.kt`, `CompanionViewModel.kt`, `CompanionUiState.kt`, `CompanionUiIntent.kt`, `CompanionUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/screens/habits/` -- `HabitsScreen.kt`, `HabitsViewModel.kt`, `HabitsUiState.kt`, `HabitsUiIntent.kt`, `HabitsUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/screens/study/` -- `StudyScreen.kt`, `StudyViewModel.kt`, `StudyUiState.kt`, `StudyUiIntent.kt`, `StudyUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/screens/settings/` -- `SettingsScreen.kt`, `SettingsViewModel.kt`, `SettingsUiState.kt`, `SettingsUiIntent.kt`, `SettingsUiEffect.kt`.
- `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` -- Connect QuickCapture and Top Context Bar (DayType, Streak, Coins) to MVI state.
- `app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt` -- Bind navigation routes to stateful `*Route()` composables.
- `app/src/test/java/com/personaltracker/ui/` -- Turbine and Coroutines test suite for all ViewModels (`HomeViewModelTest`, `QuickCaptureViewModelTest`, `CompanionViewModelTest`, `HabitsViewModelTest`, `StudyViewModelTest`).

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/` -- Create `HomeUiState`, `HomeUiIntent`, `HomeUiEffect`, and `HomeViewModel` combining repositories to drive `HeroActionCard` (6 states), daily action list, `DayTypeChip`, `StreakFlameBadge`, and `CoinPill`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt` -- Refactor to `HomeRoute` (stateful Hilt collector) and `HomeScreenContent` (100% stateless UI composable).
- [x] `app/src/main/java/com/personaltracker/ui/screens/quickcapture/` -- Create `QuickCaptureUiState`, `QuickCaptureUiIntent`, `QuickCaptureUiEffect`, and `QuickCaptureViewModel` coordinating `QuickCaptureVoiceRecorder`, `DeterministicNlpCommandParser`, and `CaptureQuickThoughtUseCase` for 3-tier routing.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/` -- Create `CompanionUiState`, `CompanionUiIntent`, `CompanionUiEffect`, and `CompanionViewModel` observing `EconomyRepository` and `GamificationLedgerEngine`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/companion/CompanionScreen.kt` -- Refactor to `CompanionRoute` and `CompanionScreenContent` (100% stateless).
- [x] `app/src/main/java/com/personaltracker/ui/screens/habits/` -- Create `HabitsUiState`, `HabitsUiIntent`, `HabitsUiEffect`, `HabitsViewModel`, and refactor `HabitsScreen.kt` to `HabitsRoute` + `HabitsScreenContent`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/study/` -- Create `StudyUiState`, `StudyUiIntent`, `StudyUiEffect`, `StudyViewModel`, and refactor `StudyScreen.kt` to `StudyRoute` + `StudyScreenContent` wired to `StudySessionForegroundService.timerState`.
- [x] `app/src/main/java/com/personaltracker/ui/screens/settings/` -- Create `SettingsUiState`, `SettingsUiIntent`, `SettingsUiEffect`, `SettingsViewModel`, and refactor `SettingsScreen.kt` to `SettingsRoute` + `SettingsScreenContent`.
- [x] `app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt` & `PersonalTrackerNavHost.kt` -- Wire navigation graph and scaffold top context bar / quick capture sheet to ViewModels.
- [x] `app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt` -- Unit-test `HomeViewModel` state emissions, hero card state priority resolution, and task/habit completion rewards.
- [x] `app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt` -- Unit-test 3-tier confidence routing (High 5s undo, Medium confirmation chips, Low unfiled inbox) and audio streaming RMS.
- [x] `app/src/test/java/com/personaltracker/ui/CompanionViewModelTest.kt` -- Unit-test mood progression, shop item purchase, milestone trigger, and zero punitive state changes.
- [x] `app/src/test/java/com/personaltracker/ui/HabitsAndStudyViewModelTest.kt` -- Unit-test habit streak tracking and study session timer state binding.

**Acceptance Criteria:**
- Given the 5 feature screens (`Home`, `Habits`, `Study`, `Companion`, `Settings`), when wired with Hilt MVI, then each screen exposes `*Route` collecting ViewModel state and a 100% stateless `*ScreenContent(state, onIntent)`.
- Given `HomeViewModel`, when repository flows emit updates, then `HeroActionCard` evaluates its 6 states accurately and daily actions update reactively.
- Given `QuickCaptureViewModel`, when audio or text is captured, then High confidence commits with 5s undo, Medium provides 5 confirmation chips, and Low stores to unfiled inbox without loss.
- Given `CompanionViewModel`, when coin rewards or pet interactions occur, then 7 non-punitive emotional registers and level progression update with zero guilt copy.
- Given ViewModel unit test suite, when `.\gradlew test` and `.\gradlew lintDebug` are run, then all tests pass with 0 errors.

## Spec Change Log

## Design Notes

- **Figma-Ready Split (AD-2)**:
```kotlin
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigate: (PersonalTrackerDestination) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowCoinReward -> rewardMotionStage.dispatchCoinReward(effect.startOffset, effect.coinCount)
                is HomeUiEffect.ShowLottieReward -> rewardMotionStage.dispatchLottieReward(effect.type)
                is HomeUiEffect.NavigateTo -> onNavigate(effect.destination)
            }
        }
    }

    HomeScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

## Verification

**Commands:**
- `.\gradlew test` -- expected: BUILD SUCCESSFUL (all ViewModel and repository unit tests passing)
- `.\gradlew lintDebug` -- expected: BUILD SUCCESSFUL (0 errors, strict accessibility and token compliance)
- `python .agents/scripts/domain_purity_check.py` -- expected: 0 forbidden imports
- `python .agent/skills/bmad-architecture/scripts/lint_spine.py --workspace .` -- expected: ok: true, total_findings: 0

## Suggested Review Order

**MVI Feature ViewModels & Contracts**

- Home priority state machine and task/habit completion rewards
  [`HomeViewModel.kt:45`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeViewModel.kt#L45)

- 3-tier confidence routing with 5s undo toast and RMS audio streaming
  [`QuickCaptureViewModel.kt:35`](../../app/src/main/java/com/personaltracker/ui/screens/quickcapture/QuickCaptureViewModel.kt#L35)

- Non-punishing 7 emotional registers, shop purchasing, and level progression
  [`CompanionViewModel.kt:35`](../../app/src/main/java/com/personaltracker/ui/screens/companion/CompanionViewModel.kt#L35)

- Study session timer state synchronization with foreground service
  [`StudyViewModel.kt:30`](../../app/src/main/java/com/personaltracker/ui/screens/study/StudyViewModel.kt#L30)

**Figma-Ready Stateless Screen Split (AD-2)**

- HomeRoute stateful Hilt collector and HomeScreenContent 100% stateless renderer
  [`HomeScreen.kt:40`](../../app/src/main/java/com/personaltracker/ui/screens/home/HomeScreen.kt#L40)

- CompanionRoute and CompanionScreenContent decoupled renderer
  [`CompanionScreen.kt:35`](../../app/src/main/java/com/personaltracker/ui/screens/companion/CompanionScreen.kt#L35)

**App Navigation & Shell Container**

- Persistent top bar context aggregation combining DayType, streaks, and coins
  [`AppShellViewModel.kt:30`](../../app/src/main/java/com/personaltracker/ui/navigation/AppShellViewModel.kt#L30)

- Global AppShellScaffold hosting quick capture and snackbar alerts
  [`AppShellScaffold.kt:65`](../../app/src/main/java/com/personaltracker/ui/navigation/AppShellScaffold.kt#L65)

- NavHost binding destinations to stateful MVI routes
  [`PersonalTrackerNavHost.kt:55`](../../app/src/main/java/com/personaltracker/ui/navigation/PersonalTrackerNavHost.kt#L55)

**Test Coverage & Edge Cases**

- Reactive top bar context aggregation verification
  [`AppShellViewModelTest.kt:25`](../../app/src/test/java/com/personaltracker/ui/AppShellViewModelTest.kt#L25)

- 3-tier NLP confidence routing and undo verification
  [`QuickCaptureViewModelTest.kt:40`](../../app/src/test/java/com/personaltracker/ui/QuickCaptureViewModelTest.kt#L40)

- Hero card priority hierarchy and coin reward emission
  [`HomeViewModelTest.kt:45`](../../app/src/test/java/com/personaltracker/ui/HomeViewModelTest.kt#L45)
