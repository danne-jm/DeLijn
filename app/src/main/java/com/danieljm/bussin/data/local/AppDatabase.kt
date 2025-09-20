package com.danieljm.bussin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.danieljm.bussin.data.local.dao.StopDao
import com.danieljm.bussin.data.local.dao.LineStopDao
import com.danieljm.bussin.data.local.entity.StopEntity
import com.danieljm.bussin.data.local.entity.LineStopEntity

@Database(entities = [StopEntity::class, LineStopEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao
    abstract fun lineStopDao(): LineStopDao
}
