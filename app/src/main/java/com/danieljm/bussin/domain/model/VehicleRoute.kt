package com.danieljm.bussin.domain.model

data class VehicleRoute(
    val vehicle: VehiclePosition?,
    val stops: List<LineStop> = emptyList(),
    val legs: List<Leg> = emptyList()
)

