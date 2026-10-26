package com.personaltracker.platform.accessibility

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.personaltracker.MainActivity
import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.platform.timer.StudySessionForegroundService
import com.personaltracker.ui.components.overlays.InterventionCard
import com.personaltracker.ui.theme.PersonalTrackerTheme
import java.util.UUID

/**
 * Custom standalone LifecycleOwner enabling Jetpack Compose within a system alert window overlay
 * without an Activity dependency (EXP-016, AD-1, AD-7).
 */
internal class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/**
 * System alert overlay controller presenting non-punitive focus intervention dialogs (FR-4.3, AD-4).
 * Manages SYSTEM_ALERT_WINDOW attachment, Compose ViewTree owners, and graceful fallbacks.
 */
class InterventionOverlay(
    private val context: Context
) {

    companion object {
        private const val TAG = "InterventionOverlay"
    }

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean
        get() = composeView != null

    /**
     * Displays the intervention overlay over the foreground app.
     * Falls back to launching [MainActivity] if overlay permissions are missing or attachment fails.
     */
    fun show(
        packageName: String,
        appDisplayName: String? = null
    ) {
        if (isShowing) return

        // 1. Verify overlay permission before attempting WindowManager operations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted; falling back to foreground activity.")
            launchMainActivity()
            return
        }

        try {
            val owner = OverlayLifecycleOwner().apply {
                onCreate()
                onResume()
            }
            lifecycleOwner = owner

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    PersonalTrackerTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PersonalTrackerTheme.colors.surfaceOverlay)
                                .padding(PersonalTrackerTheme.spacing.spacing4),
                            contentAlignment = Alignment.Center
                        ) {
                            InterventionCard(
                                onResumeFocus = {
                                    dismiss()
                                    launchMainActivity()
                                },
                                onTakeBreak = {
                                    dismiss()
                                    startFiveMinuteBreak()
                                    launchMainActivity()
                                },
                                onEndEarly = {
                                    dismiss()
                                    endSessionEarly()
                                },
                                appDisplayName = appDisplayName
                            )
                        }
                    }
                }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(view, params)
            composeView = view
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach intervention overlay; launching main activity fallback", e)
            dismiss()
            launchMainActivity()
        }
    }

    /**
     * Safely tears down and detaches the overlay window.
     */
    fun dismiss() {
        val view = composeView ?: return
        try {
            lifecycleOwner?.onPause()
            lifecycleOwner?.onDestroy()
            windowManager.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Error while removing overlay view", e)
        } finally {
            composeView = null
            lifecycleOwner = null
        }
    }

    private fun launchMainActivity() {
        runCatching {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    private fun startFiveMinuteBreak() {
        StudySessionForegroundService.startService(
            context = context,
            sessionId = UUID.randomUUID().toString(),
            subjectTag = "Break",
            durationSeconds = 5 * 60L,
            intervalType = FocusIntervalType.SHORT_BREAK
        )
    }

    private fun endSessionEarly() {
        StudySessionForegroundService.stopService(context)
    }
}
