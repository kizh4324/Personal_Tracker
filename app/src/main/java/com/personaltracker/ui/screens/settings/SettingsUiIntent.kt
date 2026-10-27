package com.personaltracker.ui.screens.settings

import com.personaltracker.domain.model.FocusTimerPolicy

/**
 * Sealed interface representing all user actions on the Settings screen (AD-2).
 */
sealed interface SettingsUiIntent {
    data class SelectDayTypeOverride(val dayTypeId: String) : SettingsUiIntent
    object ClearDayTypeOverride : SettingsUiIntent
    object ExportEncryptedBackup : SettingsUiIntent
    object CheckPermissions : SettingsUiIntent
    object OpenAccessibilitySettings : SettingsUiIntent
    object OpenOverlaySettings : SettingsUiIntent
    object Refresh : SettingsUiIntent

    // Whitelist and Call Policy Intents (Story 3.3)
    data class ToggleAppWhitelist(val packageName: String, val appDisplayName: String? = null) : SettingsUiIntent
    data class AddCustomWhitelistPackage(val packageName: String, val appDisplayName: String) : SettingsUiIntent
    data class RemoveWhitelistPackage(val packageName: String) : SettingsUiIntent
    data class SetFocusTimerPolicy(val policy: FocusTimerPolicy) : SettingsUiIntent
    data class ToggleWhitelistSheet(val isOpen: Boolean) : SettingsUiIntent
}
