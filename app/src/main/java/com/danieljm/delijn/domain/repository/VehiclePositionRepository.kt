package com.danieljm.delijn.domain.repository

import com.danieljm.delijn.domain.model.BusPositionDomain

interface VehiclePositionRepository {
    suspend fun getVehiclePosition(vehicleId: String): BusPositionDomain?
}

