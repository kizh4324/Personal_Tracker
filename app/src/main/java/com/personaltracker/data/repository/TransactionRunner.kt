package com.personaltracker.data.repository

import androidx.room.withTransaction
import com.personaltracker.data.local.db.AppDatabase

/**
 * Strategy interface abstraction for running database operations inside Room transactions.
 * Enables clean JVM unit testing while executing [AppDatabase.withTransaction] in production (AD-6).
 */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

/**
 * Default implementation delegating to [AppDatabase.withTransaction].
 */
class RoomTransactionRunner(private val db: AppDatabase) : TransactionRunner {
    override suspend operator fun <T> invoke(block: suspend () -> T): T = db.withTransaction { block() }
}
