package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.repository.StopRepository

class GetRouteGeometryUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(coordinatesLatLon: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        return stopRepository.getRouteGeometry(coordinatesLatLon)
    }
}

