package com.personaltracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Dedicated stateless Warm Organic Tactile modal bottom sheet for managing app whitelist exemptions
 * and distraction interception rules (AD-1, AD-2, AD-4, AD-7, FR-4.6, FR-10.1).
 *
 * Visual Invariants:
 * - Surface: Warm Cream via [PersonalTrackerColors.surfaceBase]
 * - Active Switches & Primary Accents: Vibrant Orange via [PersonalTrackerColors.primary]
 * - Espresso Headings & Dark Accents: Deep Espresso via [PersonalTrackerColors.primaryDark]
 * - Muted Tracks & Containers: Sand Hatched Track via [PersonalTrackerColors.surfaceSunken]
 * - 0 raw hex color literals, 0 raw dp/sp dimension literals.
 * - Minimum 48dp interactive touch target floor ([PersonalTrackerSpacing.minTouchTarget]).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WhitelistManagerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    whitelistedRules: List<InterventionRule>,
    blockedRules: List<InterventionRule>,
    onToggleRule: (packageName: String, appDisplayName: String?) -> Unit,
    onRemoveRule: (packageName: String) -> Unit,
    onAddCustomExemption: (packageName: String, appDisplayName: String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isOpen) return

    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Allowed, 1 = Blocked
    var isAddingCustomApp by remember { mutableStateOf(false) }
    var customPackageName by remember { mutableStateOf("") }
    var customDisplayName by remember { mutableStateOf("") }

    val presets = remember {
        listOf(
            "com.google.android.apps.authenticator2" to "Authenticator",
            "com.bitwarden.bitwarden" to "Bitwarden",
            "com.google.android.keep" to "Google Keep",
            "com.google.android.calculator" to "Calculator"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceBase,
        shape = shapes.xxxl,
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.screenPaddingHorizontal,
                    vertical = spacing.spacing2
                )
        ) {
            // Header Row with Title and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Rules & Whitelist",
                        style = typography.headline,
                        color = colors.primaryDark
                    )
                    Text(
                        text = "Manage tools allowed during focus sessions",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(
                        minWidth = spacing.minTouchTarget,
                        minHeight = spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close sheet",
                        tint = colors.primaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Search Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search apps or package names...",
                        style = typography.bodySecondary,
                        color = colors.inkTertiary
                    )
                },
                singleLine = true,
                shape = shapes.md,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceRaised,
                    unfocusedContainerColor = colors.surfaceRaised,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderSubtle,
                    cursorColor = colors.primary,
                    focusedTextColor = colors.inkPrimary,
                    unfocusedTextColor = colors.inkPrimary
                ),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.defaultMinSize(
                                minWidth = spacing.minTouchTarget,
                                minHeight = spacing.minTouchTarget
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = colors.inkSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.minTouchTarget)
            )

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Tactile Capsule Tab Selector (Allowed vs Blocked)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSunken, shapes.capsule)
                    .padding(spacing.spacing1),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
            ) {
                // Allowed Tab
                val isAllowedActive = selectedTabIndex == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = spacing.buttonHeight)
                        .background(
                            color = if (isAllowedActive) colors.primaryDark else colors.surfaceSunken,
                            shape = shapes.capsule
                        )
                        .clickable(role = Role.Tab) { selectedTabIndex = 0 }
                        .padding(vertical = spacing.spacing2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Allowed (${whitelistedRules.size})",
                        style = typography.ctaText,
                        color = if (isAllowedActive) colors.surfaceRaised else colors.inkSecondary
                    )
                }

                // Blocked Tab
                val isBlockedActive = selectedTabIndex == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = spacing.buttonHeight)
                        .background(
                            color = if (isBlockedActive) colors.primaryDark else colors.surfaceSunken,
                            shape = shapes.capsule
                        )
                        .clickable(role = Role.Tab) { selectedTabIndex = 1 }
                        .padding(vertical = spacing.spacing2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Blocked (${blockedRules.size})",
                        style = typography.ctaText,
                        color = if (isBlockedActive) colors.surfaceRaised else colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Quick Exemption Presets Strip
            Text(
                text = "Quick Presets:",
                style = typography.label,
                color = colors.primaryDark
            )
            Spacer(modifier = Modifier.height(spacing.spacing1))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEach { (pkg, name) ->
                    val isWhitelisted = whitelistedRules.any { it.packageName.equals(pkg, ignoreCase = true) }
                    FilterChip(
                        selected = isWhitelisted,
                        onClick = { onToggleRule(pkg, name) },
                        label = {
                            Text(
                                text = if (isWhitelisted) "$name ✓" else "+ $name",
                                style = typography.caption
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.surfaceRaised,
                            containerColor = colors.surfaceRaised,
                            labelColor = colors.primaryDark
                        ),
                        shape = shapes.capsule,
                        modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.spacing3))

            // Custom App Exemption Form Accordion
            if (isAddingCustomApp) {
                Surface(
                    shape = shapes.lg,
                    color = colors.surfaceRaised,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.spacing3),
                        verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        Text(
                            text = "Add Custom App Exemption",
                            style = typography.title,
                            color = colors.primaryDark
                        )
                        Text(
                            text = "Configure custom application packages to open unimpeded during active focus sessions.",
                            style = typography.caption,
                            color = colors.inkSecondary
                        )
                        OutlinedTextField(
                            value = customPackageName,
                            onValueChange = { customPackageName = it },
                            label = { Text("Package name (e.g. com.example.app)", style = typography.caption) },
                            singleLine = true,
                            shape = shapes.sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                cursorColor = colors.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customDisplayName,
                            onValueChange = { customDisplayName = it },
                            label = { Text("Display name (optional)", style = typography.caption) },
                            singleLine = true,
                            shape = shapes.sm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                cursorColor = colors.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isAddingCustomApp = false
                                    customPackageName = ""
                                    customDisplayName = ""
                                },
                                shape = shapes.md,
                                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                            ) {
                                Text("Cancel", style = typography.label)
                            }
                            Spacer(modifier = Modifier.padding(spacing.spacing1))
                            Button(
                                onClick = {
                                    if (customPackageName.isNotBlank()) {
                                        onAddCustomExemption(customPackageName, customDisplayName)
                                        isAddingCustomApp = false
                                        customPackageName = ""
                                        customDisplayName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = colors.surfaceRaised
                                ),
                                shape = shapes.md,
                                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                            ) {
                                Text("Add Exemption", style = typography.label)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacing.spacing3))
            } else {
                OutlinedButton(
                    onClick = { isAddingCustomApp = true },
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.primaryDark,
                        modifier = Modifier.padding(end = spacing.spacing1)
                    )
                    Text(
                        text = "Add Custom App Exemption",
                        style = typography.label,
                        color = colors.primaryDark
                    )
                }
                Spacer(modifier = Modifier.height(spacing.spacing3))
            }

            // Rules List
            val currentRules = if (selectedTabIndex == 0) whitelistedRules else blockedRules

            if (currentRules.isEmpty()) {
                Surface(
                    shape = shapes.md,
                    color = colors.surfaceRaised,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.spacing4)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.spacing4),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedTabIndex == 0) {
                                if (searchQuery.isNotBlank()) "No allowed apps match \"$searchQuery\"." else "No custom allowed apps configured. Phone and system tools remain accessible."
                            } else {
                                if (searchQuery.isNotBlank()) "No blocked apps match \"$searchQuery\"." else "No blocked apps configured."
                            },
                            style = typography.bodySecondary,
                            color = colors.inkSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                ) {
                    items(currentRules, key = { it.packageName }) { rule ->
                        Surface(
                            shape = shapes.md,
                            color = colors.surfaceRaised,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = spacing.spacing3,
                                        vertical = spacing.spacing2
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.appDisplayName,
                                        style = typography.title,
                                        color = colors.inkPrimary
                                    )
                                    Text(
                                        text = rule.packageName,
                                        style = typography.caption,
                                        color = colors.inkSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing1)
                                ) {
                                    // Vibrant Orange switch for active whitelisted state
                                    Switch(
                                        checked = !rule.isStrictBlocked,
                                        onCheckedChange = { onToggleRule(rule.packageName, rule.appDisplayName) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colors.surfaceRaised,
                                            checkedTrackColor = colors.primary,
                                            uncheckedThumbColor = colors.inkTertiary,
                                            uncheckedTrackColor = colors.surfaceSunken
                                        ),
                                        modifier = Modifier.defaultMinSize(
                                            minWidth = spacing.minTouchTarget,
                                            minHeight = spacing.minTouchTarget
                                        )
                                    )

                                    if (!rule.isStrictBlocked) {
                                        IconButton(
                                            onClick = { onRemoveRule(rule.packageName) },
                                            modifier = Modifier.defaultMinSize(
                                                minWidth = spacing.minTouchTarget,
                                                minHeight = spacing.minTouchTarget
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove exemption",
                                                tint = colors.inkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * State-bound overload for [WhitelistManagerSheet] connecting directly to [SettingsUiState] and [SettingsUiIntent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistManagerSheet(
    state: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    WhitelistManagerSheet(
        isOpen = state.isWhitelistSheetOpen,
        onDismiss = { onIntent(SettingsUiIntent.ToggleWhitelistSheet(false)) },
        searchQuery = state.searchQuery,
        onSearchQueryChange = { onIntent(SettingsUiIntent.SearchWhitelistRules(it)) },
        whitelistedRules = state.whitelistedRules,
        blockedRules = state.blockedRules,
        onToggleRule = { pkg, name -> onIntent(SettingsUiIntent.TogglePackageInterceptionRule(pkg, name)) },
        onRemoveRule = { pkg -> onIntent(SettingsUiIntent.RemoveWhitelistPackage(pkg)) },
        onAddCustomExemption = { pkg, name -> onIntent(SettingsUiIntent.AddCustomWhitelistPackage(pkg, name)) },
        modifier = modifier
    )
}
