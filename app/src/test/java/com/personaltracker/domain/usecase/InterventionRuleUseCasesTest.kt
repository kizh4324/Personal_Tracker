package com.personaltracker.domain.usecase

import app.cash.turbine.test
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for [GetInterventionRulesUseCase] and [TogglePackageInterceptionRuleUseCase]
 * (AD-1, AD-2, FR-4.6, FR-10.1, EXP-036).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InterventionRuleUseCasesTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepository: FakeRuleRepository
    private lateinit var getInterventionRulesUseCase: GetInterventionRulesUseCase
    private lateinit var togglePackageInterceptionRuleUseCase: TogglePackageInterceptionRuleUseCase

    private val initialRules = listOf(
        InterventionRule(
            packageName = "com.google.android.apps.authenticator2",
            appDisplayName = "Authenticator",
            isStrictBlocked = false
        ),
        InterventionRule(
            packageName = "com.bitwarden.bitwarden",
            appDisplayName = "Bitwarden",
            isStrictBlocked = false
        ),
        InterventionRule(
            packageName = "com.instagram.android",
            appDisplayName = "Instagram",
            isStrictBlocked = true
        ),
        InterventionRule(
            packageName = "com.twitter.android",
            appDisplayName = "Twitter / X",
            isStrictBlocked = true
        )
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        fakeRepository = FakeRuleRepository(initialRules)
        getInterventionRulesUseCase = GetInterventionRulesUseCase(fakeRepository, testDispatcher)
        togglePackageInterceptionRuleUseCase = TogglePackageInterceptionRuleUseCase(fakeRepository, testDispatcher)
    }

    @Test
    fun testCategorizedRulesSeparatesAllowedAndBlocked() = runTest(testDispatcher) {
        getInterventionRulesUseCase("").test {
            val categorized = awaitItem()
            assertEquals(2, categorized.whitelistedRules.size)
            assertEquals(2, categorized.blockedRules.size)
            assertEquals(4, categorized.allRules.size)

            assertTrue(categorized.whitelistedRules.any { it.appDisplayName == "Authenticator" })
            assertTrue(categorized.whitelistedRules.any { it.appDisplayName == "Bitwarden" })
            assertTrue(categorized.blockedRules.any { it.appDisplayName == "Instagram" })
            assertTrue(categorized.blockedRules.any { it.appDisplayName == "Twitter / X" })
        }
    }

    @Test
    fun testFilterRulesByDisplayNameMatchesCaseInsensitive() = runTest(testDispatcher) {
        getInterventionRulesUseCase("auth").test {
            val categorized = awaitItem()
            assertEquals(1, categorized.whitelistedRules.size)
            assertEquals(0, categorized.blockedRules.size)
            assertEquals("Authenticator", categorized.whitelistedRules.first().appDisplayName)
        }
    }

    @Test
    fun testFilterRulesByPackageName() = runTest(testDispatcher) {
        getInterventionRulesUseCase("instagram").test {
            val categorized = awaitItem()
            assertEquals(0, categorized.whitelistedRules.size)
            assertEquals(1, categorized.blockedRules.size)
            assertEquals("com.instagram.android", categorized.blockedRules.first().packageName)
        }
    }

    @Test
    fun testEmptySearchQueryReturnsAllRulesCategorized() = runTest(testDispatcher) {
        getInterventionRulesUseCase("   ").test {
            val categorized = awaitItem()
            assertEquals(2, categorized.whitelistedRules.size)
            assertEquals(2, categorized.blockedRules.size)
            assertEquals(4, categorized.allRules.size)
        }
    }

    @Test
    fun testReactiveSearchQueryFlowStreamsUpdatedCategorizedRules() = runTest(testDispatcher) {
        val queryFlow = MutableStateFlow("")
        getInterventionRulesUseCase(queryFlow).test {
            val initial = awaitItem()
            assertEquals(4, initial.allRules.size)

            queryFlow.value = "twitter"
            val filtered = awaitItem()
            assertEquals(1, filtered.allRules.size)
            assertEquals(1, filtered.blockedRules.size)
            assertEquals(0, filtered.whitelistedRules.size)
            assertEquals("Twitter / X", filtered.blockedRules.first().appDisplayName)

            queryFlow.value = ""
            val restored = awaitItem()
            assertEquals(4, restored.allRules.size)
        }
    }

    @Test
    fun testToggleExistingWhitelistedAppDeletesRulePerExp036() = runTest(testDispatcher) {
        // Authenticator is currently whitelisted (isStrictBlocked = false)
        assertNotNull(fakeRepository.getRuleForPackage("com.google.android.apps.authenticator2"))

        togglePackageInterceptionRuleUseCase("com.google.android.apps.authenticator2")

        // Per EXP-036, toggling off a whitelisted app deletes it to restore neutral unlisted state
        assertNull(fakeRepository.getRuleForPackage("com.google.android.apps.authenticator2"))
    }

    @Test
    fun testToggleExistingBlockedAppBecomesWhitelisted() = runTest(testDispatcher) {
        // Instagram is currently strictly blocked (isStrictBlocked = true)
        val before = fakeRepository.getRuleForPackage("com.instagram.android")
        assertNotNull(before)
        assertTrue(before!!.isStrictBlocked)

        togglePackageInterceptionRuleUseCase("com.instagram.android")

        val after = fakeRepository.getRuleForPackage("com.instagram.android")
        assertNotNull(after)
        assertFalse("Expected Instagram to be converted to whitelisted exemption", after!!.isStrictBlocked)
    }

    @Test
    fun testToggleUnlistedAppCreatesWhitelistedExemption() = runTest(testDispatcher) {
        val newPkg = "com.google.android.keep"
        assertNull(fakeRepository.getRuleForPackage(newPkg))

        togglePackageInterceptionRuleUseCase(newPkg, "Google Keep")

        val created = fakeRepository.getRuleForPackage(newPkg)
        assertNotNull(created)
        assertEquals("Google Keep", created!!.appDisplayName)
        assertFalse("Expected new rule to be a whitelisted exemption", created.isStrictBlocked)
    }

    @Test
    fun testToggleBlankPackageNameDoesNothing() = runTest(testDispatcher) {
        val countBefore = fakeRepository.getAllRulesSync().size
        togglePackageInterceptionRuleUseCase("   ", "Blank App")
        assertEquals(countBefore, fakeRepository.getAllRulesSync().size)
    }

    private class FakeRuleRepository(
        initialList: List<InterventionRule>
    ) : InterventionRuleRepository {
        private val rulesFlow = MutableStateFlow(initialList)

        override fun getAllRules(): Flow<List<InterventionRule>> = rulesFlow.asStateFlow()

        override fun getWhitelistedRules(): Flow<List<InterventionRule>> =
            rulesFlow.map { list -> list.filter { !it.isStrictBlocked } }

        override fun getBlockedAppRules(): Flow<List<InterventionRule>> =
            rulesFlow.map { list -> list.filter { it.isStrictBlocked } }

        override suspend fun getRuleForPackage(packageName: String): InterventionRule? =
            rulesFlow.value.find { it.packageName.equals(packageName, ignoreCase = true) }

        override suspend fun saveRule(rule: InterventionRule) {
            val list = rulesFlow.value.toMutableList()
            val idx = list.indexOfFirst { it.packageName.equals(rule.packageName, ignoreCase = true) }
            if (idx >= 0) {
                list[idx] = rule
            } else {
                list.add(rule)
            }
            rulesFlow.value = list
        }

        override suspend fun deleteRule(packageName: String) {
            rulesFlow.value = rulesFlow.value.filterNot { it.packageName.equals(packageName, ignoreCase = true) }
        }

        fun getAllRulesSync(): List<InterventionRule> = rulesFlow.value
    }
}
