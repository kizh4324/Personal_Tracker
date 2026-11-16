package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface PurchaseStreakShieldResult {
    data class Success(val updatedHabit: Habit, val newBalance: Int, val shieldCount: Int) : PurchaseStreakShieldResult
    data class InsufficientCoins(val required: Int, val current: Int) : PurchaseStreakShieldResult
    data class CapReached(val currentShields: Int, val maxAllowed: Int) : PurchaseStreakShieldResult
    data class HabitNotFound(val habitId: String) : PurchaseStreakShieldResult
    data class Error(val message: String) : PurchaseStreakShieldResult
}

/**
 * Pure Kotlin use case coordinating consumable streak grace shield purchases,
 * cap validation (slackBankDays < 3), dynamic habit selection when target is null,
 * and atomic coin deduction (AD-1, AD-4, AD-6).
 */
class PurchaseStreakShieldUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val economyRepository: EconomyRepository
) {
    suspend operator fun invoke(targetHabitId: String? = null): PurchaseStreakShieldResult {
        return try {
            val balance = economyRepository.getBalance()
            val shieldCost = SHIELD_PRICE_COINS

            if (balance < shieldCost) {
                return PurchaseStreakShieldResult.InsufficientCoins(
                    required = shieldCost,
                    current = balance
                )
            }

            val targetHabit = if (targetHabitId != null) {
                habitRepository.getHabitById(targetHabitId)
                    ?: return PurchaseStreakShieldResult.HabitNotFound(targetHabitId)
            } else {
                val habits = habitRepository.getAllHabits().first()
                if (habits.isEmpty()) {
                    return PurchaseStreakShieldResult.Error("No habits available. Create a habit first!")
                }
                val eligibleHabits = habits.filter { it.slackBankDays < StreakStateMachine.MAX_SLACK_BANK_DAYS }
                if (eligibleHabits.isEmpty()) {
                    return PurchaseStreakShieldResult.CapReached(
                        currentShields = StreakStateMachine.MAX_SLACK_BANK_DAYS,
                        maxAllowed = StreakStateMachine.MAX_SLACK_BANK_DAYS
                    )
                }
                // Select habit with lowest slackBankDays to protect the most vulnerable habit
                eligibleHabits.minByOrNull { it.slackBankDays } ?: eligibleHabits.first()
            }

            if (targetHabit.slackBankDays >= StreakStateMachine.MAX_SLACK_BANK_DAYS) {
                return PurchaseStreakShieldResult.CapReached(
                    currentShields = targetHabit.slackBankDays,
                    maxAllowed = StreakStateMachine.MAX_SLACK_BANK_DAYS
                )
            }

            val idempotencyKey = "SHIELD_BUY_${targetHabit.id}_${System.currentTimeMillis()}"
            val updated = habitRepository.purchaseStreakShield(
                habitId = targetHabit.id,
                priceCoins = shieldCost,
                idempotencyKey = idempotencyKey
            )
            val newBalance = economyRepository.getBalance()

            PurchaseStreakShieldResult.Success(
                updatedHabit = updated,
                newBalance = newBalance,
                shieldCount = updated.slackBankDays
            )
        } catch (e: Exception) {
            PurchaseStreakShieldResult.Error(e.message ?: "Could not purchase streak shield")
        }
    }

    companion object {
        const val SHIELD_PRICE_COINS = 30
    }
}
