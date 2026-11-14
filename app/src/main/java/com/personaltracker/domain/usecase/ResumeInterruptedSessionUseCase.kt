package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.FocusRescheduleEngine.ResumptionPlan
import com.personaltracker.domain.repository.StudySessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Pure Kotlin use case for planning study session resumption from persistent storage
 * with remaining duration and non-punitive zero-guilt microcopy (Story 3.4, AD-1, AD-3, AD-4).
 */
class ResumeInterruptedSessionUseCase @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    private val focusRescheduleEngine: FocusRescheduleEngine
) {
    suspend operator fun invoke(
        sessionId: String? = null,
        fallbackSubjectTag: String = "Focus",
        fallbackDurationMinutes: Int = 25
    ): ResumptionPlan? {
        val sessions = studySessionRepository.getAllSessions().first()

        val session = if (sessionId != null) {
            sessions.find { it.id == sessionId && it.isInterrupted }
        } else {
            // Find most recent interrupted non-break session that has remaining focus time (prevents zombie loops)
            sessions.firstOrNull {
                it.isInterrupted && !it.isBreak && it.actualElapsedMinutes < it.targetDurationMinutes
            }
        } ?: return null

        val banked = if (session.actualElapsedMinutes > 0) session.actualElapsedMinutes else session.durationMinutes
        val target = if (session.targetDurationMinutes > 0) session.targetDurationMinutes else fallbackDurationMinutes
        val remaining = maxOf(1, target - banked)

        return ResumptionPlan(
            sessionId = session.id,
            subjectTag = session.subjectTag,
            targetDurationMinutes = target,
            bankedMinutes = banked,
            remainingDurationMinutes = remaining,
            intervalType = session.intervalType,
            advisoryMessage = focusRescheduleEngine.formatResumptionPrompt(bankedMinutes = banked, remainingMinutes = remaining)
        )
    }
}
