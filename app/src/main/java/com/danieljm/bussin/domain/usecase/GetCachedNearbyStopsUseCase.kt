package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

/**
 * Return cached nearby stops from the local DB using a simple bounding box.
 */
class GetCachedNearbyStopsUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Result<List<Stop>> {
        return repository.getCachedNearbyStops(minLat, maxLat, minLon, maxLon)
    }
}

