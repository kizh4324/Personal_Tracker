package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.HeroActiveSession
import com.personaltracker.domain.model.HeroPriorityState
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolveHeroStateUseCaseTest {

    private lateinit var useCase: ResolveHeroStateUseCase

    @Before
    fun setUp() {
        useCase = ResolveHeroStateUseCase()
    }

    @Test
    fun testPriority1ActiveFocusTakesPrecedenceOverAllOthers() {
        val activeSession = HeroActiveSession(
            subjectTag = "Physics",
            remainingSeconds = 1200L,
            formattedRemainingTime = "20:00",
            isRunning = true,
            isPaused = false,
            isCompleted = false
        )
        val interruptedTask = Task(id = "task_int", title = "Interrupted Task", scheduledDate = "2026-10-24", state = "INTERRUPTED")
        val pendingTask = Task(id = "task_pending", title = "Pending Task", scheduledDate = "2026-10-24", state = "PENDING")
        val overdueTask = Task(id = "task_overdue", title = "Overdue Task", scheduledDate = "2026-10-23", state = "PENDING")

        val state = useCase.resolve(
            activeSession = activeSession,
            interruptedTask = interruptedTask,
            isMorningBannerDismissed = false,
            pendingTasks = listOf(pendingTask),
            overdueTasks = listOf(overdueTask)
        )

        assertTrue(state is HeroPriorityState.ActiveFocus)
        assertEquals(1, state.priority)
        assertEquals("Physics", (state as HeroPriorityState.ActiveFocus).session.subjectTag)
    }

    @Test
    fun testPriority2InterruptedTaskTakesPrecedenceOverMorningBannerAndUpcoming() {
        val interruptedTask = Task(
            id = "task_int",
            title = "Code Review",
            scheduledDate = "2026-10-24",
            state = "INTERRUPTED"
        )
        val pendingTask = Task(id = "task_pending", title = "Pending Task", scheduledDate = "2026-10-24", state = "PENDING")

        val state = useCase.resolve(
            activeSession = null,
            interruptedTask = interruptedTask,
            isMorningBannerDismissed = false,
            pendingTasks = listOf(pendingTask)
        )

        assertTrue(state is HeroPriorityState.InterruptedResumable)
        assertEquals(2, state.priority)
        val resumable = state as HeroPriorityState.InterruptedResumable
        assertEquals("Code Review", resumable.title)
        assertEquals("task_int", resumable.taskId)
    }

    @Test
    fun testInterruptedTaskIgnoredIfNotInInterruptedState() {
        val completedTask = Task(
            id = "task_done",
            title = "Completed Task",
            scheduledDate = "2026-10-24",
            state = "COMPLETED"
        )

        val state = useCase.resolve(
            activeSession = null,
            interruptedTask = completedTask,
            isMorningBannerDismissed = true,
            pendingTasks = emptyList()
        )

        // Falls through past Priority 2 since state is not INTERRUPTED
        assertTrue(state is HeroPriorityState.IntentionalIdle)
    }

    @Test
    fun testPriority2PausedSessionResolvesInterruptedResumable() {
        val pausedSession = HeroActiveSession(
            subjectTag = "Algorithms",
            remainingSeconds = 600L,
            formattedRemainingTime = "10:00",
            isRunning = false,
            isPaused = true,
            isCompleted = false
        )

        val state = useCase.resolve(
            activeSession = pausedSession,
            isMorningBannerDismissed = false
        )

        assertTrue(state is HeroPriorityState.InterruptedResumable)
        assertEquals(2, state.priority)
        val resumable = state as HeroPriorityState.InterruptedResumable
        assertEquals("Algorithms Session", resumable.title)
    }

    @Test
    fun testPriority3MorningBannerResolvesWhenNotDismissed() {
        val pendingTask = Task(id = "t1", title = "Task 1", scheduledDate = "2026-10-24", state = "PENDING")

        val state = useCase.resolve(
            isMorningBannerDismissed = false,
            activeDayTypeName = "Rest / Recovery",
            activeDayTypeId = "dt_rest",
            pendingTasks = listOf(pendingTask)
        )

        assertTrue(state is HeroPriorityState.MorningBanner)
        assertEquals(3, state.priority)
        assertEquals("Rest / Recovery", (state as HeroPriorityState.MorningBanner).dayTypeName)
    }

    @Test
    fun testPriority4UpcomingItemResolvesWhenBannerDismissed() {
        val task1 = Task(
            id = "t1",
            title = "Write Unit Tests",
            scheduledDate = "2026-10-24",
            estimatedDurationMinutes = -5, // Test clamping
            state = "PENDING"
        )

        val state = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = listOf(task1)
        )

        assertTrue(state is HeroPriorityState.UpcomingItem)
        assertEquals(4, state.priority)
        val upcoming = state as HeroPriorityState.UpcomingItem
        assertEquals("Write Unit Tests", upcoming.title)
        assertEquals(0, upcoming.startsInMinutes) // Clamped to >= 0
    }

    @Test
    fun testPriority4RoutineStepPromotesWhenNoPendingTasks() {
        val routineStep = RoutineStep(
            id = "step_morning",
            routineId = "routine_1",
            orderIndex = 1,
            title = "Morning Stretch & Meditation",
            durationMinutes = 15
        )

        val state = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = emptyList(),
            activeRoutineStep = routineStep,
            isRoutineStepCompleted = false
        )

        assertTrue(state is HeroPriorityState.UpcomingItem)
        assertEquals(4, state.priority)
        val upcoming = state as HeroPriorityState.UpcomingItem
        assertEquals("Morning Stretch & Meditation", upcoming.title)
        assertEquals(15, upcoming.startsInMinutes)
        assertEquals("step_morning", upcoming.taskId)
    }

    @Test
    fun testSnoozePromotesNextPendingTaskImmediately() {
        val task1 = Task(id = "t1", title = "Task 1", scheduledDate = "2026-10-24", state = "PENDING")
        val task2 = Task(id = "t2", title = "Task 2", scheduledDate = "2026-10-24", state = "PENDING")

        // Snoozing task 1
        val state = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = listOf(task1, task2),
            snoozedTaskIds = setOf("t1")
        )

        assertTrue(state is HeroPriorityState.UpcomingItem)
        val upcoming = state as HeroPriorityState.UpcomingItem
        assertEquals("t2", upcoming.taskId)
    }

    @Test
    fun testSnoozeAllPendingTasksTransitionsToCarryForwardOrIdle() {
        val task1 = Task(id = "t1", title = "Task 1", scheduledDate = "2026-10-24", state = "PENDING")
        val overdue = Task(id = "ov1", title = "Overdue 1", scheduledDate = "2026-10-23", state = "PENDING")

        // Snooze task 1, with overdue task present
        val stateWithOverdue = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = listOf(task1),
            overdueTasks = listOf(overdue),
            snoozedTaskIds = setOf("t1")
        )

        assertTrue(stateWithOverdue is HeroPriorityState.CarryForwardReview)
        assertEquals(5, stateWithOverdue.priority)
        assertEquals(1, (stateWithOverdue as HeroPriorityState.CarryForwardReview).itemCount)

        // Snooze task 1, without overdue tasks -> IntentionalIdle
        val stateWithoutOverdue = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = listOf(task1),
            overdueTasks = emptyList(),
            snoozedTaskIds = setOf("t1")
        )

        assertTrue(stateWithoutOverdue is HeroPriorityState.IntentionalIdle)
        assertEquals(6, stateWithoutOverdue.priority)
    }

    @Test
    fun testPriority5CarryForwardReviewWhenNoUpcomingTasks() {
        val overdue1 = Task(id = "ov1", title = "Overdue 1", scheduledDate = "2026-10-22", state = "PENDING")
        val overdue2 = Task(id = "ov2", title = "Overdue 2", scheduledDate = "2026-10-23", state = "PENDING")

        val state = useCase.resolve(
            isMorningBannerDismissed = true,
            pendingTasks = emptyList(),
            overdueTasks = listOf(overdue1, overdue2)
        )

        assertTrue(state is HeroPriorityState.CarryForwardReview)
        assertEquals(5, state.priority)
        assertEquals(2, (state as HeroPriorityState.CarryForwardReview).itemCount)
    }

    @Test
    fun testPriority6IntentionalIdleFallbackWhenAllCaughtUp() {
        val state = useCase.resolve(
            activeSession = null,
            interruptedTask = null,
            isMorningBannerDismissed = true,
            pendingTasks = emptyList(),
            overdueTasks = emptyList()
        )

        assertTrue(state is HeroPriorityState.IntentionalIdle)
        assertEquals(6, state.priority)
        assertEquals("All caught up for today. Rest well.", (state as HeroPriorityState.IntentionalIdle).message)
    }
}
