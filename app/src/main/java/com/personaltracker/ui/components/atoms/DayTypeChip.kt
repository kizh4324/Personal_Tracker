package com.personaltracker.ui.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.personaltracker.domain.model.DayType
import com.personaltracker.ui.theme.PersonalTrackerColors
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Resolves the semantic color coding for a given DayType template name or ID.
 */
fun resolveDayTypeColor(nameOrId: String, colors: PersonalTrackerColors): Color {
    val key = nameOrId.uppercase()
    return when {
        key.contains("WEEKEND") -> colors.daytypeWeekend
        key.contains("COLLEGE") -> colors.daytypeCollege
        key.contains("SPECIAL") -> colors.daytypeSpecial
        else -> colors.daytypeWeekday
    }
}

/**
 * Atomic DayType Chip component.
 *
 * Displays the active DayType schedule template with semantic color coding and
 * strictly enforces the 48dp minimum touch target floor.
 * Sourced 1:1 from `DESIGN.md` DayType and Chip specifications.
 */
@Composable
fun DayTypeChip(
    dayTypeName: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    val accentColor = color ?: resolveDayTypeColor(dayTypeName, colors)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(
                minWidth = spacing.minTouchTarget,
                minHeight = spacing.minTouchTarget
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = "Active DayType schedule: $dayTypeName"
            }
    ) {
        Surface(
            shape = shapes.full,
            color = accentColor.copy(alpha = 0.12f),
            border = BorderStroke(spacing.spacing0_5, accentColor),
            tonalElevation = PersonalTrackerTheme.elevation.flat,
            modifier = Modifier.defaultMinSize(minHeight = spacing.chipHeight)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                modifier = Modifier.padding(
                    horizontal = spacing.spacing3,
                    vertical = spacing.spacing1
                )
            ) {
                // Color indicator dot
                Box(
                    modifier = Modifier
                        .size(spacing.spacing2)
                        .background(color = accentColor, shape = shapes.full)
                )

                Text(
                    text = dayTypeName,
                    style = typography.label,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Domain model overload for [DayTypeChip].
 */
@Composable
fun DayTypeChip(
    dayType: DayType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    DayTypeChip(
        dayTypeName = dayType.name,
        modifier = modifier,
        onClick = onClick
    )
}
