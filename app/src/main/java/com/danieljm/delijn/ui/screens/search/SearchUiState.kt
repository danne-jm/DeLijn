package com.danieljm.delijn.ui.screens.search

import com.danieljm.delijn.domain.model.Stop

data class SearchUiState(
    val query: String = "",
    val results: List<Stop> = emptyList()
)

