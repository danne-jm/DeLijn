package com.danieljm.delijn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

//Route Entity
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stops: List<String>
)
