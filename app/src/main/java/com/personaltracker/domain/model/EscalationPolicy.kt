package com.personaltracker.domain.model

/**
 * Pure Kotlin configurable policy governing bounded follow-up reminders for URGENT tasks (AD-1, AD-4, FR-5.4).
 *
 * @property retryIntervalMinutes Minutes between follow-up reminders (valid range: 1..60).
 * @property maxReFires Maximum number of follow-up reminders after the initial trigger (valid range: 0..5).
 */
data class EscalationPolicy(
    val retryIntervalMinutes: Int = 5,
    val maxReFires: Int = 3
) {
    init {
        require(retryIntervalMinutes in 1..60) {
            "retryIntervalMinutes must be within 1..60, got $retryIntervalMinutes"
        }
        require(maxReFires in 0..5) {
            "maxReFires must be within 0..5, got $maxReFires"
        }
    }
}
