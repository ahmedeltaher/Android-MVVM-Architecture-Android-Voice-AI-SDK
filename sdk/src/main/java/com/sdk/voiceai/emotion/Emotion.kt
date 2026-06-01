package com.sdk.voiceai.emotion

enum class Emotion { NEUTRAL, HAPPY, SAD, ANGRY, FEARFUL, SURPRISED, DISGUSTED }

data class EmotionResult(
    val dominantEmotion: Emotion,
    val scores: Map<Emotion, Float>,
    val confidence: Float,
)

data class EmotionConfig(
    val targetEmotions: Set<Emotion> = Emotion.values().toSet(),
    val humeApiKey: String? = null,
)

sealed interface EmotionEvent {
    data class EmotionChanged(val previous: Emotion, val current: Emotion) : EmotionEvent
    data class EmotionDetectionFailed(val cause: Throwable) : EmotionEvent
}
