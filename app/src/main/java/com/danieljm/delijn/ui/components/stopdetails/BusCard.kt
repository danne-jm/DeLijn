package com.danieljm.delijn.ui.components.stopdetails

import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.BusFront
import com.composables.icons.lucide.Lucide
import com.danieljm.delijn.domain.model.ArrivalInfo
import kotlin.compareTo
import kotlin.math.abs

@Composable
fun BusCard(arrival: ArrivalInfo) {
    // Add logging for color and badge text
    Log.i(
        "BusCard",
        "Rendering BusCard: lineId=${arrival.lineId}, lineNumberPublic=${arrival.lineNumberPublic}, lineBackgroundColorHex=${arrival.lineBackgroundColorHex}"
    )
    // Determine badge background color from hex if available, else fallback.
    val lineBgColor: Color = try {
        arrival.lineBackgroundColorHex?.let { hex ->
            Log.i("BusCard", "Parsing color hex: $hex")
            Color(AndroidColor.parseColor(hex))
        } ?: Color(0xFF4CAF50)
    } catch (e: Exception) {
        Log.w("BusCard", "Failed to parse color hex: ${arrival.lineBackgroundColorHex}", e)
        Color(0xFF4CAF50)
    }

    val lineBadgeText = arrival.lineNumberPublic ?: arrival.lineId
    val lineBadgeTextColor: Color = try {
        arrival.lineForegroundColorHex?.let { hex ->
            Color(AndroidColor.parseColor(hex))
        } ?: Color.Black
    } catch (e: Exception) {
        Color.Black
    }
    val busIdTextColor: Color = Color.White
    Log.i("BusCard", "Badge text: $lineBadgeText, Badge color: $lineBgColor, Badge text color: $lineBadgeTextColor")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2D32)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Use a single Column for the entire card content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First Row: Icons, line number, bus number
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.BusFront,
                            contentDescription = "Bus front",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(lineBgColor) // Use dynamic color
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lineBadgeText,
                            color = lineBadgeTextColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF424242))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Bus,
                            contentDescription = "Bus icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = arrival.vrtnum ?: "-",
                            color = busIdTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Right side: Time and status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        // The original file uses arrival.scheduledTime, but ArrivalInfo has 'time'. Using 'time' here.
                        text = arrival.time,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Delay/on time logic
                    if (!arrival.isScheduleOnly) {
                        val delayMinutes = ((arrival.realArrivalTime - arrival.expectedArrivalTime) / 60_000).toInt()
                        val delayText = when {
                            delayMinutes == 0 -> "on time"
                            delayMinutes > 0 -> "+ $delayMinutes"
                            else -> "- ${abs(delayMinutes)}"
                        }
                        val delayColor = when {
                            delayMinutes == 0 -> Color(0xFF74C4AB) // green
                            delayMinutes > 0 -> Color(0xFFD6978E) // red/pink
                            else -> Color(0xFF5C86EC) // baby blue
                        }
                        Text(
                            text = delayText,
                            color = delayColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            // Add a spacer to control the vertical space between the rows
            Spacer(modifier = Modifier.height(8.dp)) // Adjust this value to change the spacing

            // Second Row/Column: Destination and Countdown
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = arrival.destination,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
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
                        text = if (arrival.remainingMinutes <= 0) "at stop" else "in ${formatCountdown(arrival.remainingMinutes)}",
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
}

private fun formatCountdown(minutes: Long): String {
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