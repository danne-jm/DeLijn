package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.NearbyStopDto
import com.danieljm.delijn.domain.model.Stop

object NearbyStopMapper {
    fun dtoToDomain(dto: NearbyStopDto): Stop {
        return Stop(
            id = dto.id,
            name = dto.naam ?: "Unknown Stop",
            latitude = dto.geoCoordinaat.latitude,
            longitude = dto.geoCoordinaat.longitude,
            entiteitnummer = dto.entiteitnummer ?: "3", // Default entity number for De Lijn
            halteNummer = dto.haltenummer ?: dto.id // Use haltenummer if available, fallback to id
        )
    }
}
