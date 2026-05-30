package com.driplab.app.core.tts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
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

    override fun onStop() {
        Log.d(TAG, "onStop")
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString() ?: request.text ?: ""
        if (text.isBlank()) {
            callback.error(TextToSpeech.ERROR_INVALID_REQUEST)
            return
        }
        Log.d(TAG, "onSynthesizeText: $text")
        callback.error(TextToSpeech.ERROR_SYNTHESIS)
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