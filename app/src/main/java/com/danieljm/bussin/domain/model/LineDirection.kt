package com.danieljm.bussin.domain.model

data class LineDirection(
    val lijnnummer: String,
    val richting: String,
    val omschrijving: String?,
    val kleurVoorGrond: String? = null,
    val kleurAchterGrond: String? = null,
    val kleurAchterGrondRand: String? = null,
    val kleurVoorGrondRand: String? = null
)

