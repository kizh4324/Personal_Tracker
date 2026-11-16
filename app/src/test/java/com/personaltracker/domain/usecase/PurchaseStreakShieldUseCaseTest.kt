package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.model.Habit
import com.personaltracker.ui.TestEconomyRepository
import com.personaltracker.ui.TestHabitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseStreakShieldUseCaseTest {

    private lateinit var habitRepository: TestHabitRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var useCase: PurchaseStreakShieldUseCase

    @Before
    fun setUp() {
        economyRepository = TestEconomyRepository()
        habitRepository = TestHabitRepository(economyRepository)
        useCase = PurchaseStreakShieldUseCase(
            habitRepository = habitRepository,
            economyRepository = economyRepository
        )
    }

    private suspend fun seedCoins(amount: Int) {
        economyRepository.recordTransaction(
            CoinTransaction(
                id = UUID.randomUUID().toString(),
                idempotencyKey = "SEED_${UUID.randomUUID()}",
                deltaAmount = amount,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                sourceEntityId = "seed",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    @Test
    fun testSuccessfulShieldPurchaseDecrementsCoinsAndIncrementsSlack() = runTest {
        seedCoins(60)
        val habit = Habit(
            id = "habit_1",
            name = "Morning Journal",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 5,
            slackBankDays = 1
        )
        habitRepository.saveHabit(habit)

        val result = useCase("habit_1")
        assertTrue(result is PurchaseStreakShieldResult.Success)
        val success = result as PurchaseStreakShieldResult.Success
        assertEquals(2, success.shieldCount)
        assertEquals(2, success.updatedHabit.slackBankDays)
        assertEquals(30, success.newBalance) // 60 - 30 = 30
    }

    @Test
    fun testInsufficientCoinsReturnsFailureWithoutDeductions() = runTest {
        seedCoins(20) // Need 30
        val habit = Habit(
            id = "habit_1",
            name = "Morning Journal",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            slackBankDays = 1
        )
        habitRepository.saveHabit(habit)

        val result = useCase("habit_1")
        assertTrue(result is PurchaseStreakShieldResult.InsufficientCoins)
        val insufficient = result as PurchaseStreakShieldResult.InsufficientCoins
        assertEquals(30, insufficient.required)
        assertEquals(20, insufficient.current)
        assertEquals(1, habitRepository.getHabitById("habit_1")?.slackBankDays)
        assertEquals(20, economyRepository.getBalance())
    }

    @Test
    fun testCapReachedWhenHabitAlreadyHasMaxSlackDays() = runTest {
        seedCoins(100)
        val habit = Habit(
            id = "habit_1",
            name = "Morning Journal",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            slackBankDays = StreakStateMachine.MAX_SLACK_BANK_DAYS
        )
        habitRepository.saveHabit(habit)

        val result = useCase("habit_1")
        assertTrue(result is PurchaseStreakShieldResult.CapReached)
        val cap = result as PurchaseStreakShieldResult.CapReached
        assertEquals(3, cap.currentShields)
        assertEquals(3, cap.maxAllowed)
        assertEquals(100, economyRepository.getBalance())
    }

    @Test
    fun testHabitNotFoundReturnsError() = runTest {
        seedCoins(50)
        val result = useCase("non_existent_habit")
        assertTrue(result is PurchaseStreakShieldResult.HabitNotFound)
        assertEquals("non_existent_habit", (result as PurchaseStreakShieldResult.HabitNotFound).habitId)
    }

    @Test
    fun testDynamicNullTargetSelectsHabitWithLowestSlackDays() = runTest {
        seedCoins(50)
        val habit1 = Habit(id = "h1", name = "Habit 1", targetDaysOfWeek = "1,2,3", slackBankDays = 2)
        val habit2 = Habit(id = "h2", name = "Habit 2", targetDaysOfWeek = "1,2,3", slackBankDays = 0)
        val habit3 = Habit(id = "h3", name = "Habit 3", targetDaysOfWeek = "1,2,3", slackBankDays = 1)
        habitRepository.saveHabit(habit1)
        habitRepository.saveHabit(habit2)
        habitRepository.saveHabit(habit3)

        val result = useCase(targetHabitId = null)
        assertTrue(result is PurchaseStreakShieldResult.Success)
        val success = result as PurchaseStreakShieldResult.Success
        assertEquals("h2", success.updatedHabit.id) // h2 had lowest (0)
        assertEquals(1, success.shieldCount)
    }

    @Test
    fun testDynamicNullTargetWhenAllHabitsAtCapReturnsCapReached() = runTest {
        seedCoins(50)
        val habit1 = Habit(id = "h1", name = "Habit 1", targetDaysOfWeek = "1,2,3", slackBankDays = 3)
        val habit2 = Habit(id = "h2", name = "Habit 2", targetDaysOfWeek = "1,2,3", slackBankDays = 3)
        habitRepository.saveHabit(habit1)
        habitRepository.saveHabit(habit2)

        val result = useCase(targetHabitId = null)
        assertTrue(result is PurchaseStreakShieldResult.CapReached)
    }

    @Test
    fun testDynamicNullTargetWhenNoHabitsReturnsError() = runTest {
        seedCoins(50)
        val result = useCase(targetHabitId = null)
        assertTrue(result is PurchaseStreakShieldResult.Error)
    }
}
