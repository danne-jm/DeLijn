package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json

/** DTOs matching De Lijn real-time arrivals response */
data class RealTimeArrivalsResponseDto(
    val halteDoorkomsten: List<RealTimeHalteDoorkomstDto>
)

data class RealTimeHalteDoorkomstDto(
    val doorkomsten: List<RealTimeDoorkomstDto>
)

data class RealTimeDoorkomstDto(
    val lijnnummer: String,
    val bestemming: String,
    @Json(name = "real-timeTijdstip") val realTimeTijdstip: String? = null,
    val dienstregelingTijdstip: String? = null,
    val verwachtTijdstip: String? = null,
    @Json(name = "vrtnum") val vrtnum: String? = null // Bus id from API
)
