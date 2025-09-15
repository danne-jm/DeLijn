package com.danieljm.delijn.ui.screens.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.SearchStopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StopsViewModel(
    private val searchStopsUseCase: SearchStopsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StopsUiState())
    val uiState: StateFlow<StopsUiState> = _uiState.asStateFlow()

    fun searchStops(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(stops = emptyList())
                return@launch
            }
            val results = try {
                searchStopsUseCase(query)
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(stops = results)
        }
    }
}
