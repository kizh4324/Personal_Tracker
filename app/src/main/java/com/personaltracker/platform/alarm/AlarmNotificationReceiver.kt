package com.personaltracker.platform.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.personaltracker.MainActivity
import com.personaltracker.R
import com.personaltracker.platform.notification.TieredNotificationManager

/**
 * BroadcastReceiver triggered by [ExactAlarmScheduler] to post calm, non-punishing task reminders (FR-5, AD-4, Story 5.1).
 * Routes reminders to tiered notification channels based on delivery intensity,
 * verifies runtime notification permissions, and configures distinct haptics for urgent items.
 */
class AlarmNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val alarmId = intent.getStringExtra(ExactAlarmScheduler.EXTRA_ALARM_ID) ?: return
        val title = intent.getStringExtra(ExactAlarmScheduler.EXTRA_TITLE) ?: context.getString(R.string.notification_channel_important_name)
        val message = intent.getStringExtra(ExactAlarmScheduler.EXTRA_MESSAGE) ?: context.getString(R.string.notification_fallback_message)
        val intensity = intent.getStringExtra(ExactAlarmScheduler.EXTRA_INTENSITY) ?: "ROUTINE"

        // Verify runtime notification permission on API 33+ before attempting notification delivery
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val targetChannelId = TieredNotificationManager.getChannelIdForIntensity(intensity)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isUrgent = intensity.equals("URGENT", ignoreCase = true)
        val isRoutine = intensity.equals("ROUTINE", ignoreCase = true)

        val priority = when {
            isUrgent -> NotificationCompat.PRIORITY_HIGH
            isRoutine -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notificationBuilder = NotificationCompat.Builder(context, targetChannelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isUrgent) {
            notificationBuilder
                .setVibrate(TieredNotificationManager.URGENT_VIBRATION_PATTERN)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        notificationManager.notify(alarmId.hashCode(), notificationBuilder.build())
    }
}
