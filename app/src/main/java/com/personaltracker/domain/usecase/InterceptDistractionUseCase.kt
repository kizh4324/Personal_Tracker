package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Pure Kotlin use case evaluating foreground package transitions against distraction rules,
 * active focus session intervals, telecom call state, and emergency/dynamic whitelist exemptions
 * (FR-4.3, FR-4.6, FR-10.1, AD-1, AD-8).
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
            "com.android.emergency",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.server.telecom",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.settings",
            "com.android.phone",
            "com.google.android.inputmethod.latin",
            "com.google.android.apps.safetyhub",
            "com.samsung.android.emergency",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui"
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
     * @param defaultDialerPackage Dynamic default dialer package name from TelecomManager
     * @param isCallActive Whether an incoming or outgoing telecom call is currently ringing or active
     */
    suspend operator fun invoke(
        packageName: String,
        isTimerRunning: Boolean,
        isTimerPaused: Boolean = false,
        intervalType: FocusIntervalType = FocusIntervalType.DEEP_WORK,
        ownPackageName: String = "com.personaltracker",
        defaultDialerPackage: String? = null,
        isCallActive: Boolean = false
    ): InterceptionResult {
        // 1. Unconditionally bypass interception during active or ringing telecom calls (FR-4.6)
        if (isCallActive) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Active phone call or ringing state detected; emergency bypass engaged."
            )
        }

        // 2. Never intercept if timer is not running or is paused
        if (!isTimerRunning || isTimerPaused) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Focus timer is idle or paused."
            )
        }

        // 3. Never intercept during rest intervals (SHORT_BREAK, LONG_BREAK)
        if (intervalType.isBreak) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Break interval active (${intervalType.displayName}); distraction interception suspended."
            )
        }

        // 4. Restrict triggers strictly to active work intervals (DEEP_WORK, POMODORO_WORK)
        if (intervalType != FocusIntervalType.DEEP_WORK && intervalType != FocusIntervalType.POMODORO_WORK) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Not an active work interval (${intervalType.displayName})."
            )
        }

        // 5. Never intercept emergency services, dialers, telecom components, or system UI (FR-4.6, FR-10.1)
        if (isSystemOrDialerOrEmergency(packageName, ownPackageName, defaultDialerPackage)) {
            return InterceptionResult(
                shouldIntercept = false,
                packageName = packageName,
                reason = "Emergency, dialer, telecom, system UI, or application self-package is exempted."
            )
        }

        // 6. Evaluate against configured rules (checking direct package lookup then active rule list)
        val matchedRule = ruleRepository.getRuleForPackage(packageName)
            ?: ruleRepository.getAllRules().firstOrNull()?.find { it.packageName.equals(packageName, ignoreCase = true) }

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
                reason = "Package is explicitly whitelisted by user."
            )
        }
    }

    private fun isSystemOrDialerOrEmergency(
        packageName: String,
        ownPackageName: String,
        defaultDialerPackage: String?
    ): Boolean {
        if (packageName.isBlank()) return true
        if (packageName.equals(ownPackageName, ignoreCase = true)) return true
        if (defaultDialerPackage != null && packageName.equals(defaultDialerPackage, ignoreCase = true)) return true

        val lower = packageName.lowercase()
        if (SYSTEM_EXCLUDED_PACKAGES.contains(lower)) return true
        if (lower.startsWith("com.android.systemui") || lower.startsWith("com.android.emergency")) return true

        // Strict segment checking prevents false matches on apps like com.phonepe.app or com.sostravel.app
        val segments = lower.split('.')
        if (segments.any { seg ->
            seg == "dialer" || seg == "emergency" || seg == "telecom" || seg == "incall" || seg == "telephony" || seg == "sos" || seg == "phone"
        }) {
            return true
        }

        return false
    }
}
