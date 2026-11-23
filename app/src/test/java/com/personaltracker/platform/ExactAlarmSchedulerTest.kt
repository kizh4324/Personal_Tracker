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
import java.time.LocalDate
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
    fun `scheduleAlarm invokes scheduleExact when exact alarm permission is granted for IMPORTANT or URGENT`() {
        fakeDelegate.hasExactPermission = true
        val futureMillis = System.currentTimeMillis() + 3600_000L

        val alarm = ScheduledAlarm(
            alarmId = "alarm-1",
            triggerEpochMillis = futureMillis,
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
        assertEquals(futureMillis, fakeDelegate.exactAlarms.first().triggerEpochMillis)
        assertEquals("Math Practice", fakeDelegate.exactAlarms.first().title)
    }

    @Test
    fun `scheduleAlarm routes ROUTINE alarms directly to inexact fallback and returns true`() {
        fakeDelegate.hasExactPermission = true
        val futureMillis = System.currentTimeMillis() + 3600_000L

        val alarm = ScheduledAlarm(
            alarmId = "alarm-routine",
            triggerEpochMillis = futureMillis,
            title = "Gentle Walk",
            message = "Time for fresh air",
            taskId = "task-routine",
            deliveryIntensity = "ROUTINE"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertTrue("Expected scheduleAlarm to return true for accepted routine task", result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(1, fakeDelegate.inexactAlarms.size)
        assertEquals("alarm-routine", fakeDelegate.inexactAlarms.first().alarmId)
    }

    @Test
    fun `scheduleAlarm falls back to inexact idle schedule when permission is denied and returns false`() {
        fakeDelegate.hasExactPermission = false
        val futureMillis = System.currentTimeMillis() + 3600_000L

        val alarm = ScheduledAlarm(
            alarmId = "alarm-2",
            triggerEpochMillis = futureMillis,
            title = "Urgent Focus",
            message = "Deadline reminder",
            taskId = "task-2",
            deliveryIntensity = "URGENT"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertFalse("Expected scheduleAlarm to return false when falling back", result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(1, fakeDelegate.inexactAlarms.size)
        assertEquals("alarm-2", fakeDelegate.inexactAlarms.first().alarmId)
    }

    @Test
    fun `scheduleAlarm catches SecurityException from scheduleExact and falls back to inexact returning false`() {
        fakeDelegate.hasExactPermission = true
        fakeDelegate.throwSecurityExceptionOnExact = true
        val futureMillis = System.currentTimeMillis() + 3600_000L

        val alarm = ScheduledAlarm(
            alarmId = "alarm-sec-ex",
            triggerEpochMillis = futureMillis,
            title = "Urgent Meeting",
            message = "Join call now",
            taskId = "task-sec-ex",
            deliveryIntensity = "URGENT"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertFalse("Expected scheduleAlarm to return false on SecurityException fallback", result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(1, fakeDelegate.inexactAlarms.size)
        assertEquals("alarm-sec-ex", fakeDelegate.inexactAlarms.first().alarmId)
    }

    @Test
    fun `scheduleAlarm rejects past triggerEpochMillis and returns false without scheduling`() {
        fakeDelegate.hasExactPermission = true
        val pastMillis = System.currentTimeMillis() - 60_000L

        val alarm = ScheduledAlarm(
            alarmId = "alarm-past",
            triggerEpochMillis = pastMillis,
            title = "Old Task",
            message = "Already past",
            taskId = "task-past",
            deliveryIntensity = "IMPORTANT"
        )

        val result = scheduler.scheduleAlarm(alarm)

        assertFalse("Expected scheduleAlarm to reject past timestamps", result)
        assertEquals(0, fakeDelegate.exactAlarms.size)
        assertEquals(0, fakeDelegate.inexactAlarms.size)
    }

    @Test
    fun `scheduleForTask schedules alarm when task has scheduledTime in future`() {
        fakeDelegate.hasExactPermission = true
        val tomorrow = LocalDate.now().plusDays(1).toString()

        val task = Task(
            id = "task-alpha",
            title = "Review Physics notes",
            description = "Chapter 4 kinematics",
            scheduledDate = tomorrow,
            scheduledTime = "14:00",
            deliveryIntensity = "URGENT"
        )

        val result = scheduler.scheduleForTask(task, ZoneId.systemDefault())

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
    fun `canScheduleExact reflects delegate state`() {
        fakeDelegate.hasExactPermission = true
        assertTrue(scheduler.canScheduleExact())

        fakeDelegate.hasExactPermission = false
        assertFalse(scheduler.canScheduleExact())
    }

    @Test
    fun `cancelAlarm invokes delegate with target alarmId`() {
        scheduler.cancelAlarm("alarm-to-cancel")

        assertEquals(listOf("alarm-to-cancel"), fakeDelegate.cancelledAlarmIds)
    }

    // --- Fake Delegate ---

    private class FakeAlarmManagerDelegate : AlarmManagerDelegate {
        var hasExactPermission: Boolean = true
        var throwSecurityExceptionOnExact: Boolean = false
        val exactAlarms = mutableListOf<ScheduledAlarm>()
        val inexactAlarms = mutableListOf<ScheduledAlarm>()
        val cancelledAlarmIds = mutableListOf<String>()

        override fun canScheduleExactAlarms(): Boolean = hasExactPermission

        override fun scheduleExact(alarm: ScheduledAlarm) {
            if (throwSecurityExceptionOnExact) {
                throw SecurityException("Caller not allowed to schedule exact alarms")
            }
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
