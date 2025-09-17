package com.danieljm.delijn.ui.screens.stops

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Navigation
import com.danieljm.delijn.R
import com.danieljm.delijn.ui.components.stops.BottomSheet
import com.danieljm.delijn.ui.navigation.Screen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.navigation.NavHostController

@Composable
fun StopsScreen(
    navController: NavHostController,
    viewModel: StopsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val userLocation = uiState.userLocation

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val permissionGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            hasLocationPermission = permissionGranted
        }
    )

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val DpSaver = Saver<Dp, Float>(
        save = { it.value },
        restore = { it.dp }
    )
    var bottomSheetHeight by rememberSaveable(stateSaver = DpSaver) { mutableStateOf(160.dp) }
    var selectedStopId by remember { mutableStateOf<String?>(null) }
    var pendingCenterOnLocation by remember { mutableStateOf(false) }
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    // Request permission or location once when the screen is first composed
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationUpdates(context)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Center map on user location only on first launch or when explicitly requested
    LaunchedEffect(isFirstLaunch, userLocation) {
        if (isFirstLaunch && userLocation != null) {
            moveMapToLocation(mapViewRef, userLocation, isInitialMove = true)
            isFirstLaunch = false
        }
    }

    LaunchedEffect(pendingCenterOnLocation, userLocation) {
        if (pendingCenterOnLocation && userLocation != null) {
            moveMapToLocation(mapViewRef, userLocation, isInitialMove = true)
            pendingCenterOnLocation = false
        }
    }

    // Animate the user's location marker smoothly
    LaunchedEffect(userLocation, mapViewRef) {
        if (userLocation != null && mapViewRef != null) {
            val oldMarker = mapViewRef!!.overlays.filterIsInstance<Marker>().find { it.title == "You are here" }
            val newGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)

            if (oldMarker == null) {
                // Initial placement of the marker
                val marker = Marker(mapViewRef).apply {
                    position = newGeoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                    icon = ContextCompat.getDrawable(mapViewRef!!.context, R.drawable.user_live_dot)
                }
                mapViewRef!!.overlays.add(marker)
                mapViewRef!!.invalidate()
            } else {
                // Animate the existing marker to the new position
                val startPoint = oldMarker.position
                val endPoint = newGeoPoint

                val latAnimatable = Animatable(startPoint.latitude.toFloat())
                val lonAnimatable = Animatable(startPoint.longitude.toFloat())

                coroutineScope.launch {
                    launch {
                        latAnimatable.animateTo(endPoint.latitude.toFloat(), animationSpec = tween(1000)) {
                            oldMarker.position.latitude = value.toDouble()
                            mapViewRef?.invalidate()
                        }
                    }
                    launch {
                        lonAnimatable.animateTo(endPoint.longitude.toFloat(), animationSpec = tween(1000)) {
                            oldMarker.position.longitude = value.toDouble()
                            mapViewRef?.invalidate()
                        }
                    }
                }
            }
        }
    }


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
                    relatedObject = stop.id
                    setOnMarkerClickListener { _, _ ->
                        navController.navigate(Screen.StopDetail.route.replace("{stopId}", stop.id))
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
                mapView.overlays.filterIsInstance<Marker>()
                    .find { marker ->
                        marker.relatedObject == selectedStopId
                    }?.let { marker ->
                        val stop = uiState.nearbyStops.find { it.id == selectedStopId }
                        if (stop != null) {
                            marker.snippet = stop.id
                            marker.showInfoWindow()
                        }
                    }
                mapView.invalidate()
            }
        }
    }

    val defaultZoom = 15.0
    val defaultLat = 50.8792 // Leuven
    val defaultLon = 4.7012  // Leuven
    var mapZoom by rememberSaveable { mutableStateOf(defaultZoom) }
    var mapCenter by rememberSaveable { mutableStateOf(Pair(defaultLat, defaultLon)) }
    val stopsListState = rememberLazyListState() // Correctly using rememberLazyListState

    DisposableEffect(mapViewRef) {
        val mapView = mapViewRef
        if (mapView != null) {
            val listener = object : org.osmdroid.events.MapListener {
                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                    mapCenter = Pair(mapView.mapCenter.latitude, mapView.mapCenter.longitude)
                    return true
                }
                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                    mapZoom = mapView.zoomLevelDouble
                    return true
                }
            }
            mapView.addMapListener(listener)
            onDispose { mapView.removeMapListener(listener) }
        } else {
            onDispose { }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(mapZoom)
                            controller.setCenter(GeoPoint(mapCenter.first, mapCenter.second))
                            mapViewRef = this
                        }
                    },
                    update = { mv ->
                        mapViewRef = mv
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
                uiState.error?.let {
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
                                text = it,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Floating Action Button positioned above the bottom sheet
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            if (userLocation != null) {
                                moveMapToLocation(mapViewRef, userLocation, isInitialMove = true)
                            } else {
                                viewModel.startLocationUpdates(context)
                                pendingCenterOnLocation = true
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    containerColor = Color(0xFF1D2124),
                    contentColor = Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomSheetHeight + 16.dp)
                ) {
                    Icon(Lucide.Navigation, "Center on my location")
                }

                // Bottom sheet overlayed on top of the map
                BottomSheet(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    stops = uiState.nearbyStops,
                    userLat = userLocation?.latitude,
                    userLon = userLocation?.longitude,
                    onHeightChanged = { height -> bottomSheetHeight = height },
                    onStopClick = { stop ->
                        mapViewRef?.controller?.animateTo(GeoPoint(stop.latitude, stop.longitude))
                    },
                    isLoading = uiState.isLoading,
                    shouldAnimateRefresh = uiState.shouldAnimateRefresh,
                    onRefresh = {
                        if(hasLocationPermission) {
                            // Use the new force refresh method instead
                            viewModel.forceLocationUpdateAndRefresh(context)
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    onRefreshAnimationComplete = {
                        viewModel.onRefreshAnimationComplete()
                    },
                    listState = stopsListState
                )
            }
        }
    }
}

private var lastCenteredLocation: Pair<Double, Double>? = null

private fun moveMapToLocation(mapView: MapView?, location: Location?, isInitialMove: Boolean = false) {
    if (mapView == null || location == null) return

    val lat = location.latitude
    val lon = location.longitude

    if (!isInitialMove && lastCenteredLocation?.first == lat && lastCenteredLocation?.second == lon) {
        return
    }

    lastCenteredLocation = lat to lon
    val geo = GeoPoint(lat, lon)
    mapView.controller.animateTo(geo)
    if (isInitialMove) {
        mapView.controller.setZoom(18.0)
    }
}