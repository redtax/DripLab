package com.driplab.app.core.theme

import android.content.Context
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppThemeState(
    val theme: DripTheme = DripTheme.CLASSIC,
    val darkTheme: Boolean = false,
    val alertMode: AlertMode = AlertMode.STANDARD,
    val bgMusicUri: String = "",
    val selectedTtsEngine: String? = null
)

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppThemeState> = _state.asStateFlow()

    private fun loadState(): AppThemeState {
        val themeIndex = prefs.getInt(KEY_THEME_INDEX, 0)
        val theme = DripTheme.entries.getOrElse(themeIndex) { DripTheme.CLASSIC }
        val darkTheme = prefs.getBoolean(KEY_DARK_THEME, false)
        val alertModeIndex = prefs.getInt(KEY_ALERT_MODE_INDEX, 2)
        val alertMode = AlertMode.entries.getOrElse(alertModeIndex) { AlertMode.STANDARD }
        val bgMusicUri = prefs.getString(KEY_BG_MUSIC_URI, "") ?: ""
        val selectedTtsEngine = prefs.getString(KEY_TTS_ENGINE, null)
        return AppThemeState(
            theme = theme,
            darkTheme = darkTheme,
            alertMode = alertMode,
            bgMusicUri = bgMusicUri,
            selectedTtsEngine = selectedTtsEngine
        )
    }

    fun selectTheme(theme: DripTheme) {
        _state.value = _state.value.copy(theme = theme)
        prefs.edit().putInt(KEY_THEME_INDEX, DripTheme.entries.indexOf(theme)).apply()
    }

    fun toggleDarkTheme() {
        val newValue = !_state.value.darkTheme
        _state.value = _state.value.copy(darkTheme = newValue)
        prefs.edit().putBoolean(KEY_DARK_THEME, newValue).apply()
    }

    fun selectAlertMode(mode: AlertMode) {
        _state.value = _state.value.copy(alertMode = mode)
        prefs.edit().putInt(KEY_ALERT_MODE_INDEX, AlertMode.entries.indexOf(mode)).apply()
    }

    fun setBgMusicUri(uri: String) {
        _state.value = _state.value.copy(bgMusicUri = uri)
        prefs.edit().putString(KEY_BG_MUSIC_URI, uri).apply()
    }

    fun selectTtsEngine(engine: String?) {
        _state.value = _state.value.copy(selectedTtsEngine = engine)
        if (engine != null) {
            prefs.edit().putString(KEY_TTS_ENGINE, engine).apply()
        } else {
            prefs.edit().remove(KEY_TTS_ENGINE).apply()
        }
    }

    fun clearTtsEngine() {
        selectTtsEngine(null)
    }

    fun restoreFromBackup(settings: com.driplab.app.core.recipe.BackupSettings) {
        val theme = DripTheme.entries.getOrElse(settings.themeIndex) { DripTheme.CLASSIC }
        val alertMode = AlertMode.entries.getOrElse(settings.alertModeIndex) { AlertMode.STANDARD }
        _state.value = AppThemeState(
            theme = theme,
            darkTheme = settings.isDarkTheme,
            alertMode = alertMode,
            bgMusicUri = settings.bgMusicUri ?: "",
            selectedTtsEngine = settings.selectedTtsEngine
        )
        prefs.edit()
            .putInt(KEY_THEME_INDEX, settings.themeIndex)
            .putBoolean(KEY_DARK_THEME, settings.isDarkTheme)
            .putInt(KEY_ALERT_MODE_INDEX, settings.alertModeIndex)
            .putString(KEY_BG_MUSIC_URI, settings.bgMusicUri)
            .apply()
        if (settings.selectedTtsEngine != null) {
            prefs.edit().putString(KEY_TTS_ENGINE, settings.selectedTtsEngine).apply()
        } else {
            prefs.edit().remove(KEY_TTS_ENGINE).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "driplab_prefs"
        private const val KEY_THEME_INDEX = "theme_index"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ALERT_MODE_INDEX = "alert_mode_index"
        private const val KEY_BG_MUSIC_URI = "bg_music_uri"
        private const val KEY_TTS_ENGINE = "tts_engine"
    }
}