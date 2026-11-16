package com.personaltracker.ui.components.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.theme.PersonalTrackerTheme
import java.util.UUID

/**
 * Event payload representing a coin reward animation request.
 */
@Immutable
data class CoinRewardEvent(
    val id: String = UUID.randomUUID().toString(),
    val startOffset: Offset,
    val coinCount: Int = 1,
    val onComplete: (() -> Unit)? = null
)

/**
 * UI State for viewport-hoisted reward animations.
 * Supports concurrent coin reward events without preemption or mid-air cancellation (Story 4.3).
 */
@Immutable
data class RewardMotionState(
    val activeCoinEvents: List<CoinRewardEvent> = emptyList(),
    val destinationTargetOffset: Offset = Offset.Zero,
    val activeLottieAnimation: RewardAnimationType? = null,
    val isLottiePlaying: Boolean = false
) {
    // Backwards-compatible accessor for single-event queries
    val activeCoinEvent: CoinRewardEvent?
        get() = activeCoinEvents.lastOrNull()
}

/**
 * Hoisted State controller for [RewardMotionStage].
 */
@Stable
class RewardMotionStageState {
    var state by mutableStateOf(RewardMotionState())
        private set

    /**
     * Optional callback notified when a batch of coins arrives at the destination HUD.
     * Passes the count of arriving coins.
     */
    var onCoinArrival: ((Int) -> Unit)? = null

    fun updateDestinationTarget(offset: Offset) {
        state = state.copy(destinationTargetOffset = offset)
    }

    /**
     * Dispatches a new coin reward burst.
     * Sanitizes negative coordinates to prevent inverted trajectories.
     * Appends to activeCoinEvents to support concurrent bursts.
     */
    fun dispatchCoinReward(startOffset: Offset, coinCount: Int = 1, onComplete: (() -> Unit)? = null) {
        if (coinCount <= 0 || startOffset == Offset.Zero) {
            onComplete?.invoke()
            return
        }

        // Coordinate sanitization: prevent off-screen negative anchors
        val sanitizedStart = Offset(
            x = maxOf(0f, startOffset.x),
            y = maxOf(0f, startOffset.y)
        )

        val newEvent = CoinRewardEvent(
            startOffset = sanitizedStart,
            coinCount = coinCount,
            onComplete = onComplete
        )

        state = state.copy(
            activeCoinEvents = state.activeCoinEvents + newEvent
        )
    }

    fun dispatchLottieReward(type: RewardAnimationType) {
        state = state.copy(
            activeLottieAnimation = type,
            isLottiePlaying = true
        )
    }

    /**
     * Completes a specific coin arc by [eventId], or the oldest event if unspecified.
     * Invokes completion callback and triggers [onCoinArrival].
     */
    fun onCoinArcCompleted(eventId: String? = null) {
        val eventToComplete = if (eventId != null) {
            state.activeCoinEvents.find { it.id == eventId }
        } else {
            state.activeCoinEvents.firstOrNull()
        }

        if (eventToComplete != null) {
            state = state.copy(
                activeCoinEvents = state.activeCoinEvents.filterNot { it.id == eventToComplete.id }
            )
            eventToComplete.onComplete?.invoke()
            onCoinArrival?.invoke(eventToComplete.coinCount)
        }
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
 * at the root scaffold level above navigation hosts (AD-7, Story 4.3).
 *
 * Story 4.3 Enhancements:
 * - Supports concurrent coin bursts ([RewardMotionState.activeCoinEvents]) without canceling in-flight coins.
 * - Coordinates arrival roll-up via [RewardMotionStageState.onCoinArrival].
 * - Adjusts coordinates relative to stage window bounds.
 * - Reduced-motion bypass: skips animations when animator scale is 0.
 */
@Composable
fun RewardMotionStage(
    state: RewardMotionState,
    onCoinArcComplete: (String) -> Unit,
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

    // Stage-relative coordinate adjustment
    var stageOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

    // Reduced motion check
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

    // If reduced motion is active, immediately complete any arriving events
    LaunchedEffect(state.activeCoinEvents, isReducedMotion) {
        if (isReducedMotion && state.activeCoinEvents.isNotEmpty()) {
            state.activeCoinEvents.forEach { event ->
                onCoinArcComplete(event.id)
            }
        }
    }

    // Modal Lottie reduced-motion bypass
    LaunchedEffect(state.isLottiePlaying, isReducedMotion) {
        if (isReducedMotion && state.isLottiePlaying) {
            onLottieComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    stageOffsetInWindow = coordinates.positionInWindow()
                }
            }
    ) {
        // Full viewport unclipped quadratic Bézier coin arc animations (concurrent bursts)
        if (!isReducedMotion) {
            state.activeCoinEvents.forEach { event ->
                val adjustedStart = Offset(
                    x = event.startOffset.x - stageOffsetInWindow.x,
                    y = event.startOffset.y - stageOffsetInWindow.y
                )
                val adjustedEnd = Offset(
                    x = effectiveDestination.x - stageOffsetInWindow.x,
                    y = effectiveDestination.y - stageOffsetInWindow.y
                )

                key(event.id) {
                    CoinArcAnimation(
                        isPlaying = true,
                        startOffset = adjustedStart,
                        endOffset = adjustedEnd,
                        coinCount = event.coinCount,
                        onComplete = { onCoinArcComplete(event.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Full viewport modal reward celebration animation (guarded against reduced motion)
        if (!isReducedMotion && state.activeLottieAnimation != null && state.isLottiePlaying) {
            RewardAnimation(
                type = state.activeLottieAnimation,
                isPlaying = true,
                onAnimationComplete = onLottieComplete,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Overload for backward compatibility with 0-arg [onCoinArcComplete].
 */
@Composable
fun RewardMotionStage(
    state: RewardMotionState,
    onCoinArcComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onLottieComplete: () -> Unit = {}
) {
    RewardMotionStage(
        state = state,
        onCoinArcComplete = { _ -> onCoinArcComplete() },
        modifier = modifier,
        onLottieComplete = onLottieComplete
    )
}
