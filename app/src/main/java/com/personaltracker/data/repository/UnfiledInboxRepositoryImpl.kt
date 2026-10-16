package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.UnfiledInboxDao
import com.personaltracker.domain.model.UnfiledInboxItem
import com.personaltracker.domain.repository.UnfiledInboxRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UnfiledInboxRepositoryImpl @Inject constructor(
    private val unfiledInboxDao: UnfiledInboxDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnfiledInboxRepository {

    override fun getAllInboxItems(): Flow<List<UnfiledInboxItem>> =
        unfiledInboxDao.getAllInboxItems()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getInboxItemById(id: String): UnfiledInboxItem? =
        withContext(ioDispatcher) {
            unfiledInboxDao.getInboxItemById(id)?.toDomain()
        }

    override suspend fun saveInboxItem(item: UnfiledInboxItem) =
        withContext(ioDispatcher) {
            unfiledInboxDao.insertInboxItem(item.toEntity())
        }

    override suspend fun deleteInboxItem(item: UnfiledInboxItem) =
        withContext(ioDispatcher) {
            unfiledInboxDao.deleteInboxItem(item.toEntity())
        }
}
