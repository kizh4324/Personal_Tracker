package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.InterventionRuleDao
import com.personaltracker.domain.model.InterventionRule
import com.personaltracker.domain.repository.InterventionRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InterventionRuleRepositoryImpl @Inject constructor(
    private val interventionRuleDao: InterventionRuleDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InterventionRuleRepository {

    override fun getBlockedAppRules(): Flow<List<InterventionRule>> =
        interventionRuleDao.getBlockedAppRules()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getRuleForPackage(packageName: String): InterventionRule? =
        withContext(ioDispatcher) {
            interventionRuleDao.getRuleForPackage(packageName)?.toDomain()
        }

    override suspend fun saveRule(rule: InterventionRule) =
        withContext(ioDispatcher) {
            interventionRuleDao.insertOrUpdateRule(rule.toEntity())
        }

    override suspend fun deleteRule(packageName: String) =
        withContext(ioDispatcher) {
            interventionRuleDao.deleteRule(packageName)
        }
}
