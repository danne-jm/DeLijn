package com.danieljm.delijn.ui.screens.searchdetail

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.Stop

data class SearchDetailUiState(
    val stop: Stop? = null,
    val realtime: List<ArrivalInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
