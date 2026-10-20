package com.personaltracker.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import com.personaltracker.platform.timer.StudySessionForegroundService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by ACTION_BOOT_COMPLETED (FR-5, AD-1, AD-6).
 * Safely recovers pending task alarms and transitions any interrupted study session to isInterrupted = true.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun taskRepository(): TaskRepository
        fun studySessionRepository(): StudySessionRepository
        fun exactAlarmScheduler(): ExactAlarmScheduler
        fun transactionRunner(): TransactionRunner
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    BootEntryPoint::class.java
                )

                val prefs = appContext.getSharedPreferences(
                    StudySessionForegroundService.PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                val activeSessionId = prefs.getString(
                    StudySessionForegroundService.KEY_ACTIVE_SESSION_ID,
                    null
                )

                val coordinator = BootRecoveryCoordinator(
                    taskRepository = entryPoint.taskRepository(),
                    studySessionRepository = entryPoint.studySessionRepository(),
                    exactAlarmScheduler = entryPoint.exactAlarmScheduler(),
                    transactionRunner = entryPoint.transactionRunner()
                )

                coordinator.recover(activeSessionId)

                // Clear active session once flagged as interrupted
                if (activeSessionId != null) {
                    prefs.edit().remove(StudySessionForegroundService.KEY_ACTIVE_SESSION_ID).apply()
                }
            } catch (e: Exception) {
                // Catches exceptions, logs quietly without crashing broadcast receiver
                Log.e("BootCompletedReceiver", "Failed to complete boot recovery", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
