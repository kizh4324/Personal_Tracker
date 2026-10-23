package com.personaltracker.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.SwitchDayTypeUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus
import com.personaltracker.ui.components.cards.resolveHeroPriority
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private data class BaseHomeData(
    val tasks: List<Task>,
    val habits: List<Habit>,
    val balance: Int,
    val dayTypes: List<DayType>,
    val override: DayOverride?
)

/**
 * Production MVI ViewModel driving the Home feed (AD-1, AD-2).
 * Combines reactive Flow repositories into a single immutable [HomeUiState]
 * and evaluates the 6-state [HeroActionCard] priority hierarchy.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val routineRepository: RoutineRepository,
    private val economyRepository: EconomyRepository,
    private val dayTypeRepository: DayTypeRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val switchDayTypeUseCase: SwitchDayTypeUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val logHabitUseCase: LogHabitUseCase,
    private val streakStateMachine: StreakStateMachine,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HomeUiEffect> = _uiEffect.receiveAsFlow()

    private val isMorningBannerDismissed = MutableStateFlow(false)
    private val activeRoutineStepStatus = MutableStateFlow(RoutineStepStatus.ACTIVE)
    private val isDayTypeSelectorOpen = MutableStateFlow(false)

    private var observeDataJob: Job? = null

    init {
        observeDataStreams()
    }

    private fun observeDataStreams() {
        observeDataJob?.cancel()
        val today = LocalDate.now().toString()

        val tasksFlow = taskRepository.getTasksForDate(today)
        val habitsFlow = habitRepository.getAllHabits()
        val balanceFlow = economyRepository.observeLatestBalance()
        val dayTypesFlow = dayTypeRepository.getAllDayTypes()
        val overrideFlow = dayTypeRepository.observeOverrideForDate(today)
        val timerFlow = StudySessionForegroundService.timerState

        val baseDataFlow = combine(
            tasksFlow,
            habitsFlow,
            balanceFlow,
            dayTypesFlow,
            overrideFlow
        ) { tasks, habits, balance, dayTypes, override ->
            BaseHomeData(tasks, habits, balance, dayTypes, override)
        }

        observeDataJob = combine(
            baseDataFlow,
            timerFlow,
            isMorningBannerDismissed,
            activeRoutineStepStatus,
            isDayTypeSelectorOpen
        ) { baseData, timer, bannerDismissed, routineStatus, selectorOpen ->
            val currentDate = LocalDate.now().toString()
            val tasks = baseData.tasks
            val habits = baseData.habits
            val balance = baseData.balance
            val dayTypes = if (baseData.dayTypes.isNotEmpty()) baseData.dayTypes else DayType.BUILT_IN_DAY_TYPES
            val override = baseData.override

            val resolvedDayType = resolveDayTypeUseCase.resolve(currentDate, override, dayTypes)
            val dayTypeName = resolvedDayType.name
            val dayTypeId = resolvedDayType.id
            val isOverrideActive = override != null

            // Habits item states
            val habitItemStates = habits.map { habit ->
                val log = habitRepository.getLogForDate(habit.id, currentDate)
                val isCompleted = log != null && log.status.equals("COMPLETED", ignoreCase = true)
                HomeHabitItemState(
                    habit = habit,
                    isCompletedToday = isCompleted,
                    flameState = streakStateMachine.getFlameState(habit.currentStreak)
                )
            }

            // Overall streak (max streak among habits)
            val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
            val overallFlameState = streakStateMachine.getFlameState(maxStreak)

            // Overdue tasks for carry forward
            val overdueTasks = runCatching { taskRepository.getOverduePendingTasks(currentDate) }.getOrDefault(emptyList())

            // Interrupted task
            val interruptedTask = runCatching { taskRepository.getLatestInterruptedTask() }.getOrNull()

            // 6-state Hero Card priority resolution
            val heroCandidates = mutableListOf<HeroCardState>()

            // 1. ActiveFocus (highest priority)
            if (timer.isRunning && !timer.isCompleted) {
                heroCandidates.add(
                    HeroCardState.ActiveFocus(
                        title = "${timer.subjectTag} Session",
                        countdownFormatted = timer.formattedRemainingTime,
                        onPause = { onIntent(HomeUiIntent.PauseActiveFocus) },
                        onEndSession = { onIntent(HomeUiIntent.EndActiveFocus) }
                    )
                )
            }

            // 2. InterruptedResumable
            if (interruptedTask != null) {
                heroCandidates.add(
                    HeroCardState.InterruptedResumable(
                        title = interruptedTask.title,
                        elapsedSinceFormatted = "Paused",
                        onResume = { onIntent(HomeUiIntent.ResumeInterruptedTask(interruptedTask.id)) },
                        onReview = { onIntent(HomeUiIntent.ReviewCarryForward) },
                        onMarkComplete = { onIntent(HomeUiIntent.ToggleTask(interruptedTask.id)) }
                    )
                )
            } else if (timer.isPaused) {
                heroCandidates.add(
                    HeroCardState.InterruptedResumable(
                        title = "${timer.subjectTag} Session",
                        elapsedSinceFormatted = "Paused",
                        onResume = { onIntent(HomeUiIntent.PauseActiveFocus) },
                        onReview = {},
                        onMarkComplete = { onIntent(HomeUiIntent.EndActiveFocus) }
                    )
                )
            }

            // 3. MorningBanner
            if (!bannerDismissed) {
                heroCandidates.add(
                    HeroCardState.MorningBanner(
                        dayTypeName = dayTypeName,
                        onSwapDayType = { onIntent(HomeUiIntent.OpenDayTypeSelector) },
                        onDismiss = { onIntent(HomeUiIntent.DismissMorningBanner) }
                    )
                )
            }

            // 4. UpcomingItem
            val pendingTasks = tasks.filter { !it.state.equals("COMPLETED", ignoreCase = true) }
            val nextUpcoming = pendingTasks.firstOrNull()
            if (nextUpcoming != null) {
                heroCandidates.add(
                    HeroCardState.UpcomingItem(
                        title = nextUpcoming.title,
                        startsInMinutes = nextUpcoming.estimatedDurationMinutes,
                        onStartNow = { onIntent(HomeUiIntent.ToggleTask(nextUpcoming.id)) },
                        onSnooze = {}
                    )
                )
            }

            // 5. CarryForwardReview
            if (overdueTasks.isNotEmpty()) {
                heroCandidates.add(
                    HeroCardState.CarryForwardReview(
                        itemCount = overdueTasks.size,
                        onReviewItems = { onIntent(HomeUiIntent.ReviewCarryForward) }
                    )
                )
            }

            // 6. IntentionalIdle (fallback)
            heroCandidates.add(
                HeroCardState.IntentionalIdle("All caught up for today. Rest well.")
            )

            val resolvedHeroState = resolveHeroPriority(heroCandidates)

            // Active Routine Step mockup/first step
            val activeRoutineStep = HomeRoutineStepState(
                step = RoutineStep(
                    id = "default_step_1",
                    routineId = "default_routine",
                    orderIndex = 1,
                    title = "Review daily focus plan",
                    durationMinutes = 10
                ),
                status = routineStatus
            )

            HomeUiState(
                activeDayTypeName = dayTypeName,
                activeDayTypeId = dayTypeId,
                activeDayType = resolvedDayType,
                availableDayTypes = dayTypes,
                isDayOverrideActive = isOverrideActive,
                isDayTypeSelectorOpen = selectorOpen,
                streakCount = maxStreak,
                flameState = overallFlameState,
                coinBalance = balance,
                heroCardState = resolvedHeroState,
                focusTasks = tasks,
                habits = habitItemStates,
                activeRoutineStep = activeRoutineStep,
                isLoading = false,
                errorMessage = null
            )
        }.distinctUntilChanged()
        .onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.ToggleTask -> handleToggleTask(intent)
            is HomeUiIntent.ToggleHabit -> handleToggleHabit(intent)
            is HomeUiIntent.AdvanceRoutineStep -> handleAdvanceRoutineStep(intent)
            is HomeUiIntent.PauseActiveFocus -> handlePauseActiveFocus()
            is HomeUiIntent.EndActiveFocus -> handleEndActiveFocus()
            is HomeUiIntent.ResumeInterruptedTask -> handleResumeTask(intent)
            is HomeUiIntent.ReviewCarryForward -> handleReviewCarryForward()
            is HomeUiIntent.DismissMorningBanner -> isMorningBannerDismissed.value = true
            is HomeUiIntent.SwapDayType -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.DayTypeClicked -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.OpenDayTypeSelector -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.DismissDayTypeSelector -> isDayTypeSelectorOpen.value = false
            is HomeUiIntent.SelectDayType -> handleSelectDayType(intent)
            is HomeUiIntent.ResetDayTypeOverride -> handleResetDayTypeOverride()
            is HomeUiIntent.Refresh -> observeDataStreams()
        }
    }

    private fun handleSelectDayType(intent: HomeUiIntent.SelectDayType) {
        isDayTypeSelectorOpen.value = false
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = switchDayTypeUseCase(intent.dayTypeId, today)
                _uiEffect.send(HomeUiEffect.DayTypeChanged(result.activeDayType.name))
                _uiEffect.send(HomeUiEffect.ShowSnackbar(result.message))
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not update schedule. Please try again."))
            }
        }
    }

    private fun handleResetDayTypeOverride() {
        isDayTypeSelectorOpen.value = false
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = switchDayTypeUseCase.resetOverride(today)
                _uiEffect.send(HomeUiEffect.DayTypeChanged(result.activeDayType.name))
                _uiEffect.send(HomeUiEffect.ShowSnackbar(result.message))
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not reset schedule. Please try again."))
            }
        }
    }

    private fun handleToggleTask(intent: HomeUiIntent.ToggleTask) {
        viewModelScope.launch {
            runCatching {
                val result = completeTaskUseCase(intent.taskId)
                if (result.coinReward > 0) {
                    _uiEffect.send(HomeUiEffect.ShowCoinReward(intent.startOffset, result.coinReward))
                    _uiEffect.send(HomeUiEffect.ShowLottieReward(RewardAnimationType.CoinEarnMicro))
                }
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not update task. Please try again."))
            }
        }
    }

    private fun handleToggleHabit(intent: HomeUiIntent.ToggleHabit) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = logHabitUseCase(intent.habitId, today, isCompleted = true)
                if (result.coinsEarned > 0) {
                    _uiEffect.send(HomeUiEffect.ShowCoinReward(intent.startOffset, result.coinsEarned))
                    _uiEffect.send(HomeUiEffect.ShowLottieReward(RewardAnimationType.CoinEarnMicro))
                }
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not update habit. Please try again."))
            }
        }
    }

    private fun handleAdvanceRoutineStep(intent: HomeUiIntent.AdvanceRoutineStep) {
        activeRoutineStepStatus.update { current ->
            if (current == RoutineStepStatus.ACTIVE) RoutineStepStatus.COMPLETED else RoutineStepStatus.ACTIVE
        }
    }

    private fun handlePauseActiveFocus() {
        context?.let { ctx ->
            StudySessionForegroundService.pauseService(ctx)
        }
    }

    private fun handleEndActiveFocus() {
        context?.let { ctx ->
            StudySessionForegroundService.stopService(ctx)
        }
    }

    private fun handleResumeTask(intent: HomeUiIntent.ResumeInterruptedTask) {
        viewModelScope.launch {
            runCatching {
                val task = taskRepository.getTaskById(intent.taskId)
                if (task != null) {
                    taskRepository.updateTask(task.copy(state = "IN_PROGRESS"))
                }
            }
        }
    }

    private fun handleReviewCarryForward() {
        viewModelScope.launch {
            _uiEffect.send(HomeUiEffect.NavigateTo(PersonalTrackerDestination.HOME))
        }
    }
}
