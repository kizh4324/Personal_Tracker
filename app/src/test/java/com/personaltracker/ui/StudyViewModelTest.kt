package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.HeatmapLedgerEngine
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudySession
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.screens.study.StudyUiEffect
import com.personaltracker.ui.screens.study.StudyUiIntent
import com.personaltracker.ui.screens.study.StudyViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Field
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var studyRepository: TestStudySessionRepository
    private lateinit var heatmapLedgerEngine: HeatmapLedgerEngine
    private lateinit var focusRescheduleEngine: FocusRescheduleEngine

    @Before
    fun setUp() {
        studyRepository = TestStudySessionRepository()
        heatmapLedgerEngine = HeatmapLedgerEngine()
        focusRescheduleEngine = FocusRescheduleEngine()
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

    @Test
    fun testInitialUiStatePopulatesDefaultsAndHeatmap() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            heatmapLedgerEngine = heatmapLedgerEngine,
            focusRescheduleEngine = focusRescheduleEngine,
            context = null
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Focus", state.selectedSubjectTag)
            assertEquals(25, state.selectedDurationMinutes)
            assertEquals(FocusIntervalType.DEEP_WORK, state.selectedIntervalType)
            assertEquals(0, state.completedCycles)
            assertNull(state.selectedCellDate)
            assertNull(state.selectedCell)
            assertEquals(0, state.todayTotalStudyMinutes)
            assertEquals(28, state.heatmapData.cells.size)
        }
    }

    @Test
    fun testSelectIntervalTypeUpdatesDurationAndMode() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            heatmapLedgerEngine = heatmapLedgerEngine,
            focusRescheduleEngine = focusRescheduleEngine,
            context = null
        )

        viewModel.uiState.test {
            val s0 = awaitItem()
            assertEquals(FocusIntervalType.DEEP_WORK, s0.selectedIntervalType)

            // Select Pomodoro Work (default 25m)
            viewModel.onIntent(StudyUiIntent.SelectIntervalType(FocusIntervalType.POMODORO_WORK))
            val s1 = awaitItem()
            assertEquals(FocusIntervalType.POMODORO_WORK, s1.selectedIntervalType)
            assertEquals(25, s1.selectedDurationMinutes)

            // Select Short Break (default 5m)
            viewModel.onIntent(StudyUiIntent.SelectIntervalType(FocusIntervalType.SHORT_BREAK))
            val s2 = awaitItem()
            assertEquals(FocusIntervalType.SHORT_BREAK, s2.selectedIntervalType)
            assertEquals(5, s2.selectedDurationMinutes)

            // Select Long Break (default 15m)
            viewModel.onIntent(StudyUiIntent.SelectIntervalType(FocusIntervalType.LONG_BREAK))
            val s3 = awaitItem()
            assertEquals(FocusIntervalType.LONG_BREAK, s3.selectedIntervalType)
            assertEquals(15, s3.selectedDurationMinutes)
        }
    }

    @Test
    fun testSelectHeatmapCellTogglesSelection() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            heatmapLedgerEngine = heatmapLedgerEngine,
            focusRescheduleEngine = focusRescheduleEngine,
            context = null
        )

        val targetDate = "2026-10-26"

        viewModel.uiState.test {
            val s0 = awaitItem()
            assertNull(s0.selectedCellDate)

            // 1. Select cell
            viewModel.onIntent(StudyUiIntent.SelectHeatmapCell(targetDate))
            val s1 = awaitItem()
            assertEquals(targetDate, s1.selectedCellDate)

            // 2. Select same cell again -> toggles off
            viewModel.onIntent(StudyUiIntent.SelectHeatmapCell(targetDate))
            val s2 = awaitItem()
            assertNull(s2.selectedCellDate)
        }
    }

    @Test
    fun testHeatmapAggregatesSessionsReactivelyFromRepository() = runTest {
        val today = LocalDate.now().toString()
        val s1 = StudySession(
            id = "sess_pomo",
            subjectTag = "Algorithms",
            sessionDate = today,
            startTime = "09:00",
            durationMinutes = 25,
            intervalType = FocusIntervalType.POMODORO_WORK
        )
        val s2 = StudySession(
            id = "sess_reading",
            subjectTag = "Documentation",
            sessionDate = today,
            startTime = "10:00",
            durationMinutes = 35,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        studyRepository.saveSession(s1)
        studyRepository.saveSession(s2)

        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            heatmapLedgerEngine = heatmapLedgerEngine,
            focusRescheduleEngine = focusRescheduleEngine,
            context = null
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(60, state.todayTotalStudyMinutes)
            assertEquals(60, state.heatmapData.totalFocusMinutes)
            assertEquals(1, state.heatmapData.activeDaysCount)

            val todayCell = state.heatmapData.cells.find { it.date == today }
            assertNotNull(todayCell)
            assertEquals(60, todayCell!!.totalMinutes)
            assertEquals(3, todayCell.intensityTier) // 60..119m -> Tier 3
            assertEquals(2, todayCell.sessionCount)
            assertEquals(25, todayCell.subjectBreakdown["Algorithms"])
            assertEquals(35, todayCell.subjectBreakdown["Documentation"])
        }
    }

    @Test
    fun testTimerCompletionWithPomodoroTransitionsToShortBreakAndAdvancesCycles() = runTest {
        val viewModel = StudyViewModel(
            studySessionRepository = studyRepository,
            heatmapLedgerEngine = heatmapLedgerEngine,
            focusRescheduleEngine = focusRescheduleEngine,
            context = null
        )

        // Set interval to Pomodoro
        viewModel.onIntent(StudyUiIntent.SelectIntervalType(FocusIntervalType.POMODORO_WORK))

        val field: Field = StudySessionForegroundService::class.java.getDeclaredField("_timerState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val serviceTimerState = field.get(null) as MutableStateFlow<StudySessionTimerState>

        viewModel.uiEffect.test {
            // Simulate 25m Pomodoro completion (1500 seconds)
            serviceTimerState.value = StudySessionTimerState(
                sessionId = "pomo_session_1",
                subjectTag = "Coding",
                totalSeconds = 1500L,
                remainingSeconds = 0L,
                isRunning = false,
                isPaused = false,
                isCompleted = true
            )

            // 1. Celebration effect
            val effect1 = awaitItem()
            assertTrue(effect1 is StudyUiEffect.ShowSessionCompleteCelebration)
            assertEquals(5, (effect1 as StudyUiEffect.ShowSessionCompleteCelebration).coinsEarned)

            // 2. Lottie reward effect
            val effect2 = awaitItem()
            assertTrue(effect2 is StudyUiEffect.ShowLottieReward)
            assertEquals(RewardAnimationType.MilestoneFullReveal, (effect2 as StudyUiEffect.ShowLottieReward).type)
        }

        // Verify state transitioned to SHORT_BREAK with 5 min and cycle = 1
        val finalState = viewModel.uiState.value
        assertEquals(FocusIntervalType.SHORT_BREAK, finalState.selectedIntervalType)
        assertEquals(5, finalState.selectedDurationMinutes)
        assertEquals(1, finalState.completedCycles)
    }
}
