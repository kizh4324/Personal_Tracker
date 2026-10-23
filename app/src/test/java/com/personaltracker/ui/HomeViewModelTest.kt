package com.personaltracker.ui

import androidx.compose.ui.geometry.Offset
import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.SwitchDayTypeUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.screens.home.HomeUiEffect
import com.personaltracker.ui.screens.home.HomeUiIntent
import com.personaltracker.ui.screens.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Field
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var taskRepository: TestTaskRepository
    private lateinit var habitRepository: TestHabitRepository
    private lateinit var routineRepository: TestRoutineRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var dayTypeRepository: TestDayTypeRepository

    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var streakMachine: StreakStateMachine
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var switchDayTypeUseCase: SwitchDayTypeUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var logHabitUseCase: LogHabitUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        taskRepository = TestTaskRepository()
        habitRepository = TestHabitRepository()
        routineRepository = TestRoutineRepository()
        economyRepository = TestEconomyRepository()
        dayTypeRepository = TestDayTypeRepository()

        adaptiveEngine = AdaptiveScheduleEngine()
        streakMachine = StreakStateMachine()
        ledgerEngine = GamificationLedgerEngine()

        resolveDayTypeUseCase = ResolveDayTypeUseCase(dayTypeRepository, adaptiveEngine)
        switchDayTypeUseCase = SwitchDayTypeUseCase(
            dayTypeRepository = dayTypeRepository,
            routineRepository = routineRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            adaptiveScheduleEngine = adaptiveEngine
        )
        completeTaskUseCase = CompleteTaskUseCase(taskRepository, economyRepository, ledgerEngine)
        logHabitUseCase = LogHabitUseCase(habitRepository, economyRepository, streakMachine, ledgerEngine)
        resetTimerState()
    }

    @After
    fun tearDown() {
        resetTimerState()
    }

    private fun resetTimerState() {
        val field: Field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>
        serviceTimerState.value = StudySessionTimerState()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            routineRepository = routineRepository,
            economyRepository = economyRepository,
            dayTypeRepository = dayTypeRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            switchDayTypeUseCase = switchDayTypeUseCase,
            completeTaskUseCase = completeTaskUseCase,
            logHabitUseCase = logHabitUseCase,
            streakStateMachine = streakMachine
        )
    }

    @Test
    fun testInitialUiStateEmitsDefaultValues() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Standard Routine", state.activeDayTypeName)
            assertEquals(0, state.coinBalance)
            assertFalse(state.isDayOverrideActive)
            assertFalse(state.isDayTypeSelectorOpen)
            assertTrue(state.heroCardState is HeroCardState.MorningBanner)
        }
    }

    @Test
    fun testDayTypeSelectorOpenAndDismissIntents() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isDayTypeSelectorOpen)

            // Open dialog
            viewModel.onIntent(HomeUiIntent.OpenDayTypeSelector)
            val openState = awaitItem()
            assertTrue(openState.isDayTypeSelectorOpen)

            // Dismiss dialog
            viewModel.onIntent(HomeUiIntent.DismissDayTypeSelector)
            val dismissedState = awaitItem()
            assertFalse(dismissedState.isDayTypeSelectorOpen)

            // Open via DayTypeClicked
            viewModel.onIntent(HomeUiIntent.DayTypeClicked)
            val clickedState = awaitItem()
            assertTrue(clickedState.isDayTypeSelectorOpen)

            viewModel.onIntent(HomeUiIntent.DismissDayTypeSelector)
            val dismissedState2 = awaitItem()
            assertFalse(dismissedState2.isDayTypeSelectorOpen)
        }
    }

    @Test
    fun testSelectDayTypeAppliesOverrideAndEmitsEffect() = runTest {
        dayTypeRepository.saveDayTypes(DayType.BUILT_IN_DAY_TYPES)
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.SelectDayType("dt_rest"))

            val effect1 = awaitItem()
            assertTrue(effect1 is HomeUiEffect.DayTypeChanged)
            assertEquals("Rest / Recovery", (effect1 as HomeUiEffect.DayTypeChanged).dayTypeName)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowSnackbar)
            assertTrue((effect2 as HomeUiEffect.ShowSnackbar).message.contains("Rest well today"))
        }

        // Verify state update
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Rest / Recovery", state.activeDayTypeName)
            assertTrue(state.isDayOverrideActive)
            assertFalse(state.isDayTypeSelectorOpen)
        }
    }

    @Test
    fun testResetDayTypeOverrideRevertsToDefault() = runTest {
        dayTypeRepository.saveDayTypes(DayType.BUILT_IN_DAY_TYPES)
        dayTypeRepository.setOverrideForDate(
            com.personaltracker.domain.model.DayOverride(
                date = today,
                dayTypeId = "dt_crunch",
                isUserModified = true,
                createdTimestamp = System.currentTimeMillis()
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Reset override
        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ResetDayTypeOverride)

            val effect1 = awaitItem()
            assertTrue(effect1 is HomeUiEffect.DayTypeChanged)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowSnackbar)
            assertTrue((effect2 as HomeUiEffect.ShowSnackbar).message.contains("reset"))
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isDayOverrideActive)
            assertEquals("Standard Routine", state.activeDayTypeName)
        }
    }

    @Test
    fun testHeroCardPriorityEvaluationHierarchy() = runTest {
        // 1. Initially MorningBanner (priority 3)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val s1 = awaitItem()
            assertTrue(s1.heroCardState is HeroCardState.MorningBanner)

            // 2. Dismiss morning banner -> drops to IntentionalIdle (priority 6) if no tasks
            viewModel.onIntent(HomeUiIntent.DismissMorningBanner)
            val s2 = awaitItem()
            assertTrue(s2.heroCardState is HeroCardState.IntentionalIdle)
        }
    }

    @Test
    fun testHeroCardUpcomingTaskPriority() = runTest {
        val pendingTask = Task(
            id = "t1",
            title = "Algorithm Review",
            scheduledDate = today,
            state = "PENDING",
            estimatedDurationMinutes = 30
        )
        taskRepository.saveTask(pendingTask)

        val viewModel = createViewModel()
        viewModel.onIntent(HomeUiIntent.DismissMorningBanner)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.heroCardState is HeroCardState.UpcomingItem)
            val upcoming = state.heroCardState as HeroCardState.UpcomingItem
            assertEquals("Algorithm Review", upcoming.title)
        }
    }

    @Test
    fun testTaskCompletionEmitsRewardEffects() = runTest {
        val task = Task(
            id = "task_1",
            title = "Build MVI Screens",
            scheduledDate = today,
            state = "PENDING",
            deliveryIntensity = "URGENT"
        )
        taskRepository.saveTask(task)
        economyRepository.profileFlow.value = CompanionProfile(speciesName = "Sproutling", level = 1)

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ToggleTask("task_1", Offset(100f, 200f)))

            val effect1 = awaitItem()
            assertTrue(effect1 is HomeUiEffect.ShowCoinReward)
            val coinReward = effect1 as HomeUiEffect.ShowCoinReward
            assertEquals(Offset(100f, 200f), coinReward.startOffset)
            assertEquals(10, coinReward.coinCount)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect2 as HomeUiEffect.ShowLottieReward).type)
        }
    }

    @Test
    fun testHabitCompletionEmitsRewardEffects() = runTest {
        val habit = Habit(
            id = "habit_meditate",
            name = "Morning Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 3
        )
        habitRepository.saveHabit(habit)
        economyRepository.profileFlow.value = CompanionProfile(speciesName = "Sproutling", level = 1)

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ToggleHabit("habit_meditate", Offset(50f, 150f)))

            val effect1 = awaitItem()
            assertTrue(effect1 is HomeUiEffect.ShowCoinReward)
            val coinReward = effect1 as HomeUiEffect.ShowCoinReward
            assertEquals(5, coinReward.coinCount)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect2 as HomeUiEffect.ShowLottieReward).type)
        }
    }
}
