package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing an append-only immutable coin audit entry (FR-8.2, AD-1, AD-4).
 */
data class CoinTransaction(
    val id: String,
    val idempotencyKey: String, // Compound format: {DOMAIN}_{ACTION}_{ID}
    val deltaAmount: Int,
    val eventType: String, // "TASK_COMPLETION", "ROUTINE_COMPLETION", "HABIT_COMPLETION", "STUDY_SESSION", "SHOP_PURCHASE"
    val sourceEntityId: String? = null,
    val balanceAfter: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
