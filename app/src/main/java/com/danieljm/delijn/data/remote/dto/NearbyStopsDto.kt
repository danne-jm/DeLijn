package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NearbyStopsResponseDto(
    @Json(name = "haltes") val haltes: List<NearbyStopDto>,
    @Json(name = "links") val links: List<LinkDto>? = null
)

@JsonClass(generateAdapter = true)
data class NearbyStopDto(
    @Json(name = "type") val type: String,
    @Json(name = "id") val id: String,
    @Json(name = "naam") val naam: String?,
    @Json(name = "afstand") val afstand: Int,
    @Json(name = "geoCoordinaat") val geoCoordinaat: GeoCoordinaatDto,
    @Json(name = "links") val links: List<LinkDto>? = null,
    @Json(name = "entiteitnummer") val entiteitnummer: String? = null,
    @Json(name = "haltenummer") val haltenummer: String? = null
)

@JsonClass(generateAdapter = true)
data class GeoCoordinaatDto(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

@JsonClass(generateAdapter = true)
data class LinkDto(
    @Json(name = "rel") val rel: String,
    @Json(name = "url") val url: String
)
