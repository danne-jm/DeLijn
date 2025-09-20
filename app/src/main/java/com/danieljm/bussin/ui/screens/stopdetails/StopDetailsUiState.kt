package com.danieljm.bussin.ui.screens.stopdetails

import com.danieljm.bussin.domain.model.Stop

/**
 * Simple UI state for the Stop Details screen.
 * Keeps only UI-related data (selected stop + loading + error) — no business logic.
 */
data class StopDetailsUiState(
    val isLoading: Boolean = false,
    val selectedStop: Stop? = null,
    val error: String? = null
)
