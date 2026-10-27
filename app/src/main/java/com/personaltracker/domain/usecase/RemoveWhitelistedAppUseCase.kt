package com.personaltracker.domain.usecase

import com.personaltracker.domain.repository.InterventionRuleRepository
import javax.inject.Inject

/**
 * Pure Kotlin interactor removing a package exemption from the intervention rules (FR-4.6, FR-10.1, AD-1).
 */
class RemoveWhitelistedAppUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository
) {
    suspend operator fun invoke(packageName: String) {
        val trimmed = packageName.trim()
        if (trimmed.isNotBlank()) {
            ruleRepository.deleteRule(trimmed)
        }
    }
}
