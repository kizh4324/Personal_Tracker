package com.personaltracker.ui.screens.companion

import com.personaltracker.ui.components.animation.RewardAnimationType

/**
 * Sealed interface representing one-shot transient side effects emitted by CompanionViewModel (AD-2).
 */
sealed interface CompanionUiEffect {
    data class PlayLottieReward(val type: RewardAnimationType) : CompanionUiEffect
    data class ShowSnackbar(val message: String) : CompanionUiEffect
}
