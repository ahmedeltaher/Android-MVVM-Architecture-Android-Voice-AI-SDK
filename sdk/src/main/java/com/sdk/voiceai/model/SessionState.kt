package com.sdk.voiceai.model

sealed interface SessionState {
    data object Idle : SessionState
    data object Listening : SessionState
    data object Processing : SessionState
    data object Speaking : SessionState
    data object Paused : SessionState
    data class Error(val error: VoiceAIError) : SessionState
}
