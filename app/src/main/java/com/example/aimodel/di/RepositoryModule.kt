package com.example.aimodel.di

import com.example.aimodel.data.repository.DataRepository
import com.example.aimodel.data.repository.OfflineFirstDataRepository
import com.example.aimodel.sync.Syncable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDataRepository(
        offlineFirstDataRepository: OfflineFirstDataRepository
    ): DataRepository

    @Binds
    @IntoSet
    abstract fun bindDataRepositoryAsSyncable(
        offlineFirstDataRepository: OfflineFirstDataRepository
    ): Syncable
}
