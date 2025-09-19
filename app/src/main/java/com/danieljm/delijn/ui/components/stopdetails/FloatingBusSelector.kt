package com.danieljm.delijn.ui.components.stopdetails

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.BusFront
import com.composables.icons.lucide.Lucide
import com.danieljm.delijn.R

/**
 * Floating selector row for bus lines at a stop.
 * - `items` is a list of unique line identifiers with display text and colors.
 * - `selected` is the currently selected line id (or null for none).
 * - `onToggle` is called with the item id when tapped (toggle behavior handled by caller).
 *
 * Extended behavior:
 * - When an item is selected it expands to the right to show live bus icons.
 * - `itemsIcons` maps lineId -> list of BusIconEntry which describes vehicleId (nullable), badge index and GPS availability.
 * - `onBusSelected` is called when the user taps a bus icon (vehicleId or null to clear selection).
 */
data class FloatingBusItem(
    val id: String,
    val displayText: String,
    val bgHex: String?,
    val fgHex: String?,
    val borderHex: String?
)

// Represents a bus icon entry for the expanded selector: optional vehicleId + badge (string, supports "X") + GPS flag
data class BusIconEntry(
    val vehicleId: String?,
    val badge: String,
    val hasGps: Boolean
)

@Composable
fun FloatingBusSelectorRow(
    items: List<FloatingBusItem>,
    selected: String?,
    modifier: Modifier = Modifier,
    // map of lineId -> list of BusIconEntry to render when that line is selected
    itemsIcons: Map<String, List<BusIconEntry>> = emptyMap(),
    // currently selected vehicle id (if user tapped an individual bus)
    selectedVehicleId: String? = null,
    onToggle: (String?) -> Unit,
    onBusSelected: (String?) -> Unit = {}
) {
    if (items.isEmpty()) return

    val scroll = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selected != null && selected == item.id

            // parse colors with fallback
            val bgColor = try { item.bgHex?.let { Color(AndroidColor.parseColor(it)) } ?: Color(0xFF4CAF50) } catch (_: Exception) { Color(0xFF4CAF50) }
            val fgColor = try { item.fgHex?.let { Color(AndroidColor.parseColor(it)) } ?: Color.Black } catch (_: Exception) { Color.Black }
            val borderColor = try { item.borderHex?.let { Color(AndroidColor.parseColor(it)) } } catch (_: Exception) { null }

            val containerColor = if (isSelected) bgColor else bgColor.copy(alpha = 0.35f)
            val textColor = if (isSelected) fgColor else fgColor.copy(alpha = 0.6f)

            // icons to show for this line (usually only populated for the selected line)
            val icons = itemsIcons[item.id] ?: emptyList()

            // Build modifier: clip -> background -> optional border (so border is visible) -> clickable -> padding -> animate
            var itemModifier = Modifier
                .wrapContentWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)

            if (borderColor != null) {
                itemModifier = itemModifier.then(Modifier.border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(8.dp)))
            }

            itemModifier = itemModifier
                .clickable(onClick = {
                    if (isSelected) onToggle(null) else onToggle(item.id)
                })
                .padding(horizontal = 12.dp)
                .animateContentSize()

            Box(
                modifier = itemModifier,
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Line text stays left-aligned in the row
                    Text(
                        text = item.displayText,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    // When selected and there are icons, show them to the right — animateContentSize will animate width
                    if (isSelected && icons.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            icons.forEach { iconEntry ->
                                // Each icon shows bus front icon regardless of GPS, with a small centered badge
                                val isThisVehicleSelected = iconEntry.vehicleId != null && selectedVehicleId != null && selectedVehicleId == iconEntry.vehicleId

                                val isDeparted = iconEntry.badge == "Departed"
                                val containerSize = if (isDeparted) 44.dp else 36.dp
                                val badgeWidth = if (isDeparted) 64.dp else 18.dp
                                val badgeHeight = if (isDeparted) 20.dp else 18.dp

                                Box(modifier = Modifier
                                    .size(containerSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThisVehicleSelected) fgColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable(onClick = {
                                        // Only select a vehicle if we actually have GPS for it.
                                        if (iconEntry.hasGps && !iconEntry.vehicleId.isNullOrBlank()) {
                                            if (isThisVehicleSelected) onBusSelected(null) else onBusSelected(iconEntry.vehicleId)
                                        } else {
                                            // no GPS available for this scheduled arrival; clear selection (no specific vehicle to show)
                                            onBusSelected(null)
                                        }
                                    })
                                ) {
                                    // Always show bus front icon (same for GPS-missing and GPS-available)
                                    Icon(
                                        imageVector = Lucide.BusFront,
                                        contentDescription = "bus icon",
                                        tint = fgColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )

                                    // badge in top-right corner; for Departed entries make it wider
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(badgeWidth)
                                            .height(badgeHeight),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.85f)
                                    ) {
                                        val badgeInnerPadding = if (isDeparted) 6.dp else 0.dp

                                        Box(
                                            modifier = Modifier
                                                .width(badgeWidth)
                                                .height(badgeHeight)
                                                .padding(horizontal = badgeInnerPadding),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = iconEntry.badge,
                                                color = Color.White,
                                                fontSize = if (isDeparted) 10.sp else 10.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                style = TextStyle(lineHeight = 12.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
