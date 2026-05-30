package com.driplab.app.core.tts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isSystemDefault: Boolean
)

@Singleton
class TtsEngineManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "DripLab"
        private const val PREFS_NAME = "driplab_tts_prefs"
        private const val KEY_SELECTED_ENGINE = "selected_tts_engine"
    }

    fun getAvailableEngines(): List<TtsEngineInfo> {
        val result = mutableListOf<TtsEngineInfo>()
        val defaultEngine = resolveDefaultEngine()

        try {
            val intent = android.content.Intent("android.intent.action.TTS_SERVICE")
            val pm = appContext.packageManager
            val infos = pm.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val seen = mutableSetOf<String>()
            for (info in infos) {
                val pkg = info.serviceInfo.packageName
                if (pkg in seen) continue
                seen.add(pkg)
                val label = try {
                    info.serviceInfo.loadLabel(pm).toString()
                } catch (_: Exception) {
                    pkg
                }
                result.add(
                    TtsEngineInfo(
                        packageName = pkg,
                        label = label,
                        isSystemDefault = pkg == defaultEngine
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "TtsEngineManager query failed: ${e.message}")
        }

        return result.sortedByDescending { it.isSystemDefault }
    }

    private fun resolveDefaultEngine(): String? {
        try {
            val engine = Settings.Secure.getString(appContext.contentResolver, "tts_default_synth")
            if (!engine.isNullOrBlank()) return engine
        } catch (_: Exception) {}
        return null
    }

    fun getSelectedEngine(): String? {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ENGINE, null)
    }

    fun setSelectedEngine(enginePackage: String?) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_ENGINE, enginePackage).apply()
    }

    fun testEngine(
        enginePackage: String,
        onComplete: (success: Boolean, errorDetail: String?) -> Unit
    ) {
        var ttsInstance: TextToSpeech? = null
        try {
            ttsInstance = TextToSpeech(appContext, TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.SUCCESS) {
                    Log.w(TAG, "TtsEngineManager test $enginePackage init status=$status")
                    ttsInstance?.shutdown()
                    onComplete(false, "初始化失败 (status=$status)")
                    return@OnInitListener
                }
                val localeResult = ttsInstance?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                val localeOk = localeResult != null &&
                        localeResult != TextToSpeech.LANG_MISSING_DATA &&
                        localeResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!localeOk) {
                    val fallback = ttsInstance?.setLanguage(Locale.CHINESE)
                    val fallbackOk = fallback != null &&
                            fallback != TextToSpeech.LANG_MISSING_DATA &&
                            fallback != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!fallbackOk) {
                        ttsInstance?.shutdown()
                        onComplete(false, "不支持中文语音")
                        return@OnInitListener
                    }
                }
                ttsInstance?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        ttsInstance?.shutdown()
                        onComplete(true, null)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        ttsInstance?.shutdown()
                        onComplete(false, "语音合成播放失败")
                    }
                    override fun onError(id: String?, code: Int) {
                        ttsInstance?.shutdown()
                        onComplete(false, "语音合成失败 (code=$code)")
                    }
                })
                val speakResult = ttsInstance?.speak("滴落间Lab语音测试", TextToSpeech.QUEUE_FLUSH, null, "test_${System.currentTimeMillis()}")
                if (speakResult != TextToSpeech.SUCCESS) {
                    ttsInstance?.shutdown()
                    onComplete(false, "语音输出失败 (result=$speakResult)")
                }
            }, enginePackage)
        } catch (e: Exception) {
            Log.e(TAG, "TtsEngineManager test constructor error: ${e.message}")
            ttsInstance?.shutdown()
            onComplete(false, "引擎初始化异常: ${e.message}")
        }
    }
}