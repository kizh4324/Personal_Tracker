package com.personaltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.personaltracker.ui.navigation.PersonalTrackerApp
import com.personaltracker.ui.theme.PersonalTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main application entry activity.
 * Sets edge-to-edge system insets and renders root Compose theme and navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PersonalTrackerTheme {
                PersonalTrackerApp()
            }
        }
    }
}
