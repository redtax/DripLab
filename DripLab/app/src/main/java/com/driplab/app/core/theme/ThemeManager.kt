package com.driplab.app.core.theme

import android.content.Context
import android.util.Log
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
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
        var selectedTtsEngine = prefs.getString(KEY_TTS_ENGINE, null)
        // 迁移：v1.0.4 之前 TTS 引擎选择存放在独立的 driplab_tts_prefs/selected_tts_engine
        // 升级到 v1.0.5+ 需把旧值搬入 driplab_prefs/tts_engine，保留用户原有选择
        if (selectedTtsEngine.isNullOrBlank()) {
            val oldTtsPrefs = appContext.getSharedPreferences(OLD_TTS_PREFS_NAME, Context.MODE_PRIVATE)
            val oldEngine = oldTtsPrefs.getString(OLD_KEY_TTS_ENGINE, null)
            if (!oldEngine.isNullOrBlank()) {
                selectedTtsEngine = oldEngine
                prefs.edit().putString(KEY_TTS_ENGINE, oldEngine).apply()
                // 迁移成功后清理旧 prefs，避免后续误读
                oldTtsPrefs.edit().remove(OLD_KEY_TTS_ENGINE).apply()
            }
        }
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
        syncToBackup()
    }

    fun toggleDarkTheme() {
        val newValue = !_state.value.darkTheme
        _state.value = _state.value.copy(darkTheme = newValue)
        prefs.edit().putBoolean(KEY_DARK_THEME, newValue).apply()
        syncToBackup()
    }

    fun selectAlertMode(mode: AlertMode) {
        _state.value = _state.value.copy(alertMode = mode)
        prefs.edit().putInt(KEY_ALERT_MODE_INDEX, AlertMode.entries.indexOf(mode)).apply()
        syncToBackup()
    }

    fun setBgMusicUri(uri: String) {
        _state.value = _state.value.copy(bgMusicUri = uri)
        prefs.edit().putString(KEY_BG_MUSIC_URI, uri).apply()
        syncToBackup()
    }

    fun selectTtsEngine(engine: String?) {
        _state.value = _state.value.copy(selectedTtsEngine = engine)
        if (engine != null) {
            prefs.edit().putString(KEY_TTS_ENGINE, engine).apply()
        } else {
            prefs.edit().remove(KEY_TTS_ENGINE).apply()
        }
        syncToBackup()
    }

    fun clearTtsEngine() {
        selectTtsEngine(null)
    }

    /**
     * v1.0.9 新增：设置变更后即时同步到 driplab_backup.json
     *
     * - 仅在备份文件已存在时执行（避免在没有备份时凭空创建，让显式导出仍是用户主导）
     * - 读取已有备份，合并当前 settings，保留 recipes 不动
     * - 失败时仅日志告警，不影响主流程
     */
    private fun syncToBackup() {
        try {
            val backupFile = File(appContext.filesDir, BACKUP_FILE_NAME)
            if (!backupFile.exists()) return
            val json = backupFile.readText()
            val obj = JSONObject(json)
            val settingsObj = obj.optJSONObject("settings") ?: JSONObject().also {
                obj.put("settings", it)
            }
            settingsObj.put("themeIndex", DripTheme.entries.indexOf(state.value.theme))
            settingsObj.put("isDarkTheme", state.value.darkTheme)
            settingsObj.put("alertModeIndex", AlertMode.entries.indexOf(state.value.alertMode))
            settingsObj.put("bgMusicUri", state.value.bgMusicUri)
            if (state.value.selectedTtsEngine != null) {
                settingsObj.put("selectedTtsEngine", state.value.selectedTtsEngine)
            } else {
                settingsObj.put("selectedTtsEngine", JSONObject.NULL)
            }
            backupFile.writeText(obj.toString())
            Log.d(TAG, "ThemeManager: synced settings to backup file")
        } catch (e: Exception) {
            Log.w(TAG, "ThemeManager: failed to sync to backup", e)
        }
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
        private const val TAG = "DripLab"
        private const val PREFS_NAME = "driplab_prefs"
        private const val BACKUP_FILE_NAME = "driplab_backup.json"
        private const val KEY_THEME_INDEX = "theme_index"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ALERT_MODE_INDEX = "alert_mode_index"
        private const val KEY_BG_MUSIC_URI = "bg_music_uri"
        private const val KEY_TTS_ENGINE = "tts_engine"
        // v1.0.4 之前的旧 TTS 引擎存储位置（升级时一次性迁移）
        private const val OLD_TTS_PREFS_NAME = "driplab_tts_prefs"
        private const val OLD_KEY_TTS_ENGINE = "selected_tts_engine"
    }
}