package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.CoinTransaction
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.ui.navigation.AppShellViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeEconomyRepo: TestEconomyRepository
    private lateinit var fakeHabitRepo: TestHabitRepository
    private lateinit var fakeDayTypeRepo: TestDayTypeRepository
    private lateinit var adaptiveScheduleEngine: AdaptiveScheduleEngine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        fakeEconomyRepo = TestEconomyRepository()
        fakeHabitRepo = TestHabitRepository()
        fakeDayTypeRepo = TestDayTypeRepository()
        adaptiveScheduleEngine = AdaptiveScheduleEngine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(fakeDayTypeRepo, adaptiveScheduleEngine)
    }

    private fun createViewModel(): AppShellViewModel {
        return AppShellViewModel(
            economyRepository = fakeEconomyRepo,
            habitRepository = fakeHabitRepo,
            dayTypeRepository = fakeDayTypeRepo,
            resolveDayTypeUseCase = resolveDayTypeUseCase
        )
    }

    @Test
    fun testAppShellInitialStateLoadsDayTypeAndZeroBalanceAndStreak() = runTest {
        val dtWeekday = DayType(
            id = "dt_weekday",
            name = "Weekday Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )
        fakeDayTypeRepo.dayTypesFlow.value = listOf(dtWeekday)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Weekday Routine", state.activeDayTypeName)
            assertEquals(0, state.streakCount)
            assertEquals(0, state.coinBalance)
        }
    }

    @Test
    fun testAppShellReactivelyUpdatesWhenBalanceChanges() = runTest {
        val dtWeekday = DayType(
            id = "dt_weekday",
            name = "Weekday Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )
        fakeDayTypeRepo.dayTypesFlow.value = listOf(dtWeekday)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Initial

            // Update balance
            fakeEconomyRepo.balanceFlow.value = 150

            val updated = awaitItem()
            assertEquals(150, updated.coinBalance)
        }
    }

    @Test
    fun testAppShellReactivelyUpdatesWhenHabitStreakChanges() = runTest {
        val dtWeekday = DayType(
            id = "dt_weekday",
            name = "Weekday Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )
        fakeDayTypeRepo.dayTypesFlow.value = listOf(dtWeekday)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Initial

            val habit1 = Habit(
                id = "h1",
                name = "Reading",
                targetDaysOfWeek = "1,2,3,4,5",
                currentStreak = 5,
                isFreezeActive = false
            )
            val habit2 = Habit(
                id = "h2",
                name = "Exercise",
                targetDaysOfWeek = "1,2,3,4,5",
                currentStreak = 14,
                isFreezeActive = false
            )
            fakeHabitRepo.habitsFlow.value = listOf(habit1, habit2)

            val updated = awaitItem()
            assertEquals(14, updated.streakCount) // Max streak
        }
    }

    @Test
    fun testAppShellReactivelyUpdatesWhenDayTypeOverrideChanges() = runTest {
        val dtDefault = DayType(
            id = "dt_default",
            name = "Regular Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 60,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )
        val dtExam = DayType(
            id = "dt_exam",
            name = "Exam Intensive",
            colorHex = "#EF4444",
            targetStudyMinutes = 180,
            isDefault = false,
            defaultDaysOfWeek = ""
        )
        fakeDayTypeRepo.dayTypesFlow.value = listOf(dtDefault, dtExam)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Initial

            // Apply override for today
            fakeDayTypeRepo.setOverrideForDate(
                DayOverride(
                    date = today,
                    dayTypeId = "dt_exam",
                    isUserModified = true,
                    createdTimestamp = System.currentTimeMillis()
                )
            )

            val updated = awaitItem()
            assertEquals("Exam Intensive", updated.activeDayTypeName)
        }
    }
}
