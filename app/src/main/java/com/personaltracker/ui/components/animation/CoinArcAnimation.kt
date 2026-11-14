package com.personaltracker.ui.components.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * High-performance VSYNC-aligned Coin Arc Motion Composable (AD-7, Story 4.3).
 *
 * Sourced from `design-system.md` Gamification Visual System & Story 4.3 Specification:
 * - Animates coins along a quadratic Bézier curve from item origin to top-right Coin HUD pill.
 * - Monotonic Frame Clock: Driven by a single coroutine loop (`withFrameNanos`) to adapt seamlessly
 *   across 60Hz, 90Hz, and 120Hz displays with zero stutter or speed anomalies.
 * - Zero Allocation Guarantee: Precomputes control points outside `Canvas`, pre-caches radii and
 *   colors, and uses primitive float calculations inside `onDraw` with zero heap allocations.
 * - Clamps dynamic curve apex to safe headroom ([PersonalTrackerTheme.spacing.minTouchTarget])
 *   to avoid flying into the status bar or display notch.
 * - Reduced-Motion Honor: Bypasses flight trajectories when system animation scale is 0f.
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
    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val coinColor = colors.coinGold
    val coinGlowColor = colors.coinGlow
    val highlightColor = remember { Color.White.copy(alpha = 0.4f) }

    val density = LocalDensity.current
    val baseCoinRadiusPx = with(density) { (spacing.coinIconSize / 2f).toPx() }
    val minHeadroomPx = with(density) { spacing.minTouchTarget.toPx() }

    // Precompute apex control point outside Canvas to eliminate per-frame allocations
    val controlPoint = remember(startOffset, endOffset, minHeadroomPx) {
        BezierMath.calculateArcControlPoint(
            start = startOffset,
            end = endOffset,
            minHeadroomPx = minHeadroomPx
        )
    }

    // Accessibility: check if animations are disabled at the system level
    val context = LocalContext.current
    val isReducedMotion = remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0f
        }.getOrDefault(false)
    }

    // Single master progress driving all staggered coins
    var masterProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, startOffset, endOffset, coinCount, isReducedMotion) {
        if (!isPlaying) return@LaunchedEffect

        // If reduced motion or invalid coordinates, bypass trajectory immediately
        if (isReducedMotion || coinCount <= 0 || startOffset == Offset.Zero || endOffset == Offset.Zero) {
            onComplete()
            return@LaunchedEffect
        }

        masterProgress = 0f
        val totalDurationMs = BezierMath.calculateTotalDurationMs(
            coinCount = coinCount,
            flightDurationMs = AnimationTokens.coinFlightDurationMs,
            staggerDelayMs = AnimationTokens.coinStaggerDelayMs
        )
        val totalDurationNanos = totalDurationMs * 1_000_000L
        val startTime = withFrameNanos { it }

        while (true) {
            val currentFrameTime = withFrameNanos { it }
            val elapsedNanos = currentFrameTime - startTime
            val progress = if (totalDurationNanos > 0L) {
                (elapsedNanos.toFloat() / totalDurationNanos).coerceIn(0f, 1f)
            } else {
                1f
            }
            masterProgress = progress
            if (progress >= 1f) break
        }

        onComplete()
    }

    if (isPlaying && !isReducedMotion && masterProgress < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val p0x = startOffset.x
            val p0y = startOffset.y
            val p1x = controlPoint.x
            val p1y = controlPoint.y
            val p2x = endOffset.x
            val p2y = endOffset.y

            for (i in 0 until coinCount) {
                val rawT = BezierMath.calculateStaggeredProgress(
                    masterProgress = masterProgress,
                    index = i,
                    totalCount = coinCount,
                    flightDurationMs = AnimationTokens.coinFlightDurationMs,
                    staggerDelayMs = AnimationTokens.coinStaggerDelayMs
                )

                if (rawT > 0f && rawT < 1f) {
                    val easedT = FastOutSlowInEasing.transform(rawT)
                    val currentX = BezierMath.evaluateQuadraticBezierX(p0x, p1x, p2x, easedT)
                    val currentY = BezierMath.evaluateQuadraticBezierY(p0y, p1y, p2y, easedT)

                    // Dynamic scaling: scale up slightly in mid-air (1.0f -> 1.25f -> 1.0f)
                    val midAirFactor = 2f * easedT - 1f
                    val scale = 1f + 0.25f * (1f - midAirFactor * midAirFactor)
                    val coinRadius = baseCoinRadiusPx * scale

                    // Outer subtle glow
                    drawCircle(
                        color = coinGlowColor,
                        radius = coinRadius * 1.6f,
                        center = Offset(currentX, currentY)
                    )

                    // Primary coin disc
                    drawCircle(
                        color = coinColor,
                        radius = coinRadius,
                        center = Offset(currentX, currentY)
                    )

                    // Inner specular highlight
                    drawCircle(
                        color = highlightColor,
                        radius = coinRadius * 0.7f,
                        center = Offset(currentX - coinRadius * 0.2f, currentY - coinRadius * 0.2f)
                    )
                }
            }
        }
    }
}
