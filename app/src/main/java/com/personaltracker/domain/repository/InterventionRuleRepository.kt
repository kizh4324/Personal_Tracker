package com.personaltracker.domain.repository

import com.personaltracker.domain.model.InterventionRule
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for focus intervention and app blocking rules (FR-4.3, FR-10.1, AD-1).
 */
interface InterventionRuleRepository {
    fun getBlockedAppRules(): Flow<List<InterventionRule>>
    suspend fun getRuleForPackage(packageName: String): InterventionRule?
    suspend fun saveRule(rule: InterventionRule)
    suspend fun deleteRule(packageName: String)
}
