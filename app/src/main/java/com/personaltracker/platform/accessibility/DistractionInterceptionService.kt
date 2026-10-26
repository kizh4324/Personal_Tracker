package com.personaltracker.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.personaltracker.domain.usecase.InterceptDistractionUseCase
import com.personaltracker.platform.timer.StudySessionForegroundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event-driven Accessibility Service for distraction detection and non-punitive intervention (FR-4.3, AD-8, NFR-4).
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

    override fun onCreate() {
        super.onCreate()
        interventionOverlay = InterventionOverlay(this)
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
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // AD-8 Invariant: Drop all events other than TYPE_WINDOW_STATE_CHANGED immediately
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // AD-8 Invariant: Extract only the package identifier. Zero inspection of text or nodes.
        val packageName = event.packageName?.toString() ?: return

        val timerState = StudySessionForegroundService.timerState.value

        serviceScope.launch {
            val result = interceptDistractionUseCase(
                packageName = packageName,
                isTimerRunning = timerState.isRunning,
                isTimerPaused = timerState.isPaused,
                intervalType = timerState.intervalType,
                ownPackageName = this@DistractionInterceptionService.packageName
            )

            if (result.shouldIntercept) {
                interventionOverlay?.show(
                    packageName = result.packageName,
                    appDisplayName = result.matchedRule?.appDisplayName
                )
            }
        }
    }

    override fun onInterrupt() {
        interventionOverlay?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        interventionOverlay?.dismiss()
        serviceScope.cancel()
    }
}
