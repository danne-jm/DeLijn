package com.danieljm.delijn.ui.screens.busdetail

import com.danieljm.delijn.domain.model.Bus

data class BusDetailUiState(
    val bus: Bus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

