package com.personaltracker.ui

import android.provider.Settings
import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.usecase.AddWhitelistedAppUseCase
import com.personaltracker.domain.usecase.GetWhitelistRulesUseCase
import com.personaltracker.domain.usecase.RemoveWhitelistedAppUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ToggleAppWhitelistUseCase
import com.personaltracker.platform.accessibility.DistractionInterceptionService
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
    private lateinit var ruleRepository: TestInterventionRuleRepository
    private lateinit var adaptiveEngine: AdaptiveScheduleEngine
    private lateinit var resolveDayTypeUseCase: ResolveDayTypeUseCase
    private lateinit var getWhitelistRulesUseCase: GetWhitelistRulesUseCase
    private lateinit var toggleAppWhitelistUseCase: ToggleAppWhitelistUseCase
    private lateinit var addWhitelistedAppUseCase: AddWhitelistedAppUseCase
    private lateinit var removeWhitelistedAppUseCase: RemoveWhitelistedAppUseCase

    private val today = LocalDate.now().toString()

    @Before
    fun setUp() {
        DistractionInterceptionService.activeFocusTimerPolicy = FocusTimerPolicy.AUTO_PAUSE
        dayTypeRepository = TestDayTypeRepository()
        ruleRepository = TestInterventionRuleRepository(
            listOf(
                InterventionRule(
                    packageName = "com.google.android.apps.authenticator2",
                    appDisplayName = "Authenticator",
                    isStrictBlocked = false
                ),
                InterventionRule(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    isStrictBlocked = true
                )
            )
        )
        adaptiveEngine = AdaptiveScheduleEngine()
        resolveDayTypeUseCase = ResolveDayTypeUseCase(dayTypeRepository, adaptiveEngine)
        getWhitelistRulesUseCase = GetWhitelistRulesUseCase(ruleRepository)
        toggleAppWhitelistUseCase = ToggleAppWhitelistUseCase(ruleRepository)
        addWhitelistedAppUseCase = AddWhitelistedAppUseCase(ruleRepository)
        removeWhitelistedAppUseCase = RemoveWhitelistedAppUseCase(ruleRepository)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            dayTypeRepository = dayTypeRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            getWhitelistRulesUseCase = getWhitelistRulesUseCase,
            toggleAppWhitelistUseCase = toggleAppWhitelistUseCase,
            addWhitelistedAppUseCase = addWhitelistedAppUseCase,
            removeWhitelistedAppUseCase = removeWhitelistedAppUseCase
        )
    }

    @Test
    fun testSettingsInitialStateLoadsDayTypesAndWhitelist() = runTest {
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
            assertEquals(1, state.whitelistedRules.size)
            assertEquals("com.google.android.apps.authenticator2", state.whitelistedRules.first().packageName)
            assertEquals(2, state.allInterventionRules.size)
            assertEquals(FocusTimerPolicy.AUTO_PAUSE, state.focusTimerPolicy)
            assertFalse(state.isWhitelistSheetOpen)
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

    @Test
    fun testToggleAppWhitelistUpdatesExemption() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // Instagram was isStrictBlocked = true; toggling should make it false (whitelisted)
            viewModel.onIntent(SettingsUiIntent.ToggleAppWhitelist("com.instagram.android"))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("whitelist status updated"))

            val rule = ruleRepository.getRuleForPackage("com.instagram.android")
            assertNotNull(rule)
            assertFalse(rule!!.isStrictBlocked)
        }
    }

    @Test
    fun testAddCustomWhitelistPackageInsertsRuleAndClosesSheet() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.ToggleWhitelistSheet(true))
            assertEquals(true, viewModel.uiState.value.isWhitelistSheetOpen)

            viewModel.onIntent(
                SettingsUiIntent.AddCustomWhitelistPackage(
                    packageName = "com.bitwarden.bitwarden",
                    appDisplayName = "Bitwarden"
                )
            )

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Added Bitwarden to whitelist"))

            val rule = ruleRepository.getRuleForPackage("com.bitwarden.bitwarden")
            assertNotNull(rule)
            assertFalse(rule!!.isStrictBlocked)
            assertFalse(viewModel.uiState.value.isWhitelistSheetOpen)
        }
    }

    @Test
    fun testRemoveWhitelistPackageDeletesRule() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.RemoveWhitelistPackage("com.google.android.apps.authenticator2"))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Removed package from whitelist"))

            val rule = ruleRepository.getRuleForPackage("com.google.android.apps.authenticator2")
            assertNull(rule)
        }
    }

    @Test
    fun testSetFocusTimerPolicyUpdatesPolicyAndService() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(SettingsUiIntent.SetFocusTimerPolicy(FocusTimerPolicy.CONTINUE_COUNTDOWN))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("Continue countdown"))

            assertEquals(FocusTimerPolicy.CONTINUE_COUNTDOWN, viewModel.uiState.value.focusTimerPolicy)
            assertEquals(FocusTimerPolicy.CONTINUE_COUNTDOWN, DistractionInterceptionService.activeFocusTimerPolicy)
        }
    }

    @Test
    fun testSettingsInitialStateSyncsWithActiveFocusTimerPolicy() = runTest {
        DistractionInterceptionService.activeFocusTimerPolicy = FocusTimerPolicy.CONTINUE_COUNTDOWN
        try {
            val viewModel = createViewModel()
            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(FocusTimerPolicy.CONTINUE_COUNTDOWN, state.focusTimerPolicy)
            }
        } finally {
            DistractionInterceptionService.activeFocusTimerPolicy = FocusTimerPolicy.AUTO_PAUSE
        }
    }

    @Test
    fun testToggleAppWhitelistTogglingOffDeletesRule() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // Authenticator was isStrictBlocked = false (whitelisted). Toggling off should delete it
            viewModel.onIntent(SettingsUiIntent.ToggleAppWhitelist("com.google.android.apps.authenticator2"))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("whitelist status updated"))

            val rule = ruleRepository.getRuleForPackage("com.google.android.apps.authenticator2")
            assertNull(rule)
        }
    }
}
