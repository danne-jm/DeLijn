package com.danieljm.delijn.ui.screens.stopdetailscreen

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine

data class StopDetailUiState(
    val stopId: String = "",
    val stopName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val servedLines: List<ServedLine> = emptyList(),
    val allArrivals: List<ArrivalInfo> = emptyList()
)