package com.danieljm.bussin.ui.screens.stop

import com.danieljm.bussin.domain.model.Stop

data class StopUiState(
    val isLoading: Boolean = false,
    val stops: List<Stop> = emptyList(),
    val selectedStop: Stop? = null,
    val error: String? = null
)

