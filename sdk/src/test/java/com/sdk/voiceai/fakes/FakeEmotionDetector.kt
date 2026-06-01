package com.sdk.voiceai.fakes

import com.sdk.voiceai.emotion.Emotion
import com.sdk.voiceai.emotion.EmotionConfig
import com.sdk.voiceai.emotion.EmotionResult
import com.sdk.voiceai.emotion.VoiceEmotionDetector

class FakeEmotionDetector(
    var nextResult: EmotionResult = EmotionResult(
        dominantEmotion = Emotion.NEUTRAL,
        scores = Emotion.values().associateWith { 0f },
        confidence = 0.5f,
    ),
) : VoiceEmotionDetector {

    var callCount: Int = 0

    override suspend fun detect(audio: ByteArray, config: EmotionConfig): EmotionResult {
        callCount++
        return nextResult
    }
}
