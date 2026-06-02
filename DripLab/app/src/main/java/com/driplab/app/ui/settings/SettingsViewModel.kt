package com.driplab.app.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driplab.app.core.backup.ImportSummary
import com.driplab.app.core.backup.SettingsBackupManager
import com.driplab.app.core.database.dao.RecipeDao
import com.driplab.app.core.theme.AppThemeState
import com.driplab.app.core.theme.DripTheme
import com.driplab.app.core.theme.ThemeManager
import com.driplab.app.domain.model.AlertMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val exportFile: File? = null,
    val importSummary: ImportSummary? = null,
    val errorMessage: String? = null
)

data class TtsEngineUiState(
    val engines: List<TtsEngineInfo> = emptyList(),
    val isLoading: Boolean = false,
    val testingEngine: String? = null,
    val testResult: TtsTestResult? = null,
    val selectedEngine: String? = null
)

data class TtsEngineInfo(
    val packageName: String,
    val label: String
)

data class TtsTestResult(
    val engine: String,
    val success: Boolean,
    val message: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val recipeDao: RecipeDao,
    private val backupManager: SettingsBackupManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<AppThemeState> = themeManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeState())

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val _ttsState = MutableStateFlow(TtsEngineUiState())
    val ttsState: StateFlow<TtsEngineUiState> = _ttsState.asStateFlow()

    private var testTts: TextToSpeech? = null

    init {
        loadSelectedEngine()
        discoverTtsEngines()
    }

    private fun loadSelectedEngine() {
        _ttsState.value = _ttsState.value.copy(selectedEngine = themeManager.state.value.selectedTtsEngine)
    }

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

    fun exportBackup() {
        viewModelScope.launch {
            _backupState.value = BackupUiState(isLoading = true)
            try {
                val file = backupManager.exportData(recipeDao)
                _backupState.value = BackupUiState(exportFile = file)
            } catch (e: Exception) {
                _backupState.value = BackupUiState(errorMessage = "导出失败: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState(isLoading = true)
            try {
                val result = backupManager.importData(uri, recipeDao, themeManager)
                result.fold(
                    onSuccess = { summary ->
                        _backupState.value = BackupUiState(importSummary = summary)
                    },
                    onFailure = { e ->
                        _backupState.value = BackupUiState(errorMessage = "导入失败: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                _backupState.value = BackupUiState(errorMessage = "导入失败: ${e.message}")
            }
        }
    }

    fun clearBackupResult() {
        _backupState.value = BackupUiState()
    }

    fun discoverTtsEngines() {
        _ttsState.value = _ttsState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val engines = resolveTtsEngineList()
                _ttsState.value = _ttsState.value.copy(engines = engines, isLoading = false)
                Log.i(TAG, "TTS settings: discovered ${engines.size} engines: ${engines.map { it.label }}")
            } catch (e: Exception) {
                _ttsState.value = _ttsState.value.copy(isLoading = false)
                Log.e(TAG, "TTS settings: discover failed: ${e.message}")
            }
        }
    }

    fun testTtsEngine(enginePackage: String) {
        _ttsState.value = _ttsState.value.copy(testingEngine = enginePackage, testResult = null)
        testTts?.stop()
        testTts?.shutdown()
        testTts = null

        try {
            testTts = TextToSpeech(appContext, { status ->
                if (status != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS test: engine init failed, status=$status")
                    _ttsState.value = _ttsState.value.copy(
                        testingEngine = null,
                        testResult = TtsTestResult(
                            engine = enginePackage,
                            success = false,
                            message = "初始化失败 (status=$status)"
                        )
                    )
                    return@TextToSpeech
                }

                var langOk = false
                val candidates = listOf(
                    Locale.forLanguageTag("zh-CN"),
                    Locale.SIMPLIFIED_CHINESE,
                    Locale.CHINESE,
                    Locale.forLanguageTag("zh")
                )
                for (locale in candidates) {
                    val result = testTts?.setLanguage(locale) ?: continue
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        langOk = true
                        break
                    }
                }
                if (!langOk) {
                    for (loc in Locale.getAvailableLocales()) {
                        if (loc.language == "zh") {
                            testTts?.setLanguage(loc)
                            langOk = true
                            break
                        }
                    }
                }
                if (!langOk) {
                    _ttsState.value = _ttsState.value.copy(
                        testingEngine = null,
                        testResult = TtsTestResult(
                            engine = enginePackage,
                            success = false,
                            message = "不支持中文语音"
                        )
                    )
                    return@TextToSpeech
                }

                testTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _ttsState.value = _ttsState.value.copy(
                            testingEngine = null,
                            testResult = TtsTestResult(
                                engine = enginePackage,
                                success = true,
                                message = "测试成功！语音播报正常"
                            )
                        )
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _ttsState.value = _ttsState.value.copy(
                            testingEngine = null,
                            testResult = TtsTestResult(
                                engine = enginePackage,
                                success = false,
                                message = "播放失败"
                            )
                        )
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _ttsState.value = _ttsState.value.copy(
                            testingEngine = null,
                            testResult = TtsTestResult(
                                engine = enginePackage,
                                success = false,
                                message = "播放失败 (code=$errorCode)"
                            )
                        )
                    }
                })

                val speakResult = testTts?.speak("测试一下，这是滴落间Lab咖啡冲煮助手", TextToSpeech.QUEUE_FLUSH, null, "tts_test_${System.currentTimeMillis()}")
                if (speakResult != TextToSpeech.SUCCESS) {
                    _ttsState.value = _ttsState.value.copy(
                        testingEngine = null,
                        testResult = TtsTestResult(
                            engine = enginePackage,
                            success = false,
                            message = "播放失败 (speak=$speakResult)"
                        )
                    )
                }
            }, enginePackage)
        } catch (e: Exception) {
            Log.e(TAG, "TTS test: exception: ${e.message}")
            _ttsState.value = _ttsState.value.copy(
                testingEngine = null,
                testResult = TtsTestResult(
                    engine = enginePackage,
                    success = false,
                    message = "引擎异常: ${e.message}"
                )
            )
        }
    }

    fun selectTtsEngine(engine: String?) {
        themeManager.selectTtsEngine(engine)
        _ttsState.value = _ttsState.value.copy(selectedEngine = engine)
    }

    fun clearTtsEngine() {
        themeManager.clearTtsEngine()
        _ttsState.value = _ttsState.value.copy(selectedEngine = null)
    }

    private fun resolveTtsEngineList(): List<TtsEngineInfo> {
        val result = linkedMapOf<String, String>()
        try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val infos = appContext.packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in infos) {
                val pkg = info.serviceInfo.packageName
                val label = info.serviceInfo.loadLabel(appContext.packageManager).toString()
                if (pkg !in result) {
                    result[pkg] = label
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS settings: PackageManager query failed: ${e.message}")
        }
        try {
            val engine = Settings.Secure.getString(appContext.contentResolver, "tts_default_synth")
            if (!engine.isNullOrBlank() && engine !in result) {
                result[engine] = engine
            }
        } catch (_: Exception) {}
        try {
            val method = TextToSpeech::class.java.getMethod("getDefaultEngine")
            val engine = method.invoke(null) as? String
            if (!engine.isNullOrBlank() && engine !in result) {
                result[engine] = engine
            }
        } catch (_: Exception) {}
        return result.map { (pkg, label) -> TtsEngineInfo(packageName = pkg, label = label) }
    }

    override fun onCleared() {
        super.onCleared()
        testTts?.stop()
        testTts?.shutdown()
        testTts = null
    }

    companion object {
        private const val TAG = "DripLab"
    }
}