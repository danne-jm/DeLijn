package com.danieljm.delijn.ui.screens.plan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PlanRepository {
    fun getPlans(): List<String> = listOf("Trip to Brussels", "Trip to Antwerp")
}

interface LocationRepository {
    fun getCurrentLocation(): String = "Ghent"
}

interface SettingsRepository {
    fun getSettings(): String = "Default settings"
}

class DefaultPlanRepository : PlanRepository
class DefaultLocationRepository : LocationRepository
class DefaultSettingsRepository : SettingsRepository

class PlanViewModel(
    private val planRepository: PlanRepository = DefaultPlanRepository(),
    private val locationRepository: LocationRepository = DefaultLocationRepository(),
    private val settingsRepository: SettingsRepository = DefaultSettingsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        // Simulate loading
        val plans = planRepository.getPlans()
        _uiState.value = _uiState.value.copy(isLoading = false, plans = plans)
    }
}
