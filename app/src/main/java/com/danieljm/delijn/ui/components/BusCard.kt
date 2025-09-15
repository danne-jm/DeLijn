package com.danieljm.delijn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danieljm.delijn.domain.model.Bus

@Composable
fun BusCard(bus: Bus, onClick: (Bus) -> Unit = {}) {
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = bus.line)
            Text(text = bus.destination)
        }
    }
}

