package com.danieljm.delijn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey val id: String,
    val line: String,
    val destination: String
)
