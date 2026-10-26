package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin validator enforcing relational integrity (ARCH-3, FR-7.6, AD-1):
 * - Verifies target habits exist in HabitRepository for LOG_HABIT commands.
 * - Verifies target routines exist in RoutineRepository for SHIFT_ROUTINE commands.
 */
@Singleton
class RelationalIntegrityValidator @Inject constructor(
    private val habitRepository: HabitRepository,
    private val routineRepository: RoutineRepository
) : CommandValidator {

    override suspend fun validate(
        command: ParsedCommand,
        referenceDateTime: LocalDateTime
    ): ValidationResult {
        return when (command.intent) {
            CommandIntent.LOG_HABIT -> validateHabit(command)
            CommandIntent.SHIFT_ROUTINE -> validateRoutine(command)
            else -> ValidationResult.Valid
        }
    }

    private suspend fun validateHabit(command: ParsedCommand): ValidationResult {
        val habitTarget = command.title.trim()
        if (habitTarget.isBlank()) {
            return ValidationResult.Invalid(
                reason = ValidationFailureReason.RELATIONAL_HABIT_NOT_FOUND,
                message = "Habit not found — check name or create habit first",
                invalidField = InvalidField.HABIT
            )
        }

        val allHabits = habitRepository.getAllHabits().first()
        val exists = allHabits.any { habit ->
            habit.name.trim().equals(habitTarget, ignoreCase = true)
        }

        return if (exists) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                reason = ValidationFailureReason.RELATIONAL_HABIT_NOT_FOUND,
                message = "Habit not found — check name or create habit first",
                invalidField = InvalidField.HABIT
            )
        }
    }

    private suspend fun validateRoutine(command: ParsedCommand): ValidationResult {
        val targetName = command.targetRoutineName?.trim()
            ?: command.title.replace(Regex("""\s+routine$""", RegexOption.IGNORE_CASE), "").trim()

        if (targetName.isBlank() || targetName.equals("the", ignoreCase = true)) {
            return ValidationResult.Invalid(
                reason = ValidationFailureReason.RELATIONAL_ROUTINE_NOT_FOUND,
                message = "Routine not found — check routine name",
                invalidField = InvalidField.ROUTINE
            )
        }

        val allRoutines = routineRepository.getAllRoutines().first()
        val exists = allRoutines.any { routine ->
            routine.title.trim().equals(targetName, ignoreCase = true) ||
                    routine.title.lowercase(Locale.ROOT).contains(targetName.lowercase(Locale.ROOT)) ||
                    targetName.lowercase(Locale.ROOT).contains(routine.title.lowercase(Locale.ROOT))
        }

        return if (exists) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                reason = ValidationFailureReason.RELATIONAL_ROUTINE_NOT_FOUND,
                message = "Routine not found — check routine name",
                invalidField = InvalidField.ROUTINE
            )
        }
    }
}
