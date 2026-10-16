package com.personaltracker.domain.repository

import com.personaltracker.domain.model.StudySession
import kotlinx.coroutines.flow.Flow

/**
 * Pure Kotlin repository contract for Study sessions and focus reward commits (FR-2.5, FR-4.1, AD-1, AD-6).
 */
interface StudySessionRepository {
    fun getSessionsForDate(date: String): Flow<List<StudySession>>
    fun getRecentSessions(limit: Int = 50): Flow<List<StudySession>>
    fun getAllSessions(): Flow<List<StudySession>>
    suspend fun saveSession(session: StudySession)
    suspend fun getTotalStudyMinutesForDate(date: String): Int
    suspend fun recordSessionWithReward(
        session: StudySession,
        idempotencyKey: String
    )
}
