package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.ShopItem
import com.personaltracker.domain.usecase.PurchaseOrEquipResult
import com.personaltracker.domain.usecase.PurchaseOrEquipShopItemUseCase
import com.personaltracker.domain.usecase.ResolveCompanionMoodUseCase
import com.personaltracker.ui.TestDayTypeRepository
import com.personaltracker.ui.TestEconomyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompanionStateEngineTest {

    private lateinit var engine: CompanionStateEngine
    private lateinit var dayTypeRepository: TestDayTypeRepository
    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var resolveMoodUseCase: ResolveCompanionMoodUseCase
    private lateinit var purchaseOrEquipUseCase: PurchaseOrEquipShopItemUseCase

    @Before
    fun setUp() {
        engine = CompanionStateEngine()
        dayTypeRepository = TestDayTypeRepository()
        economyRepository = TestEconomyRepository()
        resolveMoodUseCase = ResolveCompanionMoodUseCase(engine, dayTypeRepository)
        purchaseOrEquipUseCase = PurchaseOrEquipShopItemUseCase(economyRepository)
    }

    // =========================================================================
    // 1. All 7 Non-Punishing Companion States (AD-4)
    // =========================================================================

    @Test
    fun testAll7NonPunishingStatesResolveCorrectly() {
        // State 0: IDLE_COZY
        val idleState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            isStreakAtRisk = false,
            recentTrigger = CompanionTriggerEvent.Idle
        )
        assertEquals(CompanionState.IDLE_COZY, idleState)
        assertEquals(0, idleState.ordinalValue)
        assertEquals("🦊", idleState.emoji)

        // State 1: CELEBRATING_REWARD
        val taskCompleteState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.TaskCompleted
        )
        assertEquals(CompanionState.CELEBRATING_REWARD, taskCompleteState)
        assertEquals(1, taskCompleteState.ordinalValue)
        assertEquals("🎉", taskCompleteState.emoji)

        val nonMilestoneHabitState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.HabitCompleted(streak = 5)
        )
        assertEquals(CompanionState.CELEBRATING_REWARD, nonMilestoneHabitState)

        val focusCompletedState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.FocusSessionCompleted
        )
        assertEquals(CompanionState.CELEBRATING_REWARD, focusCompletedState)

        // State 2: ENCOURAGING_WARM
        val freezeUsedState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.FreezeUsed
        )
        assertEquals(CompanionState.ENCOURAGING_WARM, freezeUsedState)
        assertEquals(2, freezeUsedState.ordinalValue)
        assertEquals("✨", freezeUsedState.emoji)

        val returnAfterAbsenceState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.ReturnAfterAbsence
        )
        assertEquals(CompanionState.ENCOURAGING_WARM, returnAfterAbsenceState)

        // State 3: CONCERNED_CHECKIN
        val streakAtRiskState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.StreakAtRisk
        )
        assertEquals(CompanionState.CONCERNED_CHECKIN, streakAtRiskState)
        assertEquals(3, streakAtRiskState.ordinalValue)
        assertEquals("🧐", streakAtRiskState.emoji)

        val idleStreakAtRiskState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            isStreakAtRisk = true,
            recentTrigger = CompanionTriggerEvent.Idle
        )
        assertEquals(CompanionState.CONCERNED_CHECKIN, idleStreakAtRiskState)

        // State 4: FOCUSING_STUDY
        val focusSessionStartedState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.FocusSessionStarted
        )
        assertEquals(CompanionState.FOCUSING_STUDY, focusSessionStartedState)
        assertEquals(4, focusSessionStartedState.ordinalValue)
        assertEquals("🧘", focusSessionStartedState.emoji)

        // State 5: RESTING_PEACEFUL
        val eveningRestState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.EveningRest
        )
        assertEquals(CompanionState.RESTING_PEACEFUL, eveningRestState)
        assertEquals(5, eveningRestState.ordinalValue)
        assertEquals("💤", eveningRestState.emoji)

        // State 6: HAPPY_BOUNCE
        val milestoneState = engine.resolveMood(
            dayTypeId = "dt_standard",
            isStudySessionActive = false,
            recentTrigger = CompanionTriggerEvent.MilestoneReached
        )
        assertEquals(CompanionState.HAPPY_BOUNCE, milestoneState)
        assertEquals(6, milestoneState.ordinalValue)
        assertEquals("⚡", milestoneState.emoji)
    }

    // =========================================================================
    // 2. DayType Rest Immunity Tests (AD-4)
    // =========================================================================

    @Test
    fun testDayTypeRestMappingReturnsRestingPeaceful() {
        val restDayTypes = listOf("dt_rest", "dt_low_capacity", "LOW_CAPACITY", "REST_RECOVERY")
        for (dayType in restDayTypes) {
            val mood = engine.resolveMood(
                dayTypeId = dayType,
                isStudySessionActive = false,
                isStreakAtRisk = true,
                recentTrigger = CompanionTriggerEvent.StreakAtRisk
            )
            assertEquals(
                "Rest DayType $dayType must unconditionally resolve to RESTING_PEACEFUL without guilt",
                CompanionState.RESTING_PEACEFUL,
                mood
            )
        }
    }

    @Test
    fun testStudySessionActiveOverridesRestDay() {
        val mood = engine.resolveMood(
            dayTypeId = "dt_rest",
            isStudySessionActive = true,
            isStreakAtRisk = false,
            recentTrigger = CompanionTriggerEvent.Idle
        )
        assertEquals(CompanionState.FOCUSING_STUDY, mood)
    }

    // =========================================================================
    // 3. Streak Milestone Multiples (7, 14, 21, ...)
    // =========================================================================

    @Test
    fun testStreakMilestoneMultiplesTriggerHappyBounce() {
        val milestones = listOf(7, 14, 21, 28, 70)
        for (streak in milestones) {
            val mood = engine.resolveMood(
                dayTypeId = "dt_standard",
                isStudySessionActive = false,
                recentTrigger = CompanionTriggerEvent.HabitCompleted(streak)
            )
            assertEquals(
                "Streak $streak is a 7-day multiple and must trigger HAPPY_BOUNCE",
                CompanionState.HAPPY_BOUNCE,
                mood
            )
        }

        val nonMilestones = listOf(1, 2, 6, 8, 13, 15)
        for (streak in nonMilestones) {
            val mood = engine.resolveMood(
                dayTypeId = "dt_standard",
                isStudySessionActive = false,
                recentTrigger = CompanionTriggerEvent.HabitCompleted(streak)
            )
            assertEquals(
                "Streak $streak is not a 7-day multiple and must trigger CELEBRATING_REWARD",
                CompanionState.CELEBRATING_REWARD,
                mood
            )
        }
    }

    // =========================================================================
    // 4. Leveling XP and Energy Progress (addEnergy)
    // =========================================================================

    @Test
    fun testAddEnergyAdvancesXpAndLevelsUpOnOverflow() {
        val initialProfile = CompanionProfile(
            speciesName = "Sproutling",
            level = 1,
            currentEnergyUnits = 60,
            currentExpressionOrdinal = CompanionState.IDLE_COZY.ordinalValue
        )

        // 1. Add 30 energy -> 90 total, no level up, mood unchanged
        val updatedNoLevel = engine.addEnergy(initialProfile, 30)
        assertEquals(1, updatedNoLevel.level)
        assertEquals(90, updatedNoLevel.currentEnergyUnits)
        assertEquals(CompanionState.IDLE_COZY.ordinalValue, updatedNoLevel.currentExpressionOrdinal)

        // 2. Add 20 energy to reach 110 -> 1 level gained, remainder 10, mood HAPPY_BOUNCE
        val updatedWithLevel = engine.addEnergy(updatedNoLevel, 20)
        assertEquals(2, updatedWithLevel.level)
        assertEquals(10, updatedWithLevel.currentEnergyUnits)
        assertEquals(CompanionState.HAPPY_BOUNCE.ordinalValue, updatedWithLevel.currentExpressionOrdinal)

        // 3. Add 250 energy -> level gained by 2 (2 -> 4), remainder 60
        val multiLevelGain = engine.addEnergy(updatedWithLevel, 250)
        assertEquals(4, multiLevelGain.level)
        assertEquals(60, multiLevelGain.currentEnergyUnits)
        assertEquals(CompanionState.HAPPY_BOUNCE.ordinalValue, multiLevelGain.currentExpressionOrdinal)

        // 4. Zero energy returns profile unchanged
        val unchanged = engine.addEnergy(multiLevelGain, 0)
        assertEquals(multiLevelGain, unchanged)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddEnergyRejectsNegativeDelta() {
        val profile = CompanionProfile(speciesName = "Sproutling", level = 1, currentEnergyUnits = 10)
        engine.addEnergy(profile, -10)
    }

    // =========================================================================
    // 5. Purchase & Equip Shop Item Use Case Tests (AD-4, AD-6)
    // =========================================================================

    @Test
    fun testPurchaseShopItemWithSufficientBalanceEquipsItem() = runTest {
        economyRepository.balanceFlow.value = 65
        val cap = ShopItem(
            id = "item_cap",
            category = "ACCESSORY",
            name = "Study Cap",
            priceCoins = 40,
            isUnlocked = false
        )
        economyRepository.shopItemsFlow.value = listOf(cap)

        val result = purchaseOrEquipUseCase(cap)
        assertTrue(result is PurchaseOrEquipResult.PurchasedAndEquipped)
        val success = result as PurchaseOrEquipResult.PurchasedAndEquipped
        assertEquals("Study Cap", success.item.name)
        assertTrue(success.item.isUnlocked)
        assertEquals(25, success.newBalance)
        assertEquals(25, economyRepository.balanceFlow.value)
        assertEquals("item_cap", economyRepository.profileFlow.value?.activeAccessoryId)
    }

    @Test
    fun testPurchaseShopItemWithInsufficientBalanceReturnsInsufficientCoins() = runTest {
        economyRepository.balanceFlow.value = 15
        val cap = ShopItem(
            id = "item_cap",
            category = "ACCESSORY",
            name = "Study Cap",
            priceCoins = 40,
            isUnlocked = false
        )
        economyRepository.shopItemsFlow.value = listOf(cap)

        val result = purchaseOrEquipUseCase(cap)
        assertTrue(result is PurchaseOrEquipResult.InsufficientCoins)
        val failure = result as PurchaseOrEquipResult.InsufficientCoins
        assertEquals(40, failure.required)
        assertEquals(15, failure.current)
        assertEquals(15, economyRepository.balanceFlow.value)
    }

    @Test
    fun testEquipAndUnequipToggleForUnlockedItem() = runTest {
        economyRepository.balanceFlow.value = 50
        val scarf = ShopItem(
            id = "item_scarf",
            category = "OUTFIT",
            name = "Cozy Scarf",
            priceCoins = 25,
            isUnlocked = true
        )
        economyRepository.shopItemsFlow.value = listOf(scarf)

        // 1. Initial equip
        val equipResult = purchaseOrEquipUseCase(scarf)
        assertTrue(equipResult is PurchaseOrEquipResult.Equipped)
        assertEquals("item_scarf", economyRepository.profileFlow.value?.activeAccessoryId)
        assertEquals(50, economyRepository.balanceFlow.value) // Zero coins deducted

        // 2. Tapping again un-equips
        val unequipResult = purchaseOrEquipUseCase(scarf)
        assertTrue(unequipResult is PurchaseOrEquipResult.Unequipped)
        assertNull(economyRepository.profileFlow.value?.activeAccessoryId)
        assertEquals(50, economyRepository.balanceFlow.value)
    }

    @Test
    fun testShopItemIconEmojiMapping() {
        val scarf = ShopItem("item_scarf", "OUTFIT", "Scarf", 25)
        assertEquals("🧣", scarf.iconEmoji)

        val cap = ShopItem("item_cap", "ACCESSORY", "Cap", 40)
        assertEquals("🧢", cap.iconEmoji)

        val crown = ShopItem("item_crown", "ACCESSORY", "Crown", 100)
        assertEquals("👑", crown.iconEmoji)

        val coffee = ShopItem("item_coffee", "ACCESSORY", "Coffee", 15)
        assertEquals("☕", coffee.iconEmoji)

        val unknown = ShopItem("item_unknown", "DECOR", "Unknown", 50)
        assertEquals("✨", unknown.iconEmoji)
    }
}
