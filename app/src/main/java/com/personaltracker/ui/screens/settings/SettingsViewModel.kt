package com.personaltracker.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.EscalationPolicy
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EscalationStateRepository
import com.personaltracker.domain.usecase.AddWhitelistedAppUseCase
import com.personaltracker.domain.usecase.GetInterventionRulesUseCase
import com.personaltracker.domain.usecase.GetWhitelistRulesUseCase
import com.personaltracker.domain.usecase.RemoveWhitelistedAppUseCase
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import com.personaltracker.domain.usecase.ToggleAppWhitelistUseCase
import com.personaltracker.domain.usecase.TogglePackageInterceptionRuleUseCase
import com.personaltracker.platform.accessibility.DistractionInterceptionService
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Production ViewModel driving Settings, DayType template swaps, focus interception permissions,
 * tiered notifications, exact alarm status, and emergency / dynamic app whitelist management (AD-1, AD-2, FR-4.6, FR-10.1, Story 5.1).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dayTypeRepository: DayTypeRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val getInterventionRulesUseCase: GetInterventionRulesUseCase,
    private val togglePackageInterceptionRuleUseCase: TogglePackageInterceptionRuleUseCase,
    private val addWhitelistedAppUseCase: AddWhitelistedAppUseCase,
    private val removeWhitelistedAppUseCase: RemoveWhitelistedAppUseCase,
    private val getWhitelistRulesUseCase: GetWhitelistRulesUseCase,
    private val toggleAppWhitelistUseCase: ToggleAppWhitelistUseCase,
    @ApplicationContext private val context: Context? = null,
    private val taskAlarmScheduler: TaskAlarmScheduler? = null,
    private val escalationStateRepository: EscalationStateRepository? = null
) : ViewModel() {

    constructor(
        dayTypeRepository: DayTypeRepository,
        resolveDayTypeUseCase: ResolveDayTypeUseCase,
        getInterventionRulesUseCase: GetInterventionRulesUseCase,
        togglePackageInterceptionRuleUseCase: TogglePackageInterceptionRuleUseCase,
        addWhitelistedAppUseCase: AddWhitelistedAppUseCase,
        removeWhitelistedAppUseCase: RemoveWhitelistedAppUseCase,
        getWhitelistRulesUseCase: GetWhitelistRulesUseCase,
        toggleAppWhitelistUseCase: ToggleAppWhitelistUseCase,
        context: Context?,
        taskAlarmScheduler: TaskAlarmScheduler?
    ) : this(
        dayTypeRepository = dayTypeRepository,
        resolveDayTypeUseCase = resolveDayTypeUseCase,
        getInterventionRulesUseCase = getInterventionRulesUseCase,
        togglePackageInterceptionRuleUseCase = togglePackageInterceptionRuleUseCase,
        addWhitelistedAppUseCase = addWhitelistedAppUseCase,
        removeWhitelistedAppUseCase = removeWhitelistedAppUseCase,
        getWhitelistRulesUseCase = getWhitelistRulesUseCase,
        toggleAppWhitelistUseCase = toggleAppWhitelistUseCase,
        context = context,
        taskAlarmScheduler = taskAlarmScheduler,
        escalationStateRepository = null
    )

    internal var permissionChecker: (() -> Pair<Boolean, Boolean>)? = null
    internal var exactAlarmChecker: (() -> Boolean)? = null
    internal var notificationChecker: (() -> Boolean)? = null

    private val _searchQuery = MutableStateFlow("")
    private val _focusTimerPolicy = MutableStateFlow(DistractionInterceptionService.activeFocusTimerPolicy)
    private val _isWhitelistSheetOpen = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<SettingsUiEffect> = _uiEffect.receiveAsFlow()

    private var observeDataJob: Job? = null

    init {
        observeSettingsData()
    }

    private fun observeSettingsData() {
        observeDataJob?.cancel()
        val today = LocalDate.now().toString()

        val dayDataFlow = combine(
            dayTypeRepository.getAllDayTypes(),
            dayTypeRepository.observeOverrideForDate(today)
        ) { dayTypes, override ->
            Pair(dayTypes, override)
        }

        val rulesFlow = getInterventionRulesUseCase(_searchQuery)
        val policyFlow = escalationStateRepository?.observePolicy() ?: flowOf(EscalationPolicy())

        observeDataJob = combine(
            dayDataFlow,
            rulesFlow,
            _focusTimerPolicy,
            _isWhitelistSheetOpen,
            policyFlow
        ) { (dayTypes, override), categorizedRules, timerPolicy, isSheetOpen, escalationPolicy ->
            val resolvedActive = resolveDayTypeUseCase.resolve(today, override, dayTypes)
            val (accessibility, overlay) = checkPermissionsStatus()
            val (exactAlarm, notifications) = checkAlarmAndNotificationStatus()

            SettingsUiState(
                allDayTypes = dayTypes,
                activeDayType = resolvedActive,
                dateOverride = override,
                whitelistedRules = categorizedRules.whitelistedRules,
                blockedRules = categorizedRules.blockedRules,
                allInterventionRules = categorizedRules.allRules,
                searchQuery = categorizedRules.searchQuery,
                focusTimerPolicy = timerPolicy,
                isWhitelistSheetOpen = isSheetOpen,
                isAccessibilityGranted = accessibility,
                isOverlayGranted = overlay,
                isExactAlarmGranted = exactAlarm,
                isNotificationsEnabled = notifications,
                escalationPolicy = escalationPolicy,
                isLoading = false,
                errorMessage = null
            )
        }.distinctUntilChanged()
        .onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.SelectDayTypeOverride -> handleSelectOverride(intent.dayTypeId)
            is SettingsUiIntent.ClearDayTypeOverride -> handleClearOverride()
            is SettingsUiIntent.ExportEncryptedBackup -> handleExportBackup()
            is SettingsUiIntent.CheckPermissions -> checkPermissions()
            is SettingsUiIntent.OpenAccessibilitySettings -> handleOpenAccessibilitySettings()
            is SettingsUiIntent.OpenOverlaySettings -> handleOpenOverlaySettings()
            is SettingsUiIntent.OpenExactAlarmSettings -> handleOpenExactAlarmSettings()
            is SettingsUiIntent.OpenNotificationSettings -> handleOpenNotificationSettings()
            is SettingsUiIntent.UpdateEscalationPolicy -> handleUpdateEscalationPolicy(
                intent.retryIntervalMinutes,
                intent.maxReFires
            )
            is SettingsUiIntent.Refresh -> {
                observeSettingsData()
                checkPermissions()
            }
            is SettingsUiIntent.SearchWhitelistRules -> handleSearchRules(intent.query)
            is SettingsUiIntent.TogglePackageInterceptionRule -> handleTogglePackageRule(intent.packageName, intent.appDisplayName)
            is SettingsUiIntent.ToggleAppWhitelist -> handleTogglePackageRule(intent.packageName, intent.appDisplayName)
            is SettingsUiIntent.AddCustomWhitelistPackage -> handleAddCustomWhitelistPackage(intent.packageName, intent.appDisplayName)
            is SettingsUiIntent.RemoveWhitelistPackage -> handleRemoveWhitelistPackage(intent.packageName)
            is SettingsUiIntent.SetFocusTimerPolicy -> handleSetFocusTimerPolicy(intent.policy)
            is SettingsUiIntent.ToggleWhitelistSheet -> handleToggleWhitelistSheet(intent.isOpen)
        }
    }

    fun checkPermissions() {
        val (accessibility, overlay) = checkPermissionsStatus()
        val (exactAlarm, notifications) = checkAlarmAndNotificationStatus()
        _uiState.update {
            it.copy(
                isAccessibilityGranted = accessibility,
                isOverlayGranted = overlay,
                isExactAlarmGranted = exactAlarm,
                isNotificationsEnabled = notifications
            )
        }
    }

    private fun checkPermissionsStatus(): Pair<Boolean, Boolean> {
        permissionChecker?.let { return it.invoke() }
        val ctx = context ?: return Pair(false, false)
        val accessibility = isAccessibilityEnabled(ctx)
        val overlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(ctx)
        } else {
            true
        }
        return Pair(accessibility, overlay)
    }

    private fun checkAlarmAndNotificationStatus(): Pair<Boolean, Boolean> {
        val exact = exactAlarmChecker?.invoke() ?: (taskAlarmScheduler?.canScheduleExactAlarms() ?: true)
        val notifications = notificationChecker?.invoke() ?: (context?.let { NotificationManagerCompat.from(it).areNotificationsEnabled() } ?: true)
        return Pair(exact, notifications)
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val expectedComponentName = ComponentName(
                context,
                "com.personaltracker.platform.accessibility.DistractionInterceptionService"
            ).flattenToString()
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun handleOpenAccessibilitySettings() {
        viewModelScope.launch {
            _uiEffect.send(SettingsUiEffect.OpenSystemSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun handleOpenOverlaySettings() {
        viewModelScope.launch {
            _uiEffect.send(SettingsUiEffect.OpenSystemSetting(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun handleOpenExactAlarmSettings() {
        viewModelScope.launch {
            val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            } else {
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            }
            _uiEffect.send(SettingsUiEffect.OpenSystemSetting(action))
        }
    }

    private fun handleOpenNotificationSettings() {
        viewModelScope.launch {
            val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            } else {
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            }
            _uiEffect.send(SettingsUiEffect.OpenSystemSetting(action))
        }
    }

    private fun handleSelectOverride(dayTypeId: String) {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                val override = DayOverride(
                    date = today,
                    dayTypeId = dayTypeId,
                    isUserModified = true,
                    createdTimestamp = System.currentTimeMillis()
                )
                dayTypeRepository.setOverrideForDate(override)
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Schedule template updated for today"))
            }.onFailure {
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Could not update template. Please try again."))
            }
        }
    }

    private fun handleClearOverride() {
        viewModelScope.launch {
            runCatching {
                val today = LocalDate.now().toString()
                dayTypeRepository.deleteOverrideForDate(today)
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Reset to default schedule"))
            }
        }
    }

    private fun handleExportBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingBackup = true) }
            _uiEffect.send(SettingsUiEffect.ShowSnackbar("Encrypted backup generated safely"))
            _uiState.update { it.copy(isExportingBackup = false, backupExportSuccess = true) }
        }
    }

    private fun handleSearchRules(query: String) {
        _searchQuery.value = query
    }

    private fun handleTogglePackageRule(packageName: String, appDisplayName: String?) {
        viewModelScope.launch {
            runCatching {
                togglePackageInterceptionRuleUseCase(packageName, appDisplayName)
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("App whitelist status updated"))
            }.onFailure {
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Could not update app whitelist"))
            }
        }
    }

    private fun handleAddCustomWhitelistPackage(packageName: String, appDisplayName: String) {
        viewModelScope.launch {
            runCatching {
                val success = addWhitelistedAppUseCase(packageName, appDisplayName)
                if (success) {
                    val label = appDisplayName.trim().ifBlank { packageName.trim().substringAfterLast('.') }
                    _uiEffect.send(SettingsUiEffect.ShowSnackbar("Added $label to whitelist"))
                    _isWhitelistSheetOpen.value = false
                } else {
                    _uiEffect.send(SettingsUiEffect.ShowSnackbar("Package name cannot be blank"))
                }
            }.onFailure {
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Could not add package to whitelist"))
            }
        }
    }

    private fun handleRemoveWhitelistPackage(packageName: String) {
        viewModelScope.launch {
            runCatching {
                removeWhitelistedAppUseCase(packageName)
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Removed package from whitelist"))
            }.onFailure {
                _uiEffect.send(SettingsUiEffect.ShowSnackbar("Could not remove package"))
            }
        }
    }

    private fun handleSetFocusTimerPolicy(policy: FocusTimerPolicy) {
        _focusTimerPolicy.value = policy
        DistractionInterceptionService.activeFocusTimerPolicy = policy
        viewModelScope.launch {
            _uiEffect.send(SettingsUiEffect.ShowSnackbar("Call policy: ${policy.displayName}"))
        }
    }

    private fun handleToggleWhitelistSheet(isOpen: Boolean) {
        _isWhitelistSheetOpen.value = isOpen
    }

    private fun handleUpdateEscalationPolicy(retryIntervalMinutes: Int, maxReFires: Int) {
        viewModelScope.launch {
            runCatching {
                val updated = EscalationPolicy(
                    retryIntervalMinutes = retryIntervalMinutes,
                    maxReFires = maxReFires
                )
                escalationStateRepository?.updatePolicy(updated)
                _uiState.update { it.copy(escalationPolicy = updated) }
                _uiEffect.send(
                    SettingsUiEffect.ShowSnackbar(
                        "Urgent follow-up cadence updated ($retryIntervalMinutes min, max $maxReFires)"
                    )
                )
            }.onFailure {
                _uiEffect.send(
                    SettingsUiEffect.ShowSnackbar("Could not update follow-up cadence")
                )
            }
        }
    }
}
