package com.personaltracker.di

import com.personaltracker.data.repository.DayTypeRepositoryImpl
import com.personaltracker.data.repository.EconomyRepositoryImpl
import com.personaltracker.data.repository.HabitRepositoryImpl
import com.personaltracker.data.repository.InterventionRuleRepositoryImpl
import com.personaltracker.data.repository.RoutineRepositoryImpl
import com.personaltracker.data.repository.StudySessionRepositoryImpl
import com.personaltracker.data.repository.TaskRepositoryImpl
import com.personaltracker.data.repository.UnfiledInboxRepositoryImpl
import com.personaltracker.domain.repository.DayTypeRepository
import com.personaltracker.domain.repository.EconomyRepository
import com.personaltracker.domain.repository.HabitRepository
import com.personaltracker.domain.repository.InterventionRuleRepository
import com.personaltracker.domain.repository.RoutineRepository
import com.personaltracker.domain.repository.StudySessionRepository
import com.personaltracker.domain.repository.TaskRepository
import com.personaltracker.domain.repository.UnfiledInboxRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module binding all 8 pure domain repository interfaces
 * to their Flow-based Single Source of Truth (SSOT) data layer implementations (AD-1).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDayTypeRepository(
        impl: DayTypeRepositoryImpl
    ): DayTypeRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(
        impl: RoutineRepositoryImpl
    ): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(
        impl: HabitRepositoryImpl
    ): HabitRepository

    @Binds
    @Singleton
    abstract fun bindStudySessionRepository(
        impl: StudySessionRepositoryImpl
    ): StudySessionRepository

    @Binds
    @Singleton
    abstract fun bindEconomyRepository(
        impl: EconomyRepositoryImpl
    ): EconomyRepository

    @Binds
    @Singleton
    abstract fun bindUnfiledInboxRepository(
        impl: UnfiledInboxRepositoryImpl
    ): UnfiledInboxRepository

    @Binds
    @Singleton
    abstract fun bindInterventionRuleRepository(
        impl: InterventionRuleRepositoryImpl
    ): InterventionRuleRepository
}
