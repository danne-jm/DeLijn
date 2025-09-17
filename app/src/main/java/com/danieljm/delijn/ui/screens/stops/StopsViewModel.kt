package com.danieljm.delijn.ui.screens.stops

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetCachedStopsUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import com.google.android.gms.location.*
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

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastFetchedLocation: Location? = null

    // Threshold in meters. A new stop fetch will only occur if the user moves more than this distance.
    private val LOCATION_CHANGE_THRESHOLD_METERS = 500f

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { newLocation ->
                val previousLocation = _uiState.value.userLocation
                // Always update the user's location for the live map marker
                _uiState.value = _uiState.value.copy(userLocation = newLocation)

                val distance = lastFetchedLocation?.distanceTo(newLocation) ?: Float.MAX_VALUE

                // Fetch new stops only if the user has moved a significant distance
                if (distance > LOCATION_CHANGE_THRESHOLD_METERS) {
                    Log.d("StopsViewModel", "Location changed by ${distance}m. Fetching new stops.")
                    fetchNearbyStops(newLocation.latitude, newLocation.longitude)
                } else if (previousLocation == null) {
                    // This handles the very first location update
                    Log.d("StopsViewModel", "First location received. Fetching stops.")
                    fetchNearbyStops(newLocation.latitude, newLocation.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        Log.d("StopsViewModel", "Location updates stopped.")
    }

    fun fetchNearbyStops(latitude: Double, longitude: Double) {
        Log.d("StopsViewModel", "Starting to fetch nearby stops for lat=$latitude, lon=$longitude")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Create a location object to mark the position of this fetch
            val fetchLocation = Location("").apply {
                this.latitude = latitude
                this.longitude = longitude
            }
            lastFetchedLocation = fetchLocation
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