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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Import types used by the floating selector UI
import com.danieljm.delijn.ui.components.stopdetails.FloatingBusItem
import com.danieljm.delijn.ui.components.stopdetails.BusIconEntry

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

    // Job used to poll a single selected vehicle position periodically while selected
    private var selectedVehiclePollJob: Job? = null

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
                    // window variables are not needed here; arrivals are filtered later
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
                    val twoMinutesMs = 2 * 60 * 1000L
                    val cutoff = System.currentTimeMillis() - twoMinutesMs
                    val filtered = enriched.filter { arrival ->
                        val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                        // If we have a timestamp, exclude arrivals that are 10 minutes old or older (strictly greater-than cutoff required to keep)
                        if (t > 0L) {
                            t > cutoff
                        } else {
                            // unknown timestamp: keep it
                            true
                        }
                    }

                    // Determine arrivals within the time window and attempt to seed initial busPositions so the
                    // floating selector can show accurate hasGps/departed state immediately on first open.
                    val nowMs = System.currentTimeMillis()
                    val windowMs = 45 * 60 * 1000L
                    val recentPastMs = 2 * 60 * 1000L
                    val arrivalsWithinWindowAll = filtered.filter { a ->
                        val t = if (a.realArrivalTime > 0L) a.realArrivalTime else a.expectedArrivalTime
                        t in (nowMs - recentPastMs)..(nowMs + windowMs)
                    }

                    val initialBusPositions = mutableListOf<BusPosition>()
                    if (arrivalsWithinWindowAll.isNotEmpty()) {
                        val toQuery = arrivalsWithinWindowAll.mapNotNull { it.vrtnum }.distinct()
                        for (vid in toQuery) {
                            try {
                                val pos = try { getVehiclePositionUseCase(vid) } catch (_: Exception) { null }
                                if (pos != null) {
                                    initialBusPositions.add(BusPosition(vid, pos.latitude, pos.longitude, pos.bearing))
                                }
                            } catch (_: Exception) {
                                // ignore individual failures
                            }
                        }
                    }

                    // Update UI state with initial positions and vehiclesWithGps set based on what we fetched
                    val fetchedIds = initialBusPositions.map { it.vehicleId }.toSet()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stopName = stopName,
                        servedLines = servedLines,
                        allArrivals = filtered,
                        lastArrivalsRefreshMillis = System.currentTimeMillis(),
                        stopLatitude = stop.latitude,
                        stopLongitude = stop.longitude,
                        busPositions = initialBusPositions,
                        vehiclesWithGps = _uiState.value.vehiclesWithGps + fetchedIds
                    )

                    // Compute initial floating bus selector data using the positions we just fetched so UI shows accurate GPS flags
                    val (floatingBusItems, floatingBusIcons) = computeFloatingBusSelectorData(
                        arrivals = filtered,
                        allBusPositions = initialBusPositions,
                        vehiclesWithGps = _uiState.value.vehiclesWithGps + fetchedIds,
                        selectedLineId = null
                    )
                    _uiState.value = _uiState.value.copy(
                        floatingBusItems = floatingBusItems,
                        floatingBusIcons = floatingBusIcons
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
                val twoMinutesMs = 2 * 60 * 1000L
                val cutoff = System.currentTimeMillis() - twoMinutesMs
                val filtered = enriched.filter { arrival ->
                    val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                    if (t > 0L) t > cutoff else true
                }

                // Do not update global polylines/busPositions on refresh; keep map empty until user selects a line
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allArrivals = filtered,
                    servedLines = servedLines,
                    lastArrivalsRefreshMillis = System.currentTimeMillis()
                )

                // Recompute floating selector data after arrivals refresh using current known bus positions
                val allBusPositionsAfterRefresh = _uiState.value.busPositions + _uiState.value.selectedBusPositions
                val (refItems, refIcons) = computeFloatingBusSelectorData(
                    arrivals = filtered,
                    allBusPositions = allBusPositionsAfterRefresh,
                    vehiclesWithGps = _uiState.value.vehiclesWithGps,
                    selectedLineId = _uiState.value.selectedLineId
                )
                _uiState.value = _uiState.value.copy(floatingBusItems = refItems, floatingBusIcons = refIcons)
            } catch (e: Exception) {
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
                // clear selection and cancel any per-vehicle polling
                selectedVehiclePollJob?.cancel()
                selectedVehiclePollJob = null
                _uiState.value = _uiState.value.copy(selectedLineId = null, selectedPolylines = emptyList(), selectedBusPositions = emptyList(), busVehicleId = null)
                return@launch
            }

            val served = _uiState.value.servedLines.filter { it.lineId == lineId }
            val polylines = mutableListOf<LinePolyline>()

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
                        // Use the route color (kleurAchterGrondRand) for polylines shown on the map
                        val colorHex = sampleArrival?.lineRouteColorHex
                        polylines.add(LinePolyline(id = "${ent}|${lijn}|${richting}", coordinates = finalCoords, colorHex = colorHex))
                    }
                } catch (_: Exception) {
                    // ignore individual failures
                }
            }

            // Determine if this line has any upcoming arrivals within the 45-minute window — only then request GPS positions
            val nowMs = System.currentTimeMillis()
            val windowMs = 45 * 60 * 1000L
            val recentPastMs = 2 * 60 * 1000L
            val arrivalsForLine = _uiState.value.allArrivals.filter { it.lineId == lineId }
            val arrivalsWithinWindow = arrivalsForLine.filter {
                val t = if (it.realArrivalTime > 0L) it.realArrivalTime else it.expectedArrivalTime
                // include arrivals that occurred up to recentPastMs ago, and upcoming arrivals within windowMs
                t in (nowMs - recentPastMs)..(nowMs + windowMs)
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
                        // ignore individual failures
                    }
                }
            }

            // Update the UI state with refreshed polylines and positions
            val currentVehicles = _uiState.value.vehiclesWithGps
            val fetchedIds = busPositions.map { it.vehicleId }.toSet()
            val newVehicles = currentVehicles + fetchedIds
            Log.d("StopDetailViewModel", "refresh: selectedLine=$lineId fetchedIds=$fetchedIds vehiclesWithGps(before)=$currentVehicles vehiclesWithGps(after)=$newVehicles")
            _uiState.value = _uiState.value.copy(
                selectedLineId = lineId,
                selectedPolylines = polylines,
                selectedBusPositions = busPositions,
                vehiclesWithGps = newVehicles
            )

            // Recompute floating selector after we fetched positions for the selected line
            val allBusPositionsNow = (_uiState.value.busPositions + busPositions + _uiState.value.selectedBusPositions).distinctBy { it.vehicleId }
            val (selItems, selIcons) = computeFloatingBusSelectorData(
                arrivals = _uiState.value.allArrivals,
                allBusPositions = allBusPositionsNow,
                vehiclesWithGps = _uiState.value.vehiclesWithGps,
                selectedLineId = _uiState.value.selectedLineId
            )
            _uiState.value = _uiState.value.copy(floatingBusItems = selItems, floatingBusIcons = selIcons)
        }
    }

    // Allow selecting an individual bus vehicle; if vehicleId==null clear the selection
    // Replace the selectBus method in StopDetailViewModel.kt:

    fun selectBus(vehicleId: String?) {
        // update selected vehicle id in state
        _uiState.value = _uiState.value.copy(busVehicleId = vehicleId)

        // Cancel any previous polling job
        selectedVehiclePollJob?.cancel()
        selectedVehiclePollJob = null

        // If clearing selection, make sure we also clear any selectedBusPositions so the map shows all buses again
        if (vehicleId == null) {
            _uiState.value = _uiState.value.copy(selectedBusPositions = emptyList())

            // Recompute floating selector to show all buses again
            val allBusPos = _uiState.value.busPositions + _uiState.value.selectedBusPositions
            val (clearItems, clearIcons) = computeFloatingBusSelectorData(
                arrivals = _uiState.value.allArrivals,
                allBusPositions = allBusPos,
                vehiclesWithGps = _uiState.value.vehiclesWithGps,
                selectedLineId = _uiState.value.selectedLineId
            )
            _uiState.value = _uiState.value.copy(floatingBusItems = clearItems, floatingBusIcons = clearIcons)
            return
        }

        // Try to seed selectedBusPositions immediately from any already-known positions (global or previous selected)
        val existing = (_uiState.value.busPositions + _uiState.value.selectedBusPositions).find { it.vehicleId == vehicleId }
        if (existing != null) {
            _uiState.value = _uiState.value.copy(selectedBusPositions = listOf(existing))

            // Recompute floating selector to reflect the selection immediately
            val allBusPosNow = _uiState.value.busPositions + _uiState.value.selectedBusPositions
            val (selItems2, selIcons2) = computeFloatingBusSelectorData(
                arrivals = _uiState.value.allArrivals,
                allBusPositions = allBusPosNow,
                vehiclesWithGps = _uiState.value.vehiclesWithGps,
                selectedLineId = _uiState.value.selectedLineId
            )
            _uiState.value = _uiState.value.copy(floatingBusItems = selItems2, floatingBusIcons = selIcons2)
        }

        // If a vehicle was selected, mark it as having GPS and start polling
        if (!vehicleId.isNullOrBlank()) {
            // Fix: Update vehiclesWithGps in a separate state update to ensure it's captured
            val currentVehicles = _uiState.value.vehiclesWithGps
            val newVehicles = currentVehicles + setOf(vehicleId)
            _uiState.value = _uiState.value.copy(vehiclesWithGps = newVehicles)

            // Recompute selector to immediately mark this vehicle as having GPS
            val allBusPosThen = _uiState.value.busPositions + _uiState.value.selectedBusPositions
            val (imItems, imIcons) = computeFloatingBusSelectorData(
                arrivals = _uiState.value.allArrivals,
                allBusPositions = allBusPosThen,
                vehiclesWithGps = newVehicles,
                selectedLineId = _uiState.value.selectedLineId
            )
            _uiState.value = _uiState.value.copy(floatingBusItems = imItems, floatingBusIcons = imIcons)

            selectedVehiclePollJob = viewModelScope.launch {
                // Immediate position fetch
                try {
                    val pos = try { getVehiclePositionUseCase(vehicleId) } catch (_: Exception) { null }
                    if (pos != null) {
                        val bp = BusPosition(vehicleId, pos.latitude, pos.longitude, pos.bearing)
                        val merged = (_uiState.value.selectedBusPositions.filter { it.vehicleId != bp.vehicleId } + bp)
                        _uiState.value = _uiState.value.copy(
                            selectedBusPositions = merged,
                            vehiclesWithGps = _uiState.value.vehiclesWithGps + setOf(bp.vehicleId)
                        )

                        // Recompute after we fetched the immediate vehicle position
                        val allBusPosAfter = _uiState.value.busPositions + _uiState.value.selectedBusPositions
                        val (pollItems, pollIcons) = computeFloatingBusSelectorData(
                            arrivals = _uiState.value.allArrivals,
                            allBusPositions = allBusPosAfter,
                            vehiclesWithGps = _uiState.value.vehiclesWithGps,
                            selectedLineId = _uiState.value.selectedLineId
                        )
                        _uiState.value = _uiState.value.copy(floatingBusItems = pollItems, floatingBusIcons = pollIcons)
                    }
                } catch (_: Exception) {
                    // ignore initial fetch failure
                }

                // Continue with periodic polling
                while (true) {
                    try {
                        val pos = try { getVehiclePositionUseCase(vehicleId) } catch (_: Exception) { null }
                        if (pos != null) {
                            val bp = BusPosition(vehicleId, pos.latitude, pos.longitude, pos.bearing)
                            val merged = (_uiState.value.selectedBusPositions.filter { it.vehicleId != bp.vehicleId } + bp)
                            _uiState.value = _uiState.value.copy(
                                selectedBusPositions = merged,
                                vehiclesWithGps = _uiState.value.vehiclesWithGps + setOf(bp.vehicleId)
                            )
                        }
                    } catch (_: Exception) {
                        // ignore per-iteration failures
                    }
                    delay(10_000L)
                }
            }
        }
    }

    override fun onCleared() {
        selectedVehiclePollJob?.cancel()
        selectedVehiclePollJob = null
        super.onCleared()
    }

    // Compute the floating selector data (items + per-line icons) from arrivals and known bus positions
    private fun computeFloatingBusSelectorData(
        arrivals: List<com.danieljm.delijn.domain.model.ArrivalInfo>,
        allBusPositions: List<BusPosition>,
        vehiclesWithGps: Set<String>,
        selectedLineId: String?
    ): Pair<List<FloatingBusItem>, Map<String, List<BusIconEntry>>> {
        if (arrivals.isEmpty()) return Pair(emptyList(), emptyMap())

        val nowMs = System.currentTimeMillis()
        val windowMs = 45 * 60 * 1000L
        val recentPastMs = 2 * 60 * 1000L

        // Build items grouped by lineId
        val byLine = arrivals.groupBy { it.lineId }
        val items = mutableListOf<FloatingBusItem>()
        val iconsMap = mutableMapOf<String, List<BusIconEntry>>()

        for ((lineId, arrs) in byLine) {
            val a = arrs.first()
            items.add(
                FloatingBusItem(
                    id = lineId,
                    displayText = a.lineNumberPublic ?: lineId,
                    bgHex = a.lineBackgroundColorHex,
                    fgHex = a.lineForegroundColorHex,
                    borderHex = a.lineBackgroundBorderColorHex
                )
            )

            // Prepare icons for upcoming arrivals within window (and a small recent past)
            val iconsForLine = arrs
                .filter { arrival ->
                    val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                    // keep unknown timestamps as well
                    if (t <= 0L) return@filter true
                    t in (nowMs - recentPastMs)..(nowMs + windowMs)
                }
                .sortedWith(compareBy { a ->
                    when {
                        a.realArrivalTime > 0L -> a.realArrivalTime
                        a.expectedArrivalTime > 0L -> a.expectedArrivalTime
                        else -> Long.MAX_VALUE
                    }
                })
                .map { arrival ->
                    val t = if (arrival.realArrivalTime > 0L) arrival.realArrivalTime else arrival.expectedArrivalTime
                    val badge = if (t <= 0L) {
                        "?"
                    } else {
                        val minutes = ((t - nowMs) / 60_000L).toInt()
                        if (minutes < 0) "Departed" else minutes.toString()
                    }
                    val vid = arrival.vrtnum
                    val hasGps = !vid.isNullOrBlank() && (vehiclesWithGps.contains(vid) || allBusPositions.any { it.vehicleId == vid })
                    BusIconEntry(vehicleId = vid, badge = badge, hasGps = hasGps)
                }

            iconsMap[lineId] = iconsForLine
        }

        // Also include any servedLines that may not appear in arrivals? (Keep it simple: items only derived from arrivals)

        return Pair(items, iconsMap)
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
        // Do NOT clear the cache on forced refresh. Clearing the cache and then
        // attempting fresh network requests for every arrival can cause transient
        // failures (rate limits, network errors) to remove previously successful
        // enrichments and result in UI losing colors/public names. Instead, when
        // forceRefresh is requested, try to fetch fresh data but fall back to any
        // previously cached value if the network call fails or returns no candidate.

        fun lineIdsMatch(a: String?, b: String?): Boolean {
            if (a == null || b == null) return false
            val ta = a.trim()
            val tb = b.trim()
            if (ta.equals(tb, ignoreCase = true)) return true
            return try { ta.toIntOrNull() == tb.toIntOrNull() } catch (e: Exception) { false }
        }

        // Resolve metadata per unique key to avoid many network calls when arrivals contain duplicates
        val uniqueKeys = arrivals.map { "${it.lineId}|${it.omschrijving}" }.toSet()
        val resolved = mutableMapOf<String, com.danieljm.delijn.domain.model.LineDirectionSearch?>()

        for (key in uniqueKeys) {
            val parts = key.split('|', limit = 2)
            val lineId = parts.getOrNull(0)
            val omschrijving = parts.getOrNull(1) ?: ""

            val existingCached = cache[key]
            if (!forceRefresh && existingCached != null) {
                resolved[key] = existingCached
                continue
            }

            // Try search endpoint
            val candidate = try {
                val resp = getLineDirectionsSearchUseCase(omschrijving)
                val found = resp.lijnrichtingen.find { lr ->
                    lineIdsMatch(lr.lijnnummer, lineId) &&
                        (lr.omschrijving?.equals(omschrijving.orEmpty(), ignoreCase = true) == true ||
                            lr.omschrijving?.contains(omschrijving.orEmpty(), ignoreCase = true) == true)
                } ?: resp.lijnrichtingen.firstOrNull()
                if (found != null) cache[key] = found
                found
            } catch (e: Exception) {
                Log.w("StopDetailViewModel", "Search API failed for key=$key", e)
                null
            }

            if (candidate != null) {
                resolved[key] = candidate
                continue
            }

            // Fallback: try to match servedLines and fetch detail for that specific line-direction
            val servedCandidate = servedLines.find { sl ->
                lineIdsMatch(sl.lineId, lineId) &&
                    (sl.omschrijving.equals(omschrijving.orEmpty(), ignoreCase = true) || sl.omschrijving.contains(omschrijving.orEmpty(), ignoreCase = true))
            }
            if (servedCandidate != null) {
                try {
                    val detail = getLineDirectionDetailUseCase(servedCandidate.entiteitnummer, servedCandidate.lineId, servedCandidate.richting)
                    if (detail != null) {
                        cache[key] = detail
                        resolved[key] = detail
                        continue
                    }
                } catch (e: Exception) {
                    Log.w("StopDetailViewModel", "Detail API failed for ${servedCandidate.lineId}", e)
                }
            }

            // As last resort, keep any existing cached value (could be null)
            resolved[key] = existingCached
        }

        // Apply resolved metadata back to arrivals
        return arrivals.map { arrival ->
            val key = "${arrival.lineId}|${arrival.omschrijving}"
            val searchResult = resolved[key]
            if (searchResult == null) arrival else {
                arrival.copy(
                    lineNumberPublic = searchResult.lijnNummerPubliek,
                    // Keep the badge/container color as the original background color
                    lineBackgroundColorHex = searchResult.kleurAchterGrond,
                    // Expose the Rand color for use as the map polyline and badge border
                    lineRouteColorHex = searchResult.kleurAchterGrondRand,
                    lineBackgroundBorderColorHex = searchResult.kleurAchterGrondRand,
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
                    // Use the Rand (border) color for routes on the map
                    polylines.add(LinePolyline(id = key, coordinates = finalCoords, colorHex = candidate.kleurAchterGrondRand))
                }
            } catch (_: Exception) {
                Log.w("StopDetailViewModel", "Failed to fetch haltes for $key")
            }
        }
        return polylines
    }
}
