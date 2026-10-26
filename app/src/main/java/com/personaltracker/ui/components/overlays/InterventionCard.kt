package com.personaltracker.ui.components.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateless Jetpack Compose card presented inside the system overlay window when a distraction
 * app is opened during an active focus block (FR-4.3, AD-4, AD-7).
 *
 * Adheres strictly to Warm Organic Tactile design tokens with zero hardcoded literals and
 * non-punitive, supportive microcopy.
 */
@Composable
fun InterventionCard(
    onResumeFocus: () -> Unit,
    onTakeBreak: () -> Unit,
    onEndEarly: () -> Unit,
    modifier: Modifier = Modifier,
    appDisplayName: String? = null
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.xxl,
        color = colors.surfaceBase,
        border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
        shadowElevation = spacing.spacing2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.spacing6,
                    vertical = spacing.spacing6
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Non-punitive title
            Text(
                text = "Focus Session Active",
                style = typography.headline,
                color = colors.inkPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.spacing2))

            // Non-punitive empathetic guidance
            val bodyText = if (!appDisplayName.isNullOrBlank()) {
                "You set aside this time to focus. Take a gentle breath before opening $appDisplayName."
            } else {
                "You set aside this time to focus. Take a gentle breath before switching apps."
            }

            Text(
                text = bodyText,
                style = typography.body,
                color = colors.inkSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.spacing6))

            // 1. Primary CTA: Resume Focus
            Button(
                onClick = onResumeFocus,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.surfaceRaised
                ),
                shape = shapes.lg,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.buttonHeight)
            ) {
                Text(
                    text = "Resume Focus",
                    style = typography.ctaText
                )
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // 2. Break CTA: Take a 5m Break
            Button(
                onClick = onTakeBreak,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.bannerPeach,
                    contentColor = colors.primaryDark
                ),
                shape = shapes.lg,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(
                    text = "Take a 5m Break",
                    style = typography.label
                )
            }

            Spacer(modifier = Modifier.height(spacing.spacing2))

            // 3. End Early CTA
            TextButton(
                onClick = onEndEarly,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.inkTertiary
                ),
                shape = shapes.md,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Text(
                    text = "End Early",
                    style = typography.label
                )
            }
        }
    }
}
