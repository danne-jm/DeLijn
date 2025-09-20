package com.danieljm.bussin.ui.screens.stopdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.bussin.domain.usecase.GetStopDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StopDetailsViewModel @Inject constructor(
    private val getStopDetailsUseCase: GetStopDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopDetailsUiState())
    val uiState: StateFlow<StopDetailsUiState> = _uiState.asStateFlow()

    fun loadStopDetails(stopId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = getStopDetailsUseCase(stopId)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, selectedStop = res.getOrNull())
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                _uiState.value = _uiState.value.copy(isLoading = false, error = err)
            }
        }
    }
}
