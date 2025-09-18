package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.LineDirectionSearchDto
import com.danieljm.delijn.data.remote.dto.LineDirectionsSearchResponseDto
import com.danieljm.delijn.domain.model.LineDirectionSearch
import com.danieljm.delijn.domain.model.LineDirectionsSearchResponse

fun LineDirectionSearchDto.toDomain(): LineDirectionSearch {
    return LineDirectionSearch(
        lijnNummerPubliek = this.lijnNummerPubliek,
        entiteitnummer = this.entiteitnummer,
        lijnnummer = this.lijnnummer,
        richting = this.richting,
        omschrijving = this.omschrijving,
        kleurVoorGrond = this.kleurVoorGrond,
        kleurAchterGrond = this.kleurAchterGrond,
        kleurAchterGrondRand = this.kleurAchterGrondRand,
        kleurVoorGrondRand = this.kleurVoorGrondRand
    )
}

fun LineDirectionsSearchResponseDto.toDomain(): LineDirectionsSearchResponse {
    return LineDirectionsSearchResponse(
        aantalHits = this.aantalHits,
        lijnrichtingen = this.lijnrichtingen.map { it.toDomain() }
    )
}

