package com.danieljm.delijn.ui.screens.stops

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Navigation
import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.ui.components.map.MapComponent
import com.danieljm.delijn.ui.components.map.MapState
import com.danieljm.delijn.ui.components.stops.BottomSheet
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.delay

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

    val DpSaver = Saver<Dp, Float>(
        save = { it.value },
        restore = { it.dp }
    )
    var bottomSheetHeight by rememberSaveable(stateSaver = DpSaver) { mutableStateOf(160.dp) }
    var pendingCenterOnLocation by remember { mutableStateOf(false) }

    // Map state management
    var mapState by rememberSaveable(stateSaver = MapState.Saver) {
        mutableStateOf(MapState(zoom = 15.0))
    }
    var animateToLocationTrigger by remember { mutableStateOf<Location?>(null) }
    var animateToStopTrigger by remember { mutableStateOf<Stop?>(null) }
    var hasAutoCenteredOnUser by rememberSaveable { mutableStateOf(false) }

    val stopsListState = rememberLazyListState()

    // New: whether the user is manually navigating the map (panning/zooming)
    var userIsNavigatingMap by rememberSaveable { mutableStateOf(false) }
    // Debounced pending center when user navigates
    var pendingMapCenter by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Request permission or location once when the screen is first composed
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationUpdates(context)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Handle pending center on location
    LaunchedEffect(pendingCenterOnLocation, userLocation) {
        if (pendingCenterOnLocation && userLocation != null) {
            animateToLocationTrigger = userLocation
            pendingCenterOnLocation = false
        }
    }

    // Auto-center on user location on first launch
    LaunchedEffect(userLocation) {
        if (!hasAutoCenteredOnUser && userLocation != null) {
            animateToLocationTrigger = userLocation
            mapState = mapState.copy(
                centerLatitude = userLocation.latitude,
                centerLongitude = userLocation.longitude,
                zoom = 18.0
            )
            hasAutoCenteredOnUser = true
        }
    }

    // When the user manually navigates and we get a new pending center, debounce and fetch nearby stops
    LaunchedEffect(pendingMapCenter) {
        val center = pendingMapCenter
        if (center != null) {
            // debounce so we don't spam API while the user is actively panning
            delay(700L)
            // If still the same center after debounce, fetch
            if (pendingMapCenter == center) {
                viewModel.fetchNearbyStops(center.first, center.second, fromMapNavigation = true)
                // clear pending
                pendingMapCenter = null
            }
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

                // Use the new reusable MapComponent
                MapComponent(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = userLocation,
                    stops = uiState.nearbyStops,
                    mapState = mapState,
                    onMapStateChanged = { newState ->
                        // Update the UI map state
                        mapState = newState

                        // If user is navigating the map (they interacted), prepare a debounced fetch for the new center
                        if (userIsNavigatingMap && newState.centerLatitude != null && newState.centerLongitude != null) {
                            pendingMapCenter = newState.centerLatitude to newState.centerLongitude
                        }
                    },
                    onStopMarkerClick = { stop ->
                        navController.navigate("stopDetail/${stop.id}/${Uri.encode(stop.name)}")
                    },
                    animateToLocation = animateToLocationTrigger,
                    animateToStop = animateToStopTrigger,
                    showUserLocationMarker = true,
                    darkMode = isSystemInDarkTheme(),
                    onUserInteraction = {
                        // User touched the map: switch to navigation mode so that map center drives nearby stop results
                        userIsNavigatingMap = true
                    }
                )

                // Clear animation triggers after they've been processed
                LaunchedEffect(animateToLocationTrigger) {
                    if (animateToLocationTrigger != null) {
                        animateToLocationTrigger = null
                    }
                }

                LaunchedEffect(animateToStopTrigger) {
                    if (animateToStopTrigger != null) {
                        animateToStopTrigger = null
                    }
                }

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
                            // When user taps the FAB, re-enable following the user and center map on their current location
                            userIsNavigatingMap = false

                            if (userLocation != null) {
                                animateToLocationTrigger = userLocation
                                mapState = mapState.copy(
                                    centerLatitude = userLocation.latitude,
                                    centerLongitude = userLocation.longitude,
                                    zoom = 18.0
                                )
                                // Also fetch stops for the user's location
                                viewModel.fetchNearbyStops(userLocation.latitude, userLocation.longitude)
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
                        // When programmatically centering on a stop, disable map-navigation mode so further drag doesn't immediately override
                        userIsNavigatingMap = false
                        animateToStopTrigger = stop
                        mapState = mapState.copy(
                            centerLatitude = stop.latitude,
                            centerLongitude = stop.longitude
                        )
                        // Fetch stops around the stop we centered on
                        viewModel.fetchNearbyStops(stop.latitude, stop.longitude)
                    },
                    isLoading = uiState.isLoading,
                    shouldAnimateRefresh = uiState.shouldAnimateRefresh,
                    onRefresh = {
                        if(hasLocationPermission) {
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
