package com.personaltracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base Application class for Personal-Tracker v1.
 * Initializes Hilt Dependency Injection container and loads SQLCipher native library.
 */
@HiltAndroidApp
class PersonalTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {
            // Handled for non-Android JVM environments (e.g. unit tests)
        }
    }
}
