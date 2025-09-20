package com.danieljm.bussin.domain.model

data class VehiclePosition(
    val id: String?,
    val vehicleId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val bearing: Double?,
    val timestamp: Long?
)

