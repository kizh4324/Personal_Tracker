package com.personaltracker.data.repository

import app.cash.turbine.test
import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.CompanionProfileDao
import com.personaltracker.data.local.db.daos.DayOverrideDao
import com.personaltracker.data.local.db.daos.DayTypeDao
import com.personaltracker.data.local.db.daos.HabitDao
import com.personaltracker.data.local.db.daos.InterventionRuleDao
import com.personaltracker.data.local.db.daos.RoutineDao
import com.personaltracker.data.local.db.daos.ShopItemDao
import com.personaltracker.data.local.db.daos.StudySessionDao
import com.personaltracker.data.local.db.daos.TaskDao
import com.personaltracker.data.local.db.daos.UnfiledInboxDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.data.local.db.entities.CompanionProfileEntity
import com.personaltracker.data.local.db.entities.DayOverrideEntity
import com.personaltracker.data.local.db.entities.DayTypeEntity
import com.personaltracker.data.local.db.entities.HabitEntity
import com.personaltracker.data.local.db.entities.HabitLogEntity
import com.personaltracker.data.local.db.entities.InterventionRuleEntity
import com.personaltracker.data.local.db.entities.RoutineEntity
import com.personaltracker.data.local.db.entities.RoutineStepEntity
import com.personaltracker.data.local.db.entities.ShopItemEntity
import com.personaltracker.data.local.db.entities.StudySessionEntity
import com.personaltracker.data.local.db.entities.TaskEntity
import com.personaltracker.data.local.db.entities.UnfiledCaptureInboxEntity
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeTransactionRunner = object : TransactionRunner {
        override suspend operator fun <T> invoke(block: suspend () -> T): T = block()
    }

    // ==========================================
    // 1. TaskRepository & Flow SSOT Tests
    // ==========================================

    @Test
    fun testTaskRepositoryFlowEmitsUpdatedListAndMapsToDomain() = runTest(testDispatcher) {
        val fakeTaskDao = FakeTaskDao()
        val fakeCoinDao = FakeCoinLedgerDao()
        val repo = TaskRepositoryImpl(fakeTaskDao, fakeCoinDao, fakeTransactionRunner, testDispatcher)

        repo.getTasksForDate("2026-10-15").test {
            // Initial emission: empty
            assertEquals(emptyList<Task>(), awaitItem())

            // Insert task
            val task = Task(
                id = "task-1",
                title = "Study Mathematics",
                scheduledDate = "2026-10-15",
                estimatedDurationMinutes = 45,
                deliveryIntensity = "IMPORTANT"
            )
            repo.saveTask(task)

            // Flow emits updated list with domain model
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("task-1", updated[0].id)
            assertEquals("Study Mathematics", updated[0].title)
            assertEquals("IMPORTANT", updated[0].deliveryIntensity)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testTaskCompletionWithRewardAtomicityAndIdempotency() = runTest(testDispatcher) {
        val fakeTaskDao = FakeTaskDao()
        val fakeCoinDao = FakeCoinLedgerDao()
        val repo = TaskRepositoryImpl(fakeTaskDao, fakeCoinDao, fakeTransactionRunner, testDispatcher)

        val task = Task(
            id = "task-100",
            title = "Write Unit Tests",
            scheduledDate = "2026-10-15"
        )
        repo.saveTask(task)

        // Complete task with valid compound key: TASK_COMP_<id>
        val key = "TASK_COMP_${task.id}"
        repo.completeTaskWithReward(
            taskId = task.id,
            coinReward = 10,
            idempotencyKey = key
        )

        // Verify task state is COMPLETED
        val completedTask = repo.getTaskById("task-100")
        assertNotNull(completedTask)
        assertEquals("COMPLETED", completedTask?.state)
        assertNotNull(completedTask?.completionTimestamp)

        // Verify coin ledger entry and balance
        assertEquals(10, fakeCoinDao.getLatestBalance())
        val tx = fakeCoinDao.getTransactionByIdempotencyKey(key)
        assertNotNull(tx)
        assertEquals(10, tx?.deltaAmount)
        assertEquals("TASK_COMPLETION", tx?.eventType)

        // Attempting completion again with same idempotency key must fail atomically
        try {
            repo.completeTaskWithReward(
                taskId = task.id,
                coinReward = 10,
                idempotencyKey = key
            )
            fail("Duplicate coin transaction idempotency key must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Duplicate") == true)
        }

        // Ledger balance remains 10 (not duplicated)
        assertEquals(10, fakeCoinDao.getLatestBalance())
    }

    // ==========================================
    // 2. EconomyRepository & Idempotency Key Tests (AD-4)
    // ==========================================

    @Test
    fun testEconomyRepositoryEnforcesCompoundIdempotencyFormat() = runTest(testDispatcher) {
        val fakeCoinDao = FakeCoinLedgerDao()
        val fakeCompanionDao = FakeCompanionProfileDao()
        val fakeShopDao = FakeShopItemDao()
        val repo = EconomyRepositoryImpl(
            fakeCoinDao,
            fakeCompanionDao,
            fakeShopDao,
            fakeTransactionRunner,
            testDispatcher
        )

        // Invalid key: lacks 3 parts
        val invalidTx = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "INVALIDKEY",
            deltaAmount = 15,
            eventType = "TASK_COMPLETION"
        )
        try {
            repo.recordTransaction(invalidTx)
            fail("Key lacking compound format {DOMAIN}_{ACTION}_{ID} must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("compound format") == true)
        }

        // Only two parts: also invalid
        val twoPartTx = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "TASK_COMP",
            deltaAmount = 15,
            eventType = "TASK_COMPLETION"
        )
        try {
            repo.recordTransaction(twoPartTx)
            fail("Key with only 2 segments must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("compound format") == true)
        }

        // Valid compound key: TASK_COMP_123
        val validTx = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "TASK_COMP_123",
            deltaAmount = 15,
            eventType = "TASK_COMPLETION"
        )
        repo.recordTransaction(validTx)
        assertEquals(15, repo.getLatestBalance())
    }

    @Test
    fun testEconomyRepositoryDuplicateKeyRejectionPreservesLedger() = runTest(testDispatcher) {
        val fakeCoinDao = FakeCoinLedgerDao()
        val fakeCompanionDao = FakeCompanionProfileDao()
        val fakeShopDao = FakeShopItemDao()
        val repo = EconomyRepositoryImpl(
            fakeCoinDao,
            fakeCompanionDao,
            fakeShopDao,
            fakeTransactionRunner,
            testDispatcher
        )

        val tx1 = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "STUDY_SESSION_sess1",
            deltaAmount = 25,
            eventType = "STUDY_SESSION"
        )
        repo.recordTransaction(tx1)
        assertEquals(25, repo.getLatestBalance())

        // Re-inserting identical idempotency key
        val txDuplicate = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "STUDY_SESSION_sess1",
            deltaAmount = 25,
            eventType = "STUDY_SESSION"
        )
        try {
            repo.recordTransaction(txDuplicate)
            fail("Duplicate key must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Duplicate") == true)
        }

        // Balance remains unchanged at 25
        assertEquals(25, repo.getLatestBalance())
    }

    @Test
    fun testEconomyRepositoryPurchaseShopItemTransactionalSuccessAndFailures() = runTest(testDispatcher) {
        val fakeCoinDao = FakeCoinLedgerDao()
        val fakeCompanionDao = FakeCompanionProfileDao()
        val fakeShopDao = FakeShopItemDao()
        val repo = EconomyRepositoryImpl(
            fakeCoinDao,
            fakeCompanionDao,
            fakeShopDao,
            fakeTransactionRunner,
            testDispatcher
        )

        // Seed initial balance 50 coins
        repo.recordTransaction(
            CoinTransaction(
                id = UUID.randomUUID().toString(),
                idempotencyKey = "INIT_BALANCE_user",
                deltaAmount = 50,
                eventType = "INITIAL"
            )
        )
        assertEquals(50, repo.getLatestBalance())

        // Add shop item: price 30 coins
        val item = ShopItem(
            id = "hat_wizard",
            category = "ACCESSORY",
            name = "Wizard Hat",
            priceCoins = 30
        )
        repo.saveShopItems(listOf(item))

        // Purchase item with compound key
        val buyKey = "SHOP_BUY_hat_wizard_1001"
        repo.purchaseShopItem(item.id, buyKey)

        // Balance deducted from 50 to 20
        assertEquals(20, repo.getLatestBalance())

        // Item is unlocked
        val purchased = repo.getShopItemById("hat_wizard")
        assertNotNull(purchased)
        assertTrue(purchased?.isUnlocked == true)

        // Purchasing again must fail: item already unlocked
        try {
            repo.purchaseShopItem("hat_wizard", "SHOP_BUY_hat_wizard_1002")
            fail("Already unlocked item purchase must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already unlocked") == true)
        }

        // Insufficient funds test
        val expensiveItem = ShopItem(
            id = "castle_decor",
            category = "BACKGROUND_DECOR",
            name = "Castle",
            priceCoins = 100
        )
        repo.saveShopItems(listOf(expensiveItem))

        try {
            repo.purchaseShopItem("castle_decor", "SHOP_BUY_castle_1003")
            fail("Purchase exceeding balance must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Insufficient coin balance") == true)
        }

        // Balance remains 20
        assertEquals(20, repo.getLatestBalance())
    }

    // ==========================================
    // 3. InterventionRuleRepository Tests
    // ==========================================

    @Test
    fun testInterventionRuleDeletionGracefulAndFlowUpdate() = runTest(testDispatcher) {
        val fakeDao = FakeInterventionRuleDao()
        val repo = InterventionRuleRepositoryImpl(fakeDao, testDispatcher)

        repo.getBlockedAppRules().test {
            assertEquals(emptyList<InterventionRule>(), awaitItem())

            // Add rule
            val rule = InterventionRule(
                packageName = "com.distraction.app",
                appDisplayName = "Distraction App",
                isStrictBlocked = true,
                dailyLimitMinutes = 15
            )
            repo.saveRule(rule)

            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("com.distraction.app", list[0].packageName)

            // Delete rule
            repo.deleteRule("com.distraction.app")
            val emptyList = awaitItem()
            assertEquals(0, emptyList.size)

            // Deleting an absent rule gracefully succeeds
            repo.deleteRule("com.distraction.app")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==========================================
    // 4. HabitRepository & StudySessionRepository Tests
    // ==========================================

    @Test
    fun testHabitAndStudySessionRepositories() = runTest(testDispatcher) {
        val habitDao = FakeHabitDao()
        val studyDao = FakeStudySessionDao()
        val coinDao = FakeCoinLedgerDao()

        val habitRepo = HabitRepositoryImpl(habitDao, coinDao, fakeTransactionRunner, testDispatcher)
        val studyRepo = StudySessionRepositoryImpl(studyDao, coinDao, fakeTransactionRunner, testDispatcher)

        // Habit creation & log with reward
        val habit = Habit(
            id = "habit-water",
            name = "Drink 2L Water",
            targetDaysOfWeek = "1,2,3,4,5,6,7"
        )
        habitRepo.saveHabit(habit)

        val log = HabitLog(
            id = "log-1",
            habitId = "habit-water",
            logDate = "2026-10-15",
            status = "COMPLETED"
        )
        habitRepo.logHabitWithReward(log, coinReward = 5, idempotencyKey = "HABIT_LOG_habit-water_20261015")
        assertEquals(5, coinDao.getLatestBalance())

        // Study session with reward
        val session = StudySession(
            id = "study-1",
            subjectTag = "Physics",
            sessionDate = "2026-10-15",
            startTime = "14:00",
            durationMinutes = 60,
            coinsEarned = 10
        )
        studyRepo.recordSessionWithReward(session, idempotencyKey = "STUDY_SESSION_study-1")
        assertEquals(15, coinDao.getLatestBalance())

        val totalMinutes = studyRepo.getTotalStudyMinutesForDate("2026-10-15")
        assertEquals(60, totalMinutes)
    }

    // ==========================================
    // 5. DayType, Routine & UnfiledInbox Tests
    // ==========================================

    @Test
    fun testDayTypeRoutineAndUnfiledInboxRepositories() = runTest(testDispatcher) {
        val dayTypeDao = FakeDayTypeDao()
        val dayOverrideDao = FakeDayOverrideDao()
        val routineDao = FakeRoutineDao()
        val unfiledInboxDao = FakeUnfiledInboxDao()

        val dayTypeRepo = DayTypeRepositoryImpl(dayTypeDao, dayOverrideDao, testDispatcher)
        val routineRepo = RoutineRepositoryImpl(routineDao, testDispatcher)
        val inboxRepo = UnfiledInboxRepositoryImpl(unfiledInboxDao, testDispatcher)

        // DayType
        val dt = DayType(
            id = "focus_heavy",
            name = "Heavy Study",
            colorHex = "#2563EB",
            targetStudyMinutes = 180,
            isDefault = false,
            defaultDaysOfWeek = "2,4"
        )
        dayTypeRepo.saveDayType(dt)
        assertEquals("Heavy Study", dayTypeRepo.getDayTypeById("focus_heavy")?.name)

        // Routine with steps
        val routine = Routine(
            id = "routine-1",
            dayTypeId = "focus_heavy",
            title = "Morning Deep Work",
            targetStartTime = "08:00",
            totalEstimatedMinutes = 60
        )
        val step1 = RoutineStep(id = "s1", routineId = "routine-1", orderIndex = 0, title = "Setup desk", durationMinutes = 10)
        val step2 = RoutineStep(id = "s2", routineId = "routine-1", orderIndex = 1, title = "Focus block", durationMinutes = 50)
        routineRepo.saveRoutineWithSteps(routine, listOf(step1, step2))

        routineRepo.getStepsForRoutine("routine-1").test {
            val steps = awaitItem()
            assertEquals(2, steps.size)
            assertEquals("Setup desk", steps[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        // Unfiled inbox
        val inboxItem = UnfiledInboxItem(
            id = "inbox-1",
            rawTranscriptText = "Remind me to buy notebook",
            draftAttributesJson = "{}",
            parserConfidence = 0.4f,
            captureSource = "OFFLINE_STT"
        )
        inboxRepo.saveInboxItem(inboxItem)
        assertEquals("inbox-1", inboxRepo.getInboxItemById("inbox-1")?.id)
        inboxRepo.deleteInboxItem(inboxItem)
        assertNull(inboxRepo.getInboxItemById("inbox-1"))
    }

    @Test
    fun testReviewPatchEdgeCasesAndCompanionProfile() = runTest(testDispatcher) {
        val coinDao = FakeCoinLedgerDao()
        val companionDao = FakeCompanionProfileDao()
        val shopDao = FakeShopItemDao()
        val taskDao = FakeTaskDao()
        val habitDao = FakeHabitDao()
        val studyDao = FakeStudySessionDao()

        val ecoRepo = EconomyRepositoryImpl(coinDao, companionDao, shopDao, fakeTransactionRunner, testDispatcher)
        val taskRepo = TaskRepositoryImpl(taskDao, coinDao, fakeTransactionRunner, testDispatcher)
        val habitRepo = HabitRepositoryImpl(habitDao, coinDao, fakeTransactionRunner, testDispatcher)
        val studyRepo = StudySessionRepositoryImpl(studyDao, coinDao, fakeTransactionRunner, testDispatcher)

        // 1. Companion profile lifecycle
        ecoRepo.observeCompanionProfile().test {
            assertNull(awaitItem())

            val profile = com.personaltracker.domain.model.CompanionProfile(
                id = 1,
                speciesName = "Fox",
                level = 1,
                currentEnergyUnits = 50,
                activeAccessoryId = null,
                activeBackgroundId = null,
                currentExpressionOrdinal = 0
            )
            ecoRepo.updateCompanionProfile(profile)

            val updated = awaitItem()
            assertNotNull(updated)
            assertEquals("Fox", updated?.speciesName)
            assertEquals(1, updated?.level)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Fox", ecoRepo.getCompanionProfile()?.speciesName)

        // 2. Negative coin balance prevention
        val debitTx = CoinTransaction(
            id = UUID.randomUUID().toString(),
            idempotencyKey = "SHOP_BUY_item_1",
            deltaAmount = -50,
            eventType = "SHOP_PURCHASE"
        )
        try {
            ecoRepo.recordTransaction(debitTx)
            fail("Negative balance transaction must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Insufficient coin balance") == true)
        }

        // 3. Negative shop item price prevention
        val negativeItem = ShopItem(id = "neg_item", category = "ACCESSORY", name = "Bug Item", priceCoins = -10)
        ecoRepo.saveShopItems(listOf(negativeItem))
        try {
            ecoRepo.purchaseShopItem("neg_item", "SHOP_BUY_neg_1")
            fail("Negative priced shop item purchase must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot be negative") == true)
        }

        // 4. Task re-completion and negative reward prevention
        val task = Task(id = "task-patch", title = "Patch Task", scheduledDate = "2026-10-15")
        taskRepo.saveTask(task)

        try {
            taskRepo.completeTaskWithReward("task-patch", System.currentTimeMillis(), -5, "TASK_COMP_patch_neg")
            fail("Negative coin reward on task must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot be negative") == true)
        }

        taskRepo.completeTaskWithReward("task-patch", System.currentTimeMillis(), 10, "TASK_COMP_patch_1")
        try {
            taskRepo.completeTaskWithReward("task-patch", System.currentTimeMillis(), 10, "TASK_COMP_patch_2")
            fail("Completing an already completed task must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already completed") == true)
        }

        // 5. Habit non-completed status and negative reward prevention
        val habit = Habit(id = "habit-patch", name = "Test Habit", targetDaysOfWeek = "1,2,3,4,5,6,7")
        habitRepo.saveHabit(habit)

        val missedLog = HabitLog(id = "hl-miss", habitId = "habit-patch", logDate = "2026-10-15", status = "MISSED")
        try {
            habitRepo.logHabitWithReward(missedLog, 5, "HABIT_LOG_patch_miss")
            fail("Rewarding non-completed habit must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("COMPLETED") == true)
        }

        val completedLog = HabitLog(id = "hl-comp", habitId = "habit-patch", logDate = "2026-10-15", status = "COMPLETED")
        habitRepo.logHabitWithReward(completedLog, 5, "HABIT_LOG_patch_comp")
        try {
            habitRepo.logHabitWithReward(completedLog, 5, "HABIT_LOG_patch_comp2")
            fail("Logging already completed habit for date must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already completed") == true)
        }

        // 6. Zero-coin study session idempotency recording
        val zeroSession = StudySession(
            id = "study-zero",
            subjectTag = "Break",
            sessionDate = "2026-10-15",
            startTime = "10:00",
            durationMinutes = 5,
            coinsEarned = 0
        )
        studyRepo.recordSessionWithReward(zeroSession, "STUDY_SESSION_zero_1")
        assertNotNull(coinDao.getTransactionByIdempotencyKey("STUDY_SESSION_zero_1"))
        try {
            studyRepo.recordSessionWithReward(zeroSession, "STUDY_SESSION_zero_1")
            fail("Duplicate zero-coin study session idempotency key must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Duplicate") == true)
        }

        // 7. Whitespace in idempotency key rejected
        try {
            IdempotencyValidator.validate("TASK_ COMP_1")
            fail("Whitespace in idempotency key must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("without whitespace") == true)
        }

        // 8. Habit log updates reuse existing ID to prevent unique index collision on (habitId, logDate)
        val initialLog = HabitLog(id = "log-original-1", habitId = "habit-patch", logDate = "2026-10-16", status = "MISSED")
        habitRepo.logHabit(initialLog)
        val updateLog = HabitLog(id = "log-new-2", habitId = "habit-patch", logDate = "2026-10-16", status = "SLACK_USED")
        habitRepo.logHabit(updateLog)
        val fetchedLog = habitRepo.getLogForDate("habit-patch", "2026-10-16")
        assertNotNull(fetchedLog)
        assertEquals("log-original-1", fetchedLog?.id)
        assertEquals("SLACK_USED", fetchedLog?.status)

        // 9. Cancelled task cannot be completed with reward
        val cancelledTask = Task(id = "task-cancelled", title = "Cancelled Task", scheduledDate = "2026-10-15", state = "CANCELLED")
        taskRepo.saveTask(cancelledTask)
        try {
            taskRepo.completeTaskWithReward("task-cancelled", System.currentTimeMillis(), 10, "TASK_COMP_cancelled_1")
            fail("Completing a cancelled task must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("cancelled task") == true)
        }

        // 10. Non-positive completion timestamp rejected
        val pendingTask = Task(id = "task-pending-ts", title = "Pending TS Task", scheduledDate = "2026-10-15")
        taskRepo.saveTask(pendingTask)
        try {
            taskRepo.completeTaskWithReward("task-pending-ts", 0L, 10, "TASK_COMP_ts_zero")
            fail("Non-positive completion timestamp must fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("completionTimestamp must be positive") == true)
        }

        // 11. RoutineRepository.saveRoutine persists embedded steps
        val routineDao = FakeRoutineDao()
        val routineRepo = RoutineRepositoryImpl(routineDao, testDispatcher)
        val routineWithSteps = Routine(
            id = "routine-embedded",
            dayTypeId = "dt-default",
            title = "Morning Flow",
            targetStartTime = "07:00",
            totalEstimatedMinutes = 45,
            steps = listOf(
                RoutineStep(id = "step-1", routineId = "routine-embedded", orderIndex = 0, title = "Drink Water", durationMinutes = 5),
                RoutineStep(id = "step-2", routineId = "routine-embedded", orderIndex = 1, title = "Stretch", durationMinutes = 15)
            )
        )
        routineRepo.saveRoutine(routineWithSteps)
        routineRepo.getStepsForRoutine("routine-embedded").test {
            val stepsList = awaitItem()
            assertEquals(2, stepsList.size)
            assertEquals("Drink Water", stepsList[0].title)
            assertEquals("Stretch", stepsList[1].title)
            cancelAndIgnoreRemainingEvents()
        }

        // 12. Coin ledger balance arithmetic overflow protection
        val overflowTx = CoinTransaction(
            id = "tx-overflow",
            idempotencyKey = "ECO_OVERFLOW_1",
            deltaAmount = Int.MAX_VALUE,
            eventType = "BONUS"
        )
        try {
            ecoRepo.recordTransaction(overflowTx)
            fail("Coin transaction causing integer overflow must fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("overflow") == true)
        }
    }
}

// ==========================================
// In-Memory Fake DAOs for Unit Testing
// ==========================================

private class FakeTaskDao : TaskDao {
    private val state = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun getTasksForDate(date: String): Flow<List<TaskEntity>> =
        state.map { list -> list.filter { it.scheduledDate == date } }

    override suspend fun getTaskById(id: String): TaskEntity? =
        state.value.find { it.id == id }

    override suspend fun getOverduePendingTasks(currentDate: String): List<TaskEntity> =
        state.value.filter { it.scheduledDate < currentDate && it.state == "PENDING" }

    override suspend fun getActiveInProgressTask(): TaskEntity? =
        state.value.find { it.state == "IN_PROGRESS" }

    override suspend fun getLatestInterruptedTask(): TaskEntity? =
        state.value.find { it.state == "INTERRUPTED" }

    override suspend fun insertTask(task: TaskEntity) {
        state.value = state.value.filter { it.id != task.id } + task
    }

    override suspend fun insertTasks(tasks: List<TaskEntity>) {
        val ids = tasks.map { it.id }.toSet()
        state.value = state.value.filterNot { it.id in ids } + tasks
    }

    override suspend fun updateTask(task: TaskEntity) {
        insertTask(task)
    }

    override suspend fun deleteTask(task: TaskEntity) {
        state.value = state.value.filter { it.id != task.id }
    }
}

private class FakeCoinLedgerDao : CoinLedgerDao {
    private val transactions = MutableStateFlow<List<CoinLedgerEntity>>(emptyList())

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
}

private class FakeDayTypeDao : DayTypeDao {
    private val dayTypes = MutableStateFlow<List<DayTypeEntity>>(emptyList())

    override suspend fun getDayTypeById(id: String): DayTypeEntity? =
        dayTypes.value.find { it.id == id }

    override fun getAllDayTypes(): Flow<List<DayTypeEntity>> = dayTypes

    override suspend fun insertDayType(dayType: DayTypeEntity) {
        dayTypes.value = dayTypes.value.filter { it.id != dayType.id } + dayType
    }

    override suspend fun insertDayTypes(dayTypes: List<DayTypeEntity>) {
        val ids = dayTypes.map { it.id }.toSet()
        this.dayTypes.value = this.dayTypes.value.filterNot { it.id in ids } + dayTypes
    }
}

private class FakeDayOverrideDao : DayOverrideDao {
    private val overrides = MutableStateFlow<Map<String, DayOverrideEntity>>(emptyMap())

    override suspend fun getOverrideForDate(date: String): DayOverrideEntity? =
        overrides.value[date]

    override fun observeOverrideForDate(date: String): Flow<DayOverrideEntity?> =
        overrides.map { it[date] }

    override suspend fun insertOrUpdateOverride(override: DayOverrideEntity) {
        overrides.value = overrides.value + (override.date to override)
    }

    override suspend fun deleteOverrideForDate(date: String) {
        overrides.value = overrides.value - date
    }
}

private class FakeHabitDao : HabitDao {
    private val habits = MutableStateFlow<List<HabitEntity>>(emptyList())
    private val logs = MutableStateFlow<List<HabitLogEntity>>(emptyList())

    override fun getAllHabits(): Flow<List<HabitEntity>> = habits

    override suspend fun getHabitById(id: String): HabitEntity? =
        habits.value.find { it.id == id }

    override suspend fun getLogForDate(habitId: String, logDate: String): HabitLogEntity? =
        logs.value.find { it.habitId == habitId && it.logDate == logDate }

    override fun getLogsForHabit(habitId: String): Flow<List<HabitLogEntity>> =
        logs.map { list -> list.filter { it.habitId == habitId } }

    override suspend fun insertHabit(habit: HabitEntity) {
        habits.value = habits.value.filter { it.id != habit.id } + habit
    }

    override suspend fun updateHabit(habit: HabitEntity) {
        insertHabit(habit)
    }

    override suspend fun insertLog(log: HabitLogEntity) {
        logs.value = logs.value.filterNot { it.habitId == log.habitId && it.logDate == log.logDate } + log
    }
}

private class FakeStudySessionDao : StudySessionDao {
    private val sessions = MutableStateFlow<List<StudySessionEntity>>(emptyList())

    override fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>> =
        sessions.map { list -> list.filter { it.sessionDate == date } }

    override fun getRecentSessions(limit: Int): Flow<List<StudySessionEntity>> =
        sessions.map { it.takeLast(limit) }

    override fun getAllSessions(): Flow<List<StudySessionEntity>> = sessions

    override suspend fun insertSession(session: StudySessionEntity) {
        sessions.value = sessions.value.filter { it.id != session.id } + session
    }

    override suspend fun getTotalStudyMinutesForDate(date: String): Int? =
        sessions.value.filter { it.sessionDate == date }.sumOf { it.durationMinutes }.takeIf { it > 0 }
}

private class FakeRoutineDao : RoutineDao {
    private val routines = MutableStateFlow<List<RoutineEntity>>(emptyList())
    private val steps = MutableStateFlow<List<RoutineStepEntity>>(emptyList())

    override fun getRoutinesForDayType(dayTypeId: String): Flow<List<RoutineEntity>> =
        routines.map { list -> list.filter { it.dayTypeId == dayTypeId } }

    override fun getStepsForRoutine(routineId: String): Flow<List<RoutineStepEntity>> =
        steps.map { list -> list.filter { it.routineId == routineId }.sortedBy { it.orderIndex } }

    override suspend fun upsertRoutine(routine: RoutineEntity) {
        routines.value = routines.value.filter { it.id != routine.id } + routine
    }

    override suspend fun upsertSteps(steps: List<RoutineStepEntity>) {
        val ids = steps.map { it.id }.toSet()
        this.steps.value = this.steps.value.filterNot { it.id in ids } + steps
    }

    override suspend fun deleteStepsForRoutine(routineId: String) {
        steps.value = steps.value.filterNot { it.routineId == routineId }
    }

    override suspend fun deleteRoutine(routineId: String) {
        routines.value = routines.value.filterNot { it.id == routineId }
        deleteStepsForRoutine(routineId)
    }
}

private class FakeCompanionProfileDao : CompanionProfileDao {
    private val profile = MutableStateFlow<CompanionProfileEntity?>(null)

    override suspend fun getProfile(): CompanionProfileEntity? = profile.value

    override fun observeProfile(): Flow<CompanionProfileEntity?> = profile

    override suspend fun upsertProfile(profile: CompanionProfileEntity) {
        this.profile.value = profile
    }
}

private class FakeShopItemDao : ShopItemDao {
    private val items = MutableStateFlow<List<ShopItemEntity>>(emptyList())

    override fun getAllShopItems(): Flow<List<ShopItemEntity>> = items

    override suspend fun getShopItemById(id: String): ShopItemEntity? =
        items.value.find { it.id == id }

    override suspend fun insertShopItems(items: List<ShopItemEntity>) {
        val ids = items.map { it.id }.toSet()
        this.items.value = this.items.value.filterNot { it.id in ids } + items
    }

    override suspend fun updateShopItem(item: ShopItemEntity) {
        items.value = items.value.filter { it.id != item.id } + item
    }
}

private class FakeUnfiledInboxDao : UnfiledInboxDao {
    private val items = MutableStateFlow<List<UnfiledCaptureInboxEntity>>(emptyList())

    override fun getAllInboxItems(): Flow<List<UnfiledCaptureInboxEntity>> = items

    override suspend fun getInboxItemById(id: String): UnfiledCaptureInboxEntity? =
        items.value.find { it.id == id }

    override suspend fun insertInboxItem(item: UnfiledCaptureInboxEntity) {
        items.value = items.value.filter { it.id != item.id } + item
    }

    override suspend fun deleteInboxItem(item: UnfiledCaptureInboxEntity) {
        items.value = items.value.filter { it.id != item.id }
    }
}

private class FakeInterventionRuleDao : InterventionRuleDao {
    private val rules = MutableStateFlow<List<InterventionRuleEntity>>(emptyList())

    override fun getBlockedAppRules(): Flow<List<InterventionRuleEntity>> =
        rules.map { list -> list.filter { it.isStrictBlocked } }

    override suspend fun getRuleForPackage(packageName: String): InterventionRuleEntity? =
        rules.value.find { it.packageName == packageName }

    override suspend fun insertOrUpdateRule(rule: InterventionRuleEntity) {
        rules.value = rules.value.filter { it.packageName != rule.packageName } + rule
    }

    override suspend fun deleteRule(packageName: String) {
        rules.value = rules.value.filter { it.packageName != packageName }
    }
}
