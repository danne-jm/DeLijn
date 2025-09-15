package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.local.entities.RouteEntity
import com.danieljm.delijn.data.remote.dto.RouteDto
import com.danieljm.delijn.domain.model.Route

object RouteMapper {
    fun dtoToEntity(dto: RouteDto) = RouteEntity(
        id = dto.id,
        name = dto.name,
        stops = dto.stopIds
    )

    fun entityToDomain(entity: RouteEntity) = Route(
        id = entity.id,
        name = entity.name,
        stopIds = entity.stops
    )

    fun dtoToDomain(dto: RouteDto) = entityToDomain(dtoToEntity(dto))
}

