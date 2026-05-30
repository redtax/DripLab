package com.driplab.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.theme.AppThemeState
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.core.theme.ThemeManager
import com.driplab.app.core.tts.TtsEngineManager
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    val ttsEngineManager: TtsEngineManager
) : ViewModel() {

    val uiState: StateFlow<AppThemeState> = themeManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeState())

    fun selectTheme(theme: DripTheme) {
        themeManager.selectTheme(theme)
    }

    fun toggleDarkTheme() {
        themeManager.toggleDarkTheme()
    }

    fun selectAlertMode(mode: AlertMode) {
        themeManager.selectAlertMode(mode)
    }

    fun setBgMusicUri(uri: String) {
        themeManager.setBgMusicUri(uri)
    }
}