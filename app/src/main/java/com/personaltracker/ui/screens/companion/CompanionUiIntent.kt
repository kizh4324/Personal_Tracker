package com.personaltracker.ui.screens.companion

import com.personaltracker.domain.engine.CompanionState
import com.personaltracker.domain.model.ShopItem

/**
 * Sealed interface representing all user interactions with the companion pet (AD-2, AD-4).
 */
sealed interface CompanionUiIntent {
    data class SelectMood(val mood: CompanionState) : CompanionUiIntent
    data class PurchaseOrEquipItem(val item: ShopItem) : CompanionUiIntent
    data class PurchaseItem(val item: ShopItem) : CompanionUiIntent
    data class EquipItem(val item: ShopItem, val equip: Boolean) : CompanionUiIntent
    object TriggerMilestoneCelebration : CompanionUiIntent
    object TriggerCoinMicroBurst : CompanionUiIntent
    object Refresh : CompanionUiIntent
}
