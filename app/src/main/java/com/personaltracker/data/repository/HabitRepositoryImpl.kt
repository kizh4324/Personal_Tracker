package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.HabitDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val coinLedgerDao: CoinLedgerDao,
    private val transactionRunner: TransactionRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> =
        habitDao.getAllHabits()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getHabitById(id: String): Habit? =
        withContext(ioDispatcher) {
            habitDao.getHabitById(id)?.toDomain()
        }

    override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> =
        habitDao.getLogsForHabit(habitId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getLogForDate(habitId: String, logDate: String): HabitLog? =
        withContext(ioDispatcher) {
            habitDao.getLogForDate(habitId, logDate)?.toDomain()
        }

    override suspend fun saveHabit(habit: Habit) =
        withContext(ioDispatcher) {
            habitDao.insertHabit(habit.toEntity())
        }

    override suspend fun updateHabit(habit: Habit) =
        withContext(ioDispatcher) {
            habitDao.updateHabit(habit.toEntity())
        }

    override suspend fun logHabit(log: HabitLog) =
        withContext(ioDispatcher) {
            val existingLog = habitDao.getLogForDate(log.habitId, log.logDate)
            val logToInsert = if (existingLog != null) log.copy(id = existingLog.id) else log
            habitDao.insertLog(logToInsert.toEntity())
        }

    override suspend fun logHabitWithReward(
        log: HabitLog,
        coinReward: Int,
        idempotencyKey: String
    ) = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        require(log.status == "COMPLETED") { "Coin rewards require COMPLETED status, got: ${log.status}" }
        require(coinReward >= 0) { "Coin reward cannot be negative: $coinReward" }
        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                throw IllegalStateException("Duplicate coin transaction idempotency key: $idempotencyKey")
            }

            val existingLog = habitDao.getLogForDate(log.habitId, log.logDate)
            check(existingLog?.status != "COMPLETED") { "Habit already completed for date ${log.logDate}" }

            val logToInsert = if (existingLog != null) log.copy(id = existingLog.id) else log
            habitDao.insertLog(logToInsert.toEntity())

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            val newBalance = try {
                Math.addExact(currentBalance, coinReward)
            } catch (_: ArithmeticException) {
                throw IllegalStateException("Coin balance arithmetic overflow")
            }

            val ledgerEntry = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = coinReward,
                eventType = "HABIT_COMPLETION",
                sourceEntityId = log.habitId,
                balanceAfter = newBalance,
                timestamp = log.loggedTimestamp
            )
            coinLedgerDao.insertTransaction(ledgerEntry)
        }
    }

    override suspend fun purchaseStreakShield(
        habitId: String,
        priceCoins: Int,
        idempotencyKey: String
    ): Habit = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        require(priceCoins >= 0) { "Shield price cannot be negative: $priceCoins" }
        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            val habitEntity = habitDao.getHabitById(habitId)
                ?: throw IllegalArgumentException("Habit with id '$habitId' not found.")

            // If transaction already processed with this key, return current habit state idempotently
            if (existing != null) {
                return@transactionRunner habitEntity.toDomain()
            }

            check(habitEntity.slackBankDays < StreakStateMachine.MAX_SLACK_BANK_DAYS) {
                "Streak shield bank is already at maximum capacity (${habitEntity.slackBankDays}/${StreakStateMachine.MAX_SLACK_BANK_DAYS})"
            }

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            check(currentBalance >= priceCoins) {
                "Insufficient coin balance: $currentBalance < $priceCoins"
            }

            val newBalance = currentBalance - priceCoins
            val timestamp = System.currentTimeMillis()

            val ledgerEntry = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = -priceCoins,
                eventType = EconomyEventTypes.SHOP_PURCHASE,
                sourceEntityId = habitId,
                balanceAfter = newBalance,
                timestamp = timestamp
            )
            coinLedgerDao.insertTransaction(ledgerEntry)

            val updatedHabit = habitEntity.copy(
                slackBankDays = habitEntity.slackBankDays + 1
            )
            habitDao.updateHabit(updatedHabit)
            updatedHabit.toDomain()
        }
    }
}
