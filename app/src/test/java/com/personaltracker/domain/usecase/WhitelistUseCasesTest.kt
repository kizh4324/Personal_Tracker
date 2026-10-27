package com.personaltracker.domain.usecase

import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests validating whitelist interactors (AD-1, AD-3, FR-4.6, FR-10.1).
 */
class WhitelistUseCasesTest {

    private lateinit var fakeRepository: InMemoryInterventionRuleRepository
    private lateinit var getWhitelistRulesUseCase: GetWhitelistRulesUseCase
    private lateinit var toggleAppWhitelistUseCase: ToggleAppWhitelistUseCase
    private lateinit var addWhitelistedAppUseCase: AddWhitelistedAppUseCase
    private lateinit var removeWhitelistedAppUseCase: RemoveWhitelistedAppUseCase

    @Before
    fun setUp() {
        fakeRepository = InMemoryInterventionRuleRepository(
            listOf(
                InterventionRule(
                    packageName = "com.instagram.android",
                    appDisplayName = "Instagram",
                    isStrictBlocked = true
                ),
                InterventionRule(
                    packageName = "com.google.android.apps.authenticator2",
                    appDisplayName = "Authenticator",
                    isStrictBlocked = false
                )
            )
        )
        getWhitelistRulesUseCase = GetWhitelistRulesUseCase(fakeRepository)
        toggleAppWhitelistUseCase = ToggleAppWhitelistUseCase(fakeRepository)
        addWhitelistedAppUseCase = AddWhitelistedAppUseCase(fakeRepository)
        removeWhitelistedAppUseCase = RemoveWhitelistedAppUseCase(fakeRepository)
    }

    @Test
    fun testGetWhitelistRulesStreamsOnlyWhitelistedRules() = runTest {
        val whitelisted = getWhitelistRulesUseCase(onlyWhitelisted = true).first()
        assertEquals(1, whitelisted.size)
        assertEquals("com.google.android.apps.authenticator2", whitelisted.first().packageName)
        assertFalse(whitelisted.first().isStrictBlocked)
    }

    @Test
    fun testGetWhitelistRulesStreamsAllRules() = runTest {
        val allRules = getWhitelistRulesUseCase(onlyWhitelisted = false).first()
        assertEquals(2, allRules.size)
    }

    @Test
    fun testToggleAppWhitelistFlipsBlockedToWhitelisted() = runTest {
        // Toggle blocked Instagram -> should become whitelisted
        toggleAppWhitelistUseCase("com.instagram.android")
        val ruleAfter = fakeRepository.getRuleForPackage("com.instagram.android")
        assertNotNull(ruleAfter)
        assertFalse(ruleAfter!!.isStrictBlocked)
    }

    @Test
    fun testToggleAppWhitelistDeletesAlreadyWhitelistedRule() = runTest {
        // Authenticator is already whitelisted (isStrictBlocked = false).
        // Toggling it off must delete the rule so it returns to neutral unlisted state instead of blocked.
        toggleAppWhitelistUseCase("com.google.android.apps.authenticator2")
        val ruleAfter = fakeRepository.getRuleForPackage("com.google.android.apps.authenticator2")
        assertNull(ruleAfter)
    }

    @Test
    fun testToggleAppWhitelistAddsNewWhitelistedRuleIfAbsent() = runTest {
        toggleAppWhitelistUseCase("com.bitwarden.bitwarden", "Bitwarden")
        val rule = fakeRepository.getRuleForPackage("com.bitwarden.bitwarden")
        assertNotNull(rule)
        assertEquals("Bitwarden", rule?.appDisplayName)
        assertFalse(rule!!.isStrictBlocked)
    }

    @Test
    fun testAddWhitelistedAppSuccessfullyInsertsExemption() = runTest {
        val result = addWhitelistedAppUseCase(
            packageName = "com.google.android.keep",
            appDisplayName = "Google Keep"
        )
        assertTrue(result)

        val saved = fakeRepository.getRuleForPackage("com.google.android.keep")
        assertNotNull(saved)
        assertEquals("Google Keep", saved?.appDisplayName)
        assertFalse(saved!!.isStrictBlocked)
    }

    @Test
    fun testAddWhitelistedAppDefensiveDisplayNameOnTrailingDots() = runTest {
        val result = addWhitelistedAppUseCase(
            packageName = "com.bitwarden.vault...",
            appDisplayName = ""
        )
        assertTrue(result)

        val saved = fakeRepository.getRuleForPackage("com.bitwarden.vault...")
        assertNotNull(saved)
        assertEquals("Vault", saved?.appDisplayName)
    }

    @Test
    fun testAddWhitelistedAppFailsOnBlankPackage() = runTest {
        val result = addWhitelistedAppUseCase(
            packageName = "   ",
            appDisplayName = "Invalid"
        )
        assertFalse(result)
        assertEquals(2, fakeRepository.getAllRules().first().size)
    }

    @Test
    fun testRemoveWhitelistedAppDeletesFromRepository() = runTest {
        removeWhitelistedAppUseCase("com.google.android.apps.authenticator2")
        val rule = fakeRepository.getRuleForPackage("com.google.android.apps.authenticator2")
        assertNull(rule)
        assertEquals(1, fakeRepository.getAllRules().first().size)
    }

    private class InMemoryInterventionRuleRepository(
        initialRules: List<InterventionRule> = emptyList()
    ) : InterventionRuleRepository {
        private val rulesFlow = MutableStateFlow(initialRules)

        override fun getAllRules(): Flow<List<InterventionRule>> = rulesFlow

        override fun getWhitelistedRules(): Flow<List<InterventionRule>> =
            rulesFlow.map { list -> list.filter { !it.isStrictBlocked } }

        override fun getBlockedAppRules(): Flow<List<InterventionRule>> =
            rulesFlow.map { list -> list.filter { it.isStrictBlocked } }

        override suspend fun getRuleForPackage(packageName: String): InterventionRule? =
            rulesFlow.value.find { it.packageName.equals(packageName, ignoreCase = true) }

        override suspend fun saveRule(rule: InterventionRule) {
            val current = rulesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.packageName.equals(rule.packageName, ignoreCase = true) }
            if (index >= 0) {
                current[index] = rule
            } else {
                current.add(rule)
            }
            rulesFlow.value = current
        }

        override suspend fun deleteRule(packageName: String) {
            rulesFlow.value = rulesFlow.value.filterNot { it.packageName.equals(packageName, ignoreCase = true) }
        }
    }
}
