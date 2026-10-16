package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.Task
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.parser.ConfidenceTier
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import java.time.LocalDate
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
        val draftCommand: ParsedCommand
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
 * Pure Kotlin use case routing speech or text captures through deterministic 3-tier NLP confidence (FR-7, AD-8).
 */
class CaptureQuickThoughtUseCase @Inject constructor(
    private val parser: DeterministicNlpCommandParser,
    private val taskRepository: TaskRepository,
    private val unfiledInboxRepository: UnfiledInboxRepository
) {
    suspend operator fun invoke(
        rawText: String,
        captureSource: String = "OFFLINE_STT",
        referenceDate: LocalDate = LocalDate.now()
    ): QuickCaptureResult {
        val parsed = parser.parse(rawText, referenceDate)

        return when (parsed.tier) {
            ConfidenceTier.HIGH -> {
                if (parsed.intent != com.personaltracker.domain.parser.CommandIntent.CREATE_TASK) {
                    QuickCaptureResult.MediumConfidence(
                        draftCommand = parsed
                    )
                } else {
                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = parsed.title,
                        scheduledDate = parsed.date ?: referenceDate.toString(),
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
                QuickCaptureResult.MediumConfidence(
                    draftCommand = parsed
                )
            }
            ConfidenceTier.LOW -> {
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

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
