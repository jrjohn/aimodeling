package com.example.aimodel.di

import com.example.aimodel.sync.SyncManager
import com.example.aimodel.sync.Synchronizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSynchronizer(
        syncManager: SyncManager
    ): Synchronizer
}
