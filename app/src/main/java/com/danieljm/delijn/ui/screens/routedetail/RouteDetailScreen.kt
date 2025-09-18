package com.danieljm.delijn.ui.screens.routedetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun RouteDetailScreen(viewModel: RouteDetailViewModel = koinViewModel()) {
    val uiState = viewModel.uiState.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Route details", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        uiState.route?.let { route ->
            Text(text = route.name ?: "Unknown route")
        } ?: Text(text = "No route selected")
    }
}
