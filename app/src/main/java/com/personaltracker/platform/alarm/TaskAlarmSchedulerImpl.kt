package com.personaltracker.platform.alarm

import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.model.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Platform implementation of [TaskAlarmScheduler] delegating to [ExactAlarmScheduler] (FR-5, AD-1).
 */
@Singleton
class TaskAlarmSchedulerImpl @Inject constructor(
    private val exactAlarmScheduler: ExactAlarmScheduler
) : TaskAlarmScheduler {

    override fun scheduleTaskAlarm(task: Task): Boolean {
        return exactAlarmScheduler.scheduleForTask(task)
    }

    override fun cancelTaskAlarm(taskId: String) {
        val rawId = taskId.removePrefix("TASK_")
        exactAlarmScheduler.cancelAlarm("TASK_$rawId")
    }
}
