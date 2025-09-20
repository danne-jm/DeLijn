package com.danieljm.bussin.domain.model

data class LineStop(
    val entiteitnummer: String?,
    val haltenummer: String,
    val omschrijving: String?,
    val omschrijvingLang: String?,
    val gemeentenummer: Int?,
    val omschrijvingGemeente: String?,
    val latitude: Double?,
    val longitude: Double?
)

