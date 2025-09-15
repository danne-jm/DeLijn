
package com.danieljm.delijn.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danieljm.delijn.utils.LanguageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(locale = Locale.getDefault()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setLocale(locale: Locale) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locale = locale)
            // Persisting locale is left as TODO (DataStore/shared prefs)
        }
    }
    fun applyLocale(context: android.content.Context) {
        val locale = _uiState.value.locale
        LanguageUtils.setLocale(context, locale)
    }
}


