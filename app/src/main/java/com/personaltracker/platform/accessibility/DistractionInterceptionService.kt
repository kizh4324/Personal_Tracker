package com.personaltracker.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.accessibility.AccessibilityEvent
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.domain.usecase.InterceptDistractionUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event-driven Accessibility Service for distraction detection and non-punitive intervention
 * with emergency and dynamic telecom whitelist bypass (FR-4.3, FR-4.6, AD-8, NFR-4).
 *
 * Privacy Invariants:
 * - Strictly monitors only [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED].
 * - Never calls `event.text`, `event.source`, or inspects accessibility nodes.
 * - Guaranteed zero keylogging or screen scraping (`canRetrieveWindowContent = false`).
 */
@AndroidEntryPoint
class DistractionInterceptionService : AccessibilityService() {

    @Inject
    lateinit var interceptDistractionUseCase: InterceptDistractionUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var interventionOverlay: InterventionOverlay? = null
    private var interceptionJob: Job? = null

    private var defaultDialerPackage: String? = null
    private var isCallActive: Boolean = false
    private var telephonyCallback: Any? = null

    companion object {
        @Volatile
        var activeFocusTimerPolicy: FocusTimerPolicy = FocusTimerPolicy.AUTO_PAUSE
    }

    override fun onCreate() {
        super.onCreate()
        interventionOverlay = InterventionOverlay(this)
        resolveDefaultDialer()
        registerTelephonyListener()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Defense-in-depth: Programmatically ensure only window state events are delivered
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        info.flags = AccessibilityServiceInfo.DEFAULT
        serviceInfo = info
        resolveDefaultDialer()
    }

    private fun resolveDefaultDialer() {
        runCatching {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            defaultDialerPackage = telecomManager?.defaultDialerPackage
        }
    }

    private fun registerTelephonyListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state)
                    }
                }
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : android.telephony.PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
                telephonyCallback = listener
            }
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                isCallActive = true
                // Immediately dismiss overlay window upon incoming or outgoing call (FR-4.6)
                interventionOverlay?.dismiss()

                // Pause timer per FocusTimerPolicy if configured
                val currentTimerState = StudySessionForegroundService.timerState.value
                if (activeFocusTimerPolicy == FocusTimerPolicy.AUTO_PAUSE &&
                    currentTimerState.isRunning && !currentTimerState.isPaused
                ) {
                    StudySessionForegroundService.pauseService(this)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                isCallActive = false
            }
        }
    }

    private fun unregisterTelephonyListener() {
        runCatching {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback is TelephonyCallback) {
                telephonyManager?.unregisterTelephonyCallback(telephonyCallback as TelephonyCallback)
            } else if (telephonyCallback is android.telephony.PhoneStateListener) {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(
                    telephonyCallback as android.telephony.PhoneStateListener,
                    android.telephony.PhoneStateListener.LISTEN_NONE
                )
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // AD-8 Invariant: Drop all events other than TYPE_WINDOW_STATE_CHANGED immediately
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // AD-8 Invariant: Extract only the package identifier. Zero inspection of text or nodes.
        val packageName = event.packageName?.toString() ?: return

        val timerState = StudySessionForegroundService.timerState.value

        // Cancel in-flight job to prevent out-of-order race conditions over rapidly transitioning windows
        interceptionJob?.cancel()
        interceptionJob = serviceScope.launch {
            val result = interceptDistractionUseCase(
                packageName = packageName,
                isTimerRunning = timerState.isRunning,
                isTimerPaused = timerState.isPaused,
                intervalType = timerState.intervalType,
                ownPackageName = this@DistractionInterceptionService.packageName,
                defaultDialerPackage = defaultDialerPackage,
                isCallActive = isCallActive
            )

            if (result.shouldIntercept) {
                interventionOverlay?.show(
                    packageName = result.packageName,
                    appDisplayName = result.matchedRule?.appDisplayName
                )
            } else if (isCallActive || isEmergencyOrDialerPackage(packageName) || packageName == this@DistractionInterceptionService.packageName) {
                // Only dismiss if specifically emergency/dialer package, active call, or self package.
                // Pulling down notification shade (SystemUI) will not dismiss the overlay.
                interventionOverlay?.dismiss()
            }
        }
    }

    private fun isEmergencyOrDialerPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (defaultDialerPackage != null && packageName.equals(defaultDialerPackage, ignoreCase = true)) return true

        val lower = packageName.lowercase()
        if (lower.startsWith("com.android.systemui")) return false

        val emergencyDialerPackages = setOf(
            "com.android.emergency",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.server.telecom",
            "com.android.phone",
            "com.samsung.android.emergency",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui"
        )
        if (emergencyDialerPackages.contains(lower) || lower.startsWith("com.android.emergency")) return true

        val segments = lower.split('.')
        return segments.any { seg ->
            seg == "dialer" || seg == "emergency" || seg == "telecom" || seg == "incall" || seg == "telephony" || seg == "sos" || seg == "phone"
        }
    }

    override fun onInterrupt() {
        interventionOverlay?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        interceptionJob?.cancel()
        unregisterTelephonyListener()
        interventionOverlay?.dismiss()
        serviceScope.cancel()
    }
}
