package com.danieljm.delijn.data.repository

import android.util.Log
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.local.entities.StopEntity
import com.danieljm.delijn.data.mapper.NearbyStopMapper
import com.danieljm.delijn.data.mapper.StopMapper
import com.danieljm.delijn.data.mapper.toDomain
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.model.LineDirectionsResponse
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

            // Persist mapped stops into local database for quick access next time
            try {
                val entities = mappedStops.map { stop ->
                    StopEntity(
                        id = stop.id,
                        name = stop.name,
                        latitude = stop.latitude,
                        longitude = stop.longitude,
                        entiteitnummer = stop.entiteitnummer,
                        halteNummer = stop.halteNummer
                    )
                }
                dao.insertStops(entities)
                Log.d("StopRepository", "Inserted ${entities.size} stops into local DB")
            } catch (dbEx: Exception) {
                Log.e("StopRepository", "Failed to insert stops into DB", dbEx)
            }

            Log.d("StopRepository", "Successfully mapped ${mappedStops.size} stops")
            return mappedStops
        } catch (e: Exception) {
            Log.e("StopRepository", "Error fetching nearby stops", e)
            throw e
        }
    }

    override suspend fun getCachedStops(): List<Stop> {
        // Read all stops from local DB and map to domain
        val entities = dao.getAllStops()
        return entities.map { StopMapper.entityToDomain(it) }
    }

    override suspend fun getLineDirectionsForStop(entiteitnummer: String, haltenummer: String): LineDirectionsResponse {
        val response = api.getLineDirectionsForStop(entiteitnummer, haltenummer)
        return response.toDomain()
    }
}
