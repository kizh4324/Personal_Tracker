package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import javax.inject.Inject

/**
 * Pure Kotlin interactor toggling package strict blocking vs whitelisted exemption (FR-4.6, FR-10.1, AD-1).
 */
class ToggleAppWhitelistUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository
) {
    suspend operator fun invoke(packageName: String, appDisplayName: String? = null) {
        val trimmedPkg = packageName.trim()
        if (trimmedPkg.isBlank()) return

        val existingRule = ruleRepository.getRuleForPackage(trimmedPkg)
        if (existingRule != null) {
            if (!existingRule.isStrictBlocked) {
                // If already whitelisted, toggling off reverts to neutral unlisted state (deletes rule)
                ruleRepository.deleteRule(trimmedPkg)
            } else {
                // If previously strictly blocked, toggle to whitelisted exemption
                ruleRepository.saveRule(existingRule.copy(isStrictBlocked = false))
            }
        } else {
            val name = appDisplayName?.trim()?.ifBlank { null }
                ?: trimmedPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            val newRule = InterventionRule(
                packageName = trimmedPkg,
                appDisplayName = name,
                isStrictBlocked = false
            )
            ruleRepository.saveRule(newRule)
        }
    }
}
