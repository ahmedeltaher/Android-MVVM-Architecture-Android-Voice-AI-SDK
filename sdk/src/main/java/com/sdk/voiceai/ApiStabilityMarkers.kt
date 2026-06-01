package com.sdk.voiceai

/**
 * Marks an API as stable. Breaking changes follow a deprecation cycle.
 *
 * Stable SDK types: [VoiceAISession], [VoiceAISDK], [com.sdk.voiceai.model.VoiceAIConfig]
 */
typealias Stable = com.sdk.voiceai.annotation.VoiceAIStable

/**
 * Marks an API as experimental. May change or be removed in any future release without notice.
 * Opt in with `@OptIn(Experimental::class)`.
 *
 * Experimental APIs: emotion detection ([VoiceAISession.emotionState], [VoiceAISession.emotionEvents],
 * [com.sdk.voiceai.emotion.VoiceEmotionDetector], [com.sdk.voiceai.emotion.EmotionResult],
 * [com.sdk.voiceai.emotion.Emotion], and the `emotion*` fields of VoiceAIConfig).
 */
typealias Experimental = com.sdk.voiceai.annotation.VoiceAIExperimental
