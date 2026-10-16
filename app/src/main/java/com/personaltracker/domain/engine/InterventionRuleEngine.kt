package com.personaltracker.domain.engine

import com.personaltracker.domain.model.InterventionRule

/**
 * Pure Kotlin behavioral engine managing JITAI distraction evaluation, single-prompt cooldowns,
 * and urgency tier scarcity guidance (FR-4.2, FR-4.3, FR-5.2, FR-10.1, AD-1, AD-3).
 *
 * Contains zero Android framework dependencies and zero nagging/punitive copy.
 */
class InterventionRuleEngine {

    companion object {
        const val DEFAULT_COOLDOWN_MILLIS = 5 * 60 * 1000L // 5 minutes
        const val RECOMMENDED_MAX_URGENT_TASKS = 2
    }

    data class InterventionDecision(
        val shouldBlock: Boolean,
        val shouldPromptJitai: Boolean,
        val matchedRule: InterventionRule?,
        val reason: String
    )

    data class UrgencyAssessment(
        val isScarcityExceeded: Boolean,
        val advisoryMessage: String?,
        val recommendedIntensity: String
    )

    /**
     * Evaluates a foreground app access against configured intervention rules and cooldown state.
     *
     * @param packageName Foreground package opened by user
     * @param rules Configured distraction rules
     * @param lastPromptTimestamp Timestamp in epoch millis when JITAI was last prompted for this app/window
     * @param currentTimestamp Current epoch millis
     * @param cooldownMillis Cooldown window duration (default 5 minutes)
     */
    fun evaluateAppIntervention(
        packageName: String,
        rules: List<InterventionRule>,
        lastPromptTimestamp: Long?,
        currentTimestamp: Long = System.currentTimeMillis(),
        cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS
    ): InterventionDecision {
        val matchedRule = rules.find { it.packageName.equals(packageName, ignoreCase = true) }

        if (matchedRule == null) {
            return InterventionDecision(
                shouldBlock = false,
                shouldPromptJitai = false,
                matchedRule = null,
                reason = "App is not registered as a distraction target."
            )
        }

        val isCooldownActive = lastPromptTimestamp != null &&
            (currentTimestamp - lastPromptTimestamp) in 0 until cooldownMillis

        return if (isCooldownActive) {
            // Prompt suppressed to prevent nagging the user
            InterventionDecision(
                shouldBlock = matchedRule.isStrictBlocked,
                shouldPromptJitai = false,
                matchedRule = matchedRule,
                reason = "Prompt suppressed: JITAI cooldown window active."
            )
        } else {
            // Cooldown expired or first time trigger
            InterventionDecision(
                shouldBlock = matchedRule.isStrictBlocked,
                shouldPromptJitai = true,
                matchedRule = matchedRule,
                reason = "Intervention active: JITAI prompt permitted."
            )
        }
    }

    /**
     * Evaluates urgency tier capacity according to the scarcity principle (FR-5.2, FR-5.3).
     * Soft guidance prompt only — NEVER hard-blocks and NEVER silently downgrades.
     *
     * @param currentUrgentCount Number of active urgent tasks already scheduled for the day
     * @param maxRecommended Limit before advisory is triggered (default 2)
     */
    fun evaluateUrgencyCapacity(
        currentUrgentCount: Int,
        maxRecommended: Int = RECOMMENDED_MAX_URGENT_TASKS
    ): UrgencyAssessment {
        require(currentUrgentCount >= 0) { "currentUrgentCount must be non-negative: $currentUrgentCount" }

        return if (currentUrgentCount >= maxRecommended) {
            UrgencyAssessment(
                isScarcityExceeded = true,
                advisoryMessage = "You already have $currentUrgentCount Urgent items today. Keep Urgent or change to Important?",
                recommendedIntensity = "IMPORTANT"
            )
        } else {
            UrgencyAssessment(
                isScarcityExceeded = false,
                advisoryMessage = null,
                recommendedIntensity = "URGENT"
            )
        }
    }
}
