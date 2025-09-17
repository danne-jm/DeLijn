package com.danieljm.delijn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Stop Entity
@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val entiteitnummer: String,
    val halteNummer: String
)
