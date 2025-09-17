package com.danieljm.delijn.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomBottomNavBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
    val itemCount = bottomNavItems.size

    // Animate the slider position
    val animatedSliderPosition by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) selectedIndex.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 0
        ),
        label = "slider_position"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D2124)) // Background
            .drawBehind {
                // Draw main top border
                drawLine(
                    color = Color(0xFF43464C),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 3.dp.toPx()
                )

                // Draw animated floating slider if an item is selected
                if (selectedIndex >= 0) {
                    val itemWidth = size.width / itemCount
                    val sliderStart = animatedSliderPosition * itemWidth
                    val sliderEnd = sliderStart + itemWidth

                    drawLine(
                        color = Color(0xFFCDBB11), // Active yellow color
                        start = androidx.compose.ui.geometry.Offset(sliderStart, 0f),
                        end = androidx.compose.ui.geometry.Offset(sliderEnd, 0f),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val activeColor = Color(0xFFCDBB11)
            val inactiveColor = Color(0xFF8B8C8E)

            Column(
                modifier = Modifier
                    .clickable { onItemClick(screen.route) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = screen.title,
                    color = if (isSelected) activeColor else inactiveColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}