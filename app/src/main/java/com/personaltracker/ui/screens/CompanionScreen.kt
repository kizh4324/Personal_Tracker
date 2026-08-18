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
 * Greenfield scaffold for the Companion tab.
 * Companion customizer and cosmetic shop will be integrated in Story 4.4 - 4.5.
 */
@Composable
fun CompanionScreen(
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
                text = "Companion & Cosmetic Shop",
                style = typography.title,
                color = colors.inkPrimary
            )
        }
    }
}
