package com.danieljm.delijn.ui.screens.stops

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun StopsScreen() {
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

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        // Store osmdroid preferences in a private SharedPreferences to avoid depending on androidx.preference
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }

    // Request permission once when composable enters composition (if not granted)
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
                        // Ensure map reference is up-to-date
                        mapViewRef = mv
                        // If we have permission, try to get last location and center map
                        if (hasLocationPermission && activity != null) {
                            val fused = LocationServices.getFusedLocationProviderClient(activity)
                            try {
                                fused.lastLocation.addOnSuccessListener { location: Location? ->
                                    location?.let { loc ->
                                        moveMapToLocation(mv, loc)
                                    }
                                }
                            } catch (_: SecurityException) {
                                // ignore - permission should be checked before calling
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
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
    // Remove previous user marker(s) we added
    val iterator = mapView.overlays.iterator()
    while (iterator.hasNext()) {
        val overlay = iterator.next()
        if (overlay is Marker && overlay.title == "You are here") {
            iterator.remove()
        }
    }
    val marker = Marker(mapView).apply {
        position = geo
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "You are here"
    }
    mapView.overlays.add(marker)
    mapView.invalidate()
}

// Optional: lifecycle helpers could be added in the Activity to forward onResume/onPause to the MapView.