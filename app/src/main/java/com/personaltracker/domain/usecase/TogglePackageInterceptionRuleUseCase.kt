package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Pure Kotlin interactor toggling package interception state on [Dispatchers.IO] (AD-1, AD-2, FR-4.6, FR-10.1).
 *
 * Implements non-punitive whitelist inversion and exemption deletion per EXP-036:
 * - Toggling an already whitelisted app (`!existingRule.isStrictBlocked`) deletes the rule, restoring neutral unlisted state.
 * - Toggling an existing strictly blocked app (`existingRule.isStrictBlocked`) marks it as whitelisted (`isStrictBlocked = false`).
 * - Toggling an unlisted app creates a new whitelisted exemption (`isStrictBlocked = false`).
 */
class TogglePackageInterceptionRuleUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(packageName: String, appDisplayName: String? = null) = withContext(ioDispatcher) {
        val trimmedPkg = packageName.trim()
        if (trimmedPkg.isBlank()) return@withContext

        val existingRule = ruleRepository.getRuleForPackage(trimmedPkg)
        if (existingRule != null) {
            if (!existingRule.isStrictBlocked) {
                // If already whitelisted, toggling off reverts to neutral unlisted state (deletes rule per EXP-036)
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
