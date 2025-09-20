package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.LineSearchResponseDto
import com.danieljm.bussin.domain.model.LineDirection

object LineDirectionMapper {
    fun fromDto(dto: LineSearchResponseDto): LineDirection {
        return LineDirection(
            lijnnummer = dto.lijnnummer ?: dto.lijnNummerPubliek ?: "",
            richting = dto.richting ?: "",
            omschrijving = dto.omschrijving,
            kleurVoorGrond = dto.kleurVoorGrond,
            kleurAchterGrond = dto.kleurAchterGrond,
            kleurAchterGrondRand = dto.kleurAchterGrondRand,
            kleurVoorGrondRand = dto.kleurVoorGrondRand
        )
    }
}

