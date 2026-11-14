package com.personaltracker.domain.model

/**
 * Canonical event type constants for coin ledger transactions (AD-1, AD-6, FR-8.2).
 */
object EconomyEventTypes {
    const val TASK_COMPLETION = "TASK_COMPLETION"
    const val HABIT_COMPLETION = "HABIT_COMPLETION"
    const val STUDY_SESSION = "STUDY_SESSION"
    const val ROUTINE_COMPLETION = "ROUTINE_COMPLETION"
    const val CAPTURE_REVERSAL = "CAPTURE_REVERSAL"
    const val SHOP_PURCHASE = "SHOP_PURCHASE"
}
