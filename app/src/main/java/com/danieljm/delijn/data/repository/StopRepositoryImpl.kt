package com.danieljm.delijn.data.repository

import android.util.Log
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.mapper.NearbyStopMapper
import com.danieljm.delijn.data.mapper.StopMapper
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository

/** Implementation of StopRepository (wires local + remote). */
class StopRepositoryImpl(
    private val api: DeLijnApiService,
    private val dao: StopDao
) : StopRepository {
    override suspend fun searchStops(query: String): List<Stop> {
        val dtos = api.searchStops(query)
        val entities = dtos.map { StopMapper.dtoToEntity(it) }
        dao.insertStops(entities)
        return entities.map { StopMapper.entityToDomain(it) }
    }

    override suspend fun getStopDetails(stopId: String): Stop? {
        val cached = dao.getStopById(stopId)
        if (cached != null) return StopMapper.entityToDomain(cached)
        val dto = api.getStop(stopId)
        val entity = StopMapper.dtoToEntity(dto)
        dao.insertStops(listOf(entity))
        return StopMapper.entityToDomain(entity)
    }

    override suspend fun getRealTimeArrivals(stopId: String): List<RealTimeDto> {
        // Forward to network realtime endpoint. No caching for realtime data.
        return api.getRealTimeForStop(stopId)
    }

    override suspend fun getNearbyStops(latitude: Double, longitude: Double): List<Stop> {
        Log.d("StopRepository", "Fetching nearby stops for coordinates: lat=$latitude, lon=$longitude")
        try {
            val response = api.getNearbyStops(latitude, longitude)
            Log.d("StopRepository", "API response received: ${response.haltes.size} stops found")
            val mappedStops = response.haltes.map { NearbyStopMapper.dtoToDomain(it) }
            Log.d("StopRepository", "Successfully mapped ${mappedStops.size} stops")
            return mappedStops
        } catch (e: Exception) {
            Log.e("StopRepository", "Error fetching nearby stops", e)
            throw e
        }
    }
}
