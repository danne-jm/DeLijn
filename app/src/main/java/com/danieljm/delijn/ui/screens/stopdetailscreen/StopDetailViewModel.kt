package com.danieljm.delijn.ui.screens.stopdetailscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.model.ServedLine
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetScheduledArrivalsUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StopDetailViewModel(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase,
    private val getRealTimeArrivalsUseCase: GetRealTimeArrivalsUseCase,
    private val getScheduledArrivalsUseCase: GetScheduledArrivalsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState

    fun loadStopDetails(stopId: String, stopName: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, stopId = stopId, stopName = stopName)
        viewModelScope.launch {
            try {
                val stop = getStopDetailsUseCase(stopId)
                if (stop != null) {
                    val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)
                    val servedLines = directionsResponse.lijnrichtingen.map { line ->
                        ServedLine(
                            lineId = line.lijnnummer,
                            lineName = line.lijnnummer,
                            omschrijving = line.omschrijving,
                            asFromTo = line.omschrijving
                        )
                    }

                    // Delegate data fetching to use-cases (repository layer). Prefer real-time, fallback to scheduled.
                    val allArrivals = try {
                        Log.i("StopDetailViewModel", "Fetching live arrivals for stop ${stop.entiteitnummer}/${stop.halteNummer}")
                        getRealTimeArrivalsUseCase(stop.entiteitnummer, stop.halteNummer, servedLines)
                    } catch (e: Exception) {
                        Log.e("StopDetailViewModel", "Error fetching real-time arrivals", e)
                        emptyList()
                    }

                    val arrivalsToShow = if (allArrivals.isEmpty()) {
                        Log.i("StopDetailViewModel", "No live arrivals, fetching scheduled arrivals.")
                        try {
                            getScheduledArrivalsUseCase(stop.entiteitnummer, stop.halteNummer, servedLines)
                        } catch (e: Exception) {
                            Log.e("StopDetailViewModel", "Error fetching scheduled arrivals", e)
                            emptyList()
                        }
                    } else allArrivals

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stopName,
                        servedLines = servedLines,
                        allArrivals = arrivalsToShow,
                        lastArrivalsRefreshMillis = System.currentTimeMillis()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Stop not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Manual or programmatic arrivals refresh. If force==true always fetch, otherwise avoid excessive fetches elsewhere.
    fun refreshArrivals(force: Boolean = false) {
        val currentStopId = _uiState.value.stopId
        if (currentStopId.isEmpty()) return

        // Set animation flag so the UI can animate the refresh icon
        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = true)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                var servedLines = _uiState.value.servedLines
                var entiteitnummer: String? = null
                var halteNummer: String? = null

                if (servedLines.isEmpty()) {
                    // Try to fetch stop details and directions if not already available
                    val stop = getStopDetailsUseCase(currentStopId)
                    if (stop != null) {
                        entiteitnummer = stop.entiteitnummer
                        halteNummer = stop.halteNummer
                        val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)
                        servedLines = directionsResponse.lijnrichtingen.map { line ->
                            ServedLine(
                                lineId = line.lijnnummer,
                                lineName = line.lijnnummer,
                                omschrijving = line.omschrijving,
                                asFromTo = line.omschrijving
                            )
                        }
                    }
                }

                // If the entiteit/halte numbers weren't set above, derive them from previously loaded data if possible
                if (entiteitnummer == null || halteNummer == null) {
                    // Attempt to derive from stop details again
                    val stop = getStopDetailsUseCase(currentStopId)
                    if (stop != null) {
                        entiteitnummer = stop.entiteitnummer
                        halteNummer = stop.halteNummer
                    }
                }

                if (entiteitnummer == null || halteNummer == null) {
                    Log.w("StopDetailViewModel", "Unable to determine stop identifiers for refresh: $currentStopId")
                    _uiState.value = _uiState.value.copy(isLoading = false, shouldAnimateRefresh = false)
                    return@launch
                }

                // Fetch live arrivals
                val allArrivals = try {
                    getRealTimeArrivalsUseCase(entiteitnummer, halteNummer, servedLines)
                } catch (e: Exception) {
                    Log.e("StopDetailViewModel", "Error fetching real-time arrivals", e)
                    emptyList()
                }

                val arrivalsToShow = if (allArrivals.isEmpty()) {
                    try {
                        getScheduledArrivalsUseCase(entiteitnummer, halteNummer, servedLines)
                    } catch (e: Exception) {
                        Log.e("StopDetailViewModel", "Error fetching scheduled arrivals", e)
                        emptyList()
                    }
                } else allArrivals

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allArrivals = arrivalsToShow,
                    servedLines = servedLines,
                    lastArrivalsRefreshMillis = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("StopDetailViewModel", "Error during arrivals refresh", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onRefreshAnimationComplete() {
        _uiState.value = _uiState.value.copy(shouldAnimateRefresh = false)
    }
}