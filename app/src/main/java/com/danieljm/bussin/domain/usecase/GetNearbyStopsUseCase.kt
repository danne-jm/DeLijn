package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetNearbyStopsUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(stop: String, latitude: Double, longitude: Double, radius: Int?, maxAantalHaltes: Int?): Result<List<Stop>> {
        return repository.getNearbyStopsTyped(stop, latitude, longitude, radius, maxAantalHaltes)
    }
}

