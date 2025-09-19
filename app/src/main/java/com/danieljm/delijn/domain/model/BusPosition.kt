package com.danieljm.delijn.domain.model

data class BusPositionDomain(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float = 0f
)

