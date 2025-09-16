package com.danieljm.delijn.ui.screens.stops

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Navigation
import com.google.android.gms.location.LocationServices
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.danieljm.delijn.R
import com.danieljm.delijn.ui.components.stops.BottomSheet

@Composable
fun StopsScreen(
    viewModel: StopsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Track permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }
    )

    // MapView is created inside AndroidView. Keep a reference to call methods from Kotlin code.
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // State to track bottom sheet height for FAB positioning
    var bottomSheetHeight by remember { mutableStateOf(160.dp) }

    // Observe UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Track user's location
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    // Request permission once when composable enters composition (if not granted)
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Track which stop was clicked for line directions
    var selectedStopId by remember { mutableStateOf<String?>(null) }

    // Update map markers when nearby stops change
    LaunchedEffect(uiState.nearbyStops) {
        mapViewRef?.let { mapView ->
            // Remove previous stop markers (keep "You are here" marker)
            val overlaysToRemove = mapView.overlays.filter { it is Marker && it.title != "You are here" }
            overlaysToRemove.forEach { mapView.overlays.remove(it) }

            // Add markers for each nearby stop
            uiState.nearbyStops.forEach { stop ->
                val gp = GeoPoint(stop.latitude, stop.longitude)
                val marker = Marker(mapView).apply {
                    position = gp
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = stop.name
                    snippet = "Stop ID: ${stop.id}"
                    icon = ContextCompat.getDrawable(context, R.drawable.bus_stop)
                    
                    // Store the stop ID in the marker's related object for unique identification
                    relatedObject = stop.id

                    // Set click listener to fetch and show line directions
                    setOnMarkerClickListener { _, _ ->
                        selectedStopId = stop.id
                        viewModel.fetchLineDirectionsForStop(stop.id)
                        // Show the info window immediately
                        showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    }

    // Update marker snippet when line directions are loaded
    LaunchedEffect(uiState.selectedStopLineDirections, selectedStopId) {
        if (uiState.selectedStopLineDirections.isNotEmpty() && selectedStopId != null) {
            mapViewRef?.let { mapView ->
                // Find the marker for the selected stop using the unique stop ID
                mapView.overlays.filterIsInstance<Marker>()
                    .find { marker ->
                        marker.relatedObject == selectedStopId
                    }?.let { marker ->
                        val stop = uiState.nearbyStops.find { it.id == selectedStopId }
                        if (stop != null) {
                            val lineDirections = uiState.selectedStopLineDirections
                            val linesInfo = lineDirections.joinToString("\n") { line ->
                                "${line.lijnnummer} from ${line.from} to ${line.to}"
                            }
                            marker.snippet = stop.id
                            // Show the updated info window
                            marker.showInfoWindow()
                        }
                    }
                mapView.invalidate()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {

                AndroidView(
                    factory = { ctx ->
                        val mv = MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                        }
                        mapViewRef = mv
                        mv
                    },
                    update = { mv ->
                        mapViewRef = mv
                        // If we have permission, try to get last location and center map
                        if (hasLocationPermission && activity != null) {
                            val fused = LocationServices.getFusedLocationProviderClient(activity)
                            try {
                                fused.lastLocation.addOnSuccessListener { location: Location? ->
                                    location?.let { loc ->
                                        userLocation = loc
                                        moveMapToLocation(mv, loc)
                                        // Load cached stops first then fetch live stops using live GPS coordinates
                                        viewModel.loadStopsForLocation(loc.latitude, loc.longitude)
                                    }
                                }
                            } catch (_: SecurityException) {
                                // ignore - permission should be checked before calling
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Show loading indicator
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Keep commented for now
                        //CircularProgressIndicator()
                    }
                }

                // Show error message
                uiState.error?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Floating Action Button positioned above the bottom sheet
                FloatingActionButton(
                    onClick = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        } else if (mapViewRef != null && activity != null) {
                            val fused = LocationServices.getFusedLocationProviderClient(activity)
                            try {
                                fused.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                                    location?.let { loc ->
                                        userLocation = loc
                                        moveMapToLocation(mapViewRef!!, loc)
                                    }
                                }
                            } catch (_: SecurityException) {
                                // ignore
                            }
                        }
                    },
                    containerColor = Color(0xFF1D2124),
                    contentColor = Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomSheetHeight + 16.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Navigation,
                        contentDescription = "Center on my location",
                        tint = Color(0xFFBDBDBD)
                    )
                }

                // Bottom sheet overlayed on top of the map
                BottomSheet(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    stops = uiState.nearbyStops,
                    userLat = userLocation?.latitude,
                    userLon = userLocation?.longitude,
                    onHeightChanged = { height ->
                        bottomSheetHeight = height
                    },
                    onStopClick = { stop ->
                        val geoPoint = GeoPoint(stop.latitude, stop.longitude)
                        mapViewRef?.controller?.animateTo(geoPoint)
                    },
                    isLoading = uiState.isLoading,
                    shouldAnimateRefresh = uiState.shouldAnimateRefresh,
                    onRefreshAnimationComplete = { viewModel.onRefreshAnimationComplete() },
                    onRefresh = {
                        // If we have a known user location, use it directly
                        if (userLocation != null) {
                            viewModel.fetchNearbyStops(userLocation!!.latitude, userLocation!!.longitude)
                        } else if (hasLocationPermission && activity != null) {
                            // Try to get last known location and then fetch
                            val fused = LocationServices.getFusedLocationProviderClient(activity)
                            try {
                                fused.lastLocation.addOnSuccessListener { location: Location? ->
                                    location?.let { loc ->
                                        userLocation = loc
                                        viewModel.fetchNearbyStops(loc.latitude, loc.longitude)
                                    }
                                }
                            } catch (_: SecurityException) {
                                // ignore
                            }
                        } else {
                            // No permission, request it
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                )
            }

            if (!hasLocationPermission) {
                Text(
                    text = "Location permission required to show your position on the map.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text("Grant location")
                }
            }
        }
    }
}

private fun moveMapToLocation(mapView: MapView, location: Location) {
    val geo = GeoPoint(location.latitude, location.longitude)
    mapView.controller.apply {
        setZoom(18.0)
        animateTo(geo)
    }
    // Remove previous user marker(s) we added - using a safe approach for CopyOnWriteArrayList
    val overlaysToRemove = mapView.overlays.filter { overlay ->
        overlay is Marker && overlay.title == "You are here"
    }
    overlaysToRemove.forEach { overlay ->
        mapView.overlays.remove(overlay)
    }
    
    val marker = Marker(mapView).apply {
        position = geo
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "You are here"
        icon = ContextCompat.getDrawable(mapView.context, R.drawable.user_live_gps)
    }
    mapView.overlays.add(marker)
    mapView.invalidate()
}