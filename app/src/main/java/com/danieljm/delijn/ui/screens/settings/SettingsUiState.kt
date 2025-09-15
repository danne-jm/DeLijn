package com.danieljm.delijn.ui.screens.settings

import java.util.Locale

data class SettingsUiState(
    val locale: Locale = Locale.getDefault(),
    val isDarkMode: Boolean = false
)

