package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.DailyEconomySummary
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.repository.EconomyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Pure Kotlin use case observing and aggregating daily coin earnings and category cap saturation (AD-1, FR-8.2).
 */
class GetDailyEconomySummaryUseCase @Inject constructor(
    private val economyRepository: EconomyRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<DailyEconomySummary> {
        val startOfDayTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val balanceFlow = economyRepository.observeBalance()
        val transactionsFlow = economyRepository.observeTransactionsSince(startOfDayTimestamp)

        return combine(balanceFlow, transactionsFlow) { balance, transactions ->
            val taskCoins = transactions.filter { it.eventType == EconomyEventTypes.TASK_COMPLETION && it.deltaAmount > 0 }.sumOf { it.deltaAmount }
            val habitCoins = transactions.filter { it.eventType == EconomyEventTypes.HABIT_COMPLETION && it.deltaAmount > 0 }.sumOf { it.deltaAmount }
            val focusCoins = transactions.filter { it.eventType == EconomyEventTypes.STUDY_SESSION && it.deltaAmount > 0 }.sumOf { it.deltaAmount }
            val routineCoins = transactions.filter { it.eventType == EconomyEventTypes.ROUTINE_COMPLETION && it.deltaAmount > 0 }.sumOf { it.deltaAmount }
            val totalEarnedToday = transactions.filter { it.deltaAmount > 0 }.sumOf { it.deltaAmount }
            val farmableEarned = taskCoins + habitCoins + routineCoins

            val resolvedBalance = if (transactions.isNotEmpty() && transactions.first().balanceAfter > 0) {
                transactions.first().balanceAfter
            } else {
                balance
            }

            DailyEconomySummary(
                taskCoins = taskCoins,
                habitCoins = habitCoins,
                focusCoins = focusCoins,
                routineCoins = routineCoins,
                totalEarnedToday = totalEarnedToday,
                currentBalance = resolvedBalance,
                isTaskCapReached = taskCoins >= GamificationLedgerEngine.DAILY_TASK_CAP,
                isHabitCapReached = habitCoins >= GamificationLedgerEngine.DAILY_HABIT_CAP,
                isFocusCapReached = focusCoins >= GamificationLedgerEngine.DAILY_FOCUS_CAP,
                isGlobalFarmableCapReached = farmableEarned >= GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP
            )
        }.distinctUntilChanged()
    }

    suspend fun getSummary(date: LocalDate = LocalDate.now()): DailyEconomySummary {
        val startOfDayTimestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val balance = economyRepository.getBalance()
        val taskCoins = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION)
        val habitCoins = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.HABIT_COMPLETION)
        val focusCoins = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.STUDY_SESSION)
        val routineCoins = economyRepository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.ROUTINE_COMPLETION)
        val totalEarnedToday = economyRepository.getDailyEarnedTotal(startOfDayTimestamp)

        val farmableEarned = taskCoins + habitCoins + routineCoins
        return DailyEconomySummary(
            taskCoins = taskCoins,
            habitCoins = habitCoins,
            focusCoins = focusCoins,
            routineCoins = routineCoins,
            totalEarnedToday = totalEarnedToday,
            currentBalance = balance,
            isTaskCapReached = taskCoins >= GamificationLedgerEngine.DAILY_TASK_CAP,
            isHabitCapReached = habitCoins >= GamificationLedgerEngine.DAILY_HABIT_CAP,
            isFocusCapReached = focusCoins >= GamificationLedgerEngine.DAILY_FOCUS_CAP,
            isGlobalFarmableCapReached = farmableEarned >= GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP
        )
    }
}
