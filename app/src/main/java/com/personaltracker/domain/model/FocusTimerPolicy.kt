package com.personaltracker.domain.model

/**
 * Pure Kotlin enum specifying focus countdown timer progression policy
 * during active telecom or emergency calls (FR-4.6, AD-1).
 */
enum class FocusTimerPolicy(val displayName: String) {
    AUTO_PAUSE("Auto-pause timer"),
    CONTINUE_COUNTDOWN("Continue countdown")
}
