package com.personaltracker.domain.repository

import com.personaltracker.domain.model.UnfiledInboxItem
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for unfiled capture items (FR-7.5, AD-1, AD-8).
 */
interface UnfiledInboxRepository {
    fun getAllInboxItems(): Flow<List<UnfiledInboxItem>>
    suspend fun getInboxItemById(id: String): UnfiledInboxItem?
    suspend fun saveInboxItem(item: UnfiledInboxItem)
    suspend fun deleteInboxItem(item: UnfiledInboxItem)
}
