package com.personaltracker.ui

import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestTaskRepository : TaskRepository {
    val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    val overdueTasks = mutableListOf<Task>()
    var interruptedTask: Task? = null

    override fun getTasksForDate(date: String): Flow<List<Task>> =
        tasksFlow.map { list -> list.filter { it.scheduledDate == date } }

    override suspend fun getTaskById(id: String): Task? =
        tasksFlow.value.find { it.id == id }

    override suspend fun getOverduePendingTasks(currentDate: String): List<Task> = overdueTasks

    override suspend fun getActiveInProgressTask(): Task? =
        tasksFlow.value.find { it.state == "IN_PROGRESS" }

    override suspend fun getLatestInterruptedTask(): Task? = interruptedTask

    override suspend fun saveTask(task: Task) {
        tasksFlow.value = tasksFlow.value.filter { it.id != task.id } + task
    }

    override suspend fun saveTasks(tasks: List<Task>) {
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
    }
}

class TestHabitRepository : HabitRepository {
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
    }
}

class TestRoutineRepository : RoutineRepository {
    val routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    val stepsFlow = MutableStateFlow<List<RoutineStep>>(emptyList())

    override fun getRoutinesForDayType(dayTypeId: String): Flow<List<Routine>> =
        routinesFlow.map { list -> list.filter { it.dayTypeId == dayTypeId } }

    override fun getStepsForRoutine(routineId: String): Flow<List<RoutineStep>> =
        stepsFlow.map { list -> list.filter { it.routineId == routineId } }

    override suspend fun saveRoutine(routine: Routine) {
        routinesFlow.value = routinesFlow.value.filter { it.id != routine.id } + routine
    }

    override suspend fun saveSteps(steps: List<RoutineStep>) {
        val ids = steps.map { it.id }.toSet()
        stepsFlow.value = stepsFlow.value.filter { it.id !in ids } + steps
    }

    override suspend fun saveRoutineWithSteps(routine: Routine, steps: List<RoutineStep>) {
        saveRoutine(routine)
        saveSteps(steps)
    }

    override suspend fun deleteRoutine(routineId: String) {
        routinesFlow.value = routinesFlow.value.filter { it.id != routineId }
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

    override suspend fun recordTransaction(transaction: CoinTransaction) {
        transactionsFlow.value = transactionsFlow.value + transaction
        balanceFlow.value += transaction.deltaAmount
    }

    override suspend fun purchaseShopItem(shopItemId: String, idempotencyKey: String) {
        val item = getShopItemById(shopItemId) ?: return
        balanceFlow.value -= item.priceCoins
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
