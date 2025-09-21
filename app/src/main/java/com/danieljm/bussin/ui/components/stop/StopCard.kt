package com.danieljm.delijn.ui.components.stops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.danieljm.bussin.domain.model.Stop
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun StopCard(
    stop: Stop,
    userLat: Double?,
    userLon: Double?,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
) {
    // Only compute distance when both user coords and stop coords are available
    val distance = if (userLat != null && userLon != null && stop.latitude != null && stop.longitude != null) {
        calculateDistance(userLat, userLon, stop.latitude, stop.longitude)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            // Use a subtle grayish highlight that fits the dark color scheme
            containerColor = if (isHighlighted) Color(0xFF3C3F41) else Color(0xFF2A2D32)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.MapPin,
                contentDescription = "Bus stop",
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stop.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stop.id,
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (distance != null) {
                Text(
                    text = formatDistance(distance),
                    color = Color(0xFF81C784),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // Earth's radius in kilometers

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

private fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 0.1 -> "< 100m"
        distanceKm < 1.0 -> "${(distanceKm * 1000).roundToInt()}m"
        else -> "${"%.1f".format(distanceKm)}km"
    }
}
