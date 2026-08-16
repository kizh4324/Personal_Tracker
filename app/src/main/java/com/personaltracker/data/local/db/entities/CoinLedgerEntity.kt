package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only immutable coin audit ledger entity with idempotency enforcement (FR-8.2, ARCH-6).
 */
@Entity(
    tableName = "coin_ledger",
    indices = [
        Index("idempotencyKey", unique = true),
        Index("timestamp")
    ]
)
data class CoinLedgerEntity(
    @PrimaryKey val id: String, // UUIDv4
    val idempotencyKey: String, // Unique key, e.g. "TASK_COMP_<id>"
    val deltaAmount: Int, // Positive for earnings, negative for shop spend
    val eventType: String, // "TASK_COMPLETION", "ROUTINE_COMPLETION", "HABIT_COMPLETION", "STUDY_SESSION", "SHOP_PURCHASE"
    val sourceEntityId: String?,
    val balanceAfter: Int,
    val timestamp: Long
)
