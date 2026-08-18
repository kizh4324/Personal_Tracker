package com.personaltracker.data.local.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personaltracker.data.local.db.entities.StudySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Study & Focus Sessions.
 * Declared internal to preserve data layer boundaries.
 */
@Dao
internal interface StudySessionDao {

    @Query("SELECT * FROM study_sessions WHERE sessionDate = :date ORDER BY startTime ASC")
    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY sessionDate DESC, startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY sessionDate DESC, startTime DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE sessionDate = :date")
    suspend fun getTotalStudyMinutesForDate(date: String): Int?
}
