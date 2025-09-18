package com.danieljm.delijn.ui.screens.stopdetailscreen

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine

data class BusPosition(
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double
)

data class StopDetailUiState(
    val stopId: String = "",
    val stopName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val servedLines: List<ServedLine> = emptyList(),
    val allArrivals: List<ArrivalInfo> = emptyList(),
    // Controls the refresh animation for the bottom sheet refresh icon
    val shouldAnimateRefresh: Boolean = false,
    // Timestamp in millis of the last arrivals refresh (automatic or manual)
    val lastArrivalsRefreshMillis: Long? = null,
    // Coordinates for centering the map on the stop
    val stopLatitude: Double? = null,
    val stopLongitude: Double? = null,
    // Live bus coordinates and vehicle ID
    val busPositions: List<BusPosition> = emptyList()
)