package com.danieljm.delijn.ui.screens.searchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchDetailViewModel(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getRealTimeArrivalsUseCase: GetRealTimeArrivalsForStopUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchDetailUiState())
    val uiState: StateFlow<SearchDetailUiState> = _uiState.asStateFlow()

    fun load(stopId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val stop = try { getStopDetailsUseCase(stopId) } catch (e: Exception) { null }
                val realtime = try { getRealTimeArrivalsUseCase(stopId) } catch (e: Exception) { emptyList() }
                _uiState.value = _uiState.value.copy(stop = stop, realtime = realtime, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
