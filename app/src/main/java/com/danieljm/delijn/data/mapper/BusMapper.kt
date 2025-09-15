package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.local.entities.BusEntity
import com.danieljm.delijn.data.remote.dto.BusDto
import com.danieljm.delijn.domain.model.Bus

object BusMapper {
    fun dtoToEntity(dto: BusDto) = BusEntity(
        id = dto.id,
        line = dto.line,
        destination = dto.destination
    )

    fun entityToDomain(entity: BusEntity) = Bus(
        id = entity.id,
        line = entity.line,
        destination = entity.destination
    )

    fun dtoToDomain(dto: BusDto) = entityToDomain(dtoToEntity(dto))
}

