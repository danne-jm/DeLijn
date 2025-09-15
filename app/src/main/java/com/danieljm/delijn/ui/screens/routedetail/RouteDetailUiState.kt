package com.danieljm.delijn.ui.screens.routedetail

import com.danieljm.delijn.domain.model.Route

data class RouteDetailUiState(
    val route: Route? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

