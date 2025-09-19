package com.danieljm.delijn.ui.components.map

import android.content.Context
import android.graphics.Paint
import android.location.Location
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.danieljm.delijn.R
import com.danieljm.delijn.domain.model.Stop
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor
import kotlin.math.*

// Map polyline data used to draw line directions (routes)
data class MapPolyline(
    val id: String,
    val coordinates: List<Pair<Double, Double>>,
    val colorHex: String? = null,
    val width: Float = 8f
)

data class MapState(
    val centerLatitude: Double? = null,
    val centerLongitude: Double? = null,
    val zoom: Double = 15.0
) {
    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<MapState, List<Any>>(
            save = { mapState ->
                listOf(
                    mapState.centerLatitude ?: "null",
                    mapState.centerLongitude ?: "null",
                    mapState.zoom
                )
            },
            restore = { list ->
                MapState(
                    centerLatitude = if (list[0] == "null") null else list[0] as Double,
                    centerLongitude = if (list[1] == "null") null else list[1] as Double,
                    zoom = list[2] as Double
                )
            }
        )
    }
}

data class CustomMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String? = null,
    val iconResourceId: Int,
    val onClick: (() -> Unit)? = null,
    val rotation: Float = 0f // Rotation in degrees for directional markers
)

@Composable
fun MapComponent(
    modifier: Modifier = Modifier,
    userLocation: Location? = null,
    stops: List<Stop> = emptyList(),
    customMarkers: List<CustomMarker> = emptyList(),
    polylines: List<MapPolyline> = emptyList(),
    mapState: MapState = MapState(),
    onMapStateChanged: (MapState) -> Unit = {},
    onStopMarkerClick: (Stop) -> Unit = {},
    animateToLocation: Location? = null,
    animateToStop: Stop? = null,
    showUserLocationMarker: Boolean = true,
    centerOnStop: Stop? = null,
    mapCenterOffset: Double = 0.0,
    darkMode: Boolean = false, // new parameter to toggle dark tiles
    onUserInteraction: (() -> Unit)? = null // new callback when the user touches/ interacts with the map
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }

    // Map of custom marker id -> Marker overlay for incremental updates
    val markerMap = remember { mutableMapOf<String, Marker>() }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    // Center map on initial state or animate to location on first launch
    LaunchedEffect(isFirstLaunch, mapState, userLocation) {
        if (isFirstLaunch && mapViewRef != null) {
            if (mapState.centerLatitude != null && mapState.centerLongitude != null) {
                mapViewRef!!.controller.setZoom(mapState.zoom)
                mapViewRef!!.controller.animateTo(GeoPoint(mapState.centerLatitude, mapState.centerLongitude))
            } else if (userLocation != null) {
                moveMapToLocation(mapViewRef, userLocation, isInitialMove = true)
            }
            isFirstLaunch = false
        }
    }

    // Animate to specific location when requested
    LaunchedEffect(animateToLocation) {
        if (animateToLocation != null) {
            moveMapToLocation(mapViewRef, animateToLocation, isInitialMove = true)
        }
    }

    // Animate to specific stop when requested
    LaunchedEffect(animateToStop) {
        if (animateToStop != null && mapViewRef != null) {
            val geoPoint = GeoPoint(animateToStop.latitude, animateToStop.longitude)
            mapViewRef!!.controller.animateTo(geoPoint)
        }
    }

    // Center on specific stop with offset
    LaunchedEffect(centerOnStop, mapCenterOffset) {
        if (centerOnStop != null && mapViewRef != null) {
            val lat = centerOnStop.latitude
            val lon = centerOnStop.longitude

            // Apply offset if specified (offset in meters)
            val adjustedLat = if (mapCenterOffset != 0.0) {
                lat - (mapCenterOffset / 111_000.0) // 1 degree latitude ≈ 111km
            } else {
                lat
            }

            val geoPoint = GeoPoint(adjustedLat, lon)
            mapViewRef!!.controller.setZoom(18.0)
            mapViewRef!!.controller.animateTo(geoPoint)
        }
    }

    // Animate the user's location marker smoothly
    LaunchedEffect(userLocation, mapViewRef, showUserLocationMarker) {
        if (userLocation != null && mapViewRef != null && showUserLocationMarker) {
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

    // Update stops and polylines when they change (keeps previous behavior)
    LaunchedEffect(stops, polylines, darkMode) {
        mapViewRef?.let { mapView ->
            // Update tile source based on darkMode flag
            try {
                if (darkMode) {
                    // CartoDB Dark Matter tile source
                    val cartoDark = XYTileSource(
                        "CartoDB.DarkMatter",
                        0, 19, 256, ".png",
                        arrayOf("https://basemaps.cartocdn.com/dark_all/")
                    )
                    mapView.setTileSource(cartoDark)
                } else {
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                }
            } catch (_: Exception) {
                // ignore tile source change errors
            }

            // Remove previous overlays except "You are here" and any custom markers managed in markerMap
            val overlaysToRemove = mapView.overlays.filter {
                (it is Marker && it.title != "You are here" && markerMap.values.none { m -> m == it }) || it is Polyline
            }
            overlaysToRemove.forEach { mapView.overlays.remove(it) }

             // Add markers for each stop (focused stops)
             stops.forEach { stop ->
                 val gp = GeoPoint(stop.latitude, stop.longitude)
                 val marker = Marker(mapView).apply {
                     position = gp
                     setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                     title = stop.name
                     snippet = "Stop ID: ${stop.id}"
                     icon = ContextCompat.getDrawable(context, R.drawable.bus_stop)
                     relatedObject = stop.id
                     setOnMarkerClickListener { _, _ ->
                         onStopMarkerClick(stop)
                         true
                     }
                 }
                 mapView.overlays.add(marker)
             }

             // Add polylines (routes)
             polylines.forEach { poly ->
                 // Densify based on current zoom to reduce long segments and visual gaps
                 val currentZoom = try { mapView.zoomLevelDouble } catch (_: Exception) { mapState.zoom }
                 val finalCoordsList = densifyCoordinates(poly.coordinates, currentZoom)
                 val pts = finalCoordsList.map { (lat, lon) -> GeoPoint(lat, lon) }

                 // Background line to smooth out seams/gaps at low zoom
                 val bgLine = Polyline(mapView).apply {
                     setPoints(pts)
                     val baseCol = try {
                         poly.colorHex?.let { AndroidColor.parseColor(it) } ?: AndroidColor.parseColor("#2196F3")
                     } catch (_: Exception) {
                         AndroidColor.parseColor("#2196F3")
                     }
                     color = withAlpha(baseCol, 200) // slightly translucent background
                     width = poly.width + 6f
                     isEnabled = false
                     val p = paint
                     p.isAntiAlias = true
                     p.strokeJoin = Paint.Join.ROUND
                     p.strokeCap = Paint.Cap.ROUND
                 }
                 mapView.overlays.add(bgLine)

                 val line = Polyline(mapView).apply {
                     setPoints(pts)
                     val col = try {
                         poly.colorHex?.let { AndroidColor.parseColor(it) } ?: AndroidColor.parseColor("#2196F3")
                     } catch (_: Exception) {
                         AndroidColor.parseColor("#2196F3")
                     }
                     color = col
                     width = poly.width
                     isEnabled = true

                     // Improve stroke rendering to reduce visible gaps at low zoom
                     val p = paint
                     p.isAntiAlias = true
                     p.strokeJoin = Paint.Join.ROUND
                     p.strokeCap = Paint.Cap.ROUND
                 }
                 mapView.overlays.add(line)
             }

             mapView.invalidate()
         }
    }

    // Incrementally update custom markers using snapshotFlow so small changes don't recreate everything
    LaunchedEffect(customMarkers) {
        val mapView = mapViewRef
        if (mapView == null) return@LaunchedEffect

        // snapshotFlow will observe the composition snapshot if customMarkers is a SnapshotStateList
        snapshotFlow { customMarkers.toList() }.collect { list ->
            // Diff by id
            val ids = list.map { it.id }.toSet()

            // Remove markers that are no longer present
            val toRemove = markerMap.keys - ids
            toRemove.forEach { id ->
                val marker = markerMap.remove(id)
                marker?.let { mapView.overlays.remove(it) }
            }

            // Add or update markers
            for (cm in list) {
                val existing = markerMap[cm.id]
                if (existing == null) {
                    val gp = GeoPoint(cm.latitude, cm.longitude)
                    val marker = Marker(mapView).apply {
                        position = gp
                        if (cm.rotation != 0f) {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        } else {
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        title = cm.title
                        snippet = cm.snippet
                        icon = ContextCompat.getDrawable(context, cm.iconResourceId)
                        relatedObject = cm.id
                        setOnMarkerClickListener { _, _ ->
                            cm.onClick?.invoke()
                            true
                        }
                        rotation = cm.rotation
                    }
                    markerMap[cm.id] = marker
                    mapView.overlays.add(marker)
                } else {
                    // Update position/rotation/icon if necessary
                    val newLat = cm.latitude
                    val newLon = cm.longitude
                    if (existing.position.latitude != newLat || existing.position.longitude != newLon) {
                        existing.position = GeoPoint(newLat, newLon)
                    }
                    if (existing.rotation != cm.rotation) existing.rotation = cm.rotation
                    // Update icon if resource id differs - osmdroid Marker doesn't expose resource id, so always reset in case
                    existing.icon = ContextCompat.getDrawable(context, cm.iconResourceId)
                    existing.title = cm.title
                    existing.snippet = cm.snippet
                }
            }

            mapView.invalidate()
        }
    }

    // Listen for map state changes
    DisposableEffect(mapViewRef) {
        val mapView = mapViewRef
        if (mapView != null) {
            val listener = object : org.osmdroid.events.MapListener {
                override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                    val center = mapView.mapCenter
                    onMapStateChanged(
                        MapState(
                            centerLatitude = center.latitude,
                            centerLongitude = center.longitude,
                            zoom = mapView.zoomLevelDouble
                        )
                    )
                    return true
                }
                override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                    val center = mapView.mapCenter
                    onMapStateChanged(
                        MapState(
                            centerLatitude = center.latitude,
                            centerLongitude = center.longitude,
                            zoom = mapView.zoomLevelDouble
                        )
                    )
                    return true
                }
            }
            mapView.addMapListener(listener)
            onDispose { mapView.removeMapListener(listener) }
        } else {
            onDispose { }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(mapState.zoom)
                    if (mapState.centerLatitude != null && mapState.centerLongitude != null) {
                        controller.setCenter(GeoPoint(mapState.centerLatitude, mapState.centerLongitude))
                    }

                    // Notify about user interaction via touch down so callers can decide to fetch nearby stops
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            onUserInteraction?.invoke()
                        }
                        // return false so default handling continues
                        false
                    }

                    mapViewRef = this
                }
            },
            update = { mv ->
                mapViewRef = mv
            },
            modifier = Modifier.fillMaxSize()
        )
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

// Haversine formula to calculate distance between two lat/lon points in meters
private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000 // Earth radius in meters

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

// Add helper to densify coordinates to avoid visible gaps when zoomed out
private fun densifyCoordinates(coords: List<Pair<Double, Double>>, zoom: Double): List<Pair<Double, Double>> {
    if (coords.size < 2) return coords

    // Decide threshold based on zoom: at low zoom the screen-space distance between points is large
    val thresholdMeters = when {
        zoom < 6.0 -> 5000.0
        zoom < 8.0 -> 2000.0
        zoom < 10.0 -> 1000.0
        zoom < 12.0 -> 250.0
        zoom < 14.0 -> 100.0
        else -> 0.0
    }

    if (thresholdMeters <= 0.0) return coords

    val result = mutableListOf<Pair<Double, Double>>()
    for (i in 0 until coords.size - 1) {
        val a = coords[i]
        val b = coords[i + 1]
        result.add(a)
        val dist = haversineMeters(a.first, a.second, b.first, b.second)
        if (dist > thresholdMeters) {
            val segments = kotlin.math.ceil(dist / thresholdMeters).toInt()
            for (s in 1 until segments) {
                val t = s.toDouble() / segments.toDouble()
                val lat = a.first + (b.first - a.first) * t
                val lon = a.second + (b.second - a.second) * t
                result.add(Pair(lat, lon))
            }
        }
    }
    // add last point
    result.add(coords.last())
    return result
}

private fun withAlpha(color: Int, alpha: Int): Int {
    val r = AndroidColor.red(color)
    val g = AndroidColor.green(color)
    val b = AndroidColor.blue(color)
    return AndroidColor.argb(alpha, r, g, b)
}
