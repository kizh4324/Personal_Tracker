package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.HabitDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
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
            habitDao.insertLog(log.toEntity())
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

            habitDao.insertLog(log.toEntity())

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            val newBalance = currentBalance + coinReward

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
}
