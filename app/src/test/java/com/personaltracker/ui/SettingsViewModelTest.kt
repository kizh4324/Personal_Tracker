package com.personaltracker.ui

import android.provider.Settings
import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.ui.screens.settings.SettingsUiEffect
import com.personaltracker.ui.screens.settings.SettingsUiIntent
import com.personaltracker.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        dayTypeRepository = TestDayTypeRepository()
        adaptiveEngine = AdaptiveScheduleEngine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(dayTypeRepository, adaptiveEngine)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            dayTypeRepository = dayTypeRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase
        )
    }

    @Test
    fun testSettingsInitialStateLoadsDayTypes() = runTest {
        val dt1 = DayType(id = "dt_weekday", name = "Weekday Routine", colorHex = "#3B82F6", targetStudyMinutes = 60, isDefault = true, defaultDaysOfWeek = "1,2,3,4,5")
        val dt2 = DayType(id = "dt_weekend", name = "Weekend Chill", colorHex = "#10B981", targetStudyMinutes = 30, isDefault = false, defaultDaysOfWeek = "6,7")
        dayTypeRepository.saveDayTypes(listOf(dt1, dt2))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.allDayTypes.size)
            assertNotNull(state.activeDayType)
            assertNull(state.dateOverride)
            assertFalse(state.isAccessibilityGranted)
            assertFalse(state.isOverlayGranted)
        }
    }

    @Test
    fun testSelectDayTypeOverrideSetsOverrideInRepository() = runTest {
        val dt1 = DayType(id = "dt_weekday", name = "Weekday Routine", colorHex = "#3B82F6", targetStudyMinutes = 60, isDefault = true, defaultDaysOfWeek = "1,2,3,4,5")
        val dt2 = DayType(id = "dt_exam", name = "Exam Day", colorHex = "#EF4444", targetStudyMinutes = 180, isDefault = false, defaultDaysOfWeek = "")
        dayTypeRepository.saveDayTypes(listOf(dt1, dt2))

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.SelectDayTypeOverride("dt_exam"))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Schedule template updated"))

            val override = dayTypeRepository.getOverrideForDate(today)
            assertNotNull(override)
            assertEquals("dt_exam", override?.dayTypeId)
        }
    }

    @Test
    fun testClearDayTypeOverrideRemovesOverride() = runTest {
        dayTypeRepository.setOverrideForDate(
            DayOverride(
                date = today,
                dayTypeId = "dt_custom",
                isUserModified = true,
                createdTimestamp = System.currentTimeMillis()
            )
        )
        assertNotNull(dayTypeRepository.getOverrideForDate(today))

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.ClearDayTypeOverride)

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Reset to default"))

            assertNull(dayTypeRepository.getOverrideForDate(today))
        }
    }

    @Test
    fun testExportEncryptedBackupEmitsSnackbar() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.ExportEncryptedBackup)

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Encrypted backup generated"))
        }
    }

    @Test
    fun testOpenAccessibilitySettingsEmitsSystemAction() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.OpenAccessibilitySettings)

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.OpenSystemSetting)
            assertEquals(
                Settings.ACTION_ACCESSIBILITY_SETTINGS,
                (effect as SettingsUiEffect.OpenSystemSetting).action
            )
        }
    }

    @Test
    fun testOpenOverlaySettingsEmitsSystemAction() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.OpenOverlaySettings)

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.OpenSystemSetting)
            assertEquals(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                (effect as SettingsUiEffect.OpenSystemSetting).action
            )
        }
    }

    @Test
    fun testCheckPermissionsIntentUpdatesState() = runTest {
        var accessibilityGranted = false
        var overlayGranted = false

        val viewModel = createViewModel().apply {
            permissionChecker = { Pair(accessibilityGranted, overlayGranted) }
        }

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isAccessibilityGranted)
            assertFalse(initialState.isOverlayGranted)

            accessibilityGranted = true
            overlayGranted = true
            viewModel.onIntent(SettingsUiIntent.CheckPermissions)

            val updatedState = awaitItem()
            assertTrue(updatedState.isAccessibilityGranted)
            assertTrue(updatedState.isOverlayGranted)
        }
    }
}
