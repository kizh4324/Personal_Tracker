package com.personaltracker.platform.worker

import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.model.DayType
import com.personaltracker.domain.model.HabitLog
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.util.UUID

/**
 * Result output from executing a daily rollover.
 */
data class DayRolloverResult(
    val activeDayType: DayType,
    val evaluatedHabitsCount: Int,
    val habitResults: Map<String, String>
)

/**
 * Domain-orchestrated coordinator for daily rollover events (FR-3, FR-8.5, AD-1, AD-4).
 * Evaluates missed habits using [StreakStateMachine] with slack grace and freeze preservation,
 * and resolves the active DayType for the new day.
 */
class DayRolloverCoordinator(
    private val habitRepository: HabitRepository,
    private val dayTypeRepository: DayTypeRepository,
    private val streakStateMachine: StreakStateMachine,
    private val resolveDayTypeUseCase: ResolveDayTypeUseCase,
    private val transactionRunner: TransactionRunner
) {

    suspend fun processRollover(
        yesterdayDate: String = LocalDate.now().minusDays(1).toString(),
        todayDate: String = LocalDate.now().toString()
    ): DayRolloverResult {
        // 1. Resolve active DayType for today
        val activeDayType = resolveDayTypeUseCase(todayDate)

        // 2. Evaluate uncompleted habits from yesterday
        val allHabits = habitRepository.getAllHabits().firstOrNull() ?: emptyList()
        val habitResults = mutableMapOf<String, String>()

        transactionRunner {
            for (habit in allHabits) {
                val existingLog = habitRepository.getLogForDate(habit.id, yesterdayDate)
                if (existingLog != null && existingLog.status == "COMPLETED") {
                    habitResults[habit.id] = "COMPLETED"
                    continue
                }

                // Uncompleted habit: evaluate via StreakStateMachine grace rules
                val eval = streakStateMachine.evaluateDay(
                    habit = habit,
                    isCompleted = false,
                    logDate = yesterdayDate
                )

                habitRepository.updateHabit(eval.updatedHabit)

                val habitLog = HabitLog(
                    id = UUID.randomUUID().toString(),
                    habitId = habit.id,
                    logDate = yesterdayDate,
                    status = eval.resultingStatus,
                    loggedTimestamp = System.currentTimeMillis()
                )
                habitRepository.logHabit(habitLog)
                habitResults[habit.id] = eval.resultingStatus
            }
        }

        return DayRolloverResult(
            activeDayType = activeDayType,
            evaluatedHabitsCount = allHabits.size,
            habitResults = habitResults
        )
    }
}
