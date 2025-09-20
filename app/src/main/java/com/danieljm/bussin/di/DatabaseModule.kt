package com.danieljm.bussin.di

import android.content.Context
import androidx.room.Room
import com.danieljm.bussin.data.local.AppDatabase
import com.danieljm.bussin.data.local.dao.StopDao
import com.danieljm.bussin.data.local.dao.LineStopDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "bussin_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStopDao(db: AppDatabase): StopDao = db.stopDao()

    @Provides
    @Singleton
    fun provideLineStopDao(db: AppDatabase): LineStopDao = db.lineStopDao()
}
