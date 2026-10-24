package com.personaltracker.domain.usecase

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/**
 * Pure Kotlin use case validating, persisting, and coordinating reminder alarms for task creation and editing (AD-1, AD-3, FR-2.2, FR-5).
 */
class SaveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskAlarmScheduler: TaskAlarmScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    internal var clock: java.time.Clock = java.time.Clock.systemDefaultZone()

    constructor(
        taskRepository: TaskRepository,
        taskAlarmScheduler: TaskAlarmScheduler,
        clock: java.time.Clock,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(taskRepository, taskAlarmScheduler, ioDispatcher) {
        this.clock = clock
    }

    /**
     * Validates and persists the given [task], coordinating reminder alarms via [TaskAlarmScheduler].
     *
     * @throws IllegalArgumentException if the title is blank or scheduled time format is invalid.
     */
    suspend operator fun invoke(task: Task): Task = withContext(ioDispatcher) {
        val trimmedTitle = task.title.trim()
        require(trimmedTitle.isNotBlank()) { "Task title must not be blank" }

        val normalizedIntensity = when (task.deliveryIntensity.uppercase()) {
            "URGENT" -> "URGENT"
            "IMPORTANT" -> "IMPORTANT"
            else -> "ROUTINE"
        }

        val todayDate = LocalDate.now(clock)
        val taskId = task.id.ifBlank { UUID.randomUUID().toString() }
        val scheduledDate = task.scheduledDate.ifBlank { todayDate.toString() }
        val cleanScheduledTime = task.scheduledTime?.trim()?.ifBlank { null }

        val parsedTime = if (!cleanScheduledTime.isNullOrBlank()) {
            runCatching { LocalTime.parse(cleanScheduledTime) }.getOrElse {
                throw IllegalArgumentException("Scheduled time must be in 24h HH:mm format")
            }
        } else {
            null
        }

        val isPastTime = if (parsedTime != null) {
            val parsedDate = runCatching { LocalDate.parse(scheduledDate) }.getOrDefault(todayDate)
            LocalDateTime.of(parsedDate, parsedTime).isBefore(LocalDateTime.now(clock))
        } else {
            false
        }

        val taskToSave = task.copy(
            id = taskId,
            title = trimmedTitle,
            description = task.description?.trim()?.ifBlank { null },
            scheduledDate = scheduledDate,
            scheduledTime = cleanScheduledTime,
            deliveryIntensity = normalizedIntensity
        )

        taskRepository.saveTask(taskToSave)

        // Cancel previous pending alarm if any, and reschedule if scheduledTime is present and not past
        taskAlarmScheduler.cancelTaskAlarm(taskId)
        if (!cleanScheduledTime.isNullOrBlank() &&
            !isPastTime &&
            !taskToSave.state.equals("COMPLETED", ignoreCase = true) &&
            !taskToSave.state.equals("CANCELLED", ignoreCase = true)
        ) {
            taskAlarmScheduler.scheduleTaskAlarm(taskToSave)
        }

        taskToSave
    }
}
