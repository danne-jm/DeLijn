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
import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.ui.components.map.MapViewModel
import com.danieljm.bussin.ui.components.map.OpenStreetMap
import com.danieljm.bussin.util.calculateDistance
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

    // Android context for permission checks and starting location updates
    val ctx = LocalContext.current

    // Track which stop IDs the map is currently rendering as markers. OpenStreetMap will
    // invoke `onVisibleStopIdsChanged` with the set of IDs; we use that to filter the
    // BottomSheet so it only lists stops that have markers visible on the map.
    // Use a nullable set so we can tell the difference between "map hasn't reported yet"
    // (null) and "map reported an empty set" (emptySet()). When null, fall back to showing
    // the full state.stops; once the map reports, always honor its visible IDs (even if empty).
    val visibleStopIds = remember { mutableStateOf<Set<String>?>(null) }

    // The stops to render in the BottomSheet: once the map reports visible IDs, filter to that
    // set. Before the map reports, show the full set of stops from the ViewModel.
    val filteredStops = remember(state.stops, visibleStopIds.value) {
        val ids = visibleStopIds.value
        if (ids != null) {
            state.stops.filter { it.id in ids }
        } else {
            state.stops
        }
    }

    // Compute the same distance-based sort the BottomSheet uses so indexes line up
    val sortedDisplayedStops = remember(filteredStops, state.stops, /* user location observed below */) {
        // We'll actually sort inside a derived remember that depends on userLocation below to avoid
        // using a stale user location; this placeholder will be replaced in the next remember below.
        filteredStops
    }

    // Remember a center request from the BottomSheet. When non-null, OpenStreetMap will
    // animate to that stop and then the `onCenterHandled` callback will clear it.
    val centerRequestedStop = remember { mutableStateOf<Stop?>(null) }

    // Lazy list state for the BottomSheet list; declared early so map marker clicks can
    // instruct the list to scroll to a specific stop card.
    val listState = rememberLazyListState()

    // Request object to tell BottomSheet to scroll to a specific stop id. We keep
    // it separate from `centerRequestedStop` which is used to ask the map to center.
    val scrollRequestStopId = remember { mutableStateOf<String?>(null) }

    // Track expanded state and highlighted stop id for the BottomSheet
    val bottomSheetExpanded = remember { mutableStateOf(false) }
    val highlightedStopId = remember { mutableStateOf<String?>(null) }

    // Obtain MapViewModel via Hilt Compose helper so it respects lifecycle and DI
    val mapViewModel: MapViewModel = hiltViewModel()
    val userLocation by mapViewModel.location.collectAsState()

    // recompute sortedDisplayedStops when userLocation changes
    val sortedDisplayedStopsFinal = remember(filteredStops, userLocation) {
        val lat = userLocation?.latitude
        val lon = userLocation?.longitude
        if (lat != null && lon != null) {
            filteredStops.sortedBy { stop ->
                val sLat = stop.latitude
                val sLon = stop.longitude
                if (sLat != null && sLon != null) {
                    calculateDistance(lat, lon, sLat, sLon)
                } else {
                    Double.MAX_VALUE
                }
            }
        } else {
            filteredStops
        }
    }

    // A lightweight suppression so that a recent manual scroll (triggered by tapping a map marker)
    // isn't immediately overwritten by the automatic "scroll to top on displayed-stops change".
    val lastManualScrollMs = remember { mutableStateOf(0L) }
    val manualScrollSuppressMs = 1500L

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

    // Suppress map-center triggered fetches for a short window after a manual map marker tap
    val ignoreCenterFetchUntilMs = remember { mutableStateOf(0L) }

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
                onStopClick = { stop ->
                    // When user taps a map marker, expand the sheet, highlight the card, and scroll to it
                    try {
                        bottomSheetExpanded.value = true
                        highlightedStopId.value = stop.id

                        // Mark a short window where map-center triggered fetches will be ignored so
                        // the BottomSheet list doesn't immediately get replaced by a new nearby-stops fetch
                        // while we're trying to scroll to a specific item.
                        val suppressWindowMs = 600L
                        ignoreCenterFetchUntilMs.value = System.currentTimeMillis() + suppressWindowMs

                        // Request the BottomSheet to scroll to the stop ID. BottomSheet will
                        // compute the index using its own sorted list and perform the scroll.
                        scrollRequestStopId.value = stop.id
                        lastManualScrollMs.value = System.currentTimeMillis()

                        // clear highlight after a short pulse
                        coroutineScope.launch {
                            highlightedStopId.value = null
                        }
                    } catch (_: Throwable) {}
                    // Also notify external handler
                    onStopSelected(stop.id)
                },
                recenterTrigger = recenterTrigger.value,
                onMapCenterChanged = { lat, lon ->
                    val now = System.currentTimeMillis()

                    // Respect suppression window set when user tapped a marker: skip fetches that were
                    // caused by the tap-centering animation so we don't overwrite the sheet's list.
                    if (now < ignoreCenterFetchUntilMs.value) return@OpenStreetMap

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
                },
                onVisibleStopIdsChanged = { ids ->
                    // Update the visible IDs (possibly empty). This makes the BottomSheet
                    // show exactly the stops that have markers on the map.
                    visibleStopIds.value = ids
                },
                // Center request from BottomSheet card taps
                centerOnStop = centerRequestedStop.value,
                onCenterHandled = { centerRequestedStop.value = null }
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
            BottomSheet(
                // Apply scaffold padding so bottom sheet avoids navigation bars / insets
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(scaffoldPadding)
                    .align(Alignment.BottomCenter),
                stops = sortedDisplayedStopsFinal,
                userLat = userLocation?.latitude,
                userLon = userLocation?.longitude,
                // Ask the BottomSheet to scroll to a stop id when set by the map marker tap.
                scrollToStopId = scrollRequestStopId.value,
                onScrollHandled = { scrollRequestStopId.value = null },
                onStopClick = { stop ->
                    // Request the map to center on this stop, expand sheet (already visible) and highlight
                    centerRequestedStop.value = stop
                    highlightedStopId.value = stop.id
                    // clear highlight after a short duration
                    coroutineScope.launch {
                        try { delay(1500) } catch (_: Throwable) {}
                        highlightedStopId.value = null
                    }
                    onStopSelected(stop.id)
                },
                onRefresh = {
                    // When user taps refresh in the sheet header, run the same logic and animate
                    refreshAnimRequested.value = true
                    stopViewModel.loadNearbyStops(stop = "", lat = userLocation?.latitude ?: 50.873322, lon = userLocation?.longitude ?: 4.525903)
                },
                isLoading = state.isLoading,
                shouldAnimateRefresh = refreshAnimRequested.value,
                onRefreshAnimationComplete = { refreshAnimRequested.value = false },
                listState = listState,
                expanded = bottomSheetExpanded.value,
                highlightedStopId = highlightedStopId.value,
                onHeightChanged = { dp -> bottomSheetHeight = dp }
            )

            // Only auto-scroll to top when the displayed list changes if we haven't recently
            // performed a manual scroll triggered by tapping a map marker.
            LaunchedEffect(sortedDisplayedStopsFinal) {
                try {
                    if (sortedDisplayedStopsFinal.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        if (now - lastManualScrollMs.value > manualScrollSuppressMs) {
                            listState.animateScrollToItem(0)
                        }
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
