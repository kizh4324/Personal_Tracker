package com.personaltracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base Application class for Personal-Tracker v1.
 * Initializes Hilt Dependency Injection container.
 */
@HiltAndroidApp
class PersonalTrackerApplication : Application()
