package com.danieljm.bussin.ui.screens.stop

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getStopDetails: GetStopDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopUiState())
    val uiState: StateFlow<StopUiState> = _uiState.asStateFlow()

    fun loadNearbyStops(stop: String, lat: Double, lon: Double, radius: Int? = null, maxAantal: Int? = null) {
        Log.d("StopViewModel", "loadNearbyStops called: stop=$stop lat=$lat lon=$lon radius=$radius maxAantal=$maxAantal")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = getNearbyStops(stop, lat, lon, radius, maxAantal)
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                Log.d("StopViewModel", "loadNearbyStops success: count=${list.size}")
                _uiState.value = _uiState.value.copy(isLoading = false, stops = list)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                Log.w("StopViewModel", "loadNearbyStops failed: $err")
                _uiState.value = _uiState.value.copy(isLoading = false, error = err)
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
