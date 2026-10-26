package com.personaltracker.domain.validation

import com.personaltracker.domain.parser.CommandIntent
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin validator preventing duplicate active tasks on the same date (ARCH-3, FR-7.6, AD-1, AD-4).
 *
 * Rules:
 * - Only applies to CREATE_TASK intent.
 * - Checks tasks on the scheduled date (or today if date is null).
 * - Rejects task creation if an active task (PENDING, IN_PROGRESS, INTERRUPTED) with the same
 *   title already exists on the target date.
 * - Completed or cancelled tasks on the same date do NOT block creation.
 */
@Singleton
class TaskDeduplicationValidator @Inject constructor(
    private val taskRepository: TaskRepository
) : CommandValidator {

    override suspend fun validate(
        command: ParsedCommand,
        referenceDateTime: LocalDateTime
    ): ValidationResult {
        if (command.intent != CommandIntent.CREATE_TASK) {
            return ValidationResult.Valid
        }

        val targetTitle = command.title.trim()
        if (targetTitle.isBlank()) {
            return ValidationResult.Valid
        }

        val targetDate = command.date ?: referenceDateTime.toLocalDate().toString()
        val tasksForDate = taskRepository.getTasksForDate(targetDate).first()

        val hasActiveDuplicate = tasksForDate.any { existingTask ->
            val isActive = existingTask.state != "COMPLETED" && existingTask.state != "CANCELLED"
            isActive && existingTask.title.trim().equals(targetTitle, ignoreCase = true)
        }

        return if (hasActiveDuplicate) {
            ValidationResult.Invalid(
                reason = ValidationFailureReason.DUPLICATE_ACTIVE_TASK,
                message = "A task with this title is already active for today",
                invalidField = InvalidField.TITLE
            )
        } else {
            ValidationResult.Valid
        }
    }
}
