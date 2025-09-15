package com.danieljm.delijn.data.repository

import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.mapper.RouteMapper
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.domain.model.Route
import com.danieljm.delijn.domain.repository.RouteRepository

/** Implementation of RouteRepository (wires local + remote). */
class RouteRepositoryImpl(
    private val api: DeLijnApiService,
    private val dao: RouteDao
) : RouteRepository {
    override suspend fun getRouteDetails(routeId: String): Route? {
        val cached = dao.getRouteById(routeId)
        if (cached != null) return RouteMapper.entityToDomain(cached)
        val dto = api.getRoute(routeId)
        val entity = RouteMapper.dtoToEntity(dto)
        dao.insertRoute(entity)
        return RouteMapper.entityToDomain(entity)
    }
}
