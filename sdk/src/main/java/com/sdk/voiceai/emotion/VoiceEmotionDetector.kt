package com.sdk.voiceai.emotion

interface VoiceEmotionDetector {
    suspend fun detect(audio: ByteArray, config: EmotionConfig): EmotionResult
}
