package com.personaltracker.ui.components.atoms

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.R
import com.personaltracker.ui.theme.AnimationTokens
import com.personaltracker.ui.theme.PersonalTrackerTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Event payload notifying [CoinPill] of an arriving coin batch.
 */
@Immutable
data class CoinArrivalEvent(
    val count: Int,
    val timestamp: Long = System.nanoTime()
)

/**
 * Atomic Coin HUD Pill component (AD-7, EXP-016, Story 4.3).
 *
 * Displays the current coin balance and optional daily earn cap progress.
 * Measures its window-level coordinates via [onGloballyPositioned] to serve as the
 * destination target for [com.personaltracker.ui.components.animation.CoinArcAnimation].
 *
 * Story 4.3 Enhancements:
 * - Arrival-triggered Framer-calibrated spring counter roll-up ([displayedBalance]).
 * - Subtle 1.18x spring scale burst using [AnimationTokens.shopPurchaseBurstSpringSpec].
 * - Floating `"+X"` badge pill with `Modifier.graphicsLayer` translation and fade on RenderThread.
 * - Initial-mount suppression: cold starts display initial balance with zero scale burst or badge.
 * - Accessibility reduced-motion bypass: snaps immediately to target balance when animations disabled.
 */
@Composable
fun CoinPill(
    balance: Int,
    modifier: Modifier = Modifier,
    dailyEarned: Int? = null,
    dailyCap: Int? = null,
    arrivalEvent: CoinArrivalEvent? = null,
    onPositionMeasured: (Offset) -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Accessibility: check if animations are disabled
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

    // Cold-start guard: suppress spurious burst/badge on initial mount
    var isInitialLoad by remember { mutableStateOf(true) }
    var displayedBalance by remember { mutableIntStateOf(balance) }

    // Spring scale burst and floating badge animatables
    val scaleAnim = remember { Animatable(1.0f) }
    val badgeProgress = remember { Animatable(1.0f) }
    var floatingBadgeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(balance) {
        if (isInitialLoad) {
            displayedBalance = balance
            isInitialLoad = false
            return@LaunchedEffect
        }

        // If reduced motion or spending coins (decrease), update immediately without animation
        if (isReducedMotion || balance <= displayedBalance || arrivalEvent == null) {
            displayedBalance = balance
        }
    }

    // React to arrival events when coins physically arrive at HUD
    LaunchedEffect(arrivalEvent) {
        val event = arrivalEvent ?: return@LaunchedEffect
        if (isInitialLoad || event.count <= 0) return@LaunchedEffect

        if (isReducedMotion) {
            displayedBalance = balance
            return@LaunchedEffect
        }

        floatingBadgeCount = event.count

        // 1. Scale burst: 1.0f -> 1.18f -> 1.0f via shopPurchaseBurstSpringSpec
        launch {
            scaleAnim.animateTo(1.18f, AnimationTokens.shopPurchaseBurstSpringSpec())
            scaleAnim.animateTo(1.0f, AnimationTokens.shopPurchaseBurstSpringSpec())
        }

        // 2. Spring counter roll-up
        launch {
            val startVal = displayedBalance
            val targetVal = balance
            if (targetVal != startVal) {
                val countAnim = Animatable(startVal.toFloat())
                countAnim.animateTo(
                    targetValue = targetVal.toFloat(),
                    animationSpec = tween(
                        durationMillis = AnimationTokens.DurationStateShort,
                        easing = AnimationTokens.StandardEasing
                    )
                ) {
                    displayedBalance = value.roundToInt()
                }
            } else {
                displayedBalance = targetVal
            }
        }

        // 3. Floating "+X" badge pill translation and fade
        launch {
            badgeProgress.snapTo(0f)
            badgeProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(
                    durationMillis = AnimationTokens.DurationRewardShort,
                    easing = AnimationTokens.StandardEasing
                )
            )
        }
    }

    val touchTargetModifier = if (onClick != null) {
        Modifier
            .defaultMinSize(
                minWidth = spacing.minTouchTarget,
                minHeight = spacing.minTouchTarget
            )
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    val accessibilityDescription = if (dailyEarned != null && dailyCap != null) {
        "$displayedBalance coins • Today: $dailyEarned/$dailyCap"
    } else {
        "$displayedBalance coins"
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = shapes.full,
            color = colors.coinGlow,
            tonalElevation = elevation.flat,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
                .onGloballyPositioned { coordinates ->
                    if (!coordinates.isAttached) return@onGloballyPositioned
                    val windowPosition = coordinates.positionInWindow()
                    val centerOffset = Offset(
                        x = windowPosition.x + coordinates.size.width / 2f,
                        y = windowPosition.y + coordinates.size.height / 2f
                    )
                    onPositionMeasured(centerOffset)
                }
                .then(touchTargetModifier)
                .semantics {
                    contentDescription = accessibilityDescription
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing1),
                modifier = Modifier.padding(
                    horizontal = spacing.spacing3,
                    vertical = spacing.spacing1
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_coin),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(spacing.coinIconSize)
                )

                val displayText = if (dailyEarned != null && dailyCap != null) {
                    "$displayedBalance ($dailyEarned/$dailyCap)"
                } else {
                    "$displayedBalance"
                }

                Text(
                    text = displayText,
                    style = typography.data,
                    color = colors.coinGold
                )
            }
        }

        // Floating "+X" badge pill: renders on RenderThread via graphicsLayer
        if (floatingBadgeCount > 0 && badgeProgress.value < 1.0f) {
            Surface(
                shape = shapes.full,
                color = colors.surfaceRaised,
                shadowElevation = elevation.low,
                border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val progress = badgeProgress.value
                        translationY = -spacing.spacing4.toPx() * progress
                        alpha = (1f - progress).coerceIn(0f, 1f)
                    }
            ) {
                Text(
                    text = "+$floatingBadgeCount",
                    style = typography.caption,
                    color = colors.primaryDark,
                    modifier = Modifier.padding(
                        horizontal = spacing.spacing2,
                        vertical = spacing.spacing0_5
                    )
                )
            }
        }
    }
}
