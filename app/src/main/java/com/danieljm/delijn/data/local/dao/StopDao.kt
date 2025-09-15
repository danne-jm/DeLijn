package com.danieljm.delijn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danieljm.delijn.data.local.entities.StopEntity

/** DAO for cached searched stops (Room) */
@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Query("SELECT * FROM stops WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<StopEntity>

    @Query("SELECT * FROM stops WHERE id = :id LIMIT 1")
    suspend fun getStopById(id: String): StopEntity?
}
