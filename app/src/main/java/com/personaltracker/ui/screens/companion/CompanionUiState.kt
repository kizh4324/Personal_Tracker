package com.personaltracker.ui.screens.companion

import com.personaltracker.domain.model.ShopItem
import com.personaltracker.ui.components.companion.CompanionState

/**
 * Single immutable UI state representing the Companion surface (AD-2, AD-4).
 * Enforces non-punitive mechanics with zero HP loss, starvation, or decay.
 */
data class CompanionUiState(
    val speciesName: String = "Sproutling",
    val level: Int = 1,
    val currentEnergyUnits: Int = 0,
    val maxEnergyUnits: Int = 100,
    val energyProgress: Float = 0f,
    val coinBalance: Int = 0,
    val mood: CompanionState = CompanionState.Idle,
    val availableShopItems: List<ShopItem> = emptyList(),
    val activeAccessoryId: String? = null,
    val activeBackgroundId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
