package com.danieljm.delijn.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Simple ViewModel for Home screen (placeholder). */
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(welcomeText = "Welcome to De Lijn App", isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                delay(1500) // Simulate loading
                val routes = listOf("Route 1", "Route 2", "Route 3")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    featuredRoutes = routes,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load routes"
                )
            }
        }
    }
}
