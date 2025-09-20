package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.VehiclePosition
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetVehiclePositionUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<VehiclePosition> {
        return repository.getVehiclePositionTyped(vehicleId)
    }
}

