package com.personaltracker.ui.screens.settings

/**
 * Sealed interface representing one-shot side effects emitted by SettingsViewModel (AD-2).
 */
sealed interface SettingsUiEffect {
    data class ShowSnackbar(val message: String) : SettingsUiEffect
    data class OpenSystemSetting(val action: String) : SettingsUiEffect
}
