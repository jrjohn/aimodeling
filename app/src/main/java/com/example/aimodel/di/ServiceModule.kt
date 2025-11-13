package com.example.aimodel.di

import com.example.aimodel.domain.service.UserService
import com.example.aimodel.domain.service.UserServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindUserService(
        userServiceImpl: UserServiceImpl
    ): UserService
}