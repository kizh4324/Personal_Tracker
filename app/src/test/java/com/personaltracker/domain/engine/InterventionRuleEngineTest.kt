package com.personaltracker.domain.engine

import com.personaltracker.domain.model.InterventionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterventionRuleEngineTest {

    private lateinit var engine: InterventionRuleEngine

    private val sampleRules = listOf(
        InterventionRule(
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            isStrictBlocked = true,
            dailyLimitMinutes = 30
        ),
        InterventionRule(
            packageName = "com.twitter.android",
            appDisplayName = "X / Twitter",
            isStrictBlocked = true,
            dailyLimitMinutes = 15
        )
    )

    @Before
    fun setUp() {
        engine = InterventionRuleEngine()
    }

    // ==========================================
    // 1. Distraction Rule Evaluation & Cooldown Tests
    // ==========================================

    @Test
    fun testUnmatchedAppDoesNotBlockOrPrompt() {
        val decision = engine.evaluateAppIntervention(
            packageName = "com.calculator.app",
            rules = sampleRules,
            lastPromptTimestamp = null
        )

        assertFalse(decision.shouldBlock)
        assertFalse(decision.shouldPromptJitai)
        assertNull(decision.matchedRule)
    }

    @Test
    fun testFirstEncounterWithBlockedAppTriggersJitaiPrompt() {
        val now = 1000000L
        val decision = engine.evaluateAppIntervention(
            packageName = "com.instagram.android",
            rules = sampleRules,
            lastPromptTimestamp = null,
            currentTimestamp = now
        )

        assertTrue(decision.shouldBlock)
        assertTrue(decision.shouldPromptJitai)
        assertNotNull(decision.matchedRule)
        assertEquals("Instagram", decision.matchedRule?.appDisplayName)
    }

    @Test
    fun testBlockedAppWithin5MinuteCooldownSuppressesJitaiPromptToAvoidNagging() {
        val lastPromptTime = 1000000L
        // 2 minutes later (within 5m / 300,000ms cooldown)
        val currentTime = lastPromptTime + (2 * 60 * 1000L)

        val decision = engine.evaluateAppIntervention(
            packageName = "com.instagram.android",
            rules = sampleRules,
            lastPromptTimestamp = lastPromptTime,
            currentTimestamp = currentTime
        )

        assertTrue(decision.shouldBlock)
        // Prompt MUST be suppressed
        assertFalse(decision.shouldPromptJitai)
        assertTrue(decision.reason.contains("suppressed"))
    }

    @Test
    fun testBlockedAppAfterCooldownExpiresFiresJitaiPromptAgain() {
        val lastPromptTime = 1000000L
        // 6 minutes later (cooldown 5m expired)
        val currentTime = lastPromptTime + (6 * 60 * 1000L)

        val decision = engine.evaluateAppIntervention(
            packageName = "com.instagram.android",
            rules = sampleRules,
            lastPromptTimestamp = lastPromptTime,
            currentTimestamp = currentTime
        )

        assertTrue(decision.shouldBlock)
        assertTrue(decision.shouldPromptJitai)
        assertTrue(decision.reason.contains("permitted"))
    }

    // ==========================================
    // 2. Urgency Scarcity & Capacity Tests
    // ==========================================

    @Test
    fun testUrgencyCapacityUnderLimitAllowsUrgent() {
        val assessment = engine.evaluateUrgencyCapacity(currentUrgentCount = 1)
        assertFalse(assessment.isScarcityExceeded)
        assertNull(assessment.advisoryMessage)
        assertEquals("URGENT", assessment.recommendedIntensity)
    }

    @Test
    fun testUrgencyCapacityExceededProvidesSoftGuidanceWithoutHardBlocking() {
        // Spec: When 3rd urgent item is added (or 2 already active), trigger soft guidance
        val assessment = engine.evaluateUrgencyCapacity(currentUrgentCount = 2)

        assertTrue(assessment.isScarcityExceeded)
        assertNotNull(assessment.advisoryMessage)
        assertTrue(assessment.advisoryMessage?.contains("You already have 2 Urgent items") == true)
        assertEquals("IMPORTANT", assessment.recommendedIntensity)
    }

    @Test
    fun testNonStrictBlockedAppPromptsJitaiWithoutHardBlock() {
        val nonStrictRule = InterventionRule(
            packageName = "com.news.reader",
            appDisplayName = "Daily News",
            isStrictBlocked = false,
            dailyLimitMinutes = 45
        )

        val decision = engine.evaluateAppIntervention(
            packageName = "com.news.reader",
            rules = listOf(nonStrictRule),
            lastPromptTimestamp = null
        )

        assertFalse(decision.shouldBlock)
        assertTrue(decision.shouldPromptJitai)
        assertEquals("Daily News", decision.matchedRule?.appDisplayName)
    }
}
