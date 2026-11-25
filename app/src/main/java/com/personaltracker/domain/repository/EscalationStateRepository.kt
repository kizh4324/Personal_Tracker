package com.personaltracker.domain.repository

import com.personaltracker.domain.model.EscalationPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin contract managing configurable escalation cadence policies and per-task acknowledgement state (AD-1, FR-5.4).
 */
interface EscalationStateRepository {
    fun observePolicy(): Flow<EscalationPolicy>
    fun getPolicy(): EscalationPolicy
    fun updatePolicy(policy: EscalationPolicy)
    fun isAcknowledged(taskId: String): Boolean
    fun markAcknowledged(taskId: String)
    fun clearAcknowledgement(taskId: String)
}
