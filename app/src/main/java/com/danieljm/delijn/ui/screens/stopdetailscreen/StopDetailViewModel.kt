package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StopDetailViewModel(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState

    fun loadStopDetails(stopId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, stopId = stopId)
        viewModelScope.launch {
            try {
                val stop = getStopDetailsUseCase(stopId)
                if (stop != null) {
                    // Fetch served lines using correct entiteitnummer and halteNummer
                    val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)
                    val servedLines = directionsResponse.lijnrichtingen.map { line ->
                        ServedLine(
                            lineId = line.lijnnummer,
                            lineName = line.lijnnummer,
                            omschrijving = line.omschrijving,
                            asFromTo = line.omschrijving // Use the description from API
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stop.name,
                        servedLines = servedLines
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Stop not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
