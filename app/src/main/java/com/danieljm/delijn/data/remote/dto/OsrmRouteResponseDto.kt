package com.danieljm.delijn.data.remote.dto

/**
 * Minimal OSRM response DTO expected from backend proxy.
 * Structure (GeoJSON): { routes: [ { geometry: { coordinates: [[lon, lat], ...] } } ] }
 */
data class OsrmRouteResponseDto(
    val routes: List<OsrmRouteDto> = emptyList()
)

data class OsrmRouteDto(
    val geometry: OsrmGeometryDto? = null
)

data class OsrmGeometryDto(
    val coordinates: List<List<Double>> = emptyList()
)

