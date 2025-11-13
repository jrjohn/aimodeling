package com.example.aimodel.di

import com.example.aimodel.core.analytics.AnalyticsTracker
import com.example.aimodel.core.analytics.LoggingAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        loggingAnalyticsTracker: LoggingAnalyticsTracker
    ): AnalyticsTracker
}
