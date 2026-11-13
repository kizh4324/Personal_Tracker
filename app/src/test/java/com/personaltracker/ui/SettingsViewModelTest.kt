package com.personaltracker.ui

import android.provider.Settings
import app.cash.turbine.test
import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.usecase.AddWhitelistedAppUseCase
import com.personaltracker.domain.usecase.GetInterventionRulesUseCase
import com.personaltracker.domain.usecase.GetWhitelistRulesUseCase
import com.personaltracker.domain.usecase.RemoveWhitelistedAppUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ToggleAppWhitelistUseCase
import com.personaltracker.domain.usecase.TogglePackageInterceptionRuleUseCase
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
    private lateinit var getInterventionRulesUseCase: GetInterventionRulesUseCase
    private lateinit var togglePackageInterceptionRuleUseCase: TogglePackageInterceptionRuleUseCase
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
        getInterventionRulesUseCase = GetInterventionRulesUseCase(ruleRepository)
        togglePackageInterceptionRuleUseCase = TogglePackageInterceptionRuleUseCase(ruleRepository)
        getWhitelistRulesUseCase = GetWhitelistRulesUseCase(ruleRepository)
        toggleAppWhitelistUseCase = ToggleAppWhitelistUseCase(ruleRepository, togglePackageInterceptionRuleUseCase)
        addWhitelistedAppUseCase = AddWhitelistedAppUseCase(ruleRepository)
        removeWhitelistedAppUseCase = RemoveWhitelistedAppUseCase(ruleRepository)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            dayTypeRepository = dayTypeRepository,
            resolveDayTypeUseCase = resolveDayTypeUseCase,
            getInterventionRulesUseCase = getInterventionRulesUseCase,
            togglePackageInterceptionRuleUseCase = togglePackageInterceptionRuleUseCase,
            addWhitelistedAppUseCase = addWhitelistedAppUseCase,
            removeWhitelistedAppUseCase = removeWhitelistedAppUseCase,
            getWhitelistRulesUseCase = getWhitelistRulesUseCase,
            toggleAppWhitelistUseCase = toggleAppWhitelistUseCase
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
            assertEquals(1, state.blockedRules.size)
            assertEquals("com.instagram.android", state.blockedRules.first().packageName)
            assertEquals(2, state.allInterventionRules.size)
            assertEquals("", state.searchQuery)
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
    fun testTogglePackageInterceptionRuleUpdatesExemption() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // Instagram was isStrictBlocked = true; toggling should make it false (whitelisted)
            viewModel.onIntent(SettingsUiIntent.TogglePackageInterceptionRule("com.instagram.android"))

            val effect = awaitItem()
            assertTrue(effect is SettingsUiEffect.ShowSnackbar)
            assertTrue((effect as SettingsUiEffect.ShowSnackbar).message.contains("whitelist status updated"))

            val rule = ruleRepository.getRuleForPackage("com.instagram.android")
            assertNotNull(rule)
            assertFalse(rule!!.isStrictBlocked)
        }
    }

    @Test
    fun testSearchWhitelistRulesFiltersRulesInUiState() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(1, initial.whitelistedRules.size)
            assertEquals(1, initial.blockedRules.size)
            assertEquals("", initial.searchQuery)

            // Search for "auth" -> should match Authenticator (whitelisted) only
            viewModel.onIntent(SettingsUiIntent.SearchWhitelistRules("auth"))
            val filtered = awaitItem()
            assertEquals("auth", filtered.searchQuery)
            assertEquals(1, filtered.whitelistedRules.size)
            assertEquals(0, filtered.blockedRules.size)
            assertEquals("Authenticator", filtered.whitelistedRules.first().appDisplayName)

            // Search for "insta" -> should match Instagram (blocked) only
            viewModel.onIntent(SettingsUiIntent.SearchWhitelistRules("insta"))
            val filteredBlocked = awaitItem()
            assertEquals("insta", filteredBlocked.searchQuery)
            assertEquals(0, filteredBlocked.whitelistedRules.size)
            assertEquals(1, filteredBlocked.blockedRules.size)
            assertEquals("Instagram", filteredBlocked.blockedRules.first().appDisplayName)

            // Clear search query -> restores full list
            viewModel.onIntent(SettingsUiIntent.SearchWhitelistRules(""))
            val restored = awaitItem()
            assertEquals("", restored.searchQuery)
            assertEquals(1, restored.whitelistedRules.size)
            assertEquals(1, restored.blockedRules.size)
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
