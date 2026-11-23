package com.personaltracker

import android.app.Application
import com.personaltracker.platform.notification.TieredNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Base Application class for Personal-Tracker v1.
 * Initializes Hilt Dependency Injection container, loads SQLCipher native library,
 * and configures tiered notification channels (Story 5.1).
 */
@HiltAndroidApp
class PersonalTrackerApplication : Application() {

    @Inject
    lateinit var tieredNotificationManager: TieredNotificationManager

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {
            // Handled for non-Android JVM environments (e.g. unit tests)
        }

        tieredNotificationManager.createNotificationChannels()
    }
}
