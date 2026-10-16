package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.CompanionProfileDao
import com.personaltracker.data.local.db.daos.ShopItemDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.repository.EconomyRepository
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
internal class EconomyRepositoryImpl @Inject constructor(
    private val coinLedgerDao: CoinLedgerDao,
    private val companionProfileDao: CompanionProfileDao,
    private val shopItemDao: ShopItemDao,
    private val transactionRunner: TransactionRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EconomyRepository {

    override fun getAllTransactions(): Flow<List<CoinTransaction>> =
        coinLedgerDao.getAllTransactions()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction? =
        withContext(ioDispatcher) {
            coinLedgerDao.getTransactionByIdempotencyKey(key)?.toDomain()
        }

    override suspend fun getLatestBalance(): Int =
        withContext(ioDispatcher) {
            coinLedgerDao.getLatestBalance() ?: 0
        }

    override fun observeLatestBalance(): Flow<Int> =
        coinLedgerDao.observeLatestBalance()
            .map { it ?: 0 }
            .flowOn(ioDispatcher)

    override suspend fun recordTransaction(transaction: CoinTransaction) =
        withContext(ioDispatcher) {
            IdempotencyValidator.validate(transaction.idempotencyKey)
            transactionRunner {
                val existing = coinLedgerDao.getTransactionByIdempotencyKey(transaction.idempotencyKey)
                if (existing != null) {
                    throw IllegalStateException("Duplicate transaction idempotency key: ${transaction.idempotencyKey}")
                }

                val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
                val newBalance = currentBalance + transaction.deltaAmount
                check(newBalance >= 0) { "Insufficient coin balance: $newBalance" }

                val entity = transaction.toEntity().copy(balanceAfter = newBalance)
                coinLedgerDao.insertTransaction(entity)
            }
        }

    override suspend fun purchaseShopItem(
        shopItemId: String,
        idempotencyKey: String
    ) = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                throw IllegalStateException("Duplicate transaction idempotency key: $idempotencyKey")
            }

            val item = shopItemDao.getShopItemById(shopItemId)
                ?: throw IllegalArgumentException("Shop item not found with id: $shopItemId")

            require(item.priceCoins >= 0) { "Shop item price cannot be negative: ${item.priceCoins}" }

            if (item.isUnlocked) {
                throw IllegalStateException("Shop item already unlocked: $shopItemId")
            }

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            if (currentBalance < item.priceCoins) {
                throw IllegalStateException("Insufficient coin balance: $currentBalance < ${item.priceCoins}")
            }

            val newBalance = currentBalance - item.priceCoins
            val timestamp = System.currentTimeMillis()

            val ledgerEntry = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = -item.priceCoins,
                eventType = "SHOP_PURCHASE",
                sourceEntityId = shopItemId,
                balanceAfter = newBalance,
                timestamp = timestamp
            )
            coinLedgerDao.insertTransaction(ledgerEntry)

            shopItemDao.updateShopItem(
                item.copy(
                    isUnlocked = true,
                    unlockedTimestamp = timestamp
                )
            )
        }
    }

    override suspend fun getCompanionProfile(): CompanionProfile? =
        withContext(ioDispatcher) {
            companionProfileDao.getProfile()?.toDomain()
        }

    override fun observeCompanionProfile(): Flow<CompanionProfile?> =
        companionProfileDao.observeProfile()
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun updateCompanionProfile(profile: CompanionProfile) =
        withContext(ioDispatcher) {
            companionProfileDao.insertOrUpdateProfile(profile.toEntity())
        }

    override fun getAllShopItems(): Flow<List<ShopItem>> =
        shopItemDao.getAllShopItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getShopItemById(id: String): ShopItem? =
        withContext(ioDispatcher) {
            shopItemDao.getShopItemById(id)?.toDomain()
        }

    override suspend fun saveShopItems(items: List<ShopItem>) =
        withContext(ioDispatcher) {
            shopItemDao.insertShopItems(items.map { it.toEntity() })
        }

    override suspend fun updateShopItem(item: ShopItem) =
        withContext(ioDispatcher) {
            shopItemDao.updateShopItem(item.toEntity())
        }
}
