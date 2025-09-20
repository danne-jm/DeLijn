package com.danieljm.bussin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danieljm.bussin.data.local.entity.LineStopEntity

@Dao
interface LineStopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<LineStopEntity>)

    @Query("SELECT * FROM line_stops ORDER BY haltenummer ASC")
    suspend fun getAll(): List<LineStopEntity>

    @Query("SELECT * FROM line_stops WHERE haltenummer = :id LIMIT 1")
    suspend fun getById(id: String): LineStopEntity?
}

