package com.personaltracker.domain.engine

import com.personaltracker.domain.model.HeatmapCell
import com.personaltracker.domain.model.StudyHeatmapData
import com.personaltracker.domain.model.StudySession
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure Kotlin behavioral engine that aggregates historical study and focus sessions
 * into structured 5-tier contribution heatmaps and subject rollups (FR-4.2, AD-1, AD-3).
 *
 * Implements deterministic intensity tier classification:
 * - Tier 0: 0 minutes
 * - Tier 1: 1–29 minutes
 * - Tier 2: 30–59 minutes
 * - Tier 3: 60–119 minutes
 * - Tier 4: 120+ minutes
 *
 * Contains zero Android framework dependencies and zero guilt copy (AD-1, AD-4).
 */
class HeatmapLedgerEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Resolves the 5-tier intensity level for a given focus duration in minutes.
     */
    fun resolveTier(minutes: Int): Int {
        val safeMinutes = minutes.coerceAtLeast(0)
        return when {
            safeMinutes == 0 -> 0
            safeMinutes in 1..29 -> 1
            safeMinutes in 30..59 -> 2
            safeMinutes in 60..119 -> 3
            else -> 4
        }
    }

    /**
     * Aggregates completed and in-progress focus sessions into a 28-day chronological contribution heatmap.
     *
     * @param sessions All logged study sessions from repository
     * @param referenceDate End date of the 28-day window (defaults to today)
     * @param days Total days in the rolling heatmap window (defaults to 28)
     * @return [StudyHeatmapData] with 28 chronological cells, subject breakdown, and summary statistics.
     */
    fun calculateHeatmap(
        sessions: List<StudySession>,
        referenceDate: LocalDate = LocalDate.now(),
        days: Int = 28
    ): StudyHeatmapData {
        require(days > 0) { "days must be positive: $days" }

        // Filter out breaks (breaks do not count towards focus minutes)
        val focusSessions = sessions.filter { !it.isBreak }

        // Group sessions by date
        val dailySessionMap = mutableMapOf<String, MutableList<StudySession>>()
        for (session in focusSessions) {
            dailySessionMap.getOrPut(session.sessionDate) { mutableListOf() }.add(session)
        }

        val startDate = referenceDate.minusDays((days - 1).toLong())
        val cells = mutableListOf<HeatmapCell>()
        val overallSubjectBreakdown = mutableMapOf<String, Int>()
        var overallFocusMinutes = 0
        var activeDays = 0

        var currentDate = startDate
        while (!currentDate.isAfter(referenceDate)) {
            val dateStr = currentDate.format(dateFormatter)
            val daySessions = dailySessionMap[dateStr] ?: emptyList()

            var dayMinutes = 0
            val daySubjectMap = mutableMapOf<String, Int>()

            for (session in daySessions) {
                val sessionMins = when {
                    session.actualElapsedMinutes > 0 -> session.actualElapsedMinutes
                    session.durationMinutes > 0 -> session.durationMinutes
                    else -> 0
                }.coerceAtLeast(0)

                // Safe addition to prevent overflow
                dayMinutes = try {
                    Math.addExact(dayMinutes, sessionMins)
                } catch (_: ArithmeticException) {
                    Int.MAX_VALUE
                }

                val tag = session.subjectTag.ifBlank { "Focus" }
                val currentTagMins = daySubjectMap[tag] ?: 0
                daySubjectMap[tag] = try {
                    Math.addExact(currentTagMins, sessionMins)
                } catch (_: ArithmeticException) {
                    Int.MAX_VALUE
                }

                val overallTagMins = overallSubjectBreakdown[tag] ?: 0
                overallSubjectBreakdown[tag] = try {
                    Math.addExact(overallTagMins, sessionMins)
                } catch (_: ArithmeticException) {
                    Int.MAX_VALUE
                }
            }

            if (dayMinutes > 0) {
                activeDays++
            }

            overallFocusMinutes = try {
                Math.addExact(overallFocusMinutes, dayMinutes)
            } catch (_: ArithmeticException) {
                Int.MAX_VALUE
            }

            val cell = HeatmapCell(
                date = dateStr,
                totalMinutes = dayMinutes,
                intensityTier = resolveTier(dayMinutes),
                subjectBreakdown = daySubjectMap,
                sessionCount = daySessions.size
            )
            cells.add(cell)

            currentDate = currentDate.plusDays(1)
        }

        val topSubject = overallSubjectBreakdown.maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.key

        return StudyHeatmapData(
            cells = cells,
            totalFocusMinutes = overallFocusMinutes,
            activeDaysCount = activeDays,
            subjectBreakdown = overallSubjectBreakdown,
            topSubject = topSubject
        )
    }
}
