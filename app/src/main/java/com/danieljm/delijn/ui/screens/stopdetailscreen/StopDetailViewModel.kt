package com.danieljm.delijn.ui.screens.stopdetailscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.model.ServedLine
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionDetailUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetScheduledArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsSearchUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class StopDetailViewModel(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase,
    private val getRealTimeArrivalsUseCase: GetRealTimeArrivalsUseCase,
    private val getScheduledArrivalsUseCase: GetScheduledArrivalsUseCase,
    private val getLineDirectionDetailUseCase: GetLineDirectionDetailUseCase,
    private val getLineDirectionsSearchUseCase: GetLineDirectionsSearchUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState
    // Persistent cache across enrich calls. Cleared or bypassed when forceRefresh is requested.
    private val lineDetailCache = mutableMapOf<String, com.danieljm.delijn.domain.model.LineDirectionSearch?>()

    private suspend fun fetchBusPosition(vehicleId: String): Triple<Double, Double, Float>? {
        val apiUrl = "https://api.delijn.be/gtfs/v3/realtime?json=true&position=true&vehicleid=$vehicleId"
        val apiKey = "5eacdcf7e85c4637a14f4d627403935a"
        return withContext(Dispatchers.IO) {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            try {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val entities = json.optJSONArray("entity") ?: return@withContext null
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    if (entity.has("vehicle")) {
                        val vehicle = entity.getJSONObject("vehicle")
                        val position = vehicle.optJSONObject("position") ?: continue
                        val lat = position.optDouble("latitude")
                        val lon = position.optDouble("longitude")
                        val bearing = position.optDouble("bearing", 0.0).toFloat()
                        if (!lat.isNaN() && !lon.isNaN()) {
                            return@withContext Triple(lat, lon, bearing)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("StopDetailViewModel", "Error fetching bus position", e)
                null
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun fetchAllBusPositions(arrivals: List<com.danieljm.delijn.domain.model.ArrivalInfo>): List<BusPosition> {
        val positions = mutableListOf<BusPosition>()
        for (arrival in arrivals) {
            val vehicleId = arrival.vrtnum
            if (!vehicleId.isNullOrEmpty()) {
                val pos = fetchBusPosition(vehicleId)
                if (pos != null) {
                    positions.add(BusPosition(vehicleId, pos.first, pos.second, pos.third))
                }
            }
        }
        return positions
    }

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
                            asFromTo = line.omschrijving,
                            entiteitnummer = line.entiteitnummer,
                            richting = line.richting
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

                    // Enrich arrivals with public line number and background color by calling the line direction detail API per unique line-direction
                    val enriched = enrichArrivalsWithLineColors(arrivalsToShow, servedLines, forceRefresh = false)

                    val busPositions = fetchAllBusPositions(enriched)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stopName,
                        servedLines = servedLines,
                        allArrivals = enriched,
                        lastArrivalsRefreshMillis = System.currentTimeMillis(),
                        stopLatitude = stop.latitude,
                        stopLongitude = stop.longitude,
                        busPositions = busPositions
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
                        // Set coordinates so the map can center if needed
                        _uiState.value = _uiState.value.copy(stopLatitude = stop.latitude, stopLongitude = stop.longitude)
                        val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)
                        servedLines = directionsResponse.lijnrichtingen.map { line ->
                            ServedLine(
                                lineId = line.lijnnummer,
                                lineName = line.lijnnummer,
                                omschrijving = line.omschrijving,
                                asFromTo = line.omschrijving,
                                entiteitnummer = line.entiteitnummer,
                                richting = line.richting
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
                        // also ensure coords are set
                        _uiState.value = _uiState.value.copy(stopLatitude = stop.latitude, stopLongitude = stop.longitude)
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

                // Enrich arrivals
                val enriched = enrichArrivalsWithLineColors(arrivalsToShow, servedLines, forceRefresh = force)

                val busPositions = fetchAllBusPositions(enriched)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allArrivals = enriched,
                    servedLines = servedLines,
                    lastArrivalsRefreshMillis = System.currentTimeMillis(),
                    busPositions = busPositions
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

    private suspend fun enrichArrivalsWithLineColors(
        arrivals: List<com.danieljm.delijn.domain.model.ArrivalInfo>,
        servedLines: List<ServedLine>,
        forceRefresh: Boolean = false
    ): List<com.danieljm.delijn.domain.model.ArrivalInfo> {
        if (arrivals.isEmpty()) return arrivals

        val cache = lineDetailCache
        if (forceRefresh) cache.clear()

        fun lineIdsMatch(a: String?, b: String?): Boolean {
            if (a == null || b == null) return false
            val ta = a.trim()
            val tb = b.trim()
            if (ta.equals(tb, ignoreCase = true)) return true
            return try { ta.toIntOrNull() == tb.toIntOrNull() } catch (e: Exception) { false }
        }

        return arrivals.map { arrival ->
            val key = "${arrival.lineId}|${arrival.omschrijving}"
            val cached = if (!forceRefresh) cache[key] else null
            val searchResult = cached ?: try {
                val resp = getLineDirectionsSearchUseCase(arrival.omschrijving)
                val candidate = resp.lijnrichtingen.find { lr ->
                    lineIdsMatch(lr.lijnnummer, arrival.lineId) &&
                    (lr.omschrijving?.equals(arrival.omschrijving.orEmpty(), ignoreCase = true) == true ||
                     lr.omschrijving?.contains(arrival.omschrijving.orEmpty(), ignoreCase = true) == true)
                } ?: resp.lijnrichtingen.firstOrNull()
                if (candidate != null) {
                    cache[key] = candidate
                }
                candidate
            } catch (e: Exception) {
                Log.w("StopDetailViewModel", "Failed to fetch line color for ${arrival.lineId} oms=${arrival.omschrijving}", e)
                null
            }

            if (searchResult == null) {
                Log.w("StopDetailViewModel", "No color/public line found for ${arrival.lineId} oms=${arrival.omschrijving}")
                arrival
            } else {
                Log.i("StopDetailViewModel", "Enriching arrival line ${arrival.lineId} with public=${searchResult.lijnNummerPubliek} color=${searchResult.kleurAchterGrond}")
                arrival.copy(
                    lineNumberPublic = searchResult.lijnNummerPubliek,
                    lineBackgroundColorHex = searchResult.kleurAchterGrond,
                    lineForegroundColorHex = searchResult.kleurVoorGrond,
                    lineForegroundRandColorHex = searchResult.kleurVoorGrondRand
                )
            }
        }
    }
}