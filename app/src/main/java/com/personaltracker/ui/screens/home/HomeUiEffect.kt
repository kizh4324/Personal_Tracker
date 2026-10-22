package com.personaltracker.ui.screens.home

import androidx.compose.ui.geometry.Offset
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.navigation.PersonalTrackerDestination

/**
 * Sealed interface representing one-shot transient side effects emitted by HomeViewModel (AD-2).
 */
sealed interface HomeUiEffect {
    data class ShowCoinReward(val startOffset: Offset, val coinCount: Int) : HomeUiEffect
    data class ShowLottieReward(val type: RewardAnimationType) : HomeUiEffect
    data class ShowSnackbar(val message: String) : HomeUiEffect
    data class NavigateTo(val destination: PersonalTrackerDestination) : HomeUiEffect
}
