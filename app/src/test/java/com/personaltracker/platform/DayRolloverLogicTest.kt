package com.personaltracker.platform

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.platform.worker.DayRolloverCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DayRolloverLogicTest {

    private lateinit var fakeHabitRepository: FakeHabitRepository
    private lateinit var fakeDayTypeRepository: FakeDayTypeRepository
    private lateinit var streakStateMachine: StreakStateMachine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var coordinator: DayRolloverCoordinator

    private val directTransactionRunner = object : TransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Before
    fun setUp() {
        fakeHabitRepository = FakeHabitRepository()
        fakeDayTypeRepository = FakeDayTypeRepository()
        streakStateMachine = StreakStateMachine()
        val adaptiveScheduleEngine = AdaptiveScheduleEngine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(fakeDayTypeRepository, adaptiveScheduleEngine)

        coordinator = DayRolloverCoordinator(
            habitRepository = fakeHabitRepository,
            dayTypeRepository = fakeDayTypeRepository,
            streakStateMachine = streakStateMachine,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            transactionRunner = directTransactionRunner
        )
    }

    @Test
    fun `uncompleted habit with active freeze consumes freeze and preserves streak`() = runTest {
        val habit = Habit(
            id = "habit-freeze",
            name = "Morning Meditation",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            bestStreak = 10,
            slackBankDays = 1,
            isFreezeActive = true
        )
        fakeHabitRepository.saveHabit(habit)

        val result = coordinator.processRollover(
            yesterdayDate = "2026-10-17",
            todayDate = "2026-10-18"
        )

        assertEquals("FROZEN", result.habitResults["habit-freeze"])
        val updatedHabit = fakeHabitRepository.getHabitById("habit-freeze")!!
        assertEquals(5, updatedHabit.currentStreak) // Preserved
        assertFalse(updatedHabit.isFreezeActive) // Consumed
        assertEquals(1, updatedHabit.slackBankDays) // Preserved
    }

    @Test
    fun `uncompleted habit with slack bank decrements slack and preserves streak`() = runTest {
        val habit = Habit(
            id = "habit-slack",
            name = "Reading",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 7,
            bestStreak = 7,
            slackBankDays = 2,
            isFreezeActive = false
        )
        fakeHabitRepository.saveHabit(habit)

        val result = coordinator.processRollover(
            yesterdayDate = "2026-10-17",
            todayDate = "2026-10-18"
        )

        assertEquals("SLACK_USED", result.habitResults["habit-slack"])
        val updatedHabit = fakeHabitRepository.getHabitById("habit-slack")!!
        assertEquals(7, updatedHabit.currentStreak) // Preserved
        assertEquals(1, updatedHabit.slackBankDays) // Decremented by 1
    }

    @Test
    fun `uncompleted habit with no freeze and no slack resets streak to zero`() = runTest {
        val habit = Habit(
            id = "habit-missed",
            name = "Pushups",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 4,
            bestStreak = 12,
            slackBankDays = 0,
            isFreezeActive = false
        )
        fakeHabitRepository.saveHabit(habit)

        val result = coordinator.processRollover(
            yesterdayDate = "2026-10-17",
            todayDate = "2026-10-18"
        )

        assertEquals("MISSED", result.habitResults["habit-missed"])
        val updatedHabit = fakeHabitRepository.getHabitById("habit-missed")!!
        assertEquals(0, updatedHabit.currentStreak) // Reset to 0
        assertEquals(12, updatedHabit.bestStreak) // Best streak unconditionally preserved
    }

    @Test
    fun `already completed habit yesterday is not modified`() = runTest {
        val habit = Habit(
            id = "habit-completed",
            name = "Drink Water",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 15,
            bestStreak = 20,
            slackBankDays = 3,
            isFreezeActive = false
        )
        fakeHabitRepository.saveHabit(habit)
        fakeHabitRepository.logHabit(
            HabitLog(
                id = "log-1",
                habitId = "habit-completed",
                logDate = "2026-10-17",
                status = "COMPLETED"
            )
        )

        val result = coordinator.processRollover(
            yesterdayDate = "2026-10-17",
            todayDate = "2026-10-18"
        )

        assertEquals("COMPLETED", result.habitResults["habit-completed"])
        val currentHabit = fakeHabitRepository.getHabitById("habit-completed")!!
        assertEquals(15, currentHabit.currentStreak)
    }

    @Test
    fun `rollover resolves active DayType for today`() = runTest {
        val defaultType = DayType(
            id = "dt-normal",
            name = "Normal Day",
            colorHex = "#3B82F6",
            targetStudyMinutes = 90,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5,6,7"
        )
        fakeDayTypeRepository.saveDayType(defaultType)

        val result = coordinator.processRollover(
            yesterdayDate = "2026-10-17",
            todayDate = "2026-10-18"
        )

        assertEquals("dt-normal", result.activeDayType.id)
        assertEquals("Normal Day", result.activeDayType.name)
    }

    // --- In-Memory Fakes ---

    private class FakeHabitRepository : HabitRepository {
        private val habits = mutableMapOf<String, Habit>()
        private val logs = mutableListOf<HabitLog>()
        private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())

        override fun getAllHabits(): Flow<List<Habit>> = habitsFlow

        override suspend fun getHabitById(id: String): Habit? = habits[id]

        override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> =
            flowOf(logs.filter { it.habitId == habitId })

        override suspend fun getLogForDate(habitId: String, logDate: String): HabitLog? =
            logs.firstOrNull { it.habitId == habitId && it.logDate == logDate }

        override suspend fun saveHabit(habit: Habit) {
            habits[habit.id] = habit
            habitsFlow.value = habits.values.toList()
        }

        override suspend fun updateHabit(habit: Habit) {
            habits[habit.id] = habit
            habitsFlow.value = habits.values.toList()
        }

        override suspend fun logHabit(log: HabitLog) {
            logs.add(log)
        }

        override suspend fun logHabitWithReward(log: HabitLog, coinReward: Int, idempotencyKey: String) {
            logs.add(log)
        }
    }

    private class FakeDayTypeRepository : DayTypeRepository {
        private val dayTypes = mutableMapOf<String, DayType>()
        private val overrides = mutableMapOf<String, DayOverride>()

        override fun getAllDayTypes(): Flow<List<DayType>> = flowOf(dayTypes.values.toList())

        override suspend fun getDayTypeById(id: String): DayType? = dayTypes[id]

        override suspend fun saveDayType(dayType: DayType) {
            dayTypes[dayType.id] = dayType
        }

        override suspend fun saveDayTypes(dayTypes: List<DayType>) {
            dayTypes.forEach { this.dayTypes[it.id] = it }
        }

        override suspend fun ensureDefaultDayTypes(): List<DayType> {
            if (dayTypes.isEmpty()) {
                DayType.BUILT_IN_DAY_TYPES.forEach { dayTypes[it.id] = it }
            }
            return dayTypes.values.toList()
        }

        override suspend fun getOverrideForDate(date: String): DayOverride? = overrides[date]

        override fun observeOverrideForDate(date: String): Flow<DayOverride?> = flowOf(overrides[date])

        override suspend fun setOverrideForDate(override: DayOverride) {
            overrides[override.date] = override
        }

        override suspend fun deleteOverrideForDate(date: String) {
            overrides.remove(date)
        }
    }
}
