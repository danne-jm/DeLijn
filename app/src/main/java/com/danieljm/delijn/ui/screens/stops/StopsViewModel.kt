package com.danieljm.delijn.ui.screens.stops

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetCachedStopsUseCase
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StopsViewModel(
    private val getNearbyStopsUseCase: GetNearbyStopsUseCase,
    private val getCachedStopsUseCase: GetCachedStopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState: StateFlow<StopsUiState> = _uiState.asStateFlow()

    fun loadStopsForLocation(latitude: Double, longitude: Double) {
        Log.d("StopsViewModel", "Load cached stops first for quick display")
        viewModelScope.launch {
            try {
                // Load cached stops on IO
                val cached = withContext(Dispatchers.IO) { getCachedStopsUseCase() }
                if (cached.isNotEmpty()) {
                    Log.d("StopsViewModel", "Loaded ${cached.size} cached stops")
                    _uiState.value = _uiState.value.copy(nearbyStops = cached)
                } else {
                    Log.d("StopsViewModel", "No cached stops found")
                }
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error loading cached stops", e)
            }

            // Then fetch live data and update
            fetchNearbyStops(latitude, longitude)
        }
    }

    fun fetchNearbyStops(latitude: Double, longitude: Double) {
        Log.d("StopsViewModel", "Starting to fetch nearby stops for lat=$latitude, lon=$longitude")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d("StopsViewModel", "Calling use case to get nearby stops")
                val stops = withContext(Dispatchers.IO) { getNearbyStopsUseCase(latitude, longitude) }
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
