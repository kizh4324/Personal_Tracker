package com.personaltracker.platform.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.personaltracker.data.repository.TransactionRunner
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.usecase.ResolveDayTypeUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker scheduled periodically at midnight boundaries (FR-3, FR-8.5, AD-1, AD-4).
 * Evaluates uncompleted habits using [StreakStateMachine] with slack grace and freeze preservation,
 * and resolves today's active DayType.
 */
class DayRolloverWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RolloverEntryPoint {
        fun habitRepository(): HabitRepository
        fun dayTypeRepository(): DayTypeRepository
        fun streakStateMachine(): StreakStateMachine
        fun resolveDayTypeUseCase(): ResolveDayTypeUseCase
        fun transactionRunner(): TransactionRunner
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                RolloverEntryPoint::class.java
            )

            val coordinator = DayRolloverCoordinator(
                habitRepository = entryPoint.habitRepository(),
                dayTypeRepository = entryPoint.dayTypeRepository(),
                streakStateMachine = entryPoint.streakStateMachine(),
                resolveDayTypeUseCase = entryPoint.resolveDayTypeUseCase(),
                transactionRunner = entryPoint.transactionRunner()
            )

            coordinator.processRollover()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "day_rollover_periodic_work"

        /**
         * Calculates initial delay in seconds to the target rollover time (defaults to midnight 00:00).
         */
        fun calculateInitialDelayToMidnight(
            nowTime: LocalTime = LocalTime.now(),
            targetTime: LocalTime = LocalTime.MIDNIGHT
        ): Long {
            val nowSeconds = nowTime.toSecondOfDay()
            val targetSeconds = targetTime.toSecondOfDay()
            val diffSeconds = if (targetSeconds > nowSeconds) {
                targetSeconds - nowSeconds
            } else {
                (24 * 3600) - nowSeconds + targetSeconds
            }
            return diffSeconds.toLong()
        }

        /**
         * Schedules periodic daily rollover execution with exponential backoff on failure.
         */
        fun enqueuePeriodic(context: Context) {
            val initialDelaySeconds = calculateInitialDelayToMidnight()
            val workRequest = PeriodicWorkRequestBuilder<DayRolloverWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
