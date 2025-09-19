package com.danieljm.delijn.ui.components.stopdetails

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating selector row for bus lines at a stop.
 * - `items` is a list of unique line identifiers with display text and colors.
 * - `selected` is the currently selected line id (or null for none).
 * - `onToggle` is called with the item id when tapped (toggle behavior handled by caller).
 */
data class FloatingBusItem(
    val id: String,
    val displayText: String,
    val bgHex: String?,
    val fgHex: String?
)

@Composable
fun FloatingBusSelectorRow(
    items: List<FloatingBusItem>,
    selected: String?,
    modifier: Modifier = Modifier,
    onToggle: (String?) -> Unit
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

            val containerColor = if (isSelected) bgColor else bgColor.copy(alpha = 0.35f)
            val textColor = if (isSelected) fgColor else fgColor.copy(alpha = 0.6f)

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(containerColor)
                    .clickable(onClick = {
                        if (isSelected) onToggle(null) else onToggle(item.id)
                    })
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = item.displayText,
                    color = textColor,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
