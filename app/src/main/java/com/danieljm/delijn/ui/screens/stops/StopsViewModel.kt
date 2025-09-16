package com.danieljm.delijn.ui.screens.stops

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StopsViewModel(
    private val getNearbyStopsUseCase: GetNearbyStopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState: StateFlow<StopsUiState> = _uiState.asStateFlow()

    fun fetchNearbyStops(latitude: Double, longitude: Double) {
        Log.d("StopsViewModel", "Starting to fetch nearby stops for lat=$latitude, lon=$longitude")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d("StopsViewModel", "Calling use case to get nearby stops")
                val stops = getNearbyStopsUseCase(latitude, longitude)
                Log.d("StopsViewModel", "Successfully received ${stops.size} stops from use case")
                _uiState.value = _uiState.value.copy(
                    nearbyStops = stops,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error in fetchNearbyStops", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to fetch nearby stops: ${e.message}"
                )
            }
        }
    }
}
