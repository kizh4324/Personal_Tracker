package com.personaltracker.domain.engine

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pure Kotlin behavioral engine managing study session pause/resume recalculation,
 * break overrun adjustments, and non-punitive schedule shifting (FR-4.1, FR-4.5, AD-1, AD-3).
 *
 * Contains zero Android framework dependencies and zero guilt copy.
 */
class FocusRescheduleEngine {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    data class BreakAdjustment(
        val plannedBreakMinutes: Int,
        val actualBreakMinutes: Int,
        val overrunMinutes: Int,
        val remainingFocusMinutes: Int,
        val originalEndTime: String, // "HH:mm"
        val adjustedEndTime: String, // "HH:mm"
        val advisoryMessage: String
    )

    data class PauseAdjustment(
        val remainingMinutes: Int,
        val pauseDurationMinutes: Int,
        val originalEndTime: String, // "HH:mm"
        val adjustedEndTime: String, // "HH:mm"
        val advisoryMessage: String
    )

    data class InterruptionSummary(
        val plannedDurationMinutes: Int,
        val elapsedMinutes: Int,
        val remainingMinutes: Int,
        val advisoryMessage: String
    )

    /**
     * Recalculates remaining session timeline when a planned break overruns.
     *
     * @param plannedSessionDuration Total planned focus session minutes (e.g. 25)
     * @param elapsedFocusMinutes Focus minutes completed prior to break (e.g. 10)
     * @param plannedBreakMinutes Planned break duration in minutes (e.g. 5)
     * @param actualBreakMinutes Total minutes taken during the break (e.g. 15)
     * @param scheduledEndTime Original scheduled end time "HH:mm"
     * @return BreakAdjustment with non-punitive adjusted end time and calm advisory copy
     */
    fun calculateBreakOverrun(
        plannedSessionDuration: Int,
        elapsedFocusMinutes: Int,
        plannedBreakMinutes: Int,
        actualBreakMinutes: Int,
        scheduledEndTime: String
    ): BreakAdjustment {
        require(plannedSessionDuration > 0) { "plannedSessionDuration must be positive" }
        require(elapsedFocusMinutes >= 0) { "elapsedFocusMinutes must be non-negative" }
        require(plannedBreakMinutes >= 0) { "plannedBreakMinutes must be non-negative" }
        require(actualBreakMinutes >= 0) { "actualBreakMinutes must be non-negative" }

        val overrunMinutes = maxOf(0, actualBreakMinutes - plannedBreakMinutes)
        val remainingFocusMinutes = maxOf(0, plannedSessionDuration - elapsedFocusMinutes)

        val baseEnd = runCatching {
            LocalTime.parse(scheduledEndTime, timeFormatter)
        }.getOrDefault(LocalTime.of(12, 0))
        val adjustedEnd = baseEnd.plusMinutes(overrunMinutes.toLong())

        val message = if (overrunMinutes > 0) {
            "Break finished. End time adjusted by $overrunMinutes min. Ready when you are."
        } else {
            "Break complete. $remainingFocusMinutes minutes remaining in your session."
        }

        return BreakAdjustment(
            plannedBreakMinutes = plannedBreakMinutes,
            actualBreakMinutes = actualBreakMinutes,
            overrunMinutes = overrunMinutes,
            remainingFocusMinutes = remainingFocusMinutes,
            originalEndTime = scheduledEndTime,
            adjustedEndTime = adjustedEnd.format(timeFormatter),
            advisoryMessage = message
        )
    }

    /**
     * Recalculates end time when a focus session is paused and resumed.
     *
     * @param remainingMinutes Focus minutes still remaining in block
     * @param pauseDurationMinutes Duration the session was paused
     * @param currentScheduledEndTime Current scheduled end time "HH:mm"
     */
    fun calculatePauseAdjustment(
        remainingMinutes: Int,
        pauseDurationMinutes: Int,
        currentScheduledEndTime: String
    ): PauseAdjustment {
        require(remainingMinutes >= 0) { "remainingMinutes must be non-negative" }
        require(pauseDurationMinutes >= 0) { "pauseDurationMinutes must be non-negative" }

        val baseEnd = runCatching {
            LocalTime.parse(currentScheduledEndTime, timeFormatter)
        }.getOrDefault(LocalTime.of(12, 0))
        val adjustedEnd = baseEnd.plusMinutes(pauseDurationMinutes.toLong())

        return PauseAdjustment(
            remainingMinutes = remainingMinutes,
            pauseDurationMinutes = pauseDurationMinutes,
            originalEndTime = currentScheduledEndTime,
            adjustedEndTime = adjustedEnd.format(timeFormatter),
            advisoryMessage = "Session paused. End time adjusted calmly to ${adjustedEnd.format(timeFormatter)}."
        )
    }

    /**
     * Summarizes session status upon unexpected early exit or interruption (Ovsiankina effect).
     */
    fun calculateInterruption(
        plannedDurationMinutes: Int,
        elapsedMinutes: Int
    ): InterruptionSummary {
        require(plannedDurationMinutes >= 0) { "plannedDurationMinutes must be non-negative" }
        require(elapsedMinutes >= 0) { "elapsedMinutes must be non-negative" }

        val remainingMinutes = maxOf(0, plannedDurationMinutes - elapsedMinutes)

        return InterruptionSummary(
            plannedDurationMinutes = plannedDurationMinutes,
            elapsedMinutes = elapsedMinutes,
            remainingMinutes = remainingMinutes,
            advisoryMessage = "Focus block paused with $remainingMinutes min remaining. You can resume anytime."
        )
    }
}
