package com.personaltracker.ui

import androidx.compose.ui.geometry.Offset
import app.cash.turbine.test
import com.personaltracker.domain.engine.FlameState
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.screens.habits.HabitsUiEffect
import com.personaltracker.ui.screens.habits.HabitsUiIntent
import com.personaltracker.ui.screens.habits.HabitsViewModel
import com.personaltracker.ui.screens.study.StudyUiEffect
import com.personaltracker.ui.screens.study.StudyUiIntent
import com.personaltracker.ui.screens.study.StudyViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class HabitsAndStudyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var habitRepository: TestHabitRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var studyRepository: TestStudySessionRepository
    private lateinit var streakMachine: StreakStateMachine
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var logHabitUseCase: LogHabitUseCase

    @Before
    fun setUp() {
        habitRepository = TestHabitRepository()
        economyRepository = TestEconomyRepository()
        studyRepository = TestStudySessionRepository()
        streakMachine = StreakStateMachine()
        ledgerEngine = GamificationLedgerEngine()
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

    // ==========================================
    // 1. HabitsViewModel Tests
    // ==========================================

    @Test
    fun testHabitsViewModelEmitsHabitsWithCalculatedFlames() = runTest {
        val h1 = Habit(id = "h1", name = "Drink Water", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 0)
        val h2 = Habit(id = "h2", name = "Morning Walk", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 7)
        habitRepository.saveHabit(h1)
        habitRepository.saveHabit(h2)

        val viewModel = HabitsViewModel(
            habitRepository = habitRepository,
            logHabitUseCase = logHabitUseCase,
            streakStateMachine = streakMachine
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.habits.size)
            assertEquals(1, state.totalActiveStreaks)

            val item1 = state.habits.find { it.habit.id == "h1" }!!
            assertEquals(FlameState.COLD, item1.flameState)

            val item2 = state.habits.find { it.habit.id == "h2" }!!
            assertEquals(FlameState.BLAZING, item2.flameState)
        }
    }

    @Test
    fun testHabitsViewModelToggleHabitEmitsCoinAndLottieRewards() = runTest {
        val habit = Habit(id = "h_meditate", name = "Meditate", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 2)
        habitRepository.saveHabit(habit)

        val viewModel = HabitsViewModel(
            habitRepository = habitRepository,
            logHabitUseCase = logHabitUseCase,
            streakStateMachine = streakMachine
        )

        viewModel.uiEffect.test {
            viewModel.onIntent(HabitsUiIntent.ToggleHabit("h_meditate", Offset(200f, 300f)))

            val effect1 = awaitItem()
            assertTrue(effect1 is HabitsUiEffect.ShowCoinReward)
            val coinReward = effect1 as HabitsUiEffect.ShowCoinReward
            assertEquals(Offset(200f, 300f), coinReward.startOffset)
            assertEquals(5, coinReward.coinCount)

            val effect2 = awaitItem()
            assertTrue(effect2 is HabitsUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect2 as HabitsUiEffect.ShowLottieReward).type)
        }
    }

    @Test
    fun testHabitsViewModelCreateHabitAddsToRepository() = runTest {
        val viewModel = HabitsViewModel(
            habitRepository = habitRepository,
            logHabitUseCase = logHabitUseCase,
            streakStateMachine = streakMachine
        )

        viewModel.onIntent(HabitsUiIntent.CreateHabit(name = "Read 10 pages", cadence = "DAILY"))

        val allHabits = habitRepository.habitsFlow.value
        assertEquals(1, allHabits.size)
        assertEquals("Read 10 pages", allHabits.first().name)
        assertEquals(0, allHabits.first().currentStreak)
    }

    // ==========================================
    // 2. StudyViewModel Tests
    // ==========================================

    @Test
    fun testStudyViewModelBindsToForegroundServiceTimerState() = runTest {
        val today = LocalDate.now().toString()
        val s1 = StudySession(
            id = "sess_1",
            subjectTag = "Deep Work",
            sessionDate = today,
            startTime = "10:00",
            durationMinutes = 45,
            coinsEarned = 9,
            isInterrupted = false
        )
        studyRepository.saveSession(s1)

        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            context = null
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(45, state.todayTotalStudyMinutes)
            assertEquals(1, state.recentSessions.size)
        }
    }

    @Test
    fun testStudyViewModelDurationAndSubjectSelection() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            context = null
        )

        viewModel.uiState.test {
            val s1 = awaitItem()
            assertEquals("Focus", s1.selectedSubjectTag)
            assertEquals(25, s1.selectedDurationMinutes)

            viewModel.onIntent(StudyUiIntent.SelectSubjectTag("Algorithms"))
            val s2 = awaitItem()
            assertEquals("Algorithms", s2.selectedSubjectTag)

            viewModel.onIntent(StudyUiIntent.SelectDuration(45))
            val s3 = awaitItem()
            assertEquals(45, s3.selectedDurationMinutes)
        }
    }

    @Test
    fun testStudyViewModelDetectsTimerCompletionAndEmitsCelebration() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            context = null
        )

        // Inject completion into the static StateFlow of StudySessionForegroundService
        val field: Field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>

        viewModel.uiEffect.test {
            // Simulate completed timer state
            serviceTimerState.value = StudySessionTimerState(
                sessionId = "test_completed_session",
                subjectTag = "Physics",
                totalSeconds = 1500L,
                remainingSeconds = 0L,
                isRunning = false,
                isPaused = false,
                isCompleted = true
            )

            val effect1 = awaitItem()
            assertTrue(effect1 is StudyUiEffect.ShowSessionCompleteCelebration)
            val celebration = effect1 as StudyUiEffect.ShowSessionCompleteCelebration
            assertEquals(5, celebration.coinsEarned)

            val effect2 = awaitItem()
            assertTrue(effect2 is StudyUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.MilestoneFullReveal, (effect2 as StudyUiEffect.ShowLottieReward).type)
        }
    }
}
