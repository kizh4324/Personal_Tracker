package com.personaltracker.ui.screens.home

import app.cash.turbine.test
import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.usecase.AdvanceRoutineStepUseCase
import com.personaltracker.domain.usecase.CarryForwardTasksUseCase
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.DeleteTaskUseCase
import com.personaltracker.domain.usecase.DismissOverdueTasksUseCase
import com.personaltracker.domain.usecase.EvaluateTaskUrgencyScarcityUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.RecalculateRoutineTimelineUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ResolveHeroStateUseCase
import com.personaltracker.domain.usecase.SaveRoutineUseCase
import com.personaltracker.domain.usecase.SaveTaskUseCase
import com.personaltracker.domain.usecase.SwitchDayTypeUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.MainDispatcherRule
import com.personaltracker.ui.TestDayTypeRepository
import com.personaltracker.ui.TestEconomyRepository
import com.personaltracker.ui.TestHabitRepository
import com.personaltracker.ui.TestRoutineRepository
import com.personaltracker.ui.TestTaskAlarmScheduler
import com.personaltracker.ui.TestTaskRepository
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Field
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelRoutineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var taskRepository: TestTaskRepository
    private lateinit var habitRepository: TestHabitRepository
    private lateinit var routineRepository: TestRoutineRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var taskAlarmScheduler: TestTaskAlarmScheduler

    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var streakMachine: StreakStateMachine
    private lateinit var ledgerEngine: GamificationLedgerEngine

    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var switchDayTypeUseCase: SwitchDayTypeUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var logHabitUseCase: LogHabitUseCase
    private lateinit var resolveHeroStateUseCase: ResolveHeroStateUseCase
    private lateinit var carryForwardTasksUseCase: CarryForwardTasksUseCase
    private lateinit var dismissOverdueTasksUseCase: DismissOverdueTasksUseCase
    private lateinit var saveTaskUseCase: SaveTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var evaluateTaskUrgencyScarcityUseCase: EvaluateTaskUrgencyScarcityUseCase
    private lateinit var advanceRoutineStepUseCase: AdvanceRoutineStepUseCase
    private lateinit var recalculateRoutineTimelineUseCase: RecalculateRoutineTimelineUseCase
    private lateinit var saveRoutineUseCase: SaveRoutineUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        taskRepository = TestTaskRepository()
        habitRepository = TestHabitRepository()
        routineRepository = TestRoutineRepository()
        economyRepository = TestEconomyRepository()
        dayTypeRepository = TestDayTypeRepository()
        taskAlarmScheduler = TestTaskAlarmScheduler()

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
        completeTaskUseCase = CompleteTaskUseCase(taskRepository, economyRepository, ledgerEngine, taskAlarmScheduler)
        logHabitUseCase = LogHabitUseCase(habitRepository, economyRepository, streakMachine, ledgerEngine)
        resolveHeroStateUseCase = ResolveHeroStateUseCase()
        carryForwardTasksUseCase = CarryForwardTasksUseCase(taskRepository)
        dismissOverdueTasksUseCase = DismissOverdueTasksUseCase(taskRepository)
        saveTaskUseCase = SaveTaskUseCase(taskRepository, taskAlarmScheduler)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository, taskAlarmScheduler)
        evaluateTaskUrgencyScarcityUseCase = EvaluateTaskUrgencyScarcityUseCase(taskRepository)

        advanceRoutineStepUseCase = AdvanceRoutineStepUseCase(
            routineRepository = routineRepository,
            economyRepository = economyRepository,
            ledgerEngine = ledgerEngine
        )
        recalculateRoutineTimelineUseCase = RecalculateRoutineTimelineUseCase(
            adaptiveScheduleEngine = adaptiveEngine
        )
        saveRoutineUseCase = SaveRoutineUseCase(
            routineRepository = routineRepository
        )

        economyRepository.profileFlow.value = CompanionProfile(
            id = 1,
            speciesName = "Gryph",
            level = 1,
            currentEnergyUnits = 0,
            currentExpressionOrdinal = CompanionMood.IDLE_CONTENT.ordinalValue
        )

        dayTypeRepository.overridesFlow.value = mapOf(
            today to com.personaltracker.domain.model.DayOverride(
                date = today,
                dayTypeId = DayType.STANDARD.id,
                isUserModified = true,
                createdTimestamp = System.currentTimeMillis()
            )
        )

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
            streakStateMachine = streakMachine,
            resolveHeroStateUseCase = resolveHeroStateUseCase,
            carryForwardTasksUseCase = carryForwardTasksUseCase,
            dismissOverdueTasksUseCase = dismissOverdueTasksUseCase,
            saveTaskUseCase = saveTaskUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            evaluateTaskUrgencyScarcityUseCase = evaluateTaskUrgencyScarcityUseCase,
            advanceRoutineStepUseCase = advanceRoutineStepUseCase,
            recalculateRoutineTimelineUseCase = recalculateRoutineTimelineUseCase,
            saveRoutineUseCase = saveRoutineUseCase
        )
    }

    @Test
    fun testRoutineTimelineStateInitializedWithDefaultRoutine() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.activeRoutine)
            assertEquals("routine_standard_morning", state.activeRoutine?.id)
            assertEquals(3, state.routineTimelineSteps.size)
            assertFalse(state.isRoutineCompletedToday)
            assertEquals("Step 1 of 3 • 25 min", state.routineProgressSummary)

            // Step 1 is active cursor
            val step1 = state.routineTimelineSteps[0]
            assertEquals(RoutineStepStatus.ACTIVE, step1.status)
            assertTrue(step1.isCurrentCursor)
            assertEquals("08:00 - 08:05", step1.timeSlot)

            // Step 2 is upcoming
            val step2 = state.routineTimelineSteps[1]
            assertEquals(RoutineStepStatus.UPCOMING, step2.status)
            assertFalse(step2.isCurrentCursor)

            // Active step cursor in state
            assertEquals("step_std_1", state.activeRoutineStep?.step?.id)
        }
    }

    @Test
    fun testAdvanceRoutineStepAdvancesCursorAndUpdatesProgress() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("Step 1 of 3 • 25 min", initial.routineProgressSummary)

            viewModel.onIntent(HomeUiIntent.AdvanceRoutineStep("step_std_1"))

            val advancedState = awaitItem()
            assertEquals("Step 2 of 3 • 25 min", advancedState.routineProgressSummary)
            assertEquals(RoutineStepStatus.COMPLETED, advancedState.routineTimelineSteps[0].status)
            assertEquals(RoutineStepStatus.ACTIVE, advancedState.routineTimelineSteps[1].status)
            assertEquals("step_std_2", advancedState.activeRoutineStep?.step?.id)
        }
    }

    @Test
    fun testCompletingRoutineEmitsCoinRewardAndConfettiCelebration() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.AdvanceRoutineStep("step_std_1"))
            val snack1 = awaitItem()
            assertTrue(snack1 is HomeUiEffect.ShowSnackbar)

            viewModel.onIntent(HomeUiIntent.AdvanceRoutineStep("step_std_2"))
            val snack2 = awaitItem()
            assertTrue(snack2 is HomeUiEffect.ShowSnackbar)

            // Final step completion
            viewModel.onIntent(HomeUiIntent.AdvanceRoutineStep("step_std_3"))

            val coinEffect = awaitItem()
            assertTrue(coinEffect is HomeUiEffect.ShowCoinReward)
            assertEquals(15, (coinEffect as HomeUiEffect.ShowCoinReward).coinCount)

            val lottieEffect = awaitItem()
            assertTrue(lottieEffect is HomeUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.ConfettiCelebration, (lottieEffect as HomeUiEffect.ShowLottieReward).type)

            val snack3 = awaitItem()
            assertTrue(snack3 is HomeUiEffect.ShowSnackbar)
            assertTrue((snack3 as HomeUiEffect.ShowSnackbar).message.contains("15 coins"))
        }

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertTrue(finalState.isRoutineCompletedToday)
            assertEquals("Completed • 3 steps (25 min)", finalState.routineProgressSummary)
        }
    }

    @Test
    fun testSkipRoutineStepAdvancesCursorWithoutAwardingStepCoins() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.onIntent(HomeUiIntent.SkipRoutineStep("step_std_1"))

            val skippedState = awaitItem()
            assertEquals("Step 2 of 3 • 25 min", skippedState.routineProgressSummary)
            assertEquals("step_std_2", skippedState.activeRoutineStep?.step?.id)
        }
    }

    @Test
    fun testShiftRoutineToNowIntentEmitsSnackbar() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ShiftRoutineToNow)
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Routine shifted to now. Moving forward smoothly.", (effect as HomeUiEffect.ShowSnackbar).message)
        }
    }

    @Test
    fun testResetRoutineProgressResetsStepsBackToInitial() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onIntent(HomeUiIntent.AdvanceRoutineStep("step_std_1"))
            val advanced = awaitItem()
            assertEquals("Step 2 of 3 • 25 min", advanced.routineProgressSummary)

            viewModel.onIntent(HomeUiIntent.ResetRoutineProgress)
            val resetState = awaitItem()
            assertEquals("Step 1 of 3 • 25 min", resetState.routineProgressSummary)
            assertEquals(RoutineStepStatus.ACTIVE, resetState.routineTimelineSteps[0].status)
        }
    }

    @Test
    fun testHeroActionCardPrioritizesActiveRoutineStepWhenNoFocusTasksPending() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            // Morning banner initially active
            assertTrue(initial.heroCardState is HeroCardState.MorningBanner)

            // Dismiss morning banner
            viewModel.onIntent(HomeUiIntent.DismissMorningBanner)

            val heroUpdated = awaitItem()
            // Hero card transitions to UpcomingItem displaying the active routine step
            assertTrue(heroUpdated.heroCardState is HeroCardState.UpcomingItem)
            val upcoming = heroUpdated.heroCardState as HeroCardState.UpcomingItem
            assertEquals("Hydrate & gentle stretch", upcoming.title)
        }
    }
}
