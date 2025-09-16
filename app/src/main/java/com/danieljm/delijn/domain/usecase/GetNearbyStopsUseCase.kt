package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository

class GetNearbyStopsUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): List<Stop> {
        return stopRepository.getNearbyStops(latitude, longitude)
    }
}
