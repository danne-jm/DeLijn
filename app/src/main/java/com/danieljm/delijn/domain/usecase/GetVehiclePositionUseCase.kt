package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.BusPositionDomain
import com.danieljm.delijn.domain.repository.VehiclePositionRepository

class GetVehiclePositionUseCase(private val repository: VehiclePositionRepository) {
    suspend operator fun invoke(vehicleId: String): BusPositionDomain? = repository.getVehiclePosition(vehicleId)
}

