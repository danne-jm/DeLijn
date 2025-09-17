package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.local.entities.StopEntity
import com.danieljm.delijn.data.remote.dto.StopDto
import com.danieljm.delijn.domain.model.Stop

object StopMapper {
    fun dtoToEntity(dto: StopDto) = StopEntity(
        id = dto.id,
        name = dto.name,
        latitude = dto.latitude,
        longitude = dto.longitude,
        entiteitnummer = dto.entiteitnummer,
        halteNummer = dto.halteNummer
    )

    fun entityToDomain(entity: StopEntity) = Stop(
        id = entity.id,
        name = entity.name,
        latitude = entity.latitude,
        longitude = entity.longitude,
        entiteitnummer = entity.entiteitnummer,
        halteNummer = entity.halteNummer
    )

    fun dtoToDomain(dto: StopDto) = entityToDomain(dtoToEntity(dto))
}
