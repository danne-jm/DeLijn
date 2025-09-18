package com.danieljm.delijn.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Simple ViewModel for Home screen (placeholder). */
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(welcomeText = "Welcome to De Lijn App"))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
