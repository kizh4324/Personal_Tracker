package com.personaltracker.platform

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.platform.alarm.AlarmManagerDelegate
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import com.personaltracker.platform.alarm.ScheduledAlarm
import com.personaltracker.platform.receiver.BootRecoveryCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class BootCompletedRecoveryTest {

    private lateinit var fakeTaskRepository: FakeTaskRepository
    private lateinit var fakeStudySessionRepository: FakeStudySessionRepository
    private lateinit var fakeAlarmDelegate: FakeAlarmManagerDelegate
    private lateinit var exactAlarmScheduler: ExactAlarmScheduler
    private lateinit var coordinator: BootRecoveryCoordinator

    private val directTransactionRunner = object : TransactionRunner {
        override suspend fun <T> invoke(block: suspend () -> T): T = block()
    }

    @Before
    fun setUp() {
        fakeTaskRepository = FakeTaskRepository()
        fakeStudySessionRepository = FakeStudySessionRepository()
        fakeAlarmDelegate = FakeAlarmManagerDelegate()
        exactAlarmScheduler = ExactAlarmScheduler(delegate = fakeAlarmDelegate)

        coordinator = BootRecoveryCoordinator(
            taskRepository = fakeTaskRepository,
            studySessionRepository = fakeStudySessionRepository,
            exactAlarmScheduler = exactAlarmScheduler,
            transactionRunner = directTransactionRunner
        )
    }

    @Test
    fun `boot recovery marks active study session as interrupted`() = runTest {
        val testDate = LocalDate.now().toString()
        val activeSession = StudySession(
            id = "active-session-123",
            subjectTag = "Calculus",
            sessionDate = testDate,
            startTime = "10:00",
            durationMinutes = 25,
            coinsEarned = 0,
            isInterrupted = false
        )
        fakeStudySessionRepository.saveSession(activeSession)

        coordinator.recover(
            activeSessionId = "active-session-123",
            currentDate = testDate
        )

        val updated = fakeStudySessionRepository.savedSessions.last()
        assertEquals("active-session-123", updated.id)
        assertTrue("Active session must be marked as isInterrupted = true upon reboot", updated.isInterrupted)
    }

    @Test
    fun `boot recovery reschedules alarms for pending tasks today`() = runTest {
        val futureDate = LocalDate.now().plusDays(1).toString()
        val pendingTaskWithTime = Task(
            id = "task-1",
            title = "Morning Routine",
            scheduledDate = futureDate,
            scheduledTime = "09:00",
            state = "PENDING",
            deliveryIntensity = "IMPORTANT"
        )
        val completedTask = Task(
            id = "task-2",
            title = "Breakfast",
            scheduledDate = futureDate,
            scheduledTime = "08:00",
            state = "COMPLETED",
            deliveryIntensity = "IMPORTANT"
        )
        val taskWithoutTime = Task(
            id = "task-3",
            title = "Evening walk",
            scheduledDate = futureDate,
            scheduledTime = null,
            state = "PENDING",
            deliveryIntensity = "IMPORTANT"
        )

        fakeTaskRepository.tasksForDate = listOf(pendingTaskWithTime, completedTask, taskWithoutTime)

        coordinator.recover(
            activeSessionId = null,
            currentDate = futureDate
        )

        assertEquals("Only pending tasks with scheduledTime should be rescheduled", 1, fakeAlarmDelegate.exactAlarms.size)
        assertEquals("TASK_task-1", fakeAlarmDelegate.exactAlarms.first().alarmId)
    }

    // --- In-Memory Fakes ---

    private class FakeTaskRepository : TaskRepository {
        var tasksForDate = listOf<Task>()

        override fun getTasksForDate(date: String): Flow<List<Task>> = flowOf(tasksForDate)
        override suspend fun getTaskById(id: String): Task? = tasksForDate.firstOrNull { it.id == id }
        override suspend fun getOverduePendingTasks(currentDate: String): List<Task> = emptyList()
        override suspend fun getActiveInProgressTask(): Task? = null
        override suspend fun getLatestInterruptedTask(): Task? = null
        override suspend fun saveTask(task: Task) {}
        override suspend fun saveTasks(tasks: List<Task>) {}
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTask(task: Task) {}
        override suspend fun completeTaskWithReward(taskId: String, completionTimestamp: Long, coinReward: Int, idempotencyKey: String) {}
    }

    private class FakeStudySessionRepository : StudySessionRepository {
        val savedSessions = mutableListOf<StudySession>()

        override fun getSessionsForDate(date: String): Flow<List<StudySession>> = flowOf(savedSessions.filter { it.sessionDate == date })
        override fun getRecentSessions(limit: Int): Flow<List<StudySession>> = flowOf(savedSessions)
        override fun getAllSessions(): Flow<List<StudySession>> = flowOf(savedSessions)
        override suspend fun saveSession(session: StudySession) {
            savedSessions.removeAll { it.id == session.id }
            savedSessions.add(session)
        }
        override suspend fun getTotalStudyMinutesForDate(date: String): Int = 0
        override suspend fun recordSessionWithReward(session: StudySession, idempotencyKey: String) {
            saveSession(session)
        }
    }

    private class FakeAlarmManagerDelegate : AlarmManagerDelegate {
        var hasExactPermission: Boolean = true
        val exactAlarms = mutableListOf<ScheduledAlarm>()
        val inexactAlarms = mutableListOf<ScheduledAlarm>()
        val cancelledAlarmIds = mutableListOf<String>()

        override fun canScheduleExactAlarms(): Boolean = hasExactPermission

        override fun scheduleExact(alarm: ScheduledAlarm) {
            exactAlarms.add(alarm)
        }

        override fun scheduleInexactFallback(alarm: ScheduledAlarm) {
            inexactAlarms.add(alarm)
        }

        override fun cancelAlarm(alarmId: String) {
            cancelledAlarmIds.add(alarmId)
        }
    }
}
