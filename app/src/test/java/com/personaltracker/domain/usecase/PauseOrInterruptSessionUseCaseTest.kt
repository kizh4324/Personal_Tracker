package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.FocusRescheduleEngine.InterruptionReason
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.ui.TestStudySessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PauseOrInterruptSessionUseCaseTest {

    private lateinit var studyRepository: TestStudySessionRepository
    private lateinit var rescheduleEngine: FocusRescheduleEngine
    private lateinit var useCase: PauseOrInterruptSessionUseCase

    @Before
    fun setUp() {
        studyRepository = TestStudySessionRepository()
        rescheduleEngine = FocusRescheduleEngine()
        useCase = PauseOrInterruptSessionUseCase(studyRepository, rescheduleEngine)
    }

    @Test
    fun testPauseOrInterruptSavesInterruptedSessionWithBankedMinutes() = runTest {
        val result = useCase(
            sessionId = "session_123",
            elapsedSeconds = 18 * 60L,
            totalSeconds = 25 * 60L,
            subjectTag = "Kotlin Deep Work",
            intervalType = FocusIntervalType.DEEP_WORK,
            reason = InterruptionReason.USER_PAUSE
        )

        assertEquals("session_123", result.sessionId)
        assertEquals(18, result.bankedMinutes)
        assertEquals(7, result.remainingMinutes)
        assertTrue(result.isInterrupted)
        assertTrue(result.advisoryMessage.contains("18 min banked!"))

        // Verify repository persistence
        val savedSessions = studyRepository.getAllSessions().first()
        assertEquals(1, savedSessions.size)
        val session = savedSessions.first()
        assertEquals("session_123", session.id)
        assertEquals("Kotlin Deep Work", session.subjectTag)
        assertEquals(18, session.actualElapsedMinutes)
        assertEquals(25, session.targetDurationMinutes)
        assertEquals(18, session.durationMinutes)
        assertTrue(session.isInterrupted)
        assertEquals(FocusIntervalType.DEEP_WORK, session.intervalType)
    }

    @Test
    fun testPauseOrInterruptPhoneCallReasonProvidesCallSpecificCopy() = runTest {
        val result = useCase(
            sessionId = "session_call",
            elapsedSeconds = 12 * 60L,
            totalSeconds = 30 * 60L,
            reason = InterruptionReason.PHONE_CALL
        )

        assertEquals(12, result.bankedMinutes)
        assertEquals(18, result.remainingMinutes)
        assertTrue(result.advisoryMessage.contains("Call handled"))
        assertTrue(result.advisoryMessage.contains("12 min banked"))
    }

    @Test
    fun testPauseOrInterruptDistractionBreakProvidesBreakCopy() = runTest {
        val result = useCase(
            sessionId = "session_overlay",
            elapsedSeconds = 15 * 60L,
            totalSeconds = 25 * 60L,
            reason = InterruptionReason.DISTRACTION_OVERLAY_BREAK
        )

        assertEquals(15, result.bankedMinutes)
        assertEquals(10, result.remainingMinutes)
        assertTrue(result.advisoryMessage.contains("Resting for 5 min"))
        assertTrue(result.advisoryMessage.contains("15 min banked"))
    }

    @Test
    fun testPauseOrInterruptEndEarlyProvidesEndEarlyCopy() = runTest {
        val result = useCase(
            sessionId = "session_end_early",
            elapsedSeconds = 20 * 60L,
            totalSeconds = 45 * 60L,
            reason = InterruptionReason.USER_END_EARLY
        )

        assertEquals(20, result.bankedMinutes)
        assertEquals(25, result.remainingMinutes)
        assertTrue(result.advisoryMessage.contains("Session ended calmly"))
        assertTrue(result.advisoryMessage.contains("20 min banked"))
    }
}
