package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Idempotent Coin Ledger transactions (AD-3, AD-6, FR-8.2).
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface CoinLedgerDao {

    @Query("SELECT * FROM coin_ledger ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<CoinLedgerEntity>>

    @Query("SELECT * FROM coin_ledger WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(key: String): CoinLedgerEntity?

    @Query("SELECT balanceAfter FROM coin_ledger ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestBalance(): Int?

    @Query("SELECT balanceAfter FROM coin_ledger ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatestBalance(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(entry: CoinLedgerEntity)

    @Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND deltaAmount > 0")
    suspend fun getDailyEarnedTotal(startOfDayTimestamp: Long): Int

    @Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND deltaAmount > 0")
    fun observeDailyEarnedTotal(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND eventType = :eventType AND deltaAmount > 0")
    suspend fun getDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Int

    @Query("SELECT COALESCE(SUM(deltaAmount), 0) FROM coin_ledger WHERE timestamp >= :startOfDayTimestamp AND eventType = :eventType AND deltaAmount > 0")
    fun observeDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Flow<Int>

    @Query("SELECT * FROM coin_ledger WHERE timestamp >= :timestamp ORDER BY timestamp DESC, id DESC")
    fun getTransactionsSince(timestamp: Long): Flow<List<CoinLedgerEntity>>
}
