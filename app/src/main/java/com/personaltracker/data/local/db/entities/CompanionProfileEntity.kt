package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton companion pet state entity (FR-8.1, FR-8.4).
 */
@Entity(tableName = "companion_profile")
data class CompanionProfileEntity(
    @PrimaryKey val id: Int = 1, // Singleton row
    val speciesName: String,
    val level: Int,
    val currentEnergyUnits: Int,
    val activeAccessoryId: String?,
    val activeBackgroundId: String?,
    val currentExpressionOrdinal: Int // Mapped 0..6 to CompanionState
)

/**
 * Local cosmetic shop catalog entity (FR-8.4).
 */
@Entity(tableName = "shop_items")
data class ShopItemEntity(
    @PrimaryKey val id: String,
    val category: String, // "OUTFIT", "ACCESSORY", "BACKGROUND_DECOR"
    val name: String,
    val priceCoins: Int,
    val isUnlocked: Boolean,
    val unlockedTimestamp: Long?
)
