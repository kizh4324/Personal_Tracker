package com.personaltracker.ui.screens.settings

/**
 * Sealed interface representing all user actions on the Settings screen (AD-2).
 */
sealed interface SettingsUiIntent {
    data class SelectDayTypeOverride(val dayTypeId: String) : SettingsUiIntent
    object ClearDayTypeOverride : SettingsUiIntent
    object ExportEncryptedBackup : SettingsUiIntent
    object Refresh : SettingsUiIntent
}
