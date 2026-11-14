package com.personaltracker.domain.model

/**
 * Result data class for coin awarding with idempotency deduplication and cap enforcement (AD-1, AD-6, FR-8.2).
 */
data class AwardCoinsResult(
    val coinsAwarded: Int,
    val requestedCoins: Int,
    val isCapReached: Boolean,
    val isDuplicate: Boolean,
    val transaction: CoinTransaction? = null
)
