package com.personaltracker.domain.model

/**
 * Pure Kotlin domain model representing a habit with non-punitive streak tracking (FR-2.4, AD-1, AD-4).
 */
data class Habit(
    val id: String,
    val name: String,
    val description: String? = null,
    val targetDaysOfWeek: String, // e.g. "1,2,3,4,5,6,7"
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val slackBankDays: Int = 0,
    val isFreezeActive: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val cadenceType: CadenceType = CadenceType.DAILY,
    val weeklyTargetFrequency: Int = 7,
    val reminderTime: String? = null,
    val isReminderEnabled: Boolean = false,
    val goalTargetCount: Int = 1,
    val category: String? = null
) {
    /**
     * Resolves the 4-tier streak state for reward bonus calculations.
     */
    val streakTier: StreakTier
        get() = StreakTier.fromStreak(currentStreak)

    /**
     * Resolves the polymorphic [HabitCadence] representation from the habit's configuration.
     */
    val cadence: HabitCadence
        get() = when (cadenceType) {
            CadenceType.DAILY -> HabitCadence.Daily
            CadenceType.SPECIFIC_DAYS -> {
                val days = targetDaysOfWeek.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..7 }
                    .toSet()
                HabitCadence.SpecificDays(if (days.isNotEmpty()) days else (1..7).toSet())
            }
            CadenceType.WEEKLY_FREQUENCY -> HabitCadence.WeeklyFrequency(
                weeklyTargetFrequency.coerceIn(1, 7)
            )
        }
}
