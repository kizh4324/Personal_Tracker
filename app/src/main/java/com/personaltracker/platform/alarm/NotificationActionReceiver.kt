package com.personaltracker.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Internal BroadcastReceiver handling notification actions ("Got it", "Mark Done") and swipe-dismiss
 * events to halt escalation or complete tasks without launching UI (FR-5.4, FR-5.5, Story 5.2).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionEntryPoint {
        fun escalationAlarmCoordinator(): EscalationAlarmCoordinator
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_ACKNOWLEDGE_TASK && action != ACTION_COMPLETE_TASK) return

        val taskId = intent.getStringExtra(ExactAlarmScheduler.EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(RECEIVER_TIMEOUT_MS) {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        appContext,
                        NotificationActionEntryPoint::class.java
                    )
                    val outcome = entryPoint.escalationAlarmCoordinator()
                        .onNotificationAction(action = action, taskId = taskId)

                    when (outcome) {
                        is NotificationActionOutcome.Acknowledged -> {
                            NotificationManagerCompat.from(appContext).cancel(outcome.notificationId)
                        }
                        is NotificationActionOutcome.Completed -> {
                            NotificationManagerCompat.from(appContext).cancel(outcome.notificationId)
                        }
                        is NotificationActionOutcome.Ignored -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionRx"
        private const val RECEIVER_TIMEOUT_MS = 8_000L

        const val ACTION_ACKNOWLEDGE_TASK = "com.personaltracker.action.ACKNOWLEDGE_TASK"
        const val ACTION_COMPLETE_TASK = "com.personaltracker.action.COMPLETE_TASK"
    }
}
