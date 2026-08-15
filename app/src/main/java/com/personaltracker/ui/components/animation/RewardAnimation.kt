package com.personaltracker.ui.components.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Supported One-Shot Reward Animation Types.
 * Sourced from `design-system.md` Gamification Visual System & Motion Principles.
 */
enum class RewardAnimationType(
    val assetPath: String,
    val defaultSize: Dp,
    val durationMs: Int
) {
    /**
     * Micro coin earn: ~300ms, plays inline on task/habit completion.
     * PLACEHOLDER: Points to placeholder JSON asset until designer asset lands.
     */
    CoinEarnMicro(
        assetPath = "animations/coin_earn_micro.json",
        defaultSize = 48.dp,
        durationMs = 300
    ),

    /**
     * Milestone full reveal: ~800-1000ms, plays inside modal reward sheet with confetti.
     * PLACEHOLDER: Points to placeholder JSON asset until designer asset lands.
     */
    MilestoneFullReveal(
        assetPath = "animations/milestone_full_reveal.json",
        defaultSize = 200.dp,
        durationMs = 800
    )
}

/**
 * Reusable One-Shot Reward Animation Composable.
 *
 * NOTE: Currently wired against PLACEHOLDER Lottie JSON assets.
 * When real designer-authored Lottie animations land, replace the files in
 * `app/src/main/assets/animations/` with matching file names. No code changes required.
 *
 * @param type The preset reward animation type
 * @param isPlaying Trigger flag to start the one-shot animation
 * @param onAnimationComplete Callback invoked when animation completes its single play
 * @param modifier Composable modifier
 */
@Composable
fun RewardAnimation(
    type: RewardAnimationType,
    isPlaying: Boolean,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    RewardAnimation(
        assetPath = type.assetPath,
        isPlaying = isPlaying,
        onAnimationComplete = onAnimationComplete,
        modifier = modifier.size(type.defaultSize)
    )
}

/**
 * Direct asset-path overload for RewardAnimation.
 */
@Composable
fun RewardAnimation(
    assetPath: String,
    isPlaying: Boolean,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = 1,
        restartOnPlay = true
    )

    LaunchedEffect(progress, isPlaying) {
        if (isPlaying && progress >= 1f) {
            onAnimationComplete()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        }
    }
}
