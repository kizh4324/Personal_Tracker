package com.personaltracker.ui.components.companion

import androidx.annotation.RawRes
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import com.personaltracker.domain.engine.CompanionState
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Companion View Composable.
 *
 * Renders the stateful companion character powered by Rive runtime or a tactile spring-animated fallback.
 * Automatically drives the "state" Number input on the "companion_sm" StateMachine.
 * Sourced 1:1 from `docs/design/companion-asset-brief.md` and Story 4.4 specification (AD-1, AD-4, AD-7).
 */
@Composable
fun CompanionView(
    state: CompanionState,
    modifier: Modifier = Modifier,
    activeAccessoryId: String? = null,
    @RawRes rivResId: Int? = null,
    artboardName: String = "companion",
    stateMachineName: String = "companion_sm",
    inputName: String = "state"
) {
    var riveViewRef by remember { mutableStateOf<RiveAnimationView?>(null) }
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    // When the state changes, update the Rive state machine input
    LaunchedEffect(state, riveViewRef) {
        riveViewRef?.let { view ->
            try {
                view.setNumberState(stateMachineName, inputName, state.riveValue.toFloat())
            } catch (_: Exception) {
                // Catches if state machine / input is not yet loaded in placeholder
            }
        }
    }

    if (rivResId != null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        setRiveResource(
                            resId = rivResId,
                            artboardName = artboardName,
                            stateMachineName = stateMachineName,
                            fit = Fit.CONTAIN,
                            loop = Loop.LOOP,
                            autoplay = true
                        )
                        riveViewRef = this
                    }
                },
                update = { view ->
                    riveViewRef = view
                    try {
                        view.setNumberState(stateMachineName, inputName, state.riveValue.toFloat())
                    } catch (_: Exception) {}
                }
            )

            // Accessory badge overlay
            val accessoryEmoji = when (activeAccessoryId) {
                "item_scarf" -> "🧣"
                "item_cap" -> "🧢"
                "item_crown" -> "👑"
                "item_coffee" -> "☕"
                else -> null
            }
            if (accessoryEmoji != null) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceRaised,
                    border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(spacing.spacing8)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = accessoryEmoji,
                            style = typography.title
                        )
                    }
                }
            }
        }
    } else {
        // Fallback / Stand-in Visualizer with spring breathing animation
        PlaceholderCompanionView(
            state = state,
            activeAccessoryId = activeAccessoryId,
            modifier = modifier
        )
    }
}

/**
 * Clean tactile placeholder visualizer with spring breathing physics and accessory badges.
 * Conforms 100% to AD-7 token conformance and AD-4 non-punishing emotional states.
 */
@Composable
fun PlaceholderCompanionView(
    state: CompanionState,
    activeAccessoryId: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing

    val infiniteTransition = rememberInfiniteTransition(label = "CompanionBreathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    val stateColor = when (state) {
        CompanionState.IDLE_COZY -> colors.companion
        CompanionState.CELEBRATING_REWARD -> colors.accentWarm
        CompanionState.ENCOURAGING_WARM -> colors.companionLight
        CompanionState.CONCERNED_CHECKIN -> colors.warning
        CompanionState.FOCUSING_STUDY -> colors.primary
        CompanionState.RESTING_PEACEFUL -> colors.inkTertiary
        CompanionState.HAPPY_BOUNCE -> colors.accentWarmLight
    }

    val accessoryEmoji = when (activeAccessoryId) {
        "item_scarf" -> "🧣"
        "item_cap" -> "🧢"
        "item_crown" -> "👑"
        "item_coffee" -> "☕"
        else -> null
    }

    Box(
        modifier = modifier
            .size(spacing.spacing16 + spacing.spacing12)
            .clip(CircleShape)
            .background(stateColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(spacing.spacing16 + spacing.spacing8)
                .graphicsLayer {
                    scaleX = breathingScale
                    scaleY = breathingScale
                }
                .clip(CircleShape)
                .background(stateColor.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.emoji,
                style = typography.display
            )
        }

        if (accessoryEmoji != null) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceRaised,
                border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(spacing.spacing8)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = accessoryEmoji,
                        style = typography.title
                    )
                }
            }
        }
    }
}
