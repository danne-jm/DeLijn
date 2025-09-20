package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.HalteDto
import com.danieljm.bussin.domain.model.LineStop

object LineStopMapper {
    fun fromDto(dto: HalteDto): LineStop {
        return LineStop(
            entiteitnummer = dto.entiteitnummer,
            haltenummer = dto.haltenummer ?: dto.id ?: "",
            omschrijving = dto.omschrijving,
            omschrijvingLang = dto.omschrijvingLang,
            gemeentenummer = dto.gemeentenummer,
            omschrijvingGemeente = dto.omschrijvingGemeente,
            latitude = dto.geoCoordinaat?.latitude,
            longitude = dto.geoCoordinaat?.longitude
        )
    }
}
