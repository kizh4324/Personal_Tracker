package com.personaltracker.platform.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Immutable state model representing a study/focus session timer countdown.
 */
data class StudySessionTimerState(
    val sessionId: String? = null,
    val subjectTag: String = "Focus",
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false
) {
    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}

/**
 * Deterministic countdown timer controller for study and focus sessions (FR-4.1, AD-1).
 * Supports pausing, resuming, stopping, and completion callbacks.
 */
class StudySessionTimer(
    private val scope: CoroutineScope,
    private val onTick: (StudySessionTimerState) -> Unit = {},
    private val onComplete: (StudySessionTimerState) -> Unit = {}
) {
    private val _timerState = MutableStateFlow(StudySessionTimerState())
    val timerState: StateFlow<StudySessionTimerState> = _timerState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Starts a new countdown session.
     */
    fun start(sessionId: String, subjectTag: String, durationSeconds: Long) {
        require(durationSeconds > 0) { "durationSeconds must be greater than 0: $durationSeconds" }
        countdownJob?.cancel()

        val newState = StudySessionTimerState(
            sessionId = sessionId,
            subjectTag = subjectTag.ifBlank { "Focus" },
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            isRunning = true,
            isPaused = false,
            isCompleted = false
        )
        _timerState.value = newState
        onTick(newState)
        launchCountdown()
    }

    /**
     * Pauses the active countdown.
     */
    fun pause() {
        val current = _timerState.value
        if (!current.isRunning || current.isPaused || current.isCompleted) return

        countdownJob?.cancel()
        val pausedState = current.copy(isPaused = true)
        _timerState.value = pausedState
        onTick(pausedState)
    }

    /**
     * Resumes the paused countdown.
     */
    fun resume() {
        val current = _timerState.value
        if (!current.isRunning || !current.isPaused || current.isCompleted) return

        val resumedState = current.copy(isPaused = false)
        _timerState.value = resumedState
        onTick(resumedState)
        launchCountdown()
    }

    /**
     * Stops the active session prematurely.
     */
    fun stop() {
        countdownJob?.cancel()
        val stoppedState = _timerState.value.copy(
            isRunning = false,
            isPaused = false
        )
        _timerState.value = stoppedState
        onTick(stoppedState)
    }

    private fun launchCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (_timerState.value.isRunning && !_timerState.value.isPaused && _timerState.value.remainingSeconds > 0) {
                delay(1000L)
                val currentRemaining = _timerState.value.remainingSeconds
                val nextRemaining = (currentRemaining - 1).coerceAtLeast(0L)

                if (nextRemaining == 0L) {
                    val completedState = _timerState.value.copy(
                        remainingSeconds = 0L,
                        isRunning = false,
                        isPaused = false,
                        isCompleted = true
                    )
                    _timerState.value = completedState
                    onTick(completedState)
                    onComplete(completedState)
                    break
                } else {
                    val updatedState = _timerState.value.copy(remainingSeconds = nextRemaining)
                    _timerState.value = updatedState
                    onTick(updatedState)
                }
            }
        }
    }
}
