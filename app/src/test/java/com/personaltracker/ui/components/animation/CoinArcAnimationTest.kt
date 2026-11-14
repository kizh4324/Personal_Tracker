package com.personaltracker.ui.components.animation

import androidx.compose.ui.geometry.Offset
import com.personaltracker.ui.theme.AnimationTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit test suite verifying quadratic Bézier trajectory math, control point calculation
 * with apex headroom clamping, staggered particle progression, and RewardMotionStage concurrency (Story 4.3).
 */
class CoinArcAnimationTest {

    // ─── 1. Quadratic Bézier Curve Math (Boundary & Midpoint) ───

    @Test
    fun `evaluateQuadraticBezier at t = 0f returns exact origin P0`() {
        val p0 = Offset(100f, 800f)
        val p1 = Offset(300f, 200f)
        val p2 = Offset(500f, 100f)

        val result = BezierMath.evaluateQuadraticBezier(p0, p1, p2, 0f)
        assertEquals(p0.x, result.x, 0.001f)
        assertEquals(p0.y, result.y, 0.001f)
    }

    @Test
    fun `evaluateQuadraticBezier at t = 1f returns exact target P2`() {
        val p0 = Offset(100f, 800f)
        val p1 = Offset(300f, 200f)
        val p2 = Offset(500f, 100f)

        val result = BezierMath.evaluateQuadraticBezier(p0, p1, p2, 1f)
        assertEquals(p2.x, result.x, 0.001f)
        assertEquals(p2.y, result.y, 0.001f)
    }

    @Test
    fun `evaluateQuadraticBezier at t = 0_5f returns weighted midpoint formula`() {
        val p0 = Offset(0f, 0f)
        val p1 = Offset(200f, 400f)
        val p2 = Offset(400f, 0f)

        // B(0.5) = 0.25*P0 + 0.5*P1 + 0.25*P2
        // X = 0 + 0.5 * 200 + 0.25 * 400 = 100 + 100 = 200
        // Y = 0 + 0.5 * 400 + 0 = 200
        val result = BezierMath.evaluateQuadraticBezier(p0, p1, p2, 0.5f)
        assertEquals(200f, result.x, 0.001f)
        assertEquals(200f, result.y, 0.001f)
    }

    @Test
    fun `evaluateQuadraticBezier primitive overloads match Offset overloads`() {
        val p0 = Offset(120f, 650f)
        val p1 = Offset(250f, 150f)
        val p2 = Offset(380f, 80f)
        val t = 0.73f

        val offsetResult = BezierMath.evaluateQuadraticBezier(p0, p1, p2, t)
        val xResult = BezierMath.evaluateQuadraticBezierX(p0.x, p1.x, p2.x, t)
        val yResult = BezierMath.evaluateQuadraticBezierY(p0.y, p1.y, p2.y, t)

        assertEquals(offsetResult.x, xResult, 0.0001f)
        assertEquals(offsetResult.y, yResult, 0.0001f)
    }

    // ─── 2. Control Point & Headroom Clamping ───

    @Test
    fun `calculateArcControlPoint places midX halfway between P0 and P2`() {
        val p0 = Offset(100f, 800f)
        val p2 = Offset(400f, 100f)

        val cp = BezierMath.calculateArcControlPoint(p0, p2, minArcHeight = 140f, minHeadroomPx = 48f)
        assertEquals(250f, cp.x, 0.001f)
    }

    @Test
    fun `calculateArcControlPoint clamps peakY to minHeadroomPx when raw peak is negative or above safe boundary`() {
        // High origin and target, large delta leading to peakY well above screen top (y < 0)
        val p0 = Offset(100f, 150f)
        val p2 = Offset(300f, 80f)
        val minHeadroom = 48f

        val cp = BezierMath.calculateArcControlPoint(p0, p2, minArcHeight = 140f, minHeadroomPx = minHeadroom)
        // rawPeakY = min(150, 80) - max(140, ...) = 80 - 140 = -60f
        // peakY = max(48f, -60f) = 48f
        assertTrue("peakY must be >= minHeadroomPx", cp.y >= minHeadroom)
        assertEquals(minHeadroom, cp.y, 0.001f)
    }

    @Test
    fun `calculateArcControlPoint allows natural arc when raw peak is below minHeadroomPx`() {
        val p0 = Offset(100f, 900f)
        val p2 = Offset(300f, 600f)
        val minHeadroom = 48f

        val cp = BezierMath.calculateArcControlPoint(p0, p2, minArcHeight = 140f, minHeadroomPx = minHeadroom)
        // rawPeakY = 600 - max(140, 200 * 0.35 + 300 * 0.25) = 600 - max(140, 70 + 75 = 145) = 455f
        assertEquals(455f, cp.y, 0.001f)
        assertTrue(cp.y > minHeadroom)
    }

    // ─── 3. Timeline Math & Staggered Multi-Coin Progress ───

    @Test
    fun `calculateTotalDurationMs satisfies T equals D plus N minus 1 times S`() {
        val d = AnimationTokens.coinFlightDurationMs // 600L
        val s = AnimationTokens.coinStaggerDelayMs  // 80L

        // Single coin: 600ms
        assertEquals(600L, BezierMath.calculateTotalDurationMs(1, d, s))

        // 7 coins (standard task award): 600 + 6 * 80 = 1080ms
        assertEquals(1080L, BezierMath.calculateTotalDurationMs(7, d, s))

        // Zero or negative input clamped to 1 coin duration
        assertEquals(600L, BezierMath.calculateTotalDurationMs(0, d, s))
        assertEquals(600L, BezierMath.calculateTotalDurationMs(-5, d, s))
    }

    @Test
    fun `calculateStaggeredProgress returns 0f before coin departure fraction`() {
        val totalCount = 7
        val flightDuration = 600L
        val staggerDelay = 80L
        val totalDuration = BezierMath.calculateTotalDurationMs(totalCount, flightDuration, staggerDelay).toFloat()

        // Coin index 3 departs at: 3 * 80 / 1080 = 240 / 1080 ~ 0.222f
        val startFraction = (3 * staggerDelay) / totalDuration

        // Master progress at 0.10f (before departure)
        val progress = BezierMath.calculateStaggeredProgress(
            masterProgress = 0.10f,
            index = 3,
            totalCount = totalCount,
            flightDurationMs = flightDuration,
            staggerDelayMs = staggerDelay
        )
        assertEquals(0f, progress, 0.001f)

        // At exact departure time: progress should be 0f
        val progressAtStart = BezierMath.calculateStaggeredProgress(
            masterProgress = startFraction,
            index = 3,
            totalCount = totalCount,
            flightDurationMs = flightDuration,
            staggerDelayMs = staggerDelay
        )
        assertEquals(0f, progressAtStart, 0.001f)
    }

    @Test
    fun `calculateStaggeredProgress returns 1f after coin flight completes`() {
        val totalCount = 7
        val flightDuration = 600L
        val staggerDelay = 80L
        val totalDuration = BezierMath.calculateTotalDurationMs(totalCount, flightDuration, staggerDelay).toFloat()

        // Coin index 0 completes at 600 / 1080 ~ 0.555f
        val coin0ArrivalFraction = flightDuration / totalDuration

        val progressAtArrival = BezierMath.calculateStaggeredProgress(
            masterProgress = coin0ArrivalFraction,
            index = 0,
            totalCount = totalCount,
            flightDurationMs = flightDuration,
            staggerDelayMs = staggerDelay
        )
        assertEquals(1f, progressAtArrival, 0.001f)

        // After arrival, remains clamped at 1f
        val progressAfterArrival = BezierMath.calculateStaggeredProgress(
            masterProgress = 0.85f,
            index = 0,
            totalCount = totalCount,
            flightDurationMs = flightDuration,
            staggerDelayMs = staggerDelay
        )
        assertEquals(1f, progressAfterArrival, 0.001f)
    }

    @Test
    fun `calculateStaggeredProgress for single coin matches master progress exactly`() {
        for (m in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val progress = BezierMath.calculateStaggeredProgress(
                masterProgress = m,
                index = 0,
                totalCount = 1
            )
            assertEquals(m, progress, 0.001f)
        }
    }

    // ─── 4. RewardMotionStage Concurrency & Arrival Notification ───

    @Test
    fun `RewardMotionStageState supports concurrent activeCoinEvents without preemption`() {
        val stageState = RewardMotionStageState()
        var arrivalCount = 0
        stageState.onCoinArrival = { count -> arrivalCount += count }

        var event1Completed = false
        var event2Completed = false

        // Dispatch burst 1 (+7 coins)
        stageState.dispatchCoinReward(
            startOffset = Offset(100f, 200f),
            coinCount = 7,
            onComplete = { event1Completed = true }
        )

        // Dispatch burst 2 (+3 coins) rapidly while burst 1 in flight
        stageState.dispatchCoinReward(
            startOffset = Offset(150f, 350f),
            coinCount = 3,
            onComplete = { event2Completed = true }
        )

        // Both bursts must be concurrently active in state without canceling event 1
        assertEquals(2, stageState.state.activeCoinEvents.size)
        assertFalse(event1Completed)
        assertFalse(event2Completed)

        // Complete burst 1
        val event1Id = stageState.state.activeCoinEvents[0].id
        stageState.onCoinArcCompleted(event1Id)

        assertTrue(event1Completed)
        assertFalse(event2Completed)
        assertEquals(7, arrivalCount)
        assertEquals(1, stageState.state.activeCoinEvents.size)

        // Complete burst 2
        val event2Id = stageState.state.activeCoinEvents[0].id
        stageState.onCoinArcCompleted(event2Id)

        assertTrue(event2Completed)
        assertEquals(10, arrivalCount)
        assertEquals(0, stageState.state.activeCoinEvents.size)
    }

    @Test
    fun `dispatchCoinReward sanitizes negative start coordinates to prevent inverted trajectory`() {
        val stageState = RewardMotionStageState()
        stageState.dispatchCoinReward(
            startOffset = Offset(-50f, -100f),
            coinCount = 5
        )

        val activeEvent = stageState.state.activeCoinEvents.firstOrNull()
        assertNotNull(activeEvent)
        assertEquals(0f, activeEvent!!.startOffset.x, 0.001f)
        assertEquals(0f, activeEvent.startOffset.y, 0.001f)
    }

    @Test
    fun `dispatchCoinReward immediately completes when coinCount is zero or negative`() {
        val stageState = RewardMotionStageState()
        var completed = false
        stageState.dispatchCoinReward(
            startOffset = Offset(100f, 200f),
            coinCount = 0,
            onComplete = { completed = true }
        )
        assertTrue(completed)
        assertTrue(stageState.state.activeCoinEvents.isEmpty())
    }
}
