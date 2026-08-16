package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personaltracker.data.local.db.entities.DayOverrideEntity
import com.personaltracker.data.local.db.entities.DayTypeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DayType schedule templates.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface DayTypeDao {

    @Query("SELECT * FROM day_types WHERE id = :id LIMIT 1")
    suspend fun getDayTypeById(id: String): DayTypeEntity?

    @Query("SELECT * FROM day_types")
    fun getAllDayTypes(): Flow<List<DayTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayType(dayType: DayTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayTypes(dayTypes: List<DayTypeEntity>)
}

/**
 * Data Access Object for single-date user DayType overrides.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface DayOverrideDao {

    @Query("SELECT * FROM day_overrides WHERE date = :date LIMIT 1")
    suspend fun getOverrideForDate(date: String): DayOverrideEntity?

    @Query("SELECT * FROM day_overrides WHERE date = :date LIMIT 1")
    fun observeOverrideForDate(date: String): Flow<DayOverrideEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOverride(override: DayOverrideEntity)

    @Query("DELETE FROM day_overrides WHERE date = :date")
    suspend fun deleteOverrideForDate(date: String)
}
