package com.danieljm.bussin.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HalteDto(
    @param:Json(name = "type")
    val type: String?,
    // some endpoints use "id", others use "haltenummer"
    @param:Json(name = "id")
    val id: String? = null,
    @param:Json(name = "haltenummer")
    val haltenummer: String? = null,
    @param:Json(name = "naam")
    val naam: String? = null,
    @param:Json(name = "omschrijving")
    val omschrijving: String? = null,
    @param:Json(name = "omschrijvingLang")
    val omschrijvingLang: String? = null,
    @param:Json(name = "afstand")
    val afstand: Int? = null,
    @param:Json(name = "geoCoordinaat")
    val geoCoordinaat: GeoCoordinateDto? = null,

    // Additional fields referenced by mappers
    @param:Json(name = "entiteitnummer")
    val entiteitnummer: String? = null,
    @param:Json(name = "gemeentenummer")
    val gemeentenummer: Int? = null,
    @param:Json(name = "omschrijvingGemeente")
    val omschrijvingGemeente: String? = null
)
