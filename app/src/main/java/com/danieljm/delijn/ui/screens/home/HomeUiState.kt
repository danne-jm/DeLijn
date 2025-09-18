package com.danieljm.delijn.ui.screens.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val welcomeText: String? = null,
    val featuredRoutes: List<String> = emptyList()
)