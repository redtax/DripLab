package com.driplab.app.core.tts

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class DripLabTtsService : android.speech.tts.TextToSpeechService() {

    companion object {
        private const val TAG = "DripLabTts"
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val locale = buildLocale(lang, country, variant)
        return if (locale?.language == "zh") {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zh", "CN", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val locale = buildLocale(lang, country, variant)
        return if (locale?.language == "zh") {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetVoices(): MutableList<Voice> {
        return mutableListOf(
            Voice("driplab-zh-CN", Locale.SIMPLIFIED_CHINESE, Voice.QUALITY_LOW, Voice.LATENCY_LOW, false, null)
        )
    }

    override fun onGetDefaultVoiceFor(locale: java.util.Locale?): String? {
        return if (locale?.language == "zh") "driplab-zh-CN" else null
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
    }

    override fun onSynthesizeText(
        utteranceId: String?,
        text: String?,
        lang: String?,
        country: String?,
        variant: String?,
        params: Bundle?
    ) {
        if (text.isNullOrBlank()) return
        Log.d(TAG, "onSynthesizeText: $text")
    }

    private fun buildLocale(lang: String?, country: String?, variant: String?): Locale? {
        val language = lang ?: return null
        val c = country ?: ""
        val v = variant ?: ""
        return if (v.isNotEmpty()) Locale(language, c, v)
        else if (c.isNotEmpty()) Locale(language, c)
        else Locale(language)
    }
}