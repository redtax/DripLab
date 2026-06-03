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
    val selectedEngine: String? = null,
    val recommendedEngines: List<RecommendedTtsEngine> = emptyList()
)

data class TtsEngineInfo(
    val packageName: String,
    val label: String
)

data class RecommendedTtsEngine(
    val packageName: String,
    val displayName: String,
    val installHint: String,
    val isInstalled: Boolean
)

data class TtsTestResult(
    val engine: String,
    val success: Boolean,
    val message: String
)

object RecommendedTtsEngines {
    const val GOOGLE_TTS = "com.google.android.tts"
    const val XIAOMI_BRAIN = "com.xiaomi.mibrain.speech"
    // 讯飞语记实际包名（注意：不是 com.iflytek.inputmethod 那个是讯飞输入法）
    // 兼容多设备变体：voicenote 主包、tts 引擎、cloud 云
    val IFLY_CANDIDATES = listOf(
        "com.iflytek.voicenote",  // 讯飞语记（主包）
        "com.iflytek.tts",        // 讯飞语音 TTS 引擎（独立安装场景）
        "com.iflytek.cloud",      // 讯飞云能力
        "com.iflytek.speechcloud" // 讯飞语音云（老版本）
    )
    const val IFLY = "com.iflytek.voicenote"

    val ALL = listOf(
        RecommendedTtsEngine(
            packageName = GOOGLE_TTS,
            displayName = "Google 文字转语音",
            installHint = "Play Store 搜索 \"Speech Services by Google\"",
            isInstalled = false
        ),
        RecommendedTtsEngine(
            packageName = XIAOMI_BRAIN,
            displayName = "小米大脑语音引擎",
            installHint = "小米手机内置 com.xiaomi.mibrain.speech，无需安装",
            isInstalled = false
        ),
        RecommendedTtsEngine(
            packageName = IFLY,
            displayName = "讯飞语记",
            installHint = "各大应用商店搜索 \"讯飞语记\"",
            isInstalled = false
        )
    )
}

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
        loadRecommendedEngines()
    }

    private fun loadSelectedEngine() {
        _ttsState.value = _ttsState.value.copy(selectedEngine = themeManager.state.value.selectedTtsEngine)
    }

    private fun loadRecommendedEngines() {
        val installedPkgs = detectInstalledTtsPackages()
        val installedLabels = detectInstalledTtsLabels()
        val withStatus = RecommendedTtsEngines.ALL.map { rec ->
            val installed = when (rec.displayName) {
                "Google 文字转语音" -> rec.packageName in installedPkgs
                "小米大脑语音引擎" -> rec.packageName in installedPkgs
                "讯飞语记" -> isIflytekInstalled(installedPkgs, installedLabels)
                else -> rec.packageName in installedPkgs
            }
            rec.copy(isInstalled = installed)
        }
        _ttsState.value = _ttsState.value.copy(recommendedEngines = withStatus)
        Log.i(TAG, "TTS settings: recommended engines status: ${withStatus.map { "${it.displayName}=${it.isInstalled}" }}")
    }

    private fun isIflytekInstalled(installedPkgs: Set<String>, installedLabels: List<String>): Boolean {
        // 优先按包名候选匹配
        val byPackage = RecommendedTtsEngines.IFLY_CANDIDATES.any { it in installedPkgs }
        // 兜底：按已注册 TTS_SERVICE 的标签名匹配（兼容变种包名）
        val byLabel = installedLabels.any { label ->
            label.contains("讯飞", ignoreCase = true) ||
            label.contains("iflytek", ignoreCase = true) ||
            label.contains("iFly", ignoreCase = true)
        }
        Log.d(TAG, "TTS settings: iFlytek check byPackage=$byPackage byLabel=$byLabel labels=$installedLabels")
        return byPackage || byLabel
    }

    private fun detectInstalledTtsLabels(): List<String> {
        val labels = mutableListOf<String>()
        try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val infos = appContext.packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in infos) {
                val label = info.serviceInfo.loadLabel(appContext.packageManager).toString()
                labels.add(label)
                Log.d(TAG, "TTS settings: TTS_SERVICE registered pkg=${info.serviceInfo.packageName} label=$label")
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS settings: TTS_SERVICE label query failed: ${e.message}")
        }
        return labels
    }

    private fun detectInstalledTtsPackages(): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val infos = appContext.packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            infos.forEach { result.add(it.serviceInfo.packageName) }
        } catch (e: Exception) {
            Log.w(TAG, "TTS settings: PackageManager query failed: ${e.message}")
        }
        // 候选包名检测
        val allCandidates = listOf(
            RecommendedTtsEngines.GOOGLE_TTS,
            RecommendedTtsEngines.XIAOMI_BRAIN
        ) + RecommendedTtsEngines.IFLY_CANDIDATES
        for (pkg in allCandidates) {
            try {
                appContext.packageManager.getPackageInfo(pkg, 0)
                result.add(pkg)
                Log.d(TAG, "TTS settings: candidate package installed: $pkg")
            } catch (_: PackageManager.NameNotFoundException) {
                Log.d(TAG, "TTS settings: candidate package NOT installed: $pkg")
            } catch (e: Exception) {
                Log.w(TAG, "TTS settings: getPackageInfo($pkg) exception: ${e.message}")
            }
        }
        return result
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
                loadRecommendedEngines()
            } catch (e: Exception) {
                _ttsState.value = _ttsState.value.copy(isLoading = false)
                Log.e(TAG, "TTS settings: discover failed: ${e.message}")
            }
        }
    }

    fun refreshTtsState() {
        discoverTtsEngines()
        loadRecommendedEngines()
    }

    fun testTtsEngine(enginePackage: String) {
        Log.i(TAG, "TTS test: start engine=$enginePackage (API=${android.os.Build.VERSION.SDK_INT}, device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL})")
        // 预检：先确认包是否已安装
        val pm = appContext.packageManager
        val installed = try {
            pm.getPackageInfo(enginePackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "TTS test: package NOT installed: $enginePackage")
            false
        } catch (e: Exception) {
            Log.w(TAG, "TTS test: getPackageInfo exception: ${e.message}")
            true
        }
        if (!installed) {
            _ttsState.value = _ttsState.value.copy(
                testingEngine = null,
                testResult = TtsTestResult(
                    engine = enginePackage,
                    success = false,
                    message = "引擎包未安装: $enginePackage"
                )
            )
            return
        }

        // 预检：检查 TTS_SERVICE 注册情况
        try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val matched = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val registered = matched.any { it.serviceInfo.packageName == enginePackage }
            Log.i(TAG, "TTS test: TTS_SERVICE registered=$registered (total=${matched.size}) for $enginePackage")
            if (!registered) {
                Log.w(TAG, "TTS test: $enginePackage 未注册 TTS_SERVICE，可能引擎已损坏或被禁用")
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS test: queryIntentServices failed: ${e.message}")
        }

        _ttsState.value = _ttsState.value.copy(testingEngine = enginePackage, testResult = null)
        testTts?.stop()
        testTts?.shutdown()
        testTts = null

        try {
            testTts = TextToSpeech(appContext, { status ->
                Log.i(TAG, "TTS test onInit: engine=$enginePackage status=$status (0=SUCCESS, -1=ERROR, -2=STOPPED)")
                if (status != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS test: FAIL engine=$enginePackage status=$status, shutting down and trying default engine fallback")
                    testTts?.shutdown()
                    testTts = null
                    tryFallbackDefaultEngine(enginePackage)
                    return@TextToSpeech
                }
                val activeTts = testTts
                if (activeTts == null) {
                    Log.e(TAG, "TTS test: SUCCESS but testTts==null, aborting")
                    return@TextToSpeech
                }
                Log.i(TAG, "TTS test: SUCCESS, current engine=${activeTts.defaultEngine ?: "unknown"}, scanning voices...")
                val voices = try { activeTts.voices } catch (e: Exception) { null }
                val zhVoices = voices?.filter { it.locale?.language == "zh" }
                Log.i(TAG, "TTS test: voices total=${voices?.size ?: "n/a"}, zhVoices=${zhVoices?.size ?: 0}")

                var langOk = false
                val candidates = listOf(
                    Locale.forLanguageTag("zh-CN"),
                    Locale.SIMPLIFIED_CHINESE,
                    Locale.CHINESE,
                    Locale.forLanguageTag("zh"),
                    Locale.US
                )
                for (locale in candidates) {
                    val result = activeTts.setLanguage(locale)
                    val resultName = when (result) {
                        TextToSpeech.LANG_AVAILABLE -> "LANG_AVAILABLE"
                        TextToSpeech.LANG_COUNTRY_AVAILABLE -> "LANG_COUNTRY_AVAILABLE"
                        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "LANG_COUNTRY_VAR_AVAILABLE"
                        TextToSpeech.LANG_MISSING_DATA -> "LANG_MISSING_DATA"
                        TextToSpeech.LANG_NOT_SUPPORTED -> "LANG_NOT_SUPPORTED"
                        else -> "UNKNOWN($result)"
                    }
                    Log.i(TAG, "TTS test setLanguage(locale=$locale, country=${locale.country}) -> $resultName")
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        langOk = true
                        break
                    }
                }
                if (!langOk) {
                    Log.w(TAG, "TTS test: primary candidates failed, scanning all available locales for zh")
                    for (loc in Locale.getAvailableLocales()) {
                        if (loc.language == "zh") {
                            val result = activeTts.setLanguage(loc)
                            Log.i(TAG, "TTS test fallback setLanguage($loc) result=$result")
                            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                                langOk = true
                                break
                            }
                        }
                    }
                }
                if (!langOk) {
                    Log.e(TAG, "TTS test: no usable voice data for any zh locale on engine=$enginePackage")
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

                activeTts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS test utterance onStart id=$utteranceId")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.i(TAG, "TTS test utterance onDone id=$utteranceId -> SUCCESS")
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
                        Log.e(TAG, "TTS test utterance onError id=$utteranceId")
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
                        Log.e(TAG, "TTS test utterance onError id=$utteranceId code=$errorCode")
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

                val enginesActive = try { activeTts.engines?.joinToString() ?: "n/a" } catch (e: Exception) { "n/a" }
                Log.d(TAG, "TTS test speak: engine=${activeTts.defaultEngine} engines=$enginesActive")
                val speakResult = activeTts.speak("测试一下，这是滴落间Lab咖啡冲煮助手", TextToSpeech.QUEUE_FLUSH, null, "tts_test_${System.currentTimeMillis()}")
                if (speakResult != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS test speak FAIL: result=$speakResult")
                    _ttsState.value = _ttsState.value.copy(
                        testingEngine = null,
                        testResult = TtsTestResult(
                            engine = enginePackage,
                            success = false,
                            message = "播放失败 (speak=$speakResult)"
                        )
                    )
                } else {
                    Log.i(TAG, "TTS test speak enqueued OK, waiting onDone...")
                }
            }, enginePackage)
        } catch (e: Exception) {
            Log.e(TAG, "TTS test: ctor threw exception engine=$enginePackage: ${e.message}", e)
            testTts?.shutdown()
            testTts = null
            tryFallbackDefaultEngine(enginePackage)
        }
    }

    private fun tryFallbackDefaultEngine(originalEngine: String) {
        Log.i(TAG, "TTS test: attempting fallback to default engine for original=$originalEngine")
        try {
            testTts = TextToSpeech(appContext, { fallbackStatus ->
                Log.i(TAG, "TTS test fallback onInit: status=$fallbackStatus (0=SUCCESS, -1=ERROR)")
                if (fallbackStatus != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS test: fallback default engine also FAIL status=$fallbackStatus")
                    testTts?.shutdown()
                    testTts = null
                    _ttsState.value = _ttsState.value.copy(
                        testingEngine = null,
                        testResult = TtsTestResult(
                            engine = originalEngine,
                            success = false,
                            message = "引擎初始化失败 (status=$fallbackStatus)，请检查系统 TTS 设置"
                        )
                    )
                    return@TextToSpeech
                }
                val fallbackTts = testTts ?: return@TextToSpeech
                val zhVoices = try { fallbackTts.voices?.filter { it.locale?.language == "zh" } } catch (e: Exception) { null }
                Log.i(TAG, "TTS test fallback: SUCCESS, default engine=${fallbackTts.defaultEngine}, zhVoices=${zhVoices?.size ?: "n/a"}")
                _ttsState.value = _ttsState.value.copy(
                    testingEngine = null,
                    testResult = TtsTestResult(
                        engine = originalEngine,
                        success = false,
                        message = "原引擎失败，但默认引擎 ${fallbackTts.defaultEngine ?: "未知"} 可用，请在系统 TTS 设置中启用"
                    )
                )
                testTts?.shutdown()
                testTts = null
            })
        } catch (e: Exception) {
            Log.e(TAG, "TTS test: fallback default engine ctor threw: ${e.message}", e)
            testTts?.shutdown()
            testTts = null
            _ttsState.value = _ttsState.value.copy(
                testingEngine = null,
                testResult = TtsTestResult(
                    engine = originalEngine,
                    success = false,
                    message = "默认引擎也无法初始化，请检查系统 TTS 引擎"
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