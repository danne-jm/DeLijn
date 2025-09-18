package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.danieljm.delijn.R
import com.danieljm.delijn.ui.components.stopdetails.BusArrivalsBottomSheet
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun StopDetailScreen(
    stopId: String,
    stopName: String,
    viewModel: StopDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stopId, stopName) {
        viewModel.loadStopDetails(stopId, stopName)
    }

    val context = LocalContext.current
    val arrivalsListState = rememberLazyListState()
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Initialize osmdroid configuration once
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
    }

    // Center the map and add a stop marker when coordinates are available
    LaunchedEffect(uiState.stopLatitude, uiState.stopLongitude, mapViewRef) {
        val mv = mapViewRef
        val lat = uiState.stopLatitude
        val lon = uiState.stopLongitude
        if (mv != null && lat != null && lon != null) {
            // Remove previous stop markers
            val toRemove = mv.overlays.filterIsInstance<Marker>().filter { it.title == "Stop marker" }
            toRemove.forEach { mv.overlays.remove(it) }

            val gp = GeoPoint(lat, lon)
            val marker = Marker(mv).apply {
                position = gp
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Stop marker"
                snippet = stopName
                icon = ContextCompat.getDrawable(context, R.drawable.bus_stop)
            }
            mv.overlays.add(marker)
            mv.controller.setZoom(18.0)
            mv.controller.animateTo(gp)
            mv.invalidate()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        mapViewRef = this
                    }
                },
                update = { mv -> mapViewRef = mv },
                modifier = Modifier.fillMaxSize()
            )

            // Top overlay with stop title and errors
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.error?.let { err ->
                    Text(text = "Error: $err", color = MaterialTheme.colorScheme.error)
                }
            }

            BusArrivalsBottomSheet(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                arrivals = uiState.allArrivals,
                isLoading = uiState.isLoading,
                onHeightChanged = { /* no-op in detail screen */ },
                listState = arrivalsListState,
                stopName = stopName,
                stopId = uiState.stopId,
                shouldAnimateRefresh = uiState.shouldAnimateRefresh,
                onRefresh = { viewModel.refreshArrivals(force = true) },
                onRefreshAnimationComplete = { viewModel.onRefreshAnimationComplete() }
            )
        }
    }
}