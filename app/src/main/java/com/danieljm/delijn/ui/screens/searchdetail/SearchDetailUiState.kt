package com.danieljm.delijn.ui.screens.searchdetail

import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.model.Stop

data class SearchDetailUiState(
    val stop: Stop? = null,
    val realtime: List<RealTimeDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

