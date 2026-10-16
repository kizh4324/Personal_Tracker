package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a cosmetic shop item (FR-8.4, AD-1).
 */
data class ShopItem(
    val id: String,
    val category: String, // "OUTFIT", "ACCESSORY", "BACKGROUND_DECOR"
    val name: String,
    val priceCoins: Int,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null
)
