package com.danieljm.delijn.ui.components.stopdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.BusFront
import com.composables.icons.lucide.Lucide
import com.danieljm.delijn.ui.screens.stopdetailscreen.ArrivalInfo

@Composable
fun BusCard(arrival: ArrivalInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp), // Add padding to match stops BottomSheet style
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.BusFront,
                        contentDescription = "Bus front",
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = arrival.lineId,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
//                Card(
//                    colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
//                ) {
//                    Text(
//                        text = "Bus", // Placeholder for bus number or other info
//                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//                        color = Color.White,
//                        fontSize = 18.sp
//                    )
//                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF424242))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Bus",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = arrival.scheduledTime,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                // Delay/on time logic
                if (!arrival.isScheduleOnly) {
                    val delayMinutes = ((arrival.realArrivalTime - arrival.expectedArrivalTime) / 60_000).toInt()
                    val delayText = when {
                        delayMinutes == 0 -> "on time"
                        delayMinutes > 0 -> "+$delayMinutes"
                        else -> "-$delayMinutes"
                    }
                    val delayColor = when {
                        delayMinutes == 0 -> Color(0xFF74C4AB) // green
                        delayMinutes > 0 -> Color(0xFFD6978E) // red/pink
                        else -> Color(0xFF81D4FA) // baby blue
                    }
                    Text(
                        text = delayText,
                        color = delayColor,
                        fontSize = 18.sp,
                    )
                }
            }
        }
        // Destination column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = arrival.destination,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // Info row (omschrijving and countdown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = arrival.omschrijving,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "in ${formatCountdown(arrival.remainingMinutes)}",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

// if time is more than 59 minutes, show in hours and minutes
// if time is less than 0, show "at stop"
// if time is 59 minutes or less, show in minutes
private fun formatCountdown(minutes: Long): String {
//    return when {
//        minutes <= 0 -> "at stop"
//        else -> "$minutes min"
//    }
    return when {
        minutes <= 0 -> "at stop"
        minutes < 60 -> "$minutes min"
        else -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0L) {
                "$hours h"
            } else {
                "$hours h $mins min"
            }
        }
    }
}