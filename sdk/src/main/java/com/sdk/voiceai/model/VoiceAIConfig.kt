package com.sdk.voiceai.model

import java.util.Locale

enum class InputMode { HANDS_FREE, PUSH_TO_TALK }

data class VoiceAIConfig(
    val anthropicApiKey: String,
    val aiModel: String = "claude-3-5-sonnet-20241022",
    val systemPrompt: String? = "You are a helpful voice assistant. Keep responses concise and conversational.",
    val inputMode: InputMode = InputMode.HANDS_FREE,
    val locale: Locale = Locale.getDefault(),
    val silenceTimeoutMs: Long = 1200L,
    val maxHistoryTurns: Int = 20,
    val audioEnhancement: Boolean = true,
    val transcriptProcessing: Boolean = true,
    val debugLogging: Boolean = false,
    val apiBaseUrl: String = "https://api.anthropic.com",
    val certificatePins: List<String> = emptyList(),
    val piiRedaction: Boolean = false,
    val customVocabulary: List<String> = emptyList(),
    val responseFilter: (suspend (String) -> String)? = null,
    val emotionDetectionEnabled: Boolean = false,
    val emotionConsentRationale: String = "",
    val emotionAwareAI: Boolean = false,
    // Any? avoids circular module dependency; cast in VoiceAISDK.Builder
    val sttEngine: Any? = null,
    val ttsEngine: Any? = null,
    val aiEngine: Any? = null,
    val emotionDetector: Any? = null,
) {
    override fun toString(): String =
        "VoiceAIConfig(aiModel=$aiModel, inputMode=$inputMode, locale=$locale, " +
        "debugLogging=$debugLogging, anthropicApiKey=[REDACTED])"
}
