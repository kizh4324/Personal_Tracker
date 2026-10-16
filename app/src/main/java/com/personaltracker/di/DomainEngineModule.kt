package com.personaltracker.di

import com.personaltracker.domain.engine.AdaptiveScheduleEngine
import com.personaltracker.domain.engine.FocusRescheduleEngine
import com.personaltracker.domain.engine.GamificationLedgerEngine
import com.personaltracker.domain.engine.InterventionRuleEngine
import com.personaltracker.domain.engine.StreakStateMachine
import com.personaltracker.domain.parser.DeterministicNlpCommandParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module providing singleton pure Kotlin behavioral engines
 * and deterministic NLP parser (AD-1, AD-3, AD-8).
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
}
