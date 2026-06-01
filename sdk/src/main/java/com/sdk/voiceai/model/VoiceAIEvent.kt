package com.sdk.voiceai.model

sealed interface VoiceAIEvent {
    data object SpeechStart : VoiceAIEvent
    data object SpeechEnd : VoiceAIEvent
    data class PartialTranscript(val text: String) : VoiceAIEvent
    data class FinalTranscript(val text: String, val confidence: Float?) : VoiceAIEvent
    data class PartialResponse(val text: String) : VoiceAIEvent
    data class FullResponse(val text: String) : VoiceAIEvent
    data object SpeakingStarted : VoiceAIEvent
    data object SpeakingStopped : VoiceAIEvent
    data class Retrying(val attempt: Int, val delayMs: Long) : VoiceAIEvent
    data object ContextCondensed : VoiceAIEvent
    data class ErrorOccurred(val error: VoiceAIError) : VoiceAIEvent
}
