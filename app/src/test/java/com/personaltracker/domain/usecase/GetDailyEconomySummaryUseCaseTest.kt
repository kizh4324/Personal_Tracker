package com.personaltracker.domain.usecase

import app.cash.turbine.test
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.ui.TestEconomyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetDailyEconomySummaryUseCaseTest {

    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var summaryUseCase: GetDailyEconomySummaryUseCase
    private val testDate = LocalDate.parse("2026-11-14")
    private val startOfDayTimestamp = testDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        economyRepository = TestEconomyRepository()
        summaryUseCase = GetDailyEconomySummaryUseCase(economyRepository)
    }

    @Test
    fun testSummaryReflectsCategoryEarningsAndCapFlags() = runTest {
        summaryUseCase(testDate).test {
            // Initial empty summary
            val initial = awaitItem()
            assertEquals(0, initial.taskCoins)
            assertEquals(0, initial.habitCoins)
            assertEquals(0, initial.focusCoins)
            assertEquals(0, initial.routineCoins)
            assertEquals(0, initial.totalEarnedToday)
            assertEquals(0, initial.currentBalance)
            assertFalse(initial.isTaskCapReached)
            assertFalse(initial.isHabitCapReached)
            assertFalse(initial.isFocusCapReached)
            assertFalse(initial.isGlobalFarmableCapReached)

            // Seed task transaction (+40 coins)
            economyRepository.recordTransaction(
                CoinTransaction(
                    id = "tx1",
                    idempotencyKey = "TASK_COMP_1",
                    deltaAmount = 40,
                    eventType = EconomyEventTypes.TASK_COMPLETION,
                    timestamp = startOfDayTimestamp + 1000
                )
            )

            val afterTask = awaitItem()
            assertEquals(40, afterTask.taskCoins)
            assertEquals(40, afterTask.totalEarnedToday)
            assertEquals(40, afterTask.currentBalance)
            assertFalse(afterTask.isTaskCapReached)
            assertFalse(afterTask.isGlobalFarmableCapReached)

            // Seed habit transaction hitting habit cap (+40 coins)
            economyRepository.recordTransaction(
                CoinTransaction(
                    id = "tx2",
                    idempotencyKey = "HABIT_LOG_1",
                    deltaAmount = 40,
                    eventType = EconomyEventTypes.HABIT_COMPLETION,
                    timestamp = startOfDayTimestamp + 2000
                )
            )

            val afterHabit = awaitItem()
            assertEquals(40, afterHabit.habitCoins)
            assertTrue(afterHabit.isHabitCapReached)
            assertEquals(80, afterHabit.totalEarnedToday)
            assertFalse(afterHabit.isGlobalFarmableCapReached) // 40 + 40 = 80 < 100

            // Seed routine transaction hitting global farmable cap (+20 coins)
            // 40 task + 40 habit + 20 routine = 100 farmable coins
            economyRepository.recordTransaction(
                CoinTransaction(
                    id = "tx3",
                    idempotencyKey = "ROUTINE_COMP_1",
                    deltaAmount = 20,
                    eventType = EconomyEventTypes.ROUTINE_COMPLETION,
                    timestamp = startOfDayTimestamp + 3000
                )
            )

            val afterRoutine = awaitItem()
            assertEquals(20, afterRoutine.routineCoins)
            assertEquals(100, afterRoutine.totalEarnedToday)
            assertTrue(afterRoutine.isGlobalFarmableCapReached)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testGetSummaryOneShotSnapshot() = runTest {
        economyRepository.recordTransaction(
            CoinTransaction(
                id = "tx_snap",
                idempotencyKey = "STUDY_SESSION_1",
                deltaAmount = 120,
                eventType = EconomyEventTypes.STUDY_SESSION,
                timestamp = startOfDayTimestamp + 500
            )
        )

        val summary = summaryUseCase.getSummary(testDate)
        assertEquals(120, summary.focusCoins)
        assertTrue(summary.isFocusCapReached)
        assertEquals(120, summary.totalEarnedToday)
        assertEquals(120, summary.currentBalance)
        assertFalse(summary.isGlobalFarmableCapReached) // Focus sessions are exempt from farmable cap
    }
}
