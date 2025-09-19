package com.danieljm.delijn.ui.components.stopdetails

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.BusFront
import com.composables.icons.lucide.Lucide
import androidx.core.graphics.toColorInt

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
    itemsIcons: Map<String, List<BusIconEntry>> = emptyMap(),
    selectedVehicleId: String? = null,
    onToggle: (String?) -> Unit,
    onBusSelected: (String?) -> Unit = {}
) {
    if (items.isEmpty()) return

    val scroll = rememberScrollState()
    val isToggleable = items.size > 1

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

            val bgColor = try { item.bgHex?.let { Color(it.toColorInt()) } ?: Color(0xFF4CAF50) } catch (_: Exception) { Color(0xFF4CAF50) }
            val fgColor = try { item.fgHex?.let { Color(it.toColorInt()) } ?: Color.Black } catch (_: Exception) { Color.Black }
            val borderColor = try { item.borderHex?.let { Color(it.toColorInt()) } } catch (_: Exception) { null }

            val containerColor = if (isSelected || !isToggleable) bgColor else bgColor.copy(alpha = 0.35f)
            val textColor = if (isSelected || !isToggleable) fgColor else fgColor.copy(alpha = 0.6f)

            val icons = itemsIcons[item.id] ?: emptyList()

            // Calculate minimum width needed for departed badges. Produce a Modifier (width or wrapContentWidth).
            val widthModifier = if (icons.any { it.badge == "Departed" }) {
                val departedCount = icons.count { it.badge == "Departed" }
                val baseWidth = 60.dp // Minimum for line text
                // multiply Dp by Int (Dp * Int) so expressions type-check
                val departedBadgeSpace = 70.dp * departedCount // Extra space per departed badge
                val otherIconsSpace = 40.dp * (icons.size - departedCount) // Regular icons
                Modifier.width(baseWidth + departedBadgeSpace + otherIconsSpace + 8.dp)
            } else {
                Modifier.wrapContentWidth()
            }

            var itemModifier = Modifier
                .then(widthModifier)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)

            if (borderColor != null) {
                itemModifier = itemModifier.then(Modifier.border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(8.dp)))
            }

            if (isToggleable) {
                itemModifier = itemModifier.clickable(onClick = {
                    if (isSelected) onToggle(null) else onToggle(item.id)
                })
            }

            itemModifier = itemModifier
                .padding(horizontal = 12.dp)
                .animateContentSize()

            Box(modifier = itemModifier) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.displayText,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    // Show icons when either this line is selected OR when no line is selected (show all by default)
                    if ((selected == null || isSelected || !isToggleable) && icons.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            icons.forEach { iconEntry ->
                                val isThisVehicleSelected = iconEntry.vehicleId != null && selectedVehicleId != null && selectedVehicleId == iconEntry.vehicleId

                                val isDeparted = iconEntry.badge == "Departed"
                                val containerSize = if (isDeparted) 44.dp else 36.dp
                                val badgeWidth = if (isDeparted) 64.dp else 18.dp
                                val badgeHeight = if (isDeparted) 20.dp else 18.dp

                                val innerClickable = (iconEntry.hasGps && !iconEntry.vehicleId.isNullOrBlank()) || isThisVehicleSelected
                                var iconBoxModifier = Modifier
                                    .size(containerSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThisVehicleSelected) fgColor.copy(alpha = 0.12f) else Color.Transparent)

                                if (innerClickable) {
                                    iconBoxModifier = iconBoxModifier.clickable(onClick = {
                                        if (isThisVehicleSelected) onBusSelected(null) else onBusSelected(iconEntry.vehicleId)
                                    })
                                } else {
                                    // Consume taps on non-clickable icons so they don't fall through to the parent and toggle the row
                                    iconBoxModifier = iconBoxModifier.pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            // intentionally empty: consume the tap and do nothing
                                        })
                                    }
                                }

                                Box(modifier = iconBoxModifier) {
                                    Icon(
                                        imageVector = Lucide.BusFront,
                                        contentDescription = "bus icon",
                                        tint = fgColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )

                                    val badgeAlignment = Alignment.CenterEnd

                                    Surface(
                                        modifier = Modifier
                                            .align(badgeAlignment)
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
                                                fontSize = 10.sp,
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