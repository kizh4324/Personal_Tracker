package com.personaltracker.ui

import androidx.compose.ui.geometry.Offset
import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.CarryForwardTasksUseCase
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.DeleteTaskUseCase
import com.personaltracker.domain.usecase.DismissOverdueTasksUseCase
import com.personaltracker.domain.usecase.EvaluateTaskUrgencyScarcityUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ResolveHeroStateUseCase
import com.personaltracker.domain.usecase.ResumeInterruptedSessionUseCase
import com.personaltracker.domain.usecase.SaveTaskUseCase
import com.personaltracker.domain.usecase.SwitchDayTypeUseCase
import com.personaltracker.domain.usecase.UrgencyScarcityTier
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
    private lateinit var studyRepository: TestStudySessionRepository
    private lateinit var resumeInterruptedSessionUseCase: ResumeInterruptedSessionUseCase

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
    private lateinit var taskAlarmScheduler: TestTaskAlarmScheduler
    private lateinit var saveTaskUseCase: SaveTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var evaluateTaskUrgencyScarcityUseCase: EvaluateTaskUrgencyScarcityUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        taskRepository = TestTaskRepository()
        habitRepository = TestHabitRepository()
        routineRepository = TestRoutineRepository()
        economyRepository = TestEconomyRepository()
        dayTypeRepository = TestDayTypeRepository()
        taskAlarmScheduler = TestTaskAlarmScheduler()
        studyRepository = TestStudySessionRepository()
        resumeInterruptedSessionUseCase = ResumeInterruptedSessionUseCase(studyRepository, FocusRescheduleEngine())

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
        saveTaskUseCase = SaveTaskUseCase(taskRepository, taskAlarmScheduler, mainDispatcherRule.testDispatcher)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository, taskAlarmScheduler, mainDispatcherRule.testDispatcher)
        evaluateTaskUrgencyScarcityUseCase = EvaluateTaskUrgencyScarcityUseCase(taskRepository)
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
            studySessionRepository = studyRepository,
            resumeInterruptedSessionUseCase = resumeInterruptedSessionUseCase
        )
    }

    @Test
    fun testInitialUiStateEmitsDefaultValues() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            val expectedDefaultDayType = resolveDayTypeUseCase.resolve(today, null, DayType.BUILT_IN_DAY_TYPES).name
            assertEquals(expectedDefaultDayType, state.activeDayTypeName)
            assertEquals(0, state.coinBalance)
            assertFalse(state.isDayOverrideActive)
            assertFalse(state.isDayTypeSelectorOpen)
            assertFalse(state.isCarryForwardDialogOpen)
            assertTrue(state.heroCardState is HeroCardState.MorningBanner)
        }
    }

    @Test
    fun testHeroCardPriority1ActiveFocusWhenTimerRunning() = runTest {
        val field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>
        serviceTimerState.value = StudySessionTimerState(
            sessionId = "session_1",
            subjectTag = "Deep Work",
            totalSeconds = 1500L,
            remainingSeconds = 1200L,
            isRunning = true,
            isPaused = false
        )

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.heroCardState is HeroCardState.ActiveFocus)
            val focus = state.heroCardState as HeroCardState.ActiveFocus
            assertEquals("Deep Work Session", focus.title)
            assertEquals("20:00", focus.countdownFormatted)
        }
    }

    @Test
    fun testHeroCardPriority2InterruptedResumableWhenTimerPaused() = runTest {
        val field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>
        serviceTimerState.value = StudySessionTimerState(
            sessionId = "session_2",
            subjectTag = "Algorithms",
            totalSeconds = 1800L,
            remainingSeconds = 900L,
            isRunning = false,
            isPaused = true
        )

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.heroCardState is HeroCardState.InterruptedResumable)
            val interrupted = state.heroCardState as HeroCardState.InterruptedResumable
            assertEquals("Algorithms Session", interrupted.title)

            // Assert onReview emits calming snackbar
            viewModel.uiEffect.test {
                interrupted.onReview()
                val snackbar = awaitItem()
                assertTrue(snackbar is HomeUiEffect.ShowSnackbar)
                assertEquals("Take your time. Resume whenever you're ready.", (snackbar as HomeUiEffect.ShowSnackbar).message)
            }
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
            val expectedDefaultDayType = resolveDayTypeUseCase.resolve(today, null, DayType.BUILT_IN_DAY_TYPES).name
            assertEquals(expectedDefaultDayType, state.activeDayTypeName)
        }
    }

    @Test
    fun testHeroCardPriorityEvaluationHierarchy() = runTest {
        // 1. Initially MorningBanner (priority 3)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val s1 = awaitItem()
            assertTrue(s1.heroCardState is HeroCardState.MorningBanner)

            // 2. Dismiss morning banner -> shows active routine step as UpcomingItem (priority 4)
            viewModel.onIntent(HomeUiIntent.DismissMorningBanner)
            val s2 = awaitItem()
            assertTrue(s2.heroCardState is HeroCardState.UpcomingItem)

            // 3. Snooze active routine step -> drops to IntentionalIdle (priority 6) if no tasks
            val routineStepId = s2.activeRoutineStep?.step?.id ?: "step_std_1"
            viewModel.onIntent(HomeUiIntent.SnoozeHeroItem(routineStepId))
            val s3 = awaitItem()
            assertTrue(s3.heroCardState is HeroCardState.IntentionalIdle)
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
    fun testSnoozeHeroItemPromotesNextTask() = runTest {
        val task1 = Task(id = "t1", title = "Task 1", scheduledDate = today, state = "PENDING")
        val task2 = Task(id = "t2", title = "Task 2", scheduledDate = today, state = "PENDING")
        taskRepository.saveTasks(listOf(task1, task2))

        val viewModel = createViewModel()
        viewModel.onIntent(HomeUiIntent.DismissMorningBanner)

        viewModel.uiState.test {
            val state1 = awaitItem()
            assertTrue(state1.heroCardState is HeroCardState.UpcomingItem)
            assertEquals("Task 1", (state1.heroCardState as HeroCardState.UpcomingItem).title)

            // Snooze Task 1 -> Immediately promotes Task 2
            viewModel.onIntent(HomeUiIntent.SnoozeHeroItem("t1"))
            val state2 = awaitItem()
            assertTrue(state2.heroCardState is HeroCardState.UpcomingItem)
            assertEquals("Task 2", (state2.heroCardState as HeroCardState.UpcomingItem).title)
        }
    }

    @Test
    fun testStartHeroItemCompletesTaskAndEmitsEffects() = runTest {
        val task = Task(
            id = "t_hero",
            title = "Implement Feature",
            scheduledDate = today,
            state = "PENDING"
        )
        taskRepository.saveTask(task)
        economyRepository.profileFlow.value = CompanionProfile(speciesName = "Sproutling", level = 1)

        val viewModel = createViewModel()
        viewModel.onIntent(HomeUiIntent.DismissMorningBanner)

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.StartHeroItem("t_hero", Offset(10f, 20f)))

            val effect1 = awaitItem()
            assertTrue(effect1 is HomeUiEffect.ShowCoinReward)
            assertEquals(Offset(10f, 20f), (effect1 as HomeUiEffect.ShowCoinReward).startOffset)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowLottieReward)
        }
    }

    @Test
    fun testCarryForwardDialogControls() = runTest {
        val overdue = Task(id = "ov1", title = "Past task", scheduledDate = "2026-10-20", state = "PENDING")
        taskRepository.saveTask(overdue)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isCarryForwardDialogOpen)

            // Open dialog
            viewModel.onIntent(HomeUiIntent.OpenCarryForwardReview)
            val opened = awaitItem()
            assertTrue(opened.isCarryForwardDialogOpen)

            // Dismiss dialog
            viewModel.onIntent(HomeUiIntent.DismissCarryForwardDialog)
            val dismissed = awaitItem()
            assertFalse(dismissed.isCarryForwardDialogOpen)
        }
    }

    @Test
    fun testCarryForwardAllOverdueUpdatesTasksAndClosesDialog() = runTest {
        val overdue = Task(id = "ov1", title = "Past task", scheduledDate = "2026-10-20", state = "PENDING")
        taskRepository.saveTask(overdue)

        val viewModel = createViewModel()
        viewModel.onIntent(HomeUiIntent.DismissMorningBanner)
        advanceUntilIdle()

        viewModel.uiState.test {
            val s0 = awaitItem()
            s0.activeRoutineStep?.step?.id?.let { stepId ->
                viewModel.onIntent(HomeUiIntent.SnoozeHeroItem(stepId))
            }
            val stateWithOverdue = if (s0.heroCardState is HeroCardState.CarryForwardReview) s0 else awaitItem()
            assertTrue(stateWithOverdue.heroCardState is HeroCardState.CarryForwardReview)
            assertEquals(1, stateWithOverdue.overdueTasks.size)

            viewModel.onIntent(HomeUiIntent.OpenCarryForwardReview)
            val dialogOpened = awaitItem()
            assertTrue(dialogOpened.isCarryForwardDialogOpen)

            viewModel.uiEffect.test {
                viewModel.onIntent(HomeUiIntent.CarryForwardAllOverdue)

                val snackbar = awaitItem()
                assertTrue(snackbar is HomeUiEffect.ShowSnackbar)
                assertTrue((snackbar as HomeUiEffect.ShowSnackbar).message.contains("Carried forward"))
            }

            // First emission on dialog close
            val stateDialogClosed = awaitItem()
            assertFalse(stateDialogClosed.isCarryForwardDialogOpen)

            // Second emission after task update persists in repository
            val stateAfterCarry = awaitItem()
            assertFalse(stateAfterCarry.isCarryForwardDialogOpen)
            assertTrue(stateAfterCarry.overdueTasks.isEmpty())
            assertTrue(stateAfterCarry.heroCardState !is HeroCardState.CarryForwardReview)
        }

        val updated = taskRepository.getTaskById("ov1")
        assertEquals(today, updated?.scheduledDate)
        assertTrue(updated?.isCarryForward == true)
    }

    @Test
    fun testDismissAllOverdueCancelsTasksAndClosesDialog() = runTest {
        val overdue = Task(id = "ov2", title = "Skip task", scheduledDate = "2026-10-20", state = "PENDING")
        taskRepository.saveTask(overdue)

        val viewModel = createViewModel()
        viewModel.onIntent(HomeUiIntent.DismissMorningBanner)
        advanceUntilIdle()

        viewModel.uiState.test {
            val s0 = awaitItem()
            s0.activeRoutineStep?.step?.id?.let { stepId ->
                viewModel.onIntent(HomeUiIntent.SnoozeHeroItem(stepId))
            }
            val stateWithOverdue = if (s0.heroCardState is HeroCardState.CarryForwardReview) s0 else awaitItem()
            assertTrue(stateWithOverdue.heroCardState is HeroCardState.CarryForwardReview)
            assertEquals(1, stateWithOverdue.overdueTasks.size)

            viewModel.onIntent(HomeUiIntent.OpenCarryForwardReview)
            val dialogOpened = awaitItem()
            assertTrue(dialogOpened.isCarryForwardDialogOpen)

            viewModel.uiEffect.test {
                viewModel.onIntent(HomeUiIntent.DismissAllOverdue)

                val snackbar = awaitItem()
                assertTrue(snackbar is HomeUiEffect.ShowSnackbar)
                assertTrue((snackbar as HomeUiEffect.ShowSnackbar).message.contains("Tasks set aside"))
            }

            // First emission on dialog close
            val stateDialogClosed = awaitItem()
            assertFalse(stateDialogClosed.isCarryForwardDialogOpen)

            // Second emission after task dismissal persists in repository
            val stateAfterDismiss = awaitItem()
            assertFalse(stateAfterDismiss.isCarryForwardDialogOpen)
            assertTrue(stateAfterDismiss.overdueTasks.isEmpty())
            assertTrue(stateAfterDismiss.heroCardState is HeroCardState.IntentionalIdle)
        }

        val cancelled = taskRepository.getTaskById("ov2")
        assertEquals("CANCELLED", cancelled?.state)
    }

    @Test
    fun testEmptyOverdueReturnsEarlyWithoutSnackbar() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.CarryForwardAllOverdue)
            viewModel.onIntent(HomeUiIntent.DismissAllOverdue)
            expectNoEvents()
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
            assertEquals(7, coinReward.coinCount)

            val effect2 = awaitItem()
            assertTrue(effect2 is HomeUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect2 as HomeUiEffect.ShowLottieReward).type)
        }
    }

    // =========================================================================
    // Story 1.10: Task Management & Delivery Intensity Tests
    // =========================================================================

    @Test
    fun testOpenTaskEditorForNewTaskSetsEditorOpenAndNullTask() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isTaskEditorOpen)
            org.junit.Assert.assertNull(initial.editingTask)

            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))

            val opened = awaitItem()
            assertTrue(opened.isTaskEditorOpen)
            org.junit.Assert.assertNull(opened.editingTask)
        }
    }

    @Test
    fun testOpenTaskEditorForExistingTaskSetsEditingTask() = runTest {
        val task = Task(
            id = "t_edit_1",
            title = "Edit Architecture Spec",
            scheduledDate = today,
            deliveryIntensity = "IMPORTANT"
        )
        taskRepository.saveTask(task)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isTaskEditorOpen)

            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(task))

            val opened = awaitItem()
            assertTrue(opened.isTaskEditorOpen)
            org.junit.Assert.assertNotNull(opened.editingTask)
            assertEquals("t_edit_1", opened.editingTask?.id)
            assertEquals("Edit Architecture Spec", opened.editingTask?.title)
        }
    }

    @Test
    fun testDismissTaskEditorClearsEditorState() = runTest {
        val task = Task(id = "t_dismiss", title = "Dismiss test", scheduledDate = today)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isTaskEditorOpen)

            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(task))
            val opened = awaitItem()
            assertTrue(opened.isTaskEditorOpen)

            viewModel.onIntent(HomeUiIntent.DismissTaskEditor)
            val dismissed = awaitItem()
            assertFalse(dismissed.isTaskEditorOpen)
            org.junit.Assert.assertNull(dismissed.editingTask)
        }
    }

    @Test
    fun testSaveTaskItemPersistsTaskAndClosesEditorAndEmitsSnackbar() = runTest {
        saveTaskUseCase.clock = java.time.Clock.fixed(
            java.time.Instant.parse("${today}T08:00:00Z"),
            java.time.ZoneId.of("UTC")
        )
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            val newTask = Task(
                id = "t_new",
                title = "New Core Feature",
                scheduledDate = today,
                deliveryIntensity = "URGENT",
                scheduledTime = "11:00"
            )

            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
            viewModel.onIntent(HomeUiIntent.SaveTaskItem(newTask))

            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Task saved.", (effect as HomeUiEffect.ShowSnackbar).message)
        }

        val persisted = taskRepository.getTaskById("t_new")
        org.junit.Assert.assertNotNull(persisted)
        assertEquals("New Core Feature", persisted?.title)
        assertEquals("URGENT", persisted?.deliveryIntensity)
        assertTrue(taskAlarmScheduler.cancelledAlarms.contains("t_new"))
        assertEquals(1, taskAlarmScheduler.scheduledTasks.size)
    }

    @Test
    fun testDeleteTaskItemRemovesTaskAndClosesEditorAndEmitsSnackbar() = runTest {
        val task = Task(
            id = "t_to_delete",
            title = "Delete Me",
            scheduledDate = today
        )
        taskRepository.saveTask(task)

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(task))
            viewModel.onIntent(HomeUiIntent.DeleteTaskItem("t_to_delete"))

            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Task removed.", (effect as HomeUiEffect.ShowSnackbar).message)
        }

        val deleted = taskRepository.getTaskById("t_to_delete")
        org.junit.Assert.assertNull(deleted)
        assertTrue(taskAlarmScheduler.cancelledAlarms.contains("t_to_delete"))
    }

    @Test
    fun testUrgencySoftCapEvaluationReflectedInHomeUiState() = runTest {
        // Initially 0 urgent tasks
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val s0 = awaitItem()
            assertEquals(0, s0.activeUrgentTaskCount)
            assertFalse(s0.isUrgencySoftCapReached)

            // Add 1 urgent task
            val urgent1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
            taskRepository.saveTask(urgent1)
            val s1 = awaitItem()
            assertEquals(1, s1.activeUrgentTaskCount)
            assertFalse(s1.isUrgencySoftCapReached)

            // Add 2nd urgent task -> soft cap reached (>= 2)
            val urgent2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            taskRepository.saveTask(urgent2)
            val s2 = awaitItem()
            assertEquals(2, s2.activeUrgentTaskCount)
            assertTrue(s2.isUrgencySoftCapReached)

            // Complete one urgent task -> soft cap drops below threshold
            viewModel.onIntent(HomeUiIntent.ToggleTask("u1"))
            val s3 = awaitItem()
            assertEquals(1, s3.activeUrgentTaskCount)
            assertFalse(s3.isUrgencySoftCapReached)
        }
    }

    @Test
    fun testOpenTaskEditorForUrgentTaskExcludesEditingTaskFromSoftCap() = runTest {
        val urgent1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
        val urgent2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
        taskRepository.saveTasks(listOf(urgent1, urgent2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(2, initial.activeUrgentTaskCount)
            assertTrue(initial.isUrgencySoftCapReached)

            viewModel.onIntent(HomeUiIntent.OpenTaskEditor(urgent1))

            val editingState = awaitItem()
            assertTrue(editingState.isTaskEditorOpen)
            assertEquals("u1", editingState.editingTask?.id)
            assertEquals(1, editingState.activeUrgentTaskCount)
            assertFalse(editingState.isUrgencySoftCapReached)

            viewModel.onIntent(HomeUiIntent.DismissTaskEditor)
            val dismissedState = awaitItem()
            assertEquals(2, dismissedState.activeUrgentTaskCount)
            assertTrue(dismissedState.isUrgencySoftCapReached)
        }
    }

    @Test
    fun testInterruptedStudySessionFromRoomPromotesToHeroCardPriority2() = runTest {
        val interrupted = StudySession(
            id = "sess_interrupted_db",
            subjectTag = "Kotlin Flow",
            sessionDate = today,
            startTime = "11:00",
            durationMinutes = 15,
            isInterrupted = true,
            targetDurationMinutes = 25,
            actualElapsedMinutes = 15,
            intervalType = FocusIntervalType.DEEP_WORK
        )
        studyRepository.saveSession(interrupted)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.heroCardState is HeroCardState.InterruptedResumable)
            val resumable = state.heroCardState as HeroCardState.InterruptedResumable
            assertEquals("Kotlin Flow Session", resumable.title)
            assertTrue(resumable.isStudySession)
            assertEquals(15, resumable.bankedMinutes)
            assertEquals(10, resumable.remainingMinutes)
            assertTrue(resumable.advisoryPrompt.contains("15 min banked!"))
        }
    }

    @Test
    fun testPausedTimerSessionStateAPromotesToHeroCardPriority2() = runTest {
        val field: Field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>
        serviceTimerState.value = StudySessionTimerState(
            sessionId = "sess_paused_live",
            subjectTag = "Live Coding",
            totalSeconds = 30 * 60L,
            remainingSeconds = 10 * 60L,
            isRunning = false,
            isPaused = true,
            isCompleted = false
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.heroCardState is HeroCardState.InterruptedResumable)
            val resumable = state.heroCardState as HeroCardState.InterruptedResumable
            assertEquals("Live Coding Session", resumable.title)
            assertTrue(resumable.isStudySession)
            assertEquals(20, resumable.bankedMinutes)
            assertEquals(10, resumable.remainingMinutes)
        }
    }

    // =========================================================================
    // Story 5.3: Urgency Soft-Cap Scarcity Guidance Tests
    // =========================================================================

    @Test
    fun testSaveThirdUrgentTaskWithBlankDateTriggersPromptAndKeepUrgentSaves() = runTest {
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
        advanceUntilIdle()

        val thirdUrgentBlankDate = Task(
            id = "u3",
            title = "Third Urgent",
            scheduledDate = "",
            deliveryIntensity = "URGENT"
        )
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(thirdUrgentBlankDate))
        advanceUntilIdle()

        val promptedState = viewModel.uiState.value
        assertEquals(2, promptedState.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.AT_OR_OVER_CAP, promptedState.homeUrgencyScarcityTier)
        org.junit.Assert.assertNotNull(promptedState.pendingUrgencyScarcityPrompt)
        assertEquals(2, promptedState.pendingUrgencyScarcityPrompt?.activeUrgentCount)
        assertEquals(2, promptedState.pendingUrgencyScarcityPrompt?.maxActiveUrgent)
        assertTrue(promptedState.pendingUrgencyScarcityPrompt?.isTargetDateToday == true)
        assertEquals(today, promptedState.pendingUrgencyScarcityPrompt?.pendingTask?.scheduledDate)
        org.junit.Assert.assertNull(taskRepository.getTaskById("u3"))

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ResolveUrgencyScarcity(keepUrgent = true))
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Task saved as Urgent.", (effect as HomeUiEffect.ShowSnackbar).message)
        }
        advanceUntilIdle()

        val saved = taskRepository.getTaskById("u3")
        org.junit.Assert.assertNotNull(saved)
        assertEquals("URGENT", saved?.deliveryIntensity)
        assertEquals(today, saved?.scheduledDate)
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertFalse(viewModel.uiState.value.isTaskEditorOpen)
        assertEquals(3, viewModel.uiState.value.homeActiveUrgentCount)
    }

    @Test
    fun testChangeToImportantFromGuidanceDialogDowngradesAndSchedulesImportantAlarm() = runTest {
        saveTaskUseCase.clock = java.time.Clock.fixed(
            java.time.Instant.parse("${today}T08:00:00Z"),
            java.time.ZoneId.of("UTC")
        )
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(
                    id = "u3_downgrade",
                    title = "Downgraded Task",
                    scheduledDate = today,
                    scheduledTime = "11:00",
                    deliveryIntensity = "URGENT"
                )
            )
        )
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ResolveUrgencyScarcity(keepUrgent = false))
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Saved as Important.", (effect as HomeUiEffect.ShowSnackbar).message)
        }
        advanceUntilIdle()

        val saved = taskRepository.getTaskById("u3_downgrade")
        assertEquals("IMPORTANT", saved?.deliveryIntensity)
        assertEquals(1, taskAlarmScheduler.scheduledTasks.size)
        assertEquals("IMPORTANT", taskAlarmScheduler.scheduledTasks[0].deliveryIntensity)
        assertEquals(2, viewModel.uiState.value.homeActiveUrgentCount)
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertFalse(viewModel.uiState.value.isTaskEditorOpen)
    }

    @Test
    fun testDismissUrgencyScarcityPromptPreservesEditorDraftWithoutSaving() = runTest {
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(id = "u3_unsaved", title = "Unsaved Urgent", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        viewModel.onIntent(HomeUiIntent.DismissUrgencyScarcityPrompt)
        advanceUntilIdle()

        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertTrue(viewModel.uiState.value.isTaskEditorOpen)
        org.junit.Assert.assertNull(taskRepository.getTaskById("u3_unsaved"))
    }

    @Test
    fun testEditExistingActiveUrgentTaskOnSameDateSavesWithoutModalDialog() = runTest {
        val u1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
        val u2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
        taskRepository.saveTasks(listOf(u1, u2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(u1))
        advanceUntilIdle()

        // Home pill remains 2 (AT_OR_OVER_CAP) while editor sees 1 other urgent task (APPROACHING)
        assertEquals(2, viewModel.uiState.value.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.AT_OR_OVER_CAP, viewModel.uiState.value.homeUrgencyScarcityTier)
        assertEquals(1, viewModel.uiState.value.activeUrgentTaskCount)
        assertEquals(UrgencyScarcityTier.APPROACHING, viewModel.uiState.value.urgencyScarcityTier)

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.SaveTaskItem(u1.copy(title = "Urgent 1 Edited")))
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Task saved.", (effect as HomeUiEffect.ShowSnackbar).message)
        }
        advanceUntilIdle()

        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertFalse(viewModel.uiState.value.isTaskEditorOpen)
        assertEquals("Urgent 1 Edited", taskRepository.getTaskById("u1")?.title)
    }

    @Test
    fun testFutureDateScarcityEvaluationPromptsForFullFutureDateWhileTodayRemainsZero() = runTest {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        taskRepository.saveTasks(
            listOf(
                Task(id = "tm1", title = "Tomorrow 1", scheduledDate = tomorrow, deliveryIntensity = "URGENT"),
                Task(id = "tm2", title = "Tomorrow 2", scheduledDate = tomorrow, deliveryIntensity = "URGENT")
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.AVAILABLE, viewModel.uiState.value.homeUrgencyScarcityTier)

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(
                    id = "tm3",
                    title = "Tomorrow 3",
                    scheduledDate = tomorrow,
                    deliveryIntensity = "URGENT"
                )
            )
        )
        advanceUntilIdle()

        val prompt = viewModel.uiState.value.pendingUrgencyScarcityPrompt
        org.junit.Assert.assertNotNull(prompt)
        assertEquals(2, prompt?.activeUrgentCount)
        assertEquals(tomorrow, prompt?.pendingTask?.scheduledDate)
        assertFalse(prompt?.isTargetDateToday ?: true)
        assertEquals(0, viewModel.uiState.value.homeActiveUrgentCount)
    }

    @Test
    fun testCompletedOrCancelledUrgentAtCapSavesDirectlyAndDeleteReleasesSlot() = runTest {
        val u1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
        val u2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
        taskRepository.saveTasks(listOf(u1, u2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Saving a COMPLETED urgent task never opens scarcity dialog
        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(id = "u_done", title = "Done Urgent", scheduledDate = today, deliveryIntensity = "URGENT", state = "COMPLETED")
            )
        )
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertEquals(2, viewModel.uiState.value.homeActiveUrgentCount)

        // Deleting an active urgent task releases its slot immediately
        viewModel.onIntent(HomeUiIntent.DeleteTaskItem("u1"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.APPROACHING, viewModel.uiState.value.homeUrgencyScarcityTier)
    }

    @Test
    fun testUpgradeExistingRoutineTaskToUrgentAtCapTriggersPromptAndDefersSave() = runTest {
        val u1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
        val u2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
        val routine = Task(id = "r1", title = "Routine Task", scheduledDate = today, deliveryIntensity = "ROUTINE")
        taskRepository.saveTasks(listOf(u1, u2, routine))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(routine))
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(routine.copy(deliveryIntensity = "URGENT")))
        advanceUntilIdle()

        val prompt = viewModel.uiState.value.pendingUrgencyScarcityPrompt
        org.junit.Assert.assertNotNull(prompt)
        assertEquals(2, prompt?.activeUrgentCount)
        assertEquals("ROUTINE", taskRepository.getTaskById("r1")?.deliveryIntensity)
    }

    @Test
    fun testMoveExistingActiveUrgentTaskToFullFutureDateTriggersPrompt() = runTest {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val uToday = Task(id = "u_today", title = "Today Urgent", scheduledDate = today, deliveryIntensity = "URGENT")
        val tm1 = Task(id = "tm1", title = "Tomorrow 1", scheduledDate = tomorrow, deliveryIntensity = "URGENT")
        val tm2 = Task(id = "tm2", title = "Tomorrow 2", scheduledDate = tomorrow, deliveryIntensity = "URGENT")
        taskRepository.saveTasks(listOf(uToday, tm1, tm2))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(uToday))
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(uToday.copy(scheduledDate = tomorrow)))
        advanceUntilIdle()

        val prompt = viewModel.uiState.value.pendingUrgencyScarcityPrompt
        org.junit.Assert.assertNotNull(prompt)
        assertEquals(tomorrow, prompt?.pendingTask?.scheduledDate)
        assertFalse(prompt?.isTargetDateToday ?: true)
        assertEquals(today, taskRepository.getTaskById("u_today")?.scheduledDate)
    }

    @Test
    fun testSaveCancelledUrgentTaskAtCapSavesDirectlyWithoutPrompt() = runTest {
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(id = "u_cancel", title = "Cancelled Urgent", scheduledDate = today, deliveryIntensity = "URGENT", state = "CANCELLED")
            )
        )
        advanceUntilIdle()

        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        assertEquals("CANCELLED", taskRepository.getTaskById("u_cancel")?.state)
        assertEquals(2, viewModel.uiState.value.homeActiveUrgentCount)
    }

    @Test
    fun testCompleteUrgentTaskViaToggleTaskReleasesUrgentSlotReactively() = runTest {
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.AT_OR_OVER_CAP, viewModel.uiState.value.homeUrgencyScarcityTier)

        viewModel.onIntent(HomeUiIntent.ToggleTask("u1"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.homeActiveUrgentCount)
        assertEquals(UrgencyScarcityTier.APPROACHING, viewModel.uiState.value.homeUrgencyScarcityTier)
    }

    @Test
    fun testPromptClearedByEditorLifecycleAndRefreshAllowsSubsequentSave() = runTest {
        val u1 = Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT")
        val u2 = Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
        taskRepository.saveTasks(listOf(u1, u2))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val u3 = Task(id = "u3", title = "Urgent 3", scheduledDate = today, deliveryIntensity = "URGENT")

        // 1. DismissTaskEditor clears prompt
        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(null))
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(u3))
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        viewModel.onIntent(HomeUiIntent.DismissTaskEditor)
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        // 2. OpenTaskEditor clears prompt
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(u3))
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        viewModel.onIntent(HomeUiIntent.OpenTaskEditor(u1))
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        // 3. Refresh clears prompt
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(u3))
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        viewModel.onIntent(HomeUiIntent.Refresh)
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        // 4. DeleteTaskItem clears prompt and frees slot so subsequent SaveTaskItem succeeds directly
        viewModel.onIntent(HomeUiIntent.SaveTaskItem(u3))
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        viewModel.onIntent(HomeUiIntent.DeleteTaskItem("u1"))
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        viewModel.onIntent(HomeUiIntent.SaveTaskItem(u3))
        advanceUntilIdle()
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
        org.junit.Assert.assertNotNull(taskRepository.getTaskById("u3"))
    }

    @Test
    fun testFailedSaveTaskUseCaseOnDirectSaveAndPromptResolutionResetsIsSavingTask() = runTest {
        taskRepository.saveTasks(
            listOf(
                Task(id = "u1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
                Task(id = "u2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        val failingSaveRepo = object : TaskRepository by taskRepository {
            override suspend fun saveTask(task: Task) {
                throw IllegalStateException("Simulated save failure")
            }
        }
        val failingSaveUseCase = SaveTaskUseCase(
            failingSaveRepo,
            taskAlarmScheduler,
            mainDispatcherRule.testDispatcher
        )
        val viewModel = HomeViewModel(
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
            saveTaskUseCase = failingSaveUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            evaluateTaskUrgencyScarcityUseCase = evaluateTaskUrgencyScarcityUseCase,
            studySessionRepository = studyRepository,
            resumeInterruptedSessionUseCase = resumeInterruptedSessionUseCase
        )
        advanceUntilIdle()

        // 1. Direct save failure resets isSavingTask
        viewModel.uiEffect.test {
            viewModel.onIntent(
                HomeUiIntent.SaveTaskItem(
                    Task(id = "r_fail", title = "Routine Fail", scheduledDate = today, deliveryIntensity = "ROUTINE")
                )
            )
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Simulated save failure", (effect as HomeUiEffect.ShowSnackbar).message)
        }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSavingTask)

        // 2. Prompt resolution failure resets isSavingTask
        viewModel.onIntent(
            HomeUiIntent.SaveTaskItem(
                Task(id = "u3_fail", title = "Urgent Fail", scheduledDate = today, deliveryIntensity = "URGENT")
            )
        )
        advanceUntilIdle()
        org.junit.Assert.assertNotNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)

        viewModel.uiEffect.test {
            viewModel.onIntent(HomeUiIntent.ResolveUrgencyScarcity(keepUrgent = true))
            val effect = awaitItem()
            assertTrue(effect is HomeUiEffect.ShowSnackbar)
            assertEquals("Simulated save failure", (effect as HomeUiEffect.ShowSnackbar).message)
        }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSavingTask)
        org.junit.Assert.assertNull(viewModel.uiState.value.pendingUrgencyScarcityPrompt)
    }
}
