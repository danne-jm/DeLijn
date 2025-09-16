package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.LineDirectionDto
import com.danieljm.delijn.data.remote.dto.LineDirectionsResponseDto
import com.danieljm.delijn.domain.model.LineDirection
import com.danieljm.delijn.domain.model.LineDirectionsResponse

fun LineDirectionDto.toDomain(): LineDirection {
    val parts = omschrijving.split(" - ")
    val from = parts.firstOrNull()?.trim() ?: ""
    val to = parts.lastOrNull()?.trim() ?: ""

    return LineDirection(
        entiteitnummer = entiteitnummer,
        lijnnummer = lijnnummer,
        richting = richting,
        omschrijving = omschrijving,
        from = from,
        to = to
    )
}

fun LineDirectionsResponseDto.toDomain(): LineDirectionsResponse {
    return LineDirectionsResponse(
        lijnrichtingen = lijnrichtingen.map { it.toDomain() }
    )
}
