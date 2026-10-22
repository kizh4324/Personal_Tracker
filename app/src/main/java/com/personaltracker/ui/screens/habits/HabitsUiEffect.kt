package com.personaltracker.ui.screens.habits

import androidx.compose.ui.geometry.Offset
import com.personaltracker.ui.components.animation.RewardAnimationType

/**
 * Sealed interface representing one-shot side effects emitted by HabitsViewModel (AD-2).
 */
sealed interface HabitsUiEffect {
    data class ShowCoinReward(val startOffset: Offset, val coinCount: Int) : HabitsUiEffect
    data class ShowLottieReward(val type: RewardAnimationType) : HabitsUiEffect
    data class ShowSnackbar(val message: String) : HabitsUiEffect
}
