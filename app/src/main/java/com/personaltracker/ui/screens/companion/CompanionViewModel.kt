package com.personaltracker.ui.screens.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.companion.CompanionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Production ViewModel driving the non-punishing companion pet screen (AD-1, AD-4).
 * Enforces zero health points, zero hunger decay, zero death mechanics,
 * and maintains atomic idempotent ledgering for shop item purchases.
 */
@HiltViewModel
class CompanionViewModel @Inject constructor(
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState(isLoading = true))
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<CompanionUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<CompanionUiEffect> = _uiEffect.receiveAsFlow()

    private var observeDataJob: Job? = null

    init {
        observeCompanionData()
    }

    private fun observeCompanionData() {
        observeDataJob?.cancel()
        val profileFlow = economyRepository.observeCompanionProfile()
        val balanceFlow = economyRepository.observeLatestBalance()
        val shopItemsFlow = economyRepository.getAllShopItems()

        observeDataJob = combine(
            profileFlow,
            balanceFlow,
            shopItemsFlow
        ) { profile, balance, shopItems ->
            val safeProfile = profile ?: CompanionProfile(
                speciesName = "Sproutling",
                level = 1,
                currentEnergyUnits = 0,
                currentExpressionOrdinal = CompanionMood.IDLE_CONTENT.ordinalValue
            )

            val resolvedMood = mapOrdinalToCompanionState(safeProfile.currentExpressionOrdinal)
            val energyProgress = (safeProfile.currentEnergyUnits.toFloat() / GamificationLedgerEngine.ENERGY_PER_LEVEL)
                .coerceIn(0f, 1f)

            CompanionUiState(
                speciesName = safeProfile.speciesName,
                level = safeProfile.level,
                currentEnergyUnits = safeProfile.currentEnergyUnits,
                maxEnergyUnits = GamificationLedgerEngine.ENERGY_PER_LEVEL,
                energyProgress = energyProgress,
                coinBalance = balance,
                mood = resolvedMood,
                availableShopItems = shopItems,
                activeAccessoryId = safeProfile.activeAccessoryId,
                activeBackgroundId = safeProfile.activeBackgroundId,
                isLoading = false,
                errorMessage = null
            )
        }.distinctUntilChanged()
        .onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: CompanionUiIntent) {
        when (intent) {
            is CompanionUiIntent.SelectMood -> handleSelectMood(intent.mood)
            is CompanionUiIntent.PurchaseItem -> handlePurchaseItem(intent.item)
            is CompanionUiIntent.TriggerMilestoneCelebration -> handleTriggerMilestone()
            is CompanionUiIntent.TriggerCoinMicroBurst -> handleTriggerCoinMicroBurst()
            is CompanionUiIntent.Refresh -> observeCompanionData()
        }
    }

    private fun handleSelectMood(mood: CompanionState) {
        viewModelScope.launch {
            val currentProfile = economyRepository.getCompanionProfile()
            if (currentProfile != null) {
                val ordinal = mapCompanionStateToOrdinal(mood)
                val updated = currentProfile.copy(currentExpressionOrdinal = ordinal)
                economyRepository.updateCompanionProfile(updated)
            }
            _uiState.update { it.copy(mood = mood) }
        }
    }

    private fun handlePurchaseItem(item: ShopItem) {
        viewModelScope.launch {
            if (item.isUnlocked) {
                _uiEffect.send(CompanionUiEffect.ShowSnackbar("Item already unlocked"))
                return@launch
            }

            val currentBalance = _uiState.value.coinBalance
            if (currentBalance < item.priceCoins) {
                _uiEffect.send(CompanionUiEffect.ShowSnackbar("Not enough coins yet. Keep going!"))
                return@launch
            }

            val idempotencyKey = "SHOP_BUY_${item.id}"
            runCatching {
                economyRepository.purchaseShopItem(item.id, idempotencyKey)
                _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.CoinEarnMicro))
                _uiEffect.send(CompanionUiEffect.ShowSnackbar("Purchased ${item.name}!"))
            }.onFailure {
                _uiEffect.send(CompanionUiEffect.ShowSnackbar("Could not complete purchase. Please try again."))
            }
        }
    }

    private fun handleTriggerMilestone() {
        viewModelScope.launch {
            _uiState.update { it.copy(mood = CompanionState.Celebrating) }
            _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.MilestoneFullReveal))
        }
    }

    private fun handleTriggerCoinMicroBurst() {
        viewModelScope.launch {
            _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.CoinEarnMicro))
        }
    }

    companion object {
        /**
         * Maps CompanionMood ordinal (0-6) safely to UI [CompanionState] with non-punishing fallback to Idle.
         */
        fun mapOrdinalToCompanionState(ordinal: Int): CompanionState {
            return when (ordinal) {
                CompanionMood.IDLE_CONTENT.ordinalValue -> CompanionState.Idle
                CompanionMood.ENCOURAGING.ordinalValue -> CompanionState.Encouraging
                CompanionMood.CELEBRATING.ordinalValue -> CompanionState.Celebrating
                CompanionMood.CONCERNED.ordinalValue -> CompanionState.Concerned
                CompanionMood.FOCUSED.ordinalValue -> CompanionState.Focused
                CompanionMood.SLEEPING.ordinalValue -> CompanionState.Sleeping
                CompanionMood.EXCITED.ordinalValue -> CompanionState.Excited
                else -> CompanionState.Idle
            }
        }

        fun mapCompanionStateToOrdinal(state: CompanionState): Int {
            return when (state) {
                CompanionState.Idle -> CompanionMood.IDLE_CONTENT.ordinalValue
                CompanionState.Encouraging -> CompanionMood.ENCOURAGING.ordinalValue
                CompanionState.Celebrating -> CompanionMood.CELEBRATING.ordinalValue
                CompanionState.Concerned -> CompanionMood.CONCERNED.ordinalValue
                CompanionState.Focused -> CompanionMood.FOCUSED.ordinalValue
                CompanionState.Sleeping -> CompanionMood.SLEEPING.ordinalValue
                CompanionState.Excited -> CompanionMood.EXCITED.ordinalValue
            }
        }
    }
}
