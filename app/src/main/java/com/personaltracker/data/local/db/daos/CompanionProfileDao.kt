package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.personaltracker.data.local.db.entities.CompanionProfileEntity
import com.personaltracker.data.local.db.entities.ShopItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Companion Profile state.
 * Enforces singleton row with id = 1.
 */
@Dao
internal interface CompanionProfileDao {

    @Query("SELECT * FROM companion_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): CompanionProfileEntity?

    @Query("SELECT * FROM companion_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<CompanionProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: CompanionProfileEntity)

    suspend fun insertOrUpdateProfile(profile: CompanionProfileEntity) {
        upsertProfile(if (profile.id == 1) profile else profile.copy(id = 1))
    }
}

/**
 * Data Access Object for Local Cosmetic Shop items.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface ShopItemDao {

    @Query("SELECT * FROM shop_items ORDER BY priceCoins ASC")
    fun getAllShopItems(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_items WHERE id = :id LIMIT 1")
    suspend fun getShopItemById(id: String): ShopItemEntity?

    @Upsert
    suspend fun insertShopItems(items: List<ShopItemEntity>)

    @Update
    suspend fun updateShopItem(item: ShopItemEntity)
}
