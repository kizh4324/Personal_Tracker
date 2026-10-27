package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import javax.inject.Inject

/**
 * Pure Kotlin interactor adding a custom package whitelist exemption (FR-4.6, FR-10.1, AD-1).
 */
class AddWhitelistedAppUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository
) {
    suspend operator fun invoke(packageName: String, appDisplayName: String): Boolean {
        val trimmedPkg = packageName.trim()
        val trimmedName = appDisplayName.trim()
        if (trimmedPkg.isBlank()) return false

        val displayName = if (trimmedName.isBlank()) {
            trimmedPkg.trimEnd('.').substringAfterLast('.').ifBlank { trimmedPkg }.replaceFirstChar { it.uppercase() }
        } else {
            trimmedName
        }

        val rule = InterventionRule(
            packageName = trimmedPkg,
            appDisplayName = displayName,
            isStrictBlocked = false
        )
        ruleRepository.saveRule(rule)
        return true
    }
}
