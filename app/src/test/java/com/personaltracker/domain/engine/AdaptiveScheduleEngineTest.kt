package com.personaltracker.domain.engine

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Routine
import com.personaltracker.domain.model.RoutineStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AdaptiveScheduleEngineTest {

    private lateinit var engine: AdaptiveScheduleEngine

    private val weekdayTemplate = DayType(
        id = "dt_weekday",
        name = "Weekday Routine",
        colorHex = "#3B82F6",
        targetStudyMinutes = 120,
        isDefault = false,
        defaultDaysOfWeek = "1,2,3,4,5"
    )

    private val weekendTemplate = DayType(
        id = "dt_weekend",
        name = "Weekend Relax",
        colorHex = "#10B981",
        targetStudyMinutes = 60,
        isDefault = false,
        defaultDaysOfWeek = "6,7"
    )

    private val defaultFallbackTemplate = DayType(
        id = "dt_default",
        name = "Standard Day",
        colorHex = "#6B7280",
        targetStudyMinutes = 90,
        isDefault = true,
        defaultDaysOfWeek = ""
    )

    private val heavyStudyTemplate = DayType(
        id = "dt_heavy_study",
        name = "Heavy Study",
        colorHex = "#8B5CF6",
        targetStudyMinutes = 240,
        isDefault = false,
        defaultDaysOfWeek = ""
    )

    @Before
    fun setUp() {
        engine = AdaptiveScheduleEngine()
    }

    // ==========================================
    // 1. DayType Hierarchy Resolution Tests
    // ==========================================

    @Test
    fun testResolveActiveDayTypeUserOverrideTakesPrecedenceOverDayOfWeek() {
        // 2026-10-17 is a Saturday (day 6), matching weekendTemplate by default
        val targetDate = "2026-10-17"
        val override = DayOverride(
            date = targetDate,
            dayTypeId = "dt_heavy_study",
            isUserModified = true,
            createdTimestamp = System.currentTimeMillis()
        )

        val resolved = engine.resolveActiveDayType(
            date = targetDate,
            override = override,
            availableDayTypes = listOf(weekdayTemplate, weekendTemplate, defaultFallbackTemplate, heavyStudyTemplate)
        )

        assertEquals("dt_heavy_study", resolved.id)
        assertEquals("Heavy Study", resolved.name)
    }

    @Test
    fun testResolveActiveDayTypeFallbackToDayOfWeekWhenOverrideNotFound() {
        // User override points to a non-existent DayType ID
        val targetDate = "2026-10-17" // Saturday (day 6)
        val missingOverride = DayOverride(
            date = targetDate,
            dayTypeId = "dt_non_existent",
            isUserModified = true,
            createdTimestamp = System.currentTimeMillis()
        )

        val resolved = engine.resolveActiveDayType(
            date = targetDate,
            override = missingOverride,
            availableDayTypes = listOf(weekdayTemplate, weekendTemplate, defaultFallbackTemplate)
        )

        assertEquals("dt_weekend", resolved.id)
        assertEquals("Weekend Relax", resolved.name)
    }

    @Test
    fun testResolveActiveDayTypeResolvesDayOfWeekWhenNoOverride() {
        // 2026-10-16 is Friday (day 5)
        val resolved = engine.resolveActiveDayType(
            date = "2026-10-16",
            override = null,
            availableDayTypes = listOf(weekdayTemplate, weekendTemplate, defaultFallbackTemplate)
        )

        assertEquals("dt_weekday", resolved.id)
    }

    @Test
    fun testResolveActiveDayTypeFallbackToDefaultTemplateWhenNoDayOfWeekMatch() {
        // Date with empty defaultDaysOfWeek on template
        val unassignedTemplate = DayType(
            id = "dt_custom",
            name = "Custom",
            colorHex = "#FFFFFF",
            targetStudyMinutes = 30,
            isDefault = false,
            defaultDaysOfWeek = ""
        )

        val resolved = engine.resolveActiveDayType(
            date = "2026-10-16",
            override = null,
            availableDayTypes = listOf(unassignedTemplate, defaultFallbackTemplate)
        )

        assertEquals("dt_default", resolved.id)
    }

    @Test
    fun testResolveActiveDayTypeThrowsWhenListIsEmpty() {
        try {
            engine.resolveActiveDayType("2026-10-16", null, emptyList())
            fail("Expected IllegalArgumentException for empty template list")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("must not be empty") == true)
        }
    }

    // ==========================================
    // 2. Sequential Routine Recalculation Tests
    // ==========================================

    @Test
    fun testRecalculateSequentialRoutineWithCompletedStepAndShiftedDelay() {
        val steps = listOf(
            RoutineStep(id = "step1", routineId = "r1", orderIndex = 0, title = "Warmup", durationMinutes = 15),
            RoutineStep(id = "step2", routineId = "r1", orderIndex = 1, title = "Deep Work 1", durationMinutes = 45),
            RoutineStep(id = "step3", routineId = "r1", orderIndex = 2, title = "Break", durationMinutes = 10),
            RoutineStep(id = "step4", routineId = "r1", orderIndex = 3, title = "Deep Work 2", durationMinutes = 50)
        )

        // Routine starts at 08:00. Step 1 is completed (08:00 - 08:15).
        // Delay shifted by 30 min before step 2 starts.
        val scheduled = engine.recalculateSequentialRoutine(
            steps = steps,
            targetStartTime = "08:00",
            delayMinutes = 30,
            completedStepIds = setOf("step1")
        )

        assertEquals(4, scheduled.size)

        // Step 1: completed, original time preserved
        assertTrue(scheduled[0].isCompleted)
        assertEquals("08:00", scheduled[0].startTime)
        assertEquals("08:15", scheduled[0].endTime)

        // Step 2: uncompleted, shifted forward from 08:15 + 30 min = 08:45
        assertFalse(scheduled[1].isCompleted)
        assertEquals("08:45", scheduled[1].startTime)
        assertEquals("09:30", scheduled[1].endTime)

        // Step 3: uncompleted, starts immediately after Step 2
        assertEquals("09:30", scheduled[2].startTime)
        assertEquals("09:40", scheduled[2].endTime)

        // Step 4: uncompleted, starts immediately after Step 3
        assertEquals("09:40", scheduled[3].startTime)
        assertEquals("10:30", scheduled[3].endTime)
    }

    @Test
    fun testRecalculateSequentialRoutineHandlesEmptySteps() {
        val result = engine.recalculateSequentialRoutine(
            steps = emptyList(),
            targetStartTime = "08:00",
            delayMinutes = 15
        )
        assertTrue(result.isEmpty())
    }

    // ==========================================
    // 3. Routine Duration Scaling Tests
    // ==========================================

    @Test
    fun testScaleRoutineDurationsProportionally() {
        val originalRoutine = Routine(
            id = "r1",
            dayTypeId = "dt_weekday",
            title = "Morning Routine",
            targetStartTime = "07:00",
            totalEstimatedMinutes = 60,
            steps = listOf(
                RoutineStep(id = "s1", routineId = "r1", orderIndex = 0, title = "Drink Water", durationMinutes = 10),
                RoutineStep(id = "s2", routineId = "r1", orderIndex = 1, title = "Exercise", durationMinutes = 30),
                RoutineStep(id = "s3", routineId = "r1", orderIndex = 2, title = "Journal", durationMinutes = 20)
            )
        )

        // Scale from 60 minutes to 90 minutes (1.5x)
        val scaled = engine.scaleRoutineDurations(originalRoutine, targetTotalMinutes = 90)

        assertEquals(90, scaled.totalEstimatedMinutes)
        assertEquals(3, scaled.steps.size)

        val totalSum = scaled.steps.sumOf { it.durationMinutes }
        assertEquals(90, totalSum)

        // Check proportional scaling
        assertEquals(15, scaled.steps[0].durationMinutes) // 10 * 1.5 = 15
        assertEquals(45, scaled.steps[1].durationMinutes) // 30 * 1.5 = 45
        assertEquals(30, scaled.steps[2].durationMinutes) // 20 * 1.5 = 30
    }

    @Test
    fun testScaleRoutineDurationsEnsuresSumIntegrityOnRounding() {
        val originalRoutine = Routine(
            id = "r1",
            dayTypeId = "dt_weekday",
            title = "Routine",
            targetStartTime = "07:00",
            totalEstimatedMinutes = 25,
            steps = listOf(
                RoutineStep(id = "s1", routineId = "r1", orderIndex = 0, title = "A", durationMinutes = 7),
                RoutineStep(id = "s2", routineId = "r1", orderIndex = 1, title = "B", durationMinutes = 9),
                RoutineStep(id = "s3", routineId = "r1", orderIndex = 2, title = "C", durationMinutes = 9)
            )
        )

        // Scale to 40 minutes
        val scaled = engine.scaleRoutineDurations(originalRoutine, targetTotalMinutes = 40)
        assertEquals(40, scaled.steps.sumOf { it.durationMinutes })
    }
}
