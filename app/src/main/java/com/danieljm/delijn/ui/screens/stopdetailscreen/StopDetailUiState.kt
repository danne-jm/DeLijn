package com.danieljm.delijn.ui.screens.stopdetailscreen

data class ServedLine(
    val lineId: String,
    val lineName: String,
    val omschrijving: String,
    val asFromTo: String
)

data class ArrivalInfo(
    val lineId: String,
    val destination: String,
    val scheduledTime: String, // formatted scheduled time e.g. "20:02"
    val time: String, // formatted real time e.g. "20:03"
    val remainingMinutes: Long,
    val omschrijving: String,
    val expectedArrivalTime: Long,
    val realArrivalTime: Long
)

data class StopDetailUiState(
    val stopId: String = "",
    val stopName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val servedLines: List<ServedLine> = emptyList(),
    val allArrivals: List<ArrivalInfo> = emptyList()
)
