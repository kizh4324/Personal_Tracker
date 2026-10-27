package com.personaltracker.ui.screens.settings

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.domain.model.InterventionRule

/**
 * Immutable UI state for Settings, template management, focus interception permissions,
 * and emergency / telecom whitelist configurations (AD-2, FR-4.6, FR-10.1).
 */
data class SettingsUiState(
    val allDayTypes: List<DayType> = emptyList(),
    val activeDayType: DayType? = null,
    val dateOverride: DayOverride? = null,
    val isExportingBackup: Boolean = false,
    val backupExportSuccess: Boolean? = null,
    val isAccessibilityGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val whitelistedRules: List<InterventionRule> = emptyList(),
    val allInterventionRules: List<InterventionRule> = emptyList(),
    val focusTimerPolicy: FocusTimerPolicy = FocusTimerPolicy.AUTO_PAUSE,
    val isWhitelistSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
