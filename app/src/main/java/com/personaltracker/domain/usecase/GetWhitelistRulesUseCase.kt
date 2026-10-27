package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Pure Kotlin interactor streaming user-whitelisted and managed app rules (FR-4.6, FR-10.1, AD-1).
 */
class GetWhitelistRulesUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository
) {
    /**
     * Streams rules from the repository.
     * @param onlyWhitelisted When true, streams only rules where `isStrictBlocked = false`.
     *                        When false, streams all configured intervention rules.
     */
    operator fun invoke(onlyWhitelisted: Boolean = true): Flow<List<InterventionRule>> {
        return if (onlyWhitelisted) {
            ruleRepository.getWhitelistedRules()
        } else {
            ruleRepository.getAllRules()
        }
    }
}
