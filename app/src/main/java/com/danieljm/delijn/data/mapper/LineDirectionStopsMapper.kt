package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.LineDirectionStopsResponseDto
import com.danieljm.delijn.data.remote.dto.LineDirectionStopDto
import com.danieljm.delijn.domain.model.LineDirectionStop
import com.danieljm.delijn.domain.model.LineDirectionStopsResponse

fun LineDirectionStopsResponseDto.toDomain(): LineDirectionStopsResponse {
    return LineDirectionStopsResponse(
        haltes = haltes.map { it.toDomain() }
    )
}

fun LineDirectionStopDto.toDomain(): LineDirectionStop {
    return LineDirectionStop(
        // Some API responses may omit 'haltenummer', make sure to fallback to empty string
        halteNummer = this.halteNummer ?: "",
        omschrijving = this.omschrijving,
        latitude = this.geoCoordinaat.latitude,
        longitude = this.geoCoordinaat.longitude
    )
}
