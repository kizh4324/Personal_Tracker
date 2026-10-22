package com.personaltracker.ui.screens.study

import com.personaltracker.ui.components.animation.RewardAnimationType

/**
 * Sealed interface representing one-shot side effects emitted by StudyViewModel (AD-2).
 */
sealed interface StudyUiEffect {
    data class ShowSessionCompleteCelebration(val coinsEarned: Int) : StudyUiEffect
    data class ShowLottieReward(val type: RewardAnimationType) : StudyUiEffect
    data class ShowSnackbar(val message: String) : StudyUiEffect
}
