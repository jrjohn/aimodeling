package com.example.aimodel.di

import com.example.aimodel.data.repository.CachingDataRepository
import com.example.aimodel.data.repository.DataRepository
import com.example.aimodel.data.repository.OfflineFirstDataRepository
import com.example.aimodel.sync.Syncable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OfflineFirst

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Cached

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    companion object {
        /**
         * Provides the base OfflineFirstDataRepository (without caching)
         */
        @Provides
        @Singleton
        @OfflineFirst
        fun provideOfflineFirstDataRepository(
            offlineFirstDataRepository: OfflineFirstDataRepository
        ): DataRepository = offlineFirstDataRepository

        /**
         * Provides the cached version of the repository (wraps OfflineFirstDataRepository)
         */
        @Provides
        @Singleton
        @Cached
        fun provideCachedDataRepository(
            @OfflineFirst offlineFirstDataRepository: DataRepository
        ): DataRepository = CachingDataRepository(offlineFirstDataRepository)
    }

    /**
     * Binds the cached repository as the default DataRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindDataRepository(
        @Cached cachingDataRepository: DataRepository
    ): DataRepository

    /**
     * Binds OfflineFirstDataRepository into the set of Syncable components
     * Note: The underlying OfflineFirstDataRepository handles sync, not the caching wrapper
     */
    @Binds
    @IntoSet
    abstract fun bindDataRepositoryAsSyncable(
        offlineFirstDataRepository: OfflineFirstDataRepository
    ): Syncable
}
