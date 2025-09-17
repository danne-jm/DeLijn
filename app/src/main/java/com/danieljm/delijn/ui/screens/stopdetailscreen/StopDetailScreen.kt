package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danieljm.delijn.ui.components.stopdetails.BusArrivalsBottomSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun StopDetailScreen(
    stopId: String,
    stopName: String,
    viewModel: StopDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stopId, stopName) {
        viewModel.loadStopDetails(stopId, stopName)
    }

    val DpSaver = Saver<Dp, Float>(
        save = { it.value },
        restore = { it.dp }
    )
    var bottomSheetHeight by rememberSaveable(stateSaver = DpSaver) { mutableStateOf(160.dp) }
    val arrivalsListState = rememberLazyListState()

    Surface(modifier = Modifier
        .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Remove station info from main column
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Stop Details", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stopName, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.error != null) {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
            BusArrivalsBottomSheet(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                arrivals = uiState.allArrivals,
                isLoading = uiState.isLoading,
                onHeightChanged = { height -> bottomSheetHeight = height },
                listState = arrivalsListState,
                stopName = stopName,
                stopId = uiState.stopId
            )
        }
    }
}
