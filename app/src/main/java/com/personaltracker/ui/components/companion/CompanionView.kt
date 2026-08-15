package com.personaltracker.ui.components.companion

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * The 7 Companion Expression States.
 * Sourced 1:1 from `design-system.md` Illustration & Companion/Mascot Style section.
 *
 * TECHNICAL CONTRACT FOR RIVE ARTIST:
 * - Artboard: "companion"
 * - State Machine: "companion_sm"
 * - Input: "state" (Number type, values 0-6 matching ordinals below)
 */
enum class CompanionState(val riveValue: Int, val stateName: String) {
    Idle(0, "Idle / Content"),
    Celebrating(1, "Celebrating"),
    Encouraging(2, "Encouraging"),
    Concerned(3, "Concerned"),
    Focused(4, "Focused"),
    Sleeping(5, "Sleeping / Resting"),
    Excited(6, "Excited");

    companion object {
        fun fromRiveValue(value: Int): CompanionState =
            entries.firstOrNull { it.riveValue == value } ?: Idle
    }
}

/**
 * Companion View Composable.
 *
 * Renders the stateful companion character powered by Rive runtime.
 * Automatically drives the "state" Number input on the "companion_sm" StateMachine.
 *
 * PLACEHOLDER NOTE:
 * Uses fallback placeholder rendering or `companion_placeholder.riv` until the
 * artist-delivered .riv asset is dropped into `res/raw/companion.riv`.
 * No code changes will be required when the production asset lands.
 */
@Composable
fun CompanionView(
    state: CompanionState,
    modifier: Modifier = Modifier,
    @RawRes rivResId: Int? = null,
    artboardName: String = "companion",
    stateMachineName: String = "companion_sm",
    inputName: String = "state"
) {
    val context = LocalContext.current
    var riveViewRef by remember { mutableStateOf<RiveAnimationView?>(null) }

    // When the state changes, update the Rive state machine input
    LaunchedEffect(state, riveViewRef) {
        riveViewRef?.let { view ->
            try {
                view.setNumberState(stateMachineName, inputName, state.riveValue.toFloat())
            } catch (e: Exception) {
                // Catches if state machine / input is not yet loaded in placeholder
            }
        }
    }

    if (rivResId != null) {
        AndroidView(
            modifier = modifier,
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
    } else {
        // Fallback / Stand-in Visualizer when .riv is pending
        PlaceholderCompanionView(
            state = state,
            modifier = modifier
        )
    }
}

/**
 * Clean placeholder visualizer for testing companion state switching in Compose previews and debug builds.
 */
@Composable
fun PlaceholderCompanionView(
    state: CompanionState,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography

    val stateColor = when (state) {
        CompanionState.Idle -> colors.companion
        CompanionState.Celebrating -> colors.accentWarm
        CompanionState.Encouraging -> colors.companionLight
        CompanionState.Concerned -> colors.warning
        CompanionState.Focused -> colors.primary
        CompanionState.Sleeping -> colors.inkTertiary
        CompanionState.Excited -> colors.accentWarmLight
    }

    val stateEmoji = when (state) {
        CompanionState.Idle -> "🌿"
        CompanionState.Celebrating -> "🎉"
        CompanionState.Encouraging -> "✨"
        CompanionState.Concerned -> "🧐"
        CompanionState.Focused -> "🧘"
        CompanionState.Sleeping -> "💤"
        CompanionState.Excited -> "⚡"
    }

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(stateColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(stateColor.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stateEmoji,
                style = typography.display
            )
        }
    }
}
