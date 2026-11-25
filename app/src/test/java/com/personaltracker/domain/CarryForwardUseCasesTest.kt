package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.EscalationPolicy
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.EscalationStateRepository
import com.personaltracker.ui.TestTaskAlarmScheduler
import com.personaltracker.ui.TestTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarryForwardUseCasesTest {

    private lateinit var taskRepository: TestTaskRepository
    private lateinit var carryForwardUseCase: CarryForwardTasksUseCase
    private lateinit var dismissOverdueUseCase: DismissOverdueTasksUseCase

    @Before
    fun setUp() {
        taskRepository = TestTaskRepository()
        carryForwardUseCase = CarryForwardTasksUseCase(taskRepository)
        dismissOverdueUseCase = DismissOverdueTasksUseCase(taskRepository)
    }

    @Test
    fun testCarryForwardEmptyReturnsZero() = runTest {
        val result = carryForwardUseCase(targetDate = "2026-10-24", tasks = emptyList())
        assertEquals(0, result.carriedCount)
        assertTrue(result.updatedTasks.isEmpty())
    }

    @Test
    fun testCarryForwardUpdatesDateAndIncrementsCount() = runTest {
        val task1 = Task(
            id = "t1",
            title = "Read chapter 3",
            scheduledDate = "2026-10-22",
            isCarryForward = false,
            carryForwardCount = 0
        )
        val task2 = Task(
            id = "t2",
            title = "Write lab report",
            scheduledDate = "2026-10-23",
            isCarryForward = true,
            carryForwardCount = 1
        )
        taskRepository.saveTasks(listOf(task1, task2))

        val result = carryForwardUseCase(
            targetDate = "2026-10-24",
            tasks = listOf(task1, task2)
        )

        assertEquals(2, result.carriedCount)
        val updated1 = result.updatedTasks.first { it.id == "t1" }
        assertEquals("2026-10-24", updated1.scheduledDate)
        assertTrue(updated1.isCarryForward)
        assertEquals(1, updated1.carryForwardCount)

        val updated2 = result.updatedTasks.first { it.id == "t2" }
        assertEquals("2026-10-24", updated2.scheduledDate)
        assertTrue(updated2.isCarryForward)
        assertEquals(2, updated2.carryForwardCount)

        // Verify persisted in repository
        val inRepo1 = taskRepository.getTaskById("t1")
        assertEquals("2026-10-24", inRepo1?.scheduledDate)
        assertEquals(1, inRepo1?.carryForwardCount)
    }

    @Test
    fun testCarryForwardIgnoresNonPendingTasks() = runTest {
        val completedTask = Task(
            id = "t_done",
            title = "Completed earlier",
            scheduledDate = "2026-10-22",
            state = "COMPLETED"
        )
        val result = carryForwardUseCase(
            targetDate = "2026-10-24",
            tasks = listOf(completedTask)
        )
        assertEquals(0, result.carriedCount)
        assertTrue(result.updatedTasks.isEmpty())
    }

    @Test
    fun testDismissOverdueEmptyReturnsZero() = runTest {
        val result = dismissOverdueUseCase(targetDate = "2026-10-24", tasks = emptyList())
        assertEquals(0, result.dismissedCount)
        assertTrue(result.dismissedTasks.isEmpty())
    }

    @Test
    fun testDismissOverdueMarksTasksCancelledWithoutPenalty() = runTest {
        val task1 = Task(
            id = "t1",
            title = "Overdue task",
            scheduledDate = "2026-10-20",
            state = "PENDING"
        )
        taskRepository.saveTask(task1)

        val result = dismissOverdueUseCase(
            targetDate = "2026-10-24",
            tasks = listOf(task1)
        )

        assertEquals(1, result.dismissedCount)
        val dismissed = result.dismissedTasks.first()
        assertEquals("CANCELLED", dismissed.state)

        // Verify persisted in repository
        val inRepo = taskRepository.getTaskById("t1")
        assertEquals("CANCELLED", inRepo?.state)
    }

    @Test
    fun testDismissOverdueIgnoresNonPendingTasks() = runTest {
        val completedTask = Task(
            id = "t_done",
            title = "Completed earlier",
            scheduledDate = "2026-10-22",
            state = "COMPLETED"
        )
        val result = dismissOverdueUseCase(
            targetDate = "2026-10-24",
            tasks = listOf(completedTask)
        )
        assertEquals(0, result.dismissedCount)
        assertTrue(result.dismissedTasks.isEmpty())
    }

    @Test
    fun testCarryForwardClearsAcknowledgementCancelsAndReschedulesAlarms() = runTest {
        val scheduler = TestTaskAlarmScheduler()
        val escalationRepo = FakeCarryForwardEscalationRepo().apply { acked.add("t_cf") }
        val useCase = CarryForwardTasksUseCase(taskRepository, scheduler, escalationRepo)

        val taskWithTime = Task(
            id = "t_cf",
            title = "Carried Urgent Task",
            scheduledDate = "2026-10-22",
            scheduledTime = "14:00",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(taskWithTime)

        val result = useCase(targetDate = "2026-10-24", tasks = listOf(taskWithTime))

        assertEquals(1, result.carriedCount)
        assertFalse(escalationRepo.isAcknowledged("t_cf"))
        assertTrue(scheduler.cancelledAlarms.contains("t_cf"))
        assertEquals(1, scheduler.scheduledTasks.size)
        assertEquals("2026-10-24", scheduler.scheduledTasks.first().scheduledDate)
    }

    @Test
    fun testDismissOverdueCancelsAlarmsAndClearsAcknowledgement() = runTest {
        val scheduler = TestTaskAlarmScheduler()
        val escalationRepo = FakeCarryForwardEscalationRepo().apply { acked.add("t_dismiss") }
        val useCase = DismissOverdueTasksUseCase(taskRepository, scheduler, escalationRepo)

        val task = Task(
            id = "t_dismiss",
            title = "Overdue Urgent Task",
            scheduledDate = "2026-10-20",
            scheduledTime = "11:00",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val result = useCase(targetDate = "2026-10-24", tasks = listOf(task))

        assertEquals(1, result.dismissedCount)
        assertTrue(scheduler.cancelledAlarms.contains("t_dismiss"))
        assertFalse(escalationRepo.isAcknowledged("t_dismiss"))
    }

    private fun assertFalse(condition: Boolean) = org.junit.Assert.assertFalse(condition)

    private class FakeCarryForwardEscalationRepo : EscalationStateRepository {
        val acked = mutableSetOf<String>()
        private val policy = MutableStateFlow(EscalationPolicy())
        override fun observePolicy(): Flow<EscalationPolicy> = policy
        override fun getPolicy(): EscalationPolicy = policy.value
        override fun updatePolicy(policy: EscalationPolicy) { this.policy.value = policy }
        override fun isAcknowledged(taskId: String): Boolean = taskId in acked
        override fun markAcknowledged(taskId: String) { acked.add(taskId) }
        override fun clearAcknowledgement(taskId: String) { acked.remove(taskId) }
    }
}
