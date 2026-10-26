package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Pure Kotlin use case evaluating foreground package transitions against distraction rules
 * and active focus session intervals (FR-4.3, FR-10.1, AD-1, AD-8).
 *
 * Guarantees zero Android framework dependencies and zero inspection of window contents.
 */
class InterceptDistractionUseCase @Inject constructor(
    private val ruleRepository: InterventionRuleRepository
) {

    data class InterceptionResult(
        val shouldIntercept: Boolean,
        val packageName: String,
        val matchedRule: InterventionRule? = null,
        val reason: String
    )

    companion object {
        private val SYSTEM_EXCLUDED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.server.telecom",
            "com.google.android.packageinstaller",
            "com.android.settings",
            "com.android.phone",
            "com.google.android.inputmethod.latin"
        )
    }

    /**
     * Evaluates whether a foreground package should trigger an intervention overlay.
     *
     * @param packageName The package identifier that transitioned to the foreground
     * @param isTimerRunning Whether a study/focus timer is actively running
     * @param isTimerPaused Whether the study/focus timer is currently paused
     * @param intervalType The current interval type (e.g. DEEP_WORK, POMODORO_WORK, SHORT_BREAK, LONG_BREAK)
     * @param ownPackageName The application's own package name to prevent self-interception
     */
    suspend operator fun invoke(
        packageName: String,
        isTimerRunning: Boolean,
        isTimerPaused: Boolean = false,
        intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK,
        ownPackageName: String = "com.personaltracker"
    ): InterceptionResult {
        // 1. Never intercept if timer is not running or is paused
        if (!isTimerRunning || isTimerPaused) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Focus timer is idle or paused."
            )
        }

        // 2. Never intercept during rest intervals (SHORT_BREAK, LONG_BREAK)
        if (intervalType.isBreak) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Break interval active (${intervalType.displayName}); distraction interception suspended."
            )
        }

        // 3. Restrict triggers strictly to active work intervals (DEEP_WORK, POMODORO_WORK)
        if (intervalType != FocusIntervalType.DEEP_WORK && intervalType != FocusIntervalType.POMODORO_WORK) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Not an active work interval (${intervalType.displayName})."
            )
        }

        // 4. Never intercept system packages, dialers, or Personal-Tracker's own package
        if (isSystemOrSelf(packageName, ownPackageName)) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "System, dialer, or application self-package is exempted."
            )
        }

        // 5. Evaluate against configured blocked app rules
        val rules = ruleRepository.getBlockedAppRules().firstOrNull() ?: emptyList()
        val matchedRule = rules.find { it.packageName.equals(packageName, ignoreCase = true) }

        if (matchedRule == null) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "App is not registered as a distraction target."
            )
        }

        return if (matchedRule.isStrictBlocked) {
            InterceptionResult(
                shouldIntercept = true,
                packageName = packageName,
                matchedRule = matchedRule,
                reason = "Distraction app opened during active ${intervalType.name} session."
            )
        } else {
            InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                matchedRule = matchedRule,
                reason = "Rule configured with strict blocking disabled."
            )
        }
    }

    private fun isSystemOrSelf(packageName: String, ownPackageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName.equals(ownPackageName, ignoreCase = true)) return true
        if (SYSTEM_EXCLUDED_PACKAGES.contains(packageName.lowercase())) return true
        if (packageName.startsWith("com.android.systemui", ignoreCase = true)) return true
        if (packageName.contains("dialer", ignoreCase = true) || packageName.contains("telecom", ignoreCase = true)) return true
        return false
    }
}
