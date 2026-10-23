package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.ui.TestDayTypeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ResolveDayTypeUseCaseTest {

    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var useCase: ResolveDayTypeUseCase

    private val standardDayType = DayType(
        id = "dt_standard",
        name = "Standard Routine",
        colorHex = "#3B82F6",
        targetStudyMinutes = 90,
        isDefault = true,
        defaultDaysOfWeek = "1,2,3,4,5"
    )

    private val restDayType = DayType(
        id = "dt_rest",
        name = "Rest / Recovery",
        colorHex = "#10B981",
        targetStudyMinutes = 30,
        isDefault = false,
        defaultDaysOfWeek = "6,7"
    )

    private val crunchDayType = DayType(
        id = "dt_crunch",
        name = "Crunch Focus",
        colorHex = "#8B5CF6",
        targetStudyMinutes = 180,
        isDefault = false,
        defaultDaysOfWeek = ""
    )

    @Before
    fun setUp() {
        dayTypeRepository = TestDayTypeRepository()
        adaptiveEngine = AdaptiveScheduleEngine()
        useCase = ResolveDayTypeUseCase(dayTypeRepository, adaptiveEngine)
    }

    @Test
    fun `date-specific override has highest priority over day-of-week and default`() = runTest {
        // Given available templates and a date on a Monday (day-of-week = 1 -> standard)
        val mondayDate = "2026-10-19" // Monday
        dayTypeRepository.saveDayTypes(listOf(standardDayType, restDayType, crunchDayType))

        // When user overrides Monday with Crunch Focus
        dayTypeRepository.setOverrideForDate(
            DayOverride(
                date = mondayDate,
                dayTypeId = "dt_crunch",
                isUserModified = true,
                createdTimestamp = System.currentTimeMillis()
            )
        )

        val resolved = useCase(mondayDate)

        // Then override wins
        assertEquals("dt_crunch", resolved.id)
        assertEquals("Crunch Focus", resolved.name)
    }

    @Test
    fun `day-of-week matches correctly when no override exists`() = runTest {
        // Given Saturday (day-of-week = 6 -> rest)
        val saturdayDate = "2026-10-24" // Saturday
        dayTypeRepository.saveDayTypes(listOf(standardDayType, restDayType, crunchDayType))

        val resolved = useCase(saturdayDate)

        // Then day-of-week template wins
        assertEquals("dt_rest", resolved.id)
        assertEquals("Rest / Recovery", resolved.name)
    }

    @Test
    fun `default template is selected when day-of-week has no match`() = runTest {
        // Given templates without day-of-week mapping
        val genericDefault = DayType(
            id = "dt_gen_default",
            name = "Generic Default",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = ""
        )
        val genericNonDefault = DayType(
            id = "dt_gen_other",
            name = "Special Event",
            colorHex = "#F59E0B",
            targetStudyMinutes = 120,
            isDefault = false,
            defaultDaysOfWeek = ""
        )
        dayTypeRepository.saveDayTypes(listOf(genericNonDefault, genericDefault))

        val resolved = useCase("2026-10-21") // Wednesday

        // Then default template is selected
        assertEquals("dt_gen_default", resolved.id)
    }

    @Test
    fun `empty repository falls back to safe default routine template`() = runTest {
        // Given empty repository
        val resolved = useCase("2026-10-20") // Tuesday

        // Then resolves safe default routine
        assertEquals(ResolveDayTypeUseCase.SAFE_DEFAULT_DAY_TYPE.id, resolved.id)
        assertEquals("Default Routine", resolved.name)
    }
}
