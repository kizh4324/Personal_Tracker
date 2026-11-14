package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.HabitLogStatus
import com.personaltracker.ui.TestEconomyRepository
import com.personaltracker.ui.TestHabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogHabitUseCaseTest {

    private lateinit var habitRepository: TestHabitRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var streakStateMachine: StreakStateMachine
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var useCase: LogHabitUseCase

    @Before
    fun setUp() {
        habitRepository = TestHabitRepository()
        economyRepository = TestEconomyRepository()
        streakStateMachine = StreakStateMachine()
        ledgerEngine = GamificationLedgerEngine()
        useCase = LogHabitUseCase(
            habitRepository = habitRepository,
            economyRepository = economyRepository,
            streakStateMachine = streakStateMachine,
            ledgerEngine = ledgerEngine
        )
    }

    @Test
    fun testCompletionAwardsCoinsAndGeneratesIdempotencyKey() = runTest {
        val habit = Habit(
            id = "habit_1",
            name = "Morning Run",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 3,
            bestStreak = 3
        )
        habitRepository.saveHabit(habit)
        economyRepository.updateCompanionProfile(
            CompanionProfile(
                id = 1,
                speciesName = "Fox",
                level = 1,
                currentEnergyUnits = 10,
                activeAccessoryId = null,
                activeBackgroundId = null,
                currentExpressionOrdinal = 0
            )
        )

        val result = useCase(habitId = "habit_1", logDate = "2026-11-14", isCompleted = true)

        assertEquals(4, result.updatedHabit.currentStreak)
        assertEquals(4, result.updatedHabit.bestStreak)
        assertEquals(HabitLogStatus.COMPLETED, result.resultingStatus)
        assertEquals(5, result.coinsEarned)
        assertEquals("HABIT_LOG_habit_1_2026-11-14", result.idempotencyKey)
        assertNotNull(result.updatedCompanionProfile)
    }

    @Test
    fun testDuplicateCompletionReturnsZeroCoinsAndNoOp() = runTest {
        val habit = Habit(
            id = "habit_2",
            name = "Reading",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 5
        )
        habitRepository.saveHabit(habit)
        habitRepository.logHabit(
            HabitLog(
                id = "log_done",
                habitId = "habit_2",
                logDate = "2026-11-14",
                status = HabitLogStatus.COMPLETED
            )
        )

        val result = useCase(habitId = "habit_2", logDate = "2026-11-14", isCompleted = true)

        assertEquals(5, result.updatedHabit.currentStreak)
        assertEquals(HabitLogStatus.COMPLETED, result.resultingStatus)
        assertEquals(0, result.coinsEarned)
        assertNull(result.idempotencyKey)
    }

    @Test
    fun testUpgradePriorMissedLogToCompletedUpdatesStreakAndAwardsCoins() = runTest {
        val habit = Habit(
            id = "habit_3",
            name = "Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 0,
            bestStreak = 6
        )
        habitRepository.saveHabit(habit)
        habitRepository.logHabit(
            HabitLog(
                id = "log_missed",
                habitId = "habit_3",
                logDate = "2026-11-14",
                status = HabitLogStatus.MISSED
            )
        )

        val result = useCase(habitId = "habit_3", logDate = "2026-11-14", isCompleted = true)

        assertEquals(1, result.updatedHabit.currentStreak)
        assertEquals(HabitLogStatus.COMPLETED, result.resultingStatus)
        assertEquals(5, result.coinsEarned)
        assertEquals("HABIT_LOG_habit_3_2026-11-14", result.idempotencyKey)
    }

    @Test
    fun testUpgradePriorSlackUsedLogRestoresSlackBank() = runTest {
        val habit = Habit(
            id = "habit_4",
            name = "Drink Water",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 4,
            bestStreak = 4,
            slackBankDays = 1
        )
        habitRepository.saveHabit(habit)
        habitRepository.logHabit(
            HabitLog(
                id = "log_slack",
                habitId = "habit_4",
                logDate = "2026-11-14",
                status = HabitLogStatus.SLACK_USED
            )
        )

        val result = useCase(habitId = "habit_4", logDate = "2026-11-14", isCompleted = true)

        // Slack bank restored (+1 = 2) and streak incremented to 5
        assertEquals(5, result.updatedHabit.currentStreak)
        assertEquals(2, result.updatedHabit.slackBankDays)
        assertEquals(HabitLogStatus.COMPLETED, result.resultingStatus)
        assertEquals(5, result.coinsEarned)
    }

    @Test
    fun testUpgradePriorRestDayLogToCompleted() = runTest {
        val habit = Habit(
            id = "habit_5",
            name = "Yoga",
            targetDaysOfWeek = "1,3,5",
            currentStreak = 2,
            bestStreak = 2
        )
        habitRepository.saveHabit(habit)
        habitRepository.logHabit(
            HabitLog(
                id = "log_rest",
                habitId = "habit_5",
                logDate = "2026-11-14",
                status = HabitLogStatus.REST_DAY
            )
        )

        val result = useCase(habitId = "habit_5", logDate = "2026-11-14", isCompleted = true)

        assertEquals(3, result.updatedHabit.currentStreak)
        assertEquals(HabitLogStatus.COMPLETED, result.resultingStatus)
        assertEquals(5, result.coinsEarned)
    }
}
