package com.danieljm.delijn.data.remote.dto

/** DTO representing a Stop from the network */
data class StopDto(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

