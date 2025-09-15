package com.danieljm.delijn.di

import android.content.Context
import androidx.room.Room
import com.danieljm.delijn.data.local.AppDatabase
import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.local.dao.StopDao

object DatabaseModule {
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "delijn_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    fun provideStopDao(database: AppDatabase): StopDao = database.stopDao()
    fun provideBusDao(database: AppDatabase): BusDao = database.busDao()
    fun provideRouteDao(database: AppDatabase): RouteDao = database.routeDao()
}
