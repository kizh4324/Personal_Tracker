package com.personaltracker.domain.engine

import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.Habit
import com.personaltracker.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

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
    }

    @Test
    fun testCalculateHabitReward() {
        val habit = Habit(id = "h1", name = "Hydration", targetDaysOfWeek = "1,2,3,4,5,6,7")
        assertEquals(5, engine.calculateHabitReward(habit))
    }

    @Test
    fun testCalculateStudySessionRewardThresholds() {
        // Less than 10 minutes earns 0 coins
        assertEquals(0, engine.calculateStudySessionReward(5))
        assertEquals(0, engine.calculateStudySessionReward(9))

        // 25 minutes earns 5 coins (minimum)
        assertEquals(5, engine.calculateStudySessionReward(25))

        // 60 minutes earns 10 coins
        assertEquals(10, engine.calculateStudySessionReward(60))

        // 120 minutes earns 20 coins
        assertEquals(20, engine.calculateStudySessionReward(120))
    }

    // ==========================================
    // 2. Compound Idempotency Key Tests (AD-4)
    // ==========================================

    @Test
    fun testDeterministicCompoundIdempotencyKeys() {
        val taskKey1 = engine.generateTaskIdempotencyKey("t123")
        val taskKey2 = engine.generateTaskIdempotencyKey("t123")
        assertEquals("TASK_COMP_t123", taskKey1)
        assertEquals(taskKey1, taskKey2)

        val habitKey = engine.generateHabitIdempotencyKey("water", "2026-10-16")
        assertEquals("HABIT_LOG_water_2026-10-16", habitKey)

        val studyKey = engine.generateStudyIdempotencyKey("session_42")
        assertEquals("STUDY_SESSION_session_42", studyKey)

        val routineKey = engine.generateRoutineIdempotencyKey("morning", "2026-10-16")
        assertEquals("ROUTINE_COMP_morning_2026-10-16", routineKey)
    }

    @Test
    fun testIdempotencyKeyGenerationRejectsWhitespaceOrBlank() {
        try {
            engine.generateTaskIdempotencyKey("t 123")
            fail("Whitespace should be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("whitespace") == true)
        }

        try {
            engine.generateHabitIdempotencyKey("", "2026-10-16")
            fail("Blank ID should be rejected")
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

        // Award 30 energy -> total 110 energy -> Level 2, 10 energy, expression = EXCITED (6)
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
        // Task completed -> CELEBRATING (2)
        assertEquals(CompanionMood.CELEBRATING, engine.resolveMoodForEvent(CompanionTriggerEvent.TaskCompleted))

        // Focus session started -> FOCUSED (4)
        assertEquals(CompanionMood.FOCUSED, engine.resolveMoodForEvent(CompanionTriggerEvent.FocusSessionStarted))

        // Streak milestone (7 days) -> EXCITED (6)
        assertEquals(CompanionMood.EXCITED, engine.resolveMoodForEvent(CompanionTriggerEvent.HabitCompleted(7)))

        // Regular habit completion -> CELEBRATING (2)
        assertEquals(CompanionMood.CELEBRATING, engine.resolveMoodForEvent(CompanionTriggerEvent.HabitCompleted(3)))

        // Streak at risk -> gentle CONCERNED (3), strictly never sad or crying
        assertEquals(CompanionMood.CONCERNED, engine.resolveMoodForEvent(CompanionTriggerEvent.StreakAtRisk))

        // Evening rest -> SLEEPING (5)
        assertEquals(CompanionMood.SLEEPING, engine.resolveMoodForEvent(CompanionTriggerEvent.EveningRest))

        // Freeze used / returned after absence -> ENCOURAGING (1)
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

        // Adding large energy should not throw ArithmeticException and safely cap at Int.MAX_VALUE
        val updated = engine.addEnergy(profile, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, updated.level)
        assertTrue(updated.currentEnergyUnits >= 0)
    }

    @Test
    fun testCompanionProgressionSanitizesNegativeEnergyInProfile() {
        // Corrupted DB state where energy was negative
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
