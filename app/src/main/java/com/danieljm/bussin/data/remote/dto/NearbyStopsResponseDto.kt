package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NearbyStopsResponseDto(
    @param:Json(name = "haltes")
    val haltes: List<HalteDto>? = null
)
