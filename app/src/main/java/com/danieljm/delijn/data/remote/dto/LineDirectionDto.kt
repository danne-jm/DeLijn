package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LineDirectionDto(
    @Json(name = "entiteitnummer")
    val entiteitnummer: String,
    @Json(name = "lijnnummer")
    val lijnnummer: String,
    @Json(name = "richting")
    val richting: String,
    @Json(name = "omschrijving")
    val omschrijving: String
)

@JsonClass(generateAdapter = true)
data class LineDirectionsResponseDto(
    @Json(name = "lijnrichtingen")
    val lijnrichtingen: List<LineDirectionDto>
)
