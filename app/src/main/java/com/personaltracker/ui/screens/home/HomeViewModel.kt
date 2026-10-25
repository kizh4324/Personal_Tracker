package com.personaltracker.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HeroActiveSession
import com.personaltracker.domain.model.HeroPriorityState
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.usecase.AdvanceRoutineStepResult
import com.personaltracker.domain.usecase.AdvanceRoutineStepUseCase
import com.personaltracker.domain.usecase.CarryForwardTasksUseCase
import com.personaltracker.domain.usecase.CompleteTaskUseCase
import com.personaltracker.domain.usecase.DeleteTaskUseCase
import com.personaltracker.domain.usecase.DismissOverdueTasksUseCase
import com.personaltracker.domain.usecase.EvaluateTaskUrgencyScarcityUseCase
import com.personaltracker.domain.usecase.LogHabitUseCase
import com.personaltracker.domain.usecase.RecalculateRoutineTimelineUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ResolveHeroStateUseCase
import com.personaltracker.domain.usecase.RoutineStepAction
import com.personaltracker.domain.usecase.SaveRoutineUseCase
import com.personaltracker.domain.usecase.SaveTaskUseCase
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private data class BaseHomeData(
    val tasks: List<Task>,
    val habits: List<Habit>,
    val balance: Int,
    val dayTypes: List<DayType>,
    val override: DayOverride?,
    val routines: List<Routine>
)

private data class TaskEditorControl(
    val isOpen: Boolean = false,
    val task: Task? = null
)

private data class HomeControlData(
    val timer: StudySessionTimerState,
    val bannerDismissed: Boolean,
    val selectorOpen: Boolean,
    val carryDialogOpen: Boolean,
    val completedRoutineSteps: Set<String>,
    val skippedRoutineSteps: Set<String>,
    val shiftedRoutineTime: String?
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
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val evaluateTaskUrgencyScarcityUseCase: EvaluateTaskUrgencyScarcityUseCase,
    private val advanceRoutineStepUseCase: AdvanceRoutineStepUseCase = AdvanceRoutineStepUseCase(
        routineRepository,
        economyRepository,
        GamificationLedgerEngine()
    ),
    private val recalculateRoutineTimelineUseCase: RecalculateRoutineTimelineUseCase = RecalculateRoutineTimelineUseCase(
        AdaptiveScheduleEngine()
    ),
    private val saveRoutineUseCase: SaveRoutineUseCase = SaveRoutineUseCase(routineRepository),
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<HomeUiEffect> = _uiEffect.receiveAsFlow()

    private val isMorningBannerDismissed = MutableStateFlow(false)
    private val isDayTypeSelectorOpen = MutableStateFlow(false)
    private val isCarryForwardDialogOpen = MutableStateFlow(false)
    private val taskEditorControl = MutableStateFlow(TaskEditorControl())
    private val snoozedTaskIds = MutableStateFlow<Set<String>>(emptySet())

    private val completedRoutineStepIds = MutableStateFlow<Set<String>>(emptySet())
    private val skippedRoutineStepIds = MutableStateFlow<Set<String>>(emptySet())
    private val shiftedRoutineStartTime = MutableStateFlow<String?>(null)

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
        val routinesFlow = routineRepository.getAllRoutines()
            .onStart { routineRepository.ensureDefaultRoutines() }

        val baseDataFlow = combine(
            tasksFlow,
            habitsFlow,
            balanceFlow,
            dayTypesFlow,
            overrideFlow,
            routinesFlow
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            BaseHomeData(
                tasks = args[0] as List<Task>,
                habits = args[1] as List<Habit>,
                balance = args[2] as Int,
                dayTypes = args[3] as List<DayType>,
                override = args[4] as DayOverride?,
                routines = args[5] as List<Routine>
            )
        }

        val controlFlow = combine(
            timerFlow,
            isMorningBannerDismissed,
            isDayTypeSelectorOpen,
            isCarryForwardDialogOpen,
            completedRoutineStepIds,
            skippedRoutineStepIds,
            shiftedRoutineStartTime
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            HomeControlData(
                timer = args[0] as StudySessionTimerState,
                bannerDismissed = args[1] as Boolean,
                selectorOpen = args[2] as Boolean,
                carryDialogOpen = args[3] as Boolean,
                completedRoutineSteps = args[4] as Set<String>,
                skippedRoutineSteps = args[5] as Set<String>,
                shiftedRoutineTime = args[6] as String?
            )
        }

        observeDataJob = combine(
            baseDataFlow,
            controlFlow,
            taskEditorControl,
            snoozedTaskIds
        ) { baseData, control, taskEditor, snoozedIds ->
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

            // Active Routine resolution
            val rawRoutine = baseData.routines.find { it.dayTypeId == dayTypeId }
                ?: Routine.BUILT_IN_ROUTINES.find { it.dayTypeId == dayTypeId }
                ?: Routine.ROUTINE_STANDARD_MORNING

            val activeRoutine = if (rawRoutine.steps.isNotEmpty()) {
                rawRoutine
            } else {
                val dbSteps = runCatching { routineRepository.getStepsForRoutineList(rawRoutine.id) }.getOrDefault(emptyList())
                if (dbSteps.isNotEmpty()) {
                    rawRoutine.copy(steps = dbSteps)
                } else {
                    Routine.BUILT_IN_ROUTINES.find { it.id == rawRoutine.id } ?: rawRoutine
                }
            }

            // Timeline recalculation
            val effectiveStartTime = control.shiftedRoutineTime ?: activeRoutine.targetStartTime
            val timelineResult = recalculateRoutineTimelineUseCase(
                steps = activeRoutine.steps,
                baseTargetStartTime = activeRoutine.targetStartTime,
                newStartTime = effectiveStartTime,
                completedStepIds = control.completedRoutineSteps,
                skippedStepIds = control.skippedRoutineSteps
            )

            val homeScheduledSteps = timelineResult.scheduledSteps.map { scheduled ->
                val status = when {
                    scheduled.isCompleted -> RoutineStepStatus.COMPLETED
                    scheduled.isCurrentCursor -> RoutineStepStatus.ACTIVE
                    else -> RoutineStepStatus.UPCOMING
                }
                HomeScheduledStepState(
                    step = scheduled.step,
                    timeSlot = "${scheduled.startTime} - ${scheduled.endTime}",
                    status = status,
                    isCurrentCursor = scheduled.isCurrentCursor
                )
            }

            val currentCursorStep = timelineResult.scheduledSteps.firstOrNull { it.isCurrentCursor }?.step
            val activeRoutineStep = currentCursorStep?.let { step ->
                HomeRoutineStepState(
                    step = step,
                    status = RoutineStepStatus.ACTIVE
                )
            }

            val isRoutineCompletedToday = timelineResult.scheduledSteps.isNotEmpty() &&
                timelineResult.scheduledSteps.all { it.isCompleted || it.isSkipped }

            val completedCount = timelineResult.scheduledSteps.count { it.isCompleted || it.isSkipped }
            val totalCount = timelineResult.scheduledSteps.size
            val totalMinutes = timelineResult.scheduledSteps.sumOf { it.step.durationMinutes }
            val routineProgressSummary = if (isRoutineCompletedToday) {
                "Completed • $totalCount steps ($totalMinutes min)"
            } else {
                val currentStepIndex = timelineResult.scheduledSteps.indexOfFirst { it.isCurrentCursor }
                    .takeIf { it >= 0 }?.plus(1) ?: (completedCount + 1).coerceAtMost(totalCount)
                "Step $currentStepIndex of $totalCount • $totalMinutes min"
            }

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

            // 6-state Hero Card priority resolution via pure domain use case
            val resolvedHeroPriority = resolveHeroStateUseCase.resolve(
                activeSession = heroActiveSession,
                interruptedTask = interruptedTask,
                isMorningBannerDismissed = control.bannerDismissed,
                activeDayTypeName = dayTypeName,
                activeDayTypeId = dayTypeId,
                pendingTasks = tasks,
                overdueTasks = overdueTasks,
                snoozedTaskIds = snoozedIds,
                activeRoutineStep = currentCursorStep,
                isRoutineStepCompleted = isRoutineCompletedToday
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

            // Urgency soft-scarcity evaluation
            val urgencyEvaluation = evaluateTaskUrgencyScarcityUseCase(
                tasks = tasks,
                excludingTaskId = taskEditor.task?.id
            )

            HomeUiState(
                activeDayTypeName = dayTypeName,
                activeDayTypeId = dayTypeId,
                activeDayType = resolvedDayType,
                availableDayTypes = dayTypes,
                isDayOverrideActive = isOverrideActive,
                isDayTypeSelectorOpen = control.selectorOpen,
                isCarryForwardDialogOpen = control.carryDialogOpen,
                isTaskEditorOpen = taskEditor.isOpen,
                editingTask = taskEditor.task,
                activeUrgentTaskCount = urgencyEvaluation.activeUrgentCount,
                isUrgencySoftCapReached = urgencyEvaluation.isSoftCapReached,
                streakCount = maxStreak,
                flameState = overallFlameState,
                coinBalance = balance,
                heroCardState = resolvedHeroCardState,
                focusTasks = tasks,
                overdueTasks = overdueTasks,
                habits = habitItemStates,
                activeRoutine = activeRoutine,
                activeRoutineStep = activeRoutineStep,
                routineTimelineSteps = homeScheduledSteps,
                routineProgressSummary = routineProgressSummary,
                isRoutineCompletedToday = isRoutineCompletedToday,
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
            is HomeUiIntent.SkipRoutineStep -> handleSkipRoutineStep(intent)
            is HomeUiIntent.ShiftRoutineToNow -> handleShiftRoutineToNow()
            is HomeUiIntent.ResetRoutineProgress -> handleResetRoutineProgress()
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
            is HomeUiIntent.OpenTaskEditor -> handleOpenTaskEditor(intent.task)
            is HomeUiIntent.DismissTaskEditor -> handleDismissTaskEditor()
            is HomeUiIntent.SaveTaskItem -> handleSaveTaskItem(intent.task)
            is HomeUiIntent.DeleteTaskItem -> handleDeleteTaskItem(intent.taskId)
            is HomeUiIntent.Refresh -> observeDataStreams()
        }
    }

    private fun handleOpenTaskEditor(task: Task?) {
        taskEditorControl.value = TaskEditorControl(isOpen = true, task = task)
    }

    private fun handleDismissTaskEditor() {
        taskEditorControl.value = TaskEditorControl(isOpen = false, task = null)
    }

    private fun handleSaveTaskItem(task: Task) {
        viewModelScope.launch {
            runCatching {
                saveTaskUseCase(task)
                taskEditorControl.value = TaskEditorControl(isOpen = false, task = null)
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Task saved."))
            }.onFailure { e ->
                val msg = e.message ?: "Could not save task. Please try again."
                _uiEffect.send(HomeUiEffect.ShowSnackbar(msg))
            }
        }
    }

    private fun handleDeleteTaskItem(taskId: String) {
        viewModelScope.launch {
            runCatching {
                deleteTaskUseCase(taskId)
                taskEditorControl.value = TaskEditorControl(isOpen = false, task = null)
                _uiEffect.send(HomeUiEffect.ShowSnackbar("Task removed."))
            }.onFailure { e ->
                val msg = e.message ?: "Could not delete task. Please try again."
                _uiEffect.send(HomeUiEffect.ShowSnackbar(msg))
            }
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
                val result = switchDayTypeUseCase(intent.dayTypeId, today, completedRoutineStepIds.value)
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
        val routine = _uiState.value.activeRoutine
            ?: Routine.BUILT_IN_ROUTINES.find { it.dayTypeId == _uiState.value.activeDayTypeId }
            ?: Routine.ROUTINE_STANDARD_MORNING

        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = advanceRoutineStepUseCase(
                    routineId = routine.id,
                    stepId = intent.stepId,
                    action = RoutineStepAction.COMPLETE,
                    date = today,
                    completedStepIds = completedRoutineStepIds.value,
                    skippedStepIds = skippedRoutineStepIds.value
                )
                completedRoutineStepIds.value = result.completedStepIds
                skippedRoutineStepIds.value = result.skippedStepIds

                if (result.coinsAwarded > 0) {
                    _uiEffect.send(HomeUiEffect.ShowCoinReward(intent.startOffset, result.coinsAwarded))
                    _uiEffect.send(HomeUiEffect.ShowLottieReward(RewardAnimationType.ConfettiCelebration))
                }
                _uiEffect.send(HomeUiEffect.ShowSnackbar(result.message))
            }.onFailure { e ->
                val msg = e.message ?: "Could not advance routine step. Please try again."
                _uiEffect.send(HomeUiEffect.ShowSnackbar(msg))
            }
        }
    }

    private fun handleSkipRoutineStep(intent: HomeUiIntent.SkipRoutineStep) {
        val routine = _uiState.value.activeRoutine
            ?: Routine.BUILT_IN_ROUTINES.find { it.dayTypeId == _uiState.value.activeDayTypeId }
            ?: Routine.ROUTINE_STANDARD_MORNING

        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val result = advanceRoutineStepUseCase(
                    routineId = routine.id,
                    stepId = intent.stepId,
                    action = RoutineStepAction.SKIP,
                    date = today,
                    completedStepIds = completedRoutineStepIds.value,
                    skippedStepIds = skippedRoutineStepIds.value
                )
                completedRoutineStepIds.value = result.completedStepIds
                skippedRoutineStepIds.value = result.skippedStepIds

                if (result.coinsAwarded > 0) {
                    _uiEffect.send(HomeUiEffect.ShowCoinReward(androidx.compose.ui.geometry.Offset.Zero, result.coinsAwarded))
                    _uiEffect.send(HomeUiEffect.ShowLottieReward(RewardAnimationType.ConfettiCelebration))
                }
                _uiEffect.send(HomeUiEffect.ShowSnackbar(result.message))
            }.onFailure { e ->
                val msg = e.message ?: "Could not skip routine step. Please try again."
                _uiEffect.send(HomeUiEffect.ShowSnackbar(msg))
            }
        }
    }

    private fun handleShiftRoutineToNow() {
        val nowFormatted = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        shiftedRoutineStartTime.value = nowFormatted
        viewModelScope.launch {
            _uiEffect.send(HomeUiEffect.ShowSnackbar("Routine shifted to now. Moving forward smoothly."))
        }
    }

    private fun handleResetRoutineProgress() {
        completedRoutineStepIds.value = emptySet()
        skippedRoutineStepIds.value = emptySet()
        shiftedRoutineStartTime.value = null
        viewModelScope.launch {
            _uiEffect.send(HomeUiEffect.ShowSnackbar("Routine progress reset for today."))
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
