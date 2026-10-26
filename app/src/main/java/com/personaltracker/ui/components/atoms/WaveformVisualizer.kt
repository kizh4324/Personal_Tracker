package com.personaltracker.ui.components.atoms

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.R
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Clamps amplitude to 0f..1f with Float.NaN protection.
 */
fun clampAmplitude(amplitude: Float): Float {
    return if (amplitude.isNaN()) 0f else amplitude.coerceIn(0f, 1f)
}

/**
 * Atomic Waveform Visualizer component for voice capture feedback.
 *
 * Renders animated frequency bars or the themed `ic_waveform` icon reacting to
 * incoming microphone amplitude in real time. Sourced from `EXPERIENCE.md` §4 and `AD-8`.
 */
@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    amplitude: Float = 0f,
    isRecording: Boolean = false,
    useBarsLayout: Boolean = true
) {
    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val clampedAmplitude = clampAmplitude(amplitude)

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformIdleTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationTokens.DurationStateShort,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveformPulse"
    )

    if (useBarsLayout) {
        val barWeights = listOf(0.4f, 0.7f, 1.0f, 0.7f, 0.4f)
        val minBarHeight = spacing.spacing2
        val maxBarHeight = spacing.spacing8

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
            modifier = modifier
                .defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)
                .padding(horizontal = spacing.spacing2, vertical = spacing.spacing1)
                .semantics {
                    contentDescription = if (isRecording) {
                        "Voice recording active with amplitude ${(clampedAmplitude * 100).toInt()}%"
                    } else {
                        "Voice recording inactive"
                    }
                }
        ) {
            barWeights.forEach { weight ->
                val dynamicHeight = if (isRecording) {
                    minBarHeight + (maxBarHeight - minBarHeight) * (clampedAmplitude * weight).coerceIn(0.1f, 1f)
                } else {
                    minBarHeight
                }

                Box(
                    modifier = Modifier
                        .width(spacing.spacing1)
                        .height(dynamicHeight)
                        .background(
                            color = colors.primary,
                            shape = shapes.full
                        )
                )
            }
        }
    } else {
        val effectiveScale = if (isRecording) {
            (1f + clampedAmplitude * 0.3f)
        } else {
            pulseScale
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(spacing.fabSize)
                .defaultMinSize(minWidth = spacing.minTouchTarget, minHeight = spacing.minTouchTarget)
                .semantics {
                    contentDescription = if (isRecording) "Voice capture listening" else "Voice capture idle"
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_waveform),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(spacing.spacing6)
                    .scale(effectiveScale)
            )
        }
    }
}
