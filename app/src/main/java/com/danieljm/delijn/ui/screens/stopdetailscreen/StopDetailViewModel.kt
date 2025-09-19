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
import com.danieljm.delijn.domain.usecase.GetLineDirectionStopsUseCase
import com.danieljm.delijn.domain.usecase.GetVehiclePositionUseCase
import com.danieljm.delijn.domain.usecase.GetRouteGeometryUseCase
import com.danieljm.delijn.domain.model.LinePolyline
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.system.*

class StopDetailViewModel(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase,
    private val getRealTimeArrivalsUseCase: GetRealTimeArrivalsUseCase,
    private val getScheduledArrivalsUseCase: GetScheduledArrivalsUseCase,
    private val getLineDirectionDetailUseCase: GetLineDirectionDetailUseCase,
    private val getLineDirectionsSearchUseCase: GetLineDirectionsSearchUseCase,
    private val getLineDirectionStopsUseCase: GetLineDirectionStopsUseCase,
    private val getVehiclePositionUseCase: GetVehiclePositionUseCase,
    private val getRouteGeometryUseCase: GetRouteGeometryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState
    // Persistent cache across enrich calls. Cleared or bypassed when forceRefresh is requested.
    private val lineDetailCache = mutableMapOf<String, com.danieljm.delijn.domain.model.LineDirectionSearch?>()

    private suspend fun fetchAllBusPositions(arrivals: List<com.danieljm.delijn.domain.model.ArrivalInfo>): List<BusPosition> {
        val positions = mutableListOf<BusPosition>()
        for (arrival in arrivals) {
            val vehicleId = arrival.vrtnum
            if (!vehicleId.isNullOrEmpty()) {
                try {
                    val pos = getVehiclePositionUseCase(vehicleId)
                    if (pos != null) {
                        positions.add(BusPosition(vehicleId, pos.latitude, pos.longitude, pos.bearing))
                    }
                } catch (e: Exception) {
                    Log.w("StopDetailViewModel", "Failed to fetch vehicle position for $vehicleId", e)
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

                    // Fetch both scheduled and real-time arrivals, then merge them according to rules:
                    // - Only include arrivals within the next 90 minutes
                    // - For duplicates, prefer real-time arrival data (replace scheduled)
                    val nowMs = System.currentTimeMillis()
                    val windowMs = 90 * 60 * 1000L // 90 minutes

                    val scheduledArrivals = try {
                        Log.i("StopDetailViewModel", "Fetching scheduled arrivals for stop ${stop.entiteitnummer}/${stop.halteNummer}")
                        getScheduledArrivalsUseCase(stop.entiteitnummer, stop.halteNummer, servedLines)
                    } catch (_: Exception) {
                        Log.e("StopDetailViewModel", "Error fetching scheduled arrivals")
                        emptyList()
                    }

                    val realTimeArrivals = try {
                        Log.i("StopDetailViewModel", "Fetching live arrivals for stop ${stop.entiteitnummer}/${stop.halteNummer}")
                        getRealTimeArrivalsUseCase(stop.entiteitnummer, stop.halteNummer, servedLines)
                    } catch (_: Exception) {
                        Log.e("StopDetailViewModel", "Error fetching real-time arrivals")
                        emptyList()
                    }

                    // Do not filter scheduled arrivals for the list; show all scheduled for the day.
                    // Keep real-time arrivals as-is (they will replace scheduled entries where appropriate).
                    val arrivalsToShow = mergeArrivalsPreferRealtime(scheduledArrivals, realTimeArrivals)

                    // Enrich arrivals with public line number and background color by calling the line direction detail API per unique line-direction
                    val enriched = enrichArrivalsWithLineColors(arrivalsToShow, servedLines, forceRefresh = false)

                    // Filter out arrivals that already occurred 10+ minutes ago to declutter the list,
                    // but keep arrivals with unknown timestamps.
                    val tenMinutesMs = 10 * 60 * 1000L
                    val cutoff = System.currentTimeMillis() - tenMinutesMs
                    val filtered = enriched.filter { arrival ->
                        val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                        // If we have a timestamp, exclude it when it is strictly less than cutoff (i.e., arrived 10+ minutes ago)
                        if (t > 0L) {
                            t >= cutoff
                        } else {
                            // unknown timestamp: keep it
                            true
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stopName,
                        servedLines = servedLines,
                        allArrivals = filtered,
                        lastArrivalsRefreshMillis = System.currentTimeMillis(),
                        stopLatitude = stop.latitude,
                        stopLongitude = stop.longitude
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
                val nowMs = System.currentTimeMillis()
                val windowMs = 90 * 60 * 1000L

                val scheduledArrivals = try {
                    getScheduledArrivalsUseCase(entiteitnummer, halteNummer, servedLines)
                } catch (_: Exception) {
                    Log.e("StopDetailViewModel", "Error fetching scheduled arrivals")
                    emptyList()
                }

                val realTimeArrivals = try {
                    getRealTimeArrivalsUseCase(entiteitnummer, halteNummer, servedLines)
                } catch (_: Exception) {
                    Log.e("StopDetailViewModel", "Error fetching real-time arrivals")
                    emptyList()
                }

                val arrivalsToShow = mergeArrivalsPreferRealtime(scheduledArrivals, realTimeArrivals)

                // Enrich arrivals
                val enriched = enrichArrivalsWithLineColors(arrivalsToShow, servedLines, forceRefresh = force)

                // Filter out arrivals that already occurred 10+ minutes ago to declutter the list,
                // but keep arrivals with unknown timestamps.
                val tenMinutesMs = 10 * 60 * 1000L
                val cutoff = System.currentTimeMillis() - tenMinutesMs
                val filtered = enriched.filter { arrival ->
                    val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                    if (t > 0L) t >= cutoff else true
                }

                // Do not update global polylines/busPositions on refresh; keep map empty until user selects a line
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allArrivals = filtered,
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

    // Select/deselect a line to display only that line's polylines and vehicle positions on the map
    fun selectLine(lineId: String?) {
        viewModelScope.launch {
            if (lineId == null) {
                _uiState.value = _uiState.value.copy(selectedLineId = null, selectedPolylines = emptyList(), selectedBusPositions = emptyList())
                return@launch
            }

            val served = _uiState.value.servedLines.filter { it.lineId == lineId }
            val polylines = mutableListOf<com.danieljm.delijn.domain.model.LinePolyline>()

            // Fetch polylines for all directions of the selected line
            for (s in served) {
                try {
                    val ent = s.entiteitnummer ?: continue
                    val lijn = s.lineId
                    val richting = s.richting
                    val resp = try {
                        getLineDirectionStopsUseCase(ent, lijn, richting)
                    } catch (_: Exception) { null }
                    val coords = resp?.haltes?.map { it.latitude to it.longitude } ?: emptyList()
                    val routed: List<Pair<Double, Double>>? = try {
                        if (coords.size >= 2) getRouteGeometryUseCase(coords) else null
                    } catch (_: Exception) {
                        null
                    }
                    val finalCoords = routed ?: coords
                    if (finalCoords.isNotEmpty()) {
                        // try to extract a color for this line from arrivals cache if available
                        val sampleArrival = _uiState.value.allArrivals.firstOrNull { it.lineId == lijn }
                        val colorHex = sampleArrival?.lineBackgroundColorHex
                        polylines.add(com.danieljm.delijn.domain.model.LinePolyline(id = "${ent}|${lijn}|${richting}", coordinates = finalCoords, colorHex = colorHex))
                    }
                } catch (_: Exception) {
                    // ignore individual failures
                }
            }

            // Determine if this line has any upcoming arrivals within the 90-minute window — only then request GPS positions
            val nowMs = System.currentTimeMillis()
            val windowMs = 90 * 60 * 1000L
            val arrivalsForLine = _uiState.value.allArrivals.filter { it.lineId == lineId }
            val arrivalsWithinWindow = arrivalsForLine.filter {
                val t = if (it.realArrivalTime > 0L) it.realArrivalTime else it.expectedArrivalTime
                t in nowMs..(nowMs + windowMs)
            }
            val busPositions = mutableListOf<BusPosition>()
            if (arrivalsWithinWindow.isNotEmpty()) {
                val toQuery = arrivalsWithinWindow.filter { !it.vrtnum.isNullOrBlank() }
                for (a in toQuery) {
                    try {
                        val vid = a.vrtnum ?: continue
                        val pos = try { getVehiclePositionUseCase(vid) } catch (_: Exception) { null }
                        if (pos != null) {
                            busPositions.add(BusPosition(vid, pos.latitude, pos.longitude, pos.bearing))
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }

            // Update UI state inside the coroutine so local variables are in scope
            _uiState.value = _uiState.value.copy(selectedLineId = lineId, selectedPolylines = polylines, selectedBusPositions = busPositions)
        }
    }

    // Merge scheduled and real-time arrival lists:
    // - Keep scheduled arrivals that are within the window
    // - Replace scheduled entries with real-time entries when they represent the same trip
    // - Add real-time-only arrivals
    private fun mergeArrivalsPreferRealtime(
        scheduled: List<com.danieljm.delijn.domain.model.ArrivalInfo>,
        realtime: List<com.danieljm.delijn.domain.model.ArrivalInfo>
    ): List<com.danieljm.delijn.domain.model.ArrivalInfo> {
        val resultMap = linkedMapOf<String, com.danieljm.delijn.domain.model.ArrivalInfo>()

        fun keyFor(a: com.danieljm.delijn.domain.model.ArrivalInfo): String {
            // Use lineId + expectedArrivalTime + destination to dedupe; expectedArrivalTime comes from scheduled time
            return "${a.lineId}_${a.expectedArrivalTime}_${a.destination}"
        }

        // Start with scheduled arrivals (they are marked as schedule-only by mapper)
        for (s in scheduled) {
            val k = keyFor(s)
            resultMap[k] = s
        }

        // For realtime arrivals, prefer them: replace existing scheduled entry with realtime info
        for (r in realtime) {
            val k = keyFor(r)
            // Ensure realtime entries are marked as not schedule-only
            val updated = if (r.isScheduleOnly) r.copy(isScheduleOnly = false) else r
            resultMap[k] = updated
        }

        // Return values in insertion order: scheduled-first, realtime replacements will take place
        return resultMap.values.toList()
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

    private suspend fun buildPolylinesFromCache(): List<LinePolyline> {
        val polylines = mutableListOf<LinePolyline>()
        val unique = mutableSetOf<String>()
        for ((_, candidate) in lineDetailCache) {
            if (candidate == null) continue
            val ent = candidate.entiteitnummer ?: continue
            val lijn = candidate.lijnnummer ?: continue
            val richting = candidate.richting ?: continue
            val key = "$ent|$lijn|$richting"
            if (unique.contains(key)) continue
            unique.add(key)
            try {
                val resp = getLineDirectionStopsUseCase(ent, lijn, richting)
                val coords = resp.haltes.map { it.latitude to it.longitude }

                // Attempt to resolve a routed geometry from the backend OSRM proxy
                val routed: List<Pair<Double, Double>>? = try {
                    if (coords.size >= 2) {
                        getRouteGeometryUseCase(coords)
                    } else null
                } catch (e: Exception) {
                    Log.w("StopDetailViewModel", "Routing failed for $key: ${e.message}")
                    null
                }

                val finalCoords = routed ?: coords

                if (finalCoords.isNotEmpty()) {
                    polylines.add(LinePolyline(id = key, coordinates = finalCoords, colorHex = candidate.kleurAchterGrond))
                }
            } catch (_: Exception) {
                Log.w("StopDetailViewModel", "Failed to fetch haltes for $key")
            }
        }
        return polylines
    }
}

