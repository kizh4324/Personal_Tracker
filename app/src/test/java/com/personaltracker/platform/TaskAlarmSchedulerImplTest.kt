package com.personaltracker.platform

import com.personaltracker.domain.model.Task
import com.personaltracker.platform.alarm.AlarmManagerDelegate
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import com.personaltracker.platform.alarm.ScheduledAlarm
import com.personaltracker.platform.alarm.TaskAlarmSchedulerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskAlarmSchedulerImplTest {

    private lateinit var fakeDelegate: FakeAlarmManagerDelegate
    private lateinit var exactAlarmScheduler: ExactAlarmScheduler
    private lateinit var scheduler: TaskAlarmSchedulerImpl

    @Before
    fun setUp() {
        fakeDelegate = FakeAlarmManagerDelegate()
        exactAlarmScheduler = ExactAlarmScheduler(fakeDelegate)
        scheduler = TaskAlarmSchedulerImpl(exactAlarmScheduler)
    }

    @Test
    fun `scheduleTaskAlarm schedules alarm with TASK_ prefix when scheduledTime is present`() {
        val task = Task(
            id = "task_xyz",
            title = "Focus Block",
            scheduledDate = "2026-10-25",
            scheduledTime = "14:00",
            deliveryIntensity = "URGENT"
        )

        val result = scheduler.scheduleTaskAlarm(task)

        assertTrue(result)
        assertEquals(1, fakeDelegate.exactAlarms.size)
        assertEquals("TASK_task_xyz", fakeDelegate.exactAlarms[0].alarmId)
        assertEquals("task_xyz", fakeDelegate.exactAlarms[0].taskId)
        assertEquals("Focus Block", fakeDelegate.exactAlarms[0].title)
        assertEquals("URGENT", fakeDelegate.exactAlarms[0].deliveryIntensity)
    }

    @Test
    fun `scheduleTaskAlarm returns false when task scheduledTime is null`() {
        val task = Task(
            id = "task_no_time",
            title = "Open-ended Task",
            scheduledDate = "2026-10-25",
            scheduledTime = null
        )

        val result = scheduler.scheduleTaskAlarm(task)

        assertFalse(result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
    }

    @Test
    fun `cancelTaskAlarm with raw taskId normalizes to TASK_ prefix`() {
        scheduler.cancelTaskAlarm("abc_123")

        assertEquals(listOf("TASK_abc_123"), fakeDelegate.cancelledAlarmIds)
    }

    @Test
    fun `cancelTaskAlarm with already prefixed taskId avoids duplicate prefix`() {
        scheduler.cancelTaskAlarm("TASK_def_456")

        assertEquals(listOf("TASK_def_456"), fakeDelegate.cancelledAlarmIds)
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
