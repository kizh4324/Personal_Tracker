package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.CompanionProfileDao
import com.personaltracker.data.local.db.daos.ShopItemDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.AwardCoinsResult
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.EconomyEventTypes
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

    override fun observeBalance(): Flow<Int> = observeLatestBalance()

    override suspend fun getBalance(): Int = getLatestBalance()

    override suspend fun getDailyEarnedTotal(startOfDay: Long): Int =
        withContext(ioDispatcher) {
            coinLedgerDao.getDailyEarnedTotal(startOfDay)
        }

    override fun observeDailyEarnedTotal(startOfDay: Long): Flow<Int> =
        coinLedgerDao.observeDailyEarnedTotal(startOfDay)
            .flowOn(ioDispatcher)

    override suspend fun getDailyEarnedForCategory(startOfDay: Long, eventType: String): Int =
        withContext(ioDispatcher) {
            coinLedgerDao.getDailyEarnedForCategory(startOfDay, eventType)
        }

    override fun observeDailyEarnedForCategory(startOfDay: Long, eventType: String): Flow<Int> =
        coinLedgerDao.observeDailyEarnedForCategory(startOfDay, eventType)
            .flowOn(ioDispatcher)

    override fun observeTransactionsSince(timestamp: Long): Flow<List<CoinTransaction>> =
        coinLedgerDao.getTransactionsSince(timestamp)
            .map { list -> list.map { it.toDomain() } }
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
                val newBalance = try {
                    Math.addExact(currentBalance, transaction.deltaAmount)
                } catch (_: ArithmeticException) {
                    throw IllegalStateException("Coin balance arithmetic overflow")
                }
                check(newBalance >= 0) { "Insufficient coin balance: $newBalance" }

                val entity = transaction.toEntity().copy(balanceAfter = newBalance)
                coinLedgerDao.insertTransaction(entity)
            }
        }

    override suspend fun recordReversal(
        inboxId: String,
        amount: Int,
        idempotencyKey: String
    ): Result<CoinTransaction> = withContext(ioDispatcher) {
        runCatching {
            IdempotencyValidator.validate(idempotencyKey)
            transactionRunner {
                val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
                if (existing != null) {
                    return@transactionRunner existing.toDomain()
                }

                val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
                val deduction = minOf(amount.coerceAtLeast(0), currentBalance)
                val newBalance = currentBalance - deduction

                val entity = CoinLedgerEntity(
                    id = UUID.randomUUID().toString(),
                    idempotencyKey = idempotencyKey,
                    deltaAmount = -deduction,
                    eventType = EconomyEventTypes.CAPTURE_REVERSAL,
                    sourceEntityId = inboxId,
                    balanceAfter = newBalance,
                    timestamp = System.currentTimeMillis()
                )
                coinLedgerDao.insertTransaction(entity)
                entity.toDomain()
            }
        }
    }

    override suspend fun awardCoins(
        eventType: String,
        sourceEntityId: String,
        requestedAmount: Int,
        idempotencyKey: String,
        startOfDayTimestamp: Long
    ): AwardCoinsResult = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        require(requestedAmount >= 0) { "Requested amount cannot be negative: $requestedAmount" }

        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return@transactionRunner AwardCoinsResult(
                    coinsAwarded = 0,
                    requestedCoins = requestedAmount,
                    isCapReached = false,
                    isDuplicate = true,
                    transaction = existing.toDomain()
                )
            }

            val isFarmableCategory = eventType in GamificationLedgerEngine.FARMABLE_EVENT_TYPES
            val categoryCap = when (eventType) {
                EconomyEventTypes.TASK_COMPLETION -> GamificationLedgerEngine.DAILY_TASK_CAP
                EconomyEventTypes.HABIT_COMPLETION -> GamificationLedgerEngine.DAILY_HABIT_CAP
                EconomyEventTypes.STUDY_SESSION -> GamificationLedgerEngine.DAILY_FOCUS_CAP
                else -> Int.MAX_VALUE
            }

            val categoryEarned = coinLedgerDao.getDailyEarnedForCategory(startOfDayTimestamp, eventType)
            val globalFarmableEarned = if (isFarmableCategory) {
                val taskEarned = coinLedgerDao.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION)
                val habitEarned = coinLedgerDao.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.HABIT_COMPLETION)
                val routineEarned = coinLedgerDao.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.ROUTINE_COMPLETION)
                taskEarned + habitEarned + routineEarned
            } else 0

            val coinsAwarded = GamificationLedgerEngine.calculateCappedReward(
                requestedAmount = requestedAmount,
                categoryEarned = categoryEarned,
                categoryCap = categoryCap,
                globalFarmableEarned = globalFarmableEarned,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = isFarmableCategory
            )

            val isCapReached = (categoryEarned + coinsAwarded >= categoryCap) ||
                (isFarmableCategory && globalFarmableEarned + coinsAwarded >= GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP) ||
                (requestedAmount > 0 && coinsAwarded < requestedAmount)

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            val newBalance = try {
                Math.addExact(currentBalance, coinsAwarded)
            } catch (_: ArithmeticException) {
                throw IllegalStateException("Coin balance arithmetic overflow")
            }

            val entity = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = coinsAwarded,
                eventType = eventType,
                sourceEntityId = sourceEntityId,
                balanceAfter = newBalance,
                timestamp = System.currentTimeMillis()
            )

            try {
                coinLedgerDao.insertTransaction(entity)
            } catch (e: Exception) {
                val existingAfterRace = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
                if (existingAfterRace != null) {
                    return@transactionRunner AwardCoinsResult(
                        coinsAwarded = 0,
                        requestedCoins = requestedAmount,
                        isCapReached = false,
                        isDuplicate = true,
                        transaction = existingAfterRace.toDomain()
                    )
                }
                throw e
            }

            AwardCoinsResult(
                coinsAwarded = coinsAwarded,
                requestedCoins = requestedAmount,
                isCapReached = isCapReached,
                isDuplicate = false,
                transaction = entity.toDomain()
            )
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
                eventType = EconomyEventTypes.SHOP_PURCHASE,
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
