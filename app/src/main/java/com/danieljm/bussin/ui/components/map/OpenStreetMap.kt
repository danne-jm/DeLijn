package com.danieljm.bussin.ui.components.map

import android.content.Context
import android.location.Location
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.danieljm.bussin.domain.model.Stop
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * A simple Compose wrapper around osmdroid's MapView.
 * - centers on `userLocation` when provided
 * - shows a marker at the user's location
 * - can render nearby `stops` as markers and notify when tapped
 */
@Composable
fun OpenStreetMap(
    modifier: Modifier = Modifier,
    userLocation: Location? = null,
    zoom: Double = 15.0,
    stops: List<Stop> = emptyList(),
    onStopClick: (Stop) -> Unit = {},
    // Increment this external counter to request a one-time recenter action from the parent.
    recenterTrigger: Int = 0,
    // Callback invoked when the user finishes interacting and the map center is changed.
    // Provides the new center's latitude and longitude.
    onMapCenterChanged: ((Double, Double) -> Unit)? = null,
) {
    val composeCtx = LocalContext.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val userMarkerRef = remember { mutableStateOf<Marker?>(null) }

    // One-time auto-center flag (survives simple config changes)
    val didAutoCenter = rememberSaveable { mutableStateOf(false) }
    // If the user touches/pans the map we consider them as "userInteracted" and won't re-center automatically
    val userInteracted = remember { mutableStateOf(false) }

    // Keep markers for stops keyed by stop.id
    val stopMarkers = remember { mutableStateMapOf<String, Marker>() }

    // A callback holder so the AndroidView factory (created once) can call back into the
    // latest-composed lambda which has access to the current `userLocation` value.
    val touchUpCallback = remember { mutableStateOf<(MapView) -> Unit>({}) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Load osmdroid config (userAgent & proper cache path) before creating the view
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            val mapView = MapView(ctx).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(50.873322, 4.525903))

                // Respect system top inset so tiles render into the safe area (avoid white strip at top)
                try {
                    val insets = ViewCompat.getRootWindowInsets(this)
                    val top = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())?.top ?: 0
                    if (top > 0) {
                        // set padding so the MapView renders tiles properly under the top inset
                        setPadding(0, top, 0, 0)
                    }
                } catch (_: Throwable) {}

                // detect user touch to prevent forced recentering after the first auto-center
                setOnTouchListener { v, event ->
                    try {
                        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                            // Mark that the user interacted so we avoid forced auto-recentering
                            userInteracted.value = true
                        }
                        if (event.action == MotionEvent.ACTION_UP) {
                            try { v.performClick() } catch (_: Throwable) { }
                            // Post a touch-up callback so it runs on the view thread after interaction
                            try { v.post { touchUpCallback.value(this) } } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) { }
                    false
                }
            }

            // create a dedicated marker for the user
            val initialUserMarker = Marker(mapView).apply {
                position = GeoPoint(0.0, 0.0)
                //setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // user drawable user_location for user marker
                icon = ctx.getDrawable(com.danieljm.bussin.R.drawable.user_location)
                title = "You are here"

            }
            mapView.overlays.add(initialUserMarker)
            userMarkerRef.value = initialUserMarker

            mapViewRef.value = mapView
            mapView
        },
        update = { mapView ->
            // update touchUpCallback so it captures the latest `userLocation` and `onMapCenterChanged`
            touchUpCallback.value = { mv ->
                try {
                    val center = mv.mapCenter
                    val centerLat = center.latitude
                    val centerLon = center.longitude
                    // If we have a user location, only invoke the callback when center is sufficiently far
                    val thresholdMeters = 200.0
                    if (userLocation != null) {
                        val results = FloatArray(1)
                        try {
                            Location.distanceBetween(userLocation.latitude, userLocation.longitude, centerLat, centerLon, results)
                            val dist = results[0].toDouble()
                            if (dist > thresholdMeters) {
                                try { onMapCenterChanged?.invoke(centerLat, centerLon) } catch (_: Throwable) {}
                            }
                        } catch (_: Throwable) {
                            try { onMapCenterChanged?.invoke(centerLat, centerLon) } catch (_: Throwable) {}
                        }
                    } else {
                        // no user location — let parent decide
                        try { onMapCenterChanged?.invoke(centerLat, centerLon) } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) { }
            }

            // update user marker and optional one-time auto-centering
            userLocation?.let { loc ->
                val gp = GeoPoint(loc.latitude, loc.longitude)
                userMarkerRef.value?.position = gp

                // Auto-center exactly once on first fix (if user hasn't interacted)
                if (!didAutoCenter.value && !userInteracted.value) {
                    try { mapView.controller.animateTo(gp) } catch (_: Throwable) { }
                    didAutoCenter.value = true
                }
            }

            // Sync stop markers: add/update/remove as necessary
            val currentIds = stops.mapNotNull(Stop::id).toSet()

            // remove markers for stops no longer present
            val toRemove = stopMarkers.keys.filter { it !in currentIds }
            toRemove.forEach { id ->
                try {
                    val m = stopMarkers.remove(id)
                    if (m != null) mapView.overlays.remove(m)
                } catch (_: Throwable) { }
            }

            // add or update markers for current stops (only when lat/lon are present)
            stops.forEach { stop ->
                val sid = stop.id
                val lat = stop.latitude
                val lon = stop.longitude
                if (lat == null || lon == null) return@forEach

                val existing = stopMarkers[sid]
                if (existing != null) {
                    existing.position = GeoPoint(lat, lon)
                } else {
                    try {
                        // create a new marker for this stop
                        val m = Marker(mapView).apply {
                            position = GeoPoint(lat, lon)
                            // use drawable delijn_stop for stop marker
                            icon = mapView.context.getDrawable(com.danieljm.bussin.R.drawable.delijn_stop)
                            title = stop.name
                            setOnMarkerClickListener { _, _ ->
                                onStopClick(stop)
                                true
                            }
                        }
                        mapView.overlays.add(m)
                        stopMarkers[sid] = m
                    } catch (_: Throwable) { }
                }
            }

            // redraw map overlays after sync
            try { mapView.invalidate() } catch (_: Throwable) { }
        }
    )

    // When parent requests a recenter (counter increment), animate to user's current position once.
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            userLocation?.let { loc ->
                try {
                    val mv = mapViewRef.value ?: return@let

                    // Step 1: animate the map center to the user's location without changing zoom
                    val targetPoint = GeoPoint(loc.latitude, loc.longitude)
                    try { mv.controller.animateTo(targetPoint) } catch (_: Throwable) {}

                    // Wait until the map center is essentially above the user (or timeout)
                    var attempts = 0
                    val centerThresholdMeters = 25.0
                    while (attempts < 40) {
                        val center = mv.mapCenter
                        val results = FloatArray(1)
                        try {
                            Location.distanceBetween(center.latitude, center.longitude, loc.latitude, loc.longitude, results)
                        } catch (_: Throwable) { results[0] = 0f }
                        if (results[0].toDouble() <= centerThresholdMeters) break
                        attempts++
                        delay(50)
                    }

                    // Step 2: smooth / gradual zoom-in to a target zoom level
                    val targetZoom = 18.0
                    // Smooth incremental zoom: small steps with short delays for a smoother visual effect.
                    // This handles zooming both up and down and caps iterations to avoid hangs.
                    try {
                        var currentZoom = try { mv.zoomLevelDouble } catch (_: Throwable) { targetZoom }
                        val step = 0.18 // zoom step per iteration (smaller -> smoother)
                        val delayMs = 22L // delay between steps (ms)
                        var iterations = 0
                        val maxIterations = 80
                        while (kotlin.math.abs(currentZoom - targetZoom) > 0.01 && iterations < maxIterations) {
                            val diff = targetZoom - currentZoom
                            val delta = when {
                                kotlin.math.abs(diff) < step -> diff
                                diff > 0 -> step
                                else -> -step
                            }
                            currentZoom += delta
                            try { mv.controller.setZoom(currentZoom) } catch (_: Throwable) {}
                            iterations++
                            kotlinx.coroutines.delay(delayMs)
                        }
                        // Ensure exact final zoom
                        try { mv.controller.setZoom(targetZoom) } catch (_: Throwable) {}
                    } catch (_: Throwable) {}

                     // indicate we had a real center event
                     didAutoCenter.value = true
                     // clear userInteraction flag so subsequent auto-centers (if any) can run
                     userInteracted.value = false
                 } catch (_: Throwable) { }
             }
         }
     }


    DisposableEffect(Unit) {
        onDispose {
            // clean up the map view to avoid memory leaks
            mapViewRef.value?.onPause()
            mapViewRef.value?.onDetach()
        }
    }

    // Prefetch tiles into osmdroid's tile cache at most once every 24h to speed up map rendering.
    LaunchedEffect(composeCtx, userLocation, mapViewRef.value) {
        try {
            val prefs = composeCtx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            val last = prefs.getLong("last_tile_cache_ms", 0L)
            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 60L * 60L * 1000L
            if (now - last < oneDayMs) return@LaunchedEffect

            val mv = mapViewRef.value ?: return@LaunchedEffect

            // Define a small bounding box around either the user's location or the current map center.
            val latDelta = 0.03
            val lonDelta = 0.03
            val bbox = if (userLocation != null) {
                BoundingBox(
                    userLocation.latitude + latDelta,
                    userLocation.longitude - lonDelta,
                    userLocation.latitude - latDelta,
                    userLocation.longitude + lonDelta
                )
            } else {
                val c = mv.mapCenter
                BoundingBox(c.latitude + latDelta, c.longitude - lonDelta, c.latitude - latDelta, c.longitude + lonDelta)
            }

            // Prefetch tiles for a reasonable zoom range around the current zoom.
            val currentZoom = try { mv.zoomLevelDouble.toInt() } catch (_: Throwable) { 15 }
            val minZoom = kotlin.math.max(3, currentZoom - 3)
            val maxZoom = kotlin.math.min(20, currentZoom + 2)

            try {
                val cacheManager = CacheManager(mv)
                // downloadAreaAsync will populate the tile cache; progress callback is optional
                cacheManager.downloadAreaAsync(composeCtx, bbox, minZoom, maxZoom)
                prefs.edit().putLong("last_tile_cache_ms", now).apply()
            } catch (_: Throwable) {
                // ignore caching errors; it's non-fatal
            }
        } catch (_: Throwable) { }
    }
}