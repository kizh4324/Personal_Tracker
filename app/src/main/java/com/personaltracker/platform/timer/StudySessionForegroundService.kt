package com.personaltracker.platform.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.personaltracker.MainActivity
import com.personaltracker.R
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.repository.StudySessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * Foreground Service hosting lock-screen countdown timers with pause/resume actions (FR-4.1, AD-1, AD-4).
 * Enforces zero-guilt microcopy and safe session recovery.
 */
@AndroidEntryPoint
class StudySessionForegroundService : Service() {

    @Inject
    lateinit var studySessionRepository: StudySessionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timer: StudySessionTimer? = null

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: UUID.randomUUID().toString()
                val subjectTag = intent.getStringExtra(EXTRA_SUBJECT_TAG) ?: "Focus"
                val durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 25 * 60L)
                val intervalTypeName = intent.getStringExtra(EXTRA_INTERVAL_TYPE)
                val intervalType = intervalTypeName?.let {
                    runCatching { FocusIntervalType.valueOf(it) }.getOrDefault(FocusIntervalType.DEEP_WORK)
                } ?: FocusIntervalType.DEEP_WORK
                startSession(sessionId, subjectTag, durationSeconds, intervalType)
            }
            ACTION_PAUSE -> {
                timer?.pause()
            }
            ACTION_RESUME -> {
                timer?.resume()
            }
            ACTION_STOP -> {
                stopSession(interrupted = true)
            }
        }

        return START_NOT_STICKY
    }

    private fun startSession(
        sessionId: String,
        subjectTag: String,
        durationSeconds: Long,
        intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK
    ) {
        // Save active session ID in SharedPreferences for reboot recovery
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_SESSION_ID, sessionId).apply()

        val today = LocalDate.now().toString()
        val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val durationMinutes = (durationSeconds / 60).toInt().coerceAtLeast(1)

        val initialSession = StudySession(
            id = sessionId,
            subjectTag = subjectTag,
            sessionDate = today,
            startTime = timeNow,
            durationMinutes = durationMinutes,
            coinsEarned = 0,
            linkedTaskId = null,
            isInterrupted = false,
            intervalType = intervalType
        )

        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                studySessionRepository.saveSession(initialSession)
            }
        }

        timer = StudySessionTimer(
            scope = serviceScope,
            onTick = { state ->
                _timerState.value = state
                updateNotification(state)
            },
            onComplete = { state ->
                _timerState.value = state
                onSessionCompleted(state, initialSession)
            }
        )

        val initialNotification = buildNotification(
            StudySessionTimerState(
                sessionId = sessionId,
                subjectTag = subjectTag,
                totalSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
                isRunning = true,
                isPaused = false,
                intervalType = intervalType
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, initialNotification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        timer?.start(sessionId, subjectTag, durationSeconds, intervalType)
    }

    private fun onSessionCompleted(state: StudySessionTimerState, baseSession: StudySession) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ACTIVE_SESSION_ID).apply()

        val earnedCoins = maxOf(1, baseSession.durationMinutes / 5)
        val completedSession = baseSession.copy(
            coinsEarned = earnedCoins,
            isInterrupted = false
        )

        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                studySessionRepository.recordSessionWithReward(
                    session = completedSession,
                    idempotencyKey = "STUDY_COMP_${baseSession.id}"
                )
            }.onFailure {
                studySessionRepository.saveSession(completedSession)
            }
        }

        val completionNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${state.subjectTag} Session Complete")
            .setContentText("Great job staying focused! Well done.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, completionNotification)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun stopSession(interrupted: Boolean) {
        val currentState = _timerState.value
        val sessionId = currentState.sessionId

        timer?.stop()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ACTIVE_SESSION_ID).apply()

        if (interrupted && sessionId != null) {
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val today = LocalDate.now().toString()
                    val session = StudySession(
                        id = sessionId,
                        subjectTag = currentState.subjectTag,
                        sessionDate = today,
                        startTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        durationMinutes = ((currentState.totalSeconds - currentState.remainingSeconds) / 60).toInt(),
                        coinsEarned = 0,
                        isInterrupted = true,
                        intervalType = currentState.intervalType
                    )
                    studySessionRepository.saveSession(session)
                }
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSession(interrupted = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(state: StudySessionTimerState) {
        val notification = buildNotification(state)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(state: StudySessionTimerState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, StudySessionForegroundService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, StudySessionForegroundService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, StudySessionForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${state.subjectTag} Focus Session"
        val body = when {
            state.isPaused -> "Focus paused (${state.formattedRemainingTime} left) • Ready whenever you are"
            state.isCompleted -> "Session complete! Great focus."
            else -> "${state.formattedRemainingTime} remaining • You're doing great"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(state.isRunning && !state.isCompleted)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)

        if (state.isRunning && !state.isCompleted) {
            if (state.isPaused) {
                builder.addAction(0, "Resume", resumeIntent)
            } else {
                builder.addAction(0, "Pause", pauseIntent)
            }
            builder.addAction(0, "End", stopIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live study focus countdown timer on lock screen"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.personaltracker.action.START_STUDY_SESSION"
        const val ACTION_PAUSE = "com.personaltracker.action.PAUSE_STUDY_SESSION"
        const val ACTION_RESUME = "com.personaltracker.action.RESUME_STUDY_SESSION"
        const val ACTION_STOP = "com.personaltracker.action.STOP_STUDY_SESSION"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SUBJECT_TAG = "extra_subject_tag"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_INTERVAL_TYPE = "extra_interval_type"

        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "study_session_channel"
        const val CHANNEL_NAME = "Study Focus Timer"

        const val PREFS_NAME = "study_session_prefs"
        const val KEY_ACTIVE_SESSION_ID = "active_session_id"

        private val _timerState = MutableStateFlow(StudySessionTimerState())
        val timerState: StateFlow<StudySessionTimerState> = _timerState.asStateFlow()

        fun startService(
            context: Context,
            sessionId: String,
            subjectTag: String,
            durationSeconds: Long,
            intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK
        ) {
            val intent = Intent(context, StudySessionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_SUBJECT_TAG, subjectTag)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_INTERVAL_TYPE, intervalType.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, StudySessionForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, StudySessionForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StudySessionForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
