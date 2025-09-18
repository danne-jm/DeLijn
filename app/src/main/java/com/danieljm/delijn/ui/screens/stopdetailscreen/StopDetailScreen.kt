package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.danieljm.delijn.R
import com.danieljm.delijn.ui.components.stopdetails.BusArrivalsBottomSheet
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    stopId: String,
    stopName: String,
    viewModel: StopDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(stopId, stopName) {
        viewModel.loadStopDetails(stopId, stopName)
    }

    val context = LocalContext.current
    val view = LocalView.current
    val arrivalsListState = rememberLazyListState()
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Set status bar color to match top app bar and restore on dispose
    val statusBarColor = Color(0xFF1D2124)
    DisposableEffect(Unit) {
        val window = (context as? androidx.activity.ComponentActivity)?.window
        val originalStatusBarColor = window?.statusBarColor
        val originalLightStatusBars = window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars
        }

        // Set the dark status bar for this screen
        window?.let {
            it.statusBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }

        // Cleanup function to restore original values when leaving the screen
        onDispose {
            window?.let {
                originalStatusBarColor?.let { color -> it.statusBarColor = color }
                originalLightStatusBars?.let { isLight ->
                    WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = isLight
                }
            }
        }
    }

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

            val markerPoint = GeoPoint(lat, lon)
            val marker = Marker(mv).apply {
                position = markerPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Stop marker"
                snippet = stopName
                icon = ContextCompat.getDrawable(context, R.drawable.bus_stop)
            }
            mv.overlays.add(marker)

            // Offset the map center so the marker appears higher on the screen
            val offsetMeters = 250.0 // Move map center 250 meters south (so marker is higher)
            val offsetLat = lat - (offsetMeters / 111_000.0) // 1 degree latitude ≈ 111km
            val centerPoint = GeoPoint(offsetLat, lon)
            mv.controller.setZoom(18.0)
            mv.controller.animateTo(centerPoint)
            mv.invalidate()
        }
    }

    // Add live bus markers for all buses with real-time positions
    LaunchedEffect(uiState.busPositions, mapViewRef) {
        val mv = mapViewRef
        if (mv != null) {
            // Remove previous bus markers
            val toRemove = mv.overlays.filterIsInstance<Marker>().filter { it.title?.startsWith("Bus marker") == true }
            toRemove.forEach { mv.overlays.remove(it) }

            uiState.busPositions.forEach { bus ->
                val markerPoint = GeoPoint(bus.latitude, bus.longitude)
                val marker = Marker(mv).apply {
                    position = markerPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Bus marker ${bus.vehicleId}"
                    snippet = "Bus ID: ${bus.vehicleId}"
                    icon = ContextCompat.getDrawable(context, R.drawable.bus_side)
                }
                mv.overlays.add(marker)
            }
            mv.invalidate()
        }
    }

    // Use Scaffold to properly handle the top app bar and content layout
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Stop",
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Heart action */ }) {
                        Icon(Lucide.Heart, contentDescription = "Favorite", tint = Color.White.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1D2124)
                )
            )
        }
    ) { paddingValues ->
        // Content area that automatically accounts for the top app bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map fills the remaining space after accounting for top app bar
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        mapViewRef = this
                    }
                },
                update = { mv ->
                    mapViewRef = mv
                },
                modifier = Modifier.fillMaxSize()
            )

            // Error overlay positioned at the top of the content area
            uiState.error?.let { err ->
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Error: $err",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom Sheet positioned at the bottom
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