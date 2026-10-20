package com.personaltracker.platform.receiver

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

/**
 * Coordinator for recovering platform state after device reboots (FR-5, AD-1, AD-6).
 * Reschedules pending task alarms and marks any active focus session as interrupted.
 */
class BootRecoveryCoordinator(
    private val taskRepository: TaskRepository,
    private val studySessionRepository: StudySessionRepository,
    private val exactAlarmScheduler: ExactAlarmScheduler,
    private val transactionRunner: TransactionRunner
) {

    suspend fun recover(
        activeSessionId: String?,
        currentDate: String = LocalDate.now().toString()
    ) {
        transactionRunner {
            // 1. Mark unfinalized study session as interrupted
            if (!activeSessionId.isNullOrBlank()) {
                val recentSessions = studySessionRepository.getRecentSessions(10).firstOrNull() ?: emptyList()
                val activeSession = recentSessions.find { it.id == activeSessionId }
                if (activeSession != null && !activeSession.isInterrupted) {
                    studySessionRepository.saveSession(activeSession.copy(isInterrupted = true))
                }
            }

            // 2. Reschedule alarms for pending tasks today
            val tasksToday = taskRepository.getTasksForDate(currentDate).firstOrNull() ?: emptyList()
            for (task in tasksToday) {
                if (task.state == "PENDING" && task.scheduledTime != null) {
                    exactAlarmScheduler.scheduleForTask(task)
                }
            }
        }
    }
}
