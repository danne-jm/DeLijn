package com.danieljm.delijn.data.repository

import android.util.Log
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.local.entities.StopEntity
import com.danieljm.delijn.data.mapper.NearbyStopMapper
import com.danieljm.delijn.data.mapper.StopMapper
import com.danieljm.delijn.data.mapper.toDomain
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.domain.model.LineDirectionsResponse
import com.danieljm.delijn.domain.model.LineDirectionsSearchResponse
import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository
import com.danieljm.delijn.data.remote.dto.OsrmRouteRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Implementation of StopRepository (wires local + remote). */
class StopRepositoryImpl(
    private val api: DeLijnApiService,
    private val dao: StopDao
) : StopRepository {

    // Simple in-memory TTL cache for route geometries
    private data class CacheEntry(val ts: Long, val coords: List<Pair<Double, Double>>)
    private val routeCache = ConcurrentHashMap<String, CacheEntry>()
    private val ROUTE_CACHE_TTL_MS = 1000L * 60 * 60 // 1 hour
    private val ROUTE_CACHE_MAX = 300

    private fun makeRouteCacheKey(coordsLatLon: List<Pair<Double, Double>>,
                                  alternatives: Boolean = false,
                                  geometries: String = "geojson",
                                  overview: String = "full",
                                  steps: Boolean = false): String {
        // Normalize and round to 6 decimals
        val sb = StringBuilder()
        coordsLatLon.forEach { (lat, lon) ->
            sb.append(String.format("%.6f,%.6f;", lat, lon))
        }
        sb.append("alts=").append(if (alternatives) "1" else "0")
        sb.append("|geom=").append(geometries)
        sb.append("|ov=").append(overview)
        sb.append("|steps=").append(if (steps) "1" else "0")
        return sb.toString()
    }

    private fun getCachedRoute(key: String): List<Pair<Double, Double>>? {
        val entry = routeCache[key] ?: return null
        if (System.currentTimeMillis() - entry.ts > ROUTE_CACHE_TTL_MS) {
            routeCache.remove(key)
            return null
        }
        return entry.coords
    }

    private fun putCachedRoute(key: String, coords: List<Pair<Double, Double>>) {
        if (routeCache.size >= ROUTE_CACHE_MAX) {
            // remove oldest entry (not strictly LRU, but keeps bounded size)
            val oldestKey = routeCache.entries.minByOrNull { it.value.ts }?.key
            if (oldestKey != null) routeCache.remove(oldestKey)
        }
        routeCache[key] = CacheEntry(System.currentTimeMillis(), coords)
    }

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

    override suspend fun getNearbyStops(latitude: Double, longitude: Double): List<Stop> {
        Log.d("StopRepository", "Fetching nearby stops for coordinates: lat=$latitude, lon=$longitude")
        try {
            val response = api.getNearbyStops(latitude, longitude)
            Log.d("StopRepository", "API response received: ${'$'}{response.haltes.size} stops found")
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
                Log.d("StopRepository", "Inserted ${'$'}{entities.size} stops into local DB")
            } catch (dbEx: Exception) {
                Log.e("StopRepository", "Failed to insert stops into DB", dbEx)
            }

            Log.d("StopRepository", "Successfully mapped ${'$'}{mappedStops.size} stops")
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

    override suspend fun searchLineDirections(zoekArgument: String, maxAantalHits: Int): LineDirectionsSearchResponse {
        val response = api.searchLineDirections(zoekArgument, maxAantalHits)
        return response.toDomain()
    }

    override suspend fun getLineDirectionStops(entiteitnummer: String, lijnnummer: String, richting: String): com.danieljm.delijn.domain.model.LineDirectionStopsResponse {
        val dto = api.getLineDirectionStops(entiteitnummer, lijnnummer, richting)
        return dto.toDomain()
    }

    override suspend fun getLineDirectionDetail(entiteitnummer: String, lijnnummer: String, richting: String): com.danieljm.delijn.domain.model.LineDirectionSearch? {
        return try {
            val dto = api.getLineDirectionDetail(entiteitnummer, lijnnummer, richting)
            dto.toDomain()
        } catch (e: Exception) {
            // If core API doesn't have details, return null
            null
        }
    }

    override suspend fun getRouteGeometry(coordinatesLatLon: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        if (coordinatesLatLon.size < 2) return null

        val cacheKey = makeRouteCacheKey(coordinatesLatLon)
        val cached = getCachedRoute(cacheKey)
        if (cached != null) return cached

        return try {
            // Convert to OSRM expected format: [lon, lat]
            val coordsLonLat = coordinatesLatLon.map { (lat, lon) -> listOf(lon, lat) }
            val request = OsrmRouteRequestDto(coordinates = coordsLonLat)
            val resp = withContext(Dispatchers.IO) { api.getRouteGeometry(request) }
            val routeCoords = resp.routes.firstOrNull()?.geometry?.coordinates
            val mapped = routeCoords?.map { (lon, lat) -> Pair(lat, lon) }
            if (mapped != null && mapped.isNotEmpty()) {
                putCachedRoute(cacheKey, mapped)
            }
            mapped
        } catch (e: Exception) {
            Log.w("StopRepository", "getRouteGeometry failed: ${'$'}{e.message}")
            null
        }
    }
}
