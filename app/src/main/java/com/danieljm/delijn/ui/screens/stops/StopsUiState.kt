package com.danieljm.delijn.ui.screens.stops

import com.danieljm.delijn.domain.model.LineDirection
import com.danieljm.delijn.domain.model.Stop

data class StopsUiState(
    val query: String = "",
    val stops: List<Stop> = emptyList(),
    val nearbyStops: List<Stop> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedStopLineDirections: List<LineDirection> = emptyList(),
    val isLoadingLineDirections: Boolean = false,
    val shouldAnimateRefresh: Boolean = false // NEW: triggers refresh icon animation
)
