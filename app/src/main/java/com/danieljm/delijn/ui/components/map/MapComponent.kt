package com.danieljm.delijn.ui.components.map

import android.content.Context
import android.graphics.Color
import android.location.Location
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.danieljm.delijn.R
import com.danieljm.delijn.domain.model.Stop
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color as AndroidColor

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
    mapCenterOffset: Double = 0.0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }

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

    // Update stop markers, custom markers and polylines
    LaunchedEffect(stops, customMarkers, polylines) {
        mapViewRef?.let { mapView ->
            // Remove previous overlays except "You are here"
            val overlaysToRemove = mapView.overlays.filter {
                (it is Marker && it.title != "You are here") || it is Polyline
            }
            overlaysToRemove.forEach { mapView.overlays.remove(it) }

            // Add markers for each stop
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

            // Add custom markers
            customMarkers.forEach { customMarker ->
                val gp = GeoPoint(customMarker.latitude, customMarker.longitude)
                val marker = Marker(mapView).apply {
                    position = gp
                    // Use center anchor for rotated markers (like buses) to prevent visual offset
                    if (customMarker.rotation != 0f) {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    } else {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    title = customMarker.title
                    snippet = customMarker.snippet
                    icon = ContextCompat.getDrawable(context, customMarker.iconResourceId)
                    relatedObject = customMarker.id
                    setOnMarkerClickListener { _, _ ->
                        customMarker.onClick?.invoke()
                        true
                    }
                    // Apply rotation if specified
                    rotation = customMarker.rotation
                }
                mapView.overlays.add(marker)
            }

            // Add polylines (routes)
            polylines.forEach { poly ->
                val pts = poly.coordinates.map { (lat, lon) -> GeoPoint(lat, lon) }
                val line = Polyline(mapView).apply {
                    setPoints(pts)
                    val col = try {
                        poly.colorHex?.let { AndroidColor.parseColor(it) } ?: AndroidColor.parseColor("#2196F3")
                    } catch (e: Exception) {
                        AndroidColor.parseColor("#2196F3")
                    }
                    color = col
                    width = poly.width
                    isEnabled = true
                }
                mapView.overlays.add(line)

                // Add unfocused stop markers for each coordinate in the polyline, avoiding duplicates.
                val existingStopMarkerPositions = mapView.overlays
                    .filterIsInstance<Marker>()
                    .map { it.position }
                    .toSet()

                poly.coordinates.forEach { (lat, lon) ->
                    val newPoint = GeoPoint(lat, lon)
                    // Avoid adding a marker if one is already at or very near this position
                    if (existingStopMarkerPositions.none { it.distanceToAsDouble(newPoint) < 1.0 }) {
                        val stopMarker = Marker(mapView).apply {
                            position = newPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.unfocused_bus_stop)
                            // Find the corresponding stop to make it clickable
                            val matchingStop = stops.find { it.latitude == lat && it.longitude == lon }
                            if (matchingStop != null) {
                                title = matchingStop.name
                                snippet = "Stop ID: ${matchingStop.id}"
                                relatedObject = matchingStop.id
                                setOnMarkerClickListener { _, _ ->
                                    onStopMarkerClick(matchingStop)
                                    true
                                }
                            }
                        }
                        mapView.overlays.add(stopMarker)
                    }
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
