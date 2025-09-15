package com.danieljm.delijn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.danieljm.delijn.data.local.entities.BusEntity

/** DAO for cached buses & details (Room) */
@Dao
interface BusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBus(bus: BusEntity)

    @Query("SELECT * FROM buses WHERE id = :id LIMIT 1")
    suspend fun getBusById(id: String): BusEntity?
}
