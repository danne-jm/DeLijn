package com.danieljm.delijn.ui.components.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MapViewModel : ViewModel() {
    var mapState by mutableStateOf(MapState())
        private set

    fun updateMapState(newState: MapState) {
        mapState = newState
    }
}