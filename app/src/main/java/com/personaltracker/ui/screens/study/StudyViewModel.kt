package com.personaltracker.ui.screens.study

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Production ViewModel driving the Study focus timer and session records (AD-1, AD-2).
 * Synchronizes with [StudySessionForegroundService.timerState] across background/foreground boundaries.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studySessionRepository: StudySessionRepository,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState(isLoading = true))
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<StudyUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<StudyUiEffect> = _uiEffect.receiveAsFlow()

    private var previousTimerState: StudySessionTimerState = StudySessionTimerState()
    private var observeTimerJob: Job? = null
    private var observeSessionsJob: Job? = null

    init {
        observeTimerAndSessions()
    }

    private fun observeTimerAndSessions() {
        observeTimerJob?.cancel()
        observeSessionsJob?.cancel()
        val today = LocalDate.now().toString()

        // 1. Observe Foreground Service timer state
        observeTimerJob = StudySessionForegroundService.timerState
            .onEach { timer ->
                // Check if session completed
                if (timer.isCompleted && !previousTimerState.isCompleted) {
                    val earnedCoins = maxOf(1, (timer.totalSeconds / 300).toInt())
                    _uiEffect.send(StudyUiEffect.ShowSessionCompleteCelebration(earnedCoins))
                    _uiEffect.send(StudyUiEffect.ShowLottieReward(RewardAnimationType.MilestoneFullReveal))
                }
                previousTimerState = timer

                _uiState.update {
                    it.copy(
                        timerState = timer,
                        isLoading = false
                    )
                }
            }.launchIn(viewModelScope)

        // 2. Observe recent sessions from repository
        observeSessionsJob = studySessionRepository.getRecentSessions()
            .distinctUntilChanged()
            .onEach { sessions ->
                val todayMinutes = sessions
                    .filter { it.sessionDate == today && !it.isInterrupted }
                    .sumOf { it.durationMinutes }

                _uiState.update {
                    it.copy(
                        recentSessions = sessions,
                        todayTotalStudyMinutes = todayMinutes
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onIntent(intent: StudyUiIntent) {
        when (intent) {
            is StudyUiIntent.StartSession -> handleStartSession(intent.subjectTag, intent.durationMinutes)
            is StudyUiIntent.PauseSession -> handlePauseSession()
            is StudyUiIntent.ResumeSession -> handleResumeSession()
            is StudyUiIntent.StopSession -> handleStopSession()
            is StudyUiIntent.SelectSubjectTag -> _uiState.update { it.copy(selectedSubjectTag = intent.tag) }
            is StudyUiIntent.SelectDuration -> _uiState.update { it.copy(selectedDurationMinutes = intent.minutes) }
            is StudyUiIntent.Refresh -> observeTimerAndSessions()
        }
    }

    private fun handleStartSession(subjectTag: String, durationMinutes: Int) {
        val ctx = context ?: return
        val durationSeconds = durationMinutes * 60L
        val sessionId = UUID.randomUUID().toString()
        StudySessionForegroundService.startService(
            context = ctx,
            sessionId = sessionId,
            subjectTag = subjectTag,
            durationSeconds = durationSeconds
        )
    }

    private fun handlePauseSession() {
        val ctx = context ?: return
        StudySessionForegroundService.pauseService(ctx)
    }

    private fun handleResumeSession() {
        val ctx = context ?: return
        StudySessionForegroundService.resumeService(ctx)
    }

    private fun handleStopSession() {
        val ctx = context ?: return
        StudySessionForegroundService.stopService(ctx)
    }
}
