package com.personaltracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Greenfield scaffold for the Settings tab.
 * Diagnostic capabilities, template management, and backup will be integrated in Story 6.3 - 6.4.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.md, vertical = spacing.lg),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.lg)
                .background(colors.surfaceRaised)
                .padding(spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings & Data Privacy",
                style = typography.title,
                color = colors.inkPrimary
            )
        }
    }
}
