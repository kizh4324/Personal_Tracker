package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.InterventionRuleEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.InterventionRuleRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

class DomainUseCasesTest {

    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var streakMachine: StreakStateMachine
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var ruleEngine: InterventionRuleEngine
    private lateinit var parser: DeterministicNlpCommandParser

    private lateinit var fakeDayTypeRepo: FakeDayTypeRepository
    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeHabitRepo: FakeHabitRepository
    private lateinit var fakeEconomyRepo: FakeEconomyRepository
    private lateinit var fakeInboxRepo: FakeUnfiledInboxRepository
    private lateinit var fakeRuleRepo: FakeInterventionRuleRepository

    private val testDate = "2026-10-16" // Friday (day 5)

    @Before
    fun setUp() {
        adaptiveEngine = AdaptiveScheduleEngine()
        streakMachine = StreakStateMachine()
        ledgerEngine = GamificationLedgerEngine()
        ruleEngine = InterventionRuleEngine()
        parser = DeterministicNlpCommandParser()

        fakeDayTypeRepo = FakeDayTypeRepository()
        fakeTaskRepo = FakeTaskRepository()
        fakeHabitRepo = FakeHabitRepository()
        fakeEconomyRepo = FakeEconomyRepository()
        fakeInboxRepo = FakeUnfiledInboxRepository()
        fakeRuleRepo = FakeInterventionRuleRepository()
    }

    // ==========================================
    // 1. ResolveDayTypeUseCase Tests
    // ==========================================

    @Test
    fun testResolveDayTypeUseCaseResolvesOverrideOverDefault() = runTest {
        val weekday = DayType("dt_week", "Weekday", "#3B82F6", 120, false, "1,2,3,4,5")
        val holiday = DayType("dt_holiday", "Holiday", "#10B981", 0, false, "")
        fakeDayTypeRepo.saveDayTypes(listOf(weekday, holiday))

        val useCase = ResolveDayTypeUseCase(fakeDayTypeRepo, adaptiveEngine)

        // Without override -> Weekday
        val defaultResolved = useCase(testDate)
        assertEquals("dt_week", defaultResolved.id)

        // With override -> Holiday
        fakeDayTypeRepo.setOverrideForDate(DayOverride(testDate, "dt_holiday", true, System.currentTimeMillis()))
        val overrideResolved = useCase(testDate)
        assertEquals("dt_holiday", overrideResolved.id)
    }

    @Test
    fun testResolveDayTypeUseCaseEmptyListReturnsSafeDefault() = runTest {
        val emptyRepo = FakeDayTypeRepository()
        val useCase = ResolveDayTypeUseCase(emptyRepo, adaptiveEngine)

        val resolved = useCase(testDate)
        assertEquals(ResolveDayTypeUseCase.SAFE_DEFAULT_DAY_TYPE.id, resolved.id)
        assertEquals("Default Routine", resolved.name)
    }

    // ==========================================
    // 2. CompleteTaskUseCase Tests
    // ==========================================

    @Test
    fun testCompleteTaskUseCaseRewardsCoinsAndAdvancesCompanion() = runTest {
        val task = Task(
            id = "t_exam",
            title = "Prepare exam",
            scheduledDate = testDate,
            deliveryIntensity = "URGENT"
        )
        fakeTaskRepo.saveTask(task)

        val companion = CompanionProfile(
            id = 1,
            speciesName = "Fox",
            level = 1,
            currentEnergyUnits = 95
        )
        fakeEconomyRepo.updateCompanionProfile(companion)

        val useCase = CompleteTaskUseCase(fakeTaskRepo, fakeEconomyRepo, ledgerEngine)
        val result = useCase("t_exam")

        // 10 coins for URGENT task
        assertEquals(10, result.coinReward)
        assertEquals("TASK_COMP_t_exam", result.idempotencyKey)

        // Task marked completed
        val completedTask = fakeTaskRepo.getTaskById("t_exam")
        assertEquals("COMPLETED", completedTask?.state)

        // Companion leveled up to 2 (95 + 10 = 105 energy -> Level 2, 5 energy)
        assertNotNull(result.updatedCompanionProfile)
        assertEquals(2, result.updatedCompanionProfile?.level)
        assertEquals(5, result.updatedCompanionProfile?.currentEnergyUnits)
        assertEquals(CompanionMood.EXCITED.ordinalValue, result.updatedCompanionProfile?.currentExpressionOrdinal)
    }

    @Test
    fun testCompleteTaskUseCaseThrowsWhenTaskMissing() = runTest {
        val useCase = CompleteTaskUseCase(fakeTaskRepo, fakeEconomyRepo, ledgerEngine)
        try {
            useCase("non_existent_task")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("does not exist") == true)
        }
    }

    @Test
    fun testCompleteTaskUseCaseAlreadyCompletedTaskReturnsEarlyWithoutDoubleRewards() = runTest {
        val completedTask = Task(
            id = "t_already_done",
            title = "Already finished",
            scheduledDate = testDate,
            deliveryIntensity = "IMPORTANT",
            state = "COMPLETED"
        )
        fakeTaskRepo.saveTask(completedTask)

        val useCase = CompleteTaskUseCase(fakeTaskRepo, fakeEconomyRepo, ledgerEngine)
        val result = useCase("t_already_done")

        // Zero coins awarded, deterministic compound idempotency key preserved
        assertEquals(0, result.coinReward)
        assertEquals("TASK_COMP_t_already_done", result.idempotencyKey)
    }

    @Test
    fun testCompleteTaskUseCaseRejectsInvalidInputs() = runTest {
        val useCase = CompleteTaskUseCase(fakeTaskRepo, fakeEconomyRepo, ledgerEngine)
        try {
            useCase("  ")
            fail("Expected IllegalArgumentException for blank taskId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("blank") == true)
        }

        try {
            useCase("t1", completionTimestamp = -1)
            fail("Expected IllegalArgumentException for non-positive timestamp")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") == true)
        }
    }

    @Test
    fun testLogHabitUseCaseRejectsInvalidLogDate() = runTest {
        val habit = Habit("h_date", "Habit", targetDaysOfWeek = "1,2,3,4,5,6,7")
        fakeHabitRepo.saveHabit(habit)
        val useCase = LogHabitUseCase(fakeHabitRepo, fakeEconomyRepo, streakMachine, ledgerEngine)

        try {
            useCase("h_date", "invalid-date")
            fail("Expected IllegalArgumentException for invalid ISO-8601 date")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("ISO-8601") == true)
        }
    }

    // ==========================================
    // 3. LogHabitUseCase Tests
    // ==========================================

    @Test
    fun testLogHabitUseCaseCompletionAdvancesStreakAndRewardsCoins() = runTest {
        val habit = Habit(
            id = "h_run",
            name = "Morning Run",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 6,
            bestStreak = 6,
            slackBankDays = 0
        )
        fakeHabitRepo.saveHabit(habit)
        fakeEconomyRepo.updateCompanionProfile(CompanionProfile(id = 1, speciesName = "Fox", level = 1, currentEnergyUnits = 0))

        val useCase = LogHabitUseCase(fakeHabitRepo, fakeEconomyRepo, streakMachine, ledgerEngine)
        val result = useCase("h_run", testDate, isCompleted = true)

        assertEquals(7, result.updatedHabit.currentStreak)
        assertEquals(1, result.updatedHabit.slackBankDays) // 7 days milestone awards slack bank
        assertEquals("COMPLETED", result.resultingStatus)
        assertEquals(5, result.coinsEarned)
        assertEquals("HABIT_LOG_h_run_$testDate", result.idempotencyKey)

        // Verify changes were actually persisted to repository
        val persistedHabit = fakeHabitRepo.getHabitById("h_run")
        assertNotNull(persistedHabit)
        assertEquals(7, persistedHabit?.currentStreak)
        assertEquals(1, persistedHabit?.slackBankDays)

        val persistedLog = fakeHabitRepo.getLogForDate("h_run", testDate)
        assertNotNull(persistedLog)
        assertEquals("COMPLETED", persistedLog?.status)

        // At 7 days milestone, companion mood is EXCITED
        assertEquals(CompanionMood.EXCITED.ordinalValue, result.updatedCompanionProfile?.currentExpressionOrdinal)
    }

    @Test
    fun testLogHabitUseCaseDuplicateLogForDateReturnsEarlyWithoutDoubleIncrement() = runTest {
        val habit = Habit(
            id = "h_run_dup",
            name = "Morning Run",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 3,
            bestStreak = 3
        )
        fakeHabitRepo.saveHabit(habit)
        val initialLog = HabitLog(
            id = "log_first",
            habitId = "h_run_dup",
            logDate = testDate,
            status = "COMPLETED"
        )
        fakeHabitRepo.logHabit(initialLog)

        val useCase = LogHabitUseCase(fakeHabitRepo, fakeEconomyRepo, streakMachine, ledgerEngine)
        val result = useCase("h_run_dup", testDate, isCompleted = true)

        // Streak unchanged at 3, 0 coins earned, null idempotencyKey
        assertEquals(3, result.updatedHabit.currentStreak)
        assertEquals(0, result.coinsEarned)
        assertNull(result.idempotencyKey)
        assertEquals("COMPLETED", result.resultingStatus)
    }

    @Test
    fun testLogHabitUseCaseMissedDayUsesSlackBankWithoutNegativePetConsequences() = runTest {
        val habit = Habit(
            id = "h_read",
            name = "Read",
            targetDaysOfWeek = "1,2,3,4,5,6,7",
            currentStreak = 10,
            bestStreak = 10,
            slackBankDays = 1
        )
        fakeHabitRepo.saveHabit(habit)
        fakeEconomyRepo.updateCompanionProfile(CompanionProfile(id = 1, speciesName = "Fox", level = 1, currentEnergyUnits = 50))

        val useCase = LogHabitUseCase(fakeHabitRepo, fakeEconomyRepo, streakMachine, ledgerEngine)
        val result = useCase("h_read", testDate, isCompleted = false)

        // Streak is preserved, slack decremented to 0
        assertEquals(10, result.updatedHabit.currentStreak)
        assertEquals(0, result.updatedHabit.slackBankDays)
        assertEquals("SLACK_USED", result.resultingStatus)
        assertEquals(0, result.coinsEarned)
        assertNull(result.idempotencyKey)

        // Companion enters ENCOURAGING mood, 0 HP loss
        assertEquals(CompanionMood.ENCOURAGING.ordinalValue, result.updatedCompanionProfile?.currentExpressionOrdinal)
        assertEquals(50, result.updatedCompanionProfile?.currentEnergyUnits)
    }

    // ==========================================
    // 4. CaptureQuickThoughtUseCase Tests
    // ==========================================

    @Test
    fun testCaptureQuickThoughtHighConfidenceAutoCommitsTask() = runTest {
        val useCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo)
        val input = "Study algorithms for 45 minutes at 4pm tomorrow urgent"

        val result = useCase(input, referenceDate = LocalDate.parse(testDate))

        assertTrue(result is QuickCaptureResult.HighConfidence)
        val high = result as QuickCaptureResult.HighConfidence
        assertEquals("Study algorithms", high.committedTask.title)
        assertEquals("URGENT", high.committedTask.deliveryIntensity)
        assertEquals("2026-10-17", high.committedTask.scheduledDate)

        // Verified task committed to repository
        val inDb = fakeTaskRepo.getTaskById(high.committedTask.id)
        assertNotNull(inDb)
        assertEquals("Study algorithms", inDb?.title)
    }

    @Test
    fun testCaptureQuickThoughtHighConfidenceNonTaskReturnsMediumConfidence() = runTest {
        val useCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo)
        // High confidence command with HABIT intent
        val input = "Log habit drink water for 15 minutes at 8am tomorrow urgent"

        val result = useCase(input, referenceDate = LocalDate.parse(testDate))

        // Non-task high confidence should NOT auto-commit a task; routes to MediumConfidence
        assertTrue(result is QuickCaptureResult.MediumConfidence)
        assertTrue(fakeTaskRepo.allTasks.isEmpty())
    }

    @Test
    fun testCaptureQuickThoughtMediumConfidenceReturnsDraftWithoutCommit() = runTest {
        val useCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo)
        val input = "Read book tomorrow"

        val result = useCase(input, referenceDate = LocalDate.parse(testDate))

        assertTrue(result is QuickCaptureResult.MediumConfidence)
        val med = result as QuickCaptureResult.MediumConfidence
        assertEquals("Read book", med.draftCommand.title)

        // Database must NOT be modified
        assertTrue(fakeTaskRepo.allTasks.isEmpty())
        assertTrue(fakeInboxRepo.allInboxItems.isEmpty())
    }

    @Test
    fun testCaptureQuickThoughtLowConfidenceRoutesToUnfiledInbox() = runTest {
        val useCase = CaptureQuickThoughtUseCase(parser, fakeTaskRepo, fakeInboxRepo)
        val input = "mumble gibberish random noises"

        val result = useCase(input, referenceDate = LocalDate.parse(testDate))

        assertTrue(result is QuickCaptureResult.LowConfidence)
        val low = result as QuickCaptureResult.LowConfidence
        assertEquals(input, low.inboxItem.rawTranscriptText)

        // Stored in inbox repository
        val stored = fakeInboxRepo.getInboxItemById(low.inboxItem.id)
        assertNotNull(stored)
        assertEquals(input, stored?.rawTranscriptText)
    }

    // ==========================================
    // 5. EvaluateInterventionUseCase Tests
    // ==========================================

    @Test
    fun testEvaluateInterventionUseCase() = runTest {
        fakeRuleRepo.saveRule(
            InterventionRule(
                packageName = "com.distraction.app",
                appDisplayName = "Distraction",
                isStrictBlocked = true
            )
        )

        val useCase = EvaluateInterventionUseCase(fakeRuleRepo, ruleEngine)

        // First attempt -> block & prompt JITAI
        val decision = useCase("com.distraction.app", lastPromptTimestamp = null)
        assertTrue(decision.shouldBlock)
        assertTrue(decision.shouldPromptJitai)

        // Second attempt within 2 min -> prompt suppressed
        val suppressed = useCase(
            packageName = "com.distraction.app",
            lastPromptTimestamp = 1000L,
            currentTimestamp = 1000L + (2 * 60 * 1000L)
        )
        assertTrue(suppressed.shouldBlock)
        assertFalse(suppressed.shouldPromptJitai)
    }
}

// ==========================================
// In-Memory Fakes for Domain Unit Tests
// ==========================================

private class FakeDayTypeRepository : DayTypeRepository {
    private val dayTypes = MutableStateFlow<List<DayType>>(emptyList())
    private val overrides = mutableMapOf<String, DayOverride>()

    override fun getAllDayTypes(): Flow<List<DayType>> = dayTypes
    override suspend fun getDayTypeById(id: String): DayType? = dayTypes.value.find { it.id == id }
    override suspend fun saveDayType(dayType: DayType) { dayTypes.value = dayTypes.value.filter { it.id != dayType.id } + dayType }
    override suspend fun saveDayTypes(dayTypes: List<DayType>) { this.dayTypes.value = dayTypes }
    override suspend fun getOverrideForDate(date: String): DayOverride? = overrides[date]
    override fun observeOverrideForDate(date: String): Flow<DayOverride?> = MutableStateFlow(overrides[date])
    override suspend fun setOverrideForDate(override: DayOverride) { overrides[override.date] = override }
    override suspend fun deleteOverrideForDate(date: String) { overrides.remove(date) }
}

private class FakeTaskRepository : TaskRepository {
    val allTasks = mutableListOf<Task>()

    override fun getTasksForDate(date: String): Flow<List<Task>> =
        MutableStateFlow(allTasks.filter { it.scheduledDate == date })

    override suspend fun getTaskById(id: String): Task? = allTasks.find { it.id == id }
    override suspend fun getOverduePendingTasks(currentDate: String): List<Task> = emptyList()
    override suspend fun getActiveInProgressTask(): Task? = allTasks.find { it.state == "IN_PROGRESS" }
    override suspend fun getLatestInterruptedTask(): Task? = allTasks.find { it.state == "INTERRUPTED" }
    override suspend fun saveTask(task: Task) { allTasks.removeAll { it.id == task.id }; allTasks.add(task) }
    override suspend fun saveTasks(tasks: List<Task>) { tasks.forEach { saveTask(it) } }
    override suspend fun updateTask(task: Task) { saveTask(task) }
    override suspend fun deleteTask(task: Task) { allTasks.removeAll { it.id == task.id } }
    override suspend fun completeTaskWithReward(
        taskId: String,
        completionTimestamp: Long,
        coinReward: Int,
        idempotencyKey: String
    ) {
        val task = allTasks.find { it.id == taskId } ?: error("Task not found")
        val completed = task.copy(state = "COMPLETED", completionTimestamp = completionTimestamp)
        saveTask(completed)
    }
}

private class FakeHabitRepository : HabitRepository {
    val habits = mutableListOf<Habit>()
    val logs = mutableListOf<HabitLog>()

    override fun getAllHabits(): Flow<List<Habit>> = MutableStateFlow(habits)
    override suspend fun getHabitById(id: String): Habit? = habits.find { it.id == id }
    override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> =
        MutableStateFlow(logs.filter { it.habitId == habitId })

    override suspend fun getLogForDate(habitId: String, logDate: String): HabitLog? =
        logs.find { it.habitId == habitId && it.logDate == logDate }

    override suspend fun saveHabit(habit: Habit) { habits.removeAll { it.id == habit.id }; habits.add(habit) }
    override suspend fun updateHabit(habit: Habit) { saveHabit(habit) }
    override suspend fun logHabit(log: HabitLog) { logs.removeAll { it.habitId == log.habitId && it.logDate == log.logDate }; logs.add(log) }
    override suspend fun logHabitWithReward(log: HabitLog, coinReward: Int, idempotencyKey: String) { logHabit(log) }
}

private class FakeEconomyRepository : EconomyRepository {
    val transactions = mutableListOf<CoinTransaction>()
    private var profile: CompanionProfile? = null
    val shopItems = mutableListOf<ShopItem>()

    override fun getAllTransactions(): Flow<List<CoinTransaction>> = MutableStateFlow(transactions)
    override suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction? =
        transactions.find { it.idempotencyKey == key }

    override suspend fun getLatestBalance(): Int = transactions.sumOf { it.deltaAmount }
    override fun observeLatestBalance(): Flow<Int> = MutableStateFlow(transactions.sumOf { it.deltaAmount })
    override suspend fun recordTransaction(transaction: CoinTransaction) { transactions.add(transaction) }
    override suspend fun purchaseShopItem(shopItemId: String, idempotencyKey: String) {}
    override suspend fun getCompanionProfile(): CompanionProfile? = profile
    override fun observeCompanionProfile(): Flow<CompanionProfile?> = MutableStateFlow(profile)
    override suspend fun updateCompanionProfile(profile: CompanionProfile) { this.profile = profile }
    override fun getAllShopItems(): Flow<List<ShopItem>> = MutableStateFlow(shopItems)
    override suspend fun getShopItemById(id: String): ShopItem? = shopItems.find { it.id == id }
    override suspend fun saveShopItems(items: List<ShopItem>) { shopItems.addAll(items) }
    override suspend fun updateShopItem(item: ShopItem) {}
}

private class FakeUnfiledInboxRepository : UnfiledInboxRepository {
    val allInboxItems = mutableListOf<UnfiledInboxItem>()

    override fun getAllInboxItems(): Flow<List<UnfiledInboxItem>> = MutableStateFlow(allInboxItems)
    override suspend fun getInboxItemById(id: String): UnfiledInboxItem? = allInboxItems.find { it.id == id }
    override suspend fun saveInboxItem(item: UnfiledInboxItem) { allInboxItems.removeAll { it.id == item.id }; allInboxItems.add(item) }
    override suspend fun deleteInboxItem(item: UnfiledInboxItem) { allInboxItems.removeAll { it.id == item.id } }
}

private class FakeInterventionRuleRepository : InterventionRuleRepository {
    private val rules = mutableListOf<InterventionRule>()

    override fun getBlockedAppRules(): Flow<List<InterventionRule>> = MutableStateFlow(rules)
    override suspend fun getRuleForPackage(packageName: String): InterventionRule? = rules.find { it.packageName == packageName }
    override suspend fun saveRule(rule: InterventionRule) { rules.removeAll { it.packageName == rule.packageName }; rules.add(rule) }
    override suspend fun deleteRule(packageName: String) { rules.removeAll { it.packageName == packageName } }
}
