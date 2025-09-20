package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DoorkomstDto(
    @param:Json(name = "doorkomstId")
    val doorkomstId: String? = null,
    @param:Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    @param:Json(name = "lijnnummer")
    val lijnnummer: String? = null,
    @param:Json(name = "richting")
    val richting: String? = null,
    @param:Json(name = "ritnummer")
    val ritnummer: String? = null,
    @param:Json(name = "bestemming")
    val bestemming: String? = null,
    @param:Json(name = "plaatsBestemming")
    val plaatsBestemming: String? = null,
    @param:Json(name = "dienstregelingTijdstip")
    val dienstregelingTijdstip: String? = null,
    @param:Json(name = "real-timeTijdstip")
    val realTimeTijdstip: String? = null,
    @param:Json(name = "vias")
    val vias: List<String>? = null,
    @param:Json(name = "vrtnum")
    val vrtnum: String? = null,
    @param:Json(name = "predictionStatussen")
    val predictionStatussen: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class HalteDoorkomstenDto(
    @param:Json(name = "haltenummer")
    val haltenummer: String,
    @param:Json(name = "doorkomsten")
    val doorkomsten: List<DoorkomstDto>? = null
)

@JsonClass(generateAdapter = true)
data class ArrivalsResponseDto(
    @param:Json(name = "halteDoorkomsten")
    val halteDoorkomsten: List<HalteDoorkomstenDto>? = null
)
