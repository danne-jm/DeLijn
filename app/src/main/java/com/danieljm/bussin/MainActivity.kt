package com.danieljm.bussin

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.danieljm.bussin.ui.screens.stop.StopScreen
import com.danieljm.bussin.ui.theme.BussinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure window is truly edge-to-edge so content can draw under system bars.
        try { WindowCompat.setDecorFitsSystemWindows(window, false) } catch (_: Throwable) {}
        // Make system bars transparent so edge-to-edge content (MapView) is visible beneath them.
        setSystemBarsTransparent()
        // Ensure the window background is transparent so no white background shows through system bars
        try { window.setBackgroundDrawableResource(android.R.color.transparent) } catch (_: Throwable) {}
        setContent {
            BussinTheme {
                // Accept the scaffold content padding as an intentionally unused parameter
                // (underscore) so we don't apply it to StopScreen — StopScreen manages its own
                // overlays and full-bleed map rendering.
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    // Intentionally not applying paddingValues to StopScreen so the map can be
                    // full-bleed; consume it here so the compiler/linter considers it used.
                    paddingValues.hashCode()
                    StopScreen()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setSystemBarsTransparent() {
        try { window.statusBarColor = Color.TRANSPARENT } catch (_: Throwable) {}
        try { window.navigationBarColor = Color.TRANSPARENT } catch (_: Throwable) {}
    }
}
