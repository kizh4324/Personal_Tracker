package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Pure Kotlin interactor streaming categorized rules (allowed vs blocked) with search query
 * filtering on [Dispatchers.IO] (AD-1, AD-2, FR-4.6, FR-10.1).
 */
class GetInterventionRulesUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    data class CategorizedRules(
        val whitelistedRules: List<InterventionRule> = emptyList(),
        val blockedRules: List<InterventionRule> = emptyList(),
        val allRules: List<InterventionRule> = emptyList(),
        val searchQuery: String = ""
    ) {
        val whitelisted: List<InterventionRule> get() = whitelistedRules
        val blocked: List<InterventionRule> get() = blockedRules
    }

    operator fun invoke(searchQuery: String = ""): Flow<CategorizedRules> {
        return ruleRepository.getAllRules()
            .map { rules -> filterAndCategorize(rules, searchQuery) }
            .flowOn(ioDispatcher)
    }

    operator fun invoke(searchQueryFlow: Flow<String>): Flow<CategorizedRules> {
        return combine(ruleRepository.getAllRules(), searchQueryFlow) { rules, query ->
            filterAndCategorize(rules, query)
        }.flowOn(ioDispatcher)
    }

    private fun filterAndCategorize(rules: List<InterventionRule>, query: String): CategorizedRules {
        val trimmed = query.trim()
        val filtered = if (trimmed.isBlank()) {
            rules
        } else {
            rules.filter { rule ->
                rule.appDisplayName.contains(trimmed, ignoreCase = true) ||
                    rule.packageName.contains(trimmed, ignoreCase = true)
            }
        }
        val whitelisted = filtered.filter { !it.isStrictBlocked }
        val blocked = filtered.filter { it.isStrictBlocked }
        return CategorizedRules(
            whitelistedRules = whitelisted,
            blockedRules = blocked,
            allRules = filtered,
            searchQuery = query
        )
    }
}
