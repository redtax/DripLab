package com.driplab.app.core.theme

import com.driplab.app.domain.model.AlertMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppThemeState(
    val theme: DripTheme = DripTheme.CLASSIC,
    val darkTheme: Boolean = false,
    val alertMode: AlertMode = AlertMode.STANDARD,
    val bgMusicUri: String = ""
)

@Singleton
class ThemeManager @Inject constructor() {
    private val _state = MutableStateFlow(AppThemeState())
    val state: StateFlow<AppThemeState> = _state.asStateFlow()

    fun selectTheme(theme: DripTheme) {
        _state.value = _state.value.copy(theme = theme)
    }

    fun toggleDarkTheme() {
        _state.value = _state.value.copy(darkTheme = !_state.value.darkTheme)
    }

    fun selectAlertMode(mode: AlertMode) {
        _state.value = _state.value.copy(alertMode = mode)
    }

    fun setBgMusicUri(uri: String) {
        _state.value = _state.value.copy(bgMusicUri = uri)
    }
}