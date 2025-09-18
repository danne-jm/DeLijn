package com.danieljm.delijn.domain.model

data class LineDirectionSearch(
    val lijnNummerPubliek: String?,
    val entiteitnummer: String?,
    val lijnnummer: String?,
    val richting: String?,
    val omschrijving: String?,
    val kleurVoorGrond: String?,
    val kleurAchterGrond: String?,
    val kleurAchterGrondRand: String?,
    val kleurVoorGrondRand: String?
)

data class LineDirectionsSearchResponse(
    val aantalHits: Int,
    val lijnrichtingen: List<LineDirectionSearch>
)

