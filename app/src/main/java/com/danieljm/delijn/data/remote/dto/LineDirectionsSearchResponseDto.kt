package com.danieljm.delijn.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LineDirectionSearchDto(
    @Json(name = "lijnNummerPubliek")
    val lijnNummerPubliek: String?,
    @Json(name = "entiteitnummer")
    val entiteitnummer: String?,
    @Json(name = "lijnnummer")
    val lijnnummer: String?,
    @Json(name = "richting")
    val richting: String?,
    @Json(name = "omschrijving")
    val omschrijving: String?,
    @Json(name = "kleurVoorGrond")
    val kleurVoorGrond: String?,
    @Json(name = "kleurAchterGrond")
    val kleurAchterGrond: String?,
    @Json(name = "kleurAchterGrondRand")
    val kleurAchterGrondRand: String?,
    @Json(name = "kleurVoorGrondRand")
    val kleurVoorGrondRand: String?
)

@JsonClass(generateAdapter = true)
data class LineDirectionsSearchResponseDto(
    @Json(name = "aantalHits")
    val aantalHits: Int,
    @Json(name = "lijnrichtingen")
    val lijnrichtingen: List<LineDirectionSearchDto>
)

