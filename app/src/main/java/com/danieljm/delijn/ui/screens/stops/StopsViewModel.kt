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
import com.danieljm.delijn.data.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StopsViewModel(
    private val getNearbyStopsUseCase: GetNearbyStopsUseCase,
    private val getCachedStopsUseCase: GetCachedStopsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState: StateFlow<StopsUiState> = _uiState.asStateFlow()

    private var lastFetchedLocation: Location? = null

    // Threshold in meters. A new stop fetch will only occur if the user moves more than this distance.
    private val LOCATION_CHANGE_THRESHOLD_METERS = 100f // 100m threshold
    private val MAP_CENTER_CHANGE_THRESHOLD_METERS = 500f // 500m threshold for map navigation

    private var locationUpdatesJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        // No-op if already collecting
        if (locationUpdatesJob != null && locationUpdatesJob!!.isActive) return

        locationUpdatesJob = viewModelScope.launch {
            try {
                // Emit last known location immediately if available
                val last = try {
                    locationProvider.getLastKnownLocation()
                } catch (e: Exception) {
                    Log.w("StopsViewModel", "Failed to get last known location: ${e.message}")
                    null
                }

                if (last != null) {
                    _uiState.value = _uiState.value.copy(userLocation = last)

                    val distanceSinceFetch = lastFetchedLocation?.distanceTo(last) ?: Float.MAX_VALUE
                    if (distanceSinceFetch > LOCATION_CHANGE_THRESHOLD_METERS || lastFetchedLocation == null) {
                        fetchNearbyStops(last.latitude, last.longitude)
                    }
                }

                // Collect continuous updates
                locationProvider.locationUpdates().collect { newLocation ->
                    val previousLocation = _uiState.value.userLocation

                    if (previousLocation == null || newLocation.distanceTo(previousLocation) > 1f) {
                        _uiState.value = _uiState.value.copy(userLocation = newLocation)
                    }

                    val distance = lastFetchedLocation?.distanceTo(newLocation) ?: Float.MAX_VALUE
                    if (distance > LOCATION_CHANGE_THRESHOLD_METERS || lastFetchedLocation == null) {
                        fetchNearbyStops(newLocation.latitude, newLocation.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Error while collecting location updates", e)
            }
        }
    }

    fun stopLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        Log.d("StopsViewModel", "Location updates stopped.")
    }

    // New function to force location update and bus stop refetch
    @SuppressLint("MissingPermission")
    fun forceLocationUpdateAndRefresh(context: Context) {
        Log.d("StopsViewModel", "Force refresh triggered")

        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = true)

        viewModelScope.launch {
            try {
                // Try last known location first, then wait for the next update if needed
                val loc = locationProvider.getLastKnownLocation() ?: locationProvider.locationUpdates().firstOrNull()

                if (loc != null) {
                    _uiState.value = _uiState.value.copy(userLocation = loc)
                    Log.d("StopsViewModel", "Force fetching stops at lat=${loc.latitude}, lon=${loc.longitude}")
                    fetchNearbyStops(loc.latitude, loc.longitude)
                } else {
                    Log.w("StopsViewModel", "Force refresh: Unable to obtain a location")
                    _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
                }
            } catch (e: Exception) {
                Log.e("StopsViewModel", "Force refresh failed", e)
                _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
            }
        }
    }

    // Function to reset the refresh animation flag
    fun onRefreshAnimationComplete() {
        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
    }

    fun fetchNearbyStops(latitude: Double, longitude: Double, fromMapNavigation: Boolean = false) {
        viewModelScope.launch {
            // Create a location object for the current request
            val requestLocation = Location("").apply {
                this.latitude = latitude
                this.longitude = longitude
            }

            // If the fetch is from map navigation, check if we've moved enough
            if (fromMapNavigation) {
                val distance = lastFetchedLocation?.distanceTo(requestLocation) ?: Float.MAX_VALUE
                if (distance < MAP_CENTER_CHANGE_THRESHOLD_METERS) {
                    Log.d("StopsViewModel", "Skipping map-based fetch; distance ($distance m) is less than threshold (${MAP_CENTER_CHANGE_THRESHOLD_METERS}m).")
                    return@launch
                }
            }

            Log.d("StopsViewModel", "Starting to fetch nearby stops for lat=$latitude, lon=$longitude")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Mark the position of this fetch
            lastFetchedLocation = requestLocation

            try {
                val stops = withContext(Dispatchers.IO) { getNearbyStopsUseCase(latitude, longitude) }
                _uiState.value = _uiState.value.copy(
                    nearbyStops = stops,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to fetch nearby stops: ${e.message}",
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
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