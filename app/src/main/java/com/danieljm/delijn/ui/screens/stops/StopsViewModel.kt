package com.danieljm.delijn.ui.screens.stops

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetCachedStopsUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StopsViewModel(
    private val getNearbyStopsUseCase: GetNearbyStopsUseCase,
    private val getCachedStopsUseCase: GetCachedStopsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState: StateFlow<StopsUiState> = _uiState.asStateFlow()

    private var lastFetchTimeMillis: Long = 0L
    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null

    init {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val now = System.currentTimeMillis()
                if (lastFetchTimeMillis != 0L && now - lastFetchTimeMillis > 30_000) {
                    // Only auto-refresh if we have a location
                    if (lastFetchLat != null && lastFetchLon != null) {
                        // Set shouldAnimateRefresh to true before fetching
                        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = true)
                        fetchNearbyStops(lastFetchLat!!, lastFetchLon!!, isAuto = true)
                    }
                }
            }
        }
    }

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

    fun fetchNearbyStops(latitude: Double, longitude: Double, isAuto: Boolean = false) {
        Log.d("StopsViewModel", "Starting to fetch nearby stops for lat=$latitude, lon=$longitude")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, shouldAnimateRefresh = true)
            lastFetchTimeMillis = System.currentTimeMillis()
            lastFetchLat = latitude
            lastFetchLon = longitude
            try {
                Log.d("StopsViewModel", "Calling use case to get nearby stops")
                val stops = withContext(Dispatchers.IO) { getNearbyStopsUseCase(latitude, longitude) }
                Log.d("StopsViewModel", "Successfully received ${stops.size} stops from use case")
                _uiState.value = _uiState.value.copy(
                    nearbyStops = stops,
                    isLoading = false,
                    shouldAnimateRefresh = false // reset after fetch
                )
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error in fetchNearbyStops", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to fetch nearby stops: ${e.message}",
                    shouldAnimateRefresh = false
                )
            }
        }
    }

    fun onRefreshAnimationComplete() {
        // Called by UI after animation completes to reset flag
        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
    }

    fun fetchLineDirectionsForStop(stopId: String) {
        Log.d("StopsViewModel", "Fetching line directions for stop: $stopId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLineDirections = true)
            try {
                // Extract entiteitnummer (3) and haltenummer from stopId
                val entiteitnummer = "3" // Based on the API example
                val response = withContext(Dispatchers.IO) {
                    getLineDirectionsForStopUseCase(entiteitnummer, stopId)
                }
                Log.d("StopsViewModel", "Successfully received ${response.lijnrichtingen.size} line directions")
                _uiState.value = _uiState.value.copy(
                    selectedStopLineDirections = response.lijnrichtingen,
                    isLoadingLineDirections = false
                )
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error fetching line directions for stop $stopId", e)
                _uiState.value = _uiState.value.copy(
                    selectedStopLineDirections = emptyList(),
                    isLoadingLineDirections = false
                )
            }
        }
    }
}
