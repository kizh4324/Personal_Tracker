package com.personaltracker.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.R
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Atomic Coin HUD Pill component.
 *
 * Displays the current coin balance using Inter data typography and `ic_coin`.
 * Measures its window-level coordinates via [onGloballyPositioned] to serve as the
 * destination target for [com.personaltracker.ui.components.animation.CoinArcAnimation].
 *
 * Sourced 1:1 from `DESIGN.md` coin-hud specifications.
 */
@Composable
fun CoinPill(
    balance: Int,
    modifier: Modifier = Modifier,
    onPositionMeasured: (Offset) -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

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

    Surface(
        shape = shapes.full,
        color = colors.coinGlow,
        tonalElevation = PersonalTrackerTheme.elevation.flat,
        modifier = modifier
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
                contentDescription = "Coin balance: $balance coins"
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

            Text(
                text = "$balance",
                style = typography.data,
                color = colors.coinGold
            )
        }
    }
}
