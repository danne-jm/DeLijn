package com.danieljm.delijn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.local.entities.BusEntity
import com.danieljm.delijn.data.local.entities.RouteEntity
import com.danieljm.delijn.data.local.entities.StopEntity

@Database(entities = [StopEntity::class, BusEntity::class, RouteEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao
    abstract fun busDao(): BusDao
    abstract fun routeDao(): RouteDao
}
