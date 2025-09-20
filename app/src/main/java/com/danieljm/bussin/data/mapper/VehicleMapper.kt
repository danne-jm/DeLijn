package com.danieljm.bussin.data.mapper

import com.danieljm.bussin.data.remote.dto.VehicleInnerDto
import com.danieljm.bussin.data.remote.dto.VehicleRouteResponseDto
import com.danieljm.bussin.domain.model.Leg
import com.danieljm.bussin.domain.model.VehiclePosition
import com.danieljm.bussin.domain.model.VehicleRoute

object VehicleMapper {
    fun fromVehicleInner(dto: VehicleInnerDto?): VehiclePosition? {
        if (dto == null) return null
        val pos = dto.position
        val latitude = pos?.latitude
        val longitude = pos?.longitude
        val bearing = pos?.bearing
        return VehiclePosition(
            id = dto.id,
            vehicleId = dto.id,
            latitude = latitude,
            longitude = longitude,
            bearing = bearing,
            timestamp = dto.timestamp
        )
    }

    fun fromRouteDto(dto: VehicleRouteResponseDto): VehicleRoute {
        val vehicle = fromVehicleInner(dto.vehicle)
        val stops = dto.stops?.map { halte -> LineStopMapper.fromDto(halte) } ?: emptyList()
        val legs = dto.legs?.mapNotNull { l ->
            if (l.from != null && l.to != null) Leg(from = l.from, to = l.to) else null
        } ?: emptyList()
        return VehicleRoute(vehicle = vehicle, stops = stops, legs = legs)
    }
}
