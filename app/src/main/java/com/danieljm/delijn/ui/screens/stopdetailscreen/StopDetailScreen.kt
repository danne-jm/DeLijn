package com.danieljm.delijn.ui.screens.stopdetailscreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun StopDetailScreen(
    stopId: String,
    viewModel: StopDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(stopId) {
        viewModel.loadStopDetails(stopId)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Stop Details", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Stop ID: ${uiState.stopId}")
            Text(text = "Stop Name: ${uiState.stopName}")
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            } else {
                Text(text = "Served Lines:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.servedLines.forEach { line ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(text = "${line.lineName} (${line.lineId})", style = MaterialTheme.typography.bodyLarge)
                        Text(text = line.asFromTo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
