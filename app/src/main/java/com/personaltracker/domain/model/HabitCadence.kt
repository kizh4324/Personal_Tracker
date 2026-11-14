package com.personaltracker.domain.model

/**
 * Cadence types supported for habit tracking (FR-2.4, AD-1).
 */
enum class CadenceType {
    DAILY,
    SPECIFIC_DAYS,
    WEEKLY_FREQUENCY
}

/**
 * Pure Kotlin sealed hierarchy representing habit repetition schedules.
 *
 * Days of week use ISO-8601 numbering: 1 = Monday .. 7 = Sunday.
 */
sealed class HabitCadence {
    abstract val type: CadenceType

    /**
     * Every day repetition schedule.
     */
    data object Daily : HabitCadence() {
        override val type: CadenceType = CadenceType.DAILY
    }

    /**
     * Specific days of the week schedule (e.g. Mon/Wed/Fri = {1, 3, 5}).
     */
    data class SpecificDays(val daysOfWeek: Set<Int>) : HabitCadence() {
        override val type: CadenceType = CadenceType.SPECIFIC_DAYS

        init {
            require(daysOfWeek.all { it in 1..7 }) {
                "daysOfWeek elements must be within 1..7 (ISO-8601 Mon-Sun): $daysOfWeek"
            }
        }

        fun isScheduledOn(dayOfWeek: Int): Boolean = daysOfWeek.contains(dayOfWeek)
    }

    /**
     * Flexible weekly frequency schedule (e.g. 3 times per week).
     */
    data class WeeklyFrequency(val targetTimesPerWeek: Int) : HabitCadence() {
        override val type: CadenceType = CadenceType.WEEKLY_FREQUENCY

        init {
            require(targetTimesPerWeek in 1..7) {
                "targetTimesPerWeek must be between 1 and 7: $targetTimesPerWeek"
            }
        }
    }
}
