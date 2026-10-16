package com.personaltracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Focus session app intervention rule configuration entity (FR-4.3, FR-10.1).
 */
@Entity(tableName = "intervention_rules")
data class InterventionRuleEntity(
    @PrimaryKey val packageName: String, // e.g., "com.instagram.android"
    val appDisplayName: String,
    val isStrictBlocked: Boolean,
    val dailyLimitMinutes: Int?
)
