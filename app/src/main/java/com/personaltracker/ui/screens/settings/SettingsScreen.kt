package com.personaltracker.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.domain.model.FocusTimerPolicy
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Settings (AD-2 Figma-Ready Split).
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-verify permission status whenever the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(SettingsUiIntent.CheckPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.ShowSnackbar -> {
                    // Handled via toast or parent scaffold
                }
                is SettingsUiEffect.OpenSystemSetting -> {
                    runCatching {
                        val intent = Intent(effect.action).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            if (effect.action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    SettingsScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Settings, template controls, focus privacy settings,
 * and emergency/telecom whitelist management.
 * Strictly consumes design tokens with zero hardcoded literals and 48dp touch targets (AD-4, AD-7).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    state: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    // Quick exemption preset definitions (Story 3.3)
    val presets = remember {
        listOf(
            "com.google.android.apps.authenticator2" to "Authenticator",
            "com.bitwarden.bitwarden" to "Bitwarden",
            "com.google.android.keep" to "Google Keep",
            "com.google.android.calculator" to "Calculator"
        )
    }

    // Custom exemption dialog
    if (state.isWhitelistSheetOpen) {
        var customPackageName by remember { mutableStateOf("") }
        var customDisplayName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onIntent(SettingsUiIntent.ToggleWhitelistSheet(false)) },
            shape = shapes.lg,
            containerColor = colors.surfaceRaised,
            title = {
                Text(
                    text = "Add Whitelist Exemption",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                ) {
                    Text(
                        text = "Whitelisted apps can be opened during focus sessions without triggering an intervention.",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        label = { Text("Package Name (e.g. com.example.app)", style = typography.caption) },
                        singleLine = true,
                        shape = shapes.sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customDisplayName,
                        onValueChange = { customDisplayName = it },
                        label = { Text("Display Name (optional)", style = typography.caption) },
                        singleLine = true,
                        shape = shapes.sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onIntent(
                            SettingsUiIntent.AddCustomWhitelistPackage(
                                packageName = customPackageName,
                                appDisplayName = customDisplayName
                            )
                        )
                    },
                    shape = shapes.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceBase
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text("Add Exemption", style = typography.label)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onIntent(SettingsUiIntent.ToggleWhitelistSheet(false)) },
                    shape = shapes.md,
                    modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text("Cancel", style = typography.label)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = spacing.screenPaddingHorizontal,
                vertical = spacing.screenPaddingVertical
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Active Schedule Template Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.lg,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Daily Schedule Template",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                val activeName = state.activeDayType?.name ?: "Default Routine"
                Text(
                    text = "Active for Today: $activeName",
                    style = typography.title,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = if (state.dateOverride != null) {
                        "Manual override applied for today • Won't alter tomorrow"
                    } else {
                        "Resolved automatically based on day of the week"
                    },
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing4))

                Text(
                    text = "Swap Today's Template:",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing2))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.allDayTypes.forEach { dayType ->
                        val isSelected = state.activeDayType?.id == dayType.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onIntent(SettingsUiIntent.SelectDayTypeOverride(dayType.id))
                            },
                            label = { Text(text = dayType.name, style = typography.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.surfaceBase,
                                containerColor = colors.surfaceSunken,
                                labelColor = colors.inkPrimary
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                        )
                    }
                }

                if (state.dateOverride != null) {
                    Spacer(modifier = Modifier.height(spacing.spacing3))
                    OutlinedButton(
                        onClick = { onIntent(SettingsUiIntent.ClearDayTypeOverride) },
                        shape = shapes.md,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(text = "Reset to Default Schedule", style = typography.label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // 2. Focus Protection & Privacy Card (Story 3.2, AD-8, NFR-4)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.lg,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Focus Protection & Privacy",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))

                // Zero-keylogging reassurance banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.sm,
                    color = colors.surfaceSunken
                ) {
                    Text(
                        text = "Event-driven window detection only. Zero keystroke logging, zero screen scraping (canRetrieveWindowContent = false).",
                        style = typography.caption,
                        color = colors.inkSecondary,
                        modifier = Modifier.padding(spacing.spacing2)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Accessibility Service Toggle / Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distraction Interception",
                            style = typography.title,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Detects distracting apps during active focus",
                            style = typography.caption,
                            color = colors.inkSecondary
                        )
                    }
                    Text(
                        text = if (state.isAccessibilityGranted) "Active" else "Disabled",
                        style = typography.label,
                        color = if (state.isAccessibilityGranted) colors.success else colors.inkTertiary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                OutlinedButton(
                    onClick = { onIntent(SettingsUiIntent.OpenAccessibilitySettings) },
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = if (state.isAccessibilityGranted) "Configure Accessibility Service" else "Enable Interception Service",
                        style = typography.label
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Window Overlay Permission Toggle / Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Intervention Window Overlay",
                            style = typography.title,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Displays gentle reminder over opened distraction apps",
                            style = typography.caption,
                            color = colors.inkSecondary
                        )
                    }
                    Text(
                        text = if (state.isOverlayGranted) "Granted" else "Needed",
                        style = typography.label,
                        color = if (state.isOverlayGranted) colors.success else colors.warning
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing2))

                OutlinedButton(
                    onClick = { onIntent(SettingsUiIntent.OpenOverlaySettings) },
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(
                        text = if (state.isOverlayGranted) "Configure Overlay Permission" else "Grant Overlay Permission",
                        style = typography.label
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // 3. Emergency Telecom & Whitelist Manager Card (Story 3.3, FR-4.6, FR-10.1)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.lg,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Emergency Telecom & App Whitelist",
                    style = typography.headline,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))

                // Immune components reassurance banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapes.sm,
                    color = colors.surfaceSunken
                ) {
                    Text(
                        text = "Phone dialers, emergency calls (911, 112, SOS), and system UI are always immune to interception.",
                        style = typography.caption,
                        color = colors.inkSecondary,
                        modifier = Modifier.padding(spacing.spacing2)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Call Progression Policy Selector
                Text(
                    text = "Timer Behavior During Phone Calls:",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing2))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FocusTimerPolicy.entries.forEach { policy ->
                        val isSelected = state.focusTimerPolicy == policy
                        FilterChip(
                            selected = isSelected,
                            onClick = { onIntent(SettingsUiIntent.SetFocusTimerPolicy(policy)) },
                            label = { Text(text = policy.displayName, style = typography.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.surfaceBase,
                                containerColor = colors.surfaceSunken,
                                labelColor = colors.inkPrimary
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Quick Exemption Presets
                Text(
                    text = "Quick Exemption Presets:",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing2))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { (pkg, name) ->
                        val isWhitelisted = state.whitelistedRules.any { it.packageName.equals(pkg, ignoreCase = true) }
                        FilterChip(
                            selected = isWhitelisted,
                            onClick = { onIntent(SettingsUiIntent.ToggleAppWhitelist(pkg, name)) },
                            label = { Text(text = if (isWhitelisted) "$name ✓" else "+ $name", style = typography.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.companion,
                                selectedLabelColor = colors.surfaceBase,
                                containerColor = colors.surfaceSunken,
                                labelColor = colors.inkPrimary
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                // Whitelisted Rules List
                Text(
                    text = "Active Exemption Rules (${state.whitelistedRules.size}):",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing2))

                if (state.whitelistedRules.isEmpty()) {
                    Text(
                        text = "No custom exempt apps configured. System apps remain exempt.",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.spacing2)
                    ) {
                        state.whitelistedRules.forEach { rule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceSunken, shapes.sm)
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
                                OutlinedButton(
                                    onClick = { onIntent(SettingsUiIntent.RemoveWhitelistPackage(rule.packageName)) },
                                    shape = shapes.sm,
                                    modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
                                ) {
                                    Text("Remove", style = typography.caption)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                Button(
                    onClick = { onIntent(SettingsUiIntent.ToggleWhitelistSheet(true)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.surfaceBase
                    ),
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Add Custom App Exemption", style = typography.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // 4. Local-First Encryption & Data Card (AD-5)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.lg,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Local-First Encryption (AD-5)",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "• SQLCipher 4.6.1 database encryption at rest (PBKDF2-HMAC-SHA512)\n" +
                            "• Keystore AES-256-GCM hardware key isolation\n" +
                            "• Zero remote telemetry, zero analytics SDKs\n" +
                            "• 100% offline-first productivity",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing4))

                Button(
                    onClick = { onIntent(SettingsUiIntent.ExportEncryptedBackup) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondary,
                        contentColor = colors.surfaceBase
                    ),
                    shape = shapes.md,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = spacing.minTouchTarget)
                ) {
                    Text(text = "Export Encrypted Backup", style = typography.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
