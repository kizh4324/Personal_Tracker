package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a baseline schedule template (FR-3, AD-1).
 */
data class DayType(
    val id: String,
    val name: String,
    val colorHex: String,
    val targetStudyMinutes: Int,
    val isDefault: Boolean,
    val defaultDaysOfWeek: String
) {
    companion object {
        val STANDARD = DayType(
            id = "dt_standard",
            name = "Standard Routine",
            colorHex = "#3B82F6",
            targetStudyMinutes = 90,
            isDefault = true,
            defaultDaysOfWeek = "1,2,3,4,5"
        )

        val REST = DayType(
            id = "dt_rest",
            name = "Rest / Recovery",
            colorHex = "#10B981",
            targetStudyMinutes = 30,
            isDefault = false,
            defaultDaysOfWeek = "6,7"
        )

        val CRUNCH = DayType(
            id = "dt_crunch",
            name = "Crunch Focus",
            colorHex = "#8B5CF6",
            targetStudyMinutes = 180,
            isDefault = false,
            defaultDaysOfWeek = ""
        )

        val LOW_CAPACITY = DayType(
            id = "dt_low_capacity",
            name = "Low Capacity",
            colorHex = "#F59E0B",
            targetStudyMinutes = 20,
            isDefault = false,
            defaultDaysOfWeek = ""
        )

        val BUILT_IN_DAY_TYPES: List<DayType> = listOf(
            STANDARD,
            REST,
            CRUNCH,
            LOW_CAPACITY
        )
    }
}
