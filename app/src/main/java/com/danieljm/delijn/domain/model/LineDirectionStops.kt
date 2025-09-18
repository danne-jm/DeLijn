package com.danieljm.delijn.domain.model

data class LineDirectionStop(
    val halteNummer: String,
    val omschrijving: String,
    val latitude: Double,
    val longitude: Double
)

data class LineDirectionStopsResponse(
    val haltes: List<LineDirectionStop>
)

// Lightweight polyline model representing a line direction route
data class LinePolyline(
    val id: String,
    val coordinates: List<Pair<Double, Double>>,
    val colorHex: String? = null
)

