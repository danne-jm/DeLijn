package com.danieljm.delijn.ui.screens.stopdetailscreen

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine

data class BusPosition(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float = 0f
)

data class StopDetailUiState(
    val stopId: String = "",
    val stopName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val servedLines: List<ServedLine> = emptyList(),
    val allArrivals: List<ArrivalInfo> = emptyList(),
    // Currently selected line id for filtering polylines and bus markers
    val selectedLineId: String? = null,
    // Polylines and bus positions to display when a line is selected
    val selectedPolylines: List<com.danieljm.delijn.domain.model.LinePolyline> = emptyList(),
    val selectedBusPositions: List<BusPosition> = emptyList(),
    // Controls the refresh animation for the bottom sheet refresh icon
    val shouldAnimateRefresh: Boolean = false,
    // Timestamp in millis of the last arrivals refresh (automatic or manual)
    val lastArrivalsRefreshMillis: Long? = null,
    // Coordinates for centering the map on the stop
    val stopLatitude: Double? = null,
    val stopLongitude: Double? = null,
    // Live bus coordinates and vehicle ID
    val busLatitude: Double? = null,
    val busLongitude: Double? = null,
    val busVehicleId: String? = null,
    val busPositions: List<BusPosition> = emptyList(),
    // Polylines representing line directions to draw on the map
    val polylines: List<com.danieljm.delijn.domain.model.LinePolyline> = emptyList()
)