package com.danieljm.delijn.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw

@Composable
fun BottomSheet(
    modifier: Modifier = Modifier,
    // Fractions of screen height (0f..1f). Defaults: collapsed 20%, initial 20%, max 80%.
    collapsedHeightRatio: Float = 0.25f,
    initialHeightRatio: Float = 0.25f,
    maxHeightRatio: Float = 0.90f,
    collapsedHeight: Dp = 160.dp, // kept for backward compatibility (unused when ratios provided)
    onHeightChanged: ((Dp) -> Unit)? = null // Callback to report height changes
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // enforce sensible bounds and ordering: collapsed <= initial <= max
    val minVisibleRatio = 0.05f
    val maxRatioClamped = maxHeightRatio.coerceIn(minVisibleRatio, 1f)
    val collapsedRatioClamped = collapsedHeightRatio.coerceIn(minVisibleRatio, maxRatioClamped)
    val initialRatioClamped = initialHeightRatio.coerceIn(collapsedRatioClamped, maxRatioClamped)

    // Screen height in pixels
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Convert ratios to pixel heights
    val collapsedPx = screenHeightPx * collapsedRatioClamped
    val expandedPx = screenHeightPx * maxRatioClamped
    val initialPx = screenHeightPx * initialRatioClamped

    // current height in pixels (initial = initialPx)
    var heightPx by remember { mutableStateOf(initialPx.coerceIn(collapsedPx, expandedPx)) }

    // animate height in dp for smooth UI when heightPx changes
    val heightDp by remember(heightPx) { derivedStateOf { with(density) { heightPx.toDp() } } }
    val animatedHeightDp by animateDpAsState(targetValue = heightDp)

    // Report height changes to parent
    LaunchedEffect(animatedHeightDp) {
        onHeightChanged?.invoke(animatedHeightDp)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeightDp),
        color = Color(0xFF1D2124),
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Handle bar at the top center - draggable from handle
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
                            detectVerticalDragGestures { _, dragAmount ->
                                // dragAmount > 0 -> dragging down, < 0 -> dragging up
                                val newHeight = (heightPx - dragAmount).coerceIn(collapsedPx, expandedPx)
                                heightPx = newHeight
                            }
                        }
                        .background(color = Color(0xFFBDBDBD), shape = RoundedCornerShape(3.dp))
                )
            }

            // Header row (visible when collapsed because collapsed ratio is enforced)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 12.dp,
                        alignment = Alignment.Start
                    ),
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
                            .size(24.dp)
                            .padding(start = 8.dp)
                    )
                }
            }

            // Placeholder body for expanded content (below header)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp + 56.dp) // leave room for header
            ) {
                // additional content goes here
            }
        }
    }
}