package com.personaltracker.ui.components.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Event payload representing a coin reward animation request.
 */
@Immutable
data class CoinRewardEvent(
    val startOffset: Offset,
    val coinCount: Int = 1,
    val onComplete: (() -> Unit)? = null
)

/**
 * UI State for viewport-hoisted reward animations.
 */
@Immutable
data class RewardMotionState(
    val activeCoinEvent: CoinRewardEvent? = null,
    val destinationTargetOffset: Offset = Offset.Zero,
    val activeLottieAnimation: RewardAnimationType? = null,
    val isLottiePlaying: Boolean = false
)

/**
 * Hoisted State controller for [RewardMotionStage].
 */
@Stable
class RewardMotionStageState {
    var state by mutableStateOf(RewardMotionState())
        private set

    fun updateDestinationTarget(offset: Offset) {
        state = state.copy(destinationTargetOffset = offset)
    }

    fun dispatchCoinReward(startOffset: Offset, coinCount: Int = 1, onComplete: (() -> Unit)? = null) {
        if (coinCount <= 0 || startOffset == Offset.Zero) {
            onComplete?.invoke()
            return
        }

        // If a previous coin event is active, complete it before replacing
        state.activeCoinEvent?.onComplete?.invoke()

        state = state.copy(
            activeCoinEvent = CoinRewardEvent(
                startOffset = startOffset,
                coinCount = coinCount,
                onComplete = onComplete
            )
        )
    }

    fun dispatchLottieReward(type: RewardAnimationType) {
        state = state.copy(
            activeLottieAnimation = type,
            isLottiePlaying = true
        )
    }

    fun onCoinArcCompleted() {
        state.activeCoinEvent?.onComplete?.invoke()
        state = state.copy(activeCoinEvent = null)
    }

    fun onLottieCompleted() {
        state = state.copy(
            activeLottieAnimation = null,
            isLottiePlaying = false
        )
    }
}

@Composable
fun rememberRewardMotionStageState(): RewardMotionStageState {
    return remember { RewardMotionStageState() }
}

/**
 * Viewport-hoisted motion layer hosting unclipped [CoinArcAnimation] and [RewardAnimation]
 * at the root scaffold level above navigation hosts (AD-7).
 *
 * Prevents particle and vector animations from being clipped by nested scroll boundaries.
 */
@Composable
fun RewardMotionStage(
    state: RewardMotionState,
    onCoinArcComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onLottieComplete: () -> Unit = {}
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val spacing = PersonalTrackerTheme.spacing

    // Top-right fallback coordinate dynamically calculated from screen density and width
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val topPaddingPx = with(density) { spacing.minTouchTarget.toPx() }
    val rightPaddingPx = with(density) { spacing.spacing8.toPx() }
    val fallbackDestination = Offset(x = screenWidthPx - rightPaddingPx, y = topPaddingPx)

    val effectiveDestination = if (state.destinationTargetOffset != Offset.Zero) {
        state.destinationTargetOffset
    } else {
        fallbackDestination
    }

    val isCoinPlaying = state.activeCoinEvent != null && state.activeCoinEvent.startOffset != Offset.Zero

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Full viewport unclipped quadratic Bezier coin arc animation
        if (isCoinPlaying) {
            CoinArcAnimation(
                isPlaying = true,
                startOffset = state.activeCoinEvent!!.startOffset,
                endOffset = effectiveDestination,
                coinCount = state.activeCoinEvent.coinCount,
                onComplete = onCoinArcComplete,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Full viewport modal reward celebration animation
        if (state.activeLottieAnimation != null && state.isLottiePlaying) {
            RewardAnimation(
                type = state.activeLottieAnimation,
                isPlaying = true,
                onAnimationComplete = onLottieComplete,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
