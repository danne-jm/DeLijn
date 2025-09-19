package com.danieljm.delijn.data.repository

import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.domain.model.BusPositionDomain
import com.danieljm.delijn.domain.repository.VehiclePositionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VehiclePositionRepositoryImpl(private val api: DeLijnApiService) : VehiclePositionRepository {
    override suspend fun getVehiclePosition(vehicleId: String): BusPositionDomain? = withContext(Dispatchers.IO) {
        try {
            val responseBody = api.getGtfsRealtime(vehicleId = vehicleId)
            val response = responseBody.string()
            val json = JSONObject(response)
            val entities = json.optJSONArray("entity") ?: return@withContext null
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                if (entity.has("vehicle")) {
                    val vehicle = entity.getJSONObject("vehicle")
                    val position = vehicle.optJSONObject("position") ?: continue
                    val lat = position.optDouble("latitude")
                    val lon = position.optDouble("longitude")
                    val bearing = position.optDouble("bearing", 0.0).toFloat()
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return@withContext BusPositionDomain(vehicleId, lat, lon, bearing)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

