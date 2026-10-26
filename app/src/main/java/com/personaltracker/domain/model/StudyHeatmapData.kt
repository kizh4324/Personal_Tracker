package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a single daily cell in the contribution heatmap (FR-4.2, AD-1).
 */
data class HeatmapCell(
    val date: String, // "YYYY-MM-DD"
    val totalMinutes: Int,
    val intensityTier: Int, // 0 to 4
    val subjectBreakdown: Map<String, Int> = emptyMap(),
    val sessionCount: Int = 0
)

/**
 * Pure Kotlin domain model representing the aggregated 28-day study contribution heatmap (FR-4.2, AD-1).
 */
data class StudyHeatmapData(
    val cells: List<HeatmapCell> = emptyList(),
    val totalFocusMinutes: Int = 0,
    val activeDaysCount: Int = 0,
    val subjectBreakdown: Map<String, Int> = emptyMap(),
    val topSubject: String? = null
)
