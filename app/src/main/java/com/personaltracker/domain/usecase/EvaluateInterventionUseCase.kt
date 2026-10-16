package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.InterventionRuleEngine
import com.personaltracker.domain.engine.InterventionRuleEngine.InterventionDecision
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Pure Kotlin use case evaluating foreground app launches against distraction rules and cooldowns (FR-4.2, FR-10.1).
 */
class EvaluateInterventionUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository,
    private val ruleEngine: InterventionRuleEngine
) {
    suspend operator fun invoke(
        packageName: String,
        lastPromptTimestamp: Long? = null,
        currentTimestamp: Long = System.currentTimeMillis()
    ): InterventionDecision {
        val rules = ruleRepository.getBlockedAppRules().firstOrNull() ?: emptyList()
        return ruleEngine.evaluateAppIntervention(
            packageName = packageName,
            rules = rules,
            lastPromptTimestamp = lastPromptTimestamp,
            currentTimestamp = currentTimestamp
        )
    }
}
