package com.danieljm.delijn.domain.repository

import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.model.Stop

interface StopRepository {
    suspend fun searchStops(query: String): List<Stop>
    suspend fun getStopDetails(stopId: String): Stop?
    suspend fun getRealTimeArrivals(stopId: String): List<RealTimeDto>
    suspend fun getNearbyStops(latitude: Double, longitude: Double): List<Stop>
    suspend fun getCachedStops(): List<Stop>
}
