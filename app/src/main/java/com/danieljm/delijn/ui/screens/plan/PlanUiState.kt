package com.danieljm.delijn.ui.screens.plan

/** UI state for the Plan screen. */
data class PlanUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val plans: List<String> = emptyList()
)

