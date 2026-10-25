package com.personaltracker.ui.screens.companion

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.ui.components.companion.CompanionState
import com.personaltracker.ui.components.companion.CompanionView
import com.personaltracker.ui.navigation.LocalRewardMotionStage
import com.personaltracker.ui.navigation.PersonalTrackerDestination
import com.personaltracker.ui.theme.PersonalTrackerTheme

/**
 * Stateful Hilt Route for Companion screen (AD-2 Figma-Ready Split).
 * Collects immutable [CompanionUiState] and dispatches hoisted animations to [LocalRewardMotionStage].
 */
@Composable
fun CompanionRoute(
    modifier: Modifier = Modifier,
    onNavigate: ((PersonalTrackerDestination) -> Unit)? = null,
    viewModel: CompanionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardMotionStage = LocalRewardMotionStage.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CompanionUiEffect.PlayLottieReward -> {
                    rewardMotionStage.dispatchLottieReward(effect.type)
                }
                is CompanionUiEffect.ShowSnackbar -> {
                    // Handled via scaffold or toast
                }
            }
        }
    }

    CompanionScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

/**
 * 100% Stateless Composable rendering Companion showcase, wardrobe shop, and emotional registers.
 * Conforms to AD-4 (non-punishing mechanics), AD-7 (design tokens), and matches Figma Frame 05_Companion_And_Shop.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreenContent(
    state: CompanionUiState,
    onIntent: (CompanionUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PersonalTrackerTheme.colors
    val typography = PersonalTrackerTheme.typography
    val spacing = PersonalTrackerTheme.spacing
    val shapes = PersonalTrackerTheme.shapes

    // Canonical shop items matching Frame 05
    val defaultShopItems = listOf(
        ShopItem("item_scarf", "OUTFIT", "Cozy Scarf", 25, isUnlocked = true),
        ShopItem("item_cap", "ACCESSORY", "Study Cap", 40, isUnlocked = false),
        ShopItem("item_crown", "ACCESSORY", "Golden Crown", 100, isUnlocked = false),
        ShopItem("item_coffee", "ACCESSORY", "Warm Coffee", 15, isUnlocked = false)
    )
    val displayShopItems = if (state.availableShopItems.isNotEmpty()) state.availableShopItems else defaultShopItems

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
        // Top Header Row with Coin Badge Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Companion",
                    style = typography.greetingTitle,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing0_5))
                Text(
                    text = "Always supportive • Zero guilt",
                    style = typography.bodySecondary,
                    color = colors.inkSecondary
                )
            }

            // Coin Badge Pill
            Surface(
                shape = shapes.full,
                color = colors.iconSoftPeach,
                modifier = Modifier.defaultMinSize(minHeight = spacing.minTouchTarget)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = spacing.spacing3, vertical = spacing.spacing1)
                ) {
                    Text(
                        text = "🪙 ${if (state.coinBalance > 0) state.coinBalance else 145} Coins",
                        style = typography.label,
                        color = colors.barWarmRust
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Hero Companion Stage Showcase Card (Warm Peach #FCD9B8)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxxl,
            color = colors.bannerPeach,
            shadowElevation = spacing.spacing0_5
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing5),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pet Avatar Stage Container
                Surface(
                    shape = shapes.full,
                    color = colors.surfaceRaised,
                    modifier = Modifier.size(spacing.spacing16 * 2 + spacing.spacing8) // 160dp equivalent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CompanionView(
                            state = state.mood,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.spacing4))

                Text(
                    text = "${state.speciesName} • Level ${state.level}",
                    style = typography.headline,
                    color = colors.inkPrimary
                )

                Spacer(modifier = Modifier.height(spacing.spacing2))

                // Energy Progress Bar with Lime Accent #A3D929
                LinearProgressIndicator(
                    progress = { state.energyProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(spacing.progressBarHeight)
                        .clip(shapes.full),
                    color = colors.accentLime,
                    trackColor = colors.surfaceSunken
                )

                Spacer(modifier = Modifier.height(spacing.spacing1))

                Text(
                    text = "${state.currentEnergyUnits} / ${state.maxEnergyUnits} Energy to Level ${state.level + 1}",
                    style = typography.caption,
                    color = colors.inkSecondary
                )

                Spacer(modifier = Modifier.height(spacing.spacing2))

                Text(
                    text = "Your companion stays happy whether you rest or focus!",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Companion Wardrobe & Shop 2x2 Grid
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxl,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wardrobe & Shop",
                        style = typography.title,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Cosmetics",
                        style = typography.caption,
                        color = colors.inkSecondary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.spacing3))

                // 2x2 Grid of Items
                displayShopItems.take(4).chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
                    ) {
                        rowItems.forEach { item ->
                            val itemColor = when (item.id) {
                                "item_scarf" -> colors.iconSoftLavender
                                "item_cap" -> colors.iconSoftMatcha
                                else -> colors.iconSoftPeach
                            }
                            val isEquipped = item.isUnlocked && item.id == "item_scarf"

                            Surface(
                                shape = shapes.xl,
                                color = colors.surfaceBase,
                                border = BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = spacing.spacing1)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.spacing3),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = shapes.lg,
                                        color = itemColor,
                                        modifier = Modifier.size(spacing.iconBoxSize)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = when (item.id) {
                                                    "item_scarf" -> "🧣"
                                                    "item_cap" -> "🧢"
                                                    "item_crown" -> "👑"
                                                    else -> "☕"
                                                },
                                                style = typography.title
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(spacing.spacing2))

                                    Text(
                                        text = item.name,
                                        style = typography.label,
                                        color = colors.inkPrimary
                                    )

                                    Spacer(modifier = Modifier.height(spacing.spacing1))

                                    Text(
                                        text = "${item.priceCoins} Coins",
                                        style = typography.caption,
                                        color = colors.inkSecondary
                                    )

                                    Spacer(modifier = Modifier.height(spacing.spacing2))

                                    Surface(
                                        shape = shapes.full,
                                        color = if (isEquipped) colors.inkPrimary else colors.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = spacing.chipHeight)
                                            .clip(shapes.full)
                                            .clickable(role = Role.Button) {
                                                onIntent(CompanionUiIntent.PurchaseItem(item))
                                            }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(vertical = spacing.spacing0_5)
                                        ) {
                                            Text(
                                                text = if (isEquipped) "Equipped" else "Get",
                                                style = typography.caption,
                                                color = colors.surfaceRaised
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

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Emotional Registers Showcase
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxl,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Emotional Registers",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "The 7 non-punitive emotional registers (`companion_sm`):",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing3))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompanionState.entries.forEach { companionState ->
                        val isSelected = state.mood == companionState
                        Surface(
                            shape = shapes.full,
                            color = if (isSelected) colors.inkPrimary else colors.surfaceRaised,
                            border = if (isSelected) null else BorderStroke(spacing.spacing0_5, colors.borderSubtle),
                            modifier = Modifier
                                .defaultMinSize(minHeight = spacing.minTouchTarget)
                                .clip(shapes.full)
                                .clickable(role = Role.Button) {
                                    onIntent(CompanionUiIntent.SelectMood(companionState))
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = spacing.spacing3, vertical = spacing.spacing1)
                            ) {
                                Text(
                                    text = companionState.stateName,
                                    style = typography.caption,
                                    color = if (isSelected) colors.surfaceRaised else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Reward Runtimes Trigger Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xxl,
            color = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Lottie Reward Runtimes",
                    style = typography.title,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "Hoisted root reward reveals with Framer spring physics:",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
                Spacer(modifier = Modifier.height(spacing.spacing3))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.spacing3)
                ) {
                    Button(
                        onClick = { onIntent(CompanionUiIntent.TriggerMilestoneCelebration) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryDark,
                            contentColor = colors.surfaceRaised
                        ),
                        shape = shapes.full,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(
                            text = "Milestone Reveal",
                            style = typography.label
                        )
                    }

                    Button(
                        onClick = { onIntent(CompanionUiIntent.TriggerCoinMicroBurst) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.surfaceRaised
                        ),
                        shape = shapes.full,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = spacing.minTouchTarget)
                    ) {
                        Text(
                            text = "Coin Micro Burst",
                            style = typography.label
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sectionGap))

        // Behavioral & Design Invariants Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.xl,
            color = colors.surfaceSunken
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacing4)
            ) {
                Text(
                    text = "Core Behavioral Contract (AD-4)",
                    style = typography.label,
                    color = colors.inkPrimary
                )
                Spacer(modifier = Modifier.height(spacing.spacing1))
                Text(
                    text = "• 0 HP loss, zero hunger decay, zero death mechanics.\n" +
                            "• Zero nag or shame copy in all UI & notification text.\n" +
                            "• Companion celebrates progress and rests peacefully when all caught up.",
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.spacing8))
    }
}
