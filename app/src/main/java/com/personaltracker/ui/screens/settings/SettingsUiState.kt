package com.personaltracker.ui.screens.settings

import com.personaltracker.domain.model.DayOverride
import com.personaltracker.domain.model.DayType

/**
 * Immutable UI state for Settings and day template management (AD-2).
 */
data class SettingsUiState(
    val allDayTypes: List<DayType> = emptyList(),
    val activeDayType: DayType? = null,
    val dateOverride: DayOverride? = null,
    val isExportingBackup: Boolean = false,
    val backupExportSuccess: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
