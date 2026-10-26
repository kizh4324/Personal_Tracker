package com.personaltracker.di

import android.content.Context
import com.personaltracker.domain.alarm.TaskAlarmScheduler
import com.personaltracker.platform.alarm.ExactAlarmScheduler
import com.personaltracker.platform.alarm.TaskAlarmSchedulerImpl
import com.personaltracker.platform.voice.QuickCaptureVoiceRecorder
import com.personaltracker.platform.voice.SpeechRecognitionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module providing Android platform singletons (FR-4, FR-5, FR-7, AD-1).
 */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides
    @Singleton
    fun provideExactAlarmScheduler(
        @ApplicationContext context: Context
    ): ExactAlarmScheduler = ExactAlarmScheduler(context)

    @Provides
    @Singleton
    fun provideTaskAlarmScheduler(
        exactAlarmScheduler: ExactAlarmScheduler
    ): TaskAlarmScheduler = TaskAlarmSchedulerImpl(exactAlarmScheduler)

    @Provides
    @Singleton
    fun provideQuickCaptureVoiceRecorder(
        @ApplicationContext context: Context
    ): QuickCaptureVoiceRecorder = QuickCaptureVoiceRecorder(context)

    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(
        @ApplicationContext context: Context
    ): SpeechRecognitionManager = SpeechRecognitionManager(context)
}
