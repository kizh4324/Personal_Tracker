package com.personaltracker.domain.repository

import com.personaltracker.domain.model.AwardCoinsResult
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for the coin ledger, companion pet profile, and shop catalog
 * (FR-8.1, FR-8.2, FR-8.4, AD-1, AD-3, AD-4, AD-6).
 */
interface EconomyRepository {
    fun getAllTransactions(): Flow<List<CoinTransaction>>
    suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction?
    suspend fun getLatestBalance(): Int
    fun observeLatestBalance(): Flow<Int>

    fun observeBalance(): Flow<Int> = observeLatestBalance()
    suspend fun getBalance(): Int = getLatestBalance()

    suspend fun getDailyEarnedTotal(startOfDay: Long): Int = 0
    fun observeDailyEarnedTotal(startOfDay: Long): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    suspend fun getDailyEarnedForCategory(startOfDay: Long, eventType: String): Int = 0
    fun observeDailyEarnedForCategory(startOfDay: Long, eventType: String): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
    fun observeTransactionsSince(timestamp: Long): Flow<List<CoinTransaction>> = kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun recordTransaction(transaction: CoinTransaction)
    suspend fun recordReversal(inboxId: String, amount: Int, idempotencyKey: String): Result<CoinTransaction> =
        Result.failure(UnsupportedOperationException())
    suspend fun awardCoins(
        eventType: String,
        sourceEntityId: String,
        requestedAmount: Int,
        idempotencyKey: String,
        startOfDayTimestamp: Long
    ): AwardCoinsResult = AwardCoinsResult(
        coinsAwarded = requestedAmount,
        requestedCoins = requestedAmount,
        isCapReached = false,
        isDuplicate = false
    )

    suspend fun purchaseShopItem(
        shopItemId: String,
        idempotencyKey: String
    )

    suspend fun equipShopItem(
        shopItemId: String,
        equip: Boolean
    ) = Unit

    suspend fun getCompanionProfile(): CompanionProfile?
    fun observeCompanionProfile(): Flow<CompanionProfile?>
    suspend fun updateCompanionProfile(profile: CompanionProfile)

    fun getAllShopItems(): Flow<List<ShopItem>>
    suspend fun getShopItemById(id: String): ShopItem?
    suspend fun saveShopItems(items: List<ShopItem>)
    suspend fun updateShopItem(item: ShopItem)
}
