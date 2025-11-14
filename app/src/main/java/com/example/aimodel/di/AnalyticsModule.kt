package com.example.aimodel.di

import com.example.aimodel.core.analytics.AnalyticsTracker
import com.example.aimodel.core.analytics.PersistentAnalyticsTracker
import com.example.aimodel.data.network.AnalyticsApiService
import com.example.aimodel.data.network.createAnalyticsApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.jensklingenberg.ktorfit.Ktorfit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        persistentAnalyticsTracker: PersistentAnalyticsTracker
    ): AnalyticsTracker

    companion object {
        @Provides
        @Singleton
        fun provideAnalyticsApiService(ktorfit: Ktorfit): AnalyticsApiService {
            return ktorfit.createAnalyticsApiService()
        }
    }
}
