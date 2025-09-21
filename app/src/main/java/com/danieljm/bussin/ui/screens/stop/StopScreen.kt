package com.danieljm.bussin.ui.screens.stop

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Navigation
import com.danieljm.bussin.ui.components.map.MapViewModel
import com.danieljm.bussin.ui.components.map.OpenStreetMap
import com.danieljm.delijn.ui.components.stops.BottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple screen that displays nearby stops and lets the user request a load.
 * The screen only talks to its ViewModel and observes an immutable UiState.
 */

@Composable
fun StopScreen(
    modifier: Modifier = Modifier,
    stopViewModel: StopViewModel = hiltViewModel(),
    onStopSelected: (String) -> Unit = {},
) {
    val state by stopViewModel.uiState.collectAsState()

    // Obtain MapViewModel via Hilt Compose helper so it respects lifecycle and DI
    val mapViewModel: MapViewModel = hiltViewModel()
    val userLocation by mapViewModel.location.collectAsState()

    val ctx = LocalContext.current

    // Trigger for BottomSheet refresh animation
    val refreshAnimRequested = remember { mutableStateOf(false) }

    // Track bottom sheet height so we can position the FAB above it
    var bottomSheetHeight by remember { mutableStateOf(0.dp) }

    // Recenter trigger counter (increment to request recenter)
    val recenterTrigger = remember { mutableStateOf(0) }
    // If we requested a recenter but have no user location yet, remember to center when location arrives
    val pendingCenterOnLocation = remember { mutableStateOf(false) }

    // Keep last map-center triggered fetch time to avoid spamming the server when user pans repeatedly
    val lastCenterFetchMs = remember { mutableStateOf(0L) }
    // Keep last cached-fetch time to rate-limit cache reads and UI churn
    val lastCenterCachedMs = remember { mutableStateOf(0L) }

    // One-time initial load: when the screen first sees a valid user GPS location, fetch nearby stops once
    var didInitialLoad by rememberSaveable { mutableStateOf(false) }

    // Snackbar + haptic
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // For scheduling at-most-one pending network fetch when panning rapidly
    val pendingNetworkFetchTargetMs = remember { mutableStateOf(0L) }

    // Permission launcher for fine location (single)
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasLocationPermission = granted
        if (!granted) {
            coroutineScope.launch {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }

    // Multi-permission launcher (request both FINE and COARSE) used by the FAB when needed
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms: Map<String, Boolean> ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            // If the user granted permission via the FAB flow, start location updates and
            // mark that we should center on arrival (and fetch nearby stops).
            mapViewModel.startLocationUpdates(ctx)
            pendingCenterOnLocation.value = true
        } else {
            coroutineScope.launch {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }

    val permissionLauncherForMultiStart = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms: Map<String, Boolean> ->
        val any = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (any) {
            mapViewModel.startLocationUpdates(ctx)
            // Center and fetch when the location arrives
            pendingCenterOnLocation.value = true
        } else {
            coroutineScope.launch {
                try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }

    // When permission is granted, start location updates. Stop updates when this
    // composable leaves composition (DisposableEffect(Unit)). This avoids stopping
    // updates spuriously when the permission flag changes.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapViewModel.startLocationUpdates(ctx)
        }
    }


    // If we had a pending center request and now we have a location, trigger the recenter
    LaunchedEffect(userLocation, pendingCenterOnLocation.value) {
        if (pendingCenterOnLocation.value && userLocation != null) {
            // Trigger recenter on the map and also fetch nearby stops for the new location.
            recenterTrigger.value += 1
            try {
                userLocation?.let { loc ->
                    stopViewModel.loadNearbyStops(stop = "", lat = loc.latitude, lon = loc.longitude)
                }
            } catch (_: Throwable) {}
            pendingCenterOnLocation.value = false
        }
    }

    // Trigger the initial nearby stops load exactly once when we obtain the user's GPS location
    LaunchedEffect(userLocation, didInitialLoad) {
        if (!didInitialLoad && userLocation != null) {
            try {
                // use safe let to capture the current non-null userLocation and avoid smart-cast issues
                userLocation?.let { loc ->
                    refreshAnimRequested.value = true
                    stopViewModel.loadNearbyStops(stop = "", lat = loc.latitude, lon = loc.longitude)
                }
            } catch (_: Throwable) { }
            didInitialLoad = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        // Keep the map full-bleed (no scaffold padding) so the underlying MapView can render into
        // the system insets. Apply `scaffoldPadding` only to overlay UI (controls, sheets, FAB)
        Box(modifier = modifier.fillMaxSize()) {
            // Full-screen map; pass the recenter trigger so OpenStreetMap will center when incremented
            OpenStreetMap(
                modifier = Modifier.fillMaxSize(),
                userLocation = userLocation,
                stops = state.stops,
                onStopClick = { stop -> onStopSelected(stop.id) },
                recenterTrigger = recenterTrigger.value,
                onMapCenterChanged = { lat, lon ->
                    val now = System.currentTimeMillis()
                    val cachedCooldownMs = 500L
                    val networkCooldownMs = 2_000L

                    // 1) Quick cached display (rate-limited to `cachedCooldownMs`)
                    if (now - lastCenterCachedMs.value > cachedCooldownMs) {
                        lastCenterCachedMs.value = now
                        try {
                            stopViewModel.loadCachedNearbyStops(lat, lon)
                        } catch (_: Throwable) {}
                    }

                    // 2) Network fetch: if allowed now, run immediately; otherwise schedule one after cooldown.
                    val sinceLastNetwork = now - lastCenterFetchMs.value
                    if (sinceLastNetwork > networkCooldownMs) {
                        lastCenterFetchMs.value = now
                        refreshAnimRequested.value = true
                        stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon)
                    } else {
                        // schedule one fetch at time targetMs (only keep latest)
                        val delayMs = networkCooldownMs - sinceLastNetwork
                        val targetMs = now + delayMs
                        pendingNetworkFetchTargetMs.value = targetMs
                        coroutineScope.launch {
                            delay(delayMs)
                            // only run if no newer scheduled fetch replaced this
                            if (pendingNetworkFetchTargetMs.value == targetMs) {
                                lastCenterFetchMs.value = System.currentTimeMillis()
                                refreshAnimRequested.value = true
                                try {
                                    stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon)
                                } catch (_: Throwable) {}
                                // clear pending marker
                                pendingNetworkFetchTargetMs.value = 0L
                            }
                        }
                    }
                }
            )

            // Top controls overlay: only show the enable-permission button when location isn't enabled.
            Row(
                // Apply scaffold/system padding to the top controls so they avoid the status bar
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(scaffoldPadding)
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (!hasLocationPermission) {
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text(text = "Enable Location")
                    }
                }
            }

            // BottomSheet overlay on top of the map showing stop cards
            val listState = rememberLazyListState()
            BottomSheet(
                // Apply scaffold padding so bottom sheet avoids navigation bars / insets
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(scaffoldPadding)
                    .align(Alignment.BottomCenter),
                stops = state.stops,
                userLat = userLocation?.latitude,
                userLon = userLocation?.longitude,
                onStopClick = { stop -> onStopSelected(stop.id) },
                onRefresh = {
                    // When user taps refresh in the sheet header, run the same logic and animate
                    refreshAnimRequested.value = true
                    stopViewModel.loadNearbyStops(stop = "", lat = userLocation?.latitude ?: 50.873322, lon = userLocation?.longitude ?: 4.525903)
                },
                isLoading = state.isLoading,
                shouldAnimateRefresh = refreshAnimRequested.value,
                onRefreshAnimationComplete = { refreshAnimRequested.value = false },
                listState = listState,
                onHeightChanged = { dp -> bottomSheetHeight = dp }
            )

            // Always scroll the list to the top when stops change (cached or network results).
            LaunchedEffect(state.stops) {
                try {
                    if (state.stops.isNotEmpty()) {
                        // animate to top for a nicer UX; adjust to instant scroll if desired
                        listState.animateScrollToItem(0)
                    }
                } catch (_: Throwable) {}
            }

            // Floating Action Button positioned above the bottom sheet at its top-right
            FloatingActionButton(
                onClick = {
                    // ripple is automatic; trigger haptic feedback
                    try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}

                    if (hasLocationPermission) {
                        // One-time recenter: if we have a location, request recenter immediately
                        val loc = userLocation
                        if (loc != null) {
                            recenterTrigger.value += 1
                            // Also request nearby stops for the user's current location immediately
                            try { stopViewModel.loadNearbyStops(stop = "", lat = loc.latitude, lon = loc.longitude) } catch (_: Throwable) {}
                            pendingCenterOnLocation.value = false
                        } else {
                            // start location updates and wait to center when location arrives
                            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                mapViewModel.startLocationUpdates(ctx)
                                pendingCenterOnLocation.value = true
                            } else {
                                permissionLauncherForMultiStart.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    } else {
                        // Ask for both permissions when user taps FAB
                        multiPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                containerColor = Color(0xFF1D2124),
                contentColor = Color(0xFFBDBDBD),
                shape = RoundedCornerShape(32.dp),
                // Apply scaffold padding so FAB sits above system nav bar, then add custom offsets
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(scaffoldPadding)
                    .padding(end = 16.dp, bottom = bottomSheetHeight + 16.dp)
            ) {
                Icon(Lucide.Navigation, contentDescription = "Center on my location")
            }

        }
    }
}