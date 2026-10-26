package com.personaltracker.ui.screens.settings

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType

/**
 * Immutable UI state for Settings, template management, and permission readiness (AD-2).
 */
data class SettingsUiState(
    val allDayTypes: List<DayType> = emptyList(),
    val activeDayType: DayType? = null,
    val dateOverride: DayOverride? = null,
    val isExportingBackup: Boolean = false,
    val backupExportSuccess: Boolean? = null,
    val isAccessibilityGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
