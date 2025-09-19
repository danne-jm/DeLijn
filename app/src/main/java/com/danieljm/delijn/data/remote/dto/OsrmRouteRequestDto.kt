package com.danieljm.delijn.data.remote.dto

/**
 * Request DTO sent to backend OSRM proxy. Coordinates should be [lon, lat] pairs.
 */
data class OsrmRouteRequestDto(
    val coordinates: List<List<Double>>, // [[lon, lat], [lon, lat], ...]
    val alternatives: Boolean = false,
    val geometries: String = "geojson",
    val overview: String = "full",
    val steps: Boolean = false
)

