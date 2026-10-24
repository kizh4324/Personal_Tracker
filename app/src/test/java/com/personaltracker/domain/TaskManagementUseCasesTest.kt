package com.personaltracker.domain

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.DeleteTaskUseCase
import com.personaltracker.domain.usecase.EvaluateTaskUrgencyScarcityUseCase
import com.personaltracker.domain.usecase.SaveTaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

private class FakeTaskAlarmScheduler : TaskAlarmScheduler {
    val scheduledTasks = mutableListOf<Task>()
    val cancelledAlarms = mutableListOf<String>()

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
    }
}

private class FakeTaskRepository : TaskRepository {
    val tasks = mutableListOf<Task>()
    private val _tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    private fun emit() {
        _tasksFlow.value = tasks.toList()
    }

    override fun getTasksForDate(date: String): Flow<List<Task>> = _tasksFlow.asStateFlow()

    override suspend fun getTaskById(id: String): Task? = tasks.find { it.id == id }

    override suspend fun getOverduePendingTasks(currentDate: String): List<Task> =
        tasks.filter { it.scheduledDate < currentDate && it.state == "PENDING" }

    override suspend fun getActiveInProgressTask(): Task? = tasks.find { it.state == "IN_PROGRESS" }

    override suspend fun getLatestInterruptedTask(): Task? = tasks.find { it.state == "INTERRUPTED" }

    override suspend fun saveTask(task: Task) {
        tasks.removeAll { it.id == task.id }
        tasks.add(task)
        emit()
    }

    override suspend fun saveTasks(tasks: List<Task>) {
        tasks.forEach { saveTask(it) }
    }

    override suspend fun updateTask(task: Task) {
        saveTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        tasks.removeAll { it.id == task.id }
        emit()
    }

    override suspend fun completeTaskWithReward(
        taskId: String,
        completionTimestamp: Long,
        coinReward: Int,
        idempotencyKey: String
    ) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            tasks[index] = tasks[index].copy(
                state = "COMPLETED",
                completionTimestamp = completionTimestamp
            )
            emit()
        }
    }
}

private class FakeEconomyRepository : EconomyRepository {
    val transactions = mutableListOf<CoinTransaction>()
    private var profile: CompanionProfile? = CompanionProfile(
        id = 1,
        speciesName = "Fox",
        level = 1,
        currentEnergyUnits = 50
    )
    val shopItems = mutableListOf<ShopItem>()

    override fun getAllTransactions(): Flow<List<CoinTransaction>> = MutableStateFlow(transactions)
    override suspend fun getTransactionByIdempotencyKey(key: String): CoinTransaction? =
        transactions.find { it.idempotencyKey == key }

    override suspend fun getLatestBalance(): Int = 100
    override fun observeLatestBalance(): Flow<Int> = MutableStateFlow(100)
    override suspend fun recordTransaction(transaction: CoinTransaction) {
        transactions.add(transaction)
    }

    override suspend fun purchaseShopItem(shopItemId: String, idempotencyKey: String) {}
    override suspend fun getCompanionProfile(): CompanionProfile? = profile
    override fun observeCompanionProfile(): Flow<CompanionProfile?> = MutableStateFlow(profile)
    override suspend fun updateCompanionProfile(profile: CompanionProfile) {
        this.profile = profile
    }

    override fun getAllShopItems(): Flow<List<ShopItem>> = MutableStateFlow(shopItems)
    override suspend fun getShopItemById(id: String): ShopItem? = shopItems.find { it.id == id }
    override suspend fun saveShopItems(items: List<ShopItem>) {
        shopItems.addAll(items)
    }

    override suspend fun updateShopItem(item: ShopItem) {}
}

class TaskManagementUseCasesTest {

    private lateinit var fakeTaskRepo: FakeTaskRepository
    private lateinit var fakeAlarmScheduler: FakeTaskAlarmScheduler
    private lateinit var fakeEconomyRepo: FakeEconomyRepository
    private lateinit var ledgerEngine: GamificationLedgerEngine

    private lateinit var saveTaskUseCase: SaveTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var evaluateScarcityUseCase: EvaluateTaskUrgencyScarcityUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase

    private val fixedInstant = java.time.Instant.parse("2026-10-24T10:00:00Z")
    private val fixedZone = java.time.ZoneId.of("UTC")
    private val fixedClock = java.time.Clock.fixed(fixedInstant, fixedZone)
    private val today = LocalDate.now(fixedClock).toString()

    @Before
    fun setUp() {
        fakeTaskRepo = FakeTaskRepository()
        fakeAlarmScheduler = FakeTaskAlarmScheduler()
        fakeEconomyRepo = FakeEconomyRepository()
        ledgerEngine = GamificationLedgerEngine()

        saveTaskUseCase = SaveTaskUseCase(fakeTaskRepo, fakeAlarmScheduler, fixedClock)
        deleteTaskUseCase = DeleteTaskUseCase(fakeTaskRepo, fakeAlarmScheduler)
        evaluateScarcityUseCase = EvaluateTaskUrgencyScarcityUseCase(fakeTaskRepo)
        completeTaskUseCase = CompleteTaskUseCase(fakeTaskRepo, fakeEconomyRepo, ledgerEngine, fakeAlarmScheduler)
    }

    // =========================================================================
    // SaveTaskUseCase Tests
    // =========================================================================

    @Test
    fun testSaveTaskCreatesNewTaskWithDefaultValuesAndSchedulesAlarm() = runTest {
        val futureTime = "12:00"
        val task = Task(
            id = "",
            title = "  Study Discrete Math  ",
            scheduledDate = "",
            scheduledTime = futureTime,
            deliveryIntensity = "routine"
        )

        val saved = saveTaskUseCase(task)

        assertTrue(saved.id.isNotBlank())
        assertEquals("Study Discrete Math", saved.title)
        assertEquals(today, saved.scheduledDate)
        assertEquals(futureTime, saved.scheduledTime)
        assertEquals("ROUTINE", saved.deliveryIntensity)

        // Repository has task
        val repoTask = fakeTaskRepo.getTaskById(saved.id)
        assertNotNull(repoTask)
        assertEquals("Study Discrete Math", repoTask?.title)

        // Alarm scheduler called
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains(saved.id))
        assertEquals(1, fakeAlarmScheduler.scheduledTasks.size)
        assertEquals(saved.id, fakeAlarmScheduler.scheduledTasks[0].id)
    }

    @Test
    fun testSaveTaskWithCompletedOrCancelledStateSuppressesAlarm() = runTest {
        val futureTime = "12:00"
        val completedTask = Task(
            id = "t_completed",
            title = "Already Done",
            scheduledDate = today,
            scheduledTime = futureTime,
            state = "COMPLETED"
        )
        saveTaskUseCase(completedTask)
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)

        val cancelledTask = Task(
            id = "t_cancelled",
            title = "Discarded Task",
            scheduledDate = today,
            scheduledTime = futureTime,
            state = "CANCELLED"
        )
        saveTaskUseCase(cancelledTask)
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)
    }

    @Test
    fun testSaveTaskWithInvalidTimeFormatThrowsIllegalArgumentException() = runTest {
        val task = Task(
            id = "t_bad_time",
            title = "Bad Time Format",
            scheduledDate = today,
            scheduledTime = "25:70"
        )

        try {
            saveTaskUseCase(task)
            fail("Expected IllegalArgumentException for invalid scheduledTime")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("24h HH:mm format") == true)
        }
    }

    @Test
    fun testSaveTaskWithPastTimeTodaySuppressesAlarmScheduling() = runTest {
        val pastTime = "08:00"
        val task = Task(
            id = "t_past",
            title = "Past Task",
            scheduledDate = today,
            scheduledTime = pastTime
        )

        val saved = saveTaskUseCase(task)
        assertEquals(pastTime, saved.scheduledTime)
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_past"))
    }

    @Test
    fun testSaveTaskRejectsBlankTitle() = runTest {
        val task = Task(
            id = "t1",
            title = "   ",
            scheduledDate = today
        )

        try {
            saveTaskUseCase(task)
            fail("Expected IllegalArgumentException for blank title")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("blank") == true)
        }

        assertEquals(0, fakeTaskRepo.tasks.size)
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)
    }

    @Test
    fun testSaveTaskWithoutTimeCancelsAlarmAndDoesNotSchedule() = runTest {
        val task = Task(
            id = "t_no_time",
            title = "Review Architecture Notes",
            scheduledDate = today,
            scheduledTime = null,
            deliveryIntensity = "IMPORTANT"
        )

        val saved = saveTaskUseCase(task)

        assertEquals("IMPORTANT", saved.deliveryIntensity)
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_no_time"))
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)
    }

    @Test
    fun testEditTaskClearingTimeCancelsPreviousAlarm() = runTest {
        val existingTask = Task(
            id = "t_edit",
            title = "Morning Run",
            scheduledDate = today,
            scheduledTime = "07:00",
            deliveryIntensity = "ROUTINE"
        )
        fakeTaskRepo.saveTask(existingTask)

        fakeAlarmScheduler.clear()

        // User edits and clears time
        val updated = existingTask.copy(scheduledTime = "  ")
        val saved = saveTaskUseCase(updated)

        assertNull(saved.scheduledTime)
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_edit"))
        assertEquals(0, fakeAlarmScheduler.scheduledTasks.size)
    }

    @Test
    fun testSaveTaskNormalizesInvalidDeliveryIntensityToRoutine() = runTest {
        val task = Task(
            id = "t_intensity",
            title = "Quick Break",
            scheduledDate = today,
            deliveryIntensity = "UNKNOWN_INTENSITY"
        )

        val saved = saveTaskUseCase(task)
        assertEquals("ROUTINE", saved.deliveryIntensity)
    }

    // =========================================================================
    // DeleteTaskUseCase Tests
    // =========================================================================

    @Test
    fun testDeleteTaskRemovesTaskAndCancelsAlarm() = runTest {
        val task = Task(
            id = "t_delete",
            title = "To be deleted",
            scheduledDate = today,
            scheduledTime = "10:00"
        )
        fakeTaskRepo.saveTask(task)

        val result = deleteTaskUseCase("t_delete")

        assertTrue(result)
        assertNull(fakeTaskRepo.getTaskById("t_delete"))
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_delete"))
    }

    @Test
    fun testDeleteNonExistentTaskReturnsFalseSafeNoOp() = runTest {
        val result = deleteTaskUseCase("missing_task_id")

        assertFalse(result)
        assertEquals(0, fakeAlarmScheduler.cancelledAlarms.size)
    }

    @Test
    fun testDeleteTaskRejectsBlankId() = runTest {
        try {
            deleteTaskUseCase("  ")
            fail("Expected IllegalArgumentException for blank id")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("blank") == true)
        }
    }

    // =========================================================================
    // CompleteTaskUseCase Alarm Cancellation Tests
    // =========================================================================

    @Test
    fun testCompleteTaskCancelsReminderAlarm() = runTest {
        val task = Task(
            id = "t_complete",
            title = "Task with alarm",
            scheduledDate = today,
            scheduledTime = "15:00",
            deliveryIntensity = "URGENT"
        )
        fakeTaskRepo.saveTask(task)

        val result = completeTaskUseCase("t_complete")

        assertEquals(10, result.coinReward)
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_complete"))
        val completedTask = fakeTaskRepo.getTaskById("t_complete")
        assertEquals("COMPLETED", completedTask?.state)
    }

    @Test
    fun testCompleteAlreadyCompletedTaskIdempotentlyCancelsAlarm() = runTest {
        val task = Task(
            id = "t_done_already",
            title = "Already Done",
            scheduledDate = today,
            scheduledTime = "12:00",
            state = "COMPLETED"
        )
        fakeTaskRepo.saveTask(task)
        fakeAlarmScheduler.clear()
        val result = completeTaskUseCase("t_done_already")
        assertEquals(0, result.coinReward)
        assertTrue(fakeAlarmScheduler.cancelledAlarms.contains("t_done_already"))
    }

    // =========================================================================
    // EvaluateTaskUrgencyScarcityUseCase Tests
    // =========================================================================

    @Test
    fun testUrgencyScarcityUnderThreshold() {
        val tasks = listOf(
            Task(id = "1", title = "Task 1", scheduledDate = today, deliveryIntensity = "ROUTINE"),
            Task(id = "2", title = "Task 2", scheduledDate = today, deliveryIntensity = "URGENT")
        )

        val result = evaluateScarcityUseCase(tasks)

        assertEquals(1, result.activeUrgentCount)
        assertFalse(result.isSoftCapReached)
        assertNull(result.advisoryMessage)
    }

    @Test
    fun testUrgencyScarcitySoftCapReachedAtTwoUrgentTasks() {
        val tasks = listOf(
            Task(id = "1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "3", title = "Routine 1", scheduledDate = today, deliveryIntensity = "ROUTINE")
        )

        val result = evaluateScarcityUseCase(tasks)

        assertEquals(2, result.activeUrgentCount)
        assertTrue(result.isSoftCapReached)
        assertEquals("2 urgent tasks already active today. Keep focus sharp!", result.advisoryMessage)
    }

    @Test
    fun testUrgencyScarcitySoftCapExceededAtThreeUrgentTasks() {
        val tasks = listOf(
            Task(id = "1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "3", title = "Urgent 3", scheduledDate = today, deliveryIntensity = "URGENT")
        )

        val result = evaluateScarcityUseCase(tasks)

        assertEquals(3, result.activeUrgentCount)
        assertTrue(result.isSoftCapReached)
        assertEquals("3 urgent tasks already active today. Keep focus sharp!", result.advisoryMessage)
    }

    @Test
    fun testUrgencyScarcityExcludesCompletedAndCancelledTasks() {
        val tasks = listOf(
            Task(id = "1", title = "Urgent 1", scheduledDate = today, deliveryIntensity = "URGENT", state = "PENDING"),
            Task(id = "2", title = "Urgent 2", scheduledDate = today, deliveryIntensity = "URGENT", state = "COMPLETED"),
            Task(id = "3", title = "Urgent 3", scheduledDate = today, deliveryIntensity = "URGENT", state = "CANCELLED")
        )

        val result = evaluateScarcityUseCase(tasks)

        assertEquals(1, result.activeUrgentCount)
        assertFalse(result.isSoftCapReached)
        assertNull(result.advisoryMessage)
    }

    @Test
    fun testUrgencyScarcityExcludesCurrentlyEditingTaskId() {
        val tasks = listOf(
            Task(id = "task_a", title = "Urgent A", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "task_b", title = "Urgent B", scheduledDate = today, deliveryIntensity = "URGENT")
        )

        // When editing task_a, active urgent count excluding task_a is 1 (under cap)
        val result = evaluateScarcityUseCase(tasks, excludingTaskId = "task_a")

        assertEquals(1, result.activeUrgentCount)
        assertFalse(result.isSoftCapReached)
        assertNull(result.advisoryMessage)
    }

    @Test
    fun testUrgencyScarcitySuspendOverloadQueriesRepository() = runTest {
        val tasks = listOf(
            Task(id = "u1", title = "U1", scheduledDate = today, deliveryIntensity = "URGENT"),
            Task(id = "u2", title = "U2", scheduledDate = today, deliveryIntensity = "URGENT")
        )
        fakeTaskRepo.saveTasks(tasks)

        val result = evaluateScarcityUseCase(today)

        assertEquals(2, result.activeUrgentCount)
        assertTrue(result.isSoftCapReached)
    }
}
