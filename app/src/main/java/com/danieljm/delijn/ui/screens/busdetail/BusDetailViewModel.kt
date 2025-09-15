package com.danieljm.delijn.ui.screens.busdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetBusDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BusDetailViewModel(
    private val getBusDetailsUseCase: GetBusDetailsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BusDetailUiState())
    val uiState: StateFlow<BusDetailUiState> = _uiState.asStateFlow()

    fun load(busId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val bus = try {
                getBusDetailsUseCase(busId)
            } catch (e: Exception) {
                null
            }
            _uiState.value = _uiState.value.copy(bus = bus, isLoading = false)
        }
    }
}
