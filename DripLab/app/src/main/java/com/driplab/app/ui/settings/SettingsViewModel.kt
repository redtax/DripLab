package com.driplab.app.ui.settings

import androidx.lifecycle.ViewModel
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val selectedTheme: DripTheme = DripTheme.CLASSIC,
    val darkTheme: Boolean = false,
    val alertMode: AlertMode = AlertMode.STANDARD
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun selectTheme(theme: DripTheme) {
        _uiState.value = _uiState.value.copy(selectedTheme = theme)
    }

    fun toggleDarkTheme() {
        _uiState.value = _uiState.value.copy(darkTheme = !_uiState.value.darkTheme)
    }

    fun selectAlertMode(mode: AlertMode) {
        _uiState.value = _uiState.value.copy(alertMode = mode)
    }
}