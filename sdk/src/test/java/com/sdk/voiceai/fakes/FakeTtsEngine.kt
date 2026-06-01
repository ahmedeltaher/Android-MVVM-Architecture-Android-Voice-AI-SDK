package com.sdk.voiceai.fakes

import com.sdk.voiceai.tts.TextToSpeechEngine
import com.sdk.voiceai.tts.TtsConfig

class FakeTtsEngine : TextToSpeechEngine {

    var callCount: Int = 0
    val spokenTexts: MutableList<String> = mutableListOf()

    override suspend fun speak(text: String, config: TtsConfig) {
        callCount++
        spokenTexts.add(text)
    }

    override fun stop() {}
    override fun shutdown() {}
}
