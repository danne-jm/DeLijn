package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.LineDirectionDetailDto
import com.danieljm.delijn.domain.model.LineDirectionSearch

fun LineDirectionDetailDto.toDomain(): LineDirectionSearch {
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

