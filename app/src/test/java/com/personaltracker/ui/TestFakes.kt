package com.personaltracker.ui

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.AwardCoinsResult
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.InterventionRuleRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestTaskRepository(var economyRepository: EconomyRepository? = null) : TaskRepository {
    val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    val overdueTasks = mutableListOf<Task>()
    var interruptedTask: Task? = null

    override fun getTasksForDate(date: String): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.scheduledDate == date } }

    override suspend fun getTaskById(id: String): Task? =
        tasksFlow.value.find { it.id == id }

    override suspend fun getOverduePendingTasks(currentDate: String): List<Task> = overdueTasks.toList()

    override suspend fun getActiveInProgressTask(): Task? =
        tasksFlow.value.find { it.state == "IN_PROGRESS" }

    override suspend fun getLatestInterruptedTask(): Task? = interruptedTask

    private fun syncOverdueTask(task: Task) {
        val today = java.time.LocalDate.now().toString()
        if (task.state != "PENDING" || task.scheduledDate >= today) {
            overdueTasks.removeAll { it.id == task.id }
        } else {
            overdueTasks.removeAll { it.id == task.id }
            overdueTasks.add(task)
        }
    }

    override suspend fun saveTask(task: Task) {
        syncOverdueTask(task)
        tasksFlow.value = tasksFlow.value.filter { it.id != task.id } + task
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        tasks.forEach { syncOverdueTask(it) }
        val existingIds = tasks.map { it.id }.toSet()
        tasksFlow.value = tasksFlow.value.filter { it.id !in existingIds } + tasks
    }

    override suspend fun updateTask(task: Task) {
        saveTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        tasksFlow.value = tasksFlow.value.filter { it.id != task.id }
    }

    override suspend fun completeTaskWithReward(
        taskId: String,
        completionTimestamp: Long,
        coinReward: Int,
        idempotencyKey: String
    ) {
        val task = getTaskById(taskId) ?: return
        val updated = task.copy(state = "COMPLETED", completionTimestamp = completionTimestamp)
        saveTask(updated)
        economyRepository?.recordTransaction(
            CoinTransaction(
                id = java.util.UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = coinReward,
                eventType = EconomyEventTypes.TASK_COMPLETION,
                sourceEntityId = taskId,
                timestamp = completionTimestamp
            )
        )
    }
}

class TestHabitRepository(var economyRepository: EconomyRepository? = null) : HabitRepository {
    val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    val logs = mutableListOf<HabitLog>()

    override fun getAllHabits(): Flow<List<Habit>> = habitsFlow

    override suspend fun getHabitById(id: String): Habit? =
        habitsFlow.value.find { it.id == id }

    override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> =
        MutableStateFlow(logs.filter { it.habitId == habitId })

    override suspend fun getLogForDate(habitId: String, logDate: String): HabitLog? =
        logs.find { it.habitId == habitId && it.logDate == logDate }

    override suspend fun saveHabit(habit: Habit) {
        habitsFlow.value = habitsFlow.value.filter { it.id != habit.id } + habit
    }

    override suspend fun updateHabit(habit: Habit) {
        saveHabit(habit)
    }

    override suspend fun logHabit(log: HabitLog) {
        logs.removeAll { it.habitId == log.habitId && it.logDate == log.logDate }
        logs.add(log)
    }

    override suspend fun logHabitWithReward(log: HabitLog, coinReward: Int, idempotencyKey: String) {
        logHabit(log)
        economyRepository?.recordTransaction(
            CoinTransaction(
                id = java.util.UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = coinReward,
                eventType = EconomyEventTypes.HABIT_COMPLETION,
                sourceEntityId = log.habitId,
                timestamp = log.loggedTimestamp
            )
        )
    }

    override suspend fun purchaseStreakShield(
        habitId: String,
        priceCoins: Int,
        idempotencyKey: String
    ): Habit {
        val habit = getHabitById(habitId) ?: throw IllegalArgumentException("Habit with id '$habitId' not found.")
        check(habit.slackBankDays < com.personaltracker.domain.engine.StreakStateMachine.MAX_SLACK_BANK_DAYS) {
            "Streak shield bank is already at maximum capacity"
        }
        val currentBalance = economyRepository?.getLatestBalance() ?: 100
        check(currentBalance >= priceCoins) {
            "Insufficient coin balance"
        }
        economyRepository?.recordTransaction(
            CoinTransaction(
                id = java.util.UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = -priceCoins,
                eventType = EconomyEventTypes.SHOP_PURCHASE,
                sourceEntityId = habitId,
                timestamp = System.currentTimeMillis()
            )
        )
        val updated = habit.copy(slackBankDays = habit.slackBankDays + 1)
        updateHabit(updated)
        return updated
    }
}

class TestRoutineRepository : RoutineRepository {
    val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    val stepsFlow = MutableStateFlow<List<RoutineStep>>(emptyList())

    override fun getRoutinesForDayType(dayTypeId: String): Flow<List<Routine>> =
        routinesFlow.map { list -> list.filter { it.dayTypeId == dayTypeId } }

    override fun getStepsForRoutine(routineId: String): Flow<List<RoutineStep>> =
        stepsFlow.map { list -> list.filter { it.routineId == routineId } }

    override suspend fun getRoutineById(routineId: String): Routine? {
        val routine = routinesFlow.value.find { it.id == routineId } ?: return null
        val steps = stepsFlow.value.filter { it.routineId == routineId }.sortedBy { it.orderIndex }
        return routine.copy(steps = if (steps.isNotEmpty()) steps else routine.steps)
    }

    override fun getAllRoutines(): Flow<List<Routine>> = routinesFlow

    override suspend fun getStepsForRoutineList(routineId: String): List<RoutineStep> =
        stepsFlow.value.filter { it.routineId == routineId }.sortedBy { it.orderIndex }

    override suspend fun ensureDefaultRoutines(): List<Routine> {
        if (routinesFlow.value.isEmpty()) {
            routinesFlow.value = Routine.BUILT_IN_ROUTINES
            stepsFlow.value = Routine.BUILT_IN_ROUTINES.flatMap { it.steps }
        }
        return routinesFlow.value
    }

    override suspend fun saveRoutine(routine: Routine) {
        routinesFlow.value = routinesFlow.value.filter { it.id != routine.id } + routine
        if (routine.steps.isNotEmpty()) {
            saveSteps(routine.steps)
        }
    }

    override suspend fun saveSteps(steps: List<RoutineStep>) {
        val ids = steps.map { it.id }.toSet()
        stepsFlow.value = stepsFlow.value.filter { it.id !in ids } + steps
    }

    override suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        saveRoutine(routine.copy(steps = steps))
        saveSteps(steps)
    }

    override suspend fun deleteRoutine(routineId: String) {
        routinesFlow.value = routinesFlow.value.filter { it.id != routineId }
        stepsFlow.value = stepsFlow.value.filter { it.routineId != routineId }
    }
}

class TestEconomyRepository : EconomyRepository {
    val transactionsFlow = MutableStateFlow<List<CoinTransaction>>(emptyList())
    val profileFlow = MutableStateFlow<CompanionProfile?>(null)
    val shopItemsFlow = MutableStateFlow<List<ShopItem>>(emptyList())
    val balanceFlow = MutableStateFlow(0)

    override fun getAllTransactions(): Flow<List<CoinTransaction>> = transactionsFlow

    override suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction? =
        transactionsFlow.value.find { it.idempotencyKey == key }

    override suspend fun getLatestBalance(): Int = balanceFlow.value

    override fun observeLatestBalance(): Flow<Int> = balanceFlow

    override suspend fun getDailyEarnedTotal(startOfDay: Long): Int =
        transactionsFlow.value.filter { it.timestamp >= startOfDay && it.deltaAmount > 0 }.sumOf { it.deltaAmount }

    override fun observeDailyEarnedTotal(startOfDay: Long): Flow<Int> =
        transactionsFlow.map { list -> list.filter { it.timestamp >= startOfDay && it.deltaAmount > 0 }.sumOf { it.deltaAmount } }

    override suspend fun getDailyEarnedForCategory(startOfDay: Long, eventType: String): Int =
        transactionsFlow.value.filter { it.timestamp >= startOfDay && it.eventType == eventType && it.deltaAmount > 0 }.sumOf { it.deltaAmount }

    override fun observeDailyEarnedForCategory(startOfDay: Long, eventType: String): Flow<Int> =
        transactionsFlow.map { list -> list.filter { it.timestamp >= startOfDay && it.eventType == eventType && it.deltaAmount > 0 }.sumOf { it.deltaAmount } }

    override fun observeTransactionsSince(timestamp: Long): Flow<List<CoinTransaction>> =
        transactionsFlow.map { list -> list.filter { it.timestamp >= timestamp }.sortedByDescending { it.timestamp } }

    override suspend fun recordReversal(inboxId: String, amount: Int, idempotencyKey: String): Result<CoinTransaction> {
        val existing = transactionsFlow.value.find { it.idempotencyKey == idempotencyKey }
        if (existing != null) return Result.success(existing)
        val deduction = minOf(amount.coerceAtLeast(0), balanceFlow.value)
        val newBalance = balanceFlow.value - deduction
        balanceFlow.value = newBalance
        val tx = CoinTransaction(
            id = java.util.UUID.randomUUID().toString(),
            idempotencyKey = idempotencyKey,
            deltaAmount = -deduction,
            eventType = EconomyEventTypes.CAPTURE_REVERSAL,
            sourceEntityId = inboxId,
            balanceAfter = newBalance
        )
        transactionsFlow.value = transactionsFlow.value + tx
        return Result.success(tx)
    }

    override suspend fun awardCoins(
        eventType: String,
        sourceEntityId: String,
        requestedAmount: Int,
        idempotencyKey: String,
        startOfDayTimestamp: Long
    ): AwardCoinsResult {
        val existing = transactionsFlow.value.find { it.idempotencyKey == idempotencyKey }
        if (existing != null) {
            return AwardCoinsResult(
                coinsAwarded = 0,
                requestedCoins = requestedAmount,
                isCapReached = false,
                isDuplicate = true,
                transaction = existing
            )
        }
        val isFarmable = eventType in GamificationLedgerEngine.FARMABLE_EVENT_TYPES
        val categoryCap = when (eventType) {
            EconomyEventTypes.TASK_COMPLETION -> GamificationLedgerEngine.DAILY_TASK_CAP
            EconomyEventTypes.HABIT_COMPLETION -> GamificationLedgerEngine.DAILY_HABIT_CAP
            EconomyEventTypes.STUDY_SESSION -> GamificationLedgerEngine.DAILY_FOCUS_CAP
            else -> Int.MAX_VALUE
        }
        val categoryEarned = getDailyEarnedForCategory(startOfDayTimestamp, eventType)
        val globalEarned = if (isFarmable) {
            getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.TASK_COMPLETION) +
                getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.HABIT_COMPLETION) +
                getDailyEarnedForCategory(startOfDayTimestamp, EconomyEventTypes.ROUTINE_COMPLETION)
        } else 0
        val awarded = GamificationLedgerEngine.calculateCappedReward(
            requestedAmount = requestedAmount,
            categoryEarned = categoryEarned,
            categoryCap = categoryCap,
            globalFarmableEarned = globalEarned,
            globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
            isFarmableCategory = isFarmable
        )
        val isCapReached = (categoryEarned + awarded >= categoryCap) ||
            (isFarmable && globalEarned + awarded >= GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP) ||
            (requestedAmount > 0 && awarded < requestedAmount)
        val newBalance = balanceFlow.value + awarded
        balanceFlow.value = newBalance
        val tx = CoinTransaction(
            id = java.util.UUID.randomUUID().toString(),
            idempotencyKey = idempotencyKey,
            deltaAmount = awarded,
            eventType = eventType,
            sourceEntityId = sourceEntityId,
            balanceAfter = newBalance
        )
        transactionsFlow.value = transactionsFlow.value + tx
        return AwardCoinsResult(
            coinsAwarded = awarded,
            requestedCoins = requestedAmount,
            isCapReached = isCapReached,
            isDuplicate = false,
            transaction = tx
        )
    }

    override suspend fun recordTransaction(transaction: CoinTransaction) {
        val newBalance = balanceFlow.value + transaction.deltaAmount
        val tx = if (transaction.balanceAfter == 0 && newBalance != 0) {
            transaction.copy(balanceAfter = newBalance)
        } else {
            transaction
        }
        transactionsFlow.value = transactionsFlow.value + tx
        balanceFlow.value = newBalance
    }

    override suspend fun purchaseShopItem(shopItemId: String, idempotencyKey: String) {
        val item = getShopItemById(shopItemId) ?: return
        if (!item.isUnlocked) {
            balanceFlow.value -= item.priceCoins
            updateShopItem(item.copy(isUnlocked = true, unlockedTimestamp = System.currentTimeMillis()))
            equipShopItem(shopItemId, true)
        }
    }

    override suspend fun equipShopItem(shopItemId: String, equip: Boolean) {
        val current = profileFlow.value ?: CompanionProfile(speciesName = "Sproutling")
        profileFlow.value = current.copy(
            activeAccessoryId = if (equip) shopItemId else null
        )
    }

    override suspend fun getCompanionProfile(): CompanionProfile? = profileFlow.value

    override fun observeCompanionProfile(): Flow<CompanionProfile?> = profileFlow

    override suspend fun updateCompanionProfile(profile: CompanionProfile) {
        profileFlow.value = profile
    }

    override fun getAllShopItems(): Flow<List<ShopItem>> = shopItemsFlow

    override suspend fun getShopItemById(id: String): ShopItem? =
        shopItemsFlow.value.find { it.id == id }

    override suspend fun saveShopItems(items: List<ShopItem>) {
        val ids = items.map { it.id }.toSet()
        shopItemsFlow.value = shopItemsFlow.value.filter { it.id !in ids } + items
    }

    override suspend fun updateShopItem(item: ShopItem) {
        shopItemsFlow.value = shopItemsFlow.value.filter { it.id != item.id } + item
    }
}

class TestDayTypeRepository : DayTypeRepository {
    val dayTypesFlow = MutableStateFlow<List<DayType>>(emptyList())
    val overridesFlow = MutableStateFlow<Map<String, DayOverride>>(emptyMap())

    override fun getAllDayTypes(): Flow<List<DayType>> = dayTypesFlow

    override suspend fun getDayTypeById(id: String): DayType? =
        dayTypesFlow.value.find { it.id == id }

    override suspend fun saveDayType(dayType: DayType) {
        dayTypesFlow.value = dayTypesFlow.value.filter { it.id != dayType.id } + dayType
    }

    override suspend fun saveDayTypes(dayTypes: List<DayType>) {
        this.dayTypesFlow.value = dayTypes
    }

    override suspend fun ensureDefaultDayTypes(): List<DayType> {
        if (dayTypesFlow.value.isEmpty()) {
            dayTypesFlow.value = DayType.BUILT_IN_DAY_TYPES
        }
        return dayTypesFlow.value
    }

    override suspend fun getOverrideForDate(date: String): DayOverride? =
        overridesFlow.value[date]

    override fun observeOverrideForDate(date: String): Flow<DayOverride?> =
        overridesFlow.map { it[date] }

    override suspend fun setOverrideForDate(override: DayOverride) {
        overridesFlow.value = overridesFlow.value + (override.date to override)
    }

    override suspend fun deleteOverrideForDate(date: String) {
        overridesFlow.value = overridesFlow.value - date
    }
}

class TestUnfiledInboxRepository : UnfiledInboxRepository {
    val inboxItemsFlow = MutableStateFlow<List<UnfiledInboxItem>>(emptyList())

    override fun getAllInboxItems(): Flow<List<UnfiledInboxItem>> = inboxItemsFlow

    override suspend fun getInboxItemById(id: String): UnfiledInboxItem? =
        inboxItemsFlow.value.find { it.id == id }

    override suspend fun saveInboxItem(item: UnfiledInboxItem) {
        inboxItemsFlow.value = inboxItemsFlow.value.filter { it.id != item.id } + item
    }

    override suspend fun deleteInboxItem(item: UnfiledInboxItem) {
        inboxItemsFlow.value = inboxItemsFlow.value.filter { it.id != item.id }
    }
}

class TestStudySessionRepository : StudySessionRepository {
    val sessionsFlow = MutableStateFlow<List<StudySession>>(emptyList())

    override fun getSessionsForDate(date: String): Flow<List<StudySession>> =
        sessionsFlow.map { list -> list.filter { it.sessionDate == date } }

    override fun getRecentSessions(limit: Int): Flow<List<StudySession>> =
        sessionsFlow.map { it.take(limit) }

    override fun getAllSessions(): Flow<List<StudySession>> = sessionsFlow

    override suspend fun saveSession(session: StudySession) {
        sessionsFlow.value = sessionsFlow.value.filter { it.id != session.id } + session
    }

    override suspend fun getTotalStudyMinutesForDate(date: String): Int =
        sessionsFlow.value.filter { it.sessionDate == date && !it.isInterrupted }.sumOf { it.durationMinutes }

    override suspend fun recordSessionWithReward(session: StudySession, idempotencyKey: String) {
        saveSession(session)
    }
}

class TestTaskAlarmScheduler : TaskAlarmScheduler {
    var hasExactPermission: Boolean = true
    val scheduledTasks = mutableListOf<Task>()
    val cancelledAlarms = mutableListOf<String>()

    override fun canScheduleExactAlarms(): Boolean = hasExactPermission

    override fun scheduleTaskAlarm(task: Task): Boolean {
        scheduledTasks.add(task)
        return true
    }

    override fun cancelTaskAlarm(taskId: String) {
        cancelledAlarms.add(taskId)
    }

    fun clear() {
        scheduledTasks.clear()
        cancelledAlarms.clear()
        hasExactPermission = true
    }
}

class TestInterventionRuleRepository(
    initialRules: List<InterventionRule> = emptyList()
) : InterventionRuleRepository {
    val rulesFlow = MutableStateFlow<List<InterventionRule>>(initialRules)

    override fun getAllRules(): Flow<List<InterventionRule>> = rulesFlow

    override fun getWhitelistedRules(): Flow<List<InterventionRule>> =
        rulesFlow.map { list -> list.filter { !it.isStrictBlocked } }

    override fun getBlockedAppRules(): Flow<List<InterventionRule>> =
        rulesFlow.map { list -> list.filter { it.isStrictBlocked } }

    override suspend fun getRuleForPackage(packageName: String): InterventionRule? =
        rulesFlow.value.find { it.packageName.equals(packageName, ignoreCase = true) }

    override suspend fun saveRule(rule: InterventionRule) {
        val current = rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.packageName.equals(rule.packageName, ignoreCase = true) }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(rule)
        }
        rulesFlow.value = current
    }

    override suspend fun deleteRule(packageName: String) {
        rulesFlow.value = rulesFlow.value.filterNot { it.packageName.equals(packageName, ignoreCase = true) }
    }
}
