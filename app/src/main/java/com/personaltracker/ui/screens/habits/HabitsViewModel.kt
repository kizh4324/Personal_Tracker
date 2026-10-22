package com.personaltracker.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.ui.components.animation.RewardAnimationType
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * Production ViewModel driving habit streak tracking and non-punitive logging (AD-1, AD-4).
 */
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val logHabitUseCase: LogHabitUseCase,
    private val streakStateMachine: StreakStateMachine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState(isLoading = true))
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HabitsUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HabitsUiEffect> = _uiEffect.receiveAsFlow()

    private var observeDataJob: Job? = null

    init {
        observeHabits()
    }

    private fun observeHabits() {
        observeDataJob?.cancel()
        val today = LocalDate.now().toString()

        observeDataJob = habitRepository.getAllHabits()
            .distinctUntilChanged()
            .onEach { habits ->
                val items = habits.map { habit ->
                    val log = habitRepository.getLogForDate(habit.id, today)
                    val isDone = log != null && log.status.equals("COMPLETED", ignoreCase = true)
                    HabitItemUiState(
                        habit = habit,
                        isCompletedToday = isDone,
                        flameState = streakStateMachine.getFlameState(habit.currentStreak)
                    )
                }

                val activeStreaks = habits.count { it.currentStreak > 0 }

                _uiState.update {
                    it.copy(
                        habits = items,
                        totalActiveStreaks = activeStreaks,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onIntent(intent: HabitsUiIntent) {
        when (intent) {
            is HabitsUiIntent.ToggleHabit -> handleToggleHabit(intent)
            is HabitsUiIntent.CreateHabit -> handleCreateHabit(intent)
            is HabitsUiIntent.Refresh -> observeHabits()
        }
    }

    private fun handleToggleHabit(intent: HabitsUiIntent.ToggleHabit) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = logHabitUseCase(intent.habitId, today, isCompleted = true)
                if (result.coinsEarned > 0) {
                    _uiEffect.send(HabitsUiEffect.ShowCoinReward(intent.startOffset, result.coinsEarned))
                    _uiEffect.send(HabitsUiEffect.ShowLottieReward(RewardAnimationType.CoinEarnMicro))
                }
            }.onFailure {
                _uiEffect.send(HabitsUiEffect.ShowSnackbar("Could not log habit. Please try again."))
            }
        }
    }

    private fun handleCreateHabit(intent: HabitsUiIntent.CreateHabit) {
        viewModelScope.launch {
            runCatching {
                val newHabit = Habit(
                    id = UUID.randomUUID().toString(),
                    name = intent.name,
                    targetDaysOfWeek = intent.cadence,
                    currentStreak = 0,
                    bestStreak = 0,
                    slackBankDays = 2,
                    isFreezeActive = false
                )
                habitRepository.saveHabit(newHabit)
            }.onFailure {
                _uiEffect.send(HabitsUiEffect.ShowSnackbar("Could not create habit."))
            }
        }
    }
}
