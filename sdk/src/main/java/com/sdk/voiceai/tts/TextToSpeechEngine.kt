package com.sdk.voiceai.tts

import java.util.Locale

data class TtsConfig(
    val locale: Locale = Locale.getDefault(),
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
)

interface TextToSpeechEngine {
    suspend fun speak(text: String, config: TtsConfig)
    fun stop()
    fun shutdown()
}
