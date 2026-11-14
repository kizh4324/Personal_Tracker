package com.personaltracker.domain.model

/**
 * Pure Kotlin data model aggregating daily coin earnings across categories and tracking cap limits (AD-1, FR-8.2).
 */
data class DailyEconomySummary(
    val taskCoins: Int,
    val habitCoins: Int,
    val focusCoins: Int,
    val routineCoins: Int,
    val totalEarnedToday: Int,
    val currentBalance: Int,
    val isTaskCapReached: Boolean,
    val isHabitCapReached: Boolean,
    val isFocusCapReached: Boolean,
    val isGlobalFarmableCapReached: Boolean
)
