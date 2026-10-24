package com.personaltracker.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HeroActiveSession
import com.personaltracker.domain.model.HeroPriorityState
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.CarryForwardTasksUseCase
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.DismissOverdueTasksUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ResolveHeroStateUseCase
import com.personaltracker.domain.usecase.SwitchDayTypeUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.platform.timer.StudySessionTimerState
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.cards.HeroCardState
import com.personaltracker.ui.components.cards.RoutineStepStatus
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

private data class HomeControlData(
    val timer: StudySessionTimerState,
    val bannerDismissed: Boolean,
    val routineStatus: RoutineStepStatus,
    val selectorOpen: Boolean,
    val carryDialogOpen: Boolean
)

/**
 * Production MVI ViewModel driving the Home feed (AD-1, AD-2).
 * Combines reactive Flow repositories into a single immutable [HomeUiState]
 * and evaluates the 6-state [HeroActionCard] priority hierarchy via [ResolveHeroStateUseCase].
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
    private val resolveHeroStateUseCase: ResolveHeroStateUseCase,
    private val carryForwardTasksUseCase: CarryForwardTasksUseCase,
    private val dismissOverdueTasksUseCase: DismissOverdueTasksUseCase,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HomeUiEffect> = _uiEffect.receiveAsFlow()

    private val isMorningBannerDismissed = MutableStateFlow(false)
    private val activeRoutineStepStatus = MutableStateFlow(RoutineStepStatus.ACTIVE)
    private val isDayTypeSelectorOpen = MutableStateFlow(false)
    private val isCarryForwardDialogOpen = MutableStateFlow(false)
    private val snoozedTaskIds = MutableStateFlow<Set<String>>(emptySet())

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

        val controlFlow = combine(
            timerFlow,
            isMorningBannerDismissed,
            activeRoutineStepStatus,
            isDayTypeSelectorOpen,
            isCarryForwardDialogOpen
        ) { timer, bannerDismissed, routineStatus, selectorOpen, carryDialogOpen ->
            HomeControlData(timer, bannerDismissed, routineStatus, selectorOpen, carryDialogOpen)
        }

        observeDataJob = combine(
            baseDataFlow,
            controlFlow,
            snoozedTaskIds
        ) { baseData, control, snoozedIds ->
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

            // Active session DTO from timer
            val heroActiveSession = if (control.timer.isRunning || control.timer.isPaused) {
                HeroActiveSession(
                    subjectTag = control.timer.subjectTag,
                    remainingSeconds = control.timer.remainingSeconds,
                    formattedRemainingTime = control.timer.formattedRemainingTime,
                    isRunning = control.timer.isRunning,
                    isPaused = control.timer.isPaused,
                    isCompleted = control.timer.isCompleted
                )
            } else null

            // Active Routine Step mockup/first step
            val baseRoutineStep = RoutineStep(
                id = "default_step_1",
                routineId = "default_routine",
                orderIndex = 1,
                title = "Review daily focus plan",
                durationMinutes = 10
            )
            val activeRoutineStep = HomeRoutineStepState(
                step = baseRoutineStep,
                status = control.routineStatus
            )

            // 6-state Hero Card priority resolution via pure domain use case
            val resolvedHeroPriority = resolveHeroStateUseCase.resolve(
                activeSession = heroActiveSession,
                interruptedTask = interruptedTask,
                isMorningBannerDismissed = control.bannerDismissed,
                activeDayTypeName = dayTypeName,
                activeDayTypeId = dayTypeId,
                pendingTasks = tasks,
                overdueTasks = overdueTasks,
                snoozedTaskIds = snoozedIds
            )

            // Map domain HeroPriorityState to UI presentation HeroCardState
            val resolvedHeroCardState: HeroCardState = when (resolvedHeroPriority) {
                is HeroPriorityState.ActiveFocus -> {
                    HeroCardState.ActiveFocus(
                        title = "${resolvedHeroPriority.session.subjectTag} Session",
                        countdownFormatted = resolvedHeroPriority.session.formattedRemainingTime,
                        onPause = { onIntent(HomeUiIntent.PauseActiveFocus) },
                        onEndSession = { onIntent(HomeUiIntent.EndActiveFocus) }
                    )
                }
                is HeroPriorityState.InterruptedResumable -> {
                    HeroCardState.InterruptedResumable(
                        title = resolvedHeroPriority.title,
                        elapsedSinceFormatted = resolvedHeroPriority.elapsedSinceFormatted,
                        onResume = {
                            if (resolvedHeroPriority.taskId != null) {
                                onIntent(HomeUiIntent.ResumeInterruptedTask(resolvedHeroPriority.taskId))
                            } else {
                                onIntent(HomeUiIntent.ResumeActiveFocus)
                            }
                        },
                        onReview = {
                            viewModelScope.launch {
                                _uiEffect.send(HomeUiEffect.ShowSnackbar("Take your time. Resume whenever you're ready."))
                            }
                        },
                        onMarkComplete = {
                            if (resolvedHeroPriority.taskId != null) {
                                onIntent(HomeUiIntent.ToggleTask(resolvedHeroPriority.taskId))
                            } else {
                                onIntent(HomeUiIntent.EndActiveFocus)
                            }
                        }
                    )
                }
                is HeroPriorityState.MorningBanner -> {
                    HeroCardState.MorningBanner(
                        dayTypeName = resolvedHeroPriority.dayTypeName,
                        onSwapDayType = { onIntent(HomeUiIntent.OpenDayTypeSelector) },
                        onDismiss = { onIntent(HomeUiIntent.DismissMorningBanner) }
                    )
                }
                is HeroPriorityState.UpcomingItem -> {
                    HeroCardState.UpcomingItem(
                        title = resolvedHeroPriority.title,
                        startsInMinutes = resolvedHeroPriority.startsInMinutes,
                        onStartNow = {
                            val id = resolvedHeroPriority.taskId
                            if (id != null) {
                                if (resolvedHeroPriority.task != null) {
                                    onIntent(HomeUiIntent.StartHeroItem(id))
                                } else if (resolvedHeroPriority.activeRoutineStep != null) {
                                    onIntent(HomeUiIntent.AdvanceRoutineStep(id))
                                } else {
                                    onIntent(HomeUiIntent.StartHeroItem(id))
                                }
                            }
                        },
                        onSnooze = {
                            resolvedHeroPriority.taskId?.let { id ->
                                onIntent(HomeUiIntent.SnoozeHeroItem(id))
                            }
                        }
                    )
                }
                is HeroPriorityState.CarryForwardReview -> {
                    HeroCardState.CarryForwardReview(
                        itemCount = resolvedHeroPriority.itemCount,
                        onReviewItems = { onIntent(HomeUiIntent.OpenCarryForwardReview) }
                    )
                }
                is HeroPriorityState.IntentionalIdle -> {
                    HeroCardState.IntentionalIdle(
                        message = resolvedHeroPriority.message
                    )
                }
            }

            HomeUiState(
                activeDayTypeName = dayTypeName,
                activeDayTypeId = dayTypeId,
                activeDayType = resolvedDayType,
                availableDayTypes = dayTypes,
                isDayOverrideActive = isOverrideActive,
                isDayTypeSelectorOpen = control.selectorOpen,
                isCarryForwardDialogOpen = control.carryDialogOpen,
                streakCount = maxStreak,
                flameState = overallFlameState,
                coinBalance = balance,
                heroCardState = resolvedHeroCardState,
                focusTasks = tasks,
                overdueTasks = overdueTasks,
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
            is HomeUiIntent.ResumeActiveFocus -> handleResumeActiveFocus()
            is HomeUiIntent.EndActiveFocus -> handleEndActiveFocus()
            is HomeUiIntent.ResumeInterruptedTask -> handleResumeTask(intent)
            is HomeUiIntent.ReviewCarryForward -> isCarryForwardDialogOpen.value = true
            is HomeUiIntent.DismissMorningBanner -> isMorningBannerDismissed.value = true
            is HomeUiIntent.SwapDayType -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.DayTypeClicked -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.OpenDayTypeSelector -> isDayTypeSelectorOpen.value = true
            is HomeUiIntent.DismissDayTypeSelector -> isDayTypeSelectorOpen.value = false
            is HomeUiIntent.SelectDayType -> handleSelectDayType(intent)
            is HomeUiIntent.ResetDayTypeOverride -> handleResetDayTypeOverride()
            is HomeUiIntent.SnoozeHeroItem -> handleSnoozeHeroItem(intent)
            is HomeUiIntent.SkipHeroItem -> handleSkipHeroItem(intent)
            is HomeUiIntent.StartHeroItem -> handleStartHeroItem(intent)
            is HomeUiIntent.OpenCarryForwardReview -> isCarryForwardDialogOpen.value = true
            is HomeUiIntent.DismissCarryForwardDialog -> isCarryForwardDialogOpen.value = false
            is HomeUiIntent.CarryForwardAllOverdue -> handleCarryForwardAllOverdue()
            is HomeUiIntent.DismissAllOverdue -> handleDismissAllOverdue()
            is HomeUiIntent.Refresh -> observeDataStreams()
        }
    }

    private fun handleSnoozeHeroItem(intent: HomeUiIntent.SnoozeHeroItem) {
        snoozedTaskIds.update { it + intent.taskId }
    }

    private fun handleSkipHeroItem(intent: HomeUiIntent.SkipHeroItem) {
        snoozedTaskIds.update { it + intent.taskId }
    }

    private fun handleStartHeroItem(intent: HomeUiIntent.StartHeroItem) {
        handleToggleTask(HomeUiIntent.ToggleTask(intent.taskId, intent.startOffset))
    }

    private fun handleCarryForwardAllOverdue() {
        isCarryForwardDialogOpen.value = false
        val currentOverdue = _uiState.value.overdueTasks
        if (currentOverdue.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = carryForwardTasksUseCase(
                    targetDate = today,
                    tasks = currentOverdue
                )
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Carried forward ${result.carriedCount} tasks to today."))
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not reschedule tasks. Please try again."))
            }
        }
    }

    private fun handleDismissAllOverdue() {
        isCarryForwardDialogOpen.value = false
        val currentOverdue = _uiState.value.overdueTasks
        if (currentOverdue.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = dismissOverdueTasksUseCase(
                    targetDate = today,
                    tasks = currentOverdue
                )
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Tasks set aside. Take your time."))
            }.onFailure {
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Could not dismiss tasks. Please try again."))
            }
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

    private fun handleResumeActiveFocus() {
        context?.let { ctx ->
            StudySessionForegroundService.resumeService(ctx)
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
}
