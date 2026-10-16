package com.personaltracker.data.repository

import app.cash.turbine.test
import com.personaltracker.data.local.db.daos.TaskDao
import com.personaltracker.data.local.db.entities.TaskEntity
import com.personaltracker.domain.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying repository Flow emissions, entity-domain mapping,
 * and reactive Single Source of Truth (SSOT) behavior (AD-1, AD-6).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeTransactionRunner = object : TransactionRunner {
        override suspend operator fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Test
    fun testTaskRepositoryReactiveFlowObservation() = runTest(testDispatcher) {
        val taskDao = object : TaskDao {
            val state = MutableStateFlow<List<TaskEntity>>(emptyList())

            override fun getTasksForDate(date: String): Flow<List<TaskEntity>> =
                state.map { list -> list.filter { it.scheduledDate == date } }

            override suspend fun getTaskById(id: String): TaskEntity? = state.value.find { it.id == id }
            override suspend fun getOverduePendingTasks(currentDate: String): List<TaskEntity> = emptyList()
            override suspend fun getActiveInProgressTask(): TaskEntity? = null
            override suspend fun getLatestInterruptedTask(): TaskEntity? = null
            override suspend fun insertTask(task: TaskEntity) {
                state.value = state.value.filter { it.id != task.id } + task
            }
            override suspend fun insertTasks(tasks: List<TaskEntity>) {
                state.value = state.value + tasks
            }
            override suspend fun updateTask(task: TaskEntity) {
                insertTask(task)
            }
            override suspend fun deleteTask(task: TaskEntity) {
                state.value = state.value.filter { it.id != task.id }
            }
        }

        val coinLedgerDao = object : com.personaltracker.data.local.db.daos.CoinLedgerDao {
            val transactions = MutableStateFlow<List<com.personaltracker.data.local.db.entities.CoinLedgerEntity>>(emptyList())
            override fun getAllTransactions() = transactions
            override suspend fun getTransactionByIdempotencyKey(key: String) = null
            override suspend fun getLatestBalance() = 0
            override fun observeLatestBalance() = transactions.map { 0 }
            override suspend fun insertTransaction(entry: com.personaltracker.data.local.db.entities.CoinLedgerEntity) {}
        }

        val repo = TaskRepositoryImpl(taskDao, coinLedgerDao, fakeTransactionRunner, testDispatcher)

        repo.getTasksForDate("2026-10-15").test {
            assertEquals("Initial emission must be empty list", emptyList<Task>(), awaitItem())

            repo.saveTask(
                Task(
                    id = "task-alpha",
                    title = "Alpha Routine",
                    scheduledDate = "2026-10-15",
                    estimatedDurationMinutes = 30
                )
            )

            val emission1 = awaitItem()
            assertEquals(1, emission1.size)
            assertEquals("Alpha Routine", emission1[0].title)

            repo.saveTask(
                Task(
                    id = "task-beta",
                    title = "Beta Focus",
                    scheduledDate = "2026-10-15",
                    estimatedDurationMinutes = 60
                )
            )

            val emission2 = awaitItem()
            assertEquals(2, emission2.size)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
