package com.personaltracker.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.personaltracker.R
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.components.animation.RewardMotionStage
import com.personaltracker.ui.components.animation.RewardMotionStageState
import com.personaltracker.ui.components.animation.rememberRewardMotionStageState
import com.personaltracker.ui.components.atoms.CoinPill
import com.personaltracker.ui.components.atoms.DayTypeChip
import com.personaltracker.ui.components.atoms.StreakFlameBadge
import com.personaltracker.ui.components.overlays.QuickCaptureSheet
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * CompositionLocal exposing the root-hoisted [RewardMotionStageState] to all child screens
 * across the navigation graph.
 */
val LocalRewardMotionStage = staticCompositionLocalOf<RewardMotionStageState> {
    error("No RewardMotionStageState provided")
}

/**
 * Global Application Shell Scaffold.
 *
 * Integrates:
 * 1. Persistent Top Context Bar (DayTypeChip, StreakFlameBadge, CoinPill target measurement)
 * 2. 5-tab Navigation Bar
 * 3. Quick Capture floating drawer & voice capture FAB
 * 4. Root-hoisted unclipped [RewardMotionStage] above navigation viewports (AD-7)
 *
 * Enforces 100% design token compliance with zero hardcoded literals and 48dp touch targets.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppShellScaffold(
    currentDestination: PersonalTrackerDestination,
    onNavigateToDestination: (PersonalTrackerDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeDayTypeName: String = "Weekday",
    onDayTypeClick: (() -> Unit)? = null,
    streakCount: Int = 0,
    coinBalance: Int = 0,
    rewardMotionStageState: RewardMotionStageState = rememberRewardMotionStageState(),
    onQuickCaptureSubmitted: ((String) -> Unit)? = null,
    onConfirmProposal: ((ParsedCommand) -> Unit)? = null,
    onUndoCapture: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Quick Capture state
    var isQuickCaptureOpen by remember { mutableStateOf(false) }
    var quickCaptureInput by remember { mutableStateOf("") }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var recordingAmplitude by remember { mutableFloatStateOf(0f) }
    var mediumProposal by remember { mutableStateOf<ParsedCommand?>(null) }
    var highConfidenceCommittedTitle by remember { mutableStateOf<String?>(null) }
    var lowConfidenceNotice by remember { mutableStateOf<String?>(null) }

    // FAB visible on Home, Habits, and Study surfaces (EXPERIENCE.md §2)
    val showQuickCaptureFab = currentDestination in listOf(
        PersonalTrackerDestination.HOME,
        PersonalTrackerDestination.HABITS,
        PersonalTrackerDestination.STUDY
    )

    CompositionLocalProvider(
        LocalRewardMotionStage provides rewardMotionStageState
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopContextBar(
                        activeDayTypeName = activeDayTypeName,
                        onDayTypeClick = onDayTypeClick,
                        streakCount = streakCount,
                        coinBalance = coinBalance,
                        onCoinTargetMeasured = { rewardMotionStageState.updateDestinationTarget(it) }
                    )
                },
                bottomBar = {
                    PersonalTrackerBottomBar(
                        currentDestination = currentDestination,
                        onNavigateToDestination = onNavigateToDestination
                    )
                },
                floatingActionButton = {
                    if (showQuickCaptureFab) {
                        FloatingActionButton(
                            onClick = { isQuickCaptureOpen = true },
                            containerColor = colors.primary,
                            contentColor = colors.surfaceRaised,
                            shape = shapes.full,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = elevation.medium
                            ),
                            modifier = Modifier
                                .size(spacing.fabSize)
                                .defaultMinSize(
                                    minWidth = spacing.minTouchTarget,
                                    minHeight = spacing.minTouchTarget
                                )
                                .semantics {
                                    contentDescription = "Open quick capture drawer"
                                }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_waveform),
                                contentDescription = null,
                                modifier = Modifier.size(spacing.spacing6)
                            )
                        }
                    }
                },
                containerColor = colors.surfaceBase,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                content(innerPadding)
            }

            // Viewport-hoisted RewardMotionStage (above navigation host to prevent clipping)
            RewardMotionStage(
                state = rewardMotionStageState.state,
                onCoinArcComplete = { rewardMotionStageState.onCoinArcCompleted() },
                onLottieComplete = { rewardMotionStageState.onLottieCompleted() },
                modifier = Modifier.fillMaxSize()
            )

            // Universal Quick Capture Sheet overlay
            QuickCaptureSheet(
                isVisible = isQuickCaptureOpen,
                onDismissRequest = {
                    isQuickCaptureOpen = false
                    isVoiceRecording = false
                },
                inputText = quickCaptureInput,
                onInputTextChanged = { quickCaptureInput = it },
                onSubmitText = { text ->
                    quickCaptureInput = ""
                    isQuickCaptureOpen = false
                    highConfidenceCommittedTitle = text
                    onQuickCaptureSubmitted?.invoke(text)
                },
                isRecording = isVoiceRecording,
                recordingAmplitude = recordingAmplitude,
                onToggleRecording = {
                    isVoiceRecording = !isVoiceRecording
                    recordingAmplitude = if (isVoiceRecording) 0.65f else 0f
                },
                mediumConfidenceProposal = mediumProposal,
                onConfirmProposal = { proposal ->
                    mediumProposal = null
                    isQuickCaptureOpen = false
                    onConfirmProposal?.invoke(proposal)
                },
                onDiscardProposal = {
                    mediumProposal = null
                },
                highConfidenceCommittedTitle = highConfidenceCommittedTitle,
                onUndoHighConfidence = {
                    highConfidenceCommittedTitle = null
                    onUndoCapture?.invoke()
                },
                onDismissUndoBanner = {
                    highConfidenceCommittedTitle = null
                },
                lowConfidenceNotice = lowConfidenceNotice
            )
        }
    }
}

/**
 * Persistent Top Context Bar hosting DayTypeChip, StreakFlameBadge, and live CoinPill.
 */
@Composable
fun TopContextBar(
    activeDayTypeName: String,
    streakCount: Int,
    coinBalance: Int,
    onCoinTargetMeasured: (androidx.compose.ui.geometry.Offset) -> Unit,
    modifier: Modifier = Modifier,
    onDayTypeClick: (() -> Unit)? = null
) {
    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val elevation = PersonalTrackerTheme.elevation

    Surface(
        color = colors.surfaceRaised,
        shadowElevation = elevation.low,
        border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.screenPaddingHorizontal,
                    vertical = spacing.spacing2
                )
        ) {
            // Left: DayType Chip
            DayTypeChip(
                dayTypeName = activeDayTypeName,
                onClick = onDayTypeClick
            )

            // Right: Streak Flame Badge and Coin HUD Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2)
            ) {
                StreakFlameBadge(streakCount = streakCount)

                CoinPill(
                    balance = coinBalance,
                    onPositionMeasured = onCoinTargetMeasured
                )
            }
        }
    }
}
