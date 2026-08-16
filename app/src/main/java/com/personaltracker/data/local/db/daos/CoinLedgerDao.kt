package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Idempotent Coin Ledger transactions.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface CoinLedgerDao {

    @Query("SELECT * FROM coin_ledger ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<CoinLedgerEntity>>

    @Query("SELECT * FROM coin_ledger WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(key: String): CoinLedgerEntity?

    @Query("SELECT balanceAfter FROM coin_ledger ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBalance(): Int?

    @Query("SELECT balanceAfter FROM coin_ledger ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestBalance(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(entry: CoinLedgerEntity)
}
