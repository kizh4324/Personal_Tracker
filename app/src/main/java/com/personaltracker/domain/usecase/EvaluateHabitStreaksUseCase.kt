package com.personaltracker.domain.usecase

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.model.HabitLogStatus
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * Result of evaluating habit streaks for a given date or chronological gap.
 */
data class EvaluateHabitStreaksResult(
    val evaluatedHabitsCount: Int,
    val habitResults: Map<String, String>
)

/**
 * Pure Kotlin domain use case orchestrating batch/rollover streak evaluations (FR-2.4, FR-8.5, AD-1, AD-4).
 * Resolves target date DayType immunity, evaluates cadence rules, and handles multi-day chronological gaps
 * with non-punitive grace shield / slack absorption inside database transactions.
 */
class EvaluateHabitStreaksUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val dayTypeRepository: DayTypeRepository,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val streakStateMachine: StreakStateMachine,
    private val transactionRunner: TransactionRunner
) {

    suspend operator fun invoke(
        targetDate: String = LocalDate.now().toString()
    ): EvaluateHabitStreaksResult {
        require(targetDate.isNotBlank()) { "targetDate must not be blank" }
        val targetLocalDate = LocalDate.parse(targetDate)

        val allHabits = habitRepository.getAllHabits().firstOrNull() ?: emptyList()
        val habitResults = mutableMapOf<String, String>()

        transactionRunner {
            for (habit in allHabits) {
                val logs = habitRepository.getLogsForHabit(habit.id).firstOrNull() ?: emptyList()
                val latestLoggedDate = logs.mapNotNull {
                    runCatching { LocalDate.parse(it.logDate) }.getOrNull()
                }.maxOrNull()

                val createdDate = runCatching {
                    Instant.ofEpochMilli(habit.createdTimestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.getOrDefault(targetLocalDate)

                val startDate = when {
                    latestLoggedDate != null -> latestLoggedDate.plusDays(1)
                    createdDate.isBefore(targetLocalDate) -> createdDate
                    else -> targetLocalDate
                }

                var currentHabit = habit
                var lastStatus = "NO_CHANGE"

                if (!startDate.isAfter(targetLocalDate)) {
                    var currentDate = startDate
                    while (!currentDate.isAfter(targetLocalDate)) {
                        val dateStr = currentDate.toString()
                        val existingLog = habitRepository.getLogForDate(habit.id, dateStr)

                        if (existingLog == null) {
                            val activeDayType = resolveDayTypeUseCase(dateStr)
                            val eval = streakStateMachine.evaluateDay(
                                habit = currentHabit,
                                isCompleted = false,
                                logDate = dateStr,
                                activeDayType = activeDayType
                            )
                            currentHabit = eval.updatedHabit
                            lastStatus = eval.resultingStatus

                            val habitLog = HabitLog(
                                id = UUID.randomUUID().toString(),
                                habitId = habit.id,
                                logDate = dateStr,
                                status = eval.resultingStatus,
                                loggedTimestamp = System.currentTimeMillis()
                            )
                            habitRepository.logHabit(habitLog)
                        } else {
                            lastStatus = existingLog.status
                        }
                        currentDate = currentDate.plusDays(1)
                    }

                    if (currentHabit != habit) {
                        habitRepository.updateHabit(currentHabit)
                    }
                } else {
                    val existingLog = habitRepository.getLogForDate(habit.id, targetDate)
                    lastStatus = existingLog?.status ?: "NO_CHANGE"
                }

                habitResults[habit.id] = lastStatus
            }
        }

        return EvaluateHabitStreaksResult(
            evaluatedHabitsCount = allHabits.size,
            habitResults = habitResults
        )
    }
}
