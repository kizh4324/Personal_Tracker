package com.personaltracker.di

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.InterventionRuleEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.validation.CommandValidationPipeline
import com.personaltracker.domain.validation.RelationalIntegrityValidator
import com.personaltracker.domain.validation.TaskDeduplicationValidator
import com.personaltracker.domain.validation.TemporalSafetyValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module providing singleton pure Kotlin behavioral engines,
 * deterministic NLP parser, and command validation pipeline (AD-1, AD-3, AD-8).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DomainEngineModule {

    @Provides
    @Singleton
    fun provideAdaptiveScheduleEngine(): AdaptiveScheduleEngine {
        return AdaptiveScheduleEngine()
    }

    @Provides
    @Singleton
    fun provideStreakStateMachine(): StreakStateMachine {
        return StreakStateMachine()
    }

    @Provides
    @Singleton
    fun provideGamificationLedgerEngine(): GamificationLedgerEngine {
        return GamificationLedgerEngine()
    }

    @Provides
    @Singleton
    fun provideFocusRescheduleEngine(): FocusRescheduleEngine {
        return FocusRescheduleEngine()
    }

    @Provides
    @Singleton
    fun provideInterventionRuleEngine(): InterventionRuleEngine {
        return InterventionRuleEngine()
    }

    @Provides
    @Singleton
    fun provideDeterministicNlpCommandParser(): DeterministicNlpCommandParser {
        return DeterministicNlpCommandParser()
    }

    @Provides
    @Singleton
    fun provideTemporalSafetyValidator(): TemporalSafetyValidator {
        return TemporalSafetyValidator()
    }

    @Provides
    @Singleton
    fun provideRelationalIntegrityValidator(
        habitRepository: HabitRepository,
        routineRepository: RoutineRepository
    ): RelationalIntegrityValidator {
        return RelationalIntegrityValidator(habitRepository, routineRepository)
    }

    @Provides
    @Singleton
    fun provideTaskDeduplicationValidator(
        taskRepository: TaskRepository
    ): TaskDeduplicationValidator {
        return TaskDeduplicationValidator(taskRepository)
    }

    @Provides
    @Singleton
    fun provideCommandValidationPipeline(
        temporalSafetyValidator: TemporalSafetyValidator,
        relationalIntegrityValidator: RelationalIntegrityValidator,
        taskDeduplicationValidator: TaskDeduplicationValidator
    ): CommandValidationPipeline {
        return CommandValidationPipeline(
            temporalSafetyValidator,
            relationalIntegrityValidator,
            taskDeduplicationValidator
        )
    }
}
