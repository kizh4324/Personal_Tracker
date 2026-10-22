package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.ui.components.animation.RewardAnimationType
import com.personaltracker.ui.components.companion.CompanionState
import com.personaltracker.ui.screens.companion.CompanionUiEffect
import com.personaltracker.ui.screens.companion.CompanionUiIntent
import com.personaltracker.ui.screens.companion.CompanionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var ledgerEngine: GamificationLedgerEngine

    @Before
    fun setUp() {
        economyRepository = TestEconomyRepository()
        ledgerEngine = GamificationLedgerEngine()
    }

    private fun createViewModel(): CompanionViewModel {
        return CompanionViewModel(
            economyRepository = economyRepository,
            ledgerEngine = ledgerEngine
        )
    }

    @Test
    fun testInitialUiStateDefaultSproutling() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Sproutling", state.speciesName)
            assertEquals(1, state.level)
            assertEquals(0, state.currentEnergyUnits)
            assertEquals(CompanionState.Idle, state.mood)
            assertEquals(0, state.coinBalance)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun testMoodProgressionAcross7Registers() = runTest {
        val profile = CompanionProfile(
            speciesName = "Sproutling",
            level = 2,
            currentEnergyUnits = 40,
            currentExpressionOrdinal = CompanionMood.CELEBRATING.ordinalValue
        )
        economyRepository.profileFlow.value = profile

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CompanionState.Celebrating, state.mood)
            assertEquals(2, state.level)
            assertEquals(40, state.currentEnergyUnits)
            assertEquals(0.40f, state.energyProgress, 0.001f)
        }
    }

    @Test
    fun testNonPunishingFallbackToIdleOnUnknownMood() = runTest {
        val unknownOrdinal = 999
        val mappedState = CompanionViewModel.mapOrdinalToCompanionState(unknownOrdinal)
        assertEquals(CompanionState.Idle, mappedState)
    }

    @Test
    fun testShopItemPurchaseSucceedsWithSufficientBalance() = runTest {
        economyRepository.balanceFlow.value = 50
        val accessory = ShopItem(
            id = "item_hat_1",
            name = "Cozy Beanie",
            category = "ACCESSORY",
            priceCoins = 30
        )
        economyRepository.shopItemsFlow.value = listOf(accessory)

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.PurchaseItem(accessory))

            val effect1 = awaitItem()
            assertTrue(effect1 is CompanionUiEffect.PlayLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect1 as CompanionUiEffect.PlayLottieReward).type)

            val effect2 = awaitItem()
            assertTrue(effect2 is CompanionUiEffect.ShowSnackbar)
            assertTrue((effect2 as CompanionUiEffect.ShowSnackbar).message.contains("Purchased Cozy Beanie"))

            // Coin balance deducted safely
            assertEquals(20, economyRepository.balanceFlow.value)
        }
    }

    @Test
    fun testShopItemPurchaseFailsGracefullyWithZeroGuiltCopyOnInsufficientBalance() = runTest {
        economyRepository.balanceFlow.value = 10
        val item = ShopItem(
            id = "item_wings_1",
            name = "Golden Wings",
            category = "ACCESSORY",
            priceCoins = 100
        )

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.PurchaseItem(item))

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.ShowSnackbar)
            val msg = (effect as CompanionUiEffect.ShowSnackbar).message
            // Zero shame or guilt copy (AD-4)
            assertEquals("Not enough coins yet. Keep going!", msg)

            // Balance unchanged
            assertEquals(10, economyRepository.balanceFlow.value)
        }
    }

    @Test
    fun testShopItemPurchaseAlreadyUnlockedEmitsSnackbarWithoutCoinDeduction() = runTest {
        economyRepository.balanceFlow.value = 50
        val unlockedItem = ShopItem(
            id = "item_hat_unlocked",
            name = "Cozy Beanie",
            category = "ACCESSORY",
            priceCoins = 30,
            isUnlocked = true
        )

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.PurchaseItem(unlockedItem))

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.ShowSnackbar)
            assertEquals("Item already unlocked", (effect as CompanionUiEffect.ShowSnackbar).message)

            // Balance unchanged
            assertEquals(50, economyRepository.balanceFlow.value)
        }
    }

    @Test
    fun testTriggerMilestoneCelebration() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.TriggerMilestoneCelebration)

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.PlayLottieReward)
            assertEquals(RewardAnimationType.MilestoneFullReveal, (effect as CompanionUiEffect.PlayLottieReward).type)

            assertEquals(CompanionState.Celebrating, viewModel.uiState.value.mood)
        }
    }
}
