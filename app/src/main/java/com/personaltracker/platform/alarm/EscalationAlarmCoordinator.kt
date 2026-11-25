package com.personaltracker.platform.alarm

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.engine.BoundedEscalationEngine
import com.personaltracker.domain.repository.EscalationStateRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome returned by [EscalationAlarmCoordinator.onAlarmTriggered].
 */
sealed interface AlarmTriggerOutcome {
    data class PostNotification(
        val notificationId: Int,
        val title: String,
        val message: String,
        val channelIntensity: String,
        val isUrgent: Boolean,
        val taskId: String?,
        val escalationAttempt: Int,
        val maxReFires: Int = 0,
        val taskTitle: String = title,
        val attachUrgentActions: Boolean
    ) : AlarmTriggerOutcome

    data class Suppress(
        val reason: String
    ) : AlarmTriggerOutcome
}

/**
 * Outcome returned by [EscalationAlarmCoordinator.onNotificationAction].
 */
sealed interface NotificationActionOutcome {
    data class Acknowledged(
        val taskId: String,
        val notificationId: Int
    ) : NotificationActionOutcome

    data class Completed(
        val taskId: String,
        val notificationId: Int,
        val coinsAwarded: Int
    ) : NotificationActionOutcome

    data class Ignored(
        val reason: String
    ) : NotificationActionOutcome
}

/**
 * Thread-safe, JVM-testable coordinator synchronizing alarm triggers and notification actions
 * via a per-coordinator [Mutex] (AD-1, AD-4, FR-5.4, FR-5.5, Story 5.2).
 */
@Singleton
class EscalationAlarmCoordinator @Inject constructor(
    private val taskRepository: TaskRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val exactAlarmScheduler: ExactAlarmScheduler,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val escalationEngine: BoundedEscalationEngine,
    private val escalationStateRepository: EscalationStateRepository
) {
    internal var timeProvider: () -> Long = { System.currentTimeMillis() }

    private val mutex = Mutex()

    /**
     * Evaluates an incoming alarm broadcast under [mutex], verifying task state, acknowledgement,
     * and policy bounds before deciding whether to post a notification and schedule a follow-up.
     */
    suspend fun onAlarmTriggered(
        alarmId: String,
        taskId: String?,
        title: String,
        message: String,
        deliveryIntensity: String,
        escalationAttempt: Int
    ): AlarmTriggerOutcome = mutex.withLock {
        if (taskId.isNullOrBlank()) {
            val isUrgent = deliveryIntensity.equals("URGENT", ignoreCase = true)
            return@withLock AlarmTriggerOutcome.PostNotification(
                notificationId = alarmId.hashCode(),
                title = title,
                message = message,
                channelIntensity = deliveryIntensity,
                isUrgent = isUrgent,
                taskId = null,
                escalationAttempt = escalationAttempt,
                maxReFires = 0,
                taskTitle = title,
                attachUrgentActions = false
            )
        }

        val task = taskRepository.getTaskById(taskId)
        if (task == null || !task.state.equals("PENDING", ignoreCase = true)) {
            taskAlarmScheduler.cancelTaskAlarm(taskId)
            return@withLock AlarmTriggerOutcome.Suppress(
                reason = if (task == null) "TASK_NOT_FOUND" else "TASK_NOT_PENDING"
            )
        }

        if (escalationStateRepository.isAcknowledged(taskId)) {
            taskAlarmScheduler.cancelTaskAlarm(taskId)
            return@withLock AlarmTriggerOutcome.Suppress(reason = "TASK_ACKNOWLEDGED")
        }

        val policy = escalationStateRepository.getPolicy()
        if (escalationAttempt < 0) {
            exactAlarmScheduler.cancelEscalationAlarm(taskId)
            return@withLock AlarmTriggerOutcome.Suppress(reason = "INVALID_ATTEMPT")
        }
        if (escalationAttempt > policy.maxReFires) {
            exactAlarmScheduler.cancelEscalationAlarm(taskId)
            return@withLock AlarmTriggerOutcome.Suppress(reason = "ATTEMPT_EXCEEDS_POLICY")
        }

        val isUrgent = task.deliveryIntensity.equals("URGENT", ignoreCase = true)
        val notificationId = taskId.hashCode()
        val baseMessage = task.description?.ifBlank { null } ?: message

        if (!isUrgent) {
            exactAlarmScheduler.cancelEscalationAlarm(taskId)
            return@withLock AlarmTriggerOutcome.PostNotification(
                notificationId = notificationId,
                title = task.title,
                message = baseMessage,
                channelIntensity = task.deliveryIntensity,
                isUrgent = false,
                taskId = taskId,
                escalationAttempt = 0,
                maxReFires = policy.maxReFires,
                taskTitle = task.title,
                attachUrgentActions = false
            )
        }

        val decision = escalationEngine.evaluateNextEscalation(
            taskId = task.id,
            deliveryIntensity = task.deliveryIntensity,
            taskState = task.state,
            currentAttemptNumber = escalationAttempt,
            isAcknowledged = false,
            currentEpochMillis = timeProvider(),
            policy = policy
        )

        if (decision.shouldScheduleNext) {
            exactAlarmScheduler.scheduleEscalation(
                task = task,
                attemptNumber = decision.nextAttemptNumber,
                triggerEpochMillis = decision.nextTriggerEpochMillis
            )
        } else {
            exactAlarmScheduler.cancelEscalationAlarm(task.id)
        }

        val resolvedTitle = if (escalationAttempt >= 1) {
            "Reminder: ${task.title}"
        } else {
            task.title
        }
        val resolvedMessage = if (escalationAttempt >= 1) {
            "Follow-up $escalationAttempt of ${policy.maxReFires} \u2022 Ready when you are"
        } else {
            baseMessage
        }

        AlarmTriggerOutcome.PostNotification(
            notificationId = notificationId,
            title = resolvedTitle,
            message = resolvedMessage,
            channelIntensity = task.deliveryIntensity,
            isUrgent = true,
            taskId = taskId,
            escalationAttempt = escalationAttempt,
            maxReFires = policy.maxReFires,
            taskTitle = task.title,
            attachUrgentActions = true
        )
    }

    /**
     * Handles notification action buttons and swipe-dismiss intents under [mutex].
     */
    suspend fun onNotificationAction(
        action: String,
        taskId: String?
    ): NotificationActionOutcome = mutex.withLock {
        if (taskId.isNullOrBlank()) {
            return@withLock NotificationActionOutcome.Ignored(reason = "BLANK_TASK_ID")
        }

        when (action) {
            NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK -> {
                escalationStateRepository.markAcknowledged(taskId)
                exactAlarmScheduler.cancelEscalationAlarm(taskId)
                NotificationActionOutcome.Acknowledged(
                    taskId = taskId,
                    notificationId = taskId.hashCode()
                )
            }

            NotificationActionReceiver.ACTION_COMPLETE_TASK -> {
                val result = runCatching { completeTaskUseCase(taskId) }
                escalationStateRepository.clearAcknowledgement(taskId)
                taskAlarmScheduler.cancelTaskAlarm(taskId)
                NotificationActionOutcome.Completed(
                    taskId = taskId,
                    notificationId = taskId.hashCode(),
                    coinsAwarded = result.getOrNull()?.coinReward ?: 0
                )
            }

            else -> NotificationActionOutcome.Ignored(reason = "UNKNOWN_ACTION")
        }
    }
}
