package com.personaltracker.ui.screens.companion

import com.personaltracker.domain.model.ShopItem
import com.personaltracker.ui.components.companion.CompanionState

/**
 * Sealed interface representing all user interactions with the companion pet (AD-2, AD-4).
 */
sealed interface CompanionUiIntent {
    data class SelectMood(val mood: CompanionState) : CompanionUiIntent
    data class PurchaseItem(val item: ShopItem) : CompanionUiIntent
    object TriggerMilestoneCelebration : CompanionUiIntent
    object TriggerCoinMicroBurst : CompanionUiIntent
    object Refresh : CompanionUiIntent
}
