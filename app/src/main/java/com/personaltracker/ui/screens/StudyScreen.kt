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
 * Greenfield scaffold for the Study tab.
 * Study session timers and subject heatmaps will be integrated in Story 3.1.
 */
@Composable
fun StudyScreen(
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
                text = "Study Sessions & Heatmaps",
                style = typography.title,
                color = colors.inkPrimary
            )
        }
    }
}
