package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LineSearchResponseDto(
    @param:Json(name = "lijnNummerPubliek")
    val lijnNummerPubliek: String? = null,
    @param:Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    @param:Json(name = "lijnnummer")
    val lijnnummer: String? = null,
    @param:Json(name = "richting")
    val richting: String? = null,
    @param:Json(name = "omschrijving")
    val omschrijving: String? = null,
    @param:Json(name = "kleurVoorGrond")
    val kleurVoorGrond: String? = null,
    @param:Json(name = "kleurAchterGrond")
    val kleurAchterGrond: String? = null,
    @param:Json(name = "kleurAchterGrondRand")
    val kleurAchterGrondRand: String? = null,
    @param:Json(name = "kleurVoorGrondRand")
    val kleurVoorGrondRand: String? = null
)
