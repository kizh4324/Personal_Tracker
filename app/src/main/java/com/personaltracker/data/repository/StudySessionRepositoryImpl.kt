package com.personaltracker.data.repository

import com.personaltracker.data.local.db.daos.CoinLedgerDao
import com.personaltracker.data.local.db.daos.StudySessionDao
import com.personaltracker.data.local.db.entities.CoinLedgerEntity
import com.personaltracker.domain.model.StudySession
import com.personaltracker.domain.repository.StudySessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StudySessionRepositoryImpl @Inject constructor(
    private val studySessionDao: StudySessionDao,
    private val coinLedgerDao: CoinLedgerDao,
    private val transactionRunner: TransactionRunner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StudySessionRepository {

    override fun getSessionsForDate(date: String): Flow<List<StudySession>> =
        studySessionDao.getSessionsForDate(date)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getRecentSessions(limit: Int): Flow<List<StudySession>> =
        studySessionDao.getRecentSessions(limit)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getAllSessions(): Flow<List<StudySession>> =
        studySessionDao.getAllSessions()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun saveSession(session: StudySession) =
        withContext(ioDispatcher) {
            studySessionDao.insertSession(session.toEntity())
        }

    override suspend fun getTotalStudyMinutesForDate(date: String): Int =
        withContext(ioDispatcher) {
            studySessionDao.getTotalStudyMinutesForDate(date) ?: 0
        }

    override suspend fun recordSessionWithReward(
        session: StudySession,
        idempotencyKey: String
    ) = withContext(ioDispatcher) {
        IdempotencyValidator.validate(idempotencyKey)
        require(session.coinsEarned >= 0) { "coinsEarned cannot be negative: ${session.coinsEarned}" }
        require(session.durationMinutes >= 0) { "durationMinutes cannot be negative: ${session.durationMinutes}" }
        require(session.createdTimestamp > 0) { "createdTimestamp must be positive: ${session.createdTimestamp}" }
        transactionRunner {
            val existing = coinLedgerDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                throw IllegalStateException("Duplicate coin transaction idempotency key: $idempotencyKey")
            }

            studySessionDao.insertSession(session.toEntity())

            val currentBalance = coinLedgerDao.getLatestBalance() ?: 0
            val newBalance = try {
                Math.addExact(currentBalance, session.coinsEarned)
            } catch (_: ArithmeticException) {
                throw IllegalStateException("Coin balance arithmetic overflow")
            }

            val ledgerEntry = CoinLedgerEntity(
                id = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                deltaAmount = session.coinsEarned,
                eventType = "STUDY_SESSION",
                sourceEntityId = session.id,
                balanceAfter = newBalance,
                timestamp = session.createdTimestamp
            )
            coinLedgerDao.insertTransaction(ledgerEntry)
        }
    }
}
