package com.personaltracker.domain.engine

import com.personaltracker.domain.model.EscalationPolicy

/**
 * Result of evaluating whether an URGENT task reminder should schedule a bounded follow-up (AD-1, FR-5.4).
 */
data class EscalationDecision(
    val shouldScheduleNext: Boolean,
    val nextAttemptNumber: Int = 0,
    val nextTriggerEpochMillis: Long = 0L,
    val haltReason: String? = null
)

/**
 * Pure Kotlin deterministic engine computing bounded follow-up schedules for unacknowledged URGENT tasks (AD-1, AD-3, FR-5.4).
 *
 * Initial notification delivery is attempt `0`. Follow-ups are strictly bounded by `currentAttemptNumber < policy.maxReFires`.
 * Non-URGENT tasks (`ROUTINE`, `IMPORTANT`), non-PENDING tasks, and acknowledged tasks never schedule follow-ups.
 */
class BoundedEscalationEngine {

    /**
     * Evaluates whether a follow-up alarm should be scheduled after [currentAttemptNumber] fires.
     */
    fun evaluateNextEscalation(
        taskId: String,
        deliveryIntensity: String,
        taskState: String,
        currentAttemptNumber: Int,
        isAcknowledged: Boolean,
        currentEpochMillis: Long,
        policy: EscalationPolicy = EscalationPolicy()
    ): EscalationDecision {
        if (taskId.isBlank()) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "BLANK_TASK_ID")
        }
        if (!deliveryIntensity.equals("URGENT", ignoreCase = true)) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "NON_URGENT_INTENSITY")
        }
        if (!taskState.equals("PENDING", ignoreCase = true)) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "TASK_NOT_PENDING")
        }
        if (isAcknowledged) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "ACKNOWLEDGED")
        }
        if (currentAttemptNumber < 0) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "INVALID_ATTEMPT")
        }
        if (currentAttemptNumber >= policy.maxReFires) {
            return EscalationDecision(shouldScheduleNext = false, haltReason = "MAX_RE_FIRES_REACHED")
        }

        val nextAttempt = currentAttemptNumber + 1
        val nextTriggerEpochMillis = currentEpochMillis + (policy.retryIntervalMinutes * MILLIS_PER_MINUTE)
        return EscalationDecision(
            shouldScheduleNext = true,
            nextAttemptNumber = nextAttempt,
            nextTriggerEpochMillis = nextTriggerEpochMillis,
            haltReason = null
        )
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
