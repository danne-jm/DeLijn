package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Route
import com.danieljm.delijn.domain.repository.RouteRepository

class GetRouteDetailsUseCase(private val repository: RouteRepository) {
    suspend operator fun invoke(routeId: String): Route? {
        return repository.getRouteDetails(routeId)
    }
}

