package com.danieljm.delijn.ui.screens.stops

import com.danieljm.delijn.domain.model.Stop

data class StopsUiState(
    val query: String = "",
    val stops: List<Stop> = emptyList()
)

