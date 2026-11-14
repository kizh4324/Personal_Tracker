package com.personaltracker.domain.usecase

import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.model.CompanionProfile
import com.personaltracker.domain.model.EconomyEventTypes
import com.personaltracker.ui.TestEconomyRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AwardCoinsUseCaseTest {

    private lateinit var economyRepository: TestEconomyRepository
    private lateinit var ledgerEngine: GamificationLedgerEngine
    private lateinit var awardCoinsUseCase: AwardCoinsUseCase
    private val testDate = LocalDate.parse("2026-11-14")

    @Before
    fun setUp() {
        economyRepository = TestEconomyRepository()
        ledgerEngine = GamificationLedgerEngine()
        awardCoinsUseCase = AwardCoinsUseCase(economyRepository, ledgerEngine)

        // Seed initial companion profile
        val profile = CompanionProfile(
            id = 1,
            speciesName = "Sproutling",
            level = 1,
            currentEnergyUnits = 0
        )
        runTest {
            economyRepository.updateCompanionProfile(profile)
        }
    }

    @Test
    fun testAwardStudySessionStandaloneRewardAndCompanionProgression() = runTest {
        // 50 minutes study session -> (50 / 6).coerceIn(5, 15) = 8 coins
        val result = awardCoinsUseCase.awardStudySession(
            sessionId = "study_session_1",
            durationMinutes = 50,
            date = testDate
        )

        assertEquals(8, result.coinsAwarded)
        assertEquals(8, result.requestedCoins)
        assertFalse(result.isCapReached)
        assertFalse(result.isDuplicate)
        assertNotNull(result.transaction)
        assertEquals("STUDY_SESSION_study_session_1", result.transaction?.idempotencyKey)

        // Verify balance updated in repository
        assertEquals(8, economyRepository.getLatestBalance())

        // Verify companion energy progressed
        val companion = economyRepository.getCompanionProfile()
        assertNotNull(companion)
        assertEquals(8, companion?.currentEnergyUnits)
    }

    @Test
    fun testDuplicateAwardRequestReturnsZeroCoinsWithoutError() = runTest {
        // First award
        val firstResult = awardCoinsUseCase.awardStudySession(
            sessionId = "study_dup_1",
            durationMinutes = 30,
            date = testDate
        )
        assertEquals(5, firstResult.coinsAwarded)
        assertFalse(firstResult.isDuplicate)

        // Duplicate submission with same key
        val duplicateResult = awardCoinsUseCase.awardStudySession(
            sessionId = "study_dup_1",
            durationMinutes = 30,
            date = testDate
        )
        assertEquals(0, duplicateResult.coinsAwarded)
        assertEquals(5, duplicateResult.requestedCoins)
        assertTrue(duplicateResult.isDuplicate)

        // Balance remains unchanged (5, not 10)
        assertEquals(5, economyRepository.getLatestBalance())
    }

    @Test
    fun testAwardRoutineStepRewardAndCapEnforcement() = runTest {
        val result = awardCoinsUseCase.awardRoutineStep(
            routineId = "morning_routine_1",
            date = testDate
        )

        assertEquals(2, result.coinsAwarded)
        assertEquals(2, result.requestedCoins)
        assertFalse(result.isCapReached)
        assertFalse(result.isDuplicate)
        assertEquals("ROUTINE_COMP_morning_routine_1_2026-11-14", result.transaction?.idempotencyKey)
    }

    @Test
    fun testAwardCoinsEnforcesCategoryDailyCap() = runTest {
        // Pre-fill focus session category to 118 (focus cap is 120)
        val initialResult = awardCoinsUseCase(
            eventType = EconomyEventTypes.STUDY_SESSION,
            sourceEntityId = "session_prefill",
            requestedAmount = 118,
            idempotencyKey = "STUDY_SESSION_prefill",
            date = testDate
        )
        assertEquals(118, initialResult.coinsAwarded)

        // Attempt to award 10 coins -> only 2 remaining under 120 cap
        val cappedResult = awardCoinsUseCase.awardStudySession(
            sessionId = "session_clamp",
            durationMinutes = 60, // requested 10
            date = testDate
        )
        assertEquals(2, cappedResult.coinsAwarded)
        assertEquals(10, cappedResult.requestedCoins)
        assertTrue(cappedResult.isCapReached)
        assertFalse(cappedResult.isDuplicate)

        // Subsequent session awards 0 coins
        val saturatedResult = awardCoinsUseCase.awardStudySession(
            sessionId = "session_overflow",
            durationMinutes = 60,
            date = testDate
        )
        assertEquals(0, saturatedResult.coinsAwarded)
        assertTrue(saturatedResult.isCapReached)
    }

    @Test
    fun testAwardCoinsRejectsInvalidParameters() = runTest {
        try {
            awardCoinsUseCase("", "source", 5, "KEY_1", testDate)
            fail("Blank eventType should fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("eventType must not be blank") == true)
        }

        try {
            awardCoinsUseCase(EconomyEventTypes.TASK_COMPLETION, "", 5, "KEY_1", testDate)
            fail("Blank sourceEntityId should fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("sourceEntityId must not be blank") == true)
        }

        try {
            awardCoinsUseCase(EconomyEventTypes.TASK_COMPLETION, "source", -5, "KEY_1", testDate)
            fail("Negative requestedAmount should fail")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-negative") == true)
        }
    }
}
