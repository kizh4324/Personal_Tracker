package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing the singleton non-punitive companion pet state (FR-8.1, FR-8.4, AD-1, AD-4).
 */
data class CompanionProfile(
    val id: Int = 1,
    val speciesName: String,
    val level: Int = 1,
    val currentEnergyUnits: Int = 0,
    val activeAccessoryId: String? = null,
    val activeBackgroundId: String? = null,
    val currentExpressionOrdinal: Int = 0
)
