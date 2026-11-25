package com.personaltracker.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.personaltracker.MainActivity
import com.personaltracker.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata model for scheduling an exact or fallback tiered alarm.
 */
data class ScheduledAlarm(
    val alarmId: String,
    val triggerEpochMillis: Long,
    val title: String,
    val message: String,
    val taskId: String? = null,
    val deliveryIntensity: String = "ROUTINE",
    val escalationAttempt: Int = 0
)

/**
 * Delegate abstraction isolating Android OS [AlarmManager] and [PendingIntent] interactions (AD-1).
 * Permits hermetic JVM unit testing without Robolectric or Android framework stubs.
 */
interface AlarmManagerDelegate {
    fun canScheduleExactAlarms(): Boolean
    fun scheduleExact(alarm: ScheduledAlarm)
    fun scheduleInexactFallback(alarm: ScheduledAlarm)
    fun cancelAlarm(alarmId: String)
}

/**
 * Default Android OS implementation of [AlarmManagerDelegate].
 */
class SystemAlarmManagerDelegate(
    private val context: Context
) : AlarmManagerDelegate {

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun scheduleExact(alarm: ScheduledAlarm) {
        val requestCode = alarm.alarmId.hashCode()

        val triggerIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = ExactAlarmScheduler.ACTION_TRIGGER_ALARM
            putExtra(ExactAlarmScheduler.EXTRA_ALARM_ID, alarm.alarmId)
            putExtra(ExactAlarmScheduler.EXTRA_TITLE, alarm.title)
            putExtra(ExactAlarmScheduler.EXTRA_MESSAGE, alarm.message)
            putExtra(ExactAlarmScheduler.EXTRA_TASK_ID, alarm.taskId)
            putExtra(ExactAlarmScheduler.EXTRA_INTENSITY, alarm.deliveryIntensity)
            putExtra(ExactAlarmScheduler.EXTRA_ESCALATION_ATTEMPT, alarm.escalationAttempt)
        }
        val triggerPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (alarm.deliveryIntensity.equals("URGENT", ignoreCase = true)) {
            // URGENT: uses setAlarmClock for prominent wakeup with status bar indicator
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val clockInfo = AlarmManager.AlarmClockInfo(alarm.triggerEpochMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, triggerPendingIntent)
        } else {
            // IMPORTANT: exact wakeup from idle without persistent status bar clock icon
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerEpochMillis,
                triggerPendingIntent
            )
        }
    }

    override fun scheduleInexactFallback(alarm: ScheduledAlarm) {
        val requestCode = alarm.alarmId.hashCode()

        val triggerIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = ExactAlarmScheduler.ACTION_TRIGGER_ALARM
            putExtra(ExactAlarmScheduler.EXTRA_ALARM_ID, alarm.alarmId)
            putExtra(ExactAlarmScheduler.EXTRA_TITLE, alarm.title)
            putExtra(ExactAlarmScheduler.EXTRA_MESSAGE, alarm.message)
            putExtra(ExactAlarmScheduler.EXTRA_TASK_ID, alarm.taskId)
            putExtra(ExactAlarmScheduler.EXTRA_INTENSITY, alarm.deliveryIntensity)
            putExtra(ExactAlarmScheduler.EXTRA_ESCALATION_ATTEMPT, alarm.escalationAttempt)
        }
        val triggerPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarm.triggerEpochMillis,
            triggerPendingIntent
        )
    }

    override fun cancelAlarm(alarmId: String) {
        val requestCode = alarmId.hashCode()
        val triggerIntent = Intent(context, AlarmNotificationReceiver::class.java).apply {
            action = ExactAlarmScheduler.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            triggerIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

/**
 * Exact alarm scheduler managing tiered task reminders via [AlarmManager] (FR-5, AD-1, AD-4, Story 5.1).
 * Enforces API 31+ canScheduleExactAlarms() permission checks, graceful idle fallback,
 * tier-differentiated scheduling, and defensive SecurityException recovery.
 */
@Singleton
class ExactAlarmScheduler(
    private val delegate: AlarmManagerDelegate
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(SystemAlarmManagerDelegate(context))

    /**
     * Calculates UTC epoch milliseconds from ISO date (YYYY-MM-DD) and 24h time (HH:mm).
     */
    fun calculateTriggerEpochMillis(
        date: String,
        time: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = LocalDate.parse(date)
        val localTime = LocalTime.parse(time)
        return LocalDateTime.of(localDate, localTime)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Checks if exact alarm scheduling is permitted on current API level.
     */
    fun canScheduleExact(): Boolean = delegate.canScheduleExactAlarms()

    /**
     * Schedules an alarm with defensive fallbacks and tier differentiation (AD-4, Story 5.1).
     * - Past timestamps are rejected immediately.
     * - ROUTINE tasks use inexact idle-wake alarms to avoid cluttering status bar.
     * - IMPORTANT / URGENT tasks check exact alarm permission; if denied or if SecurityException
     *   is thrown at runtime, gracefully routes to inexact fallback.
     *
     * @return true if scheduled exactly (or accepted as routine), false if fallen back or rejected.
     */
    fun scheduleAlarm(alarm: ScheduledAlarm): Boolean {
        // Guard against past timestamps
        if (alarm.triggerEpochMillis <= System.currentTimeMillis()) {
            return false
        }

        // Routine tasks: battery-friendly inexact scheduling without status bar clock icon
        if (alarm.deliveryIntensity.equals("ROUTINE", ignoreCase = true)) {
            delegate.scheduleInexactFallback(alarm)
            return true
        }

        // Important / Urgent tasks: attempt exact scheduling with defensive catch
        return try {
            if (delegate.canScheduleExactAlarms()) {
                delegate.scheduleExact(alarm)
                true
            } else {
                delegate.scheduleInexactFallback(alarm)
                false
            }
        } catch (_: SecurityException) {
            delegate.scheduleInexactFallback(alarm)
            false
        }
    }

    /**
     * Convenience method to schedule an alarm for a domain [Task].
     * Returns true if scheduled, false if task does not contain scheduledTime or was rejected.
     */
    fun scheduleForTask(task: Task, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val time = task.scheduledTime ?: return false
        val triggerMillis = calculateTriggerEpochMillis(task.scheduledDate, time, zoneId)
        val alarm = ScheduledAlarm(
            alarmId = "TASK_${task.id}",
            triggerEpochMillis = triggerMillis,
            title = task.title,
            message = task.description?.ifBlank { null } ?: "Scheduled task ready when you are.",
            taskId = task.id,
            deliveryIntensity = task.deliveryIntensity
        )
        return scheduleAlarm(alarm)
    }

    /**
     * Schedules a bounded escalation follow-up alarm for [task] at [triggerEpochMillis].
     */
    fun scheduleEscalation(
        task: Task,
        attemptNumber: Int,
        triggerEpochMillis: Long
    ): Boolean {
        if (attemptNumber <= 0) return false
        val alarm = ScheduledAlarm(
            alarmId = "ESCALATION_${task.id}",
            triggerEpochMillis = triggerEpochMillis,
            title = task.title,
            message = task.description?.ifBlank { null } ?: "Scheduled task ready when you are.",
            taskId = task.id,
            deliveryIntensity = task.deliveryIntensity,
            escalationAttempt = attemptNumber
        )
        return scheduleAlarm(alarm)
    }

    /**
     * Cancels any pending escalation follow-up alarm for [taskId].
     */
    fun cancelEscalationAlarm(taskId: String) {
        val rawId = taskId.removePrefix("ESCALATION_").removePrefix("TASK_")
        delegate.cancelAlarm("ESCALATION_$rawId")
    }

    /**
     * Cancels any pending alarm with matching alarmId.
     */
    fun cancelAlarm(alarmId: String) {
        delegate.cancelAlarm(alarmId)
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.personaltracker.action.TRIGGER_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_INTENSITY = "extra_intensity"
        const val EXTRA_ESCALATION_ATTEMPT = "extra_escalation_attempt"
    }
}
