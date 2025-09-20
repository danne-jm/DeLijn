package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FinalScheduleResponseDto(
    @param:Json(name = "halteDoorkomsten")
    val halteDoorkomsten: List<HalteDoorkomstenDto>? = null,
    @param:Json(name = "doorkomstNotas")
    val doorkomstNotas: List<String>? = null,
    @param:Json(name = "ritNotas")
    val ritNotas: List<String>? = null,
    @param:Json(name = "omleidingen")
    val omleidingen: List<String>? = null
)
