package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import com.personaltracker.domain.validation.CommandValidationPipeline
import com.personaltracker.domain.validation.RelationalIntegrityValidator
import com.personaltracker.domain.validation.TaskDeduplicationValidator
import com.personaltracker.domain.validation.TemporalSafetyValidator
import com.personaltracker.domain.validation.ValidationResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/**
 * Result sealed hierarchy for 3-tier quick capture routing (AD-8).
 */
sealed interface QuickCaptureResult {
    /**
     * High Confidence (>= 0.85): Instant commit with reversible undo window.
     */
    data class HighConfidence(
        val committedTask: Task,
        val parsed: ParsedCommand
    ) : QuickCaptureResult

    /**
     * Medium Confidence (0.50..0.85): Editable confirmation chips presented in QuickCaptureSheet.
     * No autonomous database mutation occurs until user confirms.
     */
    data class MediumConfidence(
        val draftCommand: ParsedCommand,
        val validationWarning: String? = null,
        val validationResult: ValidationResult? = null
    ) : QuickCaptureResult

    /**
     * Low Confidence (< 0.50): Saved to UnfiledCaptureInboxEntity for manual triage.
     */
    data class LowConfidence(
        val inboxItem: UnfiledInboxItem,
        val parsed: ParsedCommand
    ) : QuickCaptureResult
}

/**
 * Pure Kotlin use case routing speech or text captures through deterministic 3-tier NLP confidence (FR-7, AD-8)
 * and strict AI action boundary command validation pipeline (ARCH-3, FR-7.6, AD-1, AD-4).
 */
class CaptureQuickThoughtUseCase @Inject constructor(
    private val parser: DeterministicNlpCommandParser,
    private val taskRepository: TaskRepository,
    private val unfiledInboxRepository: UnfiledInboxRepository,
    private val validationPipeline: CommandValidationPipeline
) {

    // Secondary constructor preserving compatibility for callers without explicit DI pipeline
    constructor(
        parser: DeterministicNlpCommandParser,
        taskRepository: TaskRepository,
        unfiledInboxRepository: UnfiledInboxRepository
    ) : this(
        parser = parser,
        taskRepository = taskRepository,
        unfiledInboxRepository = unfiledInboxRepository,
        validationPipeline = CommandValidationPipeline(
            TemporalSafetyValidator(),
            RelationalIntegrityValidator(
                object : HabitRepository {
                    override fun getAllHabits() = kotlinx.coroutines.flow.flowOf(emptyList<com.personaltracker.domain.model.Habit>())
                    override suspend fun getHabitById(id: String) = null
                    override fun getLogsForHabit(habitId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.personaltracker.domain.model.HabitLog>())
                    override suspend fun getLogForDate(habitId: String, logDate: String) = null
                    override suspend fun saveHabit(habit: com.personaltracker.domain.model.Habit) {}
                    override suspend fun updateHabit(habit: com.personaltracker.domain.model.Habit) {}
                    override suspend fun logHabit(log: com.personaltracker.domain.model.HabitLog) {}
                    override suspend fun logHabitWithReward(log: com.personaltracker.domain.model.HabitLog, coinReward: Int, idempotencyKey: String) {}
                },
                object : RoutineRepository {
                    override fun getRoutinesForDayType(dayTypeId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.personaltracker.domain.model.Routine>())
                    override fun getStepsForRoutine(routineId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.personaltracker.domain.model.RoutineStep>())
                    override suspend fun getRoutineById(routineId: String) = null
                    override fun getAllRoutines() = kotlinx.coroutines.flow.flowOf(emptyList<com.personaltracker.domain.model.Routine>())
                    override suspend fun getStepsForRoutineList(routineId: String) = emptyList<com.personaltracker.domain.model.RoutineStep>()
                    override suspend fun ensureDefaultRoutines() = emptyList<com.personaltracker.domain.model.Routine>()
                    override suspend fun saveRoutine(routine: com.personaltracker.domain.model.Routine) {}
                    override suspend fun saveSteps(steps: List<com.personaltracker.domain.model.RoutineStep>) {}
                    override suspend fun saveRoutineWithSteps(routine: com.personaltracker.domain.model.Routine, steps: List<com.personaltracker.domain.model.RoutineStep>) {}
                    override suspend fun deleteRoutine(routineId: String) {}
                }
            ),
            TaskDeduplicationValidator(taskRepository)
        )
    )

    suspend operator fun invoke(
        rawText: String,
        captureSource: String = "OFFLINE_STT",
        referenceDateTime: LocalDateTime = LocalDateTime.now()
    ): QuickCaptureResult {
        val parsed = parser.parse(rawText, referenceDateTime)

        return when (parsed.tier) {
            ConfidenceTier.HIGH -> {
                // Strict AI Action Boundary: Validate before any persistent Room database mutations!
                val validation = validationPipeline.validate(parsed, referenceDateTime)
                if (validation is ValidationResult.Invalid) {
                    // Gracefully downgrade to Medium Confidence with non-punishing feedback (AD-4, AD-8)
                    val downgraded = parsed.copy(tier = ConfidenceTier.MEDIUM)
                    QuickCaptureResult.MediumConfidence(
                        draftCommand = downgraded,
                        validationWarning = validation.message,
                        validationResult = validation
                    )
                } else if (parsed.intent != com.personaltracker.domain.parser.CommandIntent.CREATE_TASK) {
                    // Non-task high-confidence intent routes to Medium Confidence for 1-tap confirmation
                    QuickCaptureResult.MediumConfidence(
                        draftCommand = parsed
                    )
                } else {
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = parsed.title,
                        scheduledDate = parsed.date ?: referenceDateTime.toLocalDate().toString(),
                        scheduledTime = parsed.time,
                        estimatedDurationMinutes = parsed.durationMinutes ?: 0,
                        deliveryIntensity = parsed.deliveryIntensity,
                        state = "PENDING"
                    )
                    taskRepository.saveTask(task)
                    QuickCaptureResult.HighConfidence(
                        committedTask = task,
                        parsed = parsed
                    )
                }
            }
            ConfidenceTier.MEDIUM -> {
                val validation = validationPipeline.validate(parsed, referenceDateTime)
                if (validation is ValidationResult.Invalid) {
                    QuickCaptureResult.MediumConfidence(
                        draftCommand = parsed,
                        validationWarning = validation.message,
                        validationResult = validation
                    )
                } else {
                    QuickCaptureResult.MediumConfidence(
                        draftCommand = parsed
                    )
                }
            }
            ConfidenceTier.LOW -> {
                // Low confidence skips task validation pipeline and routes directly to unfiled inbox
                val draftJson = buildString {
                    append("{")
                    append("\"title\":\"").append(escapeJson(parsed.title)).append("\",")
                    append("\"intent\":\"").append(parsed.intent.name).append("\",")
                    append("\"confidence\":").append(parsed.confidence)
                    append("}")
                }
                val inboxItem = UnfiledInboxItem(
                    id = UUID.randomUUID().toString(),
                    rawTranscriptText = rawText,
                    draftAttributesJson = draftJson,
                    parserConfidence = parsed.confidence,
                    captureSource = captureSource
                )
                unfiledInboxRepository.saveInboxItem(inboxItem)
                QuickCaptureResult.LowConfidence(
                    inboxItem = inboxItem,
                    parsed = parsed
                )
            }
        }
    }

    suspend operator fun invoke(
        rawText: String,
        referenceDate: LocalDate
    ): QuickCaptureResult = invoke(
        rawText = rawText,
        captureSource = "OFFLINE_STT",
        referenceDateTime = LocalDateTime.of(referenceDate, LocalTime.MIDNIGHT)
    )

    suspend operator fun invoke(
        rawText: String,
        captureSource: String,
        referenceDate: LocalDate
    ): QuickCaptureResult = invoke(
        rawText = rawText,
        captureSource = captureSource,
        referenceDateTime = LocalDateTime.of(referenceDate, LocalTime.MIDNIGHT)
    )

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
