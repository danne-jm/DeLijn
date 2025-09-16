package com.danieljm.delijn.ui.screens.stops

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetCachedStopsUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import com.google.android.gms.location.LocationServices
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
        // This auto-refresh logic can remain as is
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val now = System.currentTimeMillis()
                if (lastFetchTimeMillis != 0L && now - lastFetchTimeMillis > 30_000) {
                    if (lastFetchLat != null && lastFetchLon != null) {
                        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = true)
                        fetchNearbyStops(lastFetchLat!!, lastFetchLon!!, isAuto = true)
                    }
                }
            }
        }
    }

    /**
     * Centralized function to request the user's current location.
     * This should be called from the UI when location is needed.
     * It's safe to suppress the missing permission check here because
     * the UI layer is responsible for verifying permissions before calling this.
     */
    @SuppressLint("MissingPermission")
    fun requestUserLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                _uiState.value = _uiState.value.copy(userLocation = location)
                // Automatically load stops for the new location
                loadStopsForLocation(location.latitude, location.longitude)
            }
        }.addOnFailureListener {
            Log.e("StopsViewModel", "Failed to get location.", it)
        }
    }

    fun loadStopsForLocation(latitude: Double, longitude: Double) {
        val currentState = _uiState.value
        // Prevent reloading if location hasn't changed significantly
        if (currentState.userLocation?.latitude == latitude && currentState.userLocation?.longitude == longitude && currentState.nearbyStops.isNotEmpty()) {
            return
        }

        Log.d("StopsViewModel", "Load cached stops first for quick display")
        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) { getCachedStopsUseCase() }
                if (cached.isNotEmpty()) {
                    Log.d("StopsViewModel", "Loaded ${cached.size} cached stops")
                    _uiState.value = _uiState.value.copy(nearbyStops = cached)
                }
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error loading cached stops", e)
            }
            // Then fetch live data
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
                val stops = withContext(Dispatchers.IO) { getNearbyStopsUseCase(latitude, longitude) }
                _uiState.value = _uiState.value.copy(
                    nearbyStops = stops,
                    isLoading = false,
                    shouldAnimateRefresh = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to fetch nearby stops: ${e.message}",
                    shouldAnimateRefresh = false
                )
            }
        }
    }

    fun onRefreshAnimationComplete() {
        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
    }

    fun fetchLineDirectionsForStop(stopId: String) {
        Log.d("StopsViewModel", "Fetching line directions for stop: $stopId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLineDirections = true)
            try {
                val entiteitnummer = "3"
                val response = withContext(Dispatchers.IO) {
                    getLineDirectionsForStopUseCase(entiteitnummer, stopId)
                }
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