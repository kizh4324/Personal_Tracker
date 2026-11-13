package com.personaltracker.domain.usecase

import com.personaltracker.domain.repository.InterventionRuleRepository
import javax.inject.Inject

/**
 * Pure Kotlin interactor toggling package strict blocking vs whitelisted exemption (FR-4.6, FR-10.1, AD-1).
 * Delegates to [TogglePackageInterceptionRuleUseCase].
 */
class ToggleAppWhitelistUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository,
    private val togglePackageInterceptionRuleUseCase: TogglePackageInterceptionRuleUseCase = TogglePackageInterceptionRuleUseCase(ruleRepository)
) {
    suspend operator fun invoke(packageName: String, appDisplayName: String? = null) {
        togglePackageInterceptionRuleUseCase(packageName, appDisplayName)
    }
}
