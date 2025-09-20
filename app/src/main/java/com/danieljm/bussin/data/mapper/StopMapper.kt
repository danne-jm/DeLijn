package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.HalteDto

object StopMapper {
    fun fromDto(dto: HalteDto): com.danieljm.bussin.domain.model.Stop {
        val id = dto.haltenummer ?: dto.id ?: ""
        val name = dto.naam ?: dto.omschrijving ?: dto.omschrijvingLang ?: ""
        val distance = dto.afstand
        val lat = dto.geoCoordinaat?.latitude
        val lon = dto.geoCoordinaat?.longitude
        return com.danieljm.bussin.domain.model.Stop(
            id = id,
            name = name,
            distance = distance,
            latitude = lat,
            longitude = lon
        )
    }
}
