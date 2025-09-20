package com.danieljm.bussin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.danieljm.bussin.ui.screens.stop.StopScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.danieljm.bussin.ui.theme.BussinTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BussinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StopScreen(modifier = Modifier.padding(innerPadding))
                    // Note: StopScreen signature expects optional handlers. Using defaults.
                }
            }
        }
    }
}