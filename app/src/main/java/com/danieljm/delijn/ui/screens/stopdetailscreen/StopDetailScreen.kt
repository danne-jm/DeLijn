package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.danieljm.delijn.R
import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.ui.components.map.CustomMarker
import com.danieljm.delijn.ui.components.map.MapComponent
import com.danieljm.delijn.ui.components.map.MapState
import com.danieljm.delijn.ui.components.map.MapPolyline
import com.danieljm.delijn.ui.components.stopdetails.BusArrivalsBottomSheet
import com.danieljm.delijn.ui.components.stopdetails.FloatingBusItem
import com.danieljm.delijn.ui.components.stopdetails.FloatingBusSelectorRow
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import org.koin.androidx.compose.koinViewModel

// New imports for location handling via DI
import android.location.Location
import com.danieljm.delijn.data.location.LocationProvider
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    stopId: String,
    stopName: String,
    viewModel: StopDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(stopId, stopName) {
        viewModel.loadStopDetails(stopId, stopName)
    }

    val context = LocalContext.current
    val view = LocalView.current
    val arrivalsListState = rememberLazyListState()

    // Map state management using the same state type as StopsScreen
    var mapState by rememberSaveable(stateSaver = MapState.Saver) {
        mutableStateOf(MapState(zoom = 18.0))
    }

    // Location handling: obtain LocationProvider from Koin and collect last-known + updates
    val locationProvider = get<LocationProvider>()
    var userLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(locationProvider) {
        try {
            // Try to get last known location first
            val last = try { locationProvider.getLastKnownLocation() } catch (_: Exception) { null }
            if (last != null) userLocation = last

            // Then collect continuous updates (this will suspend until cancelled)
            try {
                locationProvider.locationUpdates().collect { loc ->
                    userLocation = loc
                }
            } catch (_: Exception) {
                // ignore collection errors; LocationProviderImpl already logs permission issues
            }
        } catch (_: Exception) {
            // ignore any errors obtaining location
        }
    }

    // Set status bar color to match top app bar and restore on dispose
    val statusBarColor = Color(0xFF1D2124)
    DisposableEffect(Unit) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        val originalStatusBarColor = window?.statusBarColor
        val originalLightStatusBars = window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars
        }

        // Set the dark status bar for this screen
        window?.let {
            it.statusBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }

        // Cleanup function to restore original values when leaving the screen
        onDispose {
            window?.let {
                originalStatusBarColor?.let { color -> it.statusBarColor = color }
                originalLightStatusBars?.let { isLight ->
                    WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = isLight
                }
            }
        }
    }

    // Create a stable, mutable list of bus markers that we will update in-place
    val busMarkersState: SnapshotStateList<CustomMarker> = remember { androidx.compose.runtime.mutableStateListOf() }

    // Update markers in-place whenever the relevant parts of uiState change.
    // Using LaunchedEffect keeps the same list instance so MapComponent can update markers
    // without needing a full reinitialization.
    LaunchedEffect(uiState.busPositions, uiState.selectedBusPositions, uiState.selectedLineId, uiState.busVehicleId) {
        // Build a stable list of known positions (prioritize selectedBusPositions then global busPositions)
        val combined = ArrayList<BusPosition>()
        // add selected first for priority
        for (bp in uiState.selectedBusPositions) {
            combined.add(bp)
        }
        // add global positions if we don't already have that vehicleId
        for (bp in uiState.busPositions) {
            val found = combined.any { existing -> existing.vehicleId == bp.vehicleId }
            if (!found) combined.add(bp)
        }

        // Now determine which positions to show
        val positionsToShow = when {
            // If a specific vehicle is selected, show only that vehicle
            uiState.busVehicleId != null -> combined.filter { it.vehicleId == uiState.busVehicleId }

            // If a line is selected but no specific vehicle, show positions for vehicles that belong to that line only
            uiState.selectedLineId != null -> {
                val vehiclesForLine = uiState.allArrivals.filter { arrival -> arrival.lineId == uiState.selectedLineId }
                    .mapNotNull { arrival -> arrival.vrtnum }
                    .toSet()
                combined.filter { bp -> vehiclesForLine.contains(bp.vehicleId) }
            }

            // Default: show all combined positions
            else -> combined
        }

        val newMarkers = positionsToShow.map { bus ->
            CustomMarker(
                id = "bus_${bus.vehicleId}",
                latitude = bus.latitude,
                longitude = bus.longitude,
                title = "Bus ${bus.vehicleId}",
                snippet = "Bus ID: ${bus.vehicleId}",
                iconResourceId = R.drawable.bus_top_perspective,
                rotation = bus.bearing + 180f // Add 180 degrees to flip the direction
            )
        }

        // Update the snapshot state list in-place to maintain the same instance.
        busMarkersState.clear()
        busMarkersState.addAll(newMarkers)
    }

    // Create the current stop as a Stop object for centering
    val currentStop = remember(uiState.stopLatitude, uiState.stopLongitude) {
        val lat = uiState.stopLatitude
        val lon = uiState.stopLongitude
        if (lat != null && lon != null) {
            Stop(
                id = stopId,
                name = stopName,
                latitude = lat,
                longitude = lon,
                entiteitnummer = "",
                halteNummer = ""
            )
        } else null
    }

    // Automatically refresh arrivals every 30 seconds
    LaunchedEffect(uiState.lastArrivalsRefreshMillis) {
        val refreshIntervalMillis = 30_000L
        val startTime = uiState.lastArrivalsRefreshMillis ?: 0L
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = refreshIntervalMillis - elapsed
            if (remaining <= 0) {
                // Trigger auto-refresh and animation
                viewModel.refreshArrivals(force = true)
                break
            }
            // Check every second
            kotlinx.coroutines.delay(1000)
        }
    }

    // Use Scaffold to properly handle the top app bar and content layout
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Stop",
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Heart action */ }) {
                        Icon(Lucide.Heart, contentDescription = "Favorite", tint = Color.White.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1D2124)
                )
            )
        }
    ) { paddingValues ->
        // Content area that automatically accounts for the top app bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Use the shared MapComponent instead of creating a new map
            MapComponent(
                modifier = Modifier.fillMaxSize(),
                stops = listOfNotNull(currentStop),
                customMarkers = busMarkersState,
                polylines = (if (uiState.selectedPolylines.isNotEmpty()) uiState.selectedPolylines else uiState.polylines).map { lp ->
                    MapPolyline(
                        id = lp.id,
                        coordinates = lp.coordinates,
                        colorHex = lp.colorHex
                    )
                },
                mapState = mapState,
                onMapStateChanged = { newState -> mapState = newState },
                centerOnStop = currentStop,
                mapCenterOffset = 250.0,
                userLocation = userLocation,
                showUserLocationMarker = true,
                darkMode = isSystemInDarkTheme()
            )

            // Floating bus selector row at top of screen (above map content)
            val items = remember(uiState.allArrivals, uiState.servedLines) {
                val byLine = uiState.allArrivals.groupBy { it.lineId }
                val list = mutableListOf<FloatingBusItem>()
                // First create items from arrivals (have colors)
                for ((lineId, arrivals) in byLine) {
                    val a = arrivals.first()
                    list.add(
                        FloatingBusItem(
                            id = lineId,
                            displayText = a.lineNumberPublic ?: lineId,
                            bgHex = a.lineBackgroundColorHex,
                            fgHex = a.lineForegroundColorHex,
                            borderHex = a.lineBackgroundBorderColorHex
                        )
                    )
                }
                // Add any servedLines not present in arrivals (fallback)
                for (s in uiState.servedLines) {
                    if (list.none { it.id == s.lineId }) {
                        list.add(FloatingBusItem(id = s.lineId, displayText = s.lineName, bgHex = null, fgHex = null, borderHex = null))
                    }
                }
                list
            }

            // Use ViewModel-provided, precomputed floating selector data so UI and map refreshes are synchronized
            val selectorItems = if (uiState.floatingBusItems.isNotEmpty()) uiState.floatingBusItems else items

            FloatingBusSelectorRow(
                items = selectorItems,
                selected = uiState.selectedLineId,
                itemsIcons = uiState.floatingBusIcons,
                selectedVehicleId = uiState.busVehicleId,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                onToggle = { selectedId ->
                    viewModel.selectLine(selectedId)
                    // clear any individual vehicle selection when selecting a new line
                    viewModel.selectBus(null)
                },
                onBusSelected = { vehicleId ->
                    // Delegate vehicle selection to the ViewModel. ViewModel will mark vehiclesWithGps and fetch positions.
                    viewModel.selectBus(vehicleId)
                }
            )

            // Error overlay positioned at the top of the content area
            uiState.error?.let { err ->
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Error: $err",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Sheet positioned at the bottom

            // Sort arrivals for the bottom sheet: prefer real-time arrival timestamp when available, otherwise fall back to expected/scheduled time
            val sortedArrivals = remember(uiState.allArrivals) {
                uiState.allArrivals.sortedWith(compareBy { a ->
                    when {
                        a.realArrivalTime > 0L -> a.realArrivalTime
                        a.expectedArrivalTime > 0L -> a.expectedArrivalTime
                        else -> Long.MAX_VALUE
                    }
                })
            }

            BusArrivalsBottomSheet(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                arrivals = sortedArrivals,
                isLoading = uiState.isLoading,
                onHeightChanged = { /* no-op in detail screen */ },
                listState = arrivalsListState,
                stopName = stopName,
                stopId = uiState.stopId,
                shouldAnimateRefresh = uiState.shouldAnimateRefresh,
                onRefresh = { viewModel.refreshArrivals(force = true) },
                onRefreshAnimationComplete = { viewModel.onRefreshAnimationComplete() },
                lastArrivalsRefreshMillis = uiState.lastArrivalsRefreshMillis
            )
        }
    }
}
