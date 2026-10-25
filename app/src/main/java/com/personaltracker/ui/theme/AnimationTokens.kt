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
 * Sourced 1:1 from `design-system.md` Motion & Animation Principles section and Framer Project QaEW7GnAiltJMNVpWjTs-dKIrE.
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

    // ─── Framer-to-Compose Spring Physics (QaEW7GnAiltJMNVpWjTs-dKIrE) ───
    /**
     * 7-Day Capsule Morph physics: Framer stiffness 380, damping 28.
     */
    fun <T> dayCapsuleSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.72f, stiffness = 380f)

    /**
     * Timeline Checkmark Burst: Framer stiffness 450, damping 20, 1.0 -> 1.25 -> 1.0.
     */
    fun <T> timelineBurstSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 450f)

    /**
     * Shared Element FAB -> Modal Sheet: Framer stiffness 320, damping 26.
     */
    fun <T> fabToModalSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 320f)

    /**
     * Staggered Vertical Progress Bar Growth: Framer stiffness 220, damping 24.
     */
    fun <T> staggeredBarSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.81f, stiffness = 220f)

    /**
     * Focus Timer Pause/Resume feedback: Framer stiffness 350, damping 25.
     */
    fun <T> timerPauseResumeSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.74f, stiffness = 350f)

    /**
     * Shop Purchase Burst physics: Framer stiffness 400, damping 18.
     */
    fun <T> shopPurchaseBurstSpringSpec(): AnimationSpec<T> =
        spring(dampingRatio = 0.50f, stiffness = 400f)

    // ─── Standard Animation Specs ───
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
