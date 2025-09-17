package com.danieljm.delijn.ui.screens.stopdetailscreen

data class ServedLine(
    val lineId: String,
    val lineName: String,
    val omschrijving: String,
    val asFromTo: String
)

data class StopDetailUiState(
    val stopId: String = "",
    val stopName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val servedLines: List<ServedLine> = emptyList()
)
