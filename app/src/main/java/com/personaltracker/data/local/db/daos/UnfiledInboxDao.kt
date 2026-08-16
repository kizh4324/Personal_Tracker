package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personaltracker.data.local.db.entities.InterventionRuleEntity
import com.personaltracker.data.local.db.entities.UnfiledCaptureInboxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Unfiled Capture Inbox.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface UnfiledInboxDao {

    @Query("SELECT * FROM unfiled_capture_inbox ORDER BY createdTimestamp DESC")
    fun getAllInboxItems(): Flow<List<UnfiledCaptureInboxEntity>>

    @Query("SELECT * FROM unfiled_capture_inbox WHERE id = :id LIMIT 1")
    suspend fun getInboxItemById(id: String): UnfiledCaptureInboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInboxItem(item: UnfiledCaptureInboxEntity)

    @Delete
    suspend fun deleteInboxItem(item: UnfiledCaptureInboxEntity)
}

/**
 * Data Access Object for Focus Intervention Rules.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface InterventionRuleDao {

    @Query("SELECT * FROM intervention_rules WHERE isStrictBlocked = 1")
    fun getBlockedAppRules(): Flow<List<InterventionRuleEntity>>

    @Query("SELECT * FROM intervention_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): InterventionRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: InterventionRuleEntity)

    @Query("DELETE FROM intervention_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
