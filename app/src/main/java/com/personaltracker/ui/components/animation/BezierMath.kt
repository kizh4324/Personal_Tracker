package com.personaltracker.ui.components.animation

import androidx.compose.ui.geometry.Offset
import com.personaltracker.ui.theme.AnimationTokens
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin Quadratic Bézier Curve & Staggered Particle Math Helper (AD-1, AD-7, Story 4.3).
 *
 * Implements deterministic, zero-allocation quadratic Bézier trajectory evaluation,
 * dynamic apex control point calculation with safe headroom clamping (display notch/bezel),
 * and staggered timeline progression math.
 *
 * Sourced 1:1 from Story 4.3 Design Notes § 1:
 * - B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2, t in [0, 1]
 * - Total duration: T = D + (N - 1) * S (D = 600ms, S = 80ms)
 * - Staggered progress: progress_i = ((M - startFraction_i) / durationFraction).coerceIn(0f, 1f)
 */
object BezierMath {

    /**
     * Evaluates X coordinate along a quadratic Bézier curve at interpolation factor [t] in [0f, 1f].
     * Zero-allocation primitive evaluation for 60Hz/120Hz render loops.
     */
    fun evaluateQuadraticBezierX(p0x: Float, p1x: Float, p2x: Float, t: Float): Float {
        val clampedT = t.coerceIn(0f, 1f)
        val u = 1f - clampedT
        return u * u * p0x + 2f * u * clampedT * p1x + clampedT * clampedT * p2x
    }

    /**
     * Evaluates Y coordinate along a quadratic Bézier curve at interpolation factor [t] in [0f, 1f].
     * Zero-allocation primitive evaluation for 60Hz/120Hz render loops.
     */
    fun evaluateQuadraticBezierY(p0y: Float, p1y: Float, p2y: Float, t: Float): Float {
        val clampedT = t.coerceIn(0f, 1f)
        val u = 1f - clampedT
        return u * u * p0y + 2f * u * clampedT * p1y + clampedT * clampedT * p2y
    }

    /**
     * Evaluates 2D point [Offset] along a quadratic Bézier curve at interpolation factor [t] in [0f, 1f].
     */
    fun evaluateQuadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
        return Offset(
            x = evaluateQuadraticBezierX(p0.x, p1.x, p2.x, t),
            y = evaluateQuadraticBezierY(p0.y, p1.y, p2.y, t)
        )
    }

    /**
     * Primitive float overload for [evaluateQuadraticBezier].
     */
    fun evaluateQuadraticBezier(
        p0x: Float,
        p0y: Float,
        p1x: Float,
        p1y: Float,
        p2x: Float,
        p2y: Float,
        t: Float
    ): Offset {
        return Offset(
            x = evaluateQuadraticBezierX(p0x, p1x, p2x, t),
            y = evaluateQuadraticBezierY(p0y, p1y, p2y, t)
        )
    }

    /**
     * Calculates dynamic apex control point P1 between origin P0 and destination P2.
     *
     * Midpoint X: (P0.x + P2.x) / 2
     * Arc height scales with horizontal and vertical distance.
     * Peak Y is clamped to [minHeadroomPx] (e.g. status bar / notch safe zone)
     * so coins never fly off-screen.
     */
    fun calculateArcControlPoint(
        start: Offset,
        end: Offset,
        minArcHeight: Float = 140f,
        minHeadroomPx: Float = 0f
    ): Offset {
        return calculateArcControlPoint(
            p0x = start.x,
            p0y = start.y,
            p2x = end.x,
            p2y = end.y,
            minArcHeight = minArcHeight,
            minHeadroomPx = minHeadroomPx
        )
    }

    /**
     * Primitive float overload for [calculateArcControlPoint].
     */
    fun calculateArcControlPoint(
        p0x: Float,
        p0y: Float,
        p2x: Float,
        p2y: Float,
        minArcHeight: Float = 140f,
        minHeadroomPx: Float = 0f
    ): Offset {
        val midX = (p0x + p2x) / 2f
        val deltaX = abs(p0x - p2x)
        val deltaY = abs(p0y - p2y)
        val dynamicArc = max(minArcHeight, deltaX * 0.35f + deltaY * 0.25f)
        val rawPeakY = min(p0y, p2y) - dynamicArc
        val peakY = max(minHeadroomPx, rawPeakY)
        return Offset(midX, peakY)
    }

    /**
     * Calculates total animation duration in milliseconds for [coinCount] coins.
     * Formula: T = D + (N - 1) * S
     */
    fun calculateTotalDurationMs(
        coinCount: Int,
        flightDurationMs: Long = AnimationTokens.coinFlightDurationMs,
        staggerDelayMs: Long = AnimationTokens.coinStaggerDelayMs
    ): Long {
        val count = max(1, coinCount)
        return flightDurationMs + (count - 1) * staggerDelayMs
    }

    /**
     * Computes the individual progress t in [0f, 1f] for coin [index] given master timeline progress
     * [masterProgress] in [0f, 1f].
     *
     * startFraction_i = (i * S) / T
     * durationFraction = D / T
     * progress_i = ((M - startFraction_i) / durationFraction).coerceIn(0f, 1f)
     */
    fun calculateStaggeredProgress(
        masterProgress: Float,
        index: Int,
        totalCount: Int,
        flightDurationMs: Long = AnimationTokens.coinFlightDurationMs,
        staggerDelayMs: Long = AnimationTokens.coinStaggerDelayMs
    ): Float {
        if (totalCount <= 0) return 1f
        val clampedMaster = masterProgress.coerceIn(0f, 1f)
        val totalDuration = calculateTotalDurationMs(totalCount, flightDurationMs, staggerDelayMs).toFloat()
        if (totalDuration <= 0f) return 1f

        val startFraction = (index * staggerDelayMs).toFloat() / totalDuration
        val durationFraction = flightDurationMs.toFloat() / totalDuration
        if (durationFraction <= 0f) return 1f

        return ((clampedMaster - startFraction) / durationFraction).coerceIn(0f, 1f)
    }
}
