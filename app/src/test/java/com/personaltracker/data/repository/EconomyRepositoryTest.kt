package com.personaltracker.data.repository

import app.cash.turbine.test
import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.CompanionProfileDao
import com.personaltracker.data.local.db.daos.ShopItemDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.data.local.db.entities.CompanionProfileEntity
import com.personaltracker.data.local.db.entities.ShopItemEntity
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.EconomyEventTypes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EconomyRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var coinLedgerDao: TestCoinLedgerDao
    private lateinit var companionProfileDao: TestCompanionProfileDao
    private lateinit var shopItemDao: TestShopItemDao
    private lateinit var repository: EconomyRepositoryImpl

    private val testDate = LocalDate.parse("2026-11-14")
    private val startOfDayTimestamp = testDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val directTransactionRunner = object : TransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Before
    fun setUp() {
        coinLedgerDao = TestCoinLedgerDao()
        companionProfileDao = TestCompanionProfileDao()
        shopItemDao = TestShopItemDao()
        repository = EconomyRepositoryImpl(
            coinLedgerDao = coinLedgerDao,
            companionProfileDao = companionProfileDao,
            shopItemDao = shopItemDao,
            transactionRunner = directTransactionRunner,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testAwardCoinsDeduplicationReturnsZeroCoinsAndExistingTransaction() = runTest(testDispatcher) {
        val key = "TASK_COMP_task_101"

        // First award attempt
        val firstResult = repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_101",
            requestedAmount = 10,
            idempotencyKey = key,
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(10, firstResult.coinsAwarded)
        assertFalse(firstResult.isDuplicate)
        assertFalse(firstResult.isCapReached)
        assertEquals(10, repository.getLatestBalance())

        // Rapid duplicate award attempt with same compound key
        val duplicateResult = repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_101",
            requestedAmount = 10,
            idempotencyKey = key,
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(0, duplicateResult.coinsAwarded)
        assertEquals(10, duplicateResult.requestedCoins)
        assertTrue(duplicateResult.isDuplicate)
        assertFalse(duplicateResult.isCapReached)
        assertNotNull(duplicateResult.transaction)
        assertEquals(key, duplicateResult.transaction?.idempotencyKey)

        // Balance remains unchanged (10, not 20)
        assertEquals(10, repository.getLatestBalance())
    }

    @Test
    fun testAwardCoinsClampsAtCategoryCap() = runTest(testDispatcher) {
        // Pre-earn 48 task coins today (DAILY_TASK_CAP = 50)
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_pre_1",
                idempotencyKey = "TASK_COMP_pre_1",
                deltaAmount = 48,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                timestamp = startOfDayTimestamp + 1000
            )
        )
        assertEquals(48, repository.getLatestBalance())
        assertEquals(48, repository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION))

        // Complete an Important task (+7 coins requested) -> clamped to +2 coins
        val clampedResult = repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_clamp_1",
            requestedAmount = 7,
            idempotencyKey = "TASK_COMP_clamp_1",
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(2, clampedResult.coinsAwarded)
        assertEquals(7, clampedResult.requestedCoins)
        assertTrue(clampedResult.isCapReached)
        assertFalse(clampedResult.isDuplicate)
        assertEquals(50, repository.getLatestBalance())
        assertEquals(50, repository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION))

        // Subsequent task completions today award 0 coins
        val overflowResult = repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_overflow_1",
            requestedAmount = 5,
            idempotencyKey = "TASK_COMP_overflow_1",
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(0, overflowResult.coinsAwarded)
        assertEquals(5, overflowResult.requestedCoins)
        assertTrue(overflowResult.isCapReached)
        assertFalse(overflowResult.isDuplicate)
        assertEquals(50, repository.getLatestBalance())
    }

    @Test
    fun testAwardCoinsClampsAtGlobalFarmableCap() = runTest(testDispatcher) {
        // Task earned 50, Habit earned 40 -> 90 farmable coins earned today
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_farm_task",
                idempotencyKey = "TASK_COMP_farm_task",
                deltaAmount = 50,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                timestamp = startOfDayTimestamp + 1000
            )
        )
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_farm_habit",
                idempotencyKey = "HABIT_LOG_farm_habit",
                deltaAmount = 40,
                eventType = EconomyEventTypes.HABIT_COMPLETION,
                timestamp = startOfDayTimestamp + 2000
            )
        )
        assertEquals(90, repository.getDailyEarnedTotal(startOfDayTimestamp))

        // Routine completion requests 15 coins -> global farmable cap is 100 -> only 10 coins awarded
        val routineResult = repository.awardCoins(
            eventType = EconomyEventTypes.ROUTINE_COMPLETION,
            sourceEntityId = "routine_farm_1",
            requestedAmount = 15,
            idempotencyKey = "ROUTINE_COMP_farm_1_2026-11-14",
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(10, routineResult.coinsAwarded)
        assertEquals(15, routineResult.requestedCoins)
        assertTrue(routineResult.isCapReached)
        assertEquals(100, repository.getLatestBalance())

        // Next routine step requests 2 coins -> awards 0 coins due to global farmable cap reached
        val stepResult = repository.awardCoins(
            eventType = EconomyEventTypes.ROUTINE_COMPLETION,
            sourceEntityId = "routine_step_farm",
            requestedAmount = 2,
            idempotencyKey = "ROUTINE_COMP_step_farm_2026-11-14",
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(0, stepResult.coinsAwarded)
        assertTrue(stepResult.isCapReached)
    }

    @Test
    fun testStudySessionExemptFromGlobalFarmableCap() = runTest(testDispatcher) {
        // Pre-fill global farmable cap to 100
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_farm_full",
                idempotencyKey = "TASK_COMP_full",
                deltaAmount = 100,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                timestamp = startOfDayTimestamp + 1000
            )
        )

        // Study session completes for 15 coins -> exempt from 100 farmable cap, governed by DAILY_FOCUS_CAP (120)
        val studyResult = repository.awardCoins(
            eventType = EconomyEventTypes.STUDY_SESSION,
            sourceEntityId = "focus_sess_1",
            requestedAmount = 15,
            idempotencyKey = "STUDY_SESSION_sess_1",
            startOfDayTimestamp = startOfDayTimestamp
        )

        assertEquals(15, studyResult.coinsAwarded)
        assertFalse(studyResult.isCapReached)
        assertEquals(115, repository.getLatestBalance())
    }

    @Test
    fun testRecordReversalClampsDeductionAndPreventsNegativeBalance() = runTest(testDispatcher) {
        // Seed initial balance of 15 coins
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_seed",
                idempotencyKey = "TASK_COMP_seed",
                deltaAmount = 15,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                timestamp = startOfDayTimestamp + 1000
            )
        )
        assertEquals(15, repository.getLatestBalance())

        // 1. Valid reversal within balance: deduct 10 coins
        val firstReversal = repository.recordReversal(
            inboxId = "inbox_rev_1",
            amount = 10,
            idempotencyKey = "CAPTURE_UNDO_inbox_rev_1"
        )
        assertTrue(firstReversal.isSuccess)
        val tx1 = firstReversal.getOrNull()
        assertNotNull(tx1)
        assertEquals(-10, tx1?.deltaAmount)
        assertEquals(5, repository.getLatestBalance())

        // 2. Duplicate reversal key returns existing without second deduction
        val duplicateReversal = repository.recordReversal(
            inboxId = "inbox_rev_1",
            amount = 10,
            idempotencyKey = "CAPTURE_UNDO_inbox_rev_1"
        )
        assertTrue(duplicateReversal.isSuccess)
        assertEquals(5, repository.getLatestBalance())

        // 3. Second reversal exceeding remaining balance: balance is 5, amount is 20
        // Clamps deduction to current balance (5), resulting in balanceAfter = 0
        val clampedReversal = repository.recordReversal(
            inboxId = "inbox_rev_2",
            amount = 20,
            idempotencyKey = "CAPTURE_UNDO_inbox_rev_2"
        )
        assertTrue(clampedReversal.isSuccess)
        val tx2 = clampedReversal.getOrNull()
        assertNotNull(tx2)
        assertEquals(-5, tx2?.deltaAmount)
        assertEquals(0, repository.getLatestBalance())
    }

    @Test
    fun testGrossSpendQueryDefensePreventsCapRefarming() = runTest(testDispatcher) {
        // User earns 50 task coins today
        repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_earned_50",
            requestedAmount = 50,
            idempotencyKey = "TASK_COMP_earned_50",
            startOfDayTimestamp = startOfDayTimestamp
        )
        assertEquals(50, repository.getDailyEarnedTotal(startOfDayTimestamp))
        assertEquals(50, repository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION))

        // User triggers a reversal deduction of -20 coins
        repository.recordReversal(
            inboxId = "inbox_undo",
            amount = 20,
            idempotencyKey = "CAPTURE_UNDO_undo_defense"
        )
        assertEquals(30, repository.getLatestBalance())

        // Daily earned queries filter deltaAmount > 0, so earned total remains 50
        assertEquals(50, repository.getDailyEarnedTotal(startOfDayTimestamp))
        assertEquals(50, repository.getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION))

        // Subsequent task completions are still blocked by the daily cap
        val retryResult = repository.awardCoins(
            eventType = EconomyEventTypes.TASK_COMPLETION,
            sourceEntityId = "task_retry_cap",
            requestedAmount = 10,
            idempotencyKey = "TASK_COMP_retry_cap",
            startOfDayTimestamp = startOfDayTimestamp
        )
        assertEquals(0, retryResult.coinsAwarded)
        assertTrue(retryResult.isCapReached)
    }

    @Test
    fun testPurchaseShopItemDeductsBalanceAndAutoEquips() = runTest(testDispatcher) {
        // Pre-earn 50 coins
        repository.recordTransaction(
            CoinTransaction(
                id = "tx_seed_50",
                idempotencyKey = "TASK_COMP_seed_50",
                deltaAmount = 50,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                timestamp = startOfDayTimestamp
            )
        )
        val cap = ShopItemEntity("item_cap", "ACCESSORY", "Study Cap", 40, false, null)
        shopItemDao.insertShopItems(listOf(cap))

        repository.purchaseShopItem("item_cap", "SHOP_BUY_item_cap_1")

        assertEquals(10, repository.getLatestBalance())
        val updatedItem = shopItemDao.getShopItemById("item_cap")
        assertTrue(updatedItem?.isUnlocked == true)
        val profile = companionProfileDao.getProfile()
        assertEquals("item_cap", profile?.activeAccessoryId)

        // Rapid duplicate purchase is idempotent (does not deduct extra coins, preserves equip)
        repository.purchaseShopItem("item_cap", "SHOP_BUY_item_cap_2")
        assertEquals(10, repository.getLatestBalance())
        assertEquals("item_cap", companionProfileDao.getProfile()?.activeAccessoryId)
    }

    @Test
    fun testEquipShopItemUpdatesActiveAccessory() = runTest(testDispatcher) {
        repository.equipShopItem("item_scarf", true)
        assertEquals("item_scarf", companionProfileDao.getProfile()?.activeAccessoryId)

        repository.equipShopItem("item_scarf", false)
        assertNull(companionProfileDao.getProfile()?.activeAccessoryId)
    }

    // =========================================================================
    // Lightweight Test Fakes for EconomyRepositoryImpl Isolation
    // =========================================================================

    private class TestCoinLedgerDao : CoinLedgerDao {
        val transactions = MutableStateFlow<List<CoinLedgerEntity>>(emptyList())

        override fun getAllTransactions(): Flow<List<CoinLedgerEntity>> = transactions

        override suspend fun getTransactionByIdempotencyKey(key: String): CoinLedgerEntity? =
            transactions.value.find { it.idempotencyKey == key }

        override suspend fun getLatestBalance(): Int? =
            transactions.value.lastOrNull()?.balanceAfter

        override fun observeLatestBalance(): Flow<Int?> =
            transactions.map { it.lastOrNull()?.balanceAfter }

        override suspend fun insertTransaction(entry: CoinLedgerEntity) {
            if (transactions.value.any { it.idempotencyKey == entry.idempotencyKey }) {
                throw IllegalStateException("UNIQUE constraint failed: coin_ledger.idempotencyKey (${entry.idempotencyKey})")
            }
            transactions.value = transactions.value + entry
        }

        override suspend fun getDailyEarnedTotal(startOfDayTimestamp: Long): Int =
            transactions.value
                .filter { it.timestamp >= startOfDayTimestamp && it.deltaAmount > 0 }
                .sumOf { it.deltaAmount }

        override fun observeDailyEarnedTotal(startOfDayTimestamp: Long): Flow<Int> =
            transactions.map { list ->
                list.filter { it.timestamp >= startOfDayTimestamp && it.deltaAmount > 0 }
                    .sumOf { it.deltaAmount }
            }

        override suspend fun getDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Int =
            transactions.value
                .filter { it.timestamp >= startOfDayTimestamp && it.eventType == eventType && it.deltaAmount > 0 }
                .sumOf { it.deltaAmount }

        override fun observeDailyEarnedForCategory(startOfDayTimestamp: Long, eventType: String): Flow<Int> =
            transactions.map { list ->
                list.filter { it.timestamp >= startOfDayTimestamp && it.eventType == eventType && it.deltaAmount > 0 }
                    .sumOf { it.deltaAmount }
            }

        override fun getTransactionsSince(timestamp: Long): Flow<List<CoinLedgerEntity>> =
            transactions.map { list ->
                list.filter { it.timestamp >= timestamp }
                    .sortedWith(compareByDescending<CoinLedgerEntity> { it.timestamp }.thenByDescending { it.id })
            }
    }

    private class TestCompanionProfileDao : CompanionProfileDao {
        val profile = MutableStateFlow<CompanionProfileEntity?>(null)

        override suspend fun getProfile(): CompanionProfileEntity? = profile.value
        override fun observeProfile(): Flow<CompanionProfileEntity?> = profile
        override suspend fun upsertProfile(profile: CompanionProfileEntity) {
            this.profile.value = profile
        }
    }

    private class TestShopItemDao : ShopItemDao {
        val items = MutableStateFlow<List<ShopItemEntity>>(emptyList())

        override fun getAllShopItems(): Flow<List<ShopItemEntity>> = items
        override suspend fun getShopItemById(id: String): ShopItemEntity? = items.value.find { it.id == id }
        override suspend fun insertShopItems(items: List<ShopItemEntity>) {
            val ids = items.map { it.id }.toSet()
            this.items.value = this.items.value.filter { it.id !in ids } + items
        }
        override suspend fun updateShopItem(item: ShopItemEntity) {
            this.items.value = this.items.value.filter { it.id != item.id } + item
        }
    }
}
