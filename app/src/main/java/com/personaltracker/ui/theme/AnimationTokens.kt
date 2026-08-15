package com.personaltracker.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion & Animation Timing Bands and Curves for Personal-Tracker.
 * Governing rule: "Motion communicates real state change resulting from a real user action. Never decoration."
 * Sourced 1:1 from `design-system.md` Motion & Animation Principles section.
 */
@Immutable
object AnimationTokens {
    // ─── Duration Bands (in Milliseconds) ───
    const val DurationMicroShort = 150
    const val DurationMicro = 200
    const val DurationMicroLong = 250

    const val DurationStateShort = 300
    const val DurationState = 350
    const val DurationStateLong = 400

    const val DurationScreenShort = 350
    const val DurationScreen = 400
    const val DurationScreenLong = 500

    const val DurationRewardShort = 600
    const val DurationReward = 800
    const val DurationRewardLong = 1000

    // ─── Easings ───
    val StandardEasing = FastOutSlowInEasing
    val OvershootSpringEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val GentleEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val Linear = LinearEasing

    // ─── Animation Specs ───
    /**
     * Micro-interaction: Checkbox check, chip selection, row highlight, button press feedback.
     * Duration: 150-250ms.
     */
    fun <T> microSpec(durationMillis: Int = DurationMicro): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = StandardEasing)

    /**
     * State transition: Card completion state change, progress bar fill, bottom sheet snap.
     * Duration: 300-400ms.
     */
    fun <T> stateSpec(durationMillis: Int = DurationState): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = StandardEasing)

    /**
     * Screen transition: Screen navigation, modal bottom sheet entrance.
     * Duration: 350-500ms.
     */
    fun <T> screenSpec(durationMillis: Int = DurationScreen): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = StandardEasing)

    /**
     * Reward reveal: Coin earn animation, streak milestone, companion celebration, reward-card reveal.
     * Custom spring curve with overshoot and settle.
     * Duration: 600-1000ms.
     */
    fun <T> rewardSpringSpec(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessLow
    ): AnimationSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

    /**
     * Continuous session: Focus session growth metaphor, streak flame flicker.
     */
    fun <T> continuousSpec(durationMillis: Int): AnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = GentleEaseInOut)
}

val LocalPersonalTrackerAnimationTokens = staticCompositionLocalOf { AnimationTokens }
