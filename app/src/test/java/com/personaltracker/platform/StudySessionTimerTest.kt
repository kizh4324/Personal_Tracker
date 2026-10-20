package com.personaltracker.platform

import com.personaltracker.platform.timer.StudySessionTimer
import com.personaltracker.platform.timer.StudySessionTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionTimerTest {

    @Test
    fun `timer initial start initializes state correctly`() = runTest {
        val ticks = mutableListOf<StudySessionTimerState>()
        val timer = StudySessionTimer(
            scope = backgroundScope,
            onTick = { ticks.add(it) }
        )

        timer.start(
            sessionId = "session-1",
            subjectTag = "Mathematics",
            durationSeconds = 1500L
        )

        val state = timer.timerState.value
        assertEquals("session-1", state.sessionId)
        assertEquals("Mathematics", state.subjectTag)
        assertEquals(1500L, state.totalSeconds)
        assertEquals(1500L, state.remainingSeconds)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
        assertFalse(state.isCompleted)
        assertEquals("25:00", state.formattedRemainingTime)
    }

    @Test
    fun `timer countdown decrements remaining seconds over time`() = runTest {
        val timer = StudySessionTimer(scope = backgroundScope)
        timer.start("session-1", "Coding", 10L)

        advanceTimeBy(1000L)
        runCurrent()
        assertEquals(9L, timer.timerState.value.remainingSeconds)

        advanceTimeBy(3000L)
        runCurrent()
        assertEquals(6L, timer.timerState.value.remainingSeconds)
    }

    @Test
    fun `pause freezes countdown and resume continues`() = runTest {
        val timer = StudySessionTimer(scope = backgroundScope)
        timer.start("session-1", "History", 20L)

        advanceTimeBy(5000L)
        runCurrent()
        assertEquals(15L, timer.timerState.value.remainingSeconds)

        timer.pause()
        assertTrue(timer.timerState.value.isPaused)

        // Time passes while paused: remaining seconds must NOT decrement
        advanceTimeBy(10000L)
        runCurrent()
        assertEquals(15L, timer.timerState.value.remainingSeconds)
        assertTrue(timer.timerState.value.isPaused)

        // Resume: countdown continues
        timer.resume()
        assertFalse(timer.timerState.value.isPaused)

        advanceTimeBy(2000L)
        runCurrent()
        assertEquals(13L, timer.timerState.value.remainingSeconds)
    }

    @Test
    fun `stop halts countdown and updates running flag`() = runTest {
        val timer = StudySessionTimer(scope = backgroundScope)
        timer.start("session-1", "Physics", 30L)

        advanceTimeBy(10000L)
        runCurrent()
        assertEquals(20L, timer.timerState.value.remainingSeconds)

        timer.stop()
        assertFalse(timer.timerState.value.isRunning)
        assertFalse(timer.timerState.value.isPaused)

        advanceTimeBy(10000L)
        runCurrent()
        assertEquals(20L, timer.timerState.value.remainingSeconds)
    }

    @Test
    fun `reaching zero triggers completion callback and marks completed`() = runTest {
        var completedState: StudySessionTimerState? = null
        val timer = StudySessionTimer(
            scope = backgroundScope,
            onComplete = { completedState = it }
        )

        timer.start("session-1", "Biology", 3L)

        advanceTimeBy(3000L)
        runCurrent()

        val state = timer.timerState.value
        assertEquals(0L, state.remainingSeconds)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertTrue(state.isCompleted)
        assertEquals("00:00", state.formattedRemainingTime)

        assertEquals("session-1", completedState?.sessionId)
        assertTrue(completedState?.isCompleted == true)
    }
}
