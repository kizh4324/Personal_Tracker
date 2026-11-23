package com.personaltracker.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.personaltracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Platform manager declaring, initializing, and managing tiered Android notification channels (AD-4, Story 5.1).
 * Registers channels for API 26+ (Routine [Low], Important [Default], Urgent [High + Haptics])
 * and cleanly purges the legacy monolithic channel.
 */
@Singleton
class TieredNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Idempotently creates the 3 tiered notification channels on API 26+ and purges
     * the legacy [LEGACY_CHANNEL_ID].
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Purge obsolete legacy monolithic channel
            runCatching {
                notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            }

            // 1. Routine Channel (Low importance, silent in-tray only)
            val channelRoutine = NotificationChannel(
                CHANNEL_ID_ROUTINE,
                context.getString(R.string.notification_channel_routine_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_routine_description)
                enableLights(false)
                enableVibration(false)
            }

            // 2. Important Channel (Default importance, standard notification chime + vibration)
            val channelImportant = NotificationChannel(
                CHANNEL_ID_IMPORTANT,
                context.getString(R.string.notification_channel_important_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_important_description)
            }

            // 3. Urgent Channel (High importance, heads-up banner, double-pulse companion haptics)
            val channelUrgent = NotificationChannel(
                CHANNEL_ID_URGENT,
                context.getString(R.string.notification_channel_urgent_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_urgent_description)
                enableVibration(true)
                vibrationPattern = URGENT_VIBRATION_PATTERN
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannels(
                listOf(channelRoutine, channelImportant, channelUrgent)
            )
        }
    }

    companion object {
        const val CHANNEL_ID_ROUTINE = "channel_routine"
        const val CHANNEL_ID_IMPORTANT = "channel_important"
        const val CHANNEL_ID_URGENT = "channel_urgent"
        const val LEGACY_CHANNEL_ID = "task_reminders_channel"

        /**
         * Distinct companion double-pulse haptic pattern: [delay, pulse, rest, pulse] in millis.
         */
        val URGENT_VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

        /**
         * Pure static mapping resolving delivery intensity to target channel ID.
         * Decouples broadcast receivers and background workers from Android Context.
         */
        fun getChannelIdForIntensity(intensity: String?): String {
            return when (intensity?.trim()?.uppercase()) {
                "URGENT" -> CHANNEL_ID_URGENT
                "IMPORTANT" -> CHANNEL_ID_IMPORTANT
                else -> CHANNEL_ID_ROUTINE
            }
        }
    }
}
