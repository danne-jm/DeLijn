package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeoCoordinateDto(
    @param:Json(name = "latitude")
    val latitude: Double?,
    @param:Json(name = "longitude")
    val longitude: Double?
)
