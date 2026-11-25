package com.personaltracker.platform.alarm

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.model.Task
import javax.inject.Singleton

/**
 * Platform implementation of [TaskAlarmScheduler] delegating to [ExactAlarmScheduler] (FR-5, AD-1, Story 5.1, Story 5.2).
 */
@Singleton
class TaskAlarmSchedulerImpl(
    private val exactAlarmScheduler: ExactAlarmScheduler,
    private val notificationCanceller: (Int) -> Unit = {}
) : TaskAlarmScheduler {

    override fun canScheduleExactAlarms(): Boolean {
        return exactAlarmScheduler.canScheduleExact()
    }

    override fun scheduleTaskAlarm(task: Task): Boolean {
        return exactAlarmScheduler.scheduleForTask(task)
    }

    override fun cancelTaskAlarm(taskId: String) {
        val rawId = taskId.removePrefix("TASK_").removePrefix("ESCALATION_")
        exactAlarmScheduler.cancelAlarm("TASK_$rawId")
        exactAlarmScheduler.cancelEscalationAlarm(rawId)
        notificationCanceller(rawId.hashCode())
    }
}
