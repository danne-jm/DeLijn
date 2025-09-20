package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.VehicleRoute
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetVehicleRouteUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(vehicleId: String, halteNumbers: String? = null, lijnnummer: String? = null, richting: String? = null): Result<VehicleRoute> {
        return repository.getVehicleRouteTyped(vehicleId, halteNumbers, lijnnummer, richting)
    }
}

