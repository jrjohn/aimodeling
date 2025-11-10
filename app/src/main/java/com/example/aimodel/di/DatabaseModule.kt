package com.example.aimodel.di

import android.content.Context
import androidx.room.Room
import com.example.aimodel.data.local.AppDatabase
import com.example.aimodel.data.local.UserChangeDao
import com.example.aimodel.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aimodel.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideUserChangeDao(appDatabase: AppDatabase): UserChangeDao {
        return appDatabase.userChangeDao()
    }
}
