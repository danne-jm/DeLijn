package com.danieljm.bussin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danieljm.bussin.data.local.entity.StopEntity

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)

    @Query("SELECT * FROM stops ORDER BY distance ASC")
    suspend fun getAll(): List<StopEntity>

    @Query("SELECT * FROM stops WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StopEntity?
}

