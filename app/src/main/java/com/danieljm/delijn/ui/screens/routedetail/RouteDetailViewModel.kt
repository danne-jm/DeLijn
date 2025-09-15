package com.danieljm.delijn.ui.screens.routedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.domain.usecase.GetRouteDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouteDetailViewModel(
    private val getRouteDetailsUseCase: GetRouteDetailsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    fun load(routeId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val route = try {
                getRouteDetailsUseCase(routeId)
            } catch (e: Exception) {
                null
            }
            _uiState.value = _uiState.value.copy(route = route, isLoading = false)
        }
    }
}
