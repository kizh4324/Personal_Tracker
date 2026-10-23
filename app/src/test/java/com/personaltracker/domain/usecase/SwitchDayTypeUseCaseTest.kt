package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import com.personaltracker.ui.TestDayTypeRepository
import com.personaltracker.ui.TestRoutineRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwitchDayTypeUseCaseTest {

    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var routineRepository: TestRoutineRepository
    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var switchDayTypeUseCase: SwitchDayTypeUseCase

    private val targetDate = "2026-10-23"

    @Before
    fun setUp() {
        dayTypeRepository = TestDayTypeRepository()
        routineRepository = TestRoutineRepository()
        adaptiveEngine = AdaptiveScheduleEngine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(dayTypeRepository, adaptiveEngine)
        switchDayTypeUseCase = SwitchDayTypeUseCase(
            dayTypeRepository = dayTypeRepository,
            routineRepository = routineRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            adaptiveScheduleEngine = adaptiveEngine
        )
        // Ensure defaults are present
        dayTypeRepository.dayTypesFlow.value = DayType.BUILT_IN_DAY_TYPES
    }

    @Test
    fun `switchDayType sets single date override and preserves base templates`() = runTest {
        // When user swaps to Rest / Recovery
        val result = switchDayTypeUseCase(
            targetDayTypeId = "dt_rest",
            date = targetDate
        )

        // Then override is active for targetDate
        assertTrue(result.isOverrideActive)
        assertEquals("dt_rest", result.activeDayType.id)
        assertTrue(result.message.contains("Rest well today"))

        // And persistent override exists in repository
        val savedOverride = dayTypeRepository.getOverrideForDate(targetDate)
        assertNotNull(savedOverride)
        assertEquals("dt_rest", savedOverride?.dayTypeId)
        assertTrue(savedOverride?.isUserModified == true)

        // And other dates remain unmutated
        assertNull(dayTypeRepository.getOverrideForDate("2026-10-24"))
    }

    @Test
    fun `switchDayType scales uncompleted routine steps while preserving completed steps`() = runTest {
        // Given routine with 2 steps (Step 1: 30m, Step 2: 30m, total = 60m)
        val routine = Routine(
            id = "r_crunch",
            dayTypeId = "dt_crunch",
            title = "Deep Study Block",
            targetStartTime = "09:00",
            totalEstimatedMinutes = 60
        )
        val step1 = RoutineStep("step_1", "r_crunch", 1, "Review Material", 30)
        val step2 = RoutineStep("step_2", "r_crunch", 2, "Practice Problems", 30)

        routineRepository.saveRoutine(routine)
        routineRepository.saveSteps(listOf(step1, step2))

        // When user switches to Crunch Focus (targetStudyMinutes = 180), with step_1 already completed
        val result = switchDayTypeUseCase(
            targetDayTypeId = "dt_crunch",
            date = targetDate,
            completedStepIds = setOf("step_1")
        )

        // Then uncompleted step scaling is computed for the active schedule
        assertEquals(1, result.scaledRoutineStepCount)

        // And base template steps in routineRepository remain uncorrupted (non-destructive scaling)
        val baseSteps = routineRepository.getStepsForRoutine("r_crunch").first()
        assertEquals(30, baseSteps.find { it.id == "step_1" }?.durationMinutes)
        assertEquals(30, baseSteps.find { it.id == "step_2" }?.durationMinutes)
    }

    @Test
    fun `resetOverride deletes override and reverts to standard day-of-week schedule`() = runTest {
        // Given an active override
        switchDayTypeUseCase(
            targetDayTypeId = "dt_rest",
            date = targetDate
        )
        assertNotNull(dayTypeRepository.getOverrideForDate(targetDate))

        // When user resets override
        val result = switchDayTypeUseCase.resetOverride(targetDate)

        // Then override is deleted
        assertNull(dayTypeRepository.getOverrideForDate(targetDate))
        assertFalse(result.isOverrideActive)
        assertEquals("Standard Routine", result.activeDayType.name)
        assertTrue(result.message.contains("Schedule reset"))
    }

    @Test
    fun `low capacity swap emits gentle supportive copy`() = runTest {
        val result = switchDayTypeUseCase(
            targetDayTypeId = "dt_low_capacity",
            date = targetDate
        )

        assertEquals("Low Capacity", result.activeDayType.name)
        assertTrue(result.message.contains("adjusted gently"))
    }
}
