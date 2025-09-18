package com.danieljm.delijn.ui.components.stops

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.danieljm.delijn.domain.model.Stop
import kotlin.math.*

@Composable
fun BottomSheet(
    modifier: Modifier = Modifier,
    maxHeightRatio: Float = 0.85f,
    onHeightChanged: ((Dp) -> Unit)? = null,
    stops: List<Stop> = emptyList(),
    userLat: Double? = null,
    userLon: Double? = null,
    onStopClick: (Stop) -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    isLoading: Boolean = false,
    shouldAnimateRefresh: Boolean = false,
    onRefreshAnimationComplete: (() -> Unit)? = null,
    listState: LazyListState
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val collapsedPx = with(density) { 160.dp.toPx() }
    val expandedPx = screenHeightPx * maxHeightRatio
    // 50% of screen height as initial height
    val initialPx = ((expandedPx - collapsedPx) / 2f + collapsedPx).coerceIn(collapsedPx, expandedPx)

    // Use rememberSaveable to preserve the height across navigation
    var heightPx by rememberSaveable { mutableStateOf(initialPx) }
    var isDragging by remember { mutableStateOf(false) }

    val heightDp by remember(heightPx) { derivedStateOf { with(density) { heightPx.toDp() } } }
    val animatedHeightDp by animateDpAsState(targetValue = heightDp, label = "BottomSheetHeightAnimation")

    val rotation = remember { Animatable(0f) }

    val sortedStops = remember(stops, userLat, userLon) {
        if (userLat != null && userLon != null) {
            stops.sortedBy { stop ->
                calculateDistance(userLat, userLon, stop.latitude, stop.longitude)
            }
        } else {
            stops
        }
    }

    LaunchedEffect(heightDp, animatedHeightDp, isDragging) {
        val reportHeight = if (isDragging) heightDp else animatedHeightDp
        onHeightChanged?.invoke(reportHeight)
    }

    LaunchedEffect(shouldAnimateRefresh) {
        if (shouldAnimateRefresh) {
            rotation.snapTo(0f) // Start from 0
            rotation.animateTo(360f, animationSpec = tween(durationMillis = 800))
            rotation.snapTo(0f) // Reset after animation
            onRefreshAnimationComplete?.invoke()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeightDp),
        color = Color(0xFF1D2124),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        // Content
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { _, dragAmount ->
                                val newHeight = (heightPx - dragAmount).coerceIn(collapsedPx, expandedPx)
                                heightPx = newHeight
                            }
                        }
                        .background(color = Color(0xFFBDBDBD), shape = RoundedCornerShape(3.dp))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Stops",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Lucide.RefreshCw,
                    contentDescription = "Refresh",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(28.dp)
                        .padding(start = 8.dp)
                        .graphicsLayer { rotationZ = rotation.value }
                        .clickable(enabled = onRefresh != null && !rotation.isRunning) {
                            onRefresh?.invoke()
                        }
                )
            }

            if (sortedStops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No nearby stops found.", color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    state = listState
                ) {
                    items(sortedStops, key = { it.id }) { stop ->
                        StopCard(
                            stop = stop,
                            userLat = userLat,
                            userLon = userLon,
                            onClick = { onStopClick(stop) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}