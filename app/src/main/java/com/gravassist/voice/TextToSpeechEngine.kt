package com.gravassist.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.gravassist.services.LoggerService
import java.util.Locale

class TextToSpeechEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                LoggerService.log("TTS", "Engine initialized ready")
            }
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GravAssistTTS")
            LoggerService.log("TTS", "Speaking: $text")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
