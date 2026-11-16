package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a shop item (cosmetic or consumable utility) (FR-8.4, AD-1).
 */
data class ShopItem(
    val id: String,
    val category: String, // CATEGORY_OUTFIT, CATEGORY_ACCESSORY, CATEGORY_BACKGROUND_DECOR, CATEGORY_UTILITY
    val name: String,
    val priceCoins: Int,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null
) {
    val iconEmoji: String get() = when (id) {
        "item_scarf" -> "🧣"
        "item_cap" -> "🧢"
        "item_crown" -> "👑"
        "item_coffee" -> "☕"
        "item_streak_shield" -> "🛡️"
        else -> "✨"
    }

    companion object {
        const val CATEGORY_OUTFIT = "OUTFIT"
        const val CATEGORY_ACCESSORY = "ACCESSORY"
        const val CATEGORY_BACKGROUND_DECOR = "BACKGROUND_DECOR"
        const val CATEGORY_UTILITY = "UTILITY"
    }
}
