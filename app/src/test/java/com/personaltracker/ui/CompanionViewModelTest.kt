package com.personaltracker.ui

import app.cash.turbine.test
import com.personaltracker.domain.engine.CompanionMood
import com.personaltracker.domain.engine.CompanionState
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.usecase.PurchaseOrEquipShopItemUseCase
import com.personaltracker.domain.usecase.PurchaseStreakShieldUseCase
import com.personaltracker.ui.components.animation.RewardAnimationType
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
    private lateinit var habitRepository: TestHabitRepository
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var purchaseOrEquipShopItemUseCase: PurchaseOrEquipShopItemUseCase
    private lateinit var purchaseStreakShieldUseCase: PurchaseStreakShieldUseCase

    @Before
    fun setUp() {
        economyRepository = TestEconomyRepository()
        habitRepository = TestHabitRepository(economyRepository)
        ledgerEngine = GamificationLedgerEngine()
        purchaseOrEquipShopItemUseCase = PurchaseOrEquipShopItemUseCase(economyRepository)
        purchaseStreakShieldUseCase = PurchaseStreakShieldUseCase(habitRepository, economyRepository)
    }

    private fun createViewModel(): CompanionViewModel {
        return CompanionViewModel(
            economyRepository = economyRepository,
            ledgerEngine = ledgerEngine,
            purchaseOrEquipShopItemUseCase = purchaseOrEquipShopItemUseCase,
            purchaseStreakShieldUseCase = purchaseStreakShieldUseCase
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
            assertEquals(CompanionState.IDLE_COZY, state.mood)
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
            currentExpressionOrdinal = CompanionState.CELEBRATING_REWARD.ordinalValue
        )
        economyRepository.profileFlow.value = profile

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CompanionState.CELEBRATING_REWARD, state.mood)
            assertEquals(2, state.level)
            assertEquals(40, state.currentEnergyUnits)
            assertEquals(0.40f, state.energyProgress, 0.001f)
        }
    }

    @Test
    fun testNonPunishingFallbackToIdleOnUnknownMood() = runTest {
        val unknownOrdinal = 999
        val mappedState = CompanionViewModel.mapOrdinalToCompanionState(unknownOrdinal)
        assertEquals(CompanionState.IDLE_COZY, mappedState)
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
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(accessory))

            val effect1 = awaitItem()
            assertTrue(effect1 is CompanionUiEffect.PlayLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect1 as CompanionUiEffect.PlayLottieReward).type)

            val effect2 = awaitItem()
            assertTrue(effect2 is CompanionUiEffect.ShowSnackbar)
            assertTrue((effect2 as CompanionUiEffect.ShowSnackbar).message.contains("Purchased and equipped Cozy Beanie"))

            // Coin balance deducted safely
            assertEquals(20, economyRepository.balanceFlow.value)
            // Auto-equipped
            assertEquals("item_hat_1", economyRepository.profileFlow.value?.activeAccessoryId)
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
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(item))

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
    fun testShopItemPurchaseAlreadyUnlockedTogglesEquipWithoutCoinDeduction() = runTest {
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
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(unlockedItem))

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.ShowSnackbar)
            assertEquals("Equipped Cozy Beanie!", (effect as CompanionUiEffect.ShowSnackbar).message)

            // Balance unchanged
            assertEquals(50, economyRepository.balanceFlow.value)
            assertEquals("item_hat_unlocked", economyRepository.profileFlow.value?.activeAccessoryId)

            // Tapping again un-equips
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(unlockedItem))
            val effect2 = awaitItem()
            assertTrue(effect2 is CompanionUiEffect.ShowSnackbar)
            assertEquals("Unequipped Cozy Beanie.", (effect2 as CompanionUiEffect.ShowSnackbar).message)
            assertEquals(null, economyRepository.profileFlow.value?.activeAccessoryId)
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

            assertEquals(CompanionState.CELEBRATING_REWARD, viewModel.uiState.value.mood)
        }
    }

    @Test
    fun testCompanionLevelUpTriggersMilestoneCelebration() = runTest {
        val initialProfile = CompanionProfile(
            speciesName = "Sproutling",
            level = 1,
            currentEnergyUnits = 90
        )
        economyRepository.profileFlow.value = initialProfile

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            // Simulate level up: Level 1 -> 2
            val updatedProfile = initialProfile.copy(level = 2, currentEnergyUnits = 10)
            economyRepository.profileFlow.value = updatedProfile

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.PlayLottieReward)
            assertEquals(RewardAnimationType.MilestoneFullReveal, (effect as CompanionUiEffect.PlayLottieReward).type)
        }
    }

    @Test
    fun testPurchaseGraceShieldFromCompanionShopSucceeds() = runTest {
        economyRepository.balanceFlow.value = 50
        val habit = Habit(id = "h_companion_test", name = "Reading", targetDaysOfWeek = "1,2,3,4,5,6,7", slackBankDays = 0)
        habitRepository.saveHabit(habit)

        val shieldItem = ShopItem(
            id = "item_streak_shield",
            name = "Grace Shield",
            category = ShopItem.CATEGORY_UTILITY,
            priceCoins = 30
        )

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(shieldItem))

            val effect1 = awaitItem()
            assertTrue(effect1 is CompanionUiEffect.PlayLottieReward)
            assertEquals(RewardAnimationType.CoinEarnMicro, (effect1 as CompanionUiEffect.PlayLottieReward).type)

            val effect2 = awaitItem()
            assertTrue(effect2 is CompanionUiEffect.ShowSnackbar)
            assertTrue((effect2 as CompanionUiEffect.ShowSnackbar).message.contains("Grace Shield activated on Reading"))

            assertEquals(20, economyRepository.balanceFlow.value)
            assertEquals(1, habitRepository.getHabitById("h_companion_test")?.slackBankDays)
            // Utility item should NOT equip or corrupt accessory
            assertEquals(null, economyRepository.profileFlow.value?.activeAccessoryId)
        }
    }

    @Test
    fun testPurchaseGraceShieldAtCapEmitsEmpoweringMessage() = runTest {
        economyRepository.balanceFlow.value = 100
        val habit = Habit(id = "h_cap", name = "Meditation", targetDaysOfWeek = "1,2,3,4,5,6,7", slackBankDays = 3)
        habitRepository.saveHabit(habit)

        val shieldItem = ShopItem(
            id = "item_streak_shield",
            name = "Grace Shield",
            category = ShopItem.CATEGORY_UTILITY,
            priceCoins = 30
        )

        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onIntent(CompanionUiIntent.PurchaseOrEquipItem(shieldItem))

            val effect = awaitItem()
            assertTrue(effect is CompanionUiEffect.ShowSnackbar)
            assertTrue((effect as CompanionUiEffect.ShowSnackbar).message.contains("Streak shield bank is already at maximum capacity"))
            assertEquals(100, economyRepository.balanceFlow.value)
        }
    }
}
