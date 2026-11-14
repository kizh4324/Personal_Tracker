package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.FocusRescheduleEngine.InterruptionReason
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.repository.StudySessionRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class PauseOrInterruptResult(
    val sessionId: String,
    val bankedMinutes: Int,
    val remainingMinutes: Int,
    val isInterrupted: Boolean,
    val advisoryMessage: String
)

/**
 * Pure Kotlin use case responsible for crediting partial focus minutes into repository
 * with non-punitive zero-guilt microcopy upon interruption (Story 3.4, AD-1, AD-3, AD-4).
 */
class PauseOrInterruptSessionUseCase @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val focusRescheduleEngine: FocusRescheduleEngine
) {
    suspend operator fun invoke(
        sessionId: String,
        elapsedSeconds: Long,
        totalSeconds: Long,
        subjectTag: String = "Focus",
        intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK,
        reason: InterruptionReason = InterruptionReason.USER_PAUSE
    ): PauseOrInterruptResult {
        val totalMinutes = (totalSeconds / 60).toInt().coerceAtLeast(1)
        val summary = focusRescheduleEngine.calculateInterruptionSummary(
            plannedDurationMinutes = totalMinutes,
            elapsedSeconds = elapsedSeconds,
            reason = reason
        )

        val today = LocalDate.now().toString()
        val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val session = StudySession(
            id = sessionId,
            subjectTag = subjectTag,
            sessionDate = today,
            startTime = timeNow,
            durationMinutes = summary.elapsedMinutes,
            coinsEarned = 0,
            linkedTaskId = null,
            isInterrupted = true,
            targetDurationMinutes = totalMinutes,
            actualElapsedMinutes = summary.elapsedMinutes,
            intervalType = intervalType
        )

        studySessionRepository.saveSession(session)

        return PauseOrInterruptResult(
            sessionId = sessionId,
            bankedMinutes = summary.elapsedMinutes,
            remainingMinutes = summary.remainingMinutes,
            isInterrupted = true,
            advisoryMessage = summary.advisoryMessage
        )
    }
}
