package com.personaltracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Greenfield scaffold for the Home tab.
 * Subordinate views (Hero Card, DayType Timeline, Action List) will be integrated in Stories 1.3 - 1.6.
 */
@Composable
fun HomeScreen(
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
        // Hero Card Placeholder Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.xl)
                .background(colors.surfaceRaised)
                .padding(spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Personal Tracker",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = "Your daily productivity & focus companion",
                    style = typography.bodySecondary,
                    color = colors.inkSecondary
                )
            }
        }
    }
}
