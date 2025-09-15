package com.danieljm.delijn.domain.repository

import com.danieljm.delijn.domain.model.Route

interface RouteRepository {
    suspend fun getRouteDetails(routeId: String): Route?
}

