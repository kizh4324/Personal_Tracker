package com.personaltracker.platform.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.personaltracker.MainActivity
import com.personaltracker.R
import com.personaltracker.platform.notification.TieredNotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * BroadcastReceiver triggered by [ExactAlarmScheduler] to post calm, non-punishing task reminders
 * and coordinate bounded follow-up escalation for URGENT tasks (FR-5, AD-4, Story 5.1, Story 5.2).
 */
class AlarmNotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlarmReceiverEntryPoint {
        fun escalationAlarmCoordinator(): EscalationAlarmCoordinator
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val alarmId = intent.getStringExtra(ExactAlarmScheduler.EXTRA_ALARM_ID) ?: return
        val title = intent.getStringExtra(ExactAlarmScheduler.EXTRA_TITLE)
            ?: context.getString(R.string.notification_channel_important_name)
        val message = intent.getStringExtra(ExactAlarmScheduler.EXTRA_MESSAGE)
            ?: context.getString(R.string.notification_fallback_message)
        val taskId = intent.getStringExtra(ExactAlarmScheduler.EXTRA_TASK_ID)
        val intensity = intent.getStringExtra(ExactAlarmScheduler.EXTRA_INTENSITY) ?: "ROUTINE"
        val escalationAttempt = intent.getIntExtra(ExactAlarmScheduler.EXTRA_ESCALATION_ATTEMPT, 0)

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(RECEIVER_TIMEOUT_MS) {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        appContext,
                        AlarmReceiverEntryPoint::class.java
                    )
                    val outcome = entryPoint.escalationAlarmCoordinator().onAlarmTriggered(
                        alarmId = alarmId,
                        taskId = taskId,
                        title = title,
                        message = message,
                        deliveryIntensity = intensity,
                        escalationAttempt = escalationAttempt
                    )

                    if (outcome !is AlarmTriggerOutcome.PostNotification) {
                        return@withTimeout
                    }

                    // Verify runtime notification permission on API 33+ before attempting notification delivery
                    if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
                        return@withTimeout
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(
                                appContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return@withTimeout
                        }
                    }

                    postNotification(appContext, alarmId, outcome)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle alarm trigger for $alarmId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        alarmId: String,
        outcome: AlarmTriggerOutcome.PostNotification
    ) {
        val targetChannelId = TieredNotificationManager.getChannelIdForIntensity(outcome.channelIntensity)
        val targetTaskId = outcome.taskId
        val baseCode = (targetTaskId?.hashCode() ?: alarmId.hashCode()) * 31

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetTaskId != null) {
                data = Uri.parse("personaltracker://notification/$targetTaskId/open")
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            if (targetTaskId != null) baseCode + 1 else alarmId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isUrgent = outcome.isUrgent
        val isRoutine = outcome.channelIntensity.equals("ROUTINE", ignoreCase = true)

        val priority = when {
            isUrgent -> NotificationCompat.PRIORITY_HIGH
            isRoutine -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notifTitle = if (isUrgent && outcome.escalationAttempt >= 1) {
            context.getString(R.string.notification_escalation_title, outcome.taskTitle)
        } else {
            outcome.title
        }
        val notifMessage = if (isUrgent && outcome.escalationAttempt >= 1) {
            context.getString(
                R.string.notification_escalation_message,
                outcome.escalationAttempt,
                outcome.maxReFires
            )
        } else {
            outcome.message
        }

        val notificationBuilder = NotificationCompat.Builder(context, targetChannelId)
            .setContentTitle(notifTitle)
            .setContentText(notifMessage)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(priority)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (isUrgent) {
            notificationBuilder
                .setVibrate(TieredNotificationManager.URGENT_VIBRATION_PATTERN)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        }

        if (outcome.attachUrgentActions && targetTaskId != null) {
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COMPLETE_TASK
                data = Uri.parse("personaltracker://notification/$targetTaskId/complete")
                putExtra(ExactAlarmScheduler.EXTRA_TASK_ID, targetTaskId)
            }
            val completePendingIntent = PendingIntent.getBroadcast(
                context,
                baseCode + 2,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val acknowledgeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK
                data = Uri.parse("personaltracker://notification/$targetTaskId/acknowledge")
                putExtra(ExactAlarmScheduler.EXTRA_TASK_ID, targetTaskId)
            }
            val acknowledgePendingIntent = PendingIntent.getBroadcast(
                context,
                baseCode + 3,
                acknowledgeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val swipeDismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_ACKNOWLEDGE_TASK
                data = Uri.parse("personaltracker://notification/$targetTaskId/swipe_dismiss")
                putExtra(ExactAlarmScheduler.EXTRA_TASK_ID, targetTaskId)
            }
            val swipeDismissPendingIntent = PendingIntent.getBroadcast(
                context,
                baseCode + 4,
                swipeDismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .addAction(
                    0,
                    context.getString(R.string.notification_action_complete),
                    completePendingIntent
                )
                .addAction(
                    0,
                    context.getString(R.string.notification_action_acknowledge),
                    acknowledgePendingIntent
                )
                .setDeleteIntent(swipeDismissPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        notificationManager.notify(outcome.notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "AlarmNotificationRx"
        private const val RECEIVER_TIMEOUT_MS = 8_000L
    }
}
