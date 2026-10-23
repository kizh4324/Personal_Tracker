package com.personaltracker.ui.components.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.personaltracker.domain.model.DayType
import com.personaltracker.ui.components.atoms.resolveDayTypeColor
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * 100% Stateless Dialog enabling 1-tap DayType template selection and date override management.
 *
 * Enforces zero hardcoded colors, dp, or sp literals, guarantees 48dp minimum touch target floor,
 * and presents zero-guilt microcopy (AD-2, AD-4, AD-7, EXP-008, EXP-016).
 */
@Composable
fun DayTypeSelectorDialog(
    isOpen: Boolean,
    availableDayTypes: List<DayType>,
    activeDayTypeId: String?,
    isOverrideActive: Boolean,
    onSelectDayType: (String) -> Unit,
    onResetOverride: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val typography = PersonalTrackerTheme.typography
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.spacing4)
                .semantics { contentDescription = "DayType template selector" },
            shape = shapes.lg,
            color = colors.surfaceRaised,
            tonalElevation = elevation.medium,
            shadowElevation = elevation.medium,
            border = BorderStroke(spacing.spacing0_5, colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Text(
                    text = "Today's Schedule Template",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Choose your focus pace for today. Routine durations scale non-destructively.",
                    style = typography.caption,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // List of DayTypes
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                ) {
                    items(
                        items = availableDayTypes,
                        key = { it.id }
                    ) { dayType ->
                        val isSelected = dayType.id == activeDayTypeId
                        val itemColor = resolveDayTypeColor(dayType.name, colors)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = spacing.minTouchTarget)
                                .clip(shapes.md)
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { onSelectDayType(dayType.id) }
                                )
                                .semantics {
                                    contentDescription = "${dayType.name}, ${dayType.targetStudyMinutes} minutes study target"
                                },
                            shape = shapes.md,
                            color = if (isSelected) colors.primarySubtle else colors.surfaceBase,
                            border = BorderStroke(
                                width = if (isSelected) spacing.spacing1 else spacing.spacing0_5,
                                color = if (isSelected) colors.primary else colors.borderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.spacing3, vertical = spacing.spacing2),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Template color dot
                                Box(
                                    modifier = Modifier
                                        .size(spacing.spacing3)
                                        .clip(shapes.full)
                                        .background(itemColor)
                                )

                                Spacer(modifier = Modifier.width(spacing.spacing3))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dayType.name,
                                            style = typography.label,
                                            color = colors.inkPrimary
                                        )
                                        if (dayType.isDefault) {
                                            Spacer(modifier = Modifier.width(spacing.spacing2))
                                            Text(
                                                text = "Default",
                                                style = typography.caption,
                                                color = colors.inkTertiary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${dayType.targetStudyMinutes}m daily focus target",
                                        style = typography.caption,
                                        color = colors.inkSecondary
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active template",
                                        tint = colors.primary,
                                        modifier = Modifier.size(spacing.spacing4)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Optional Reset button if an override is currently active
                if (isOverrideActive) {
                    TextButton(
                        onClick = onResetOverride,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(
                            text = "Reset to Default Schedule",
                            style = typography.label,
                            color = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.spacing2))
                }

                // Close / Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = "Dismiss",
                        style = typography.label,
                        color = colors.inkSecondary
                    )
                }
            }
        }
    }
}
