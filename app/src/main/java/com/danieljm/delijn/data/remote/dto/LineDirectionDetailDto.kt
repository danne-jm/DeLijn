package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LineDirectionDetailDto(
    @Json(name = "lijnNummerPubliek")
    val lijnNummerPubliek: String? = null,
    @Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    @Json(name = "lijnnummer")
    val lijnnummer: String? = null,
    @Json(name = "richting")
    val richting: String? = null,
    @Json(name = "omschrijving")
    val omschrijving: String? = null,
    @Json(name = "kleurVoorGrond")
    val kleurVoorGrond: String? = null,
    @Json(name = "kleurAchterGrond")
    val kleurAchterGrond: String? = null,
    @Json(name = "kleurAchterGrondRand")
    val kleurAchterGrondRand: String? = null,
    @Json(name = "kleurVoorGrondRand")
    val kleurVoorGrondRand: String? = null
)

