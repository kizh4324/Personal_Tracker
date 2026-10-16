package com.personaltracker.domain.repository

import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for the coin ledger, companion pet profile, and shop catalog (FR-8.1, FR-8.2, FR-8.4, AD-1, AD-4).
 */
interface EconomyRepository {
    fun getAllTransactions(): Flow<List<CoinTransaction>>
    suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction?
    suspend fun getLatestBalance(): Int
    fun observeLatestBalance(): Flow<Int>
    suspend fun recordTransaction(transaction: CoinTransaction)
    suspend fun purchaseShopItem(
        shopItemId: String,
        idempotencyKey: String
    )
    suspend fun getCompanionProfile(): CompanionProfile?
    fun observeCompanionProfile(): Flow<CompanionProfile?>
    suspend fun updateCompanionProfile(profile: CompanionProfile)
    fun getAllShopItems(): Flow<List<ShopItem>>
    suspend fun getShopItemById(id: String): ShopItem?
    suspend fun saveShopItems(items: List<ShopItem>)
    suspend fun updateShopItem(item: ShopItem)
}
