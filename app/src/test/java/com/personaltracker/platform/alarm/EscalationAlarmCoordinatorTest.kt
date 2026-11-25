package com.personaltracker.platform.alarm

import com.personaltracker.domain.engine.BoundedEscalationEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.EscalationPolicy
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.EscalationStateRepository
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.EvaluateTaskUrgencyScarcityUseCase
import com.personaltracker.ui.TestEconomyRepository
import com.personaltracker.ui.TestTaskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EscalationAlarmCoordinatorTest {

    private lateinit var taskRepository: TestTaskRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var fakeDelegate: FakeAlarmManagerDelegate
    private lateinit var exactAlarmScheduler: ExactAlarmScheduler
    private lateinit var taskAlarmScheduler: TaskAlarmSchedulerImpl
    private lateinit var escalationStateRepository: InMemoryEscalationStateRepository
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var scarcityUseCase: EvaluateTaskUrgencyScarcityUseCase
    private lateinit var coordinator: EscalationAlarmCoordinator

    private val cancelledNotificationIds = mutableListOf<Int>()
    private var currentMillis = 2_000_000_000_000L

    @Before
    fun setUp() {
        cancelledNotificationIds.clear()
        currentMillis = 2_000_000_000_000L
        economyRepository = TestEconomyRepository().apply {
            profileFlow.value = CompanionProfile(
                id = 1,
                speciesName = "Sproutling",
                level = 1,
                currentEnergyUnits = 20
            )
        }
        taskRepository = TestTaskRepository(economyRepository)
        fakeDelegate = FakeAlarmManagerDelegate()
        exactAlarmScheduler = ExactAlarmScheduler(fakeDelegate)
        taskAlarmScheduler = TaskAlarmSchedulerImpl(
            exactAlarmScheduler = exactAlarmScheduler,
            notificationCanceller = { cancelledNotificationIds.add(it) }
        )
        escalationStateRepository = InMemoryEscalationStateRepository()
        completeTaskUseCase = CompleteTaskUseCase(
            taskRepository = taskRepository,
            economyRepository = economyRepository,
            ledgerEngine = GamificationLedgerEngine(),
            taskAlarmScheduler = taskAlarmScheduler,
            escalationStateRepository = escalationStateRepository
        )
        scarcityUseCase = EvaluateTaskUrgencyScarcityUseCase(taskRepository)
        coordinator = EscalationAlarmCoordinator(
            taskRepository = taskRepository,
            completeTaskUseCase = completeTaskUseCase,
            exactAlarmScheduler = exactAlarmScheduler,
            taskAlarmScheduler = taskAlarmScheduler,
            escalationEngine = BoundedEscalationEngine(),
            escalationStateRepository = escalationStateRepository
        ).apply {
            timeProvider = { currentMillis }
        }
    }

    @Test
    fun `initial URGENT attempt 0 posts original task details and schedules escalation attempt 1`() = runTest {
        val task = Task(
            id = "task-u1",
            title = "Submit Exam Form",
            description = "Deadline is 5 PM",
            scheduledDate = "2026-11-25",
            scheduledTime = "16:30",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val outcome = coordinator.onAlarmTriggered(
            alarmId = "TASK_task-u1",
            taskId = "task-u1",
            title = task.title,
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 0
        )

        assertTrue(outcome is AlarmTriggerOutcome.PostNotification)
        val post = outcome as AlarmTriggerOutcome.PostNotification
        assertEquals("task-u1".hashCode(), post.notificationId)
        assertEquals("Submit Exam Form", post.title)
        assertEquals("Deadline is 5 PM", post.message)
        assertTrue(post.isUrgent)
        assertTrue(post.attachUrgentActions)
        assertEquals(0, post.escalationAttempt)
        assertEquals(3, post.maxReFires)

        assertEquals(1, fakeDelegate.exactAlarms.size)
        val scheduled = fakeDelegate.exactAlarms.first()
        assertEquals("ESCALATION_task-u1", scheduled.alarmId)
        assertEquals(1, scheduled.escalationAttempt)
        assertEquals(currentMillis + 5 * 60_000L, scheduled.triggerEpochMillis)
    }

    @Test
    fun `follow-up cap reached on attempt 3 posts final follow-up without scheduling attempt 4`() = runTest {
        val task = Task(
            id = "task-u2",
            title = "Take Medication",
            scheduledDate = "2026-11-25",
            scheduledTime = "09:00",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val outcome = coordinator.onAlarmTriggered(
            alarmId = "ESCALATION_task-u2",
            taskId = "task-u2",
            title = task.title,
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 3
        )

        assertTrue(outcome is AlarmTriggerOutcome.PostNotification)
        val post = outcome as AlarmTriggerOutcome.PostNotification
        assertEquals("Reminder: Take Medication", post.title)
        assertEquals("Follow-up 3 of 3 \u2022 Ready when you are", post.message)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_task-u2"))
    }

    @Test
    fun `policy lowered mid-sequence suppresses stale attempt exceeding maxReFires`() = runTest {
        val task = Task(
            id = "task-u3",
            title = "Call Advisor",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)
        escalationStateRepository.updatePolicy(EscalationPolicy(retryIntervalMinutes = 5, maxReFires = 1))

        val outcome = coordinator.onAlarmTriggered(
            alarmId = "ESCALATION_task-u3",
            taskId = "task-u3",
            title = task.title,
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 2
        )

        assertTrue(outcome is AlarmTriggerOutcome.Suppress)
        assertEquals("ATTEMPT_EXCEEDS_POLICY", (outcome as AlarmTriggerOutcome.Suppress).reason)
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_task-u3"))
        assertEquals(0, fakeDelegate.exactAlarms.size)
    }

    @Test
    fun `acknowledge action marks ack, cancels escalation, keeps task PENDING, and is idempotent`() = runTest {
        val task = Task(
            id = "task-ack",
            title = "Pack Passport",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val firstAction = coordinator.onNotificationAction(
            action = NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK,
            taskId = "task-ack"
        )
        val secondAction = coordinator.onNotificationAction(
            action = NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK,
            taskId = "task-ack"
        )

        assertTrue(firstAction is NotificationActionOutcome.Acknowledged)
        assertTrue(secondAction is NotificationActionOutcome.Acknowledged)
        assertEquals("task-ack".hashCode(), (firstAction as NotificationActionOutcome.Acknowledged).notificationId)
        assertTrue(escalationStateRepository.isAcknowledged("task-ack"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_task-ack"))
        assertEquals("PENDING", taskRepository.getTaskById("task-ack")?.state)

        // Subsequent trigger sees ack=true and suppresses cleanly
        val triggerAfterAck = coordinator.onAlarmTriggered(
            alarmId = "ESCALATION_task-ack",
            taskId = "task-ack",
            title = task.title,
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 1
        )
        assertTrue(triggerAfterAck is AlarmTriggerOutcome.Suppress)
        assertEquals("TASK_ACKNOWLEDGED", (triggerAfterAck as AlarmTriggerOutcome.Suppress).reason)
    }

    @Test
    fun `Mark Done action completes task, clears ack, cancels alarms, and releases Urgent slot`() = runTest {
        val task1 = Task(
            id = "urgent-1",
            title = "Urgent One",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        val task2 = Task(
            id = "urgent-2",
            title = "Urgent Two",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTasks(listOf(task1, task2))
        escalationStateRepository.markAcknowledged("urgent-1")

        val beforeScarcity = scarcityUseCase("2026-11-25")
        assertEquals(2, beforeScarcity.activeUrgentCount)
        assertTrue(beforeScarcity.isSoftCapReached)

        val actionOutcome = coordinator.onNotificationAction(
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK,
            taskId = "urgent-1"
        )

        assertTrue(actionOutcome is NotificationActionOutcome.Completed)
        val completed = actionOutcome as NotificationActionOutcome.Completed
        assertEquals("urgent-1".hashCode(), completed.notificationId)
        assertEquals(10, completed.coinsAwarded)
        assertEquals("COMPLETED", taskRepository.getTaskById("urgent-1")?.state)
        assertFalse(escalationStateRepository.isAcknowledged("urgent-1"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("TASK_urgent-1"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_urgent-1"))
        assertTrue(cancelledNotificationIds.contains("urgent-1".hashCode()))

        val afterScarcity = scarcityUseCase("2026-11-25")
        assertEquals(1, afterScarcity.activeUrgentCount)
        assertFalse(afterScarcity.isSoftCapReached)
    }

    @Test
    fun `Mark Done on missing task suppresses error and still cancels alarms and clears ack`() = runTest {
        escalationStateRepository.markAcknowledged("missing-task")

        val outcome = coordinator.onNotificationAction(
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK,
            taskId = "missing-task"
        )

        assertTrue(outcome is NotificationActionOutcome.Completed)
        assertEquals(0, (outcome as NotificationActionOutcome.Completed).coinsAwarded)
        assertFalse(escalationStateRepository.isAcknowledged("missing-task"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("TASK_missing-task"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_missing-task"))
    }

    @Test
    fun `stale or missing task alarm trigger suppresses notification and cancels both alarms`() = runTest {
        val completedTask = Task(
            id = "task-done",
            title = "Finished",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "COMPLETED"
        )
        taskRepository.saveTask(completedTask)

        val completedOutcome = coordinator.onAlarmTriggered(
            alarmId = "TASK_task-done",
            taskId = "task-done",
            title = "Finished",
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 0
        )
        assertTrue(completedOutcome is AlarmTriggerOutcome.Suppress)
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("TASK_task-done"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_task-done"))

        fakeDelegate.cancelledAlarmIds.clear()
        val missingOutcome = coordinator.onAlarmTriggered(
            alarmId = "TASK_missing",
            taskId = "missing",
            title = "Missing",
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 1
        )
        assertTrue(missingOutcome is AlarmTriggerOutcome.Suppress)
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("TASK_missing"))
        assertTrue(fakeDelegate.cancelledAlarmIds.contains("ESCALATION_missing"))
    }

    @Test
    fun `standalone broadcast with null taskId posts directly without Room lookup or escalation`() = runTest {
        val outcome = coordinator.onAlarmTriggered(
            alarmId = "standalone-alert",
            taskId = null,
            title = "Evening Check-in",
            message = "Ready when you are",
            deliveryIntensity = "URGENT",
            escalationAttempt = 0
        )

        assertTrue(outcome is AlarmTriggerOutcome.PostNotification)
        val post = outcome as AlarmTriggerOutcome.PostNotification
        assertEquals("standalone-alert".hashCode(), post.notificationId)
        assertFalse(post.attachUrgentActions)
        assertEquals(0, fakeDelegate.exactAlarms.size)
    }

    @Test
    fun `non-urgent task fires once without urgent actions or escalation`() = runTest {
        val task = Task(
            id = "task-imp",
            title = "Water Plants",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "IMPORTANT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val outcome = coordinator.onAlarmTriggered(
            alarmId = "TASK_task-imp",
            taskId = "task-imp",
            title = task.title,
            message = "Ready when you are",
            deliveryIntensity = "IMPORTANT",
            escalationAttempt = 0
        )

        assertTrue(outcome is AlarmTriggerOutcome.PostNotification)
        val post = outcome as AlarmTriggerOutcome.PostNotification
        assertFalse(post.isUrgent)
        assertFalse(post.attachUrgentActions)
        assertEquals(0, fakeDelegate.exactAlarms.size)
    }

    @Test
    fun `concurrent trigger and acknowledge calls remain serialized and safe`() = runTest {
        val task = Task(
            id = "task-race",
            title = "Concurrent Check",
            scheduledDate = "2026-11-25",
            deliveryIntensity = "URGENT",
            state = "PENDING"
        )
        taskRepository.saveTask(task)

        val ackDeferred = async {
            coordinator.onNotificationAction(
                action = NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK,
                taskId = "task-race"
            )
        }
        val triggerDeferred = async {
            coordinator.onAlarmTriggered(
                alarmId = "ESCALATION_task-race",
                taskId = "task-race",
                title = task.title,
                message = "Fallback",
                deliveryIntensity = "URGENT",
                escalationAttempt = 1
            )
        }
        awaitAll(ackDeferred, triggerDeferred)

        assertTrue(escalationStateRepository.isAcknowledged("task-race"))
        val followUpTrigger = coordinator.onAlarmTriggered(
            alarmId = "ESCALATION_task-race",
            taskId = "task-race",
            title = task.title,
            message = "Fallback",
            deliveryIntensity = "URGENT",
            escalationAttempt = 2
        )
        assertTrue(followUpTrigger is AlarmTriggerOutcome.Suppress)
    }

    private class InMemoryEscalationStateRepository : EscalationStateRepository {
        private val policyFlow = MutableStateFlow(EscalationPolicy())
        private val ackedIds = mutableSetOf<String>()

        override fun observePolicy(): Flow<EscalationPolicy> = policyFlow.asStateFlow()
        override fun getPolicy(): EscalationPolicy = policyFlow.value
        override fun updatePolicy(policy: EscalationPolicy) {
            policyFlow.value = policy
        }

        override fun isAcknowledged(taskId: String): Boolean = taskId in ackedIds
        override fun markAcknowledged(taskId: String) {
            ackedIds.add(taskId)
        }

        override fun clearAcknowledgement(taskId: String) {
            ackedIds.remove(taskId)
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
