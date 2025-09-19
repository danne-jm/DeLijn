package com.danieljm.delijn.ui.components.stopdetails

import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.BusFront
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SignalZero
import com.danieljm.delijn.domain.model.ArrivalInfo
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
                            // Draw a slight border using the Rand color while keeping the container color as the background
                            .then(
                                arrival.lineBackgroundBorderColorHex?.let { hex ->
                                    try {
                                        val parsed = Color(AndroidColor.parseColor(hex))
                                        Modifier.border(width = 1.dp, color = parsed, shape = RoundedCornerShape(8.dp))
                                    } catch (_: Exception) {
                                        Modifier
                                    }
                                } ?: Modifier
                            )
                            .background(lineBgColor) // Use dynamic color (kleurAchterGrond)
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
                            imageVector = if (arrival.isScheduleOnly) Lucide.CloudOff else Lucide.Bus,
                            contentDescription = if (arrival.isScheduleOnly) "No GPS" else "Bus",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            // If schedule-only (no real-time tracking), show explicit badge text
                            text = if (arrival.isScheduleOnly) "No GPS" else (arrival.vrtnum ?: "-"),
                            color = busIdTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Right side: Time and status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        // Always show scheduled arrival time. Do NOT recalculate or override with real-time data.
                        text = arrival.scheduledTime,
                        color = Color.White,
                        fontSize = 20.sp,
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

                    // Compute remaining milliseconds (prefer real-time if available)
                    val now = System.currentTimeMillis()
                    val remainingMillis = when {
                        arrival.realArrivalTime > 0L -> arrival.realArrivalTime - now
                        arrival.expectedArrivalTime > 0L -> arrival.expectedArrivalTime - now
                        else -> Long.MIN_VALUE
                    }

                    Text(
                        text = formatCountdownMillis(remainingMillis),
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

private fun formatCountdownMillis(remainingMillis: Long): String {
    // If no valid timestamp available
    if (remainingMillis == Long.MIN_VALUE) return ""

    return when {
        remainingMillis < 0L -> "departed"
        remainingMillis < 20_000L -> "at stop"
        remainingMillis < 60_000L -> "arriving"
        else -> {
            val minutes = remainingMillis / 60_000L
            if (minutes < 60L) {
                "in $minutes min"
            } else {
                val hours = minutes / 60L
                val mins = minutes % 60L
                if (mins == 0L) {
                    "in $hours h"
                } else {
                    "in $hours h $mins min"
                }
            }
        }
    }
}