package com.personaltracker.ui.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.theme.PersonalTrackerTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hand-built Coin Arc Motion Composable.
 *
 * Sourced from `design-system.md` Gamification Visual System & Motion Principles:
 * - Animates coins along a quadratic Bezier curve from item position to top-right coin HUD badge.
 * - Duration: ~600ms per coin, with staggered departure for multiple coins.
 * - VSYNC Aware: Uses Compose coroutine clock (`withFrameNanos`) to adapt seamlessly across
 *   variable refresh rate displays (60Hz, 90Hz, 120Hz).
 *
 * NOTE ON DEVICE REFRESH RATE (CRED LESSON):
 * This animation does NOT use a fixed frame count. Timing is driven by elapsed monotonic time
 * synced to VSYNC frames, preventing the animation from playing artificially fast on 120Hz screens
 * or jittering on 60Hz screens.
 *
 * TODO (QA): Test and visually verify on physical 60Hz and 120Hz Android devices.
 */
@Composable
fun CoinArcAnimation(
    isPlaying: Boolean,
    startOffset: Offset,
    endOffset: Offset,
    coinCount: Int = 1,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coinColor = PersonalTrackerTheme.colors.coinGold
    val coinGlowColor = PersonalTrackerTheme.colors.coinGlow

    // Track progress of each coin (0f to 1f)
    val coinAnimatables = remember(coinCount) {
        List(coinCount) { Animatable(0f) }
    }

    LaunchedEffect(isPlaying, startOffset, endOffset, coinCount) {
        if (!isPlaying || startOffset == Offset.Zero || endOffset == Offset.Zero) return@LaunchedEffect

        coroutineScope {
            coinAnimatables.forEachIndexed { index, animatable ->
                launch {
                    // Reset to 0
                    animatable.snapTo(0f)

                    // Stagger departure by 80ms per coin
                    if (index > 0) {
                        delay(index * 80L)
                    }

                    // Calculate control point for high quadratic curve arc
                    val midX = (startOffset.x + endOffset.x) / 2f
                    val arcHeight = max(120f, (startOffset.y - endOffset.y) * 0.4f)
                    val controlY = min(startOffset.y, endOffset.y) - arcHeight

                    // Animate 0f -> 1f over 600ms with VSYNC frame sync
                    val startTime = withFrameNanos { it }
                    val totalDurationNanos = 600_000_000L // 600ms in nanoseconds

                    while (true) {
                        val currentFrameTime = withFrameNanos { it }
                        val elapsedNanos = currentFrameTime - startTime
                        val rawProgress = (elapsedNanos.toFloat() / totalDurationNanos).coerceIn(0f, 1f)

                        // Apply standard FastOutSlowIn easing curve
                        val easedProgress = FastOutSlowInEasing.transform(rawProgress)
                        animatable.snapTo(easedProgress)

                        if (rawProgress >= 1f) {
                            break
                        }
                    }

                    // If this was the last coin, notify completion
                    if (index == coinCount - 1) {
                        onComplete()
                    }
                }
            }
        }
    }

    if (isPlaying) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val controlPoint = calculateControlPoint(startOffset, endOffset)

            coinAnimatables.forEach { animatable ->
                val t = animatable.value
                if (t > 0f && t < 1f) {
                    // Quadratic Bezier curve formula: B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
                    val currentPos = evaluateQuadraticBezier(startOffset, controlPoint, endOffset, t)
                    
                    // Dynamic scaling: scale up slightly in mid-air (1.15), settle to 1.0
                    val scale = 1f + 0.25f * (1f - (2f * t - 1f).pow(2))
                    val coinRadius = 10.dp.toPx() * scale

                    // Draw outer subtle glow
                    drawCircle(
                        color = coinGlowColor,
                        radius = coinRadius * 1.6f,
                        center = currentPos
                    )

                    // Draw primary coin disc
                    drawCircle(
                        color = coinColor,
                        radius = coinRadius,
                        center = currentPos
                    )

                    // Draw inner highlight rim
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = coinRadius * 0.7f,
                        center = Offset(currentPos.x - coinRadius * 0.2f, currentPos.y - coinRadius * 0.2f)
                    )
                }
            }
        }
    }
}

private fun calculateControlPoint(start: Offset, end: Offset): Offset {
    val midX = (start.x + end.x) / 2f
    // Arch upward relative to the higher of the two points
    val peakY = min(start.y, end.y) - max(140f, kotlin.math.abs(start.x - end.x) * 0.35f)
    return Offset(midX, peakY)
}

private fun evaluateQuadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u

    val x = uu * p0.x + 2 * u * t * p1.x + tt * p2.x
    val y = uu * p0.y + 2 * u * t * p1.y + tt * p2.y
    return Offset(x, y)
}
