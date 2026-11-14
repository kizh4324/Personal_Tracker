package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.StudySession
import com.personaltracker.ui.TestStudySessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResumeInterruptedSessionUseCaseTest {

    private lateinit var studyRepository: TestStudySessionRepository
    private lateinit var rescheduleEngine: FocusRescheduleEngine
    private lateinit var useCase: ResumeInterruptedSessionUseCase

    @Before
    fun setUp() {
        studyRepository = TestStudySessionRepository()
        rescheduleEngine = FocusRescheduleEngine()
        useCase = ResumeInterruptedSessionUseCase(studyRepository, rescheduleEngine)
    }

    @Test
    fun testResumeInterruptedSessionReturnsPlanWithRemainingDuration() = runTest {
        val interrupted = StudySession(
            id = "s_interrupted_1",
            subjectTag = "Architecture",
            sessionDate = "2026-11-14",
            startTime = "10:00",
            durationMinutes = 15,
            coinsEarned = 0,
            isInterrupted = true,
            targetDurationMinutes = 45,
            actualElapsedMinutes = 15,
            intervalType = FocusIntervalType.DEEP_WORK
        )
        studyRepository.saveSession(interrupted)

        val plan = useCase()
        assertNotNull(plan)
        assertEquals("s_interrupted_1", plan!!.sessionId)
        assertEquals("Architecture", plan.subjectTag)
        assertEquals(45, plan.targetDurationMinutes)
        assertEquals(15, plan.bankedMinutes)
        assertEquals(30, plan.remainingDurationMinutes)
        assertEquals(FocusIntervalType.DEEP_WORK, plan.intervalType)
        assertTrue(plan.advisoryMessage.contains("15 min banked!"))
    }

    @Test
    fun testResumeInterruptedSessionIgnoresBreaks() = runTest {
        val breakSession = StudySession(
            id = "s_break_1",
            subjectTag = "Break",
            sessionDate = "2026-11-14",
            startTime = "10:30",
            durationMinutes = 2,
            coinsEarned = 0,
            isInterrupted = true,
            targetDurationMinutes = 5,
            actualElapsedMinutes = 2,
            intervalType = FocusIntervalType.SHORT_BREAK
        )
        studyRepository.saveSession(breakSession)

        val plan = useCase()
        assertNull(plan)
    }

    @Test
    fun testResumeInterruptedSessionPreventsZombieLoopsWhenTargetCompleted() = runTest {
        // When actualElapsedMinutes >= targetDurationMinutes, session has no remaining focus time
        val zombieSession = StudySession(
            id = "s_zombie_1",
            subjectTag = "Math",
            sessionDate = "2026-11-14",
            startTime = "09:00",
            durationMinutes = 25,
            coinsEarned = 0,
            isInterrupted = true,
            targetDurationMinutes = 25,
            actualElapsedMinutes = 25,
            intervalType = FocusIntervalType.POMODORO_WORK
        )
        studyRepository.saveSession(zombieSession)

        val plan = useCase()
        assertNull(plan)
    }

    @Test
    fun testResumeInterruptedSessionWithSpecificSessionId() = runTest {
        val session1 = StudySession(
            id = "s_1",
            subjectTag = "History",
            sessionDate = "2026-11-14",
            startTime = "08:00",
            durationMinutes = 10,
            isInterrupted = true,
            targetDurationMinutes = 30,
            actualElapsedMinutes = 10
        )
        val session2 = StudySession(
            id = "s_2",
            subjectTag = "Physics",
            sessionDate = "2026-11-14",
            startTime = "09:00",
            durationMinutes = 20,
            isInterrupted = true,
            targetDurationMinutes = 50,
            actualElapsedMinutes = 20
        )
        studyRepository.saveSession(session1)
        studyRepository.saveSession(session2)

        val plan = useCase(sessionId = "s_1")
        assertNotNull(plan)
        assertEquals("s_1", plan!!.sessionId)
        assertEquals("History", plan.subjectTag)
        assertEquals(20, plan.remainingDurationMinutes)
    }
}
