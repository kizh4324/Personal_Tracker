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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.R
import com.personaltracker.domain.parser.ParsedCommand
import com.personaltracker.ui.components.animation.RewardMotionStage
import com.personaltracker.ui.components.animation.RewardMotionStageState
import com.personaltracker.ui.components.animation.rememberRewardMotionStageState
import com.personaltracker.ui.components.atoms.CoinPill
import com.personaltracker.ui.components.atoms.DayTypeChip
import com.personaltracker.ui.components.atoms.StreakFlameBadge
import com.personaltracker.ui.components.overlays.QuickCaptureSheet
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiEffect
import com.personaltracker.ui.screens.quickcapture.QuickCaptureUiIntent
import com.personaltracker.ui.screens.quickcapture.QuickCaptureViewModel
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
    quickCaptureViewModel: QuickCaptureViewModel? = null,
    onQuickCaptureSubmitted: ((String) -> Unit)? = null,
    onConfirmProposal: ((ParsedCommand) -> Unit)? = null,
    onUndoCapture: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = PersonalTrackerTheme.colors
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes
    val elevation = PersonalTrackerTheme.elevation

    // Optional QuickCapture ViewModel state collection
    val qcState = quickCaptureViewModel?.uiState?.collectAsStateWithLifecycle()?.value

    // Fallback ephemeral state for Previews and tests when ViewModel is omitted
    var localIsOpen by remember { mutableStateOf(false) }
    var localInputText by remember { mutableStateOf("") }
    var localIsRecording by remember { mutableStateOf(false) }
    var localRecordingAmplitude by remember { mutableFloatStateOf(0f) }
    var localMediumProposal by remember { mutableStateOf<ParsedCommand?>(null) }
    var localHighConfidenceCommittedTitle by remember { mutableStateOf<String?>(null) }
    var localLowConfidenceNotice by remember { mutableStateOf<String?>(null) }

    val isQuickCaptureOpen = qcState?.isVisible ?: localIsOpen
    val quickCaptureInput = qcState?.inputText ?: localInputText
    val isVoiceRecording = qcState?.isRecording ?: localIsRecording
    val recordingAmplitude = qcState?.recordingAmplitude ?: localRecordingAmplitude
    val mediumProposal = qcState?.mediumProposal ?: localMediumProposal
    val highConfidenceCommittedTitle = qcState?.highConfidenceCommittedTitle ?: localHighConfidenceCommittedTitle
    val lowConfidenceNotice = qcState?.lowConfidenceNotice ?: localLowConfidenceNotice

    // FAB visible on Home, Habits, and Study surfaces (EXPERIENCE.md §2)
    val showQuickCaptureFab = currentDestination in listOf(
        PersonalTrackerDestination.HOME,
        PersonalTrackerDestination.HABITS,
        PersonalTrackerDestination.STUDY
    )

    val snackbarHostState = remember { SnackbarHostState() }

    if (quickCaptureViewModel != null) {
        LaunchedEffect(quickCaptureViewModel) {
            quickCaptureViewModel.uiEffect.collect { effect ->
                when (effect) {
                    is QuickCaptureUiEffect.ShowUndoToast -> {
                        val result = snackbarHostState.showSnackbar(
                            message = "Captured: ${effect.taskTitle}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            quickCaptureViewModel.onIntent(QuickCaptureUiIntent.UndoHighConfidence)
                        }
                    }
                    is QuickCaptureUiEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    is QuickCaptureUiEffect.CloseSheet -> {
                        // Handled via state visibility
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalRewardMotionStage provides rewardMotionStageState
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            onClick = {
                                if (quickCaptureViewModel != null) {
                                    quickCaptureViewModel.onIntent(QuickCaptureUiIntent.OpenSheet)
                                } else {
                                    localIsOpen = true
                                }
                            },
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
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.DismissSheet)
                    } else {
                        localIsOpen = false
                        localIsRecording = false
                    }
                },
                inputText = quickCaptureInput,
                onInputTextChanged = { text ->
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.InputTextChanged(text))
                    } else {
                        localInputText = text
                    }
                },
                onSubmitText = { text ->
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.SubmitText(text))
                    } else {
                        localInputText = ""
                        localIsOpen = false
                        localHighConfidenceCommittedTitle = text
                        onQuickCaptureSubmitted?.invoke(text)
                    }
                },
                isRecording = isVoiceRecording,
                recordingAmplitude = recordingAmplitude,
                onToggleRecording = {
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.ToggleRecording)
                    } else {
                        localIsRecording = !localIsRecording
                        localRecordingAmplitude = if (localIsRecording) 0.65f else 0f
                    }
                },
                mediumConfidenceProposal = mediumProposal,
                onConfirmProposal = { proposal ->
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.ConfirmProposal(proposal))
                    } else {
                        localMediumProposal = null
                        localIsOpen = false
                        onConfirmProposal?.invoke(proposal)
                    }
                },
                onDiscardProposal = {
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.DiscardProposal)
                    } else {
                        localMediumProposal = null
                    }
                },
                onProposalChipClick = { chipType ->
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.ProposalChipClicked(chipType))
                    }
                },
                highConfidenceCommittedTitle = highConfidenceCommittedTitle,
                onUndoHighConfidence = {
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.UndoHighConfidence)
                    } else {
                        localHighConfidenceCommittedTitle = null
                        onUndoCapture?.invoke()
                    }
                },
                onDismissUndoBanner = {
                    if (quickCaptureViewModel != null) {
                        quickCaptureViewModel.onIntent(QuickCaptureUiIntent.DismissUndoBanner)
                    } else {
                        localHighConfidenceCommittedTitle = null
                    }
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
