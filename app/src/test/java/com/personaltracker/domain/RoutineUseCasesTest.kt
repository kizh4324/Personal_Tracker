package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.ui.TestEconomyRepository
import com.personaltracker.ui.TestRoutineRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class RoutineUseCasesTest {

    private lateinit var routineRepository: TestRoutineRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var scheduleEngine: AdaptiveScheduleEngine

    private lateinit var advanceRoutineStepUseCase: AdvanceRoutineStepUseCase
    private lateinit var recalculateRoutineTimelineUseCase: RecalculateRoutineTimelineUseCase
    private lateinit var saveRoutineUseCase: SaveRoutineUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        routineRepository = TestRoutineRepository()
        economyRepository = TestEconomyRepository()
        ledgerEngine = GamificationLedgerEngine()
        scheduleEngine = AdaptiveScheduleEngine()

        advanceRoutineStepUseCase = AdvanceRoutineStepUseCase(
            routineRepository = routineRepository,
            economyRepository = economyRepository,
            ledgerEngine = ledgerEngine
        )
        recalculateRoutineTimelineUseCase = RecalculateRoutineTimelineUseCase(
            adaptiveScheduleEngine = scheduleEngine
        )
        saveRoutineUseCase = SaveRoutineUseCase(
            routineRepository = routineRepository
        )

        // Seed initial companion profile
        economyRepository.profileFlow.value = CompanionProfile(
            id = 1,
            speciesName = "Gryph",
            level = 1,
            currentEnergyUnits = 10,
            currentExpressionOrdinal = CompanionMood.IDLE_CONTENT.ordinalValue
        )
    }

    @Test
    fun testAdvanceRoutineStepAdvancesToNextSequentialStep() = runTest {
        val routine = Routine.ROUTINE_STANDARD_MORNING
        routineRepository.saveRoutineWithSteps(routine, routine.steps)

        val result1 = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_1",
            action = RoutineStepAction.COMPLETE,
            date = today
        )

        assertFalse(result1.isRoutineCompleted)
        assertEquals("step_std_2", result1.nextStep?.id)
        assertEquals(0, result1.coinsAwarded)
        assertTrue(result1.completedStepIds.contains("step_std_1"))
        assertFalse(result1.skippedStepIds.contains("step_std_1"))
        assertTrue(result1.message.contains("completed"))
    }

    @Test
    fun testSkipRoutineStepAdvancesToNextSequentialStepWithoutCompleting() = runTest {
        val routine = Routine.ROUTINE_STANDARD_MORNING
        routineRepository.saveRoutineWithSteps(routine, routine.steps)

        val result = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_1",
            action = RoutineStepAction.SKIP,
            date = today
        )

        assertFalse(result.isRoutineCompleted)
        assertEquals("step_std_2", result.nextStep?.id)
        assertEquals(0, result.coinsAwarded)
        assertTrue(result.skippedStepIds.contains("step_std_1"))
        assertFalse(result.completedStepIds.contains("step_std_1"))
        assertTrue(result.message.contains("skipped"))
    }

    @Test
    fun testCompletingFinalStepAwards15CoinsAndUpdatesCompanionCelebrating() = runTest {
        val routine = Routine.ROUTINE_STANDARD_MORNING
        routineRepository.saveRoutineWithSteps(routine, routine.steps)

        val initialBalance = economyRepository.getLatestBalance()
        val initialProfile = economyRepository.getCompanionProfile()!!

        // Advance step 1
        val r1 = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_1",
            action = RoutineStepAction.COMPLETE,
            date = today
        )

        // Advance step 2
        val r2 = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_2",
            action = RoutineStepAction.COMPLETE,
            date = today,
            completedStepIds = r1.completedStepIds,
            skippedStepIds = r1.skippedStepIds
        )

        // Advance final step 3
        val r3 = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_3",
            action = RoutineStepAction.COMPLETE,
            date = today,
            completedStepIds = r2.completedStepIds,
            skippedStepIds = r2.skippedStepIds
        )

        assertTrue(r3.isRoutineCompleted)
        assertNull(r3.nextStep)
        assertEquals(15, r3.coinsAwarded)
        assertEquals("ROUTINE_COMP_${routine.id}_$today", r3.idempotencyKey)
        assertEquals(initialBalance + 15, economyRepository.getLatestBalance())

        val tx = economyRepository.getTransactionByIdempotencyKey(r3.idempotencyKey!!)
        assertNotNull(tx)
        assertEquals("ROUTINE_COMPLETION", tx?.eventType)
        assertEquals(15, tx?.deltaAmount)
        assertEquals(routine.id, tx?.sourceEntityId)

        val updatedProfile = economyRepository.getCompanionProfile()
        assertNotNull(updatedProfile)
        assertEquals(initialProfile.currentEnergyUnits + 15, updatedProfile?.currentEnergyUnits)
        assertEquals(CompanionMood.CELEBRATING.ordinalValue, updatedProfile?.currentExpressionOrdinal)
    }

    @Test
    fun testRoutineCompletionIsIdempotentOnSameDate() = runTest {
        val routine = Routine.ROUTINE_STANDARD_MORNING
        routineRepository.saveRoutineWithSteps(routine, routine.steps)

        // First completion
        val rFirst = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_3",
            action = RoutineStepAction.COMPLETE,
            date = today,
            completedStepIds = setOf("step_std_1", "step_std_2")
        )
        assertTrue(rFirst.isRoutineCompleted)
        assertEquals(15, rFirst.coinsAwarded)

        // Second completion on same date (e.g. replay or duplicate event)
        val rSecond = advanceRoutineStepUseCase(
            routineId = routine.id,
            stepId = "step_std_3",
            action = RoutineStepAction.COMPLETE,
            date = today,
            completedStepIds = setOf("step_std_1", "step_std_2")
        )
        assertTrue(rSecond.isRoutineCompleted)
        assertEquals(0, rSecond.coinsAwarded)
        assertEquals("Routine completed for today.", rSecond.message)
    }

    @Test
    fun testRecalculateRoutineTimelineShiftsUncompletedStepsToNow() {
        val steps = listOf(
            RoutineStep(id = "s1", routineId = "r1", orderIndex = 1, title = "Step 1", durationMinutes = 10),
            RoutineStep(id = "s2", routineId = "r1", orderIndex = 2, title = "Step 2", durationMinutes = 15),
            RoutineStep(id = "s3", routineId = "r1", orderIndex = 3, title = "Step 3", durationMinutes = 20)
        )

        val result = recalculateRoutineTimelineUseCase(
            steps = steps,
            baseTargetStartTime = "08:00",
            newStartTime = "14:30",
            completedStepIds = setOf("s1")
        )

        assertEquals(3, result.scheduledSteps.size)
        assertEquals("Routine shifted to now. Moving forward smoothly.", result.message)

        val step1 = result.scheduledSteps[0]
        val step2 = result.scheduledSteps[1]
        val step3 = result.scheduledSteps[2]

        // Completed step 1 keeps its original timeline
        assertTrue(step1.isCompleted)
        assertEquals("08:00", step1.startTime)
        assertEquals("08:10", step1.endTime)
        assertFalse(step1.isCurrentCursor)

        // Uncompleted step 2 shifts to 14:30 and is current cursor
        assertFalse(step2.isCompleted)
        assertEquals("14:30", step2.startTime)
        assertEquals("14:45", step2.endTime)
        assertTrue(step2.isCurrentCursor)

        // Uncompleted step 3 follows step 2 sequentially
        assertFalse(step3.isCompleted)
        assertEquals("14:45", step3.startTime)
        assertEquals("15:05", step3.endTime)
        assertFalse(step3.isCurrentCursor)
    }

    @Test
    fun testRecalculateRoutineTimelineClampsAt2359WithoutMidnightRollover() {
        val steps = listOf(
            RoutineStep(id = "s1", routineId = "r1", orderIndex = 1, title = "Late Step 1", durationMinutes = 45),
            RoutineStep(id = "s2", routineId = "r1", orderIndex = 2, title = "Late Step 2", durationMinutes = 60)
        )

        val result = recalculateRoutineTimelineUseCase(
            steps = steps,
            baseTargetStartTime = "23:00",
            newStartTime = "23:30",
            completedStepIds = emptySet()
        )

        val step1 = result.scheduledSteps[0]
        val step2 = result.scheduledSteps[1]

        assertEquals("23:30", step1.startTime)
        assertEquals("23:59", step1.endTime)

        assertEquals("23:59", step2.startTime)
        assertEquals("23:59", step2.endTime)
    }

    @Test
    fun testRecalculateRoutineTimelineScalesDurationsWhenTargetDayTypeProvided() {
        val steps = listOf(
            RoutineStep(id = "s1", routineId = "r1", orderIndex = 1, title = "Step 1", durationMinutes = 10),
            RoutineStep(id = "s2", routineId = "r1", orderIndex = 2, title = "Step 2", durationMinutes = 10)
        )

        // Target day type with 40 target study minutes (scales 20 min -> 40 min)
        val crunchDayType = DayType.CRUNCH.copy(targetStudyMinutes = 40)

        val result = recalculateRoutineTimelineUseCase(
            steps = steps,
            baseTargetStartTime = "08:00",
            newStartTime = "08:00",
            completedStepIds = emptySet(),
            targetDayType = crunchDayType
        )

        val totalScaledMinutes = result.scheduledSteps.sumOf { it.step.durationMinutes }
        assertEquals(40, totalScaledMinutes)
        assertEquals("08:00", result.scheduledSteps[0].startTime)
        assertEquals("08:20", result.scheduledSteps[0].endTime)
        assertEquals("08:20", result.scheduledSteps[1].startTime)
        assertEquals("08:40", result.scheduledSteps[1].endTime)
    }

    @Test
    fun testSaveRoutineUseCaseValidatesAndSavesRoutineWithSteps() = runTest {
        val newRoutine = Routine(
            id = "custom_routine",
            dayTypeId = "dt_standard",
            title = "Evening Wind Down",
            targetStartTime = "21:00",
            totalEstimatedMinutes = 20
        )
        val steps = listOf(
            RoutineStep(id = "step_c1", routineId = "custom_routine", orderIndex = 1, title = "Prep night tea", durationMinutes = 5),
            RoutineStep(id = "step_c2", routineId = "custom_routine", orderIndex = 2, title = "Read 10 pages", durationMinutes = 15)
        )

        saveRoutineUseCase(newRoutine, steps)

        val saved = routineRepository.getRoutineById("custom_routine")
        assertNotNull(saved)
        assertEquals("Evening Wind Down", saved?.title)
        assertEquals(20, saved?.totalEstimatedMinutes)
        assertEquals(2, saved?.steps?.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSaveRoutineUseCaseThrowsOnBlankTitle() = runTest {
        val invalidRoutine = Routine(
            id = "r_invalid",
            dayTypeId = "dt_standard",
            title = "   ",
            targetStartTime = "08:00",
            totalEstimatedMinutes = 10
        )
        saveRoutineUseCase(invalidRoutine)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSaveRoutineUseCaseThrowsOnDurationLessThanOneMinute() = runTest {
        val routine = Routine(
            id = "r_invalid_step",
            dayTypeId = "dt_standard",
            title = "Valid Routine",
            targetStartTime = "08:00",
            totalEstimatedMinutes = 10
        )
        val steps = listOf(
            RoutineStep(id = "st1", routineId = "r_invalid_step", orderIndex = 1, title = "Zero min step", durationMinutes = 0)
        )
        saveRoutineUseCase(routine, steps)
    }
}
