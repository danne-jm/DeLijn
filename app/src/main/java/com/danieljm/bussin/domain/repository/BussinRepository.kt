package com.danieljm.bussin.domain.repository

import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.model.FinalSchedule
import com.danieljm.bussin.domain.model.LineDirection
import com.danieljm.bussin.domain.model.LineStop
import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.domain.model.VehiclePosition

/**
 * Domain-level repository contract. Keep this interface free of Android and network types.
 * For now many legacy methods return raw JSON as String for compatibility; new typed methods
 * return domain models and should be preferred by the UI/ViewModel layer.
 */
interface BussinRepository {
    // Legacy / raw JSON responses (kept for compatibility)
    suspend fun discovery(): Result<String>
    suspend fun getNearbyStops(stop: String, latitude: Double, longitude: Double, radius: Int?, maxAantalHaltes: Int?): Result<String>
    suspend fun getStopLines(stop: String): Result<String>
    suspend fun getStopArrivals(stop: String): Result<String>
    suspend fun getStopDetails(stop: String): Result<String>
    suspend fun getDienstregelingen(stop: String, datum: String): Result<String>
    suspend fun getFinalSchedule(stop: String, datum: String, maxAantalDoorkomsten: Int?): Result<String>
    suspend fun searchLines(query: String, maxAantalHits: Int?): Result<String>
    suspend fun getLineStops(lijnnummer: String, richting: String): Result<String>
    suspend fun getVehiclePosition(vehicleId: String): Result<String>
    suspend fun getVehicleRoute(vehicleId: String, halteNumbers: String?, lijnnummer: String?, richting: String?): Result<String>

    // New typed/domain-safe methods (return domain models)
    suspend fun getNearbyStopsTyped(stop: String, latitude: Double, longitude: Double, radius: Int?, maxAantalHaltes: Int?): Result<List<Stop>>
    suspend fun getStopDetailsTyped(stop: String): Result<Stop>

    // New typed methods for other endpoints
    suspend fun searchLinesTyped(query: String, maxAantalHits: Int?): Result<List<LineDirection>>
    suspend fun getLineStopsTyped(lijnnummer: String, richting: String): Result<List<LineStop>>
    suspend fun getStopArrivalsTyped(stop: String): Result<List<Arrival>>
    suspend fun getFinalScheduleTyped(stop: String, datum: String, maxAantalDoorkomsten: Int?): Result<FinalSchedule>
    suspend fun getVehiclePositionTyped(vehicleId: String): Result<VehiclePosition>
    suspend fun getVehicleRouteTyped(vehicleId: String, halteNumbers: String?, lijnnummer: String?, richting: String?): Result<com.danieljm.bussin.domain.model.VehicleRoute>

    // Local cached reads for nearby stops (bounding-box style)
    suspend fun getCachedNearbyStops(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): Result<List<Stop>>
}
