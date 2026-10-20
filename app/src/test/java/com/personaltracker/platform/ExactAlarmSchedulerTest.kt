package com.personaltracker.platform

import com.personaltracker.domain.model.Task
import com.personaltracker.platform.alarm.AlarmManagerDelegate
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import com.personaltracker.platform.alarm.ScheduledAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ExactAlarmSchedulerTest {

    private lateinit var fakeDelegate: FakeAlarmManagerDelegate
    private lateinit var scheduler: ExactAlarmScheduler

    @Before
    fun setUp() {
        fakeDelegate = FakeAlarmManagerDelegate()
        scheduler = ExactAlarmScheduler(delegate = fakeDelegate)
    }

    @Test
    fun `calculateTriggerEpochMillis accurately parses date and time in target timezone`() {
        val utcZone = ZoneId.of("UTC")
        val epochMillis = scheduler.calculateTriggerEpochMillis(
            date = "2026-10-18",
            time = "15:30",
            zoneId = utcZone
        )

        val expected = LocalDateTime.of(2026, 10, 18, 15, 30)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        assertEquals(expected, epochMillis)
    }

    @Test
    fun `scheduleAlarm invokes scheduleExact when exact alarm permission is granted`() {
        fakeDelegate.hasExactPermission = true

        val alarm = ScheduledAlarm(
            alarmId = "alarm-1",
            triggerEpochMillis = 1760790000000L,
            title = "Math Practice",
            message = "Time for study",
            taskId = "task-1",
            deliveryIntensity = "IMPORTANT"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertTrue("Expected scheduleAlarm to return true for exact scheduling", result)
        assertEquals(1, fakeDelegate.exactAlarms.size)
        assertEquals(0, fakeDelegate.inexactAlarms.size)
        assertEquals("alarm-1", fakeDelegate.exactAlarms.first().alarmId)
        assertEquals(1760790000000L, fakeDelegate.exactAlarms.first().triggerEpochMillis)
        assertEquals("Math Practice", fakeDelegate.exactAlarms.first().title)
    }

    @Test
    fun `scheduleAlarm falls back to inexact idle schedule when permission is denied`() {
        fakeDelegate.hasExactPermission = false

        val alarm = ScheduledAlarm(
            alarmId = "alarm-2",
            triggerEpochMillis = 1760795000000L,
            title = "Calm Reminder",
            message = "Ready when you are",
            taskId = "task-2",
            deliveryIntensity = "ROUTINE"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertFalse("Expected scheduleAlarm to return false when falling back", result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(1, fakeDelegate.inexactAlarms.size)
        assertEquals("alarm-2", fakeDelegate.inexactAlarms.first().alarmId)
        assertEquals(1760795000000L, fakeDelegate.inexactAlarms.first().triggerEpochMillis)
    }

    @Test
    fun `scheduleForTask schedules alarm when task has scheduledTime`() {
        fakeDelegate.hasExactPermission = true

        val task = Task(
            id = "task-alpha",
            title = "Review Physics notes",
            description = "Chapter 4 kinematics",
            scheduledDate = "2026-10-18",
            scheduledTime = "14:00",
            deliveryIntensity = "URGENT"
        )

        val result = scheduler.scheduleForTask(task, ZoneId.of("UTC"))

        assertTrue(result)
        assertEquals(1, fakeDelegate.exactAlarms.size)
        val scheduled = fakeDelegate.exactAlarms.first()
        assertEquals("TASK_task-alpha", scheduled.alarmId)
        assertEquals("Review Physics notes", scheduled.title)
        assertEquals("Chapter 4 kinematics", scheduled.message)
        assertEquals("URGENT", scheduled.deliveryIntensity)
    }

    @Test
    fun `scheduleForTask returns false and does not schedule when task has null time`() {
        val task = Task(
            id = "task-no-time",
            title = "Someday task",
            scheduledDate = "2026-10-18",
            scheduledTime = null
        )

        val result = scheduler.scheduleForTask(task)

        assertFalse(result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(0, fakeDelegate.inexactAlarms.size)
    }

    @Test
    fun `cancelAlarm invokes delegate with target alarmId`() {
        scheduler.cancelAlarm("alarm-to-cancel")

        assertEquals(listOf("alarm-to-cancel"), fakeDelegate.cancelledAlarmIds)
    }

    // --- Fake Delegate ---

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
