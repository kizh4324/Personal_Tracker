package com.personaltracker.domain.alarm

import com.personaltracker.domain.model.Task

/**
 * Pure Kotlin domain contract for scheduling and cancelling task reminder alarms (FR-5, AD-1).
 * Isolates Android OS platform alarm APIs from pure domain business logic.
 */
interface TaskAlarmScheduler {

    /**
     * Schedules a reminder alarm for the given [task].
     * Returns true if scheduled, false otherwise (e.g. if task has no scheduledTime).
     */
    fun scheduleTaskAlarm(task: Task): Boolean

    /**
     * Cancels any pending reminder alarm associated with the given [taskId].
     */
    fun cancelTaskAlarm(taskId: String)
}
