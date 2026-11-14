package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.DeliveryIntensity
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.StreakTier
import com.personaltracker.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GamificationLedgerEngineTest {

    private lateinit var engine: GamificationLedgerEngine

    @Before
    fun setUp() {
        engine = GamificationLedgerEngine()
    }

    // ==========================================
    // 1. Coin Reward Math Tests
    // ==========================================

    @Test
    fun testCalculateTaskRewardAcrossDeliveryIntensities() {
        val urgentTask = Task(id = "t1", title = "Algorithms Exam", scheduledDate = "2026-10-16", deliveryIntensity = "URGENT")
        val importantTask = Task(id = "t2", title = "Write essay", scheduledDate = "2026-10-16", deliveryIntensity = "IMPORTANT")
        val routineTask = Task(id = "t3", title = "Buy groceries", scheduledDate = "2026-10-16", deliveryIntensity = "ROUTINE")

        assertEquals(10, engine.calculateTaskReward(urgentTask))
        assertEquals(7, engine.calculateTaskReward(importantTask))
        assertEquals(5, engine.calculateTaskReward(routineTask))

        assertEquals(10, engine.calculateTaskReward(DeliveryIntensity.URGENT))
        assertEquals(7, engine.calculateTaskReward(DeliveryIntensity.IMPORTANT))
        assertEquals(5, engine.calculateTaskReward(DeliveryIntensity.ROUTINE))
    }

    @Test
    fun testCalculateHabitRewardWithStreakTierAndMilestones() {
        // Cold (streak 0): base 5 + 0 bonus = 5
        val coldHabit = Habit(id = "h0", name = "Hydration", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 0)
        assertEquals(StreakTier.COLD, coldHabit.streakTier)
        assertEquals(5, engine.calculateHabitReward(coldHabit))

        // Warming (streak 2): base 5 + 1 = 6
        val warmingHabit = Habit(id = "h1", name = "Reading", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 2)
        assertEquals(StreakTier.WARMING, warmingHabit.streakTier)
        assertEquals(6, engine.calculateHabitReward(warmingHabit))

        // Burning (streak 4): base 5 + 2 = 7
        val burningHabit = Habit(id = "h2", name = "Meditate", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 4)
        assertEquals(StreakTier.BURNING, burningHabit.streakTier)
        assertEquals(7, engine.calculateHabitReward(burningHabit))

        // Blazing non-milestone (streak 8): base 5 + 3 = 8
        val blazingHabit = Habit(id = "h3", name = "Exercise", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 8)
        assertEquals(StreakTier.BLAZING, blazingHabit.streakTier)
        assertEquals(8, engine.calculateHabitReward(blazingHabit))

        // 7-day milestone (streak 7): base 5 + 3 (Blazing) + 10 (Milestone) = 18
        val milestone7Habit = Habit(id = "h4", name = "Code", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 7)
        assertEquals(18, engine.calculateHabitReward(milestone7Habit))

        // 14-day milestone (streak 14): base 5 + 3 (Blazing) + 10 (Milestone) = 18
        val milestone14Habit = Habit(id = "h5", name = "Walk", targetDaysOfWeek = "1,2,3,4,5,6,7", currentStreak = 14)
        assertEquals(18, engine.calculateHabitReward(milestone14Habit))
    }

    @Test
    fun testCalculateStudySessionRewardThresholds() {
        // Less than 10 minutes earns 0 coins
        assertEquals(0, engine.calculateStudyReward(5))
        assertEquals(0, engine.calculateStudyReward(9))
        assertEquals(0, engine.calculateStudySessionReward(9))

        // 10 minutes earns minimum 5 coins
        assertEquals(5, engine.calculateStudyReward(10))

        // 25 minutes earns 5 coins (minimum)
        assertEquals(5, engine.calculateStudyReward(25))

        // 60 minutes earns 10 coins
        assertEquals(10, engine.calculateStudyReward(60))

        // 90 minutes earns 15 coins
        assertEquals(15, engine.calculateStudyReward(90))

        // 120 minutes is capped at 15 coins
        assertEquals(15, engine.calculateStudyReward(120))
        assertEquals(15, engine.calculateStudySessionReward(120))
    }

    @Test
    fun testCalculateRoutineStepReward() {
        assertEquals(2, engine.calculateRoutineStepReward())
        assertEquals(2, GamificationLedgerEngine.calculateRoutineStepReward())
    }

    @Test
    fun testTwoTierCapClampingLogic() {
        // 1. Standard task reward under cap: requested 10, earned 20, cap 50 -> 10 awarded
        assertEquals(
            10,
            engine.calculateCappedReward(
                requestedAmount = 10,
                categoryEarned = 20,
                categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
                globalFarmableEarned = 20,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = true
            )
        )

        // 2. Partial clamping at category boundary: requested 7, earned 48, cap 50 -> 2 awarded
        assertEquals(
            2,
            engine.calculateCappedReward(
                requestedAmount = 7,
                categoryEarned = 48,
                categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
                globalFarmableEarned = 48,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = true
            )
        )

        // 3. Category cap saturated: requested 5, earned 50, cap 50 -> 0 awarded
        assertEquals(
            0,
            engine.calculateCappedReward(
                requestedAmount = 5,
                categoryEarned = 50,
                categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
                globalFarmableEarned = 50,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = true
            )
        )

        // 4. Global farmable cap reached (even if category has space):
        // Task earned 30 (cap 50), but global farmable earned 100 (cap 100) -> 0 awarded
        assertEquals(
            0,
            engine.calculateCappedReward(
                requestedAmount = 5,
                categoryEarned = 30,
                categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
                globalFarmableEarned = 100,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = true
            )
        )

        // 5. Global farmable cap partial clamp:
        // Task earned 10 (cap 50), global farmable earned 97 (cap 100), requested 5 -> 3 awarded
        assertEquals(
            3,
            engine.calculateCappedReward(
                requestedAmount = 5,
                categoryEarned = 10,
                categoryCap = GamificationLedgerEngine.DAILY_TASK_CAP,
                globalFarmableEarned = 97,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = true
            )
        )

        // 6. Study session is exempt from global farmable cap:
        // Focus earned 115 (cap 120), global farmable earned 100 -> 5 awarded
        assertEquals(
            5,
            engine.calculateCappedReward(
                requestedAmount = 10,
                categoryEarned = 115,
                categoryCap = GamificationLedgerEngine.DAILY_FOCUS_CAP,
                globalFarmableEarned = 100,
                globalFarmableCap = GamificationLedgerEngine.DAILY_GLOBAL_FARMABLE_CAP,
                isFarmableCategory = false
            )
        )

        // 7. Non-positive requested amount returns 0
        assertEquals(0, engine.calculateCappedReward(0, 0, 50))
        assertEquals(0, engine.calculateCappedReward(-5, 0, 50))
    }

    // ==========================================
    // 2. Compound Idempotency Key Tests (AD-4, AD-6)
    // ==========================================

    @Test
    fun testDeterministicCompoundIdempotencyKeys() {
        val taskKey1 = engine.generateTaskCompletionKey("t123")
        val taskKey2 = engine.generateTaskIdempotencyKey("t123")
        assertEquals("TASK_COMP_t123", taskKey1)
        assertEquals(taskKey1, taskKey2)

        val habitKey1 = engine.generateHabitLogKey("water", LocalDate.parse("2026-10-16"))
        val habitKey2 = engine.generateHabitIdempotencyKey("water", "2026-10-16")
        assertEquals("HABIT_LOG_water_2026-10-16", habitKey1)
        assertEquals(habitKey1, habitKey2)

        val studyKey = engine.generateStudySessionKey("session_42")
        assertEquals("STUDY_SESSION_session_42", studyKey)

        val routineKey1 = engine.generateRoutineStepKey("morning", LocalDate.parse("2026-10-16"))
        val routineKey2 = engine.generateRoutineIdempotencyKey("morning", "2026-10-16")
        assertEquals("ROUTINE_COMP_morning_2026-10-16", routineKey1)
        assertEquals(routineKey1, routineKey2)

        val undoKey = engine.generateCaptureUndoKey("inbox_99")
        assertEquals("CAPTURE_UNDO_inbox_99", undoKey)
    }

    @Test
    fun testIdempotencyKeyGenerationRejectsWhitespaceOrBlank() {
        try {
            engine.generateTaskCompletionKey("t 123")
            fail("Whitespace should be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("whitespace") == true)
        }

        try {
            engine.generateHabitLogKey("", "2026-10-16")
            fail("Blank ID should be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-blank") == true)
        }

        try {
            engine.generateCaptureUndoKey("   ")
            fail("Blank inboxId should be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-blank") == true)
        }
    }

    // ==========================================
    // 3. Companion XP and Leveling Progression Tests
    // ==========================================

    @Test
    fun testCompanionProgressionLevelsUpAndCarriesEnergy() {
        val initialProfile = CompanionProfile(
            id = 1,
            speciesName = "Fox",
            level = 1,
            currentEnergyUnits = 80,
            activeAccessoryId = null,
            activeBackgroundId = null,
            currentExpressionOrdinal = 0
        )

        val updated = engine.addEnergy(initialProfile, 30)

        assertEquals(2, updated.level)
        assertEquals(10, updated.currentEnergyUnits)
        assertEquals(CompanionMood.EXCITED.ordinalValue, updated.currentExpressionOrdinal)
    }

    @Test
    fun testCompanionProgressionRejectsNegativeEnergy() {
        val profile = CompanionProfile(id = 1, speciesName = "Fox", level = 1, currentEnergyUnits = 50)
        try {
            engine.addEnergy(profile, -10)
            fail("Negative energy delta must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot be negative") == true)
        }
    }

    // ==========================================
    // 4. Positive Companion Mood Transitions Tests
    // ==========================================

    @Test
    fun testCompanionMoodTransitionsAreNonPunitive() {
        assertEquals(CompanionMood.CELEBRATING, engine.resolveMoodForEvent(CompanionTriggerEvent.TaskCompleted))
        assertEquals(CompanionMood.FOCUSED, engine.resolveMoodForEvent(CompanionTriggerEvent.FocusSessionStarted))
        assertEquals(CompanionMood.EXCITED, engine.resolveMoodForEvent(CompanionTriggerEvent.HabitCompleted(7)))
        assertEquals(CompanionMood.CELEBRATING, engine.resolveMoodForEvent(CompanionTriggerEvent.HabitCompleted(3)))
        assertEquals(CompanionMood.CONCERNED, engine.resolveMoodForEvent(CompanionTriggerEvent.StreakAtRisk))
        assertEquals(CompanionMood.SLEEPING, engine.resolveMoodForEvent(CompanionTriggerEvent.EveningRest))
        assertEquals(CompanionMood.ENCOURAGING, engine.resolveMoodForEvent(CompanionTriggerEvent.FreezeUsed))
        assertEquals(CompanionMood.ENCOURAGING, engine.resolveMoodForEvent(CompanionTriggerEvent.ReturnAfterAbsence))
    }

    @Test
    fun testCompanionProgressionProtectsAgainstIntegerOverflow() {
        val profile = CompanionProfile(
            id = 1,
            speciesName = "Fox",
            level = Int.MAX_VALUE - 1,
            currentEnergyUnits = 90
        )

        val updated = engine.addEnergy(profile, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, updated.level)
        assertTrue(updated.currentEnergyUnits >= 0)
    }

    @Test
    fun testCompanionProgressionSanitizesNegativeEnergyInProfile() {
        val corruptedProfile = CompanionProfile(
            id = 1,
            speciesName = "Fox",
            level = 1,
            currentEnergyUnits = -20
        )

        val updated = engine.addEnergy(corruptedProfile, 50)
        assertEquals(1, updated.level)
        assertEquals(50, updated.currentEnergyUnits)
    }
}
