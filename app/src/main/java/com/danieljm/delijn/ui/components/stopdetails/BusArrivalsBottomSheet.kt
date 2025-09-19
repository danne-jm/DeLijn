package com.danieljm.delijn.ui.components.stopdetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.danieljm.delijn.domain.model.ArrivalInfo

@Composable
fun BusArrivalsBottomSheet(
    modifier: Modifier = Modifier,
    maxHeightRatio: Float = 0.85f,
    onHeightChanged: ((Dp) -> Unit)? = null,
    arrivals: List<ArrivalInfo> = emptyList(),
    isLoading: Boolean = false,
    listState: LazyListState,
    stopName: String = "",
    stopId: String = "",
    // onRefresh now accepts a force flag: true when user tapped, false when conditional auto-refresh.
    onRefresh: ((force: Boolean) -> Unit)? = null,
    shouldAnimateRefresh: Boolean = false,
    onRefreshAnimationComplete: (() -> Unit)? = null,
    // Timestamp in millis of last arrivals refresh. Used to trigger a conditional refresh when stale.
    lastArrivalsRefreshMillis: Long? = null
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val collapsedPx = with(density) { 188.dp.toPx() }
    val expandedPx = screenHeightPx * maxHeightRatio
    // 50% of screen height as initial height
    val initialPx = ((expandedPx - collapsedPx) / 2f + collapsedPx).coerceIn(collapsedPx, expandedPx)

    var heightPx by rememberSaveable { mutableStateOf(initialPx) }
    var isDragging by remember { mutableStateOf(false) }

    val heightDp by remember(heightPx) { derivedStateOf { with(density) { heightPx.toDp() } } }
    val animatedHeightDp by animateDpAsState(targetValue = heightDp, label = "BusArrivalsBottomSheetHeightAnimation")

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(heightDp, animatedHeightDp, isDragging) {
        val reportHeight = if (isDragging) heightDp else animatedHeightDp
        onHeightChanged?.invoke(reportHeight)
    }

    // If the sheet is composed and it's been more than 30s since last arrivals refresh, trigger a conditional refresh.
    LaunchedEffect(stopId, lastArrivalsRefreshMillis) {
        val now = System.currentTimeMillis()
        val last = lastArrivalsRefreshMillis
        if (onRefresh != null) {
            if (last == null || now - last > 30_000) {
                // trigger conditional (non-forced) refresh - ViewModel will respect cooldown when force=false
                onRefresh(false)
            }
        }
    }

    LaunchedEffect(shouldAnimateRefresh) {
        if (shouldAnimateRefresh) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, animationSpec = tween(durationMillis = 800))
            rotation.snapTo(0f)
            onRefreshAnimationComplete?.invoke()
        }
    }

    // Compute bottom content padding to ensure the last item can be scrolled fully into view
    val view = LocalView.current
    val navBarInsetDp = remember {
        try {
            val insets = ViewCompat.getRootWindowInsets(view)
            val bottom = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            with(density) { bottom.toDp() }
        } catch (_: Exception) {
            16.dp
        }
    }
    val bottomContentPadding = (24.dp + navBarInsetDp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeightDp),
        color = Color(0xFF1D2124),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
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

            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stopName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            )  {
                Text(
                    text = stopId,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
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
                            // manual tap should force a refetch
                            onRefresh?.invoke(true)
                        }
                )
            }


            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (arrivals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No live arrivals found.", color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp), // Only top padding
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    state = listState,
                    contentPadding = PaddingValues(bottom = bottomContentPadding)
                ) {
                    items(items = arrivals, key = { arrival -> arrival.lineId + arrival.time }) { arrival ->
                        BusCard(arrival = arrival)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}