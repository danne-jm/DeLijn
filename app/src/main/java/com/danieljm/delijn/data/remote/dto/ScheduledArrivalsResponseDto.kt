package com.danieljm.delijn.data.remote.dto

/** DTOs matching De Lijn scheduled arrivals (dienstregelingen) response */
data class ScheduledArrivalsResponseDto(
    val halteDoorkomsten: List<ScheduledHalteDoorkomstDto>
)

data class ScheduledHalteDoorkomstDto(
    val doorkomsten: List<ScheduledDoorkomstDto>
)

data class ScheduledDoorkomstDto(
    val lijnnummer: String,
    val bestemming: String,
    val dienstregelingTijdstip: String
)

