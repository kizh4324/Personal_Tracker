package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.FocusIntervalType
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests validating [InterceptDistractionUseCase] (AD-1, AD-3, FR-4.3, NFR-4).
 * Guarantees zero Android framework imports and tests all interval/package permutations.
 */
class InterceptDistractionUseCaseTest {

    private lateinit var fakeRepository: FakeInterventionRuleRepository
    private lateinit var useCase: InterceptDistractionUseCase

    private val testRules = listOf(
        InterventionRule(
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            isStrictBlocked = true
        ),
        InterventionRule(
            packageName = "com.twitter.android",
            appDisplayName = "Twitter / X",
            isStrictBlocked = true
        ),
        InterventionRule(
            packageName = "com.reddit.frontpage",
            appDisplayName = "Reddit",
            isStrictBlocked = false // Non-strict rule
        )
    )

    @Before
    fun setUp() {
        fakeRepository = FakeInterventionRuleRepository(testRules)
        useCase = InterceptDistractionUseCase(fakeRepository)
    }

    @Test
    fun testDistractionInterceptedDuringActiveDeepWork() = runTest {
        val result = useCase(
            packageName = "com.instagram.android",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertTrue("Expected Instagram to be intercepted during DEEP_WORK", result.shouldIntercept)
        assertNotNull(result.matchedRule)
        assertEquals("Instagram", result.matchedRule?.appDisplayName)
    }

    @Test
    fun testDistractionInterceptedDuringActivePomodoroWork() = runTest {
        val result = useCase(
            packageName = "com.twitter.android",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.POMODORO_WORK
        )

        assertTrue("Expected Twitter to be intercepted during POMODORO_WORK", result.shouldIntercept)
        assertNotNull(result.matchedRule)
        assertEquals("Twitter / X", result.matchedRule?.appDisplayName)
    }

    @Test
    fun testDistractionNotInterceptedDuringShortBreak() = runTest {
        val result = useCase(
            packageName = "com.instagram.android",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.SHORT_BREAK
        )

        assertFalse("Expected interception suppressed during SHORT_BREAK", result.shouldIntercept)
        assertTrue(result.reason.contains("Break interval active"))
    }

    @Test
    fun testDistractionNotInterceptedDuringLongBreak() = runTest {
        val result = useCase(
            packageName = "com.instagram.android",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.LONG_BREAK
        )

        assertFalse("Expected interception suppressed during LONG_BREAK", result.shouldIntercept)
        assertTrue(result.reason.contains("Break interval active"))
    }

    @Test
    fun testDistractionNotInterceptedWhenTimerIsIdle() = runTest {
        val result = useCase(
            packageName = "com.instagram.android",
            isTimerRunning = false,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected interception suppressed when timer is not running", result.shouldIntercept)
        assertTrue(result.reason.contains("idle or paused"))
    }

    @Test
    fun testDistractionNotInterceptedWhenTimerIsPaused() = runTest {
        val result = useCase(
            packageName = "com.instagram.android",
            isTimerRunning = true,
            isTimerPaused = true,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected interception suppressed when timer is paused", result.shouldIntercept)
        assertTrue(result.reason.contains("idle or paused"))
    }

    @Test
    fun testUnlistedPackageNotIntercepted() = runTest {
        val result = useCase(
            packageName = "com.google.android.calculator",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected unlisted package not to be intercepted", result.shouldIntercept)
        assertNull(result.matchedRule)
        assertTrue(result.reason.contains("not registered as a distraction target"))
    }

    @Test
    fun testOwnPackageNeverIntercepted() = runTest {
        val result = useCase(
            packageName = "com.personaltracker",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK,
            ownPackageName = "com.personaltracker"
        )

        assertFalse("Expected own package never to be intercepted", result.shouldIntercept)
        assertTrue(result.reason.contains("exempted"))
    }

    @Test
    fun testSystemDialerNeverIntercepted() = runTest {
        val result = useCase(
            packageName = "com.android.dialer",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected phone dialer never to be intercepted", result.shouldIntercept)
        assertTrue(result.reason.contains("exempted"))
    }

    @Test
    fun testSystemUiNeverIntercepted() = runTest {
        val result = useCase(
            packageName = "com.android.systemui",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected system UI never to be intercepted", result.shouldIntercept)
        assertTrue(result.reason.contains("exempted"))
    }

    @Test
    fun testNonStrictRuleNotIntercepted() = runTest {
        val result = useCase(
            packageName = "com.reddit.frontpage",
            isTimerRunning = true,
            isTimerPaused = false,
            intervalType = FocusIntervalType.DEEP_WORK
        )

        assertFalse("Expected non-strict rule not to trigger interception", result.shouldIntercept)
        assertNotNull(result.matchedRule)
        assertFalse(result.matchedRule?.isStrictBlocked ?: true)
    }

    private class FakeInterventionRuleRepository(
        private val rules: List<InterventionRule>
    ) : InterventionRuleRepository {
        override fun getBlockedAppRules(): Flow<List<InterventionRule>> = flowOf(rules)
        override suspend fun getRuleForPackage(packageName: String): InterventionRule? =
            rules.find { it.packageName.equals(packageName, ignoreCase = true) }
        override suspend fun saveRule(rule: InterventionRule) {}
        override suspend fun deleteRule(packageName: String) {}
    }
}
