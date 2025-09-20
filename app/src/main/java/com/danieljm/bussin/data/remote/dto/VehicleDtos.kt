package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VehiclePositionDto(
    @param:Json(name = "vehicle")
    val vehicleWrapper: VehicleWrapperDto? = null
)

@JsonClass(generateAdapter = true)
data class VehicleWrapperDto(
    @param:Json(name = "vehicle")
    val vehicle: VehicleInnerDto? = null
)

@JsonClass(generateAdapter = true)
data class VehicleInnerDto(
    @param:Json(name = "vehicle")
    val id: String? = null,
    @param:Json(name = "position")
    val position: PositionDto? = null,
    @param:Json(name = "timestamp")
    val timestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class PositionDto(
    @param:Json(name = "latitude")
    val latitude: Double? = null,
    @param:Json(name = "longitude")
    val longitude: Double? = null,
    @param:Json(name = "bearing")
    val bearing: Double? = null
)

@JsonClass(generateAdapter = true)
data class VehicleRouteResponseDto(
    @param:Json(name = "vehicle")
    val vehicle: VehicleInnerDto? = null,
    @param:Json(name = "stops")
    val stops: List<HalteDto>? = null,
    @param:Json(name = "legs")
    val legs: List<LegDto>? = null
)

@JsonClass(generateAdapter = true)
data class LegDto(
    @param:Json(name = "from")
    val from: String? = null,
    @param:Json(name = "to")
    val to: String? = null
)
