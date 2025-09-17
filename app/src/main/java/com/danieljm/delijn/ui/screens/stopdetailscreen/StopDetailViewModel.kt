package com.danieljm.delijn.ui.screens.stopdetailscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import com.danieljm.delijn.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
                    val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)
                    val servedLines = directionsResponse.lijnrichtingen.map { line ->
                        ServedLine(
                            lineId = line.lijnnummer.toString(),
                            lineName = line.lijnnummer.toString(),
                            omschrijving = line.omschrijving,
                            asFromTo = line.omschrijving
                        )
                    }

                    val allArrivals = try {
                        Log.i("StopDetailViewModel", "Fetching live arrivals for stop ${stop.entiteitnummer}/${stop.halteNummer}")
                        fetchLiveArrivals(stop.entiteitnummer, stop.halteNummer)
                    } catch (e: Exception) {
                        Log.e("StopDetailViewModel", "Error in fetchLiveArrivals", e)
                        emptyList()
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stop.name,
                        servedLines = servedLines,
                        allArrivals = allArrivals
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Stop not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun fetchLiveArrivals(entiteitnummer: String, halteNummer: String): List<ArrivalInfo> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = "https://api.delijn.be/DLKernOpenData/api/v1/haltes/$entiteitnummer/$halteNummer/real-time?maxAantalDoorkomsten=10"
                Log.i("StopDetailViewModel", "Real-time request URL: $url")

                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Ocp-Apim-Subscription-Key", Constants.API_KEY)
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val responseCode = connection.responseCode
                Log.i("StopDetailViewModel", "Real-time response code: $responseCode")

                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.w("StopDetailViewModel", "Real-time API error ($responseCode): $errorResponse")
                    return@withContext emptyList()
                }

                Log.i("StopDetailViewModel", "Real-time response: $response")

                val json = JSONObject(response)
                val halteDoorkomsten = json.optJSONArray("halteDoorkomsten")

                if (halteDoorkomsten == null) {
                    Log.w("StopDetailViewModel", "No halteDoorkomsten array in response")
                    return@withContext emptyList()
                }

                val allArrivals = mutableListOf<ArrivalInfo>()
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                for (i in 0 until halteDoorkomsten.length()) {
                    val halteObj = halteDoorkomsten.getJSONObject(i)
                    val doorkomsten = halteObj.optJSONArray("doorkomsten") ?: continue

                    for (j in 0 until doorkomsten.length()) {
                        val doorkomst = doorkomsten.getJSONObject(j)
                        val lineId = doorkomst.optString("lijnnummer")
                        val destination = doorkomst.optString("bestemming")
                        val realTimeStr = doorkomst.optString("real-timeTijdstip")

                        if (realTimeStr.isBlank() || lineId.isBlank()) {
                            Log.d("StopDetailViewModel", "Skipping arrival: lineId=$lineId, realTime=$realTimeStr")
                            continue
                        }

                        val realTime = try {
                            LocalDateTime.parse(realTimeStr, formatter)
                        } catch (e: Exception) {
                            Log.e("StopDetailViewModel", "Failed to parse time: $realTimeStr", e)
                            continue
                        }

                        var remaining = Duration.between(now, realTime).toMinutes()
                        if (remaining < 0) remaining = 0

                        val arrival = ArrivalInfo(
                            lineId = lineId,
                            destination = destination.ifBlank { "-" },
                            time = realTime.format(timeFormatter),
                            remainingMinutes = remaining
                        )
                        allArrivals.add(arrival)
                        Log.d("StopDetailViewModel", "Added arrival for line $lineId: $destination at ${arrival.time} (${arrival.remainingMinutes} min)")
                    }
                }

                allArrivals.sortBy { it.remainingMinutes }
                if (allArrivals.isEmpty()) {
                    Log.w("StopDetailViewModel", "No arrivals parsed from response")
                } else {
                    Log.i("StopDetailViewModel", "Parsed and sorted ${allArrivals.size} arrivals.")
                }

                allArrivals
            } catch (e: Exception) {
                Log.e("StopDetailViewModel", "Error fetching live arrivals: ${e.message}", e)
                emptyList()
            } finally {
                connection?.disconnect()
            }
        }
    }
}