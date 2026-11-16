package com.personaltracker.ui.screens.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.engine.CompanionState
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.usecase.PurchaseOrEquipResult
import com.personaltracker.domain.usecase.PurchaseOrEquipShopItemUseCase
import com.personaltracker.domain.usecase.PurchaseStreakShieldResult
import com.personaltracker.domain.usecase.PurchaseStreakShieldUseCase
import com.personaltracker.ui.components.animation.RewardAnimationType
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
 * maintains atomic idempotent ledgering for shop item purchases and equip toggles,
 * and triggers milestone celebrations on companion level-ups.
 */
@HiltViewModel
class CompanionViewModel @Inject constructor(
    private val economyRepository: EconomyRepository,
    private val ledgerEngine: GamificationLedgerEngine,
    private val purchaseOrEquipShopItemUseCase: PurchaseOrEquipShopItemUseCase,
    private val purchaseStreakShieldUseCase: PurchaseStreakShieldUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState(isLoading = true))
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<CompanionUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<CompanionUiEffect> = _uiEffect.receiveAsFlow()

    private var observeDataJob: Job? = null
    private var previousLevel: Int? = null
    private var isPurchasingShield = false

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
                currentExpressionOrdinal = CompanionState.IDLE_COZY.ordinalValue
            )

            // Trigger celebration only on real level-up after initial cold start
            if (previousLevel != null && safeProfile.level > previousLevel!!) {
                _uiEffect.trySend(CompanionUiEffect.PlayLottieReward(RewardAnimationType.MilestoneFullReveal))
                handleSelectMood(CompanionState.CELEBRATING_REWARD)
            }
            previousLevel = safeProfile.level

            val resolvedMood = CompanionState.fromOrdinal(safeProfile.currentExpressionOrdinal)
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
            is CompanionUiIntent.PurchaseOrEquipItem -> handlePurchaseOrEquipItem(intent.item)
            is CompanionUiIntent.PurchaseItem -> handlePurchaseOrEquipItem(intent.item)
            is CompanionUiIntent.EquipItem -> handleEquipItem(intent.item, intent.equip)
            is CompanionUiIntent.TriggerMilestoneCelebration -> handleTriggerMilestone()
            is CompanionUiIntent.TriggerCoinMicroBurst -> handleTriggerCoinMicroBurst()
            is CompanionUiIntent.Refresh -> observeCompanionData()
        }
    }

    private fun handleSelectMood(mood: CompanionState) {
        viewModelScope.launch {
            val currentProfile = economyRepository.getCompanionProfile()
            if (currentProfile != null) {
                val updated = currentProfile.copy(currentExpressionOrdinal = mood.ordinalValue)
                economyRepository.updateCompanionProfile(updated)
            }
            _uiState.update { it.copy(mood = mood) }
        }
    }

    private fun handlePurchaseOrEquipItem(item: ShopItem) {
        if (item.category == ShopItem.CATEGORY_UTILITY || item.id == "item_streak_shield") {
            if (isPurchasingShield) return
            isPurchasingShield = true
            viewModelScope.launch {
                try {
                    when (val result = purchaseStreakShieldUseCase(targetHabitId = null)) {
                        is PurchaseStreakShieldResult.Success -> {
                            _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.CoinEarnMicro))
                            _uiEffect.send(CompanionUiEffect.ShowSnackbar("Grace Shield activated on ${result.updatedHabit.name}! Streak protected."))
                            handleSelectMood(CompanionState.CELEBRATING_REWARD)
                        }
                        is PurchaseStreakShieldResult.CapReached -> {
                            _uiEffect.send(
                                CompanionUiEffect.ShowSnackbar(
                                    "Streak shield bank is already at maximum capacity (${result.currentShields}/${result.maxAllowed}). Keep up the wonderful momentum!"
                                )
                            )
                        }
                        is PurchaseStreakShieldResult.InsufficientCoins -> {
                            _uiEffect.send(CompanionUiEffect.ShowSnackbar("Not enough coins yet. Keep going!"))
                        }
                        is PurchaseStreakShieldResult.HabitNotFound -> {
                            _uiEffect.send(CompanionUiEffect.ShowSnackbar("No habit found to protect."))
                        }
                        is PurchaseStreakShieldResult.Error -> {
                            _uiEffect.send(CompanionUiEffect.ShowSnackbar(result.message))
                        }
                    }
                } finally {
                    isPurchasingShield = false
                }
            }
            return
        }

        viewModelScope.launch {
            when (val result = purchaseOrEquipShopItemUseCase(item)) {
                is PurchaseOrEquipResult.PurchasedAndEquipped -> {
                    _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.CoinEarnMicro))
                    _uiEffect.send(CompanionUiEffect.ShowSnackbar("Purchased and equipped ${result.item.name}!"))
                }
                is PurchaseOrEquipResult.Equipped -> {
                    _uiEffect.send(CompanionUiEffect.ShowSnackbar("Equipped ${result.item.name}!"))
                }
                is PurchaseOrEquipResult.Unequipped -> {
                    _uiEffect.send(CompanionUiEffect.ShowSnackbar("Unequipped ${result.item.name}."))
                }
                is PurchaseOrEquipResult.InsufficientCoins -> {
                    _uiEffect.send(CompanionUiEffect.ShowSnackbar("Not enough coins yet. Keep going!"))
                }
                is PurchaseOrEquipResult.Error -> {
                    _uiEffect.send(CompanionUiEffect.ShowSnackbar(result.message))
                }
            }
        }
    }

    private fun handleEquipItem(item: ShopItem, equip: Boolean) {
        viewModelScope.launch {
            economyRepository.equipShopItem(item.id, equip)
            val action = if (equip) "Equipped" else "Unequipped"
            _uiEffect.send(CompanionUiEffect.ShowSnackbar("$action ${item.name}!"))
        }
    }

    private fun handleTriggerMilestone() {
        viewModelScope.launch {
            _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.MilestoneFullReveal))
            handleSelectMood(CompanionState.CELEBRATING_REWARD)
        }
    }

    private fun handleTriggerCoinMicroBurst() {
        viewModelScope.launch {
            _uiEffect.send(CompanionUiEffect.PlayLottieReward(RewardAnimationType.CoinEarnMicro))
        }
    }

    companion object {
        fun mapOrdinalToCompanionState(ordinal: Int): CompanionState =
            CompanionState.fromOrdinal(ordinal)

        fun mapCompanionStateToOrdinal(state: CompanionState): Int =
            state.ordinalValue
    }
}
