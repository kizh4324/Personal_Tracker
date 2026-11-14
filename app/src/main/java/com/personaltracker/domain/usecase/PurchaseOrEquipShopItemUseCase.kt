package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.repository.EconomyRepository
import javax.inject.Inject

sealed interface PurchaseOrEquipResult {
    data class PurchasedAndEquipped(val item: ShopItem, val newBalance: Int) : PurchaseOrEquipResult
    data class Equipped(val item: ShopItem) : PurchaseOrEquipResult
    data class Unequipped(val item: ShopItem) : PurchaseOrEquipResult
    data class InsufficientCoins(val required: Int, val current: Int) : PurchaseOrEquipResult
    data class Error(val message: String) : PurchaseOrEquipResult
}

/**
 * Pure Kotlin use case coordinating atomic cosmetics purchase validation,
 * coin deduction, and equip/unequip toggling (AD-1, AD-4, AD-6).
 */
class PurchaseOrEquipShopItemUseCase @Inject constructor(
    private val economyRepository: EconomyRepository
) {
    suspend operator fun invoke(item: ShopItem, equipOnlyToggle: Boolean = false): PurchaseOrEquipResult {
        return try {
            val profile = economyRepository.getCompanionProfile()
            val currentlyEquipped = profile?.activeAccessoryId == item.id

            if (item.isUnlocked || equipOnlyToggle) {
                val shouldEquip = !currentlyEquipped
                economyRepository.equipShopItem(item.id, shouldEquip)
                if (shouldEquip) {
                    PurchaseOrEquipResult.Equipped(item)
                } else {
                    PurchaseOrEquipResult.Unequipped(item)
                }
            } else {
                val currentBalance = economyRepository.getBalance()
                if (currentBalance < item.priceCoins) {
                    return PurchaseOrEquipResult.InsufficientCoins(
                        required = item.priceCoins,
                        current = currentBalance
                    )
                }
                val idempotencyKey = "SHOP_BUY_${item.id}"
                economyRepository.purchaseShopItem(item.id, idempotencyKey)
                val newBalance = economyRepository.getBalance()
                val unlockedItem = item.copy(
                    isUnlocked = true,
                    unlockedTimestamp = System.currentTimeMillis()
                )
                PurchaseOrEquipResult.PurchasedAndEquipped(unlockedItem, newBalance)
            }
        } catch (e: Exception) {
            PurchaseOrEquipResult.Error(e.message ?: "Could not complete transaction")
        }
    }
}
