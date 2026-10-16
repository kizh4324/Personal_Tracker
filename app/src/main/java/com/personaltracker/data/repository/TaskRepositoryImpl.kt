package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.TaskDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
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
internal class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val coinLedgerDao: CoinLedgerDao,
    private val transactionRunner: TransactionRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    override fun getTasksForDate(date: String): Flow<List<Task>> =
        taskDao.getTasksForDate(date)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getTaskById(id: String): Task? =
        withContext(ioDispatcher) {
            taskDao.getTaskById(id)?.toDomain()
        }

    override suspend fun getOverduePendingTasks(currentDate: String): List<Task> =
        withContext(ioDispatcher) {
            taskDao.getOverduePendingTasks(currentDate).map { it.toDomain() }
        }

    override suspend fun getActiveInProgressTask(): Task? =
        withContext(ioDispatcher) {
            taskDao.getActiveInProgressTask()?.toDomain()
        }

    override suspend fun getLatestInterruptedTask(): Task? =
        withContext(ioDispatcher) {
            taskDao.getLatestInterruptedTask()?.toDomain()
        }

    override suspend fun saveTask(task: Task) =
        withContext(ioDispatcher) {
            taskDao.insertTask(task.toEntity())
        }

    override suspend fun saveTasks(tasks: List<Task>) =
        withContext(ioDispatcher) {
            taskDao.insertTasks(tasks.map { it.toEntity() })
        }

    override suspend fun updateTask(task: Task) =
        withContext(ioDispatcher) {
            taskDao.updateTask(task.toEntity())
        }

    override suspend fun deleteTask(task: Task) =
        withContext(ioDispatcher) {
            taskDao.deleteTask(task.toEntity())
        }

    override suspend fun completeTaskWithReward(
        taskId: String,
        completionTimestamp: Long,
        coinReward: Int,
        idempotencyKey: String
    ) = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        require(coinReward >= 0) { "Coin reward cannot be negative: $coinReward" }
        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                throw IllegalStateException("Duplicate coin transaction idempotency key: $idempotencyKey")
            }

            val task = taskDao.getTaskById(taskId)
                ?: throw IllegalArgumentException("Task not found with id: $taskId")

            check(task.state != "COMPLETED") { "Task is already completed: $taskId" }

            val updatedTask = task.copy(
                state = "COMPLETED",
                completionTimestamp = completionTimestamp
            )
            taskDao.updateTask(updatedTask)

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            val newBalance = currentBalance + coinReward

            val ledgerEntry = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = coinReward,
                eventType = "TASK_COMPLETION",
                sourceEntityId = taskId,
                balanceAfter = newBalance,
                timestamp = completionTimestamp
            )
            coinLedgerDao.insertTransaction(ledgerEntry)
        }
    }
}
