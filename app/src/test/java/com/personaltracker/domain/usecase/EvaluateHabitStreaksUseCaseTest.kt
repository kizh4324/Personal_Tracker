package com.personaltracker.domain.usecase

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CadenceType
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.HabitLogStatus
import com.personaltracker.ui.TestDayTypeRepository
import com.personaltracker.ui.TestHabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EvaluateHabitStreaksUseCaseTest {

    private lateinit var habitRepository: TestHabitRepository
    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var streakStateMachine: StreakStateMachine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var useCase: EvaluateHabitStreaksUseCase

    private val directTransactionRunner = object : TransactionRunner {
        override suspend operator fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Before
    fun setUp() {
        habitRepository = TestHabitRepository()
        dayTypeRepository = TestDayTypeRepository()
        streakStateMachine = StreakStateMachine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(dayTypeRepository, AdaptiveScheduleEngine())
        useCase = EvaluateHabitStreaksUseCase(
            habitRepository = habitRepository,
            dayTypeRepository = dayTypeRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            streakStateMachine = streakStateMachine,
            transactionRunner = directTransactionRunner
        )
    }

    @Test
    fun testScheduledDayMissAbsorbedByFreezeGrace() = runTest {
        val targetDate = "2026-11-14"
        val habit = Habit(
            id = "h_freeze",
            name = "Morning Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 5,
            slackBankDays = 0,
            isFreezeActive = true,
            cadenceType = CadenceType.DAILY
        )
        habitRepository.saveHabit(habit)

        val result = useCase(targetDate)

        assertEquals(1, result.evaluatedHabitsCount)
        assertEquals(HabitLogStatus.GRACE_SHIELD_ABSORBED, result.habitResults["h_freeze"])

        val updated = habitRepository.getHabitById("h_freeze")!!
        assertEquals(5, updated.currentStreak)
        assertFalse(updated.isFreezeActive)
    }

    @Test
    fun testScheduledDayMissAbsorbedBySlackBank() = runTest {
        val targetDate = "2026-11-14"
        val habit = Habit(
            id = "h_slack",
            name = "Water Intake",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 8,
            bestStreak = 8,
            slackBankDays = 2,
            isFreezeActive = false,
            cadenceType = CadenceType.DAILY
        )
        habitRepository.saveHabit(habit)

        val result = useCase(targetDate)

        assertEquals(1, result.evaluatedHabitsCount)
        assertEquals(HabitLogStatus.SLACK_USED, result.habitResults["h_slack"])

        val updated = habitRepository.getHabitById("h_slack")!!
        assertEquals(8, updated.currentStreak)
        assertEquals(1, updated.slackBankDays)
    }

    @Test
    fun testUnprotectedMissResetsStreakToZeroPreservingBest() = runTest {
        val targetDate = "2026-11-14"
        val habit = Habit(
            id = "h_unprotected",
            name = "Reading",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 12,
            slackBankDays = 0,
            isFreezeActive = false,
            cadenceType = CadenceType.DAILY
        )
        habitRepository.saveHabit(habit)

        val result = useCase(targetDate)

        assertEquals(1, result.evaluatedHabitsCount)
        assertEquals(HabitLogStatus.MISSED, result.habitResults["h_unprotected"])

        val updated = habitRepository.getHabitById("h_unprotected")!!
        assertEquals(0, updated.currentStreak)
        assertEquals(12, updated.bestStreak)
    }

    @Test
    fun testOffCadenceDayPreservesStreakAsRestDay() = runTest {
        // 2026-11-10 is a Tuesday (Day 2)
        // Mon/Wed/Fri habit (1,3,5)
        val targetDate = "2026-11-10"
        val habit = Habit(
            id = "h_off_cadence",
            name = "Gym Mon Wed Fri",
            targetDaysOfWeek = "1,3,5",
            currentStreak = 6,
            bestStreak = 6,
            slackBankDays = 1,
            isFreezeActive = false,
            cadenceType = CadenceType.SPECIFIC_DAYS
        )
        habitRepository.saveHabit(habit)

        val result = useCase(targetDate)

        assertEquals(1, result.evaluatedHabitsCount)
        assertEquals(HabitLogStatus.REST_DAY, result.habitResults["h_off_cadence"])

        val updated = habitRepository.getHabitById("h_off_cadence")!!
        assertEquals(6, updated.currentStreak)
        assertEquals(1, updated.slackBankDays)
    }

    @Test
    fun testActiveDayTypeImmunityPreservesStreak() = runTest {
        val targetDate = "2026-11-14"
        dayTypeRepository.saveDayTypes(listOf(DayType.LOW_CAPACITY, DayType.STANDARD))
        val override = com.personaltracker.domain.model.DayOverride(
            date = targetDate,
            dayTypeId = DayType.LOW_CAPACITY.id,
            isUserModified = true,
            createdTimestamp = System.currentTimeMillis()
        )
        dayTypeRepository.setOverrideForDate(override)

        val habit = Habit(
            id = "h_immunity",
            name = "Deep Coding",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 4,
            bestStreak = 4,
            slackBankDays = 0,
            isFreezeActive = false,
            cadenceType = CadenceType.DAILY
        )
        habitRepository.saveHabit(habit)

        val result = useCase(targetDate)

        assertEquals(1, result.evaluatedHabitsCount)
        assertEquals(HabitLogStatus.DAY_TYPE_IMMUNITY, result.habitResults["h_immunity"])

        val updated = habitRepository.getHabitById("h_immunity")!!
        assertEquals(4, updated.currentStreak)
    }

    @Test
    fun testAlreadyCompletedHabitPreservesCompletedStatus() = runTest {
        val targetDate = "2026-11-14"
        val habit = Habit(
            id = "h_done",
            name = "Journaling",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 3,
            bestStreak = 3
        )
        habitRepository.saveHabit(habit)
        habitRepository.logHabit(
            HabitLog(
                id = "log_done",
                habitId = "h_done",
                logDate = targetDate,
                status = HabitLogStatus.COMPLETED
            )
        )

        val result = useCase(targetDate)

        assertEquals(HabitLogStatus.COMPLETED, result.habitResults["h_done"])
        val updated = habitRepository.getHabitById("h_done")!!
        assertEquals(3, updated.currentStreak)
    }
}
