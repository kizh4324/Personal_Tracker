package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.personaltracker.data.local.db.entities.InterventionRuleEntity
import kotlinx.coroutines.flow.Flow

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

    @Upsert
    suspend fun insertOrUpdateRule(rule: InterventionRuleEntity)

    @Query("DELETE FROM intervention_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
