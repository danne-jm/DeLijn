package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json

data class LineDirectionStopsResponseDto(
    @Json(name = "haltes") val haltes: List<LineDirectionStopDto>
)

data class LineDirectionStopDto(
    // API may return 'haltenummer' (all lowercase). Make this optional to avoid parsing errors when missing.
    @Json(name = "haltenummer") val halteNummer: String?,
    @Json(name = "omschrijving") val omschrijving: String,
    @Json(name = "geoCoordinaat") val geoCoordinaat: GeoCoordinaatDto
)



