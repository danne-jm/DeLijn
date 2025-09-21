package com.danieljm.bussin.ui.screens.stop

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.bussin.domain.usecase.GetCachedNearbyStopsUseCase
import com.danieljm.bussin.domain.usecase.GetNearbyStopsUseCase
import com.danieljm.bussin.domain.usecase.GetStopDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopViewModel @Inject constructor(
    private val getNearbyStops: GetNearbyStopsUseCase,
    private val getStopDetails: GetStopDetailsUseCase,
    private val getCachedNearbyStops: GetCachedNearbyStopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopUiState())
    val uiState: StateFlow<StopUiState> = _uiState.asStateFlow()

    // Remember the last fetch center to avoid redundant network calls when the map moves only a little.
    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null

    fun loadNearbyStops(stop: String, lat: Double, lon: Double, radius: Int? = null, maxAantal: Int? = null) {
        // Optimization: avoid refetching when the requested center is very close to the last one.
        try {
            val lastLat = lastFetchLat
            val lastLon = lastFetchLon
            if (lastLat != null && lastLon != null) {
                val results = FloatArray(1)
                Location.distanceBetween(lat, lon, lastLat, lastLon, results)
                val dist = results[0]
                // If the map center hasn't moved more than ~50 meters since the last fetch, skip.
                if (dist < 50f) {
                    // ensure we are not showing a loader
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return
                }
            }

            Log.d("StopViewModel", "loadNearbyStops called: stop=$stop lat=$lat lon=$lon radius=$radius maxAantal=$maxAantal")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // store last fetch coords now to throttle subsequent calls
            lastFetchLat = lat
            lastFetchLon = lon
        } catch (_: Throwable) {
            // proceed normally if distance check fails for any reason
            Log.d("StopViewModel", "loadNearbyStops: distance check failed, proceeding")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            lastFetchLat = lat
            lastFetchLon = lon
        }
        viewModelScope.launch {
            // First, emit cached stops immediately using a small bounding box around the center.
            try {
                // Simple bounding box: ~0.02 degrees (~2km) either side. This is a practical default
                // - If you need radius-accurate bounding box, replace with haversine math.
                val delta = 0.02
                val minLat = lat - delta
                val maxLat = lat + delta
                val minLon = lon - delta
                val maxLon = lon + delta
                val cachedRes = getCachedNearbyStops(minLat, maxLat, minLon, maxLon)
                if (cachedRes.isSuccess) {
                    val cached = cachedRes.getOrNull() ?: emptyList()
                    if (cached.isNotEmpty()) {
                        // Immediately show cached stops so markers can appear without waiting for network
                        _uiState.value = _uiState.value.copy(isLoading = true, stops = cached)
                    }
                }
            } catch (e: Exception) {
                // ignore cached read errors; continue to network
            }

            // Now perform network fetch in background and merge results.
            val res = getNearbyStops(stop, lat, lon, radius, maxAantal)
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                Log.d("StopViewModel", "loadNearbyStops success: count=${'$'}{list.size}")

                // Merge: keep existing cached markers (by id) and only add new stops reported by network.
                val current = _uiState.value.stops
                val currentById = current.associateBy { it.id }.toMutableMap()
                val toAdd = mutableListOf<com.danieljm.bussin.domain.model.Stop>()
                for (s in list) {
                    if (!currentById.containsKey(s.id)) {
                        // New stop: include in UI
                        currentById[s.id] = s
                        toAdd.add(s)
                    } else {
                        // Existing stop present in cache: do not modify UI marker to avoid jumpiness.
                        // Still, the repository already persisted the fresh network data.
                    }
                }
                val merged = currentById.values.toList()
                _uiState.value = _uiState.value.copy(isLoading = false, stops = merged)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                Log.w("StopViewModel", "loadNearbyStops failed: $err")
                _uiState.value = _uiState.value.copy(isLoading = false, error = err)
            }
        }
    }

    /**
     * Read cached nearby stops around the provided center and immediately emit them into UI state.
     * This method intentionally does not alter the last-fetch throttling markers so callers can
     * show cached results quickly while a separate network fetch is scheduled by the caller.
     */
    fun loadCachedNearbyStops(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val delta = 0.02
                val minLat = lat - delta
                val maxLat = lat + delta
                val minLon = lon - delta
                val maxLon = lon + delta
                val cachedRes = getCachedNearbyStops(minLat, maxLat, minLon, maxLon)
                if (cachedRes.isSuccess) {
                    val cached = cachedRes.getOrNull() ?: emptyList()
                    if (cached.isNotEmpty()) {
                        // Show cached stops quickly. Keep isLoading as true to indicate a background refresh may follow.
                        _uiState.value = _uiState.value.copy(isLoading = true, stops = cached)
                    }
                }
            } catch (_: Exception) {
                // swallow cache errors - non-fatal
            }
        }
    }

    fun loadStopDetails(stopId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = getStopDetails(stopId)
            if (res.isSuccess) {
                val s = res.getOrNull()
                _uiState.value = _uiState.value.copy(isLoading = false, selectedStop = s)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                _uiState.value = _uiState.value.copy(isLoading = false, error = err)
            }
        }
    }
}
